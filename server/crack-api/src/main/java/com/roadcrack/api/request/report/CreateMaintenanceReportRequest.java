package com.roadcrack.api.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateMaintenanceReportRequest(
        @NotNull Long workOrderId,
        @NotBlank String executor,
        String beforeImageUrl,
        String afterImageUrl,
        String materials,
        String description,
        @NotNull LocalDateTime finishedAt
) {
}
