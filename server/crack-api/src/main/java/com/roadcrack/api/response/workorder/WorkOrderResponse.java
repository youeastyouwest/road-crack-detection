package com.roadcrack.api.response.workorder;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record WorkOrderResponse(
        Long id,
        String workOrderCode,
        Long detectionTaskId,
        String title,
        DamageType damageType,
        SeverityLevel severityLevel,
        String location,
        DepartmentCode departmentCode,
        String assignee,
        WorkOrderStatus status,
        String evidenceUrl,
        String description,
        LocalDateTime dueAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<WorkOrderStatusLogResponse> statusLogs
) {
}
