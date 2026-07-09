package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import com.roadcrack.api.response.detection.DetectionItemResponse;
import com.roadcrack.api.response.detection.DetectionProgressMessage;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.api.response.websocket.AlertMessageResponse;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.service.client.AlgorithmClient;
import com.roadcrack.service.model.AuditLogRecord;
import com.roadcrack.service.model.DetectionAnalysisResult;
import com.roadcrack.service.model.DetectionTaskAggregate;
import com.roadcrack.service.port.RealtimeMessagePublisher;
import com.roadcrack.service.service.AuditLogService;
import com.roadcrack.service.service.DetectionTaskService;
import com.roadcrack.service.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryDetectionTaskService implements DetectionTaskService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDetectionTaskService.class);
    private static final String MODULE_DETECTION_TASK = "DETECTION_TASK";
    private static final String DEFAULT_SUBMITTED_BY = "admin";
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, DetectionTaskAggregate> store = new ConcurrentHashMap<>();
    private final AlgorithmClient algorithmClient;
    private final WorkOrderService workOrderService;
    private final AuditLogService auditLogService;
    private final RealtimeMessagePublisher realtimeMessagePublisher;
    private final TaskExecutor detectionTaskExecutor;

    public InMemoryDetectionTaskService(AlgorithmClient algorithmClient,
                                        WorkOrderService workOrderService,
                                        AuditLogService auditLogService,
                                        RealtimeMessagePublisher realtimeMessagePublisher,
                                        @Qualifier("detectionTaskExecutor") TaskExecutor detectionTaskExecutor) {
        this.algorithmClient = algorithmClient;
        this.workOrderService = workOrderService;
        this.auditLogService = auditLogService;
        this.realtimeMessagePublisher = realtimeMessagePublisher;
        this.detectionTaskExecutor = detectionTaskExecutor;
    }

    @Override
    public DetectionTaskResponse createTask(CreateDetectionTaskRequest request) {
        long id = idGenerator.getAndIncrement();
        LocalDateTime now = LocalDateTime.now();
        DetectionTaskAggregate aggregate = new DetectionTaskAggregate(
                id,
                buildCode(id, now.toLocalDate()),
                request.dataSourceType(),
                request.fileName(),
                request.fileUrl(),
                request.location(),
                request.remark(),
                DEFAULT_SUBMITTED_BY,
                now
        );
        store.put(id, aggregate);
        log.info("Detection task created in memory: taskId={}, taskCode={}, sourceType={}, fileName={}, location={}",
                aggregate.getId(),
                aggregate.getTaskCode(),
                aggregate.getDataSourceType(),
                aggregate.getFileName(),
                aggregate.getLocation());
        auditLogService.record(AuditLogRecord.success(
                        MODULE_DETECTION_TASK,
                        "CREATE",
                        "Created detection task " + aggregate.getTaskCode())
                .setUsername(aggregate.getSubmittedBy())
                .setParams(buildTaskParams(
                        aggregate.getId(),
                        aggregate.getTaskCode(),
                        aggregate.getFileName(),
                        aggregate.getLocation()))
                .setCreateTime(now));
        return aggregate.toResponse();
    }

    @Override
    public void executeTask(Long taskId) {
        DetectionTaskAggregate aggregate = getRequired(taskId);
        if (aggregate.getStatus() == DetectionTaskStatus.PROCESSING) {
            throw new BusinessException(ResultCode.CONFLICT, "任务正在处理中，不能重复提交");
        }
        if (aggregate.getStatus() == DetectionTaskStatus.COMPLETED) {
            throw new BusinessException(ResultCode.CONFLICT, "任务已完成，如需重跑请新建任务");
        }

        aggregate.markProcessing();
        log.info("Detection task queued in memory: taskId={}, taskCode={}", aggregate.getId(), aggregate.getTaskCode());
        publishProgress(taskId, DetectionTaskStatus.PROCESSING, 5, "任务已进入检测队列");
        detectionTaskExecutor.execute(() -> processTask(aggregate));
    }

    @Override
    public PageResponse<DetectionTaskResponse> listTasks(int page,
                                                         int size,
                                                         DetectionTaskStatus status,
                                                         DataSourceType dataSourceType,
                                                         String location,
                                                         String submittedBy) {
        List<DetectionTaskResponse> filtered = store.values().stream()
                .map(DetectionTaskAggregate::toResponse)
                .filter(item -> status == null || item.status() == status)
                .filter(item -> dataSourceType == null || item.dataSourceType() == dataSourceType)
                .filter(item -> location == null || containsIgnoreCase(item.location(), location))
                .filter(item -> submittedBy == null || containsIgnoreCase(item.submittedBy(), submittedBy))
                .sorted(Comparator.comparing(DetectionTaskResponse::createdAt).reversed())
                .toList();
        return paginate(filtered, page, size);
    }

    @Override
    public DetectionTaskResponse getTask(Long taskId) {
        return getRequired(taskId).toResponse();
    }

    @Override
    public DetectionResultResponse getResult(Long taskId) {
        DetectionTaskAggregate aggregate = getRequired(taskId);
        if (aggregate.getResult() == null) {
            throw new BusinessException(ResultCode.CONFLICT, "检测结果尚未生成");
        }
        return aggregate.getResult();
    }

    @Override
    public void deleteTask(Long taskId) {
        DetectionTaskAggregate aggregate = getRequired(taskId);
        if (aggregate.getStatus() != DetectionTaskStatus.PENDING && aggregate.getStatus() != DetectionTaskStatus.FAILED) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待处理或失败的任务允许删除");
        }
        store.remove(taskId);
    }

    private void processTask(DetectionTaskAggregate aggregate) {
        Long taskId = aggregate.getId();
        long startTime = System.currentTimeMillis();
        try {
            log.info("Detection task started in memory: taskId={}, taskCode={}", taskId, aggregate.getTaskCode());
            publishProgress(taskId, DetectionTaskStatus.PROCESSING, 25, "图像分析开始执行");
            DetectionAnalysisResult analysisResult = algorithmClient.analyze(aggregate);
            publishProgress(taskId, DetectionTaskStatus.PROCESSING, 70, "图像分析完成，正在生成检测结果");

            Long generatedWorkOrderId = createWorkOrderIfNeeded(taskId, aggregate, analysisResult.items());
            DetectionResultResponse result = new DetectionResultResponse(
                    taskId,
                    analysisResult.summary(),
                    analysisResult.items(),
                    generatedWorkOrderId,
                    LocalDateTime.now()
            );

            aggregate.markCompleted(result);
            log.info("Detection task completed in memory: taskId={}, taskCode={}, itemCount={}, generatedWorkOrderId={}, durationMs={}",
                    taskId,
                    aggregate.getTaskCode(),
                    analysisResult.items().size(),
                    generatedWorkOrderId,
                    System.currentTimeMillis() - startTime);
            auditLogService.record(AuditLogRecord.success(
                            MODULE_DETECTION_TASK,
                            "COMPLETE",
                            "Completed detection task " + aggregate.getTaskCode())
                    .setUsername(aggregate.getSubmittedBy())
                    .setParams(buildCompletionParams(
                            taskId,
                            aggregate.getTaskCode(),
                            analysisResult.items().size(),
                            generatedWorkOrderId))
                    .setCostTime(System.currentTimeMillis() - startTime));
            publishProgress(taskId, DetectionTaskStatus.COMPLETED, 100, "检测任务已完成");
            realtimeMessagePublisher.publishDetectionResult(taskId, result);
        } catch (Exception exception) {
            aggregate.markFailed(exception.getMessage());
            log.error("Detection task failed in memory: taskId={}, taskCode={}, durationMs={}",
                    taskId,
                    aggregate.getTaskCode(),
                    System.currentTimeMillis() - startTime,
                    exception);
            auditLogService.record(AuditLogRecord.failure(
                            MODULE_DETECTION_TASK,
                            "FAIL",
                            "Detection task failed: " + aggregate.getTaskCode(),
                            exception.getMessage())
                    .setUsername(aggregate.getSubmittedBy())
                    .setParams(buildFailureParams(taskId, aggregate.getTaskCode(), aggregate.getLocation()))
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

    private Long createWorkOrderIfNeeded(Long taskId,
                                         DetectionTaskAggregate aggregate,
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

        log.info("Creating work order from detection task in memory: taskId={}, taskCode={}, damageType={}, severity={}",
                taskId,
                aggregate.getTaskCode(),
                topItem.damageType(),
                topItem.severityLevel());
        WorkOrderResponse workOrder = workOrderService.createFromDetection(
                taskId,
                topItem.damageType(),
                topItem.severityLevel(),
                aggregate.getLocation(),
                aggregate.getFileUrl()
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

    private String buildFailureParams(Long taskId, String taskCode, String location) {
        return "taskId=" + taskId
                + ", taskCode=" + safeValue(taskCode)
                + ", location=" + safeValue(location);
    }

    private DetectionTaskAggregate getRequired(Long taskId) {
        DetectionTaskAggregate aggregate = store.get(taskId);
        if (aggregate == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检测任务不存在: " + taskId);
        }
        return aggregate;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }

    private PageResponse<DetectionTaskResponse> paginate(List<DetectionTaskResponse> items, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, items.size());
        int toIndex = Math.min(fromIndex + safeSize, items.size());
        long total = items.size();
        long pages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;
        return new PageResponse<>(items.subList(fromIndex, toIndex), total, safeSize, safePage, pages);
    }

    private String buildCode(long id, LocalDate date) {
        return "DT-" + date.format(CODE_DATE_FORMATTER) + "-" + String.format("%06d", id);
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
