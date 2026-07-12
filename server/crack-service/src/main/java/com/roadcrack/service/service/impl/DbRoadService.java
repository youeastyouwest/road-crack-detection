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
import com.roadcrack.dao.entity.WorkOrderEntity;
import com.roadcrack.dao.mapper.DetectionMediaMapper;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionResultMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.RoadMapper;
import com.roadcrack.dao.mapper.WorkOrderMapper;
import com.roadcrack.service.service.RoadService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbRoadService implements RoadService {

    private final RoadMapper roadMapper;
    private final DetectionTaskMapper taskMapper;
    private final DetectionResultItemMapper resultItemMapper;
    private final DetectionResultMapper resultMapper;
    private final DetectionMediaMapper mediaMapper;
    private final WorkOrderMapper workOrderMapper;

    public DbRoadService(RoadMapper roadMapper, DetectionTaskMapper taskMapper,
                         DetectionResultItemMapper resultItemMapper,
                         DetectionResultMapper resultMapper,
                         DetectionMediaMapper mediaMapper,
                         WorkOrderMapper workOrderMapper) {
        this.roadMapper = roadMapper;
        this.taskMapper = taskMapper;
        this.resultItemMapper = resultItemMapper;
        this.resultMapper = resultMapper;
        this.mediaMapper = mediaMapper;
        this.workOrderMapper = workOrderMapper;
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
        Map<Long, RoadEntity> roadMap = new HashMap<>();
        for (RoadEntity road : roads) {
            roadMap.put(road.getId(), road);
        }

        // 查询所有有 location 的任务
        List<DetectionTaskEntity> allTasks = taskMapper.selectList(
                new LambdaQueryWrapper<DetectionTaskEntity>().isNotNull(DetectionTaskEntity::getLocation));

        // 查询所有结果项，用于 road_id 兜底匹配
        List<DetectionResultItemEntity> allItems = resultItemMapper.selectList(null);
        Map<Long, List<DetectionResultItemEntity>> itemsByTaskId = new HashMap<>();
        for (DetectionResultItemEntity item : allItems) {
            itemsByTaskId.computeIfAbsent(item.getTaskId(), k -> new ArrayList<>()).add(item);
        }

        // 按道路 ID 聚合任务。key: roadId（null 表示未知道路）
        Map<Long, List<DetectionTaskEntity>> taskByRoad = new HashMap<>();
        for (DetectionTaskEntity task : allTasks) {
            Long roadId = task.getRoadId();
            if (roadId == null || !roadMap.containsKey(roadId)) {
                // 尝试从该任务的 result_item 中提取共同的 road_id
                List<DetectionResultItemEntity> items = itemsByTaskId.getOrDefault(task.getId(), new ArrayList<>());
                roadId = extractCommonRoadId(items);
            }
            taskByRoad.computeIfAbsent(roadId, k -> new ArrayList<>()).add(task);
        }

        List<RoadDiseaseSummaryResponse> result = new ArrayList<>();
        for (Map.Entry<Long, List<DetectionTaskEntity>> entry : taskByRoad.entrySet()) {
            Long roadId = entry.getKey();
            RoadEntity road = roadMap.get(roadId);
            String roadName = road != null ? road.getRoadName() : "未知道路";
            Double centerLng = 0.0;
            Double centerLat = 0.0;

            int total = 0, high = 0, med = 0, low = 0;
            List<RoadDiseaseSummaryResponse.DiseasePoint> pts = new ArrayList<>();
            double sumLng = 0, sumLat = 0;
            int coordCount = 0;

            for (DetectionTaskEntity task : entry.getValue()) {
                double[] coords = parseLoc(task.getLocation());

                List<DetectionResultItemEntity> items = itemsByTaskId.getOrDefault(task.getId(), new ArrayList<>());
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
                // 查该检测任务关联的工单状态
                String workOrderStatus = null;
                List<WorkOrderEntity> wos = workOrderMapper.selectList(
                    new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getDetectionTaskId, task.getId()));
                if (!wos.isEmpty()) {
                    workOrderStatus = wos.get(wos.size() - 1).getStatus();
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
                    // 使用 result_item 自身的坐标（如果存在），否则回退到 task.location
                    Double itemLng = item.getLng() != null ? item.getLng().doubleValue() : (coords != null ? coords[0] : 0.0);
                    Double itemLat = item.getLat() != null ? item.getLat().doubleValue() : (coords != null ? coords[1] : 0.0);
                    RoadDiseaseSummaryResponse.DiseasePoint dp = new RoadDiseaseSummaryResponse.DiseasePoint(
                        item.getId(), itemLng, itemLat,
                        item.getDamageType(), item.getSeverityLevel(),
                        item.getConfidence() != null ? item.getConfidence().doubleValue() : 0.0,
                        task.getCreatedAt() != null ? task.getCreatedAt().toString() : "");
                    dp.setAddress(task.getLocation());
                    dp.setImageBase64(annotatedImageUrl);
                    dp.setFileUrl(fileUrl);
                    dp.setWorkOrderNo(workOrderStatus);
                    pts.add(dp);
                }
            }

            if (coordCount > 0) {
                centerLng = sumLng / coordCount;
                centerLat = sumLat / coordCount;
            }
            String overall = high > 0 ? "HIGH" : med > 0 ? "MEDIUM" : "LOW";
            result.add(new RoadDiseaseSummaryResponse(roadId != null ? roadId : 0L, roadName,
                centerLng, centerLat, new ArrayList<>(), overall, total, high, med, low, pts));
        }
        return result;
    }

    private Long extractCommonRoadId(List<DetectionResultItemEntity> items) {
        if (items == null || items.isEmpty()) return null;
        Long common = null;
        for (DetectionResultItemEntity item : items) {
            if (item.getRoadId() != null) {
                if (common == null) {
                    common = item.getRoadId();
                } else if (!common.equals(item.getRoadId())) {
                    // 多个结果项对应不同道路，说明数据不一致，返回 null 作为未知道路
                    return null;
                }
            }
        }
        return common;
    }

    @Override
    public List<RoadResponse> listRoadsWithDetections() {
        // 查询所有有检测任务关联的道路 ID（包括直接关联和通过坐标匹配的）
        List<DetectionTaskEntity> allTasks = taskMapper.selectList(
                new LambdaQueryWrapper<DetectionTaskEntity>()
                        .isNotNull(DetectionTaskEntity::getRoadId)
                        .eq(DetectionTaskEntity::getStatus, "COMPLETED"));

        // 收集所有有检测记录的道路 ID
        java.util.Set<Long> roadIdsWithDetection = new java.util.HashSet<>();
        for (DetectionTaskEntity task : allTasks) {
            if (task.getRoadId() != null) {
                roadIdsWithDetection.add(task.getRoadId());
            }
        }

        // 同时查询 result_item 中直接关联的道路 ID
        List<DetectionResultItemEntity> allItems = resultItemMapper.selectList(null);
        for (DetectionResultItemEntity item : allItems) {
            if (item.getRoadId() != null) {
                roadIdsWithDetection.add(item.getRoadId());
            }
        }

        if (roadIdsWithDetection.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询这些道路的信息
        List<RoadEntity> roads = roadMapper.selectList(
                new LambdaQueryWrapper<RoadEntity>()
                        .in(RoadEntity::getId, new ArrayList<>(roadIdsWithDetection)));

        return roads.stream().map(this::toRoadRes).collect(Collectors.toList());
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
