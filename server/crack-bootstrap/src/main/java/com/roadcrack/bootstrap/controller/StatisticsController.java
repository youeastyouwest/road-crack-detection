package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.response.statistics.CrackTypeDistributionResponse;
import com.roadcrack.api.response.statistics.DashboardResponse;
import com.roadcrack.api.response.statistics.DepartmentWorkloadResponse;
import com.roadcrack.api.response.statistics.SeverityDistributionResponse;
import com.roadcrack.api.response.statistics.TrendResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.service.service.StatisticsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.success(statisticsService.getDashboard());
    }

    @GetMapping("/trend")
    public ApiResponse<java.util.List<TrendResponse.TrendItem>> trend(
            @RequestParam(defaultValue = "30") int days
    ) {
        TrendResponse r = statisticsService.getTrend(days);
        return ApiResponse.success(r.items());
    }

    @GetMapping("/crack-type")
    public ApiResponse<java.util.List<CrackTypeDistributionResponse.TypeItem>> crackType() {
        CrackTypeDistributionResponse r = statisticsService.getCrackTypeDistribution();
        return ApiResponse.success(r.items());
    }

    @GetMapping("/severity")
    public ApiResponse<java.util.List<SeverityDistributionResponse.SeverityItem>> severity() {
        SeverityDistributionResponse r = statisticsService.getSeverityDistribution();
        return ApiResponse.success(r.items());
    }

    @GetMapping("/department-workload")
    public ApiResponse<java.util.List<DepartmentWorkloadResponse.DeptItem>> departmentWorkload() {
        DepartmentWorkloadResponse r = statisticsService.getDepartmentWorkload();
        return ApiResponse.success(r.items());
    }
}