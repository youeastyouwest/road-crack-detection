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

    private String status = "PENDING";
    private String reviewer;
    private String reviewRemark;
    private LocalDateTime reviewedAt;
    private LocalDateTime updatedAt;

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
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getReportCode() {
        return reportCode;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public String getExecutor() {
        return executor;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewer() {
        return reviewer;
    }

    public String getReviewRemark() {
        return reviewRemark;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void review(String status, String reviewer, String reviewRemark, LocalDateTime reviewedAt) {
        this.status = status;
        this.reviewer = reviewer;
        this.reviewRemark = reviewRemark;
        this.reviewedAt = reviewedAt;
        this.updatedAt = reviewedAt;
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
                status,
                reviewRemark,
                reviewer,
                reviewedAt,
                createdAt
        );
    }
}
