package com.roadcrack.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("detection_result_item")
public class DetectionResultItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("result_id")
    private Long resultId;

    @TableField("task_id")
    private Long taskId;

    @TableField("media_id")
    private Long mediaId;

    @TableField("road_id")
    private Long roadId;

    @TableField("road_segment_id")
    private Long roadSegmentId;

    @TableField("damage_type")
    private String damageType;

    @TableField("severity_level")
    private String severityLevel;

    private BigDecimal confidence;

    @TableField("bbox_x")
    private Integer bboxX;

    @TableField("bbox_y")
    private Integer bboxY;

    @TableField("bbox_width")
    private Integer bboxWidth;

    @TableField("bbox_height")
    private Integer bboxHeight;

    private BigDecimal lng;

    private BigDecimal lat;

    @TableField("length_mm")
    private BigDecimal lengthMm;

    @TableField("width_mm")
    private BigDecimal widthMm;

    @TableField("area_mm2")
    private BigDecimal areaMm2;

    @TableField("snapshot_url")
    private String snapshotUrl;

    private String suggestion;

    @TableField("detected_at")
    private LocalDateTime detectedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public Long getRoadId() {
        return roadId;
    }

    public void setRoadId(Long roadId) {
        this.roadId = roadId;
    }

    public Long getRoadSegmentId() {
        return roadSegmentId;
    }

    public void setRoadSegmentId(Long roadSegmentId) {
        this.roadSegmentId = roadSegmentId;
    }

    public String getDamageType() {
        return damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Integer getBboxX() {
        return bboxX;
    }

    public void setBboxX(Integer bboxX) {
        this.bboxX = bboxX;
    }

    public Integer getBboxY() {
        return bboxY;
    }

    public void setBboxY(Integer bboxY) {
        this.bboxY = bboxY;
    }

    public Integer getBboxWidth() {
        return bboxWidth;
    }

    public void setBboxWidth(Integer bboxWidth) {
        this.bboxWidth = bboxWidth;
    }

    public Integer getBboxHeight() {
        return bboxHeight;
    }

    public void setBboxHeight(Integer bboxHeight) {
        this.bboxHeight = bboxHeight;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public BigDecimal getLengthMm() {
        return lengthMm;
    }

    public void setLengthMm(BigDecimal lengthMm) {
        this.lengthMm = lengthMm;
    }

    public BigDecimal getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(BigDecimal widthMm) {
        this.widthMm = widthMm;
    }

    public BigDecimal getAreaMm2() {
        return areaMm2;
    }

    public void setAreaMm2(BigDecimal areaMm2) {
        this.areaMm2 = areaMm2;
    }

    public String getSnapshotUrl() {
        return snapshotUrl;
    }

    public void setSnapshotUrl(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
