package com.roadcrack.api.request.report;

import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull Boolean approved,
        String remark
) {
}
