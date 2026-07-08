package com.roadcrack.service.model;

import com.roadcrack.api.response.report.MaintenanceReportResponse;

import java.time.LocalDateTime;

public class MaintenanceReportAggregate {

    private final Long id;
    private final String reportCode;
    private final Long workOrderId;
    private final String executor;
    private final String beforeImageUrl;
    private final String afterImageUrl;
    private final String materials;
    private final String description;
    private final LocalDateTime finishedAt;
    private final LocalDateTime createdAt;

    public MaintenanceReportAggregate(Long id,
                                      String reportCode,
                                      Long workOrderId,
                                      String executor,
                                      String beforeImageUrl,
                                      String afterImageUrl,
                                      String materials,
                                      String description,
                                      LocalDateTime finishedAt,
                                      LocalDateTime createdAt) {
        this.id = id;
        this.reportCode = reportCode;
        this.workOrderId = workOrderId;
        this.executor = executor;
        this.beforeImageUrl = beforeImageUrl;
        this.afterImageUrl = afterImageUrl;
        this.materials = materials;
        this.description = description;
        this.finishedAt = finishedAt;
        this.createdAt = createdAt;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public String getExecutor() {
        return executor;
    }

    public MaintenanceReportResponse toResponse() {
        return new MaintenanceReportResponse(
                id,
                reportCode,
                workOrderId,
                executor,
                beforeImageUrl,
                afterImageUrl,
                materials,
                description,
                finishedAt,
                createdAt
        );
    }
}
