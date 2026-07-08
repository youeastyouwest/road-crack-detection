package com.roadcrack.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("detection_result")
public class DetectionResultEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    private String summary;

    @TableField("total_damage_count")
    private Integer totalDamageCount;

    @TableField("highest_severity")
    private String highestSeverity;

    @TableField("avg_confidence")
    private BigDecimal avgConfidence;

    @TableField("generated_work_order_id")
    private Long generatedWorkOrderId;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getTotalDamageCount() {
        return totalDamageCount;
    }

    public void setTotalDamageCount(Integer totalDamageCount) {
        this.totalDamageCount = totalDamageCount;
    }

    public String getHighestSeverity() {
        return highestSeverity;
    }

    public void setHighestSeverity(String highestSeverity) {
        this.highestSeverity = highestSeverity;
    }

    public BigDecimal getAvgConfidence() {
        return avgConfidence;
    }

    public void setAvgConfidence(BigDecimal avgConfidence) {
        this.avgConfidence = avgConfidence;
    }

    public Long getGeneratedWorkOrderId() {
        return generatedWorkOrderId;
    }

    public void setGeneratedWorkOrderId(Long generatedWorkOrderId) {
        this.generatedWorkOrderId = generatedWorkOrderId;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
