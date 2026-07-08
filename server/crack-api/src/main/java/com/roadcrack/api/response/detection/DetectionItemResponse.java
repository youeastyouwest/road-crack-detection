package com.roadcrack.api.response.detection;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;

public record DetectionItemResponse(
        DamageType damageType,
        SeverityLevel severityLevel,
        double confidence,
        BoundingBoxResponse boundingBox,
        String suggestion
) {
}
