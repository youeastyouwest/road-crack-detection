package com.roadcrack.service.client;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.response.detection.BoundingBoxResponse;
import com.roadcrack.api.response.detection.DetectionItemResponse;
import com.roadcrack.service.config.AlgorithmClientProperties;
import com.roadcrack.service.model.DetectionAnalysisResult;
import com.roadcrack.service.model.DetectionTaskAggregate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Component
@ConditionalOnProperty(name = "crack.algorithm.mock-enabled", havingValue = "false")
public class HttpAlgorithmClient implements AlgorithmClient {

    private static final Logger log = LoggerFactory.getLogger(HttpAlgorithmClient.class);
    private final AlgorithmClientProperties props;
    @org.springframework.beans.factory.annotation.Value("${crack.upload.dir:uploads}")
    private String uploadDir;

    public HttpAlgorithmClient(AlgorithmClientProperties props) {
        this.props = props;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DetectionAnalysisResult analyze(DetectionTaskAggregate task) {
        // 视频检测使用独立端点
        if (task.getDataSourceType() == com.roadcrack.api.enums.DataSourceType.MANUAL_VIDEO) {
            return analyzeVideo(task);
        }
        return analyzeImage(task);
    }

    /**
     * 图片检测：Base64 方式调用 /detect/base64
     */
    private DetectionAnalysisResult analyzeImage(DetectionTaskAggregate task) {
        String url = props.getBaseUrl() + (props.getAnalyzePath() != null ? props.getAnalyzePath() : "/detect/base64");
        try {
            // Read image file and encode as base64
            String b64 = readFileAsBase64(task.getFileUrl());
            if (b64.isEmpty()) {
                log.warn("No image data for task {}, using fallback", task.getId());
                return fallbackResult(task);
            }

            // Build request body matching FastAPI DetectRequest format
            Map<String, Object> body = new HashMap<>();
            body.put("image", b64);
            body.put("conf_threshold", 0.05);
            body.put("iou_threshold", 0.45);

            log.info("Calling YOLOv8 at {} for image task {} (file: {})", url, task.getId(), task.getFileName());
            String json = objectMapper.writeValueAsString(body);
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(props.getConnectTimeoutMillis());
            conn.setReadTimeout(props.getReadTimeoutMillis());
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int status = conn.getResponseCode();
            if (status == 200) {
                String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return parseImageResponse(task, resp);
            } else {
                String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                log.warn("YOLOv8 returned {}: {}", status, err);
                return fallbackResult(task);
            }
        } catch (Exception e) {
            log.error("YOLOv8 image call failed: {}", e.getMessage());
            return fallbackResult(task);
        }
    }

    /**
     * 视频检测：Multipart 方式调用 /detect/video
     */
    private DetectionAnalysisResult analyzeVideo(DetectionTaskAggregate task) {
        String url = props.getBaseUrl() + (props.getVideoPath() != null ? props.getVideoPath() : "/detect/video");
        try {
            // Read video file from local path
            byte[] videoBytes = readFileBytes(task.getFileUrl());
            if (videoBytes == null || videoBytes.length == 0) {
                log.warn("No video data for task {}, using fallback", task.getId());
                return fallbackResult(task);
            }

            log.info("Calling YOLOv8 /detect/video for task {} (file: {}, size: {} bytes)",
                    task.getId(), task.getFileName(), videoBytes.length);

            // Build multipart form data
            String boundary = "----RoadCrackVideo" + System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setConnectTimeout(props.getConnectTimeoutMillis());
            conn.setReadTimeout(props.getVideoReadTimeoutMillis());

            // Write multipart body
            try (OutputStream os = conn.getOutputStream()) {
                // file part
                StringBuilder sb = new StringBuilder();
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(task.getFileName() != null ? task.getFileName() : "video.mp4").append("\"\r\n");
                sb.append("Content-Type: video/mp4\r\n\r\n");
                os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                os.write(videoBytes);
                os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                // conf_threshold part
                sb = new StringBuilder();
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Disposition: form-data; name=\"conf_threshold\"\r\n\r\n");
                sb.append("0.05\r\n");
                os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                // vid_stride part
                sb = new StringBuilder();
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Disposition: form-data; name=\"vid_stride\"\r\n\r\n");
                sb.append("2\r\n");
                os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                // end boundary
                String end = "--" + boundary + "--\r\n";
                os.write(end.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return parseVideoResponse(task, resp);
            } else {
                String err = conn.getErrorStream() != null
                        ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
                        : "unknown error";
                log.warn("YOLOv8 video endpoint returned {}: {}", status, err);
                return fallbackResult(task);
            }
        } catch (Exception e) {
            log.error("YOLOv8 video call failed: {}", e.getMessage(), e);
            return fallbackResult(task);
        }
    }

