package com.roadcrack.service.service.impl;

import com.roadcrack.api.request.config.SystemConfigRequest;
import com.roadcrack.api.response.config.SystemConfigResponse;
import com.roadcrack.service.service.SystemConfigService;
import org.springframework.stereotype.Service;

@Service
public class InMemorySystemConfigService implements SystemConfigService {

    private final SystemConfigResponse config = createDefaultConfig();

    @Override
    public SystemConfigResponse getConfig() {
        return config;
    }

    @Override
    public SystemConfigResponse updateConfig(SystemConfigRequest request) {
        config.setSiteName(request.getSiteName());
        config.setLanguage(request.getLanguage());
        config.setAllowRegister(request.getAllowRegister());
        config.setDetectionInterval(request.getDetectionInterval());
        config.setAlertThreshold(request.getAlertThreshold());
        config.setDataRetentionDays(request.getDataRetentionDays());
        config.setMaxUploadSize(request.getMaxUploadSize());
        config.setEmailNotification(request.getEmailNotification());
        config.setSmsNotification(request.getSmsNotification());
        config.setMaintenanceAlert(request.getMaintenanceAlert());
        config.setMinPasswordLength(request.getMinPasswordLength());
        config.setMaxLoginAttempts(request.getMaxLoginAttempts());
        config.setSessionTimeout(request.getSessionTimeout());
        config.setCaptchaEnabled(request.getCaptchaEnabled());
        config.setDarkMode(request.getDarkMode());
        return config;
    }

    @Override
    public boolean isRegisterEnabled() {
        Boolean allowRegister = config.getAllowRegister();
        return allowRegister != null && allowRegister;
    }

    private static SystemConfigResponse createDefaultConfig() {
        SystemConfigResponse defaultConfig = new SystemConfigResponse();
        defaultConfig.setSiteName("途安智巡道路裂缝检测系统");
        defaultConfig.setLanguage("zh-CN");
        defaultConfig.setAllowRegister(false);
        defaultConfig.setDetectionInterval(30);
        defaultConfig.setAlertThreshold(3);
        defaultConfig.setDataRetentionDays(180);
        defaultConfig.setMaxUploadSize(50);
        defaultConfig.setEmailNotification(true);
        defaultConfig.setSmsNotification(false);
        defaultConfig.setMaintenanceAlert(true);
        defaultConfig.setMinPasswordLength(6);
        defaultConfig.setMaxLoginAttempts(5);
        defaultConfig.setSessionTimeout(30);
        defaultConfig.setCaptchaEnabled(false);
        defaultConfig.setDarkMode(false);
        return defaultConfig;
    }
}
