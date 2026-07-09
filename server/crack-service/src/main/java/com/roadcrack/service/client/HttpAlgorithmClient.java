package com.roadcrack.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "crack.algorithm.mock-enabled", havingValue = "false")
public class HttpAlgorithmClient implements AlgorithmClient {

    private static final Logger log = LoggerFactory.getLogger(HttpAlgorithmClient.class);

    private final ObjectMapper objectMapper;
    private final AlgorithmClientProperties properties;
    private final HttpClient httpClient;

    public HttpAlgorithmClient(ObjectMapper objectMapper, AlgorithmClientProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                .build();
    }

    @Override
    public DetectionAnalysisResult analyze(DetectionTaskAggregate task) {
        long startTime = System.currentTimeMillis();
        URI uri = buildAnalyzeUri();
        log.info("Calling algorithm service: taskId={}, taskCode={}, uri={}, fileUrl={}",
                task.getId(),
                task.getTaskCode(),
                uri,
                task.getFileUrl());
        try {
            byte[] imageBytes = loadImageBytes(task);
            String requestBody = buildAnalyzeRequestBody(imageBytes);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMillis()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Algorithm service returned non-success status: taskId={}, status={}, body={}",
                        task.getId(),
                        response.statusCode(),
                        response.body());
                throw new IllegalStateException("algorithm service returned status " + response.statusCode());
            }

