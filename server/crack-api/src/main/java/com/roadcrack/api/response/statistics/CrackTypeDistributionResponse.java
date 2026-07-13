package com.roadcrack.api.response.statistics;

import java.util.List;

public record CrackTypeDistributionResponse(
    List<TypeItem> items
) {
    public record TypeItem(String type, int count, double percentage) {}

    public static CrackTypeDistributionResponse empty() {
        return new CrackTypeDistributionResponse(List.of());
    }
}