    /**
     * 读取文件并编码为 Base64
     */
    private String readFileAsBase64(String fileUrl) {
        try {
            byte[] bytes = readFileBytes(fileUrl);
            if (bytes != null && bytes.length > 0) {
                return Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            log.warn("Failed to read file from {}: {}", fileUrl, e.getMessage());
        }
        return "";
    }

    /**
     * 从各种来源读取文件字节数组
     */
    private byte[] readFileBytes(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        try {
            // HTTP/HTTPS URL
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                java.net.URL imgUrl = new java.net.URL(fileUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) imgUrl.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "RoadCrack/1.0");
                if (conn.getResponseCode() == 200) {
                    try (java.io.InputStream is = conn.getInputStream()) {
                        byte[] bytes = is.readAllBytes();
                        log.info("Downloaded from {} ({} bytes)", fileUrl, bytes.length);
                        return bytes;
                    }
                } else {
                    log.warn("HTTP download failed: {} returned {}", fileUrl, conn.getResponseCode());
                }
            } else if (fileUrl.startsWith("/uploads/") || fileUrl.startsWith("/")) {
                // Relative path: resolve against upload dir
                String subPath = fileUrl.startsWith("/uploads/")
                        ? fileUrl.substring("/uploads/".length())
                        : fileUrl.substring(1);
                java.nio.file.Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
                java.nio.file.Path filePath = uploadPath.resolve(subPath);
                if (Files.exists(filePath)) {
                    byte[] bytes = Files.readAllBytes(filePath);
                    log.info("Read local file {} ({} bytes)", filePath, bytes.length);
                    return bytes;
                } else {
                    log.warn("Local file not found: {}", filePath);
                }
            } else {
                // Absolute path
                java.nio.file.Path filePath = Paths.get(fileUrl);
                if (Files.exists(filePath)) {
                    byte[] bytes = Files.readAllBytes(filePath);
                    log.info("Read file {} ({} bytes)", filePath, bytes.length);
                    return bytes;
                } else {
                    log.warn("File not found: {}", fileUrl);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read file from {}: {}", fileUrl, e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private DetectionAnalysisResult parseImageResponse(DetectionTaskAggregate task, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            boolean success = root.path("success").asBoolean(false);
            if (!success) {
                String msg = root.path("message").asText("Detection failed");
                log.warn("YOLOv8 returned unsuccessful: {}", msg);
                return fallbackResult(task);
            }
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return fallbackResult(task);
            }
            JsonNode detections = data.path("detections");
            List<DetectionItemResponse> items = new ArrayList<>();
            if (detections.isArray()) {
                for (JsonNode d : detections) {
                    String typeStr = d.path("class_name").asText("CRACK");
                    double confidence = d.path("confidence").asDouble(0.0);
                    String severityStr = resolveSeverityFromConfidence(confidence);
                    JsonNode bboxRaw = d.path("bbox");
                    BoundingBoxResponse bbox;
                    if (bboxRaw.isArray() && bboxRaw.size() >= 4) {
                        bbox = new BoundingBoxResponse(
                            bboxRaw.get(0).asInt(),
                            bboxRaw.get(1).asInt(),
                            bboxRaw.get(2).asInt(),
                            bboxRaw.get(3).asInt()
                        );
                    } else {
                        bbox = new BoundingBoxResponse(0, 0, 0, 0);
                    }
                    String suggestion = generateSuggestion(parseDamageType(typeStr), parseSeverity(severityStr));
                    items.add(new DetectionItemResponse(parseDamageType(typeStr), parseSeverity(severityStr), confidence, bbox, suggestion));
                }
            }
            int total = data.path("num_detections").asInt(items.size());
            String summary = "YOLOv8 detection complete: " + total + " items found";
            String imageBase64 = data.path("image_base64").asText(null);
            // 解码 base64 写成文件，返回 URL（避免 200KB+ 字符串进数据库/响应体）
            String imageUrl = persistAnnotatedImage(task, imageBase64);
            return new DetectionAnalysisResult(summary, items, imageUrl);
        } catch (Exception e) {
            log.error("Failed to parse YOLOv8 response: {}", e.getMessage());
            return fallbackResult(task);
        }
    }

