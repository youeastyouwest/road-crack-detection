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

    public InMemoryDetectionTaskService(AlgorithmClient algorithmClient,
                                        WorkOrderService workOrderService,
                                        RealtimeMessagePublisher realtimeMessagePublisher,
                                        @Qualifier("detectionTaskExecutor") TaskExecutor detectionTaskExecutor) {
        this.algorithmClient = algorithmClient;
        this.workOrderService = workOrderService;
        this.realtimeMessagePublisher = realtimeMessagePublisher;
        this.detectionTaskExecutor = detectionTaskExecutor;
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
        if (aggregate.getStatus() != DetectionTaskStatus.PENDING && aggregate.getStatus() != DetectionTaskStatus.FAILED) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待处理或失败的任务允许删除");
        }
        store.remove(taskId);
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
