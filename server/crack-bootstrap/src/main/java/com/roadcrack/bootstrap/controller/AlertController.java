package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.response.alert.AlertResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.AlertService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AlertResponse>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "alertLevel", required = false) String alertLevel,
            @RequestParam(value = "alertType", required = false) String alertType,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(alertService.page(page, size, alertLevel, alertType, status));
    }

    @GetMapping("/recent")
    public ApiResponse<List<AlertResponse>> recent(
            @RequestParam(value = "count", defaultValue = "5") int count
    ) {
        return ApiResponse.success(alertService.recent(count));
    }
}
