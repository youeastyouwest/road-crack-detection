package com.roadcrack.api.response.alert;

/**
 * 告警统计响应
 */
public class AlertStatsResponse {

    private int highCount;
    private int mediumCount;
    private int lowCount;
    private int pendingCount;
    private int handledCount;
    private int totalCount;

    public AlertStatsResponse() {}

    public AlertStatsResponse(int highCount, int mediumCount, int lowCount,
                              int pendingCount, int handledCount, int totalCount) {
        this.highCount = highCount;
        this.mediumCount = mediumCount;
        this.lowCount = lowCount;
        this.pendingCount = pendingCount;
        this.handledCount = handledCount;
        this.totalCount = totalCount;
    }

    public int getHighCount() { return highCount; }
    public void setHighCount(int highCount) { this.highCount = highCount; }

    public int getMediumCount() { return mediumCount; }
    public void setMediumCount(int mediumCount) { this.mediumCount = mediumCount; }

    public int getLowCount() { return lowCount; }
    public void setLowCount(int lowCount) { this.lowCount = lowCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getHandledCount() { return handledCount; }
    public void setHandledCount(int handledCount) { this.handledCount = handledCount; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}
