package com.roadcrack.service.model;

import com.roadcrack.api.response.detection.DetectionItemResponse;

import java.util.List;

public record DetectionAnalysisResult(
        String summary,
        List<DetectionItemResponse> items,
        String imageBase64
) {
        public DetectionAnalysisResult(String summary, List<DetectionItemResponse> items) {
                this(summary, items, null);
        }
}
