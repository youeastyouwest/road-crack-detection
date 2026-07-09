package com.roadcrack.api.response.map;

import java.time.LocalDate;

public record MapTrendPointResponse(
        LocalDate date,
        long count
) {
}
