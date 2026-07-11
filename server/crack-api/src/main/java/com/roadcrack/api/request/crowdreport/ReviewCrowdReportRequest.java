package com.roadcrack.api.request.crowdreport;

import jakarta.validation.constraints.NotBlank;

public record ReviewCrowdReportRequest(
        @NotBlank String action,
        String remark
) {
}
