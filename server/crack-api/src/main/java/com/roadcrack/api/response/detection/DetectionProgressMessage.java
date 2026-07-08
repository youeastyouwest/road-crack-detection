package com.roadcrack.api.response.detection;

import com.roadcrack.api.enums.DetectionTaskStatus;

import java.time.LocalDateTime;

public record DetectionProgressMessage(
        Long taskId,
        DetectionTaskStatus status,
        int progress,
        String message,
        LocalDateTime timestamp
) {
}
