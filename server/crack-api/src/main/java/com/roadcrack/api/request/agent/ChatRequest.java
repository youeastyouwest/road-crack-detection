package com.roadcrack.api.request.agent;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {

    @NotBlank(message = "闂涓嶈兘涓虹┖")
    private String message;

    private String sessionId;

    private boolean includeContext = true;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public boolean isIncludeContext() { return includeContext; }
    public void setIncludeContext(boolean includeContext) { this.includeContext = includeContext; }
}