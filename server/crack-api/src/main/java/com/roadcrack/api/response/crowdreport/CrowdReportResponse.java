package com.roadcrack.api.response.crowdreport;

import com.roadcrack.api.enums.CrowdReportStatus;
import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CrowdReportResponse(
        Long id,
        String reportCode,
        String reporterName,
        String reporterPhone,
        String location,
        BigDecimal lng,
        BigDecimal lat,
        DamageType damageType,
        SeverityLevel severityLevel,
        String description,
        String imageUrl,
        CrowdReportStatus status,
        String remark,
        String reviewedBy,
        LocalDateTime reviewedAt,
        Long detectionTaskId,
        Long workOrderId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
