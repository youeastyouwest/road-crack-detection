package com.roadcrack.api.response.alert;


import java.time.LocalDateTime;

public class AlertResponse {

    private Long id;

    private String alertCode;

    private String alertType;

    private String alertLevel;

    private String title;

    private String content;

    private String damageType;

    private String location;

    private Long workOrderId;

    private Long detectionTaskId;

    private String status;

    private String handledBy;

    private LocalDateTime handledAt;

    private String handleRemark;

    private LocalDateTime createdAt;

    public AlertResponse() {}

    public AlertResponse(Long id, String alertCode, String alertType, String alertLevel,
                         String title, String content, String damageType, String location,
                         Long workOrderId, Long detectionTaskId, String status,
                         String handledBy, LocalDateTime handledAt, String handleRemark,
                         LocalDateTime createdAt) {
        this.id = id;
        this.alertCode = alertCode;
        this.alertType = alertType;
        this.alertLevel = alertLevel;
        this.title = title;
        this.content = content;
        this.damageType = damageType;
        this.location = location;
        this.workOrderId = workOrderId;
        this.detectionTaskId = detectionTaskId;
        this.status = status;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
        this.handleRemark = handleRemark;
        this.createdAt = createdAt;
    }

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
}