package com.roadcrack.service.service.impl;

import com.roadcrack.api.response.statistics.CrackTypeDistributionResponse;
import com.roadcrack.api.response.statistics.DashboardResponse;
import com.roadcrack.api.response.statistics.DepartmentWorkloadResponse;
import com.roadcrack.api.response.statistics.SeverityDistributionResponse;
import com.roadcrack.api.response.statistics.TrendResponse;
import com.roadcrack.dao.entity.AlertEntity;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.entity.RoadEntity;
import com.roadcrack.dao.entity.WorkOrderEntity;
import com.roadcrack.dao.mapper.AlertMapper;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.RoadMapper;
import com.roadcrack.dao.mapper.WorkOrderMapper;
import com.roadcrack.service.service.StatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    private final RoadMapper roadMapper;
    private final AlertMapper alertMapper;

    public DbStatisticsService(DetectionTaskMapper taskMapper, DetectionResultItemMapper itemMapper,
                               WorkOrderMapper woMapper, RoadMapper roadMapper, AlertMapper alertMapper) {
        this.taskMapper = taskMapper; this.itemMapper = itemMapper; this.woMapper = woMapper;
        this.roadMapper = roadMapper; this.alertMapper = alertMapper;
    }

    @Override
    public DashboardResponse getDashboard() {
        // 道路总数 — 查 road 表
        long roadCount = roadMapper.selectCount(null);

        // 在管道路数 — status = 'ACTIVE' 的道路
        long monitoredCount = roadMapper.selectCount(
                new LambdaQueryWrapper<RoadEntity>().eq(RoadEntity::getStatus, "ACTIVE"));

        // 今日检测数 — detection_task 表中 created_at 为今天的
        List<DetectionTaskEntity> tasks = taskMapper.selectList(null);
        int today = 0;
        LocalDate now = LocalDate.now();
        for (DetectionTaskEntity t : tasks) {
            if (t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(now)) today++;
        }

        // 待处理告警数 — alert 表中 status = 'PENDING'
        long pendingAlerts = alertMapper.selectCount(
                new LambdaQueryWrapper<AlertEntity>().eq(AlertEntity::getStatus, "PENDING"));

        // 病害检出总数 — detection_result_item 表总记录数
        long totalCracks = itemMapper.selectCount(null);

        // 工单总数 — work_order 表总记录数
        long woCount = woMapper.selectCount(null);

        return new DashboardResponse(
                (int) roadCount,
                (int) monitoredCount,
                today,
                (int) pendingAlerts,
                (int) totalCracks,
                (int) woCount
        );
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
