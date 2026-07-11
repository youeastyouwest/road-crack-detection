package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.response.detection.DetectionItemResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.api.response.map.MapDamageTypeRatioResponse;
import com.roadcrack.api.response.map.MapMarkerDetailResponse;
import com.roadcrack.api.response.map.MapMarkerResponse;
import com.roadcrack.api.response.map.MapStatisticsResponse;
import com.roadcrack.api.response.map.MapTrendPointResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.DetectionTaskService;
import com.roadcrack.service.service.MapDataService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryMapDataService implements MapDataService {

    private final DetectionTaskService detectionTaskService;

    public InMemoryMapDataService(DetectionTaskService detectionTaskService) {
        this.detectionTaskService = detectionTaskService;
    }

    @Override
    public List<MapMarkerResponse> listMarkers(DamageType damageType, SeverityLevel severityLevel,
                                               WorkOrderStatus status, Boolean hasWorkOrder,
                                               Boolean onlyWithCoordinates, String keyword) {
        List<MapMarkerResponse> markers = new ArrayList<>();
        PageResponse<DetectionTaskResponse> page = detectionTaskService.listTasks(1, Integer.MAX_VALUE, null, null, null, null);
        for (DetectionTaskResponse task : page.records()) {
            if (task.location() == null || task.location().isBlank()) continue;
            double[] coords = parseLocation(task.location());
            if (coords == null) continue;
            if (task.result() == null || task.result().items() == null) continue;
            for (DetectionItemResponse item : task.result().items()) {
                MapMarkerResponse m = new MapMarkerResponse();
                m.setId(task.id() * 1000 + (long)markers.size());
                m.setLongitude(coords[0]);
                m.setLatitude(coords[1]);
                m.setDamageType(item.damageType() != null ? item.damageType().name() : "UNKNOWN");
                m.setSeverity(item.severityLevel() != null ? item.severityLevel().name() : "UNKNOWN");
                m.setStatus(task.status().name());
                m.setTaskId(task.id());
                m.setAddress(task.location());
                markers.add(m);
            }
        }
        return markers;
    }

    @Override
    public MapMarkerDetailResponse getMarkerDetail(Long markerId) {
        return new MapMarkerDetailResponse();
    }

    @Override
    public MapStatisticsResponse getStatistics() {
        MapStatisticsResponse stats = new MapStatisticsResponse();
        PageResponse<DetectionTaskResponse> page = detectionTaskService.listTasks(1, Integer.MAX_VALUE, null, null, null, null);
        long high = 0, medium = 0, low = 0, total = 0, repaired = 0, newMarkers = 0;
        LocalDate today = LocalDate.now();
        for (DetectionTaskResponse task : page.records()) {
            if (task.result() != null && task.result().items() != null) {
                for (DetectionItemResponse item : task.result().items()) {
                    total++;
                    if (item.severityLevel() == SeverityLevel.HIGH) high++;
                    else if (item.severityLevel() == SeverityLevel.MEDIUM) medium++;
                    else low++;
                }
            }
            if (task.createdAt() != null && task.createdAt().toLocalDate().equals(today)) {
                newMarkers++;
            }
            if (task.result() != null && task.result().generatedWorkOrderId() != null) {
                repaired++;
            }
        }
        stats.setTotalMarkers(total);
        stats.setNewMarkers(newMarkers);
        stats.setRepairedCount(repaired);
        stats.setPendingRepair(total - repaired);
        stats.setHighSeverityCount(high);
        stats.setMediumSeverityCount(medium);
        stats.setLowSeverityCount(low);
        return stats;
    }

    @Override
    public List<MapTrendPointResponse> getTrend(int days) {
        List<MapTrendPointResponse> trend = new ArrayList<>();
        PageResponse<DetectionTaskResponse> page = detectionTaskService.listTasks(1, Integer.MAX_VALUE, null, null, null, null);
        Map<String, Integer> countByDate = new HashMap<>();
        for (DetectionTaskResponse task : page.records()) {
            if (task.createdAt() != null) {
                String dateKey = task.createdAt().toLocalDate().toString();
                countByDate.merge(dateKey, 1, Integer::sum);
            }
        }
        // Fill days
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
        PageResponse<DetectionTaskResponse> page = detectionTaskService.listTasks(1, Integer.MAX_VALUE, null, null, null, null);
        Map<String, Integer> countByType = new HashMap<>();
        for (DetectionTaskResponse task : page.records()) {
            if (task.result() != null && task.result().items() != null) {
                for (DetectionItemResponse item : task.result().items()) {
                    String type = item.damageType() != null ? item.damageType().name() : "UNKNOWN";
                    countByType.merge(type, 1, Integer::sum);
                }
            }
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
            String trimmed = location.trim();
            if (trimmed.contains(",")) {
                String[] parts = trimmed.split(",");
                return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
            }
        } catch (Exception ignored) {}
        return null;
    }
}
