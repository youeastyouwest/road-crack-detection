package com.roadcrack.api.response.workorder;

import com.roadcrack.api.enums.WorkOrderStatus;

import java.time.LocalDateTime;

public record WorkOrderStatusLogResponse(
        WorkOrderStatus fromStatus,
        WorkOrderStatus toStatus,
        String note,
        LocalDateTime operatedAt
) {
}
