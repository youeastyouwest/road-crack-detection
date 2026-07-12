package com.roadcrack.service.service;

import com.roadcrack.api.request.config.SystemConfigRequest;
import com.roadcrack.api.response.config.SystemConfigResponse;

public interface SystemConfigService {

    SystemConfigResponse getConfig();

    SystemConfigResponse updateConfig(SystemConfigRequest request);

    boolean isRegisterEnabled();
}
