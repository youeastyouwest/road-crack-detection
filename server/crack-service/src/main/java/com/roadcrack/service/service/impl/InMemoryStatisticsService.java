package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.response.detection.DetectionItemResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.api.response.statistics.CrackTypeDistributionResponse;
import com.roadcrack.api.response.statistics.CrackTypeDistributionResponse.TypeItem;
import com.roadcrack.api.response.statistics.DashboardResponse;
import com.roadcrack.api.response.statistics.DepartmentWorkloadResponse;
import com.roadcrack.api.response.statistics.DepartmentWorkloadResponse.DeptItem;
import com.roadcrack.api.response.statistics.SeverityDistributionResponse;
import com.roadcrack.api.response.statistics.SeverityDistributionResponse.SeverityItem;
import com.roadcrack.api.response.statistics.TrendResponse;
import com.roadcrack.api.response.statistics.TrendResponse.TrendItem;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.DetectionTaskService;
import com.roadcrack.service.service.StatisticsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@org.springframework.context.annotation.Primary
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryStatisticsService implements StatisticsService {

    private final DetectionTaskService detectionTaskService;

    public InMemoryStatisticsService(DetectionTaskService detectionTaskService) {
        this.detectionTaskService = detectionTaskService;
    }

    private List<DetectionTaskResponse> getAllTasks() {
        PageResponse<DetectionTaskResponse> page = detectionTaskService.listTasks(1, Integer.MAX_VALUE, null, null, null, null);
        return page.records();
    }

    @Override
    public DashboardResponse getDashboard() {
        List<DetectionTaskResponse> tasks = getAllTasks();
        long completedTasks = tasks.stream().filter(t -> t.status() == DetectionTaskStatus.COMPLETED).count();
        long todayTasks = tasks.stream().filter(t -> {
            LocalDateTime now = LocalDateTime.now();
            return t.createdAt() != null
                    && t.createdAt().toLocalDate().equals(now.toLocalDate());
        }).count();
        long totalCracks = 0;
        for (DetectionTaskResponse task : tasks) {
            if (task.result() != null && task.result().items() != null) {
                totalCracks += task.result().items().size();
            }
        }
        return new DashboardResponse(
                50,           // totalRoads
                (int) tasks.size(), // monitoredRoads
                (int) todayTasks,   // detectionToday
                0,            // pendingAlerts
                (int) totalCracks,  // totalCracksDetected
                (int) tasks.stream().filter(t -> t.result() != null && t.result().generatedWorkOrderId() != null).count()
        );
    }

    @Override
    public TrendResponse getTrend(int days) {
        List<DetectionTaskResponse> tasks = getAllTasks();
        LocalDate today = LocalDate.now();
        Map<LocalDate, Integer> countByDate = new HashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            countByDate.put(today.minusDays(i), 0);
        }
        for (DetectionTaskResponse task : tasks) {
            if (task.createdAt() != null) {
                LocalDate d = task.createdAt().toLocalDate();
                if (d.isAfter(today.minusDays(days)) || d.isEqual(today.minusDays(days))) {
                    countByDate.merge(d, 1, Integer::sum);
                }
            }
        }
        List<TrendItem> items = countByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new TrendItem(e.getKey().toString(), e.getValue()))
                .collect(Collectors.toList());
        return new TrendResponse(items);
    }

    @Override
    public CrackTypeDistributionResponse getCrackTypeDistribution() {
        List<DetectionTaskResponse> tasks = getAllTasks();
        Map<String, Integer> countByType = new HashMap<>();
        for (DetectionTaskResponse task : tasks) {
            if (task.result() != null && task.result().items() != null) {
                for (DetectionItemResponse item : task.result().items()) {
                    String type = item.damageType() != null ? item.damageType().name() : "UNKNOWN";
                    countByType.merge(type, 1, Integer::sum);
                }
            }
        }
        int total = countByType.values().stream().mapToInt(Integer::intValue).sum();
        double t = total > 0 ? total : 1;
        List<TypeItem> items = countByType.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new TypeItem(e.getKey(), e.getValue(), e.getValue() / t))
                .collect(Collectors.toList());
        return new CrackTypeDistributionResponse(items);
    }

    @Override
    public SeverityDistributionResponse getSeverityDistribution() {
        List<DetectionTaskResponse> tasks = getAllTasks();
        Map<String, Integer> countByLevel = new HashMap<>();
        for (DetectionTaskResponse task : tasks) {
            if (task.result() != null && task.result().items() != null) {
                for (DetectionItemResponse item : task.result().items()) {
                    String level = item.severityLevel() != null ? item.severityLevel().name() : "UNKNOWN";
                    countByLevel.merge(level, 1, Integer::sum);
                }
            }
        }
        int total = countByLevel.values().stream().mapToInt(Integer::intValue).sum();
        double t = total > 0 ? total : 1;
        List<SeverityItem> items = countByLevel.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new SeverityItem(e.getKey(), e.getValue(), e.getValue() / t))
                .collect(Collectors.toList());
        return new SeverityDistributionResponse(items);
    }

    @Override
    public DepartmentWorkloadResponse getDepartmentWorkload() {
        return DepartmentWorkloadResponse.empty();
    }
}