package com.roadcrack.service.model;

import com.roadcrack.api.response.detection.DetectionItemResponse;

import java.util.List;

public record DetectionAnalysisResult(
        String summary,
        List<DetectionItemResponse> items,
        String imageBase64,
        List<String> keyframeUrls
) {
        public DetectionAnalysisResult(String summary, List<DetectionItemResponse> items) {
                this(summary, items, null, null);
        }

        public DetectionAnalysisResult(String summary, List<DetectionItemResponse> items, String imageBase64) {
                this(summary, items, imageBase64, null);
        }

        /**
         * 返回是否携带标注图（可能是 base64 字符串，也可能是 HTTP 路径）。
         */
        public boolean hasImage() {
                return imageBase64 != null && !imageBase64.isBlank();
        }
}
