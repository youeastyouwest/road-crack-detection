package com.roadcrack.api.request.workorder;

import com.roadcrack.api.enums.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkOrderStatusRequest(
        @NotNull WorkOrderStatus status,
        String note
) {
}
