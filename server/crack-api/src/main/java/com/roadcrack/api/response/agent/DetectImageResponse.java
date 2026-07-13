package com.roadcrack.api.response.agent;


import java.util.List;
import java.util.Map;

public class DetectImageResponse {

    private String taskId;

    private boolean hasCrack;

    private int numDetections;

    private List<String> crackTypes;

    private Map<String, Object> detectionData;

    private String resultImageBase64;

    private String advice;

    private String dataSource;

    private Long generatedWorkOrderId;

    private String workOrderStatus;

    private String dispatchedDepartment;

    private String dispatchedAssignee;

    private long timestamp;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public boolean isHasCrack() { return hasCrack; }
    public void setHasCrack(boolean hasCrack) { this.hasCrack = hasCrack; }

    public int getNumDetections() { return numDetections; }
    public void setNumDetections(int numDetections) { this.numDetections = numDetections; }

    public List<String> getCrackTypes() { return crackTypes; }
    public void setCrackTypes(List<String> crackTypes) { this.crackTypes = crackTypes; }

    public Map<String, Object> getDetectionData() { return detectionData; }
    public void setDetectionData(Map<String, Object> detectionData) { this.detectionData = detectionData; }

    public String getResultImageBase64() { return resultImageBase64; }
    public void setResultImageBase64(String resultImageBase64) { this.resultImageBase64 = resultImageBase64; }

    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public Long getGeneratedWorkOrderId() { return generatedWorkOrderId; }
    public void setGeneratedWorkOrderId(Long generatedWorkOrderId) { this.generatedWorkOrderId = generatedWorkOrderId; }

    public String getWorkOrderStatus() { return workOrderStatus; }
    public void setWorkOrderStatus(String workOrderStatus) { this.workOrderStatus = workOrderStatus; }

    public String getDispatchedDepartment() { return dispatchedDepartment; }
    public void setDispatchedDepartment(String dispatchedDepartment) { this.dispatchedDepartment = dispatchedDepartment; }

    public String getDispatchedAssignee() { return dispatchedAssignee; }
    public void setDispatchedAssignee(String dispatchedAssignee) { this.dispatchedAssignee = dispatchedAssignee; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}