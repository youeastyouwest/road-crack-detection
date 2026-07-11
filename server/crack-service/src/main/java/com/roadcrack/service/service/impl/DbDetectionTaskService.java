package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
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
import com.roadcrack.dao.entity.DetectionMediaEntity;
import com.roadcrack.dao.entity.DetectionResultEntity;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.mapper.DetectionMediaMapper;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionResultMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.service.client.AlgorithmClient;
import com.roadcrack.service.model.DetectionAnalysisResult;
import com.roadcrack.service.model.DetectionTaskAggregate;
import com.roadcrack.service.port.RealtimeMessagePublisher;
import com.roadcrack.service.service.DetectionTaskService;
import com.roadcrack.service.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbDetectionTaskService implements DetectionTaskService {

    private static final String DEFAULT_SUBMITTED_BY = "admin";
    private static final String DEFAULT_MEDIA_TYPE = "IMAGE";
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DetectionTaskMapper detectionTaskMapper;
    private final DetectionMediaMapper detectionMediaMapper;
    private final DetectionResultMapper detectionResultMapper;
    private final DetectionResultItemMapper detectionResultItemMapper;
    private final AlgorithmClient algorithmClient;
    private final WorkOrderService workOrderService;
    private final RealtimeMessagePublisher realtimeMessagePublisher;
    private final TaskExecutor detectionTaskExecutor;

    public DbDetectionTaskService(DetectionTaskMapper detectionTaskMapper,
                                  DetectionMediaMapper detectionMediaMapper,
                                  DetectionResultMapper detectionResultMapper,
                                  DetectionResultItemMapper detectionResultItemMapper,
                                  AlgorithmClient algorithmClient,
                                  WorkOrderService workOrderService,
                                  RealtimeMessagePublisher realtimeMessagePublisher,
                                  @Qualifier("detectionTaskExecutor") TaskExecutor detectionTaskExecutor) {
        this.detectionTaskMapper = detectionTaskMapper;
        this.detectionMediaMapper = detectionMediaMapper;
        this.detectionResultMapper = detectionResultMapper;
        this.detectionResultItemMapper = detectionResultItemMapper;
        this.algorithmClient = algorithmClient;
        this.workOrderService = workOrderService;
        this.realtimeMessagePublisher = realtimeMessagePublisher;
        this.detectionTaskExecutor = detectionTaskExecutor;
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
        detectionTaskMapper.insert(entity);

        DetectionMediaEntity mediaEntity = new DetectionMediaEntity();
        mediaEntity.setTaskId(entity.getId());
        mediaEntity.setMediaType(DEFAULT_MEDIA_TYPE);
        mediaEntity.setFileName(request.fileName());
        mediaEntity.setFileUrl(request.fileUrl());
        mediaEntity.setCapturedAt(now);
        mediaEntity.setCreatedAt(now);
        detectionMediaMapper.insert(mediaEntity);

        return toTaskResponse(entity, mediaEntity, null);
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
        Map<Long, DetectionMediaEntity> mediaMap = findFirstMediaMap(taskEntities.stream()
                .map(DetectionTaskEntity::getId)
                .toList());

        List<DetectionTaskResponse> records = taskEntities.stream()
                .map(task -> toTaskResponse(task, mediaMap.get(task.getId()), null))
                .toList();

        return new PageResponse<>(records, queryPage.getTotal(), queryPage.getSize(), queryPage.getCurrent(), queryPage.getPages());
    }

    @Override
    public DetectionTaskResponse getTask(Long taskId) {
        DetectionTaskEntity taskEntity = getRequiredTask(taskId);
        DetectionMediaEntity mediaEntity = getFirstMedia(taskId);
        DetectionResultResponse result = loadResult(taskId);
        return toTaskResponse(taskEntity, mediaEntity, result);
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
        if (status != DetectionTaskStatus.PENDING && status != DetectionTaskStatus.FAILED) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待处理或失败的任务允许删除");
        }

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

    private void processTask(Long taskId) {
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
                    taskEntity.getRemark(),
                    taskEntity.getSubmittedBy(),
                    taskEntity.getCreatedAt()
            );

            publishProgress(taskId, DetectionTaskStatus.PROCESSING, 25, "图像分析开始执行");
            DetectionAnalysisResult analysisResult = algorithmClient.analyze(aggregate);
            publishProgress(taskId, DetectionTaskStatus.PROCESSING, 70, "图像分析完成，正在生成检测结果");

            Long generatedWorkOrderId = createWorkOrderIfNeeded(taskEntity, mediaEntity, analysisResult.items());
            persistSuccessfulResult(taskEntity, mediaEntity, analysisResult, generatedWorkOrderId);

            DetectionResultResponse result = getResult(taskId);
            publishProgress(taskId, DetectionTaskStatus.COMPLETED, 100, "检测任务已完成");
            realtimeMessagePublisher.publishDetectionResult(taskId, result);
        } catch (Exception exception) {
            markTaskFailed(taskId, exception.getMessage());
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
                                           DetectionAnalysisResult analysisResult,
                                           Long generatedWorkOrderId) {
        LocalDateTime now = LocalDateTime.now();
        DetectionResultEntity resultEntity = new DetectionResultEntity();
        resultEntity.setTaskId(taskEntity.getId());
        resultEntity.setSummary(analysisResult.summary());
        resultEntity.setTotalDamageCount(analysisResult.items().size());
        resultEntity.setHighestSeverity(resolveHighestSeverity(analysisResult.items()));
        resultEntity.setAvgConfidence(calculateAverageConfidence(analysisResult.items()));
        resultEntity.setGeneratedWorkOrderId(generatedWorkOrderId);
        resultEntity.setCompletedAt(now);
        resultEntity.setCreatedAt(now);
        resultEntity.setUpdatedAt(now);
        detectionResultMapper.insert(resultEntity);

        for (DetectionItemResponse item : analysisResult.items()) {
            DetectionResultItemEntity itemEntity = new DetectionResultItemEntity();
            itemEntity.setResultId(resultEntity.getId());
            itemEntity.setTaskId(taskEntity.getId());
            itemEntity.setMediaId(mediaEntity == null ? null : mediaEntity.getId());
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

        return new DetectionResultResponse(
                taskId,
                resultEntity.getSummary(),
                items,
                resultEntity.getGeneratedWorkOrderId(),
                resultEntity.getCompletedAt(),
                null
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
                com.roadcrack.api.enums.DamageType.valueOf(entity.getDamageType()),
                SeverityLevel.valueOf(entity.getSeverityLevel()),
                entity.getConfidence().doubleValue(),
                boundingBox,
                entity.getSuggestion()
        );
    }

    private DetectionTaskResponse toTaskResponse(DetectionTaskEntity taskEntity,
                                                 DetectionMediaEntity mediaEntity,
                                                 DetectionResultResponse result) {
        return new DetectionTaskResponse(
                taskEntity.getId(),
                taskEntity.getTaskCode(),
                DataSourceType.valueOf(taskEntity.getSourceType()),
                mediaEntity == null ? null : mediaEntity.getFileName(),
                mediaEntity == null ? null : mediaEntity.getFileUrl(),
                taskEntity.getLocation(),
                taskEntity.getRemark(),
                taskEntity.getSubmittedBy(),
                DetectionTaskStatus.valueOf(taskEntity.getStatus()),
                taskEntity.getFailureReason(),
                taskEntity.getCreatedAt(),
                taskEntity.getUpdatedAt(),
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

    private void publishProgress(Long taskId, DetectionTaskStatus status, int progress, String message) {
        realtimeMessagePublisher.publishDetectionProgress(
                taskId,
                new DetectionProgressMessage(taskId, status, progress, message, LocalDateTime.now())
        );
    }

    private String buildCode(LocalDate date) {
        long count = detectionTaskMapper.selectCount(new LambdaQueryWrapper<DetectionTaskEntity>()
                .ge(DetectionTaskEntity::getCreatedAt, date.atStartOfDay())
                .lt(DetectionTaskEntity::getCreatedAt, date.plusDays(1).atStartOfDay()));
        return "DT-" + date.format(CODE_DATE_FORMATTER) + "-" + String.format("%06d", count + 1);
    }
}
