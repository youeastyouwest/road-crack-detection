package com.roadcrack.api.response.map;

public record MapStatisticsResponse(
        long totalDamageCount,
        long mappedPointCount,
        long highSeverityCount,
        long todayDamageCount,
        long workOrderCount,
        long pendingWorkOrderCount,
        long processingWorkOrderCount,
        long completedWorkOrderCount,
        long closedWorkOrderCount,
        long cancelledWorkOrderCount
) {
}
