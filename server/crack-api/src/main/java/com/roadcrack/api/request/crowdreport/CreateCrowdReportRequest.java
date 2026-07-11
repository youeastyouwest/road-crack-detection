package com.roadcrack.api.request.crowdreport;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateCrowdReportRequest(
        String reporterName,
        String reporterPhone,
        @NotBlank String location,
        BigDecimal lng,
        BigDecimal lat,
        DamageType damageType,
        SeverityLevel severityLevel,
        String description,
        @NotBlank String imageUrl
) {
}
