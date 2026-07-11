package com.roadcrack.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("road_health_archive")
public class RoadHealthArchiveEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("road_id")
    private Long roadId;

    @TableField("archive_date")
    private LocalDate archiveDate;

    @TableField("health_score")
    private BigDecimal healthScore;

    @TableField("damage_level")
    private String damageLevel;

    @TableField("total_detection_count")
    private Integer totalDetectionCount;

    @TableField("total_damage_count")
    private Integer totalDamageCount;

    @TableField("crack_count")
    private Integer crackCount;

    @TableField("pothole_count")
    private Integer potholeCount;

    @TableField("marking_damage_count")
    private Integer markingDamageCount;

    @TableField("road_spill_count")
    private Integer roadSpillCount;

    @TableField("unknown_count")
    private Integer unknownCount;

    @TableField("severity_low_count")
    private Integer severityLowCount;

    @TableField("severity_medium_count")
    private Integer severityMediumCount;

    @TableField("severity_high_count")
    private Integer severityHighCount;

    private String evaluation;

    private String suggestion;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoadId() { return roadId; }
    public void setRoadId(Long roadId) { this.roadId = roadId; }

    public LocalDate getArchiveDate() { return archiveDate; }
    public void setArchiveDate(LocalDate archiveDate) { this.archiveDate = archiveDate; }

    public BigDecimal getHealthScore() { return healthScore; }
    public void setHealthScore(BigDecimal healthScore) { this.healthScore = healthScore; }

    public String getDamageLevel() { return damageLevel; }
    public void setDamageLevel(String damageLevel) { this.damageLevel = damageLevel; }

    public Integer getTotalDetectionCount() { return totalDetectionCount; }
    public void setTotalDetectionCount(Integer totalDetectionCount) { this.totalDetectionCount = totalDetectionCount; }

    public Integer getTotalDamageCount() { return totalDamageCount; }
    public void setTotalDamageCount(Integer totalDamageCount) { this.totalDamageCount = totalDamageCount; }

    public Integer getCrackCount() { return crackCount; }
    public void setCrackCount(Integer crackCount) { this.crackCount = crackCount; }

    public Integer getPotholeCount() { return potholeCount; }
    public void setPotholeCount(Integer potholeCount) { this.potholeCount = potholeCount; }

    public Integer getMarkingDamageCount() { return markingDamageCount; }
    public void setMarkingDamageCount(Integer markingDamageCount) { this.markingDamageCount = markingDamageCount; }

    public Integer getRoadSpillCount() { return roadSpillCount; }
    public void setRoadSpillCount(Integer roadSpillCount) { this.roadSpillCount = roadSpillCount; }

    public Integer getUnknownCount() { return unknownCount; }
    public void setUnknownCount(Integer unknownCount) { this.unknownCount = unknownCount; }

    public Integer getSeverityLowCount() { return severityLowCount; }
    public void setSeverityLowCount(Integer severityLowCount) { this.severityLowCount = severityLowCount; }

    public Integer getSeverityMediumCount() { return severityMediumCount; }
    public void setSeverityMediumCount(Integer severityMediumCount) { this.severityMediumCount = severityMediumCount; }

    public Integer getSeverityHighCount() { return severityHighCount; }
    public void setSeverityHighCount(Integer severityHighCount) { this.severityHighCount = severityHighCount; }

    public String getEvaluation() { return evaluation; }
    public void setEvaluation(String evaluation) { this.evaluation = evaluation; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
