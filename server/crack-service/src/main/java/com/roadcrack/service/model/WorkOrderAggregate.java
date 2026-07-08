package com.roadcrack.service.model;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.api.response.workorder.WorkOrderStatusLogResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkOrderAggregate {

    private final Long id;
    private final String workOrderCode;
    private final Long detectionTaskId;
    private final String title;
    private final DamageType damageType;
    private final SeverityLevel severityLevel;
    private final String location;
    private DepartmentCode departmentCode;
    private String assignee;
    private WorkOrderStatus status;
    private final String evidenceUrl;
    private final String description;
    private final LocalDateTime dueAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<WorkOrderStatusLogResponse> statusLogs = new ArrayList<>();

    public WorkOrderAggregate(Long id,
                              String workOrderCode,
                              Long detectionTaskId,
                              String title,
                              DamageType damageType,
                              SeverityLevel severityLevel,
                              String location,
                              DepartmentCode departmentCode,
                              String evidenceUrl,
                              String description,
                              LocalDateTime dueAt,
                              LocalDateTime createdAt) {
        this.id = id;
        this.workOrderCode = workOrderCode;
        this.detectionTaskId = detectionTaskId;
        this.title = title;
        this.damageType = damageType;
        this.severityLevel = severityLevel;
        this.location = location;
        this.departmentCode = departmentCode;
        this.evidenceUrl = evidenceUrl;
        this.description = description;
        this.dueAt = dueAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = WorkOrderStatus.PENDING_ASSIGNMENT;
    }

    public void assign(DepartmentCode departmentCode, String assignee) {
        if (status != WorkOrderStatus.PENDING_ASSIGNMENT) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待分配工单可以执行分配");
        }
        WorkOrderStatus previous = this.status;
        this.departmentCode = departmentCode;
        this.assignee = assignee;
        this.status = WorkOrderStatus.ASSIGNED;
        this.updatedAt = LocalDateTime.now();
        statusLogs.add(new WorkOrderStatusLogResponse(previous, this.status, "工单已分配给 " + assignee, this.updatedAt));
    }

    public void updateStatus(WorkOrderStatus targetStatus, String note) {
        if (targetStatus == WorkOrderStatus.CANCELLED) {
            throw new BusinessException(ResultCode.CONFLICT, "取消工单请调用专用取消接口");
        }
        if (targetStatus == WorkOrderStatus.ASSIGNED && status == WorkOrderStatus.PENDING_ASSIGNMENT) {
            throw new BusinessException(ResultCode.CONFLICT, "待分配工单需先调用分配接口");
        }
        if (targetStatus == WorkOrderStatus.CLOSED) {
            throw new BusinessException(ResultCode.CONFLICT, "工单关闭需通过维修报告流程完成");
        }
        if (!isAllowedTransition(status, targetStatus)) {
            throw new BusinessException(ResultCode.CONFLICT, "非法的工单状态流转: " + status + " -> " + targetStatus);
        }
        changeStatus(targetStatus, note);
    }

    public void cancel(String reason) {
        if (status == WorkOrderStatus.CLOSED || status == WorkOrderStatus.CANCELLED) {
            throw new BusinessException(ResultCode.CONFLICT, "终态工单不可取消");
        }
        changeStatus(WorkOrderStatus.CANCELLED, reason);
    }

    public void closeByReport(String note) {
        if (status != WorkOrderStatus.COMPLETED) {
            throw new BusinessException(ResultCode.CONFLICT, "仅已完成工单可通过维修报告关闭");
        }
        changeStatus(WorkOrderStatus.CLOSED, note);
    }

    private void changeStatus(WorkOrderStatus targetStatus, String note) {
        WorkOrderStatus previous = this.status;
        this.status = targetStatus;
        this.updatedAt = LocalDateTime.now();
        statusLogs.add(new WorkOrderStatusLogResponse(previous, targetStatus, note, this.updatedAt));
    }

    private boolean isAllowedTransition(WorkOrderStatus current, WorkOrderStatus target) {
        return switch (current) {
            case PENDING_ASSIGNMENT -> target == WorkOrderStatus.CANCELLED;
            case ASSIGNED -> target == WorkOrderStatus.IN_PROGRESS || target == WorkOrderStatus.CANCELLED;
            case IN_PROGRESS -> target == WorkOrderStatus.COMPLETED || target == WorkOrderStatus.CANCELLED;
            case COMPLETED -> target == WorkOrderStatus.CANCELLED;
            case CLOSED, CANCELLED -> false;
        };
    }

    public WorkOrderResponse toResponse() {
        return new WorkOrderResponse(
                id,
                workOrderCode,
                detectionTaskId,
                title,
                damageType,
                severityLevel,
                location,
                departmentCode,
                assignee,
                status,
                evidenceUrl,
                description,
                dueAt,
                createdAt,
                updatedAt,
                List.copyOf(statusLogs)
        );
    }

    public Long getId() {
        return id;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public SeverityLevel getSeverityLevel() {
        return severityLevel;
    }

    public DepartmentCode getDepartmentCode() {
        return departmentCode;
    }

    public String getAssignee() {
        return assignee;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }
}
