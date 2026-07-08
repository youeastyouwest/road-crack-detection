package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.response.report.MaintenanceReportResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.MaintenanceReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance-reports")
public class MaintenanceReportController {

    private final MaintenanceReportService maintenanceReportService;

    public MaintenanceReportController(MaintenanceReportService maintenanceReportService) {
        this.maintenanceReportService = maintenanceReportService;
    }

    @PostMapping
    public ApiResponse<MaintenanceReportResponse> createReport(@Valid @RequestBody CreateMaintenanceReportRequest request) {
        return ApiResponse.success(maintenanceReportService.createReport(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<MaintenanceReportResponse>> listReports(@RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            @RequestParam(required = false) Long workOrderId,
                                                                            @RequestParam(required = false) String executor) {
        return ApiResponse.success(maintenanceReportService.listReports(page, size, workOrderId, executor));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<MaintenanceReportResponse> getReport(@PathVariable Long reportId) {
        return ApiResponse.success(maintenanceReportService.getReport(reportId));
    }
}
