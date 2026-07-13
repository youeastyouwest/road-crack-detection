package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.response.map.MapDamageTypeRatioResponse;
import com.roadcrack.api.response.map.MapMarkerDetailResponse;
import com.roadcrack.api.response.map.MapMarkerResponse;
import com.roadcrack.api.response.map.MapStatisticsResponse;
import com.roadcrack.api.response.map.MapTrendPointResponse;
import com.roadcrack.dao.entity.DetectionMediaEntity;
import com.roadcrack.dao.entity.DetectionResultEntity;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.entity.WorkOrderEntity;
import com.roadcrack.dao.mapper.DetectionMediaMapper;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionResultMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.WorkOrderMapper;
import com.roadcrack.service.service.MapDataService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbMapDataService implements MapDataService {

    private final DetectionTaskMapper taskMapper;
    private final DetectionResultItemMapper resultItemMapper;
    private final DetectionResultMapper resultMapper;
    private final DetectionMediaMapper mediaMapper;
    private final WorkOrderMapper workOrderMapper;

    public DbMapDataService(DetectionTaskMapper taskMapper,
                            DetectionResultItemMapper resultItemMapper,
                            DetectionResultMapper resultMapper,
                            DetectionMediaMapper mediaMapper,
                            WorkOrderMapper workOrderMapper) {
        this.taskMapper = taskMapper;
        this.resultItemMapper = resultItemMapper;
        this.resultMapper = resultMapper;
        this.mediaMapper = mediaMapper;
        this.workOrderMapper = workOrderMapper;
    }

    @Override
    public List<MapMarkerResponse> listMarkers(DamageType damageType, SeverityLevel severityLevel,
                                               WorkOrderStatus status, Boolean hasWorkOrder,
                                               Boolean onlyWithCoordinates, String keyword) {
        List<MapMarkerResponse> markers = new ArrayList<>();
        List<DetectionTaskEntity> tasks = taskMapper.selectList(null);
        for (DetectionTaskEntity task : tasks) {
            if (task.getLocation() == null || task.getLocation().isBlank()) continue;
            double[] coords = parseLocation(task.getLocation());
            if (coords == null) continue;
            // 查该任务的检测结果（含标注图 URL）
            String annotatedImageUrl = null;
            List<DetectionResultEntity> results = resultMapper.selectList(
                    new LambdaQueryWrapper<DetectionResultEntity>()
                            .eq(DetectionResultEntity::getTaskId, task.getId()));
            if (!results.isEmpty()) {
                annotatedImageUrl = results.get(0).getAnnotatedImageUrl();
            }
            // 查该任务的原始图片 URL
            String fileUrl = null;
            List<DetectionMediaEntity> medias = mediaMapper.selectList(
                    new LambdaQueryWrapper<DetectionMediaEntity>()
                            .eq(DetectionMediaEntity::getTaskId, task.getId()));
            if (!medias.isEmpty()) {
                fileUrl = medias.get(0).getFileUrl();
            }
            List<DetectionResultItemEntity> items = resultItemMapper.selectList(
                    new LambdaQueryWrapper<DetectionResultItemEntity>()
                            .eq(DetectionResultItemEntity::getTaskId, task.getId()));
            for (DetectionResultItemEntity item : items) {
                MapMarkerResponse m = new MapMarkerResponse();
                m.setId(task.getId() * 1000 + (long) markers.size());
                m.setLongitude(coords[0]);
                m.setLatitude(coords[1]);
                m.setDamageType(item.getDamageType() != null ? item.getDamageType() : "UNKNOWN");
                m.setSeverity(item.getSeverityLevel() != null ? item.getSeverityLevel() : "UNKNOWN");
                m.setStatus(task.getStatus() != null ? task.getStatus() : "UNKNOWN");
                m.setTaskId(task.getId());
                m.setAddress(task.getLocation());
                m.setImageBase64(annotatedImageUrl);
                m.setFileUrl(fileUrl);
                markers.add(m);
            }
        }
        return markers;
    }

    @Override
    public MapMarkerDetailResponse getMarkerDetail(Long markerId) { return new MapMarkerDetailResponse(); }

    @Override
    public MapStatisticsResponse getStatistics() {
        MapStatisticsResponse stats = new MapStatisticsResponse();
        List<DetectionTaskEntity> tasks = taskMapper.selectList(null);
        long high = 0, medium = 0, low = 0, total = 0;
        LocalDate today = LocalDate.now();
        long newMarkers = 0;
        for (DetectionTaskEntity task : tasks) {
            List<DetectionResultItemEntity> items = resultItemMapper.selectList(
                    new LambdaQueryWrapper<DetectionResultItemEntity>()
                            .eq(DetectionResultItemEntity::getTaskId, task.getId()));
            for (DetectionResultItemEntity item : items) {
                total++;
                String sev = item.getSeverityLevel();
                if ("HIGH".equals(sev)) high++;
                else if ("MEDIUM".equals(sev)) medium++;
                else low++;
            }
            if (task.getCreatedAt() != null && task.getCreatedAt().toLocalDate().equals(today)) newMarkers++;
        }
        long woCount = workOrderMapper.selectCount(null);
        long closedCount = workOrderMapper.selectCount(
            new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getStatus, "CLOSED"));
        stats.setTotalMarkers(total);
        stats.setNewMarkers(newMarkers);
        stats.setRepairedCount(closedCount);
        stats.setPendingRepair(total - closedCount);
        stats.setHighSeverityCount(high);
        stats.setMediumSeverityCount(medium);
        stats.setLowSeverityCount(low);
        return stats;
    }

    @Override
    public List<MapTrendPointResponse> getTrend(int days) {
        List<MapTrendPointResponse> trend = new ArrayList<>();
        List<DetectionTaskEntity> tasks = taskMapper.selectList(null);
        Map<String, Integer> countByDate = new HashMap<>();
        for (DetectionTaskEntity task : tasks) {
            if (task.getCreatedAt() != null) {
                countByDate.merge(task.getCreatedAt().toLocalDate().toString(), 1, Integer::sum);
            }
        }
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            MapTrendPointResponse p = new MapTrendPointResponse();
            p.setDate(d);
            p.setCount(countByDate.getOrDefault(d.toString(), 0));
            trend.add(p);
        }
        return trend;
    }

    @Override
    public List<MapDamageTypeRatioResponse> getDamageTypeRatios() {
        List<MapDamageTypeRatioResponse> ratios = new ArrayList<>();
        List<DetectionResultItemEntity> allItems = resultItemMapper.selectList(null);
        Map<String, Integer> countByType = new HashMap<>();
        for (DetectionResultItemEntity item : allItems) {
            String type = item.getDamageType() != null ? item.getDamageType() : "UNKNOWN";
            countByType.merge(type, 1, Integer::sum);
        }
        int total = countByType.values().stream().mapToInt(Integer::intValue).sum();
        double t = total > 0 ? total : 1;
        for (Map.Entry<String, Integer> e : countByType.entrySet()) {
            MapDamageTypeRatioResponse r = new MapDamageTypeRatioResponse();
            r.setDamageType(e.getKey());
            r.setCount(e.getValue());
            r.setRatio(e.getValue() / t);
            ratios.add(r);
        }
        return ratios;
    }

    private double[] parseLocation(String location) {
        if (location == null || location.isBlank()) return null;
        try {
            String[] parts = location.trim().split(",");
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        } catch (Exception e) { return null; }
    }
}
