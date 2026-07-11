package com.roadcrack.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 预警记录实体
 */
@TableName("alert")
public class AlertEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("alert_code")
    private String alertCode;

    @TableField("alert_type")
    private String alertType;

    @TableField("alert_level")
    private String alertLevel;

    private String title;

    private String content;

    @TableField("damage_type")
    private String damageType;

    @TableField("location")
    private String location;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("detection_task_id")
    private Long detectionTaskId;

    @TableField("status")
    private String status;

    @TableField("handled_by")
    private String handledBy;

    @TableField("handled_at")
    private LocalDateTime handledAt;

    @TableField("handle_remark")
    private String handleRemark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // === Getters and Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAlertCode() { return alertCode; }
    public void setAlertCode(String alertCode) { this.alertCode = alertCode; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDamageType() { return damageType; }
    public void setDamageType(String damageType) { this.damageType = damageType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }

    public Long getDetectionTaskId() { return detectionTaskId; }
    public void setDetectionTaskId(Long detectionTaskId) { this.detectionTaskId = detectionTaskId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHandledBy() { return handledBy; }
    public void setHandledBy(String handledBy) { this.handledBy = handledBy; }

    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }

    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
