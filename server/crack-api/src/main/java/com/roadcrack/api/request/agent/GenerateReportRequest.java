package com.roadcrack.api.request.agent;

import jakarta.validation.constraints.NotBlank;

public class GenerateReportRequest {

    @NotBlank(message = "鎶ュ憡绫诲瀷涓嶈兘涓虹┖")
    private String reportType;

    private String roadSection;

    private String startTime;

    private String endTime;

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getRoadSection() { return roadSection; }
    public void setRoadSection(String roadSection) { this.roadSection = roadSection; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}