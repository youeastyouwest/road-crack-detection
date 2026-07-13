package com.roadcrack.service.service;

import com.roadcrack.api.response.statistics.CrackTypeDistributionResponse;
import com.roadcrack.api.response.statistics.DashboardResponse;
import com.roadcrack.api.response.statistics.DepartmentWorkloadResponse;
import com.roadcrack.api.response.statistics.SeverityDistributionResponse;
import com.roadcrack.api.response.statistics.TrendResponse;

public interface StatisticsService {
    DashboardResponse getDashboard();
    TrendResponse getTrend(int days);
    CrackTypeDistributionResponse getCrackTypeDistribution();
    SeverityDistributionResponse getSeverityDistribution();
    DepartmentWorkloadResponse getDepartmentWorkload();
}