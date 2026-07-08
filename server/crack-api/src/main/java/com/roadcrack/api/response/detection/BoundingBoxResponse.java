package com.roadcrack.api.response.detection;

public record BoundingBoxResponse(
        int x,
        int y,
        int width,
        int height
) {
}
