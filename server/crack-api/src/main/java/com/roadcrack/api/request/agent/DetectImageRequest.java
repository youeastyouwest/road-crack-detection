package com.roadcrack.api.request.agent;


public class DetectImageRequest {

    private String question;

    private boolean generateAdvice = true;

    private boolean autoGenerateWorkOrder = true;

    private boolean autoDispatch = true;

    private Double confidenceThreshold;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public boolean isGenerateAdvice() { return generateAdvice; }
    public void setGenerateAdvice(boolean generateAdvice) { this.generateAdvice = generateAdvice; }

    public boolean isAutoGenerateWorkOrder() { return autoGenerateWorkOrder; }
    public void setAutoGenerateWorkOrder(boolean autoGenerateWorkOrder) { this.autoGenerateWorkOrder = autoGenerateWorkOrder; }

    public boolean isAutoDispatch() { return autoDispatch; }
    public void setAutoDispatch(boolean autoDispatch) { this.autoDispatch = autoDispatch; }

    public Double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(Double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
}