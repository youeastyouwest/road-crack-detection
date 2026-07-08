package com.roadcrack.api.response.detection;

import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;

import java.time.LocalDateTime;

public record DetectionTaskResponse(
        Long id,
        String taskCode,
        DataSourceType dataSourceType,
        String fileName,
        String fileUrl,
        String location,
        String remark,
        String submittedBy,
        DetectionTaskStatus status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        DetectionResultResponse result
) {
}
