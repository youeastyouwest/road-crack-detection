package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.request.crowdreport.CreateCrowdReportRequest;
import com.roadcrack.api.request.crowdreport.ReviewCrowdReportRequest;
import com.roadcrack.api.response.crowdreport.CrowdReportResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.CrowdReportService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crowd-reports")
public class CrowdReportController {

    private final CrowdReportService crowdReportService;

    public CrowdReportController(CrowdReportService crowdReportService) {
        this.crowdReportService = crowdReportService;
    }

    @PostMapping
    public ApiResponse<CrowdReportResponse> submit(@Valid @RequestBody CreateCrowdReportRequest request) {
        return ApiResponse.success(crowdReportService.submitReport(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<CrowdReportResponse>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "reporterPhone", required = false) String reporterPhone,
            @RequestParam(value = "location", required = false) String location) {
        return ApiResponse.success(crowdReportService.listReports(page, size, status, reporterPhone, location));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<CrowdReportResponse> get(@PathVariable("reportId") Long reportId) {
        return ApiResponse.success(crowdReportService.getReport(reportId));
    }

    @PutMapping("/{reportId}/review")
    public ApiResponse<CrowdReportResponse> review(
            @PathVariable("reportId") Long reportId,
            @Valid @RequestBody ReviewCrowdReportRequest request,
            @RequestParam(value = "reviewer", defaultValue = "admin") String reviewer) {
        return ApiResponse.success(crowdReportService.reviewReport(reportId, request, reviewer));
    }
}
