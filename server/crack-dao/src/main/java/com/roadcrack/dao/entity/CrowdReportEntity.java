package com.roadcrack.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("crowd_report")
public class CrowdReportEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("report_code")
    private String reportCode;

    @TableField("reporter_name")
    private String reporterName;

    @TableField("reporter_phone")
    private String reporterPhone;

    private String location;

    private BigDecimal lng;

    private BigDecimal lat;

    @TableField("damage_type")
    private String damageType;

    @TableField("severity_level")
    private String severityLevel;

    private String description;

    @TableField("image_url")
    private String imageUrl;

    private String status;

    private String remark;

    @TableField("reviewed_by")
    private String reviewedBy;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("detection_task_id")
    private Long detectionTaskId;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }

    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }

    public String getDamageType() { return damageType; }
    public void setDamageType(String damageType) { this.damageType = damageType; }

    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public Long getDetectionTaskId() { return detectionTaskId; }
    public void setDetectionTaskId(Long detectionTaskId) { this.detectionTaskId = detectionTaskId; }

    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
