package com.roadcrack.api.response.config;

import java.io.Serializable;

public class SystemConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String siteName;
    private String siteLogo;
    private String language;
    private Boolean allowRegister;
    private Integer detectionInterval;
    private Integer alertThreshold;
    private Integer dataRetentionDays;
    private Integer maxUploadSize;
    private Boolean emailNotification;
    private Boolean smsNotification;
    private Boolean maintenanceAlert;
    private Integer minPasswordLength;
    private Integer maxLoginAttempts;
    private Integer sessionTimeout;
    private Boolean captchaEnabled;
    private Boolean darkMode;

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteLogo() {
        return siteLogo;
    }

    public void setSiteLogo(String siteLogo) {
        this.siteLogo = siteLogo;
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
