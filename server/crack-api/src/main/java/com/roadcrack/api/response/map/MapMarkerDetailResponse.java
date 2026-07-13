// MapMarkerDetailResponse.java
package com.roadcrack.api.response.map;

import java.time.LocalDateTime;
import java.util.List;

public class MapMarkerDetailResponse {
    private Long id;
    private Double longitude;
    private Double latitude;
    private String damageType;
    private String severity;
    private String roadName;
    private String description;
    private String status;
    private String roadSegment;
    private String departmentName;
    private String distance;
    private Double confidence;
    private LocalDateTime detectedAt;
    private List<String> imageUrls;
    private String workOrderStatus;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // Getters/setters...
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public String getDamageType() { return damageType; }
    public void setDamageType(String damageType) { this.damageType = damageType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRoadSegment() { return roadSegment; }
    public void setRoadSegment(String roadSegment) { this.roadSegment = roadSegment; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getWorkOrderStatus() { return workOrderStatus; }
    public void setWorkOrderStatus(String workOrderStatus) { this.workOrderStatus = workOrderStatus; }
}
