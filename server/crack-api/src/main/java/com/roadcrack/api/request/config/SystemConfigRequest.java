package com.roadcrack.api.request.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SystemConfigRequest {

    @NotBlank(message = "站点名称不能为空")
    @Size(max = 64, message = "站点名称不能超过 64 个字符")
    private String siteName;

    @NotBlank(message = "界面语言不能为空")
    private String language;

    @NotNull(message = "请设置是否允许注册")
    private Boolean allowRegister;

    @Min(value = 5, message = "检测间隔不能小于 5 秒")
    @Max(value = 300, message = "检测间隔不能大于 300 秒")
    private Integer detectionInterval;

    @Min(value = 1, message = "告警阈值不能小于 1")
    @Max(value = 20, message = "告警阈值不能大于 20")
    private Integer alertThreshold;

    @Min(value = 30, message = "数据保留天数不能小于 30 天")
    @Max(value = 720, message = "数据保留天数不能大于 720 天")
    private Integer dataRetentionDays;

    @Min(value = 1, message = "最大上传大小不能小于 1 MB")
    @Max(value = 200, message = "最大上传大小不能大于 200 MB")
    private Integer maxUploadSize;

    @NotNull(message = "请设置邮件通知")
    private Boolean emailNotification;

    @NotNull(message = "请设置短信通知")
    private Boolean smsNotification;

    @NotNull(message = "请设置养护通知")
    private Boolean maintenanceAlert;

    @Min(value = 6, message = "密码最小长度不能小于 6")
    @Max(value = 32, message = "密码最小长度不能大于 32")
    private Integer minPasswordLength;

    @Min(value = 3, message = "登录失败锁定次数不能小于 3")
    @Max(value = 10, message = "登录失败锁定次数不能大于 10")
    private Integer maxLoginAttempts;

    @Min(value = 5, message = "会话超时不能小于 5 分钟")
    @Max(value = 1440, message = "会话超时不能大于 1440 分钟")
    private Integer sessionTimeout;

    @NotNull(message = "请设置验证码登录")
    private Boolean captchaEnabled;

    @NotNull(message = "请设置深色模式")
    private Boolean darkMode;

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getAllowRegister() {
        return allowRegister;
    }

    public void setAllowRegister(Boolean allowRegister) {
        this.allowRegister = allowRegister;
    }

    public Integer getDetectionInterval() {
        return detectionInterval;
    }

    public void setDetectionInterval(Integer detectionInterval) {
        this.detectionInterval = detectionInterval;
    }

    public Integer getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(Integer alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public Integer getDataRetentionDays() {
        return dataRetentionDays;
    }

    public void setDataRetentionDays(Integer dataRetentionDays) {
        this.dataRetentionDays = dataRetentionDays;
    }

    public Integer getMaxUploadSize() {
        return maxUploadSize;
    }

    public void setMaxUploadSize(Integer maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    public Boolean getEmailNotification() {
        return emailNotification;
    }

    public void setEmailNotification(Boolean emailNotification) {
        this.emailNotification = emailNotification;
    }

    public Boolean getSmsNotification() {
        return smsNotification;
    }

    public void setSmsNotification(Boolean smsNotification) {
        this.smsNotification = smsNotification;
    }

    public Boolean getMaintenanceAlert() {
        return maintenanceAlert;
    }

    public void setMaintenanceAlert(Boolean maintenanceAlert) {
        this.maintenanceAlert = maintenanceAlert;
    }

    public Integer getMinPasswordLength() {
        return minPasswordLength;
    }

    public void setMinPasswordLength(Integer minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
    }

    public Integer getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(Integer maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public Integer getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Integer sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public Boolean getCaptchaEnabled() {
        return captchaEnabled;
    }

    public void setCaptchaEnabled(Boolean captchaEnabled) {
        this.captchaEnabled = captchaEnabled;
    }

    public Boolean getDarkMode() {
        return darkMode;
    }

    public void setDarkMode(Boolean darkMode) {
        this.darkMode = darkMode;
    }
}
