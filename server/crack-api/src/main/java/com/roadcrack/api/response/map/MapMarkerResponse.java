// MapMarkerResponse.java
package com.roadcrack.api.response.map;

public class MapMarkerResponse {
    private Long id;
    private Double longitude;
    private Double latitude;
    private String damageType;
    private String severity;
    private String roadName;
    private String status;
    private Long taskId;
    private String address;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
