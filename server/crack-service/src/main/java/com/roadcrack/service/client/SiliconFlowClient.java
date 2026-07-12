package com.roadcrack.service.client;

import com.roadcrack.api.response.agent.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class SiliconFlowClient {

    private static final Logger log = LoggerFactory.getLogger(SiliconFlowClient.class);
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public SiliconFlowClient(
            @Value("${crack.agent.siliconflow.api-key:}") String apiKey,
            @Value("${crack.agent.siliconflow.base-url:https://api.siliconflow.cn/v1}") String baseUrl,
            @Value("${crack.agent.siliconflow.model:Qwen/Qwen2.5-7B-Instruct}") String model
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
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
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(45))
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
        return "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + jsonEscape(sp) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(message) + "\"}"
                + "],"
                + "\"temperature\":0.7,"
                + "\"max_tokens\":2048"
                + "}";
    }

    private ChatResponse parseResponse(String sessionId, String question, String json) {
        try {
            String answer = extractJsonString(json, "content");
            if (answer == null || answer.isEmpty()) {
                answer = "AI \u672a\u80fd\u751f\u6210\u6709\u6548\u56de\u7b54\u3002";
            }
            String sid = (sessionId != null) ? sessionId : "session-" + System.currentTimeMillis();
            return new ChatResponse(sid, question, answer, "siliconflow", System.currentTimeMillis());
        } catch (Exception e) {
            log.error("Failed to parse SiliconFlow response: {}", e.getMessage());
            return fallbackChat(sessionId, question);
        }
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"' || next == '\\' || next == '/') {
                    sb.append(next);
                    i++;
                } else if (next == 'n') { sb.append('\n'); i++; }
                else if (next == 't') { sb.append('\t'); i++; }
                else if (next == 'r') { sb.append('\r'); i++; }
                else { sb.append(c); }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private ChatResponse fallbackChat(String sessionId, String question) {
        String sid = (sessionId != null) ? sessionId : "session-" + System.currentTimeMillis();
        String answer = "AI \u670d\u52a1\u6682\u4e0d\u53ef\u7528\uff0c\u8bf7\u914d\u7f6e SiliconFlow API Key \u540e\u91cd\u8bd5\u3002";
        return new ChatResponse(sid, question, answer, "local", System.currentTimeMillis());
    }
}
