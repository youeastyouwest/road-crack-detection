package com.roadcrack.service.model;

import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;

import java.time.LocalDateTime;

public class DetectionTaskAggregate {

    private final Long id;
    private final String taskCode;
    private final DataSourceType dataSourceType;
    private final String fileName;
    private final String fileUrl;
    private final String location;
    private final String remark;
    private final String submittedBy;
    private DetectionTaskStatus status;
    private String failureReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private DetectionResultResponse result;

    public DetectionTaskAggregate(Long id,
                                  String taskCode,
                                  DataSourceType dataSourceType,
                                  String fileName,
                                  String fileUrl,
                                  String location,
                                  String remark,
                                  String submittedBy,
                                  LocalDateTime createdAt) {
        this.id = id;
        this.taskCode = taskCode;
        this.dataSourceType = dataSourceType;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.location = location;
        this.remark = remark;
        this.submittedBy = submittedBy;
        this.status = DetectionTaskStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void markProcessing() {
        this.status = DetectionTaskStatus.PROCESSING;
        this.failureReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted(DetectionResultResponse result) {
        this.result = result;
        this.status = DetectionTaskStatus.COMPLETED;
        this.failureReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String failureReason) {
        this.status = DetectionTaskStatus.FAILED;
        this.failureReason = failureReason;
        this.updatedAt = LocalDateTime.now();
    }

    public DetectionTaskResponse toResponse() {
        return new DetectionTaskResponse(
                id,
                taskCode,
                dataSourceType,
                fileName,
                fileUrl,
                location,
                remark,
                submittedBy,
                status,
                failureReason,
                createdAt,
                updatedAt,
                result
        );
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getLocation() {
        return location;
    }

    public String getRemark() {
        return remark;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public DetectionTaskStatus getStatus() {
        return status;
    }

    public DetectionResultResponse getResult() {
        return result;
    }
}
