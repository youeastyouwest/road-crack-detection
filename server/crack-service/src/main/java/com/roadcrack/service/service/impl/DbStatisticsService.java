package com.roadcrack.service.service.impl;

import com.roadcrack.api.response.statistics.CrackTypeDistributionResponse;
import com.roadcrack.api.response.statistics.DashboardResponse;
import com.roadcrack.api.response.statistics.DepartmentWorkloadResponse;
import com.roadcrack.api.response.statistics.SeverityDistributionResponse;
import com.roadcrack.api.response.statistics.TrendResponse;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.entity.WorkOrderEntity;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.WorkOrderMapper;
import com.roadcrack.service.service.StatisticsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbStatisticsService implements StatisticsService {

    private final DetectionTaskMapper taskMapper;
    private final DetectionResultItemMapper itemMapper;
    private final WorkOrderMapper woMapper;

    public DbStatisticsService(DetectionTaskMapper taskMapper, DetectionResultItemMapper itemMapper, WorkOrderMapper woMapper) {
        this.taskMapper = taskMapper; this.itemMapper = itemMapper; this.woMapper = woMapper;
    }

    @Override
    public DashboardResponse getDashboard() {
        List<DetectionTaskEntity> tasks = taskMapper.selectList(null);
        long woCount = woMapper.selectCount(null);
        int today = 0;
        LocalDate now = LocalDate.now();
        for (DetectionTaskEntity t : tasks) {
            if (t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(now)) today++;
        }
        return new DashboardResponse(tasks.size(), tasks.size(), today, 0, (int) woCount, (int) woCount);
    }

    @Override
    public TrendResponse getTrend(int days) {
        List<DetectionTaskEntity> tasks = taskMapper.selectList(null);
        Map<String, Integer> m = new LinkedHashMap<>();
        for (DetectionTaskEntity t : tasks) {
            if (t.getCreatedAt() != null) m.merge(t.getCreatedAt().toLocalDate().toString(), 1, Integer::sum);
        }
        LocalDate now = LocalDate.now();
        List<TrendResponse.TrendItem> items = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = now.minusDays(i);
            items.add(new TrendResponse.TrendItem(d.toString(), m.getOrDefault(d.toString(), 0)));
        }
        return new TrendResponse(items);
    }

    @Override
    public CrackTypeDistributionResponse getCrackTypeDistribution() {
        List<DetectionResultItemEntity> items = itemMapper.selectList(null);
        Map<String, Integer> m = new HashMap<>();
        for (DetectionResultItemEntity i : items) m.merge(i.getDamageType() != null ? i.getDamageType() : "UNKNOWN", 1, Integer::sum);
        int total = items.size();
        List<CrackTypeDistributionResponse.TypeItem> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            double pct = total > 0 ? (e.getValue() * 100.0 / total) : 0.0;
            result.add(new CrackTypeDistributionResponse.TypeItem(e.getKey(), e.getValue(), pct));
        }
        return new CrackTypeDistributionResponse(result);
    }

    @Override
    public SeverityDistributionResponse getSeverityDistribution() {
        List<DetectionResultItemEntity> items = itemMapper.selectList(null);
        int h = 0, m = 0, l = 0;
        for (DetectionResultItemEntity i : items) {
            String s = i.getSeverityLevel();
            if ("HIGH".equals(s)) h++; else if ("MEDIUM".equals(s)) m++; else l++;
        }
        int total = h + m + l;
        List<SeverityDistributionResponse.SeverityItem> result = new ArrayList<>();
        result.add(new SeverityDistributionResponse.SeverityItem("HIGH", h, total > 0 ? h * 100.0 / total : 0.0));
        result.add(new SeverityDistributionResponse.SeverityItem("MEDIUM", m, total > 0 ? m * 100.0 / total : 0.0));
        result.add(new SeverityDistributionResponse.SeverityItem("LOW", l, total > 0 ? l * 100.0 / total : 0.0));
        return new SeverityDistributionResponse(result);
    }

    @Override
    public DepartmentWorkloadResponse getDepartmentWorkload() {
        List<WorkOrderEntity> orders = woMapper.selectList(null);
        Map<String, int[]> m = new HashMap<>();
        for (WorkOrderEntity o : orders) {
            String dept = o.getDepartmentCode() != null ? o.getDepartmentCode() : "UNKNOWN";
            int[] counts = m.computeIfAbsent(dept, k -> new int[]{0, 0, 0});
            counts[0]++;
            if ("FINISHED".equals(o.getStatus()) || "COMPLETED".equals(o.getStatus())) counts[1]++;
            else counts[2]++;
        }
        List<DepartmentWorkloadResponse.DeptItem> result = new ArrayList<>();
        for (Map.Entry<String, int[]> e : m.entrySet()) {
            result.add(new DepartmentWorkloadResponse.DeptItem(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]));
        }
        return new DepartmentWorkloadResponse(result);
    }
}
