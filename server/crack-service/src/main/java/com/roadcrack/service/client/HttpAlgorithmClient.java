package com.roadcrack.service.client;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.response.detection.BoundingBoxResponse;
import com.roadcrack.api.response.detection.DetectionItemResponse;
import com.roadcrack.service.config.AlgorithmClientProperties;
import com.roadcrack.service.model.DetectionAnalysisResult;
import com.roadcrack.service.model.DetectionTaskAggregate;
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

    public HttpAlgorithmClient(AlgorithmClientProperties props) {
        this.props = props;
    }

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
            String json = simpleToJson(body);
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
            Map<String, Object> root = simpleJsonParse(json);
            boolean success = (Boolean) root.getOrDefault("success", false);
            if (!success) {
                String msg = (String) root.getOrDefault("message", "Detection failed");
                log.warn("YOLOv8 returned unsuccessful: {}", msg);
                return fallbackResult(task);
            }
            // Response: { success:true, data: { detections: [...], num_detections: ..., ... } }
            Map<String, Object> data = (Map<String, Object>) root.get("data");
            if (data == null) {
                return fallbackResult(task);
            }
            List<Map<String, Object>> detections = (List<Map<String, Object>>) data.getOrDefault("detections", Collections.emptyList());
            List<DetectionItemResponse> items = new ArrayList<>();
            for (Map<String, Object> d : detections) {
                String typeStr = (String) d.getOrDefault("class_name", "CRACK");
                double confidence = ((Number) d.getOrDefault("confidence", 0.0)).doubleValue();
                String severityStr = resolveSeverityFromConfidence(confidence);
                List<Number> bboxRaw = (List<Number>) d.getOrDefault("bbox", List.of(0, 0, 0, 0));
                BoundingBoxResponse bbox = bboxRaw.size() >= 4
                    ? new BoundingBoxResponse(bboxRaw.get(0).intValue(), bboxRaw.get(1).intValue(), bboxRaw.get(2).intValue(), bboxRaw.get(3).intValue())
                    : new BoundingBoxResponse(0, 0, 0, 0);
                String suggestion = generateSuggestion(parseDamageType(typeStr), parseSeverity(severityStr));
                items.add(new DetectionItemResponse(parseDamageType(typeStr), parseSeverity(severityStr), confidence, bbox, suggestion));
            }
            int total = ((Number) data.getOrDefault("num_detections", items.size())).intValue();
            String summary = "YOLOv8 detection complete: " + total + " items found";
            String imageBase64 = (String) data.get("image_base64");
            return new DetectionAnalysisResult(summary, items, imageBase64);
        } catch (Exception e) {
            log.error("Failed to parse YOLOv8 response: {}", e.getMessage());
            return fallbackResult(task);
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
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v instanceof String) sb.append("\"").append(escapeJson((String) v)).append("\"");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append("\"\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> simpleJsonParse(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) return result;
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return result;
        json = json.substring(1, json.length() - 1).trim();
        int i = 0;
        while (i < json.length()) {
            if (json.charAt(i) == ',' || json.charAt(i) == ' ') { i++; continue; }
            if (json.charAt(i) == '"') {
                int keyEnd = json.indexOf('"', i + 1);
                if (keyEnd < 0) break;
                String key = json.substring(i + 1, keyEnd);
                i = keyEnd + 1;
                while (i < json.length() && json.charAt(i) != ':') i++;
                i++;
                while (i < json.length() && json.charAt(i) == ' ') i++;
                if (i >= json.length()) break;
                char c = json.charAt(i);
                if (c == '"') {
                    int valEnd = i + 1;
                    boolean escaped = false;
                    while (valEnd < json.length()) {
                        if (!escaped && json.charAt(valEnd) == '\\') { escaped = true; valEnd++; continue; }
                        if (!escaped && json.charAt(valEnd) == '"') break;
                        escaped = false;
                        valEnd++;
                    }
                    result.put(key, json.substring(i + 1, valEnd));
                    i = valEnd + 1;
                } else if (c == '{') {
                    int brace = 1, j = i + 1;
                    while (j < json.length() && brace > 0) {
                        if (json.charAt(j) == '{') brace++;
                        else if (json.charAt(j) == '}') brace--;
                        j++;
                    }
                    result.put(key, simpleJsonParse(json.substring(i, j)));
                    i = j;
                } else if (c == '[') {
                    int bracket = 1, j = i + 1;
                    while (j < json.length() && bracket > 0) {
                        if (json.charAt(j) == '[') bracket++;
                        else if (json.charAt(j) == ']') bracket--;
                        j++;
                    }
                    result.put(key, parseJsonArray(json.substring(i, j)));
                    i = j;
                } else {
                    int j2 = i;
                    while (j2 < json.length() && json.charAt(j2) != ',' && json.charAt(j2) != '}' && json.charAt(j2) != ' ') j2++;
                    String raw = json.substring(i, j2).trim();
                    if ("true".equals(raw)) result.put(key, true);
                    else if ("false".equals(raw)) result.put(key, false);
                    else if ("null".equals(raw)) result.put(key, null);
                    else try { result.put(key, Double.parseDouble(raw)); } catch (Exception e) { result.put(key, raw); }
                    i = j2;
                }
            } else i++;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Object> parseJsonArray(String json) {
        List<Object> list = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[")) return list;
        json = json.substring(1, json.lastIndexOf(']')).trim();
        if (json.isEmpty()) return list;
        int i = 0;
        while (i < json.length()) {
            if (json.charAt(i) == ',' || json.charAt(i) == ' ') { i++; continue; }
            char c = json.charAt(i);
            if (c == '"') {
                int end = json.indexOf('"', i + 1);
                if (end < 0) break;
                list.add(json.substring(i + 1, end));
                i = end + 1;
            } else if (c == '{') {
                int brace = 1, j = i + 1;
                while (j < json.length() && brace > 0) {
                    if (json.charAt(j) == '{') brace++;
                    else if (json.charAt(j) == '}') brace--;
                    j++;
                }
                list.add(simpleJsonParse(json.substring(i, j)));
                i = j;
            } else {
                int j2 = i;
                while (j2 < json.length() && json.charAt(j2) != ',' && json.charAt(j2) != ']') j2++;
                String raw = json.substring(i, j2).trim();
                if ("true".equals(raw)) list.add(true);
                else if ("false".equals(raw)) list.add(false);
                else if ("null".equals(raw)) list.add(null);
                else try { list.add(Double.parseDouble(raw)); } catch (Exception e) { list.add(raw); }
                i = j2;
            }
        }
        return list;
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
