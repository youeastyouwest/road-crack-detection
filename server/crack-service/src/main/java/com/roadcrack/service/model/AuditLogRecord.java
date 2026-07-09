package com.roadcrack.service.model;

import java.time.LocalDateTime;

public class AuditLogRecord {

    private Long userId;
    private String username;
    private String module;
    private String action;
    private String description;
    private String ip;
    private String params;
    private Integer status = 1;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime createTime;

    public static AuditLogRecord success(String module, String action, String description) {
        return new AuditLogRecord()
                .setModule(module)
                .setAction(action)
                .setDescription(description)
                .setStatus(1);
    }

    public static AuditLogRecord failure(String module, String action, String description, String errorMsg) {
        return new AuditLogRecord()
                .setModule(module)
                .setAction(action)
                .setDescription(description)
                .setStatus(0)
                .setErrorMsg(errorMsg);
    }

    public Long getUserId() {
        return userId;
    }

    public AuditLogRecord setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public AuditLogRecord setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getModule() {
        return module;
    }

    public AuditLogRecord setModule(String module) {
        this.module = module;
        return this;
    }

    public String getAction() {
        return action;
    }

    public AuditLogRecord setAction(String action) {
        this.action = action;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public AuditLogRecord setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public AuditLogRecord setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public String getParams() {
        return params;
    }

    public AuditLogRecord setParams(String params) {
        this.params = params;
        return this;
    }

    public Integer getStatus() {
        return status;
    }

    public AuditLogRecord setStatus(Integer status) {
        this.status = status;
        return this;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public AuditLogRecord setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }

    public Long getCostTime() {
        return costTime;
    }

    public AuditLogRecord setCostTime(Long costTime) {
        this.costTime = costTime;
        return this;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public AuditLogRecord setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }
}
