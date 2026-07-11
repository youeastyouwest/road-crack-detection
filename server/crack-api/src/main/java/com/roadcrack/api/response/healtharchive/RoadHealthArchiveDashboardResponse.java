package com.roadcrack.api.response.healtharchive;

import java.math.BigDecimal;

public record RoadHealthArchiveDashboardResponse(
        Long totalRoads,
        Long archivedRoads,
        BigDecimal averageHealthScore,
        Long healthyRoads,
        Long subHealthyRoads,
        Long unhealthyRoads
) {
}