            DetectionAnalysisResult result = parseResponse(response.body());
            log.info("Algorithm service completed: taskId={}, status={}, itemCount={}, durationMs={}",
                    task.getId(),
                    response.statusCode(),
                    result.items() == null ? 0 : result.items().size(),
                    System.currentTimeMillis() - startTime);
            return result;
        } catch (IOException exception) {
            log.error("Algorithm service I/O failure: taskId={}, message={}",
                    task.getId(),
                    exception.getMessage(),
                    exception);
            throw new IllegalStateException("failed to call algorithm service: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Algorithm service call interrupted: taskId={}", task.getId(), exception);
            throw new IllegalStateException("algorithm call interrupted", exception);
        }
    }

    private URI buildAnalyzeUri() {
        String baseUrl = trimTrailingSlash(properties.getBaseUrl());
        String analyzePath = properties.getAnalyzePath();
        if (analyzePath == null || analyzePath.isBlank()) {
            analyzePath = "/detect/base64";
        }
        if (!analyzePath.startsWith("/")) {
            analyzePath = "/" + analyzePath;
        }
        return URI.create(baseUrl + analyzePath);
    }

    private String buildAnalyzeRequestBody(byte[] imageBytes) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("image", Base64.getEncoder().encodeToString(imageBytes));
        return objectMapper.writeValueAsString(requestBody);
    }

    private byte[] loadImageBytes(DetectionTaskAggregate task) throws IOException, InterruptedException {
        String fileUrl = task.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalStateException("detection task file url is blank");
        }

        if (fileUrl.startsWith("data:")) {
            return decodeDataUri(fileUrl);
        }

        Path localPath = resolveLocalPath(fileUrl);
        if (localPath != null) {
            return Files.readAllBytes(localPath);
        }

        URI uri = resolveUri(fileUrl);
        if (uri == null) {
            throw new IllegalStateException("unsupported file url: " + fileUrl);
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            Path filePath = Path.of(uri);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
            throw new IllegalStateException("file does not exist: " + filePath);
        }
        if (isHttpUri(uri)) {
            return downloadRemoteBytes(uri);
        }
        throw new IllegalStateException("unsupported file url scheme: " + uri.getScheme());
    }

    private byte[] decodeDataUri(String dataUri) {
        int commaIndex = dataUri.indexOf(',');
        if (commaIndex < 0 || commaIndex == dataUri.length() - 1) {
            throw new IllegalStateException("invalid data uri image content");
        }
        return Base64.getDecoder().decode(dataUri.substring(commaIndex + 1));
    }

    private Path resolveLocalPath(String fileUrl) {
        try {
            Path directPath = Path.of(fileUrl);
            if (Files.exists(directPath)) {
                return directPath;
            }
            if (!directPath.isAbsolute()) {
                Path workspacePath = Path.of("").toAbsolutePath().resolve(directPath).normalize();
                if (Files.exists(workspacePath)) {
                    return workspacePath;
                }
            }
        } catch (InvalidPathException ignored) {
            return null;
        }
        return null;
    }

    private URI resolveUri(String fileUrl) {
        try {
            URI uri = URI.create(fileUrl);
            return uri.getScheme() == null ? null : uri;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private byte[] downloadRemoteBytes(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(properties.getReadTimeoutMillis()))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("failed to download image, status=" + response.statusCode());
        }
        return response.body();
    }

    private DetectionAnalysisResult parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.path("success").isBoolean() && !root.path("success").asBoolean()) {
            String error = firstText(root, "error", "message");
            throw new IllegalStateException(error == null ? "algorithm service returned failure" : error);
        }

        JsonNode payload = root.path("data").isMissingNode() || root.path("data").isNull() ? root : root.path("data");
        JsonNode itemsNode = findItemsNode(payload);

        List<DetectionItemResponse> items = new ArrayList<>();
        if (itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                items.add(toDetectionItem(itemNode));
            }
        }

        String summary = firstText(payload, "summary");
        if (summary == null || summary.isBlank()) {
            summary = firstText(root, "message");
        }
        if (summary == null || summary.isBlank()) {
            summary = "Algorithm detection completed, found " + items.size() + " potential damage items.";
        }
        return new DetectionAnalysisResult(summary, items);
    }

    private JsonNode findItemsNode(JsonNode payload) {
        if (payload.isArray()) {
            return payload;
        }
        JsonNode detections = payload.path("detections");
        if (detections.isArray()) {
            return detections;
        }
        JsonNode items = payload.path("items");
        if (items.isArray()) {
            return items;
        }
        JsonNode results = payload.path("results");
        if (results.isArray()) {
            return results;
        }
        return objectMapper.createArrayNode();
    }

    private DetectionItemResponse toDetectionItem(JsonNode itemNode) {
        String rawDamageType = firstText(itemNode,
                "damageType", "type", "category", "label", "class_name", "className", "class_name_cn", "classNameCn", "damage");
        double confidence = resolveDouble(itemNode,
                "confidence", "score", "probability", "conf");
        DamageType damageType = resolveDamageType(rawDamageType);
        SeverityLevel severityLevel = resolveSeverity(firstText(itemNode,
                "severityLevel", "severity", "level", "riskLevel"), confidence);
        BoundingBoxResponse boundingBox = resolveBoundingBox(itemNode);
        String suggestion = firstText(itemNode,
                "suggestion", "recommendation", "message", "advice");
        if (suggestion == null || suggestion.isBlank()) {
            suggestion = resolveSuggestion(damageType, severityLevel);
        }

        return new DetectionItemResponse(
                damageType,
                severityLevel,
                confidence,
                boundingBox,
                suggestion
        );
    }

    private BoundingBoxResponse resolveBoundingBox(JsonNode itemNode) {
        JsonNode boxNode = itemNode.path("boundingBox");
        if (boxNode.isMissingNode() || boxNode.isNull()) {
            boxNode = itemNode.path("bbox");
        }
        if (boxNode.isMissingNode() || boxNode.isNull()) {
            boxNode = itemNode.path("box");
        }

        if (boxNode.isArray() && boxNode.size() >= 4) {
            int x = roundNode(boxNode.get(0));
            int y = roundNode(boxNode.get(1));
            int x2 = roundNode(boxNode.get(2));
            int y2 = roundNode(boxNode.get(3));
            return new BoundingBoxResponse(x, y, Math.max(0, x2 - x), Math.max(0, y2 - y));
        }

        int x;
        int y;
        int width;
        int height;
        if (!boxNode.isMissingNode() && !boxNode.isNull()) {
            x = resolveInt(boxNode, "x", "left", "x1");
            y = resolveInt(boxNode, "y", "top", "y1");
            width = resolveInt(boxNode, "width", "w");
            height = resolveInt(boxNode, "height", "h");
            if (width == 0 && boxNode.has("x2")) {
                width = Math.max(0, resolveInt(boxNode, "x2") - x);
            }
            if (height == 0 && boxNode.has("y2")) {
                height = Math.max(0, resolveInt(boxNode, "y2") - y);
            }
            return new BoundingBoxResponse(x, y, width, height);
        }

        if (hasAny(itemNode, "x", "left", "x1")) {
            x = resolveInt(itemNode, "x", "left", "x1");
            y = resolveInt(itemNode, "y", "top", "y1");
            width = resolveInt(itemNode, "width", "w");
            height = resolveInt(itemNode, "height", "h");
            if (width == 0 && itemNode.has("x2")) {
                width = Math.max(0, resolveInt(itemNode, "x2") - x);
            }
            if (height == 0 && itemNode.has("y2")) {
                height = Math.max(0, resolveInt(itemNode, "y2") - y);
            }
            return new BoundingBoxResponse(x, y, width, height);
        }
        return null;
    }

    private DamageType resolveDamageType(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DamageType.UNKNOWN;
        }
        String normalized = normalize(rawValue);
        return switch (normalized) {
            case "CRACK", "VERTICAL_CRACK", "HORIZONTAL_CRACK", "ALLIGATOR_CRACK" -> DamageType.CRACK;
            case "MARKING_DAMAGE", "MARKING", "LINE" -> DamageType.MARKING_DAMAGE;
            case "ROAD_SPILL", "SPILL" -> DamageType.ROAD_SPILL;
            case "POTHOLE" -> DamageType.POTHOLE;
            default -> DamageType.UNKNOWN;
        };
    }

    private SeverityLevel resolveSeverity(String rawValue, double confidence) {
        if (rawValue != null && !rawValue.isBlank()) {
            String normalized = normalize(rawValue);
            return switch (normalized) {
                case "LOW" -> SeverityLevel.LOW;
                case "HIGH", "SEVERE" -> SeverityLevel.HIGH;
                default -> SeverityLevel.MEDIUM;
            };
        }
        if (confidence >= 0.85D) {
            return SeverityLevel.HIGH;
        }
        if (confidence >= 0.60D) {
            return SeverityLevel.MEDIUM;
        }
        return SeverityLevel.LOW;
    }

    private String resolveSuggestion(DamageType damageType, SeverityLevel severityLevel) {
        if (severityLevel == SeverityLevel.HIGH) {
            return "High priority issue. Please schedule field verification as soon as possible.";
        }
        return switch (damageType) {
            case ROAD_SPILL -> "Recommend notifying the sanitation team to clear the road section quickly.";
            case MARKING_DAMAGE -> "Recommend arranging a repair check for the road marking facilities.";
            case POTHOLE -> "Recommend scheduling pothole repair and reviewing traffic safety risk.";
            case CRACK, UNKNOWN -> "Recommend reviewing the damage range and arranging follow-up handling.";
        };
    }

    private String normalize(String value) {
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private double resolveDouble(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = node.path(fieldName);
            if (!fieldNode.isMissingNode() && fieldNode.isNumber()) {
                return fieldNode.asDouble();
            }
            if (!fieldNode.isMissingNode() && fieldNode.isTextual()) {
                try {
                    return Double.parseDouble(fieldNode.asText());
                } catch (NumberFormatException ignored) {
                    // Continue trying aliases.
                }
            }
        }
        return 0D;
    }

    private int resolveInt(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = node.path(fieldName);
            if (!fieldNode.isMissingNode() && fieldNode.isNumber()) {
                return (int) Math.round(fieldNode.asDouble());
            }
            if (!fieldNode.isMissingNode() && fieldNode.isTextual()) {
                try {
                    return (int) Math.round(Double.parseDouble(fieldNode.asText()));
                } catch (NumberFormatException ignored) {
                    // Continue trying aliases.
                }
            }
        }
        return 0;
    }

    private int roundNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        if (node.isNumber()) {
            return (int) Math.round(node.asDouble());
        }
        if (node.isTextual()) {
            try {
                return (int) Math.round(Double.parseDouble(node.asText()));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = node.path(fieldName);
            if (!fieldNode.isMissingNode() && !fieldNode.isNull()) {
                String value = fieldNode.asText();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private boolean hasAny(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (!node.path(fieldName).isMissingNode() && !node.path(fieldName).isNull()) {
                return true;
            }
        }
        return false;
    }

    private boolean isHttpUri(URI uri) {
        return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("algorithm base url is not configured");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
