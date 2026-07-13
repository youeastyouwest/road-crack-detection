package com.roadcrack.api.response.agent;

import java.util.Map;

public class AgentReportResponse {

    private String reportId;

    private String reportType;

    private String title;

    private String summary;

    private String content;

    private Map<String, Object> keyStats;

    private String[] suggestions;

    private long timestamp;

    public AgentReportResponse() {}

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, Object> getKeyStats() { return keyStats; }
    public void setKeyStats(Map<String, Object> keyStats) { this.keyStats = keyStats; }
    public String[] getSuggestions() { return suggestions; }
    public void setSuggestions(String[] suggestions) { this.suggestions = suggestions; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}