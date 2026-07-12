package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.DamageType;
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
import com.roadcrack.service.client.AlgorithmClient;
import com.roadcrack.service.model.DetectionAnalysisResult;
import com.roadcrack.service.model.DetectionTaskAggregate;
import com.roadcrack.service.port.RealtimeMessagePublisher;
import com.roadcrack.service.service.AuditLogService;
import com.roadcrack.service.service.DetectionTaskService;
import com.roadcrack.service.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
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

    private static final String DEFAULT_SUBMITTED_BY = "admin";
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, DetectionTaskAggregate> store = new ConcurrentHashMap<>();
    private final AlgorithmClient algorithmClient;
    private final WorkOrderService workOrderService;
    private final RealtimeMessagePublisher realtimeMessagePublisher;
    private final TaskExecutor detectionTaskExecutor;
    private final AuditLogService auditLogService;

    public InMemoryDetectionTaskService(AlgorithmClient algorithmClient,
                                        WorkOrderService workOrderService,
                                        RealtimeMessagePublisher realtimeMessagePublisher,
                                        @Qualifier("detectionTaskExecutor") TaskExecutor detectionTaskExecutor,
                                        AuditLogService auditLogService) {
        this.algorithmClient = algorithmClient;
        this.workOrderService = workOrderService;
        this.realtimeMessagePublisher = realtimeMessagePublisher;
        this.detectionTaskExecutor = detectionTaskExecutor;
        this.auditLogService = auditLogService;
        seedTasks();
    }

    private void seedTasks() {
        // Seed some completed demo tasks so the data-screen has data to show
        LocalDateTime now = LocalDateTime.now();
        // roadId: 1=长安街, 2=东二环路, 3=东三环路, 4=朝阳路, 5=建国路, 6=通惠河北路
        record Seed(String location, DamageType type, SeverityLevel level, Long roadId) {}
        List<Seed> seeds = List.of(
                new Seed("116.397,39.909", DamageType.CRACK, SeverityLevel.HIGH, 1L),
                new Seed("116.40,39.91",  DamageType.POTHOLE, SeverityLevel.MEDIUM, 1L),
                new Seed("116.42,39.93",  DamageType.CRACK, SeverityLevel.LOW, 2L),
                new Seed("116.35,39.95",  DamageType.ROAD_SPILL, SeverityLevel.HIGH, 2L)
        );

        for (int i = 0; i < seeds.size(); i++) {
            Seed s = seeds.get(i);
            long id = idGenerator.getAndIncrement();
            LocalDateTime createdAt = now.minusDays(seeds.size() - i);
            DetectionTaskAggregate aggregate = new DetectionTaskAggregate(
                    id,
                    buildCode(id, createdAt.toLocalDate()),
                    DataSourceType.MANUAL_IMAGE,
                    "demo-image-" + id + ".jpg",
                    "/uploads/demo-image-" + id + ".jpg",
                    s.location,
                    s.roadId,
                    "演示数据：" + s.type.name() + " @ " + s.level.name(),
                    DEFAULT_SUBMITTED_BY,
                    createdAt
            );
            List<DetectionItemResponse> items = List.of(
                    new DetectionItemResponse(
                            null,
                            s.type,
                            s.level,
                            0.85 + i * 0.03,
                            new BoundingBoxResponse(120 + i * 20, 80 + i * 10, 80, 60),
                            "建议尽快安排修复处理"
                    )
            );
            DetectionResultResponse result = new DetectionResultResponse(
                    id,
                    "检测到 " + s.type.name() + "，严重等级 " + s.level.name(),
                    items,
                    null,
                    createdAt,
                    null
            );
            aggregate.markCompleted(result);
            store.put(id, aggregate);
        }
    }

    @Override
    public DetectionTaskResponse createTask(CreateDetectionTaskRequest request) {
        long id = idGenerator.getAndIncrement();
        LocalDateTime now = LocalDateTime.now();
        // 内存模式：根据坐标简单匹配道路（与 DB 模式使用相同逻辑）
        Long roadId = matchNearestRoadByCoord(request.location());
        DetectionTaskAggregate aggregate = new DetectionTaskAggregate(
                id,
                buildCode(id, now.toLocalDate()),
                request.dataSourceType(),
                request.fileName(),
                request.fileUrl(),
                request.location(),
                roadId,
                request.remark(),
                DEFAULT_SUBMITTED_BY,
                now
        );
        store.put(id, aggregate);
        return aggregate.toResponse();
    }

    /**
     * 内存模式下的道路匹配：根据坐标匹配预置的 6 条北京道路
     */
    private Long matchNearestRoadByCoord(String location) {
        if (location == null || location.isBlank()) return null;
        double[] coords = parseCoord(location);
        if (coords == null) return null;

        // 预置道路中心点：(roadId, lng, lat)
        record RoadCoord(long id, double lng, double lat) {}
        List<RoadCoord> roads = List.of(
            new RoadCoord(1L, 116.4075, 39.9070),  // 长安街
            new RoadCoord(2L, 116.4350, 39.9150),  // 东二环路
            new RoadCoord(3L, 116.4550, 39.9150),  // 东三环路
            new RoadCoord(4L, 116.4600, 39.9200),  // 朝阳路
            new RoadCoord(5L, 116.4500, 39.9080),  // 建国路
            new RoadCoord(6L, 116.4480, 39.9000)   // 通惠河北路
        );

        long nearestId = 0;
        double minDist = Double.MAX_VALUE;
        for (RoadCoord r : roads) {
            double dist = haversineDist(coords[0], coords[1], r.lng, r.lat);
            if (dist < minDist) {
                minDist = dist;
                nearestId = r.id;
            }
        }
        return nearestId > 0 ? nearestId : null;
    }

    private double haversineDist(double lng1, double lat1, double lng2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double[] parseCoord(String location) {
        if (location == null || location.isBlank()) return null;
        String[] parts = location.split(",");
        if (parts.length != 2) return null;
        try {
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
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
        if (aggregate.getStatus() != DetectionTaskStatus.PENDING && aggregate.getStatus() != DetectionTaskStatus.FAILED
                && aggregate.getStatus() != DetectionTaskStatus.COMPLETED) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待处理、失败或已完成的任务允许删除");
        }
        store.remove(taskId);
    }

    @Override
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
            DetectionTaskAggregate aggregate = store.get(taskId);
            if (aggregate == null) continue;
            taskCodes.add(aggregate.getTaskCode() != null ? aggregate.getTaskCode() : "DT-" + taskId);

            DetectionResultResponse result = aggregate.getResult();
            if (result == null) continue;

            SeverityLevel newLevel;
            if ("NORMAL".equals(upperSeverity)) {
                // 设为"无病害"：清空所有病害项
                DetectionResultResponse updatedResult = new DetectionResultResponse(
                        taskId,
                        "经人工复核，此路段无病害",
                        List.of(),
                        result.generatedWorkOrderId(),
                        result.completedAt(),
                        result.imageBase64()
                );
                aggregate.markCompleted(updatedResult);
                updatedCount++;
            } else {
                newLevel = SeverityLevel.valueOf(upperSeverity);
                // 更新所有病害项的严重等级
                List<DetectionItemResponse> updatedItems = new java.util.ArrayList<>();
                for (DetectionItemResponse item : result.items()) {
                    updatedItems.add(new DetectionItemResponse(
                            item.id(),
                            item.damageType(),
                            newLevel,
                            item.confidence(),
                            item.boundingBox(),
                            item.suggestion()
                    ));
                }
                DetectionResultResponse updatedResult = new DetectionResultResponse(
                        taskId,
                        result.summary(),
                        updatedItems,
                        result.generatedWorkOrderId(),
                        result.completedAt(),
                        result.imageBase64()
                );
                aggregate.markCompleted(updatedResult);
                updatedCount++;
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
    public void batchDeleteTasks(List<Long> taskIds, String operator) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择至少一条检测结果");
        }

        int deletedCount = 0;
        List<String> taskCodes = new java.util.ArrayList<>();
        StringBuilder errors = new StringBuilder();

        for (Long taskId : taskIds) {
            DetectionTaskAggregate aggregate = store.get(taskId);
            if (aggregate == null) {
                errors.append("任务 ID=").append(taskId).append(" 不存在; ");
                continue;
            }
            DetectionTaskStatus status = aggregate.getStatus();
            if (status != DetectionTaskStatus.PENDING && status != DetectionTaskStatus.FAILED
                    && status != DetectionTaskStatus.COMPLETED) {
                errors.append("任务 ").append(aggregate.getTaskCode()).append(" 状态不允许删除; ");
                continue;
            }
            taskCodes.add(aggregate.getTaskCode() != null ? aggregate.getTaskCode() : "DT-" + taskId);
            store.remove(taskId);
            deletedCount++;
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

    private void processTask(DetectionTaskAggregate aggregate) {
        Long taskId = aggregate.getId();
        try {
            publishProgress(taskId, DetectionTaskStatus.PROCESSING, 25, "图像分析开始执行");
            DetectionAnalysisResult analysisResult = algorithmClient.analyze(aggregate);
            publishProgress(taskId, DetectionTaskStatus.PROCESSING, 70, "图像分析完成，正在生成检测结果");

            Long generatedWorkOrderId = createWorkOrderIfNeeded(taskId, aggregate, analysisResult.items());
            DetectionResultResponse result = new DetectionResultResponse(
                    taskId,
                    analysisResult.summary(),
                    analysisResult.items(),
                    generatedWorkOrderId,
                    LocalDateTime.now(),
                    analysisResult.imageBase64()
            );

            aggregate.markCompleted(result);
            publishProgress(taskId, DetectionTaskStatus.COMPLETED, 100, "检测任务已完成");
            realtimeMessagePublisher.publishDetectionResult(taskId, result);
        } catch (Exception exception) {
            aggregate.markFailed(exception.getMessage());
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
}
