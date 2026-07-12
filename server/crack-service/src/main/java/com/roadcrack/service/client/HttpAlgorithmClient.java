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
        String url = props.getBaseUrl() + (props.getAnalyzePath() != null ? props.getAnalyzePath() : "/detect/base64");
        try {
            // Read image file and encode as base64
            String b64 = "";
            String fileUrl = task.getFileUrl();
            if (fileUrl != null && !fileUrl.isEmpty()) {
                try {
                    // Try HTTP download first (handles http:// and https:// URLs)
                    if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                        java.net.URL imgUrl = new java.net.URL(fileUrl);
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) imgUrl.openConnection();
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(30000);
                        conn.setRequestProperty("User-Agent", "RoadCrack/1.0");
                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200) {
                            try (java.io.InputStream is = conn.getInputStream()) {
                                byte[] imgBytes = is.readAllBytes();
                                b64 = Base64.getEncoder().encodeToString(imgBytes);
                                log.info("Downloaded image from {} ({} bytes, HTTP {})", fileUrl, imgBytes.length, responseCode);
                            }
                        } else {
                            log.warn("HTTP download failed: {} returned {}", fileUrl, responseCode);
                        }
                    } else if (fileUrl.startsWith("/uploads/") || fileUrl.startsWith("/")) {
                        // Relative URL exposed to frontend; resolve to configured upload dir
                        String subPath = fileUrl.startsWith("/uploads/")
                                ? fileUrl.substring("/uploads/".length())
                                : fileUrl.substring(1);
                        java.nio.file.Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
                        java.nio.file.Path imgPath = uploadPath.resolve(subPath);
                        if (Files.exists(imgPath)) {
                            byte[] imgBytes = Files.readAllBytes(imgPath);
                            b64 = Base64.getEncoder().encodeToString(imgBytes);
                        } else {
                            log.warn("Image file not found: {}", imgPath);
                        }
                    } else {
                        // Read from local file path
                        java.nio.file.Path imgPath = Paths.get(fileUrl);
                        if (Files.exists(imgPath)) {
                            byte[] imgBytes = Files.readAllBytes(imgPath);
                            b64 = Base64.getEncoder().encodeToString(imgBytes);
                        } else {
                            log.warn("Image file not found: {}", fileUrl);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to read image from {}: {}", fileUrl, e.getMessage());
                }
            }
            if (b64.isEmpty()) {
                log.warn("No image data for task {}, using fallback", task.getId());
                return fallbackResult(task);
            }

            // Build request body matching FastAPI DetectRequest format
            Map<String, Object> body = new HashMap<>();
            body.put("image", b64);
            body.put("conf_threshold", 0.05);
            body.put("iou_threshold", 0.45);

            log.info("Calling YOLOv8 at {} for task {} (file: {})", url, task.getId(), task.getFileName());
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
                return parseResponse(task, resp);
            } else {
                String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                log.warn("YOLOv8 returned {}: {}", status, err);
                return fallbackResult(task);
            }
        } catch (Exception e) {
            log.error("YOLOv8 call failed: {}", e.getMessage());
            return fallbackResult(task);
        }
    }

    @SuppressWarnings("unchecked")
    private DetectionAnalysisResult parseResponse(DetectionTaskAggregate task, String json) {
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
                    items.add(new DetectionItemResponse(null, parseDamageType(typeStr), parseSeverity(severityStr), confidence, bbox, suggestion));
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
