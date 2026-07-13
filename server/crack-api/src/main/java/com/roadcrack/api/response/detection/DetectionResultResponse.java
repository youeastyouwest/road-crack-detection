package com.roadcrack.api.response.detection;

import java.time.LocalDateTime;
import java.util.List;

public record DetectionResultResponse(
        Long taskId,
        String summary,
        List<DetectionItemResponse> items,
        Long generatedWorkOrderId,
        LocalDateTime completedAt,
        String imageBase64,
        List<String> keyframeUrls
) {
        public DetectionResultResponse(Long taskId, String summary, List<DetectionItemResponse> items,
                                       Long generatedWorkOrderId, LocalDateTime completedAt, String imageBase64) {
                this(taskId, summary, items, generatedWorkOrderId, completedAt, imageBase64, null);
        }
}
