package com.roadcrack.api.response.map;

public class MapStatisticsResponse {
    private long totalMarkers;
    private long newMarkers;
    private long repairedCount;
    private long pendingRepair;
    private long highSeverityCount;
    private long mediumSeverityCount;
    private long lowSeverityCount;
    public long getTotalMarkers() { return totalMarkers; }
    public void setTotalMarkers(long v) { this.totalMarkers = v; }
    public long getNewMarkers() { return newMarkers; }
    public void setNewMarkers(long v) { this.newMarkers = v; }
    public long getRepairedCount() { return repairedCount; }
    public void setRepairedCount(long v) { this.repairedCount = v; }
    public long getPendingRepair() { return pendingRepair; }
    public void setPendingRepair(long v) { this.pendingRepair = v; }
    public long getHighSeverityCount() { return highSeverityCount; }
    public void setHighSeverityCount(long v) { this.highSeverityCount = v; }
    public long getMediumSeverityCount() { return mediumSeverityCount; }
    public void setMediumSeverityCount(long v) { this.mediumSeverityCount = v; }
    public long getLowSeverityCount() { return lowSeverityCount; }
    public void setLowSeverityCount(long v) { this.lowSeverityCount = v; }
}