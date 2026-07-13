package com.roadcrack.api.request.auditlog;

public class AuditLogPageQuery {
    private String operator;
    private String action;
    private String target;
    private String startDate;
    private String endDate;
    public String getOperator() { return operator; }
    public void setOperator(String v) { this.operator = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getTarget() { return target; }
    public void setTarget(String v) { this.target = v; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String v) { this.startDate = v; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String v) { this.endDate = v; }
}