    /**
     * 解析 /detect/video 返回的视频检测结果
     * 汇总所有帧的检测数据，按空间位置去重后聚合为统一的结果列表
     */
    private DetectionAnalysisResult parseVideoResponse(DetectionTaskAggregate task, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            boolean success = root.path("success").asBoolean(false);
            if (!success) {
                String msg = root.path("message").asText("Video detection failed");
                log.warn("YOLOv8 video returned unsuccessful: {}", msg);
                return fallbackResult(task);
            }
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return fallbackResult(task);
            }

            int totalFrames = data.path("total_frames_processed").asInt(0);
            int framesWithDetections = data.path("frames_with_detections").asInt(0);
            int totalDetections = data.path("total_detections").asInt(0);
            String videoOutputPath = data.path("output_video_path").asText(null);

            // 收集所有帧的检测结果
            List<DetectionItemResponse> rawItems = new ArrayList<>();
            JsonNode frameResults = data.path("frame_results");
            if (frameResults.isArray()) {
                for (JsonNode frame : frameResults) {
                    JsonNode detections = frame.path("detections");
                    if (detections.isArray()) {
                        for (JsonNode d : detections) {
                            String typeStr = d.path("class_name").asText("CRACK");
                            double confidence = d.path("confidence").asDouble(0.0);
                            String severityStr = resolveSeverityFromConfidence(confidence);
                            JsonNode bboxRaw = d.path("bbox");
                            BoundingBoxResponse bbox;
                            if (bboxRaw.isArray() && bboxRaw.size() >= 4) {
                                bbox = new BoundingBoxResponse(
                                    bboxRaw.get(0).asInt(),
                                    bboxRaw.get(1).asInt(),
                                    bboxRaw.get(2).asInt(),
                                    bboxRaw.get(3).asInt()
                                );
                            } else {
                                bbox = new BoundingBoxResponse(0, 0, 0, 0);
                            }
                            String suggestion = generateSuggestion(parseDamageType(typeStr), parseSeverity(severityStr));
                            rawItems.add(new DetectionItemResponse(
                                parseDamageType(typeStr), parseSeverity(severityStr), confidence, bbox, suggestion));
                        }
                    }
                }
            }

            // 按空间位置去重：相邻帧中同一位置的裂缝合并，保留置信度最高的
            List<DetectionItemResponse> items = deduplicateByIoU(rawItems, 0.5);

            // 按置信度降序排列，取前30条展示
            items.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
            if (items.size() > 30) {
                items = new ArrayList<>(items.subList(0, 30));
            }

            String summary = String.format(
                "视频检测完成：共 %d 帧，%d 帧含病害，检测到 %d 处不同位置病害",
                totalFrames, framesWithDetections, items.size());

            // Python 端已返回可访问的 URL 路径
            String annotatedVideoUrl = videoOutputPath;

            // 解析关键帧 URL 列表
            List<String> keyframeUrls = new ArrayList<>();
            JsonNode kfNode = data.path("keyframe_urls");
            if (kfNode.isArray()) {
                for (JsonNode kf : kfNode) {
                    if (kf.isTextual()) {
                        keyframeUrls.add(kf.asText());
                    }
                }
            }

