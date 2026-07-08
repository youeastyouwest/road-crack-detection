package com.roadcrack.service.model;

import com.roadcrack.api.response.detection.DetectionItemResponse;

import java.util.List;

public record DetectionAnalysisResult(
        String summary,
        List<DetectionItemResponse> items
) {
}
