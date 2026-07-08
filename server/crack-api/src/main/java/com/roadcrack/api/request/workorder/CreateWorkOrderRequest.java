package com.roadcrack.api.request.workorder;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkOrderRequest(
        @NotNull Long detectionTaskId,
        @NotBlank String title,
        @NotNull DamageType damageType,
        @NotNull SeverityLevel severityLevel,
        @NotBlank String location,
        @NotNull DepartmentCode departmentCode,
        @NotBlank String evidenceUrl,
        String description
) {
}