            return new DetectionAnalysisResult(summary, items, annotatedVideoUrl, keyframeUrls);
        } catch (Exception e) {
            log.error("Failed to parse YOLOv8 video response: {}", e.getMessage());
            return fallbackResult(task);
        }
    }

    /**
     * 按空间 IoU 对检测框去重：重叠度高的视为同一裂缝，保留置信度最高的
     */
    private List<DetectionItemResponse> deduplicateByIoU(List<DetectionItemResponse> items, double iouThreshold) {
        if (items == null || items.size() <= 1) return items;
        List<DetectionItemResponse> result = new ArrayList<>();
        for (DetectionItemResponse item : items) {
            boolean merged = false;
            for (int i = 0; i < result.size(); i++) {
                DetectionItemResponse existing = result.get(i);
                if (item.damageType() == existing.damageType() && bboxIou(item.boundingBox(), existing.boundingBox()) >= iouThreshold) {
                    // 同类型且重叠度高，保留置信度更高的
                    if (item.confidence() > existing.confidence()) {
                        result.set(i, item);
                    }
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 计算两个边界框的 IoU (Intersection over Union)
     */
    private double bboxIou(BoundingBoxResponse a, BoundingBoxResponse b) {
        int x1 = Math.max(a.x(), b.x());
        int y1 = Math.max(a.y(), b.y());
        int x2 = Math.min(a.x() + a.width(), b.x() + b.width());
        int y2 = Math.min(a.y() + a.height(), b.y() + b.height());
        if (x2 <= x1 || y2 <= y1) return 0.0;
        double inter = (double) (x2 - x1) * (y2 - y1);
        double areaA = (double) a.width() * a.height();
        double areaB = (double) b.width() * b.height();
        double union = areaA + areaB - inter;
        return union > 0 ? inter / union : 0.0;
    }

    /**
     * 把 YOLO 返回的标注图 base64 解码为 PNG，写入 uploads/result/task_{id}_{ts}.png。
     * 失败时返回 null（不影响检测结果数据）。
     */
    private String persistAnnotatedImage(DetectionTaskAggregate task, String imageBase64) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            java.nio.file.Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            java.nio.file.Path resultDir = uploadPath.resolve("result");
            Files.createDirectories(resultDir);
            String fileName = "task_" + task.getId() + "_" + System.currentTimeMillis() + ".jpg";
            java.nio.file.Path target = resultDir.resolve(fileName);
            Files.write(target, bytes);
            String url = "/uploads/result/" + fileName;
            log.info("Persisted annotated image for task {} -> {} ({} bytes)", task.getId(), url, bytes.length);
            return url;
        } catch (Exception e) {
            log.error("Failed to persist annotated image for task {}: {}", task.getId(), e.getMessage());
            return null;
        }
    }

    private String resolveSeverityFromConfidence(double confidence) {
        if (confidence >= 0.85) return "HIGH";
        if (confidence >= 0.60) return "MEDIUM";
        return "LOW";
    }

    private DetectionAnalysisResult fallbackResult(DetectionTaskAggregate task) {
        return new DetectionAnalysisResult("AI service unavailable", Collections.emptyList());
    }

    private String generateSuggestion(DamageType damageType, SeverityLevel severityLevel) {
        if (severityLevel == SeverityLevel.HIGH) {
            return "建议立即派单，优先安排现场核查与处置。";
        }
        return switch (damageType) {
            case ROAD_SPILL -> "建议通知环卫部门尽快清理，避免二次交通风险。";
            case MARKING_DAMAGE -> "建议路政部门补画或修复标志线。";
            case POTHOLE -> "建议安排坑槽修补，必要时同步交通管制。";
            default -> "建议继续复核裂缝宽度和延展趋势。";
        };
    }

    private String simpleToJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private DamageType parseDamageType(String s) {
        if (s == null) return DamageType.CRACK;
        switch (s.toUpperCase()) {
            case "CRACK": return DamageType.CRACK;
            case "POTHOLE": return DamageType.POTHOLE;
            case "MARKING_DAMAGE": case "MARKING": return DamageType.MARKING_DAMAGE;
            case "ROAD_SPILL": case "SPILL": case "SPILLAGE": return DamageType.ROAD_SPILL;
            default: return DamageType.CRACK;
        }
    }

    private SeverityLevel parseSeverity(String s) {
        if (s == null) return SeverityLevel.LOW;
        switch (s.toUpperCase()) {
            case "HIGH": return SeverityLevel.HIGH;
            case "MEDIUM": case "MED": return SeverityLevel.MEDIUM;
            default: return SeverityLevel.LOW;
        }
    }
}
