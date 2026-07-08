package com.roadcrack.api.request.workorder;

import com.roadcrack.api.enums.DepartmentCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssignWorkOrderRequest(
        @NotNull DepartmentCode departmentCode,
        @NotBlank String assignee
) {
}
