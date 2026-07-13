package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.request.config.SystemConfigRequest;
import com.roadcrack.api.response.config.SystemConfigResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.service.service.SystemConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public ApiResponse<SystemConfigResponse> getConfig() {
        return ApiResponse.success(systemConfigService.getConfig());
    }

    @PutMapping
    public ApiResponse<SystemConfigResponse> updateConfig(@Valid @RequestBody SystemConfigRequest request) {
        return ApiResponse.success(systemConfigService.updateConfig(request));
    }
}
