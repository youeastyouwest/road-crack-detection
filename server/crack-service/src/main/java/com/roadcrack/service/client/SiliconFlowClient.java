package com.roadcrack.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadcrack.api.response.agent.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class SiliconFlowClient {

    private static final Logger log = LoggerFactory.getLogger(SiliconFlowClient.class);
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper;

    public SiliconFlowClient(
            @Value("${crack.agent.siliconflow.api-key:}") String apiKey,
            @Value("${crack.agent.siliconflow.base-url:https://api.siliconflow.cn/v1}") String baseUrl,
            @Value("${crack.agent.siliconflow.model:Qwen/Qwen2.5-7B-Instruct}") String model
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        log.info("SiliconFlowClient initialized: baseUrl={}, model={}, apiKey={}...{}", 
                baseUrl, model, 
                (apiKey != null && apiKey.length() > 8) ? apiKey.substring(0, 8) : "EMPTY",
                (apiKey != null && apiKey.length() > 4) ? apiKey.substring(apiKey.length() - 4) : "EMPTY");
    }

    public ChatResponse chat(String sessionId, String message, String systemPrompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("SiliconFlow API key not configured, using fallback");
            return fallbackChat(sessionId, message);
        }
        try {
            String requestBody = buildRequestBody(message, systemPrompt);
            log.info("Calling SiliconFlow API: model={}, messageLength={}, promptLength={}", 
                    model, message.length(), systemPrompt != null ? systemPrompt.length() : 0);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(12))
                    .build();
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("SiliconFlow API response: status={}, duration={}ms, bodyLength={}", 
                    response.statusCode(), elapsed, response.body().length());
            if (response.statusCode() == 200) {
                return parseResponse(sessionId, message, response.body());
            } else {
                log.warn("SiliconFlow API returned {}: {}", response.statusCode(), response.body());
                return fallbackChat(sessionId, message);
            }
        } catch (Exception e) {
            log.error("SiliconFlow API call failed: {}", e.getMessage());
            return fallbackChat(sessionId, message);
        }
    }

    private String buildRequestBody(String message, String systemPrompt) {
        String sp = (systemPrompt != null && !systemPrompt.isEmpty())
                ? systemPrompt
                : "You are a professional road damage detection AI assistant. Answer in Chinese.";
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", sp),
                            Map.of("role", "user", "content", message)
                    ),
                    "temperature", 0.2,
                    "max_tokens", 900
            );
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.warn("Failed to build SiliconFlow request body with ObjectMapper: {}", e.getMessage());
            return "{"
                    + "\"model\":\"" + jsonEscape(model) + "\","
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":\"" + jsonEscape(sp) + "\"},"
                    + "{\"role\":\"user\",\"content\":\"" + jsonEscape(message) + "\"}"
                    + "],"
                    + "\"temperature\":0.2,"
                    + "\"max_tokens\":900"
                    + "}";
        }
    }

    private ChatResponse parseResponse(String sessionId, String question, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String answer = root.path("choices").path(0).path("message").path("content").asText("");
            if (answer == null || answer.isEmpty()) {
                answer = "AI \u672a\u80fd\u751f\u6210\u6709\u6548\u56de\u7b54\u3002";
            }
            answer = sanitizeAnswer(answer);
            String sid = (sessionId != null) ? sessionId : "session-" + System.currentTimeMillis();
            return new ChatResponse(sid, question, answer, "siliconflow", System.currentTimeMillis());
        } catch (Exception e) {
            log.error("Failed to parse SiliconFlow response: {}", e.getMessage());
            return fallbackChat(sessionId, question);
        }
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String sanitizeAnswer(String answer) {
        String cleaned = answer
                .replace("atk. ###", "###")
                .replace("atk. ##", "##")
                .replace("atk. #", "#")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();

        List<String> cleanedLines = new java.util.ArrayList<>();
        for (String line : cleaned.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase("user")
                    || trimmed.equalsIgnoreCase("assistant")
                    || trimmed.equalsIgnoreCase("system")) {
                continue;
            }
            cleanedLines.add(line);
        }
        return String.join("\n", cleanedLines).trim();
    }

    private ChatResponse fallbackChat(String sessionId, String question) {
        String sid = (sessionId != null) ? sessionId : "session-" + System.currentTimeMillis();
        String answer = "智能问答服务暂时不可用，已切换到本地快速回复。";
        return new ChatResponse(sid, question, answer, "local", System.currentTimeMillis());
    }
}
