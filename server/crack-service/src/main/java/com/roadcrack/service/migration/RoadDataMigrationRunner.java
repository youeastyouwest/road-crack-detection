package com.roadcrack.service.migration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.entity.RoadEntity;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.RoadMapper;
import com.roadcrack.service.client.AmapGeocodeClient;
import com.roadcrack.service.util.RoadHealthScoreCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据回填脚本：将历史检测任务的 road_id 从旧 Haversine 匹配结果
 * 更新为高德逆地理编码返回的真实道路名。
 *
 * 激活方式：启动时添加 --spring.profiles.active=data-migration
 *
 * 执行流程：
 * 1. 遍历所有检测任务（不限状态），根据 location 调用高德逆地理编码
 * 2. 在 road 表中按道路名查找已有记录，不存在则自动创建
 * 3. 更新 task.road_id 和该 task 下所有 result_item 的 road_id
 * 4. 统计旧道路（手动录入的6条标准道路）上不再有病害的，重置其 current_damage_count 和 health_score
 * 5. 按新归属重新计算所有道路的 current_damage_count、health_score、damage_level
 * 6. 输出迁移报告
 */
@Component
@Profile("data-migration")
public class RoadDataMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoadDataMigrationRunner.class);

    private final DetectionTaskMapper taskMapper;
    private final DetectionResultItemMapper itemMapper;
    private final RoadMapper roadMapper;
    private final AmapGeocodeClient amapGeocodeClient;

    public RoadDataMigrationRunner(DetectionTaskMapper taskMapper,
                                   DetectionResultItemMapper itemMapper,
                                   RoadMapper roadMapper,
                                   AmapGeocodeClient amapGeocodeClient) {
        this.taskMapper = taskMapper;
        this.itemMapper = itemMapper;
        this.roadMapper = roadMapper;
        this.amapGeocodeClient = amapGeocodeClient;
    }

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("  道路数据回填脚本开始执行");
        log.info("========================================");

        if (!amapGeocodeClient.isConfigured()) {
            log.error("高德 API Key 未配置！请在 application.yml 中设置 amap.api-key 后重新运行。");
            log.error("脚本退出。");
            return;
        }

        // === 步骤 1：收集所有有 location 的检测任务 ===
        List<DetectionTaskEntity> allTasks = taskMapper.selectList(
                new LambdaQueryWrapper<DetectionTaskEntity>()
                        .isNotNull(DetectionTaskEntity::getLocation)
                        .ne(DetectionTaskEntity::getLocation, ""));

        log.info("找到 {} 个有位置坐标的检测任务", allTasks.size());

        // 记录每个 task 旧 road_id → 新 road_id 的映射
        Map<Long, Long> taskRoadMigration = new LinkedHashMap<>();
        // 记录所有被引用的新 road_id（用于后续重算统计）
        Set<Long> allNewRoadIds = new HashSet<>();
        // 记录所有被替换掉的旧 road_id（可能需要清理）
        Set<Long> oldRoadIds = new HashSet<>();

        int geocodeSuccess = 0;
        int geocodeFailed = 0;
        int roadCreated = 0;
        int roadReused = 0;
        int noChange = 0;

        for (DetectionTaskEntity task : allTasks) {
            String location = task.getLocation();
            Long oldRoadId = task.getRoadId();
            if (oldRoadId != null) {
                oldRoadIds.add(oldRoadId);
            }

            // 解析坐标
            double[] coords = parseLocation(location);
            if (coords == null) {
                log.warn("任务 {} location 格式异常: {}", task.getId(), location);
                continue;
            }

            // 调用高德逆地理编码
            String roadName = amapGeocodeClient.reverseGeocode(coords[0], coords[1]);
            if (roadName.isBlank()) {
                geocodeFailed++;
                log.debug("任务 {} 逆地理编码未返回道路名 (lng={}, lat={})", task.getId(), coords[0], coords[1]);
                // 回退：创建未命名道路
                roadName = String.format("未命名道路-%.4f,%.4f", coords[0], coords[1]);
            } else {
                geocodeSuccess++;
            }

            // 查找或创建道路
            RoadEntity road = roadMapper.selectOne(
                    new LambdaQueryWrapper<RoadEntity>()
                            .eq(RoadEntity::getRoadName, roadName));

            Long newRoadId;
            if (road != null) {
                newRoadId = road.getId();
                roadReused++;
            } else {
                newRoadId = createAutoRoad(roadName, coords[0], coords[1]);
                roadCreated++;
            }

            allNewRoadIds.add(newRoadId);

            // 如果 road_id 变了，记录并更新
            if (!Objects.equals(oldRoadId, newRoadId)) {
                taskRoadMigration.put(task.getId(), newRoadId);

                // 更新 task.road_id
                taskMapper.update(null,
                        new LambdaUpdateWrapper<DetectionTaskEntity>()
                                .eq(DetectionTaskEntity::getId, task.getId())
                                .set(DetectionTaskEntity::getRoadId, newRoadId));

                // 同时更新该 task 下所有 result_item 的 road_id
                itemMapper.update(null,
                        new LambdaUpdateWrapper<DetectionResultItemEntity>()
                                .eq(DetectionResultItemEntity::getTaskId, task.getId())
                                .set(DetectionResultItemEntity::getRoadId, newRoadId));

                log.debug("任务 {} road_id: {} → {} (道路: {})", task.getId(), oldRoadId, newRoadId, roadName);
            } else {
                noChange++;
            }
        }

        log.info("--- 步骤 1 完成 ---");
        log.info("逆地理编码成功: {}, 失败/回退: {}", geocodeSuccess, geocodeFailed);
        log.info("道路记录复用: {}, 新建: {}", roadReused, roadCreated);
        log.info("road_id 变更: {} 个任务, 无变化: {} 个", taskRoadMigration.size(), noChange);

        // === 步骤 2：重新计算所有受影响道路的统计 ===
        log.info("--- 步骤 2：重新计算道路统计 ---");

        // 所有需要重算的道路 = 旧道路 + 新道路
        Set<Long> allAffectedRoadIds = new HashSet<>();
        allAffectedRoadIds.addAll(oldRoadIds);
        allAffectedRoadIds.addAll(allNewRoadIds);

        int roadRecalcCount = 0;
        for (Long roadId : allAffectedRoadIds) {
            recalcRoadStats(roadId);
            roadRecalcCount++;
        }

        log.info("重新计算了 {} 条道路的统计数据", roadRecalcCount);

        // === 步骤 3：输出迁移报告 ===
        log.info("========================================");
        log.info("  数据回填脚本执行完毕");
        log.info("========================================");
        log.info("总任务数: {}", allTasks.size());
        log.info("逆地理编码成功: {}", geocodeSuccess);
        log.info("逆地理编码失败（自动创建未命名道路）: {}", geocodeFailed);
        log.info("新建道路记录: {}", roadCreated);
        log.info("复用已有道路: {}", roadReused);
        log.info("road_id 发生变更的任务数: {}", taskRoadMigration.size());
        log.info("road_id 无变化的任务数: {}", noChange);
        log.info("涉及的道路总数: {}", allAffectedRoadIds.size());

        // 列出新建的道路
        if (roadCreated > 0) {
            log.info("--- 新建的道路 ---");
            List<RoadEntity> newRoads = roadMapper.selectList(
                    new LambdaQueryWrapper<RoadEntity>()
                            .likeRight(RoadEntity::getRoadCode, "AUTO-"));
            for (RoadEntity r : newRoads) {
                log.info("  ID={}, 名称={}, 编码={}", r.getId(), r.getRoadName(), r.getRoadCode());
            }
        }

        // 列出当前所有道路的统计
        log.info("--- 所有道路当前统计 ---");
        List<RoadEntity> allRoads = roadMapper.selectList(null);
        for (RoadEntity r : allRoads) {
            log.info("  {} | 病害数={} | 评分={} | 等级={} | 状态={}",
                    r.getRoadName(),
                    r.getCurrentDamageCount(),
                    r.getHealthScore(),
                    r.getDamageLevel(),
                    r.getStatus());
        }

        log.info("脚本执行完毕，系统将退出。");
        // Spring Boot 会在 CommandLineRunner 完成后自动退出（因为 profile=data-migration 不会启动 Web 服务器）
    }

    /**
     * 重新计算指定道路的 current_damage_count、health_score、damage_level。
     */
    private void recalcRoadStats(Long roadId) {
        RoadEntity road = roadMapper.selectById(roadId);
        if (road == null) return;

        // 查询该道路下所有 COMPLETED 任务的病害项
        List<DetectionTaskEntity> roadTasks = taskMapper.selectList(
                new LambdaQueryWrapper<DetectionTaskEntity>()
                        .eq(DetectionTaskEntity::getRoadId, roadId)
                        .eq(DetectionTaskEntity::getStatus, "COMPLETED"));

        Set<Long> seenItemIds = new HashSet<>();
        List<DetectionResultItemEntity> allItems = new ArrayList<>();

        // 1. 直接通过 result_item.road_id 查
        List<DetectionResultItemEntity> directItems = itemMapper.selectList(
                new LambdaQueryWrapper<DetectionResultItemEntity>()
                        .eq(DetectionResultItemEntity::getRoadId, roadId));
        for (DetectionResultItemEntity item : directItems) {
            if (seenItemIds.add(item.getId())) {
                allItems.add(item);
            }
        }

        // 2. 通过 task 关联查
        for (DetectionTaskEntity task : roadTasks) {
            List<DetectionResultItemEntity> items = itemMapper.selectList(
                    new LambdaQueryWrapper<DetectionResultItemEntity>()
                            .eq(DetectionResultItemEntity::getTaskId, task.getId()));
            for (DetectionResultItemEntity item : items) {
                if (seenItemIds.add(item.getId())) {
                    allItems.add(item);
                }
            }
        }

        int damageCount = allItems.size();
        BigDecimal healthScore = RoadHealthScoreCalculator.calculate(road, allItems);
        String damageLevel = RoadHealthScoreCalculator.resolveDamageLevel(healthScore);

        road.setCurrentDamageCount(damageCount);
        road.setHealthScore(healthScore);
        road.setDamageLevel(damageLevel);
        road.setUpdatedAt(LocalDateTime.now());

        // 如果有病害，记录最近检测时间
        if (!roadTasks.isEmpty()) {
            roadTasks.stream()
                    .map(DetectionTaskEntity::getCompletedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .ifPresent(road::setLatestDetectionAt);
        }

        roadMapper.updateById(road);
    }

    /**
     * 自动创建道路记录。
     */
    private Long createAutoRoad(String roadName, double lng, double lat) {
        LocalDateTime now = LocalDateTime.now();
        RoadEntity road = new RoadEntity();
        road.setRoadName(roadName != null && !roadName.isBlank() ? roadName
                : String.format("未命名道路-%.4f,%.4f", lng, lat));
        road.setRoadCode("AUTO-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        road.setCenterLng(BigDecimal.valueOf(lng).setScale(7, RoundingMode.HALF_UP));
        road.setCenterLat(BigDecimal.valueOf(lat).setScale(7, RoundingMode.HALF_UP));
        road.setLengthKm(BigDecimal.valueOf(1.0));
        road.setHealthScore(BigDecimal.valueOf(100));
        road.setDamageLevel("HEALTHY");
        road.setTotalDetectionCount(0);
        road.setCurrentDamageCount(0);
        road.setStatus("ACTIVE");
        road.setCreatedAt(now);
        road.setUpdatedAt(now);
        roadMapper.insert(road);
        return road.getId();
    }

    /**
     * 解析 "lng,lat" 格式的坐标字符串。
     */
    private double[] parseLocation(String location) {
        if (location == null || location.isBlank()) return null;
        String[] parts = location.split(",");
        if (parts.length < 2) return null;
        try {
            double lng = Double.parseDouble(parts[0].trim());
            double lat = Double.parseDouble(parts[1].trim());
            return new double[]{lng, lat};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
