package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.request.report.ReviewRequest;
import com.roadcrack.api.response.report.MaintenanceReportResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.MaintenanceReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ApiResponse<PageResponse<MaintenanceReportResponse>> listReports(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "workOrderId", required = false) Long workOrderId,
            @RequestParam(value = "executor", required = false) String executor) {
        return ApiResponse.success(maintenanceReportService.listReports(page, size, workOrderId, executor));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<MaintenanceReportResponse> getReport(@PathVariable("reportId") Long reportId) {
        return ApiResponse.success(maintenanceReportService.getReport(reportId));
    }

    @PutMapping("/{reportId}/dept-review")
    public ApiResponse<MaintenanceReportResponse> deptReview(@PathVariable("reportId") Long reportId,
                                                              @Valid @RequestBody ReviewRequest request,
                                                              @RequestHeader(value = "X-User-Name", required = false) String userName) {
        return ApiResponse.success(maintenanceReportService.deptReview(reportId, request, userName != null ? userName : "system"));
    }

    @PutMapping("/{reportId}/admin-review")
    public ApiResponse<MaintenanceReportResponse> adminReview(@PathVariable("reportId") Long reportId,
                                                               @Valid @RequestBody ReviewRequest request,
                                                               @RequestHeader(value = "X-User-Name", required = false) String userName) {
        return ApiResponse.success(maintenanceReportService.adminReview(reportId, request, userName != null ? userName : "system"));
    }
}
