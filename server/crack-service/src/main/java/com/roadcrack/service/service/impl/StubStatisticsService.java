package com.roadcrack.service.service.impl;

import com.roadcrack.api.response.statistics.CrackTypeDistributionResponse;
import com.roadcrack.api.response.statistics.DashboardResponse;
import com.roadcrack.api.response.statistics.DepartmentWorkloadResponse;
import com.roadcrack.api.response.statistics.SeverityDistributionResponse;
import com.roadcrack.api.response.statistics.TrendResponse;
import com.roadcrack.service.service.StatisticsService;
import org.springframework.stereotype.Service;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "stub")
public class StubStatisticsService implements StatisticsService {
    @Override
    public DashboardResponse getDashboard() {
        return DashboardResponse.empty();
    }

    @Override
    public TrendResponse getTrend(int days) {
        return TrendResponse.empty();
    }

    @Override
    public CrackTypeDistributionResponse getCrackTypeDistribution() {
        return CrackTypeDistributionResponse.empty();
    }

    @Override
    public SeverityDistributionResponse getSeverityDistribution() {
        return SeverityDistributionResponse.empty();
    }

    @Override
    public DepartmentWorkloadResponse getDepartmentWorkload() {
        return DepartmentWorkloadResponse.empty();
    }
}