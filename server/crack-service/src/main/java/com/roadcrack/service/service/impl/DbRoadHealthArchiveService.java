package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadcrack.api.request.healtharchive.GenerateRoadHealthArchiveRequest;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveDashboardResponse;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveResponse;
import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.entity.RoadEntity;
import com.roadcrack.dao.entity.RoadHealthArchiveEntity;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.RoadHealthArchiveMapper;
import com.roadcrack.dao.mapper.RoadMapper;
import com.roadcrack.service.service.RoadHealthArchiveService;
import com.roadcrack.service.util.RoadHealthScoreCalculator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbRoadHealthArchiveService implements RoadHealthArchiveService {

    private final RoadHealthArchiveMapper archiveMapper;
    private final RoadMapper roadMapper;
    private final DetectionTaskMapper detectionTaskMapper;
    private final DetectionResultItemMapper resultItemMapper;

    public DbRoadHealthArchiveService(RoadHealthArchiveMapper archiveMapper, RoadMapper roadMapper,
                                       DetectionTaskMapper detectionTaskMapper,
                                       DetectionResultItemMapper resultItemMapper) {
        this.archiveMapper = archiveMapper;
        this.roadMapper = roadMapper;
        this.detectionTaskMapper = detectionTaskMapper;
        this.resultItemMapper = resultItemMapper;
    }

    @Override
    public RoadHealthArchiveResponse generateArchive(GenerateRoadHealthArchiveRequest req) {
        RoadEntity road = roadMapper.selectById(req.roadId());
        if (road == null) return null;

        LocalDate archiveDate = req.archiveDate() != null ? req.archiveDate() : LocalDate.now();

        // 幂等性：同一道路+同一日期，先删除旧档案再生成新档案
        archiveMapper.delete(new LambdaQueryWrapper<RoadHealthArchiveEntity>()
                .eq(RoadHealthArchiveEntity::getRoadId, req.roadId())
                .eq(RoadHealthArchiveEntity::getArchiveDate, archiveDate));

        // 查询该道路下的所有 COMPLETED 检测任务
        List<DetectionTaskEntity> allTasks = detectionTaskMapper.selectList(
            new LambdaQueryWrapper<DetectionTaskEntity>()
                .eq(DetectionTaskEntity::getRoadId, req.roadId())
                .eq(DetectionTaskEntity::getStatus, "COMPLETED"));

        // 同时查询 result_item 中直接关联该道路的数据（兜底：task.road_id 为 null 但 result_item.road_id 有值的情况）
        List<DetectionResultItemEntity> directItems = resultItemMapper.selectList(
            new LambdaQueryWrapper<DetectionResultItemEntity>()
                .eq(DetectionResultItemEntity::getRoadId, req.roadId()));

        // 收集所有属于该道路的病害项（去重：同一 item 不会同时出现在两个来源）
        java.util.Set<Long> seenItemIds = new java.util.HashSet<>();
        List<DetectionResultItemEntity> matchedItems = new ArrayList<>();
        for (DetectionResultItemEntity item : directItems) {
            if (seenItemIds.add(item.getId())) {
                matchedItems.add(item);
            }
        }
        for (DetectionTaskEntity task : allTasks) {
            List<DetectionResultItemEntity> items = resultItemMapper.selectList(
                new LambdaQueryWrapper<DetectionResultItemEntity>()
                    .eq(DetectionResultItemEntity::getTaskId, task.getId()));
            for (DetectionResultItemEntity item : items) {
                if (seenItemIds.add(item.getId())) {
                    matchedItems.add(item);
                }
            }
        }

        // 统计各类型和各严重等级的病害数量
        int crackCount = 0, potholeCount = 0, markingDamageCount = 0, roadSpillCount = 0, unknownCount = 0;
        int severityLowCount = 0, severityMediumCount = 0, severityHighCount = 0;
        int totalDamageCount = matchedItems.size();

        for (DetectionResultItemEntity item : matchedItems) {
            String dt = item.getDamageType();
            if (dt != null) {
                switch (dt) {
                    case "CRACK": case "TRANSVERSE_CRACK": case "LONGITUDINAL_CRACK": case "NET_CRACK":
                        crackCount++; break;
                    case "POTHOLE": potholeCount++; break;
                    case "MARKING_DAMAGE": markingDamageCount++; break;
                    case "ROAD_SPILL": roadSpillCount++; break;
                    default: unknownCount++; break;
                }
            } else {
                unknownCount++;
            }
            String sev = item.getSeverityLevel();
            if ("HIGH".equals(sev)) severityHighCount++;
            else if ("MEDIUM".equals(sev)) severityMediumCount++;
            else severityLowCount++;
        }

        // 动态计算健康评分（而不是读取可能过时的 road.health_score）
        BigDecimal healthScore = RoadHealthScoreCalculator.calculate(road, matchedItems);
        String damageLevel = RoadHealthScoreCalculator.resolveDamageLevel(healthScore);

        // 同步更新道路表的最新健康信息
        road.setHealthScore(healthScore);
        road.setDamageLevel(damageLevel);
        road.setTotalDetectionCount(allTasks.size());
        road.setCurrentDamageCount(totalDamageCount);
        road.setLatestDetectionAt(LocalDateTime.now());
        road.setUpdatedAt(LocalDateTime.now());
        roadMapper.updateById(road);

        RoadHealthArchiveEntity e = new RoadHealthArchiveEntity();
        e.setRoadId(req.roadId());
        e.setArchiveDate(archiveDate);
        e.setHealthScore(healthScore);
        e.setDamageLevel(damageLevel);
        e.setTotalDetectionCount(allTasks.size());
        e.setTotalDamageCount(totalDamageCount);
        e.setCrackCount(crackCount);
        e.setPotholeCount(potholeCount);
        e.setMarkingDamageCount(markingDamageCount);
        e.setRoadSpillCount(roadSpillCount);
        e.setUnknownCount(unknownCount);
        e.setSeverityLowCount(severityLowCount);
        e.setSeverityMediumCount(severityMediumCount);
        e.setSeverityHighCount(severityHighCount);

        // 根据健康评分和病害分布生成评估和建议
        String evaluation = generateEvaluation(healthScore, severityHighCount, severityMediumCount, totalDamageCount);
        String suggestion = generateSuggestion(healthScore, severityHighCount, crackCount, potholeCount);
        e.setEvaluation(evaluation);
        e.setSuggestion(suggestion);

        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        archiveMapper.insert(e);
        return toRes(e);
    }

    private Long extractCommonRoadId(List<DetectionResultItemEntity> items) {
        if (items == null || items.isEmpty()) return null;
        Long common = null;
        for (DetectionResultItemEntity item : items) {
            if (item.getRoadId() != null) {
                if (common == null) {
                    common = item.getRoadId();
                } else if (!common.equals(item.getRoadId())) {
                    return null;
                }
            }
        }
        return common;
    }

    private String generateEvaluation(BigDecimal healthScore, int highCount, int mediumCount, int totalDamage) {
        if (healthScore == null) return "待评估";
        int score = healthScore.intValue();
        if (score >= 85) {
            return "道路状况良好，病害数量较少，建议保持日常巡查频率。";
        } else if (score >= 60) {
            return "道路状况一般，存在" + totalDamage + "处病害（其中严重" + highCount + "处、中等" + mediumCount + "处），需安排养护计划。";
        } else {
            return "道路状况较差，存在" + totalDamage + "处病害（其中严重" + highCount + "处），需立即安排维修。";
        }
    }

    private String generateSuggestion(BigDecimal healthScore, int highCount, int crackCount, int potholeCount) {
        StringBuilder sb = new StringBuilder();
        if (highCount > 0) {
            sb.append("优先处理").append(highCount).append("处严重病害；");
        }
        if (crackCount > 5) {
            sb.append("裂缝数量较多(").append(crackCount).append("处)，建议进行灌缝处理；");
        }
        if (potholeCount > 0) {
            sb.append("存在").append(potholeCount).append("处坑槽，建议及时修补；");
        }
        if (healthScore != null && healthScore.intValue() >= 85) {
            sb.append("继续保持定期巡查。");
        } else if (healthScore != null && healthScore.intValue() < 60) {
            sb.append("建议安排大修或专项养护。");
        } else {
            sb.append("建议制定中期养护计划。");
        }
        return sb.toString();
    }

    @Override
    public PageResponse<RoadHealthArchiveResponse> listArchives(int page, int size, Long roadId, String dl, LocalDate sd, LocalDate ed) {
        // 每个道路只返回最新一条档案，避免同一道路重复行
        LambdaQueryWrapper<RoadHealthArchiveEntity> w = new LambdaQueryWrapper<RoadHealthArchiveEntity>()
                .orderByDesc(RoadHealthArchiveEntity::getRoadId)
                .orderByDesc(RoadHealthArchiveEntity::getCreatedAt);
        if (roadId != null) w.eq(RoadHealthArchiveEntity::getRoadId, roadId);
        if (dl != null && !dl.isBlank()) w.eq(RoadHealthArchiveEntity::getDamageLevel, dl);
        if (sd != null) w.ge(RoadHealthArchiveEntity::getArchiveDate, sd);
        if (ed != null) w.le(RoadHealthArchiveEntity::getArchiveDate, ed);

        // 先查出全部符合条件的，再按道路去重取最新
        List<RoadHealthArchiveEntity> allMatched = archiveMapper.selectList(w);
        Map<Long, RoadHealthArchiveEntity> latestByRoad = new LinkedHashMap<>();
        for (RoadHealthArchiveEntity a : allMatched) {
            latestByRoad.putIfAbsent(a.getRoadId(), a); // 第一条就是最新的（因为按 created_at 降序）
        }
        List<RoadHealthArchiveEntity> deduped = new ArrayList<>(latestByRoad.values());

        // 手动分页
        int total = deduped.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<RoadHealthArchiveEntity> pageRecords = fromIndex < total
                ? deduped.subList(fromIndex, toIndex)
                : new ArrayList<>();

        return new PageResponse<>(pageRecords.stream().map(this::toRes).collect(Collectors.toList()),
            (long) total, size, page, (total + size - 1) / size);
    }

    @Override
    public RoadHealthArchiveResponse getArchive(Long id) {
        RoadHealthArchiveEntity e = archiveMapper.selectById(id);
        return e != null ? toRes(e) : null;
    }

    @Override
    public RoadHealthArchiveDashboardResponse dashboard() {
        List<RoadHealthArchiveEntity> all = archiveMapper.selectList(null);
        List<RoadEntity> roads = roadMapper.selectList(null);

        // 按道路 ID 分组，每组取最新的一条档案（按 created_at 降序），避免重复行干扰统计
        Map<Long, RoadHealthArchiveEntity> latestByRoad = new HashMap<>();
        for (RoadHealthArchiveEntity a : all) {
            RoadHealthArchiveEntity existing = latestByRoad.get(a.getRoadId());
            if (existing == null
                    || (a.getCreatedAt() != null && existing.getCreatedAt() != null
                        && a.getCreatedAt().isAfter(existing.getCreatedAt()))) {
                latestByRoad.put(a.getRoadId(), a);
            }
        }

        long archived = latestByRoad.size();
        int healthy = 0, sub = 0, unhealthy = 0;
        double sumScore = 0;

        for (RoadHealthArchiveEntity a : latestByRoad.values()) {
            if (a.getHealthScore() != null) {
                sumScore += a.getHealthScore().doubleValue();
            }
            String l = a.getDamageLevel();
            if ("HEALTHY".equals(l)) healthy++;
            else if ("SUB_HEALTHY".equals(l)) sub++;
            else unhealthy++;
        }

        double avg = latestByRoad.isEmpty() ? 0 : sumScore / latestByRoad.size();
        return new RoadHealthArchiveDashboardResponse(
            (long) roads.size(), archived, BigDecimal.valueOf(avg).setScale(1, java.math.RoundingMode.HALF_UP),
            (long) healthy, (long) sub, (long) unhealthy
        );
    }

    private RoadHealthArchiveResponse toRes(RoadHealthArchiveEntity e) {
        RoadEntity road = roadMapper.selectById(e.getRoadId());
        RoadResponse roadRes = null;
        if (road != null) {
            roadRes = new RoadResponse();
            roadRes.setId(road.getId());
            roadRes.setRoadCode(road.getRoadCode());
            roadRes.setRoadName(road.getRoadName());
            roadRes.setRoadGrade(road.getRoadGrade());
            roadRes.setDistrict(road.getDistrict());
            roadRes.setStartPoint(road.getStartPoint());
            roadRes.setEndPoint(road.getEndPoint());
            roadRes.setLengthKm(road.getLengthKm());
            roadRes.setLaneCount(road.getLaneCount());
            roadRes.setSurfaceType(road.getSurfaceType());
            roadRes.setBuiltYear(road.getBuiltYear());
            roadRes.setLastMaintained(road.getLastMaintained());
            roadRes.setHealthScore(road.getHealthScore());
            roadRes.setDamageLevel(road.getDamageLevel());
            roadRes.setLatestDetectionAt(road.getLatestDetectionAt());
            roadRes.setTotalDetectionCount(road.getTotalDetectionCount());
            roadRes.setCurrentDamageCount(road.getCurrentDamageCount());
            roadRes.setDepartmentCode(road.getDepartmentCode());
            roadRes.setStatus(road.getStatus());
            roadRes.setRemark(road.getRemark());
            roadRes.setCreatedAt(road.getCreatedAt());
        }
        return new RoadHealthArchiveResponse(
            e.getId(), e.getRoadId(), roadRes, e.getArchiveDate(),
            e.getHealthScore(), e.getDamageLevel(),
            e.getTotalDetectionCount(), e.getTotalDamageCount(),
            e.getCrackCount(), e.getPotholeCount(),
            e.getMarkingDamageCount(), e.getRoadSpillCount(), e.getUnknownCount(),
            e.getSeverityLowCount(), e.getSeverityMediumCount(), e.getSeverityHighCount(),
            e.getEvaluation(), e.getSuggestion(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
