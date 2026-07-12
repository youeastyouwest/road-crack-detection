package com.roadcrack.api.response.road;

import java.util.List;

public class RoadDiseaseSummaryResponse {
    private Long roadId;
    private String roadName;
    private Double centerLng;
    private Double centerLat;
    private List<double[]> pathPoints;
    private int totalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;
    private String overallSeverity;
    private List<DiseasePoint> diseasePoints;

    public RoadDiseaseSummaryResponse() {}

    public RoadDiseaseSummaryResponse(Long roadId, String roadName, Double centerLng, Double centerLat,
                                      List<double[]> pathPoints, String overallSeverity, int totalCount,
                                      int highCount, int mediumCount, int lowCount,
                                      List<DiseasePoint> diseasePoints) {
        this.roadId = roadId;
        this.roadName = roadName;
        this.centerLng = centerLng;
        this.centerLat = centerLat;
        this.pathPoints = pathPoints;
        this.overallSeverity = overallSeverity;
        this.totalCount = totalCount;
        this.highCount = highCount;
        this.mediumCount = mediumCount;
        this.lowCount = lowCount;
        this.diseasePoints = diseasePoints;
    }

    public Long getRoadId() { return roadId; }
    public void setRoadId(Long roadId) { this.roadId = roadId; }
    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }
    public Double getCenterLng() { return centerLng; }
    public void setCenterLng(Double centerLng) { this.centerLng = centerLng; }
    public Double getCenterLat() { return centerLat; }
    public void setCenterLat(Double centerLat) { this.centerLat = centerLat; }
    public List<double[]> getPathPoints() { return pathPoints; }
    public void setPathPoints(List<double[]> pathPoints) { this.pathPoints = pathPoints; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getHighCount() { return highCount; }
    public void setHighCount(int highCount) { this.highCount = highCount; }
    public int getMediumCount() { return mediumCount; }
    public void setMediumCount(int mediumCount) { this.mediumCount = mediumCount; }
    public int getLowCount() { return lowCount; }
    public void setLowCount(int lowCount) { this.lowCount = lowCount; }
    public String getOverallSeverity() { return overallSeverity; }
    public void setOverallSeverity(String overallSeverity) { this.overallSeverity = overallSeverity; }
    public List<DiseasePoint> getDiseasePoints() { return diseasePoints; }
    public void setDiseasePoints(List<DiseasePoint> diseasePoints) { this.diseasePoints = diseasePoints; }

    public static class DiseasePoint {
        private Long taskId;
        private Double lng;
        private Double lat;
        private String damageType;
        private String severity;
        private Double confidence;
        private String detectionTime;
        private String address;
        private String workOrderNo;
        private String imageBase64;
        private String fileUrl;
        private String bbox;

        public DiseasePoint() {}

        public DiseasePoint(Long taskId, Double lng, Double lat, String damageType, String severity,
                            Double confidence, String detectionTime, String address, String workOrderNo) {
            this.taskId = taskId;
            this.lng = lng;
            this.lat = lat;
            this.damageType = damageType;
            this.severity = severity;
            this.confidence = confidence;
            this.detectionTime = detectionTime;
            this.address = address;
            this.workOrderNo = workOrderNo;
        }

        public DiseasePoint(Long taskId, Double lng, Double lat, String damageType, String severity,
                            Double confidence, String detectionTime) {
            this(taskId, lng, lat, damageType, severity, confidence, detectionTime, "", "");
        }

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        public String getDamageType() { return damageType; }
        public void setDamageType(String damageType) { this.damageType = damageType; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public String getDetectionTime() { return detectionTime; }
        public void setDetectionTime(String detectionTime) { this.detectionTime = detectionTime; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getWorkOrderNo() { return workOrderNo; }
        public void setWorkOrderNo(String workOrderNo) { this.workOrderNo = workOrderNo; }
        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        public String getBbox() { return bbox; }
        public void setBbox(String bbox) { this.bbox = bbox; }
    }
}