package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import com.roadcrack.api.request.healtharchive.GenerateRoadHealthArchiveRequest;
import com.roadcrack.api.response.detection.BoundingBoxResponse;
import com.roadcrack.api.response.detection.DetectionItemResponse;
import com.roadcrack.api.response.detection.DetectionProgressMessage;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.api.response.websocket.AlertMessageResponse;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.AlertEntity;
import com.roadcrack.dao.entity.DetectionMediaEntity;
import com.roadcrack.dao.entity.DetectionResultEntity;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.entity.RoadEntity;
import com.roadcrack.dao.entity.WorkOrderEntity;
import com.roadcrack.dao.mapper.AlertMapper;
import com.roadcrack.dao.mapper.DetectionMediaMapper;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionResultMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.RoadMapper;
import com.roadcrack.dao.mapper.WorkOrderMapper;
import com.roadcrack.service.model.AuditLogRecord;
import com.roadcrack.service.client.AlgorithmClient;
import com.roadcrack.service.client.AmapGeocodeClient;
import com.roadcrack.service.model.DetectionAnalysisResult;
import com.roadcrack.service.model.DetectionTaskAggregate;
import com.roadcrack.service.port.RealtimeMessagePublisher;
import com.roadcrack.service.service.AuditLogService;
import com.roadcrack.service.service.DetectionTaskService;
import com.roadcrack.service.service.RoadHealthArchiveService;
import com.roadcrack.service.service.WorkOrderService;
import com.roadcrack.service.util.RoadHealthScoreCalculator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbDetectionTaskService implements DetectionTaskService {

    private static final Logger log = LoggerFactory.getLogger(DbDetectionTaskService.class);
    private static final String MODULE_DETECTION_TASK = "DETECTION_TASK";
    private static final String DEFAULT_SUBMITTED_BY = "admin";
    private static final String DEFAULT_MEDIA_TYPE = "IMAGE";
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DetectionTaskMapper detectionTaskMapper;
    private final DetectionMediaMapper detectionMediaMapper;
    private final DetectionResultMapper detectionResultMapper;
    private final DetectionResultItemMapper detectionResultItemMapper;
    private final RoadMapper roadMapper;
    private final AlgorithmClient algorithmClient;
    private final WorkOrderService workOrderService;
    private final AuditLogService auditLogService;
    private final RealtimeMessagePublisher realtimeMessagePublisher;
    private final TaskExecutor detectionTaskExecutor;
    private final AlertMapper alertMapper;
    private final AmapGeocodeClient amapGeocodeClient;
    private final RoadHealthArchiveService roadHealthArchiveService;
    private final WorkOrderMapper workOrderMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DbDetectionTaskService(DetectionTaskMapper detectionTaskMapper,
                                  DetectionMediaMapper detectionMediaMapper,
                                  DetectionResultMapper detectionResultMapper,
                                  DetectionResultItemMapper detectionResultItemMapper,
                                  RoadMapper roadMapper,
                                  AlgorithmClient algorithmClient,
                                  WorkOrderService workOrderService,
                                  AuditLogService auditLogService,
                                  RealtimeMessagePublisher realtimeMessagePublisher,
                                  @Qualifier("detectionTaskExecutor") TaskExecutor detectionTaskExecutor,
                                  AlertMapper alertMapper,
                                  AmapGeocodeClient amapGeocodeClient,
                                  RoadHealthArchiveService roadHealthArchiveService,
                                  WorkOrderMapper workOrderMapper) {
        this.detectionTaskMapper = detectionTaskMapper;
        this.detectionMediaMapper = detectionMediaMapper;
        this.detectionResultMapper = detectionResultMapper;
        this.detectionResultItemMapper = detectionResultItemMapper;
        this.roadMapper = roadMapper;
        this.algorithmClient = algorithmClient;
        this.workOrderService = workOrderService;
        this.auditLogService = auditLogService;
        this.realtimeMessagePublisher = realtimeMessagePublisher;
        this.detectionTaskExecutor = detectionTaskExecutor;
        this.alertMapper = alertMapper;
        this.amapGeocodeClient = amapGeocodeClient;
        this.roadHealthArchiveService = roadHealthArchiveService;
        this.workOrderMapper = workOrderMapper;
    }

    @Override
    @Transactional
    public DetectionTaskResponse createTask(CreateDetectionTaskRequest request) {
        LocalDateTime now = LocalDateTime.now();

        DetectionTaskEntity entity = new DetectionTaskEntity();
        entity.setTaskCode(buildCode(now.toLocalDate()));
        entity.setSourceType(request.dataSourceType().name());
        entity.setLocation(request.location());
        entity.setSubmittedBy(DEFAULT_SUBMITTED_BY);
        entity.setStatus(DetectionTaskStatus.PENDING.name());
        entity.setRemark(request.remark());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // 优先用前端传来的道路名匹配，其次用坐标自动匹配
        Long matchedRoadId = matchNearestRoad(request.location(), request.roadName());
        entity.setRoadId(matchedRoadId);

        detectionTaskMapper.insert(entity);

        DetectionMediaEntity mediaEntity = new DetectionMediaEntity();
        mediaEntity.setTaskId(entity.getId());
        mediaEntity.setMediaType(DEFAULT_MEDIA_TYPE);
        mediaEntity.setFileName(request.fileName());
        mediaEntity.setFileUrl(request.fileUrl());
        mediaEntity.setCapturedAt(now);
        mediaEntity.setCreatedAt(now);
        detectionMediaMapper.insert(mediaEntity);
        log.info("Detection task created in db: taskId={}, taskCode={}, sourceType={}, fileName={}, location={}",
                entity.getId(),
                entity.getTaskCode(),
                entity.getSourceType(),
                mediaEntity.getFileName(),
                entity.getLocation());
        auditLogService.record(AuditLogRecord.success(
                        MODULE_DETECTION_TASK,
                        "CREATE",
                        "Created detection task " + entity.getTaskCode())
                .setUsername(entity.getSubmittedBy())
                .setParams(buildTaskParams(entity.getId(), entity.getTaskCode(), mediaEntity.getFileName(), entity.getLocation()))
                .setCreateTime(now));

        return toTaskResponse(entity, mediaEntity, null, null);
    }

    /**
     * 根据 location（格式 "lng,lat" 或 "道路名 (lng,lat)"）和可选的道路名匹配道路。
     *
     * 策略（优先级从高到低）：
     * 0. 如果前端传入了 roadName，优先在 road 表中按名称精确匹配
     * 1. 调用高德逆地理编码 API 获取真实道路名称
     * 2. 在 road 表中按名称查找已有道路记录；找到则直接返回其 ID
     * 3. 未找到则自动创建一条 road 记录（road_name=高德返回的道路名，自动生成 road_code），返回新 ID
     * 4. 如果高德未配置或返回空，则回退到 Haversine 距离匹配（2km 阈值内匹配已有标准道路）
     * 5. 如果以上都失败，自动创建一条 "未命名道路" 记录
     */
    private Long matchNearestRoad(String location, String roadName) {
        if (location == null || location.isBlank()) return null;
        double[] coords = parseLocationCoord(location);
        if (coords == null) return null;

        // 策略 0：前端传入了道路名，优先精确匹配
        if (roadName != null && !roadName.isBlank()) {
            RoadEntity existing = roadMapper.selectOne(
                    new LambdaQueryWrapper<RoadEntity>()
                            .eq(RoadEntity::getRoadName, roadName));
            if (existing != null) {
                return existing.getId();
            }
            // road 表中没有这条路，自动创建
            return createAutoRoad(roadName, coords[0], coords[1]);
        }

        // 策略 1：高德逆地理编码获取真实道路名
        if (amapGeocodeClient.isConfigured()) {
            String geocodeRoadName = amapGeocodeClient.reverseGeocode(coords[0], coords[1]);
            if (!geocodeRoadName.isBlank()) {
                // 在 road 表中查找同名道路
                RoadEntity existing = roadMapper.selectOne(
                        new LambdaQueryWrapper<RoadEntity>()
                                .eq(RoadEntity::getRoadName, geocodeRoadName));
                if (existing != null) {
                    return existing.getId();
                }
                // 自动创建新道路记录
                return createAutoRoad(geocodeRoadName, coords[0], coords[1]);
            }
        }

        // 策略 2：回退到 Haversine 距离匹配（2km 阈值）
        List<RoadEntity> roads = roadMapper.selectList(null);
        if (!roads.isEmpty()) {
            Long nearestRoadId = null;
            double minDistance = Double.MAX_VALUE;
            for (RoadEntity road : roads) {
                if (road.getCenterLng() == null || road.getCenterLat() == null) continue;
                double dist = haversineDistance(
                    coords[0], coords[1],
                    road.getCenterLng().doubleValue(), road.getCenterLat().doubleValue()
                );
                if (dist < minDistance) {
                    minDistance = dist;
                    nearestRoadId = road.getId();
                }
            }
            // 2km 阈值：只有距离在 2000m 以内才匹配
            if (nearestRoadId != null && minDistance <= 2000) {
                return nearestRoadId;
            }
        }

        // 策略 3：高德未配置或解析失败，自动创建未命名道路
        return createAutoRoad(null, coords[0], coords[1]);
    }

    /**
     * 自动创建道路记录。
     * @param roadName 道路名称（可为 null，此时生成 "未命名道路-经度,纬度" 格式）
     * @param lng 经度
     * @param lat 纬度
     * @return 新创建的道路 ID
     */
    private Long createAutoRoad(String roadName, double lng, double lat) {
        LocalDateTime now = LocalDateTime.now();
        RoadEntity road = new RoadEntity();
        if (roadName != null && !roadName.isBlank()) {
            road.setRoadName(roadName);
        } else {
            road.setRoadName(String.format("未命名道路-%.4f,%.4f", lng, lat));
        }
        road.setRoadCode("AUTO-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        road.setCenterLng(BigDecimal.valueOf(lng).setScale(7, RoundingMode.HALF_UP));
        road.setCenterLat(BigDecimal.valueOf(lat).setScale(7, RoundingMode.HALF_UP));
        road.setLengthKm(BigDecimal.valueOf(1.0)); // 默认 1km
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
     * 简化的 Haversine 球面距离（单位：米）
     */
    private double haversineDistance(double lng1, double lat1, double lng2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000.0 * c; // 地球半径 6371km
    }

    /**
     * 解析位置字符串为坐标数组。
     * 支持格式：
     * - "lng,lat"（纯坐标）
     * - "道路名 (lng,lat)"（带道路名的坐标）
     */
    private double[] parseLocationCoord(String location) {
        if (location == null || location.isBlank()) return null;
        // 如果格式是 "道路名 (lng,lat)"，先提取括号内的坐标
        int parenStart = location.lastIndexOf('(');
        int parenEnd = location.lastIndexOf(')');
        String coordStr = location;
        if (parenStart >= 0 && parenEnd > parenStart) {
            coordStr = location.substring(parenStart + 1, parenEnd);
        }
        String[] parts = coordStr.split(",");
        if (parts.length != 2) return null;
        try {
            double lng = Double.parseDouble(parts[0].trim());
            double lat = Double.parseDouble(parts[1].trim());
            return new double[]{lng, lat};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Transactional
    public void executeTask(Long taskId) {
        DetectionTaskEntity taskEntity = getRequiredTask(taskId);
        DetectionTaskStatus status = DetectionTaskStatus.valueOf(taskEntity.getStatus());
        if (status == DetectionTaskStatus.PROCESSING) {
            throw new BusinessException(ResultCode.CONFLICT, "任务正在处理中，不能重复提交");
        }
        if (status == DetectionTaskStatus.COMPLETED) {
            throw new BusinessException(ResultCode.CONFLICT, "任务已完成，如需重跑请新建任务");
        }

        LocalDateTime now = LocalDateTime.now();
        detectionTaskMapper.update(null, new LambdaUpdateWrapper<DetectionTaskEntity>()
                .eq(DetectionTaskEntity::getId, taskId)
                .set(DetectionTaskEntity::getStatus, DetectionTaskStatus.PROCESSING.name())
                .set(DetectionTaskEntity::getStartedAt, now)
                .set(DetectionTaskEntity::getFailureReason, null)
                .set(DetectionTaskEntity::getUpdatedAt, now));

        publishProgress(taskId, DetectionTaskStatus.PROCESSING, 5, "任务已进入检测队列");
        log.info("Detection task queued in db: taskId={}, taskCode={}", taskId, taskEntity.getTaskCode());
        detectionTaskExecutor.execute(() -> processTask(taskId));
    }

    @Override
    public PageResponse<DetectionTaskResponse> listTasks(int page,
                                                         int size,
                                                         DetectionTaskStatus status,
                                                         DataSourceType dataSourceType,
                                                         String location,
                                                         String submittedBy) {
        Page<DetectionTaskEntity> queryPage = new Page<>(page, size);
        LambdaQueryWrapper<DetectionTaskEntity> wrapper = new LambdaQueryWrapper<DetectionTaskEntity>()
                .orderByDesc(DetectionTaskEntity::getCreatedAt);
        if (status != null) {
            wrapper.eq(DetectionTaskEntity::getStatus, status.name());
        }
        if (dataSourceType != null) {
            wrapper.eq(DetectionTaskEntity::getSourceType, dataSourceType.name());
        }
        if (submittedBy != null && !submittedBy.isBlank()) {
            wrapper.like(DetectionTaskEntity::getSubmittedBy, submittedBy);
        }
        if (location != null && !location.isBlank()) {
            wrapper.like(DetectionTaskEntity::getLocation, location);
        }
        detectionTaskMapper.selectPage(queryPage, wrapper);

        List<DetectionTaskEntity> taskEntities = queryPage.getRecords();
        List<Long> taskIds = taskEntities.stream().map(DetectionTaskEntity::getId).toList();
        Map<Long, DetectionMediaEntity> mediaMap = findFirstMediaMap(taskIds);
        Map<Long, DetectionResultEntity> resultMap = findResultMap(taskIds);

        List<DetectionTaskResponse> records = taskEntities.stream()
                .map(task -> toTaskResponse(task, mediaMap.get(task.getId()), resultMap.get(task.getId()), null))
                .toList();

        return new PageResponse<>(records, queryPage.getTotal(), queryPage.getSize(), queryPage.getCurrent(), queryPage.getPages());
    }

    @Override
    public DetectionTaskResponse getTask(Long taskId) {
        DetectionTaskEntity taskEntity = getRequiredTask(taskId);
        DetectionMediaEntity mediaEntity = getFirstMedia(taskId);
        DetectionResultEntity resultEntity = findResultEntity(taskId);
        DetectionResultResponse result = loadResult(taskId);
        return toTaskResponse(taskEntity, mediaEntity, resultEntity, result);
    }

    @Override
    public DetectionResultResponse getResult(Long taskId) {
        getRequiredTask(taskId);
        DetectionResultResponse result = loadResult(taskId);
        if (result == null) {
            throw new BusinessException(ResultCode.CONFLICT, "检测结果尚未生成");
        }
        return result;
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        DetectionTaskEntity taskEntity = getRequiredTask(taskId);
        DetectionTaskStatus status = DetectionTaskStatus.valueOf(taskEntity.getStatus());
        if (status != DetectionTaskStatus.PENDING && status != DetectionTaskStatus.FAILED
                && status != DetectionTaskStatus.COMPLETED) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待处理、失败或已完成的任务允许删除");
        }

        // 级联删除关联的工单（含 COMPLETED 状态任务自动生成的工单）
        workOrderMapper.delete(new LambdaQueryWrapper<WorkOrderEntity>()
                .eq(WorkOrderEntity::getDetectionTaskId, taskId));

        DetectionResultEntity resultEntity = findResultEntity(taskId);
        if (resultEntity != null) {
            detectionResultItemMapper.delete(new LambdaQueryWrapper<DetectionResultItemEntity>()
                    .eq(DetectionResultItemEntity::getResultId, resultEntity.getId()));
            detectionResultMapper.deleteById(resultEntity.getId());
        }
        detectionMediaMapper.delete(new LambdaQueryWrapper<DetectionMediaEntity>()
                .eq(DetectionMediaEntity::getTaskId, taskId));
        detectionTaskMapper.deleteById(taskId);
    }

    @Override
    @Transactional
    public void batchUpdateSeverity(List<Long> taskIds, String newSeverity, String operator) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择至少一条检测结果");
        }
        if (newSeverity == null || newSeverity.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请指定目标严重等级");
        }
        String upperSeverity = newSeverity.toUpperCase();
        if (!List.of("HIGH", "MEDIUM", "LOW", "NORMAL").contains(upperSeverity)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的严重等级: " + newSeverity + "，可选值: HIGH, MEDIUM, LOW, NORMAL");
        }

        int updatedCount = 0;
        List<String> taskCodes = new java.util.ArrayList<>();

        for (Long taskId : taskIds) {
            DetectionTaskEntity taskEntity = getRequiredTask(taskId);
            taskCodes.add(taskEntity.getTaskCode());

            DetectionResultEntity resultEntity = findResultEntity(taskId);
            if (resultEntity == null) {
                continue; // 无检测结果的任务跳过
            }

            if ("NORMAL".equals(upperSeverity)) {
                // 设为"无病害"：删除所有病害项
                detectionResultItemMapper.delete(new LambdaQueryWrapper<DetectionResultItemEntity>()
                        .eq(DetectionResultItemEntity::getResultId, resultEntity.getId()));
                // 更新结果汇总
                resultEntity.setHighestSeverity("NORMAL");
                resultEntity.setTotalDamageCount(0);
                resultEntity.setAvgConfidence(java.math.BigDecimal.ZERO);
                resultEntity.setSummary("经人工复核，此路段无病害");
                resultEntity.setUpdatedAt(LocalDateTime.now());
                detectionResultMapper.updateById(resultEntity);

                // 同时更新任务关联的工单（若有则取消）
                WorkOrderEntity workOrder = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrderEntity>()
                        .eq(WorkOrderEntity::getDetectionTaskId, taskId));
                if (workOrder != null) {
                    workOrder.setStatus("CANCELLED");
                    workOrder.setUpdatedAt(LocalDateTime.now());
                    workOrderMapper.updateById(workOrder);
                }

                updatedCount++;
            } else {
                // 调整所有病害项的严重等级
                List<DetectionResultItemEntity> items = detectionResultItemMapper.selectList(
                        new LambdaQueryWrapper<DetectionResultItemEntity>()
                                .eq(DetectionResultItemEntity::getResultId, resultEntity.getId()));

                if (items.isEmpty()) {
                    continue;
                }

                for (DetectionResultItemEntity item : items) {
                    item.setSeverityLevel(upperSeverity);
                    item.setCreatedAt(item.getCreatedAt()); // 保留原始创建时间
                    detectionResultItemMapper.updateById(item);
                }

                // 更新结果汇总的最高严重等级
                resultEntity.setHighestSeverity(upperSeverity);
                resultEntity.setUpdatedAt(LocalDateTime.now());
                detectionResultMapper.updateById(resultEntity);

                updatedCount++;
            }

            // 更新关联道路的健康信息
            if (taskEntity.getRoadId() != null) {
                try {
                    updateRoadHealthAfterDetection(taskEntity.getRoadId(), LocalDateTime.now());
                } catch (Exception ignored) {
                    // 健康评分更新失败不影响主流程
                }
            }
        }

        // 记录审计日志
        String severityLabel = switch (upperSeverity) {
            case "HIGH" -> "严重";
            case "MEDIUM" -> "中等";
            case "LOW" -> "轻微";
            case "NORMAL" -> "无病害";
            default -> upperSeverity;
        };
        auditLogService.record(
                operator,
                "DETECTION",
                "BATCH_UPDATE_SEVERITY",
                String.format("批量调整 %d 条检测结果的严重等级为「%s」：%s",
                        updatedCount, severityLabel, String.join(", ", taskCodes)),
                "0.0.0.0",
                null,
                "SUCCESS",
                null
        );
    }

    @Override
    @Transactional
    public void batchDeleteTasks(List<Long> taskIds, String operator) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择至少一条检测结果");
        }

        int deletedCount = 0;
        List<String> taskCodes = new java.util.ArrayList<>();
        StringBuilder errors = new StringBuilder();

        for (Long taskId : taskIds) {
            try {
                DetectionTaskEntity taskEntity = getRequiredTask(taskId);
                DetectionTaskStatus status = DetectionTaskStatus.valueOf(taskEntity.getStatus());
                if (status != DetectionTaskStatus.PENDING && status != DetectionTaskStatus.FAILED
                        && status != DetectionTaskStatus.COMPLETED) {
                    errors.append("任务 ").append(taskEntity.getTaskCode()).append(" 状态不允许删除; ");
                    continue;
                }
                taskCodes.add(taskEntity.getTaskCode());

                // 级联删除关联的工单
                workOrderMapper.delete(new LambdaQueryWrapper<WorkOrderEntity>()
                        .eq(WorkOrderEntity::getDetectionTaskId, taskId));

                DetectionResultEntity resultEntity = findResultEntity(taskId);
                if (resultEntity != null) {
                    detectionResultItemMapper.delete(new LambdaQueryWrapper<DetectionResultItemEntity>()
                            .eq(DetectionResultItemEntity::getResultId, resultEntity.getId()));
                    detectionResultMapper.deleteById(resultEntity.getId());
                }
                detectionMediaMapper.delete(new LambdaQueryWrapper<DetectionMediaEntity>()
                        .eq(DetectionMediaEntity::getTaskId, taskId));
                detectionTaskMapper.deleteById(taskId);
                deletedCount++;
            } catch (Exception e) {
                errors.append("任务 ID=").append(taskId).append(" 删除失败: ").append(e.getMessage()).append("; ");
            }
        }

        // 记录审计日志
        String status = errors.length() > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";
        auditLogService.record(
                operator,
                "DETECTION",
                "BATCH_DELETE",
                String.format("批量删除 %d 条检测结果（共选中 %d 条）：%s%s",
                        deletedCount, taskIds.size(),
                        String.join(", ", taskCodes),
                        errors.length() > 0 ? "。部分失败: " + errors : ""),
                "0.0.0.0",
                null,
                status,
                errors.length() > 0 ? errors.toString() : null
        );

        if (deletedCount == 0 && errors.length() > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "所有任务删除失败: " + errors);
        }
    }

    private void processTask(Long taskId) {
        long startTime = System.currentTimeMillis();
        try {
            DetectionTaskEntity taskEntity = getRequiredTask(taskId);
            DetectionMediaEntity mediaEntity = getFirstMedia(taskId);
            DetectionTaskAggregate aggregate = new DetectionTaskAggregate(
                    taskEntity.getId(),
                    taskEntity.getTaskCode(),
                    DataSourceType.valueOf(taskEntity.getSourceType()),
                    mediaEntity == null ? "unknown" : mediaEntity.getFileName(),
                    mediaEntity == null ? "" : mediaEntity.getFileUrl(),
                    taskEntity.getLocation(),
                    taskEntity.getRoadId(),
                    taskEntity.getRemark(),
                    taskEntity.getSubmittedBy(),
                    taskEntity.getCreatedAt()
            );

            publishProgress(taskId, DetectionTaskStatus.PROCESSING, 25, "图像分析开始执行");
            log.info("Detection task started in db: taskId={}, taskCode={}", taskId, taskEntity.getTaskCode());
            DetectionAnalysisResult analysisResult = algorithmClient.analyze(aggregate);
            publishProgress(taskId, DetectionTaskStatus.PROCESSING, 70, "图像分析完成，正在生成检测结果");

            persistSuccessfulResult(taskEntity, mediaEntity, analysisResult);

            Long generatedWorkOrderId = null;
            try {
                generatedWorkOrderId = createWorkOrderIfNeeded(taskEntity, mediaEntity, analysisResult.items());
                if (generatedWorkOrderId != null) {
                    attachGeneratedWorkOrder(taskEntity.getId(), generatedWorkOrderId);
                }
            } catch (Exception workOrderException) {
                log.error("Auto work order creation failed for detection task: taskId={}, taskCode={}",
                        taskId,
                        taskEntity.getTaskCode(),
                        workOrderException);
            }

            // 检测到严重病害时自动生成告警
            createAlertIfNeeded(taskEntity, analysisResult, generatedWorkOrderId);

            DetectionResultResponse result = getResult(taskId);
            log.info("Detection task completed in db: taskId={}, taskCode={}, itemCount={}, generatedWorkOrderId={}, durationMs={}",
                    taskId,
                    taskEntity.getTaskCode(),
                    analysisResult.items().size(),
                    generatedWorkOrderId,
                    System.currentTimeMillis() - startTime);
            auditLogService.record(AuditLogRecord.success(
                            MODULE_DETECTION_TASK,
                            "COMPLETE",
                            "Completed detection task " + taskEntity.getTaskCode())
                    .setUsername(aggregate.getSubmittedBy())
                    .setParams(buildCompletionParams(
                            taskId,
                            taskEntity.getTaskCode(),
                            analysisResult.items().size(),
                            generatedWorkOrderId))
                    .setCostTime(System.currentTimeMillis() - startTime));
            publishProgress(taskId, DetectionTaskStatus.COMPLETED, 100, "检测任务已完成");
            realtimeMessagePublisher.publishDetectionResult(taskId, result);
        } catch (Exception exception) {
            markTaskFailed(taskId, exception.getMessage());
            log.error("Detection task failed in db: taskId={}, durationMs={}",
                    taskId,
                    System.currentTimeMillis() - startTime,
                    exception);
            DetectionTaskEntity failedTask = detectionTaskMapper.selectById(taskId);
            auditLogService.record(AuditLogRecord.failure(
                            MODULE_DETECTION_TASK,
                            "FAIL",
                            "Detection task failed: " + resolveTaskCode(taskId, failedTask),
                            exception.getMessage())
                    .setUsername(failedTask == null ? DEFAULT_SUBMITTED_BY : failedTask.getSubmittedBy())
                    .setParams(buildFailureParams(taskId, failedTask))
                    .setCostTime(System.currentTimeMillis() - startTime));
            publishProgress(taskId, DetectionTaskStatus.FAILED, 100, "检测失败: " + exception.getMessage());
            realtimeMessagePublisher.publishAlert(new AlertMessageResponse(
                    "DETECTION_FAILED",
                    "WARNING",
                    taskId,
                    "检测任务执行失败",
                    exception.getMessage(),
                    LocalDateTime.now()
            ));
        }
    }

    @Transactional
    protected void persistSuccessfulResult(DetectionTaskEntity taskEntity,
                                           DetectionMediaEntity mediaEntity,
                                           DetectionAnalysisResult analysisResult) {
        LocalDateTime now = LocalDateTime.now();
        DetectionResultEntity resultEntity = new DetectionResultEntity();
        resultEntity.setTaskId(taskEntity.getId());
        resultEntity.setSummary(analysisResult.summary());
        resultEntity.setTotalDamageCount(analysisResult.items().size());
        resultEntity.setHighestSeverity(resolveHighestSeverity(analysisResult.items()));
        resultEntity.setAvgConfidence(calculateAverageConfidence(analysisResult.items()));
        resultEntity.setGeneratedWorkOrderId(null);
        // imageBase64 字段已被 HttpAlgorithmClient 转换为 /uploads/result/xxx.png URL
        resultEntity.setAnnotatedImageUrl(analysisResult.imageBase64());
        // 保存关键帧 URL 列表（JSON 格式）
        if (analysisResult.keyframeUrls() != null && !analysisResult.keyframeUrls().isEmpty()) {
            try {
                resultEntity.setKeyframeUrls(objectMapper.writeValueAsString(analysisResult.keyframeUrls()));
            } catch (Exception e) {
                resultEntity.setKeyframeUrls(null);
            }
        }
        resultEntity.setCompletedAt(now);
        resultEntity.setCreatedAt(now);
        resultEntity.setUpdatedAt(now);
        detectionResultMapper.insert(resultEntity);

        for (DetectionItemResponse item : analysisResult.items()) {
            DetectionResultItemEntity itemEntity = new DetectionResultItemEntity();
            itemEntity.setResultId(resultEntity.getId());
            itemEntity.setTaskId(taskEntity.getId());
            itemEntity.setMediaId(mediaEntity == null ? null : mediaEntity.getId());
            // 继承 task 的 road_id，确保病害与道路关联
            itemEntity.setRoadId(taskEntity.getRoadId());
            // 从 task.location 解析坐标写入 result_item
            double[] taskCoords = parseLocationCoord(taskEntity.getLocation());
            if (taskCoords != null) {
                itemEntity.setLng(BigDecimal.valueOf(taskCoords[0]).setScale(7, RoundingMode.HALF_UP));
                itemEntity.setLat(BigDecimal.valueOf(taskCoords[1]).setScale(7, RoundingMode.HALF_UP));
            }
            itemEntity.setDamageType(item.damageType().name());
            itemEntity.setSeverityLevel(item.severityLevel().name());
            itemEntity.setConfidence(BigDecimal.valueOf(item.confidence()).setScale(4, RoundingMode.HALF_UP));
            if (item.boundingBox() != null) {
                itemEntity.setBboxX(item.boundingBox().x());
                itemEntity.setBboxY(item.boundingBox().y());
                itemEntity.setBboxWidth(item.boundingBox().width());
                itemEntity.setBboxHeight(item.boundingBox().height());
            }
            itemEntity.setSuggestion(item.suggestion());
            itemEntity.setDetectedAt(now);
            itemEntity.setCreatedAt(now);
            detectionResultItemMapper.insert(itemEntity);
        }

        detectionTaskMapper.update(null, new LambdaUpdateWrapper<DetectionTaskEntity>()
                .eq(DetectionTaskEntity::getId, taskEntity.getId())
                .set(DetectionTaskEntity::getStatus, DetectionTaskStatus.COMPLETED.name())
                .set(DetectionTaskEntity::getFailureReason, null)
                .set(DetectionTaskEntity::getCompletedAt, now)
                .set(DetectionTaskEntity::getUpdatedAt, now));

        // 检测完成后，自动更新关联道路的健康信息
        updateRoadHealthAfterDetection(taskEntity.getRoadId(), now);

        // 检测完成后，自动生成道路健康档案（当天）
        autoGenerateHealthArchive(taskEntity.getRoadId(), now);
    }

    private void attachGeneratedWorkOrder(Long taskId, Long workOrderId) {
        detectionResultMapper.update(null, new LambdaUpdateWrapper<DetectionResultEntity>()
                .eq(DetectionResultEntity::getTaskId, taskId)
                .set(DetectionResultEntity::getGeneratedWorkOrderId, workOrderId)
                .set(DetectionResultEntity::getUpdatedAt, LocalDateTime.now()));
    }

    /**
     * 检测完成后自动为该道路生成当天的健康档案。
     * generateArchive 内部有幂等性处理（同道路同日期先删后建），所以重复调用是安全的。
     */
    private void autoGenerateHealthArchive(Long roadId, LocalDateTime now) {
        if (roadId == null) return;
        try {
            roadHealthArchiveService.generateArchive(new GenerateRoadHealthArchiveRequest(
                    roadId, now.toLocalDate()
            ));
        } catch (Exception e) {
            // 健康档案生成失败不应影响检测任务本身的结果
            System.err.println("[autoGenerateHealthArchive] roadId=" + roadId + " failed: " + e.getMessage());
        }
    }

    /**
     * 检测完成后，根据该道路下的所有病害数据重新计算并更新道路健康信息。
     */
    private void updateRoadHealthAfterDetection(Long roadId, LocalDateTime now) {
        if (roadId == null) return;

        RoadEntity road = roadMapper.selectById(roadId);
        if (road == null) return;

        // 查询该道路下的所有检测任务
        List<DetectionTaskEntity> roadTasks = detectionTaskMapper.selectList(
                new LambdaQueryWrapper<DetectionTaskEntity>()
                        .eq(DetectionTaskEntity::getRoadId, roadId)
                        .eq(DetectionTaskEntity::getStatus, DetectionTaskStatus.COMPLETED.name()));

        // 查询该道路下的所有病害结果项
        List<Long> taskIds = roadTasks.stream().map(DetectionTaskEntity::getId).toList();
        if (taskIds.isEmpty()) {
            // 没有已完成任务，重置健康信息
            road.setHealthScore(BigDecimal.valueOf(100));
            road.setDamageLevel("HEALTHY");
            road.setTotalDetectionCount(0);
            road.setCurrentDamageCount(0);
            road.setLatestDetectionAt(null);
            road.setUpdatedAt(now);
            roadMapper.updateById(road);
            return;
        }

        List<DetectionResultItemEntity> allItems = detectionResultItemMapper.selectList(
                new LambdaQueryWrapper<DetectionResultItemEntity>()
                        .in(DetectionResultItemEntity::getTaskId, taskIds));

        // 计算健康评分
        BigDecimal healthScore = RoadHealthScoreCalculator.calculate(road, allItems);
        String damageLevel = RoadHealthScoreCalculator.resolveDamageLevel(healthScore);

        // 更新道路信息
        road.setHealthScore(healthScore);
        road.setDamageLevel(damageLevel);
        road.setTotalDetectionCount(roadTasks.size());
        road.setCurrentDamageCount(allItems.size());
        road.setLatestDetectionAt(now);
        road.setUpdatedAt(now);
        roadMapper.updateById(road);
    }

    @Transactional
    protected void markTaskFailed(Long taskId, String failureReason) {
        detectionTaskMapper.update(null, new LambdaUpdateWrapper<DetectionTaskEntity>()
                .eq(DetectionTaskEntity::getId, taskId)
                .set(DetectionTaskEntity::getStatus, DetectionTaskStatus.FAILED.name())
                .set(DetectionTaskEntity::getFailureReason, failureReason)
                .set(DetectionTaskEntity::getUpdatedAt, LocalDateTime.now()));
    }

    private Long createWorkOrderIfNeeded(DetectionTaskEntity taskEntity,
                                         DetectionMediaEntity mediaEntity,
                                         List<DetectionItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        DetectionItemResponse topItem = items.stream()
                .max(Comparator.comparingInt(item -> severityScore(item.severityLevel())))
                .orElse(null);
        if (topItem == null) {
            return null;
        }
        log.info("Creating work order from detection task in db: taskId={}, taskCode={}, damageType={}, severity={}",
                taskEntity.getId(),
                taskEntity.getTaskCode(),
                topItem.damageType(),
                topItem.severityLevel());
        WorkOrderResponse workOrder = workOrderService.createFromDetection(
                taskEntity.getId(),
                topItem.damageType(),
                topItem.severityLevel(),
                taskEntity.getLocation(),
                mediaEntity == null ? null : mediaEntity.getFileUrl()
        );
        return workOrder.id();
    }

    private int severityScore(SeverityLevel level) {
        return switch (level) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    private void createAlertIfNeeded(DetectionTaskEntity taskEntity,
                                     DetectionAnalysisResult analysisResult,
                                     Long workOrderId) {
        if (analysisResult.items() == null || analysisResult.items().isEmpty()) {
            return;
        }
        // 找到最高严重等级
        DetectionItemResponse topItem = analysisResult.items().stream()
                .max(Comparator.comparingInt(item -> severityScore(item.severityLevel())))
                .orElse(null);
        if (topItem == null) {
            return;
        }

        SeverityLevel severity = topItem.severityLevel();
        // HIGH → 严重告警, MEDIUM → 中等告警, LOW → 一般告警
        String alertLevel = switch (severity) {
            case HIGH -> "HIGH";
            case MEDIUM -> "MEDIUM";
            case LOW -> "LOW";
        };

        // 只对 HIGH 和 MEDIUM 生成告警，LOW 不需要
        if (severity == SeverityLevel.LOW) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long alertCount = alertMapper.selectCount(null) + 1;
        String alertCode = "ALT-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + String.format("%03d", alertCount);

        String damageType = topItem.damageType().name();
        String location = taskEntity.getLocation() != null ? taskEntity.getLocation() : "未知位置";

        String title;
        String content;
        String alertType;
        if (severity == SeverityLevel.HIGH) {
            alertType = "SEVERITY_DAMAGE";
            title = location + " 检测到严重" + damageTypeName(damageType);
            content = String.format("检测到%s级别病害，类型: %s，置信度: %.1f%%，建议立即处理",
                    severity.name(), damageTypeName(damageType), topItem.confidence() * 100);
        } else {
            alertType = "SUDDEN_CRACK";
            title = location + " 检测到" + damageTypeName(damageType);
            content = String.format("检测到%s级别病害，类型: %s，置信度: %.1f%%",
                    severity.name(), damageTypeName(damageType), topItem.confidence() * 100);
        }

        AlertEntity alert = new AlertEntity();
        alert.setAlertCode(alertCode);
        alert.setAlertType(alertType);
        alert.setAlertLevel(alertLevel);
        alert.setTitle(title);
        alert.setContent(content);
        alert.setDamageType(damageType);
        alert.setLocation(location);
        alert.setWorkOrderId(workOrderId);
        alert.setDetectionTaskId(taskEntity.getId());
        alert.setStatus("PENDING");
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);
        alertMapper.insert(alert);
    }

    private String damageTypeName(String type) {
        return switch (type) {
            case "CRACK" -> "裂缝";
            case "POTHOLE" -> "坑槽";
            case "ALLIGATOR" -> "龟裂";
            case "BLOCK" -> "块状裂缝";
            case "LONGITUDINAL" -> "纵向裂缝";
            case "TRANSVERSE" -> "横向裂缝";
            default -> type;
        };
    }

    private String resolveHighestSeverity(List<DetectionItemResponse> items) {
        return items.stream()
                .map(DetectionItemResponse::severityLevel)
                .max(Comparator.comparingInt(this::severityScore))
                .map(Enum::name)
                .orElse(null);
    }

    private BigDecimal calculateAverageConfidence(List<DetectionItemResponse> items) {
        if (items.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        double total = items.stream().mapToDouble(DetectionItemResponse::confidence).sum();
        return BigDecimal.valueOf(total / items.size()).setScale(4, RoundingMode.HALF_UP);
    }

    private DetectionResultResponse loadResult(Long taskId) {
        DetectionResultEntity resultEntity = findResultEntity(taskId);
        if (resultEntity == null) {
            return null;
        }

        List<DetectionResultItemEntity> itemEntities = detectionResultItemMapper.selectList(
                new LambdaQueryWrapper<DetectionResultItemEntity>()
                        .eq(DetectionResultItemEntity::getResultId, resultEntity.getId())
                        .orderByAsc(DetectionResultItemEntity::getId)
        );

        List<DetectionItemResponse> items = itemEntities.stream()
                .map(this::toItemResponse)
                .toList();

        // 解析关键帧 URL 列表
        List<String> keyframeUrls = null;
        String kfJson = resultEntity.getKeyframeUrls();
        if (kfJson != null && !kfJson.isBlank()) {
            try {
                keyframeUrls = objectMapper.readValue(kfJson, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception e) {
                keyframeUrls = null;
            }
        }

        return new DetectionResultResponse(
                taskId,
                resultEntity.getSummary(),
                items,
                resultEntity.getGeneratedWorkOrderId(),
                resultEntity.getCompletedAt(),
                resultEntity.getAnnotatedImageUrl(),
                keyframeUrls
        );
    }

    private DetectionItemResponse toItemResponse(DetectionResultItemEntity entity) {
        BoundingBoxResponse boundingBox = null;
        if (entity.getBboxX() != null || entity.getBboxY() != null || entity.getBboxWidth() != null || entity.getBboxHeight() != null) {
            boundingBox = new BoundingBoxResponse(
                    entity.getBboxX() == null ? 0 : entity.getBboxX(),
                    entity.getBboxY() == null ? 0 : entity.getBboxY(),
                    entity.getBboxWidth() == null ? 0 : entity.getBboxWidth(),
                    entity.getBboxHeight() == null ? 0 : entity.getBboxHeight()
            );
        }
        return new DetectionItemResponse(
                entity.getId(),
                com.roadcrack.api.enums.DamageType.valueOf(entity.getDamageType()),
                SeverityLevel.valueOf(entity.getSeverityLevel()),
                entity.getConfidence().doubleValue(),
                boundingBox,
                entity.getSuggestion()
        );
    }

    private DetectionTaskResponse toTaskResponse(DetectionTaskEntity taskEntity,
                                                 DetectionMediaEntity mediaEntity,
                                                 DetectionResultEntity resultEntity,
                                                 DetectionResultResponse result) {
        return new DetectionTaskResponse(
                taskEntity.getId(),
                taskEntity.getTaskCode(),
                DataSourceType.valueOf(taskEntity.getSourceType()),
                mediaEntity == null ? null : mediaEntity.getFileName(),
                mediaEntity == null ? null : mediaEntity.getFileUrl(),
                taskEntity.getLocation(),
                taskEntity.getRoadId(),
                taskEntity.getRemark(),
                taskEntity.getSubmittedBy(),
                DetectionTaskStatus.valueOf(taskEntity.getStatus()),
                taskEntity.getFailureReason(),
                taskEntity.getCreatedAt(),
                taskEntity.getUpdatedAt(),
                resultEntity == null ? null : resultEntity.getHighestSeverity(),
                resultEntity == null ? null : resultEntity.getTotalDamageCount(),
                result
        );
    }

    private DetectionTaskEntity getRequiredTask(Long taskId) {
        DetectionTaskEntity entity = detectionTaskMapper.selectById(taskId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检测任务不存在: " + taskId);
        }
        return entity;
    }

    private DetectionMediaEntity getFirstMedia(Long taskId) {
        return detectionMediaMapper.selectOne(new LambdaQueryWrapper<DetectionMediaEntity>()
                .eq(DetectionMediaEntity::getTaskId, taskId)
                .orderByAsc(DetectionMediaEntity::getId)
                .last("limit 1"));
    }

    private Map<Long, DetectionMediaEntity> findFirstMediaMap(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return detectionMediaMapper.selectList(new LambdaQueryWrapper<DetectionMediaEntity>()
                        .in(DetectionMediaEntity::getTaskId, taskIds)
                        .orderByAsc(DetectionMediaEntity::getId))
                .stream()
                .collect(Collectors.toMap(DetectionMediaEntity::getTaskId, Function.identity(), (left, right) -> left));
    }

    private DetectionResultEntity findResultEntity(Long taskId) {
        return detectionResultMapper.selectOne(new LambdaQueryWrapper<DetectionResultEntity>()
                .eq(DetectionResultEntity::getTaskId, taskId)
                .last("limit 1"));
    }

    private Map<Long, DetectionResultEntity> findResultMap(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return detectionResultMapper.selectList(new LambdaQueryWrapper<DetectionResultEntity>()
                        .in(DetectionResultEntity::getTaskId, taskIds))
                .stream()
                .collect(Collectors.toMap(DetectionResultEntity::getTaskId, Function.identity(), (left, right) -> left));
    }

    private void publishProgress(Long taskId, DetectionTaskStatus status, int progress, String message) {
        realtimeMessagePublisher.publishDetectionProgress(
                taskId,
                new DetectionProgressMessage(taskId, status, progress, message, LocalDateTime.now())
        );
    }

    private String buildTaskParams(Long taskId, String taskCode, String fileName, String location) {
        return "taskId=" + taskId
                + ", taskCode=" + taskCode
                + ", fileName=" + safeValue(fileName)
                + ", location=" + safeValue(location);
    }

    private String buildCompletionParams(Long taskId, String taskCode, int itemCount, Long generatedWorkOrderId) {
        return "taskId=" + taskId
                + ", taskCode=" + taskCode
                + ", itemCount=" + itemCount
                + ", generatedWorkOrderId=" + (generatedWorkOrderId == null ? "null" : generatedWorkOrderId);
    }

    private String buildFailureParams(Long taskId, DetectionTaskEntity taskEntity) {
        return "taskId=" + taskId
                + ", taskCode=" + resolveTaskCode(taskId, taskEntity)
                + ", location=" + safeValue(taskEntity == null ? null : taskEntity.getLocation());
    }

    private String resolveTaskCode(Long taskId, DetectionTaskEntity taskEntity) {
        return taskEntity == null || taskEntity.getTaskCode() == null
                ? "TASK-" + taskId
                : taskEntity.getTaskCode();
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String buildCode(LocalDate date) {
        long count = detectionTaskMapper.selectCount(new LambdaQueryWrapper<DetectionTaskEntity>()
                .ge(DetectionTaskEntity::getCreatedAt, date.atStartOfDay())
                .lt(DetectionTaskEntity::getCreatedAt, date.plusDays(1).atStartOfDay()));
        return "DT-" + date.format(CODE_DATE_FORMATTER) + "-" + String.format("%06d", count + 1);
    }
}
