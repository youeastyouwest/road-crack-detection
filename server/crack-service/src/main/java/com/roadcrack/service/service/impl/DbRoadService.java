package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.response.road.RoadDiseaseSummaryResponse;
import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.dao.entity.DetectionMediaEntity;
import com.roadcrack.dao.entity.DetectionResultEntity;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.entity.RoadEntity;
import com.roadcrack.dao.mapper.DetectionMediaMapper;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionResultMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.RoadMapper;
import com.roadcrack.service.service.RoadService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbRoadService implements RoadService {

    private final RoadMapper roadMapper;
    private final DetectionTaskMapper taskMapper;
    private final DetectionResultItemMapper resultItemMapper;
    private final DetectionResultMapper resultMapper;
    private final DetectionMediaMapper mediaMapper;

    public DbRoadService(RoadMapper roadMapper, DetectionTaskMapper taskMapper,
                         DetectionResultItemMapper resultItemMapper,
                         DetectionResultMapper resultMapper,
                         DetectionMediaMapper mediaMapper) {
        this.roadMapper = roadMapper;
        this.taskMapper = taskMapper;
        this.resultItemMapper = resultItemMapper;
        this.resultMapper = resultMapper;
        this.mediaMapper = mediaMapper;
    }

    @Override
    public PageResponse<RoadResponse> page(int page, int size, String roadName, String district, String roadGrade, String status) {
        LambdaQueryWrapper<RoadEntity> w = new LambdaQueryWrapper<RoadEntity>().orderByDesc(RoadEntity::getCreatedAt);
        if (roadName != null && !roadName.isBlank()) w.like(RoadEntity::getRoadName, roadName);
        if (district != null && !district.isBlank()) w.eq(RoadEntity::getDistrict, district);
        if (roadGrade != null && !roadGrade.isBlank()) w.eq(RoadEntity::getRoadGrade, roadGrade);
        if (status != null && !status.isBlank()) w.eq(RoadEntity::getStatus, status);
        Page<RoadEntity> r = roadMapper.selectPage(new Page<>(page, size), w);
        return new PageResponse<>(r.getRecords().stream().map(this::toRoadRes).collect(Collectors.toList()),
            r.getTotal(), r.getSize(), r.getCurrent(), r.getPages());
    }

    @Override
    public List<RoadResponse> listAll() {
        return roadMapper.selectList(null).stream().map(this::toRoadRes).collect(Collectors.toList());
    }

    @Override
    public List<RoadDiseaseSummaryResponse> getRoadsWithDisease() {
        List<RoadEntity> roads = roadMapper.selectList(null);
        // 查所有有 location 的任务（不再依赖 road_id 关联）
        List<DetectionTaskEntity> allTasks = taskMapper.selectList(
                new LambdaQueryWrapper<DetectionTaskEntity>().isNotNull(DetectionTaskEntity::getLocation));

        // 为每条道路收集病害点和坐标（用于计算 centerLng/centerLat）
        List<RoadDiseaseSummaryResponse> result = new ArrayList<>();
        for (RoadEntity road : roads) {
            int total = 0, high = 0, med = 0, low = 0;
            List<RoadDiseaseSummaryResponse.DiseasePoint> pts = new ArrayList<>();
            double sumLng = 0, sumLat = 0;
            int coordCount = 0;

            for (DetectionTaskEntity task : allTasks) {
                // 如果 task 有 road_id 且匹配当前 road，直接用；否则按坐标就近匹配
                boolean match = false;
                if (task.getRoadId() != null && task.getRoadId().equals(road.getId())) {
                    match = true;
                }
                double[] coords = parseLoc(task.getLocation());
                if (!match && coords != null) {
                    // 按坐标就近匹配：检查这个坐标是否可能属于当前道路
                    // 简单策略：把任务分配给第一条道路（因为 road 表没有坐标信息无法做距离匹配）
                    // 更好的策略：把所有未关联的病害点都放到第一条道路上
                    if (road.getId().equals(roads.get(0).getId())) {
                        match = true;
                    }
                }
                if (!match) continue;

                List<DetectionResultItemEntity> items = resultItemMapper.selectList(
                    new LambdaQueryWrapper<DetectionResultItemEntity>().eq(DetectionResultItemEntity::getTaskId, task.getId()));
                // 查标注图 URL
                String annotatedImageUrl = null;
                List<DetectionResultEntity> results = resultMapper.selectList(
                    new LambdaQueryWrapper<DetectionResultEntity>().eq(DetectionResultEntity::getTaskId, task.getId()));
                if (!results.isEmpty()) {
                    annotatedImageUrl = results.get(0).getAnnotatedImageUrl();
                }
                // 查原始图片 URL
                String fileUrl = null;
                List<DetectionMediaEntity> medias = mediaMapper.selectList(
                    new LambdaQueryWrapper<DetectionMediaEntity>().eq(DetectionMediaEntity::getTaskId, task.getId()));
                if (!medias.isEmpty()) {
                    fileUrl = medias.get(0).getFileUrl();
                }

                for (DetectionResultItemEntity item : items) {
                    total++;
                    if ("HIGH".equals(item.getSeverityLevel())) high++;
                    else if ("MEDIUM".equals(item.getSeverityLevel())) med++;
                    else low++;
                    if (coords != null) {
                        sumLng += coords[0];
                        sumLat += coords[1];
                        coordCount++;
                    }
                    RoadDiseaseSummaryResponse.DiseasePoint dp = new RoadDiseaseSummaryResponse.DiseasePoint(
                        item.getId(), coords != null ? coords[0] : 0.0, coords != null ? coords[1] : 0.0,
                        item.getDamageType(), item.getSeverityLevel(),
                        item.getConfidence() != null ? item.getConfidence().doubleValue() : 0.0,
                        task.getCreatedAt() != null ? task.getCreatedAt().toString() : "");
                    dp.setAddress(task.getLocation());
                    dp.setImageBase64(annotatedImageUrl);
                    dp.setFileUrl(fileUrl);
                    pts.add(dp);
                }
            }
            if (pts.isEmpty()) {
                // 空道路也返回（侧边栏需要显示），但 centerLng/centerLat 为 0
                result.add(new RoadDiseaseSummaryResponse(road.getId(), road.getRoadName(), 0.0, 0.0,
                    new ArrayList<>(), "LOW", 0, 0, 0, 0, pts));
            } else {
                String overall = high > 0 ? "HIGH" : med > 0 ? "MEDIUM" : "LOW";
                double centerLng = coordCount > 0 ? sumLng / coordCount : 0.0;
                double centerLat = coordCount > 0 ? sumLat / coordCount : 0.0;
                result.add(new RoadDiseaseSummaryResponse(road.getId(), road.getRoadName(), centerLng, centerLat,
                    new ArrayList<>(), overall, total, high, med, low, pts));
            }
        }
        return result;
    }

    private RoadResponse toRoadRes(RoadEntity e) {
        RoadResponse r = new RoadResponse();
        r.setId(e.getId()); r.setRoadCode(e.getRoadCode()); r.setRoadName(e.getRoadName());
        r.setRoadGrade(e.getRoadGrade()); r.setDistrict(e.getDistrict());
        r.setStartPoint(e.getStartPoint()); r.setEndPoint(e.getEndPoint());
        r.setLengthKm(e.getLengthKm()); r.setLaneCount(e.getLaneCount());
        r.setSurfaceType(e.getSurfaceType()); r.setBuiltYear(e.getBuiltYear());
        r.setLastMaintained(e.getLastMaintained()); r.setHealthScore(e.getHealthScore());
        r.setDamageLevel(e.getDamageLevel()); r.setLatestDetectionAt(e.getLatestDetectionAt());
        r.setTotalDetectionCount(e.getTotalDetectionCount()); r.setCurrentDamageCount(e.getCurrentDamageCount());
        r.setDepartmentCode(e.getDepartmentCode()); r.setStatus(e.getStatus());
        r.setRemark(e.getRemark()); r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private double[] parseLoc(String loc) {
        if (loc == null || loc.isBlank()) return null;
        try { String[] p = loc.trim().split(","); return new double[]{Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim())}; }
        catch (Exception e) { return null; }
    }
}
