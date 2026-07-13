package com.roadcrack.api.request.healtharchive;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerateRoadHealthArchiveRequest(
        @NotNull Long roadId,
        @NotNull LocalDate archiveDate
) {
}
