package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.response.alert.AlertResponse;
import com.roadcrack.api.response.alert.AlertStatsResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.AlertService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/stats")
    public ApiResponse<AlertStatsResponse> stats() {
        return ApiResponse.success(alertService.stats());
    }

    @PutMapping("/{id}/handle")
    public ApiResponse<AlertResponse> handle(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestAttribute("username") String username,
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> body
    ) {
        String remark = body != null ? body.get("handleRemark") : null;
        return ApiResponse.success(alertService.handle(id, username, remark));
    }
}
