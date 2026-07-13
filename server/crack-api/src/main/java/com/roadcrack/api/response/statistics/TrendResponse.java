package com.roadcrack.api.response.statistics;

import java.util.List;

public record TrendResponse(
    List<TrendItem> items
) {
    public record TrendItem(String date, int count) {}

    public static TrendResponse empty() {
        return new TrendResponse(List.of());
    }
}