package com.roadcrack.api.response.road;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RoadResponse {

    private Long id;

    private String roadCode;

    private String roadName;

    private String roadGrade;

    private String district;

    private String startPoint;

    private String endPoint;

    private BigDecimal lengthKm;

    private Integer laneCount;

    private String surfaceType;

    private Integer builtYear;

    private LocalDateTime lastMaintained;

    private BigDecimal healthScore;

    private String damageLevel;

    private LocalDateTime latestDetectionAt;

    private Integer totalDetectionCount;

    private Integer currentDamageCount;

    private String departmentCode;

    private String status;

    private String remark;

    private LocalDateTime createdAt;

    public RoadResponse() {}

    public RoadResponse(Long id, String roadCode, String roadName, String roadGrade,
                        String district, String startPoint, String endPoint,
                        BigDecimal lengthKm, Integer laneCount, String surfaceType,
                        Integer builtYear, LocalDateTime lastMaintained,
                        BigDecimal healthScore, String damageLevel,
                        LocalDateTime latestDetectionAt, Integer totalDetectionCount,
                        Integer currentDamageCount, String departmentCode,
                        String status, String remark, LocalDateTime createdAt) {
        this.id = id;
        this.roadCode = roadCode;
        this.roadName = roadName;
        this.roadGrade = roadGrade;
        this.district = district;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.lengthKm = lengthKm;
        this.laneCount = laneCount;
        this.surfaceType = surfaceType;
        this.builtYear = builtYear;
        this.lastMaintained = lastMaintained;
        this.healthScore = healthScore;
        this.damageLevel = damageLevel;
        this.latestDetectionAt = latestDetectionAt;
        this.totalDetectionCount = totalDetectionCount;
        this.currentDamageCount = currentDamageCount;
        this.departmentCode = departmentCode;
        this.status = status;
        this.remark = remark;
        this.createdAt = createdAt;
    }

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
}