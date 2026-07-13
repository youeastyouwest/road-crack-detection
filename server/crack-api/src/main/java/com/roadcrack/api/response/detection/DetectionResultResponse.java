package com.roadcrack.api.response.detection;

import java.time.LocalDateTime;
import java.util.List;

public record DetectionResultResponse(
        Long taskId,
        String summary,
        List<DetectionItemResponse> items,
        Long generatedWorkOrderId,
        LocalDateTime completedAt,
        String imageBase64
) {
}
