package com.roadcrack.api.response.statistics;

public record DashboardResponse(
    int totalRoads,
    int monitoredRoads,
    int detectionToday,
    int pendingAlerts,
    int totalCracksDetected,
    int totalWorkOrders
) {
    public static DashboardResponse empty() {
        return new DashboardResponse(0, 0, 0, 0, 0, 0);
    }
}