package com.roadcrack.api.response.report;

import java.time.LocalDateTime;

public record MaintenanceReportResponse(
        Long id,
        String reportCode,
        Long workOrderId,
        String executor,
        String beforeImageUrl,
        String afterImageUrl,
        String materials,
        String description,
        LocalDateTime finishedAt,
        String status,
        String reviewRemark,
        String reviewer,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
}
