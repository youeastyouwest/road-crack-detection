package com.roadcrack.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("work_order_flow")
public class WorkOrderFlowEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    @TableField("operation_type")
    private String operationType;

    @TableField("operator_name")
    private String operatorName;

    @TableField("target_department_code")
    private String targetDepartmentCode;

    @TableField("target_assignee")
    private String targetAssignee;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getTargetDepartmentCode() {
        return targetDepartmentCode;
    }

    public void setTargetDepartmentCode(String targetDepartmentCode) {
        this.targetDepartmentCode = targetDepartmentCode;
    }

    public String getTargetAssignee() {
        return targetAssignee;
    }

    public void setTargetAssignee(String targetAssignee) {
        this.targetAssignee = targetAssignee;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
