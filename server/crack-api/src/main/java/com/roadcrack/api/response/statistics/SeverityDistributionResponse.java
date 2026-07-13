package com.roadcrack.api.response.statistics;

import java.util.List;

public record SeverityDistributionResponse(
    List<SeverityItem> items
) {
    public record SeverityItem(String level, int count, double percentage) {}

    public static SeverityDistributionResponse empty() {
        return new SeverityDistributionResponse(List.of());
    }
}