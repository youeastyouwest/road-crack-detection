package com.roadcrack.api.response.map;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MapMarkerDetailResponse(
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
        Double confidence,
        String suggestion,
        String snapshotUrl,
        String detectionSummary,
        Long workOrderId,
        String workOrderCode,
        boolean hasWorkOrder,
        boolean generatedWorkOrder,
        WorkOrderStatus status,
        String statusLabel,
        DepartmentCode departmentCode,
        String departmentName,
        String assignee,
        LocalDateTime detectedAt,
        LocalDateTime workOrderCreatedAt,
        LocalDateTime workOrderDueAt
) {
}
