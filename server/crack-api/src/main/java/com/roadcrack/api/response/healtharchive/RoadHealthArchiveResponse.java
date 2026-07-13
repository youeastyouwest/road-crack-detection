package com.roadcrack.api.response.healtharchive;

import com.roadcrack.api.response.road.RoadResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RoadHealthArchiveResponse(
        Long id,
        Long roadId,
        RoadResponse road,
        LocalDate archiveDate,
        BigDecimal healthScore,
        String damageLevel,
        Integer totalDetectionCount,
        Integer totalDamageCount,
        Integer crackCount,
        Integer potholeCount,
        Integer markingDamageCount,
        Integer roadSpillCount,
        Integer unknownCount,
        Integer severityLowCount,
        Integer severityMediumCount,
        Integer severityHighCount,
        String evaluation,
        String suggestion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
