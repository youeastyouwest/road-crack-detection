package com.roadcrack.api.response.map;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MapMarkerResponse(
        Long id,
        Long taskId,
        String taskCode,
        String location,
        BigDecimal lng,
        BigDecimal lat,
        boolean hasCoordinates,
        DamageType damageType,
        String damageTypeLabel,
        SeverityLevel severityLevel,
        String severityLevelLabel,
        WorkOrderStatus status,
        String statusLabel,
        boolean hasWorkOrder,
        boolean generatedWorkOrder,
        Long workOrderId,
        String workOrderCode,
        DepartmentCode departmentCode,
        String departmentName,
        LocalDateTime detectedAt
) {
}
