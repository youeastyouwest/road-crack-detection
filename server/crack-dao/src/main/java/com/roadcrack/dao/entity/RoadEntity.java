package com.roadcrack.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 道路信息实体
 */
@TableName("road")
public class RoadEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("road_code")
    private String roadCode;

    @TableField("road_name")
    private String roadName;

    @TableField("road_grade")
    private String roadGrade;

    @TableField("district")
    private String district;

    @TableField("start_point")
    private String startPoint;

    @TableField("end_point")
    private String endPoint;

    @TableField("center_lng")
    private BigDecimal centerLng;

    @TableField("center_lat")
    private BigDecimal centerLat;

    @TableField("length_km")
    private BigDecimal lengthKm;

    @TableField("lane_count")
    private Integer laneCount;

    @TableField("surface_type")
    private String surfaceType;

    @TableField("built_year")
    private Integer builtYear;

    @TableField("last_maintained")
    private LocalDateTime lastMaintained;

    @TableField("health_score")
    private BigDecimal healthScore;

    @TableField("damage_level")
    private String damageLevel;

    @TableField("latest_detection_at")
    private LocalDateTime latestDetectionAt;

    @TableField("total_detection_count")
    private Integer totalDetectionCount;

    @TableField("current_damage_count")
    private Integer currentDamageCount;

    @TableField("department_code")
    private String departmentCode;

    @TableField("status")
    private String status;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // === Getters and Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoadCode() { return roadCode; }
    public void setRoadCode(String roadCode) { this.roadCode = roadCode; }

    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }

    public String getRoadGrade() { return roadGrade; }
    public void setRoadGrade(String roadGrade) { this.roadGrade = roadGrade; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getStartPoint() { return startPoint; }
    public void setStartPoint(String startPoint) { this.startPoint = startPoint; }

    public String getEndPoint() { return endPoint; }
    public void setEndPoint(String endPoint) { this.endPoint = endPoint; }

    public BigDecimal getCenterLng() { return centerLng; }
    public void setCenterLng(BigDecimal centerLng) { this.centerLng = centerLng; }

    public BigDecimal getCenterLat() { return centerLat; }
    public void setCenterLat(BigDecimal centerLat) { this.centerLat = centerLat; }

    public BigDecimal getLengthKm() { return lengthKm; }
    public void setLengthKm(BigDecimal lengthKm) { this.lengthKm = lengthKm; }

    public Integer getLaneCount() { return laneCount; }
    public void setLaneCount(Integer laneCount) { this.laneCount = laneCount; }

    public String getSurfaceType() { return surfaceType; }
    public void setSurfaceType(String surfaceType) { this.surfaceType = surfaceType; }

    public Integer getBuiltYear() { return builtYear; }
    public void setBuiltYear(Integer builtYear) { this.builtYear = builtYear; }

    public LocalDateTime getLastMaintained() { return lastMaintained; }
    public void setLastMaintained(LocalDateTime lastMaintained) { this.lastMaintained = lastMaintained; }

    public BigDecimal getHealthScore() { return healthScore; }
    public void setHealthScore(BigDecimal healthScore) { this.healthScore = healthScore; }

    public String getDamageLevel() { return damageLevel; }
    public void setDamageLevel(String damageLevel) { this.damageLevel = damageLevel; }

    public LocalDateTime getLatestDetectionAt() { return latestDetectionAt; }
    public void setLatestDetectionAt(LocalDateTime latestDetectionAt) { this.latestDetectionAt = latestDetectionAt; }

    public Integer getTotalDetectionCount() { return totalDetectionCount; }
    public void setTotalDetectionCount(Integer totalDetectionCount) { this.totalDetectionCount = totalDetectionCount; }

    public Integer getCurrentDamageCount() { return currentDamageCount; }
    public void setCurrentDamageCount(Integer currentDamageCount) { this.currentDamageCount = currentDamageCount; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
