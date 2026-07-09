package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.response.detection.DetectionItemResponse;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.api.response.map.MapDamageTypeRatioResponse;
import com.roadcrack.api.response.map.MapMarkerDetailResponse;
import com.roadcrack.api.response.map.MapMarkerResponse;
import com.roadcrack.api.response.map.MapStatisticsResponse;
import com.roadcrack.api.response.map.MapTrendPointResponse;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.service.service.DetectionTaskService;
import com.roadcrack.service.service.MapDataService;
import com.roadcrack.service.service.WorkOrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryMapDataService implements MapDataService {

    private static final int QUERY_SIZE = 10_000;

    private final DetectionTaskService detectionTaskService;
    private final WorkOrderService workOrderService;

    public InMemoryMapDataService(DetectionTaskService detectionTaskService,
                                  WorkOrderService workOrderService) {
        this.detectionTaskService = detectionTaskService;
        this.workOrderService = workOrderService;
    }

    @Override
    public List<MapMarkerResponse> listMarkers(DamageType damageType,
                                               SeverityLevel severityLevel,
                                               WorkOrderStatus status,
                                               Boolean hasWorkOrder,
                                               Boolean onlyWithCoordinates,
                                               String keyword) {
        return loadMarkerViews().stream()
                .filter(view -> damageType == null || view.damageType() == damageType)
                .filter(view -> severityLevel == null || view.severityLevel() == severityLevel)
                .filter(view -> status == null || view.status() == status)
                .filter(view -> hasWorkOrder == null || view.hasWorkOrder() == hasWorkOrder)
                .filter(view -> !Boolean.TRUE.equals(onlyWithCoordinates) || view.hasCoordinates())
                .filter(view -> keyword == null || keyword.isBlank() || matchesKeyword(view, keyword))
                .sorted(Comparator.comparing(MarkerView::detectedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(MarkerView::id, Comparator.reverseOrder()))
                .map(this::toMarkerResponse)
                .toList();
    }

    @Override
    public MapMarkerDetailResponse getMarkerDetail(Long markerId) {
        MarkerView markerView = loadMarkerViews().stream()
                .filter(view -> view.id().equals(markerId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "地图标记不存在: " + markerId));
        return toMarkerDetailResponse(markerView);
    }

    @Override
    public MapStatisticsResponse getStatistics() {
        List<MarkerView> views = loadMarkerViews();
        List<WorkOrderResponse> workOrders = loadWorkOrders();
        LocalDate today = LocalDate.now();

        long highSeverityCount = views.stream()
                .filter(view -> view.severityLevel() == SeverityLevel.HIGH)
                .count();
        long todayDamageCount = views.stream()
                .map(MarkerView::detectedAt)
                .filter(Objects::nonNull)
                .filter(detectedAt -> detectedAt.toLocalDate().equals(today))
                .count();
        long pendingWorkOrderCount = countByStatus(workOrders, WorkOrderStatus.PENDING_ASSIGNMENT);
        long processingWorkOrderCount = workOrders.stream()
                .filter(workOrder -> workOrder.status() == WorkOrderStatus.ASSIGNED
                        || workOrder.status() == WorkOrderStatus.IN_PROGRESS)
                .count();

        return new MapStatisticsResponse(
                views.size(),
                views.stream().filter(MarkerView::hasCoordinates).count(),
                highSeverityCount,
                todayDamageCount,
                workOrders.size(),
                pendingWorkOrderCount,
                processingWorkOrderCount,
                countByStatus(workOrders, WorkOrderStatus.COMPLETED),
                countByStatus(workOrders, WorkOrderStatus.CLOSED),
                countByStatus(workOrders, WorkOrderStatus.CANCELLED)
        );
    }

    @Override
    public List<MapTrendPointResponse> getTrend(int days) {
        int safeDays = Math.max(1, Math.min(days, 30));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(safeDays - 1L);

        Map<LocalDate, Long> countByDate = loadMarkerViews().stream()
                .map(MarkerView::detectedAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .filter(date -> !date.isBefore(start) && !date.isAfter(end))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return java.util.stream.IntStream.range(0, safeDays)
                .mapToObj(index -> start.plusDays(index))
                .map(date -> new MapTrendPointResponse(date, countByDate.getOrDefault(date, 0L)))
                .toList();
    }

    @Override
    public List<MapDamageTypeRatioResponse> getDamageTypeRatios() {
        List<MarkerView> views = loadMarkerViews();
        long total = views.size();
        if (total == 0) {
            return List.of();
        }

        return views.stream()
                .collect(Collectors.groupingBy(MarkerView::damageType, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<DamageType, Long>comparingByValue().reversed())
                .map(entry -> new MapDamageTypeRatioResponse(
                        entry.getKey(),
                        MapDataSupport.damageTypeLabel(entry.getKey()),
                        entry.getValue(),
                        MapDataSupport.ratio(entry.getValue(), total)
                ))
                .toList();
    }

    private List<MarkerView> loadMarkerViews() {
        List<DetectionTaskResponse> tasks = detectionTaskService.listTasks(1, QUERY_SIZE, null, null, null, null).records();
        Map<Long, WorkOrderResponse> workOrderMap = loadWorkOrders().stream()
                .filter(workOrder -> workOrder.detectionTaskId() != null)
                .collect(Collectors.toMap(
                        WorkOrderResponse::detectionTaskId,
                        Function.identity(),
                        (left, right) -> left.createdAt().isAfter(right.createdAt()) ? left : right
                ));

        return tasks.stream()
                .filter(task -> task.result() != null)
                .flatMap(task -> buildViewsFromTask(task, workOrderMap.get(task.id())).stream())
                .toList();
    }

    private List<MarkerView> buildViewsFromTask(DetectionTaskResponse task, WorkOrderResponse workOrder) {
        DetectionResultResponse result = task.result();
        if (result == null || result.items() == null || result.items().isEmpty()) {
            return List.of();
        }

        boolean generatedWorkOrder = result.generatedWorkOrderId() != null;
        DepartmentCode departmentCode = workOrder == null ? null : workOrder.departmentCode();
        String departmentName = workOrder == null
                ? MapDataSupport.departmentLabel(null)
                : MapDataSupport.departmentLabel(departmentCode);
        List<DetectionItemResponse> items = result.items();

        return java.util.stream.IntStream.range(0, items.size())
                .mapToObj(index -> {
                    DetectionItemResponse item = items.get(index);
                    long markerId = task.id() * 1_000L + index + 1L;
                    BigDecimal lng = MapDataSupport.demoLng(task.id(), index);
                    BigDecimal lat = MapDataSupport.demoLat(task.id(), index);
                    return new MarkerView(
                            markerId,
                            task.id(),
                            task.taskCode(),
                            task.location(),
                            lng,
                            lat,
                            true,
                            item.damageType(),
                            item.severityLevel(),
                            item.confidence(),
                            item.suggestion(),
                            task.fileUrl(),
                            result.summary(),
                            workOrder != null,
                            generatedWorkOrder,
                            workOrder == null ? null : workOrder.id(),
                            workOrder == null ? null : workOrder.workOrderCode(),
                            workOrder == null ? null : workOrder.status(),
                            departmentCode,
                            departmentName,
                            workOrder == null ? null : workOrder.assignee(),
                            result.completedAt(),
                            workOrder == null ? null : workOrder.createdAt(),
                            workOrder == null ? null : workOrder.dueAt()
                    );
                })
                .toList();
    }

    private List<WorkOrderResponse> loadWorkOrders() {
        return workOrderService.listWorkOrders(1, QUERY_SIZE, null, null, null, null, null).records();
    }

    private boolean matchesKeyword(MarkerView view, String keyword) {
        return MapDataSupport.containsIgnoreCase(view.location(), keyword)
                || MapDataSupport.containsIgnoreCase(view.taskCode(), keyword)
                || MapDataSupport.containsIgnoreCase(view.departmentName(), keyword)
                || MapDataSupport.containsIgnoreCase(MapDataSupport.damageTypeLabel(view.damageType()), keyword);
    }

    private long countByStatus(List<WorkOrderResponse> workOrders, WorkOrderStatus status) {
        return workOrders.stream().filter(workOrder -> workOrder.status() == status).count();
    }

    private MapMarkerResponse toMarkerResponse(MarkerView view) {
        return new MapMarkerResponse(
                view.id(),
                view.taskId(),
                view.taskCode(),
                view.location(),
                view.lng(),
                view.lat(),
                view.hasCoordinates(),
                view.damageType(),
                MapDataSupport.damageTypeLabel(view.damageType()),
                view.severityLevel(),
                MapDataSupport.severityLabel(view.severityLevel()),
                view.status(),
                MapDataSupport.statusLabel(view.status()),
                view.hasWorkOrder(),
                view.generatedWorkOrder(),
                view.workOrderId(),
                view.workOrderCode(),
                view.departmentCode(),
                view.departmentName(),
                view.detectedAt()
        );
    }

    private MapMarkerDetailResponse toMarkerDetailResponse(MarkerView view) {
        return new MapMarkerDetailResponse(
                view.id(),
                view.taskId(),
                view.taskCode(),
                view.location(),
                view.lng(),
                view.lat(),
                view.hasCoordinates(),
                view.damageType(),
                MapDataSupport.damageTypeLabel(view.damageType()),
                view.severityLevel(),
                MapDataSupport.severityLabel(view.severityLevel()),
                view.confidence(),
                view.suggestion(),
                view.snapshotUrl(),
                view.detectionSummary(),
                view.workOrderId(),
                view.workOrderCode(),
                view.hasWorkOrder(),
                view.generatedWorkOrder(),
                view.status(),
                MapDataSupport.statusLabel(view.status()),
                view.departmentCode(),
                view.departmentName(),
                view.assignee(),
                view.detectedAt(),
                view.workOrderCreatedAt(),
                view.workOrderDueAt()
        );
    }

    private record MarkerView(
            Long id,
            Long taskId,
            String taskCode,
            String location,
            BigDecimal lng,
            BigDecimal lat,
            boolean hasCoordinates,
            DamageType damageType,
            SeverityLevel severityLevel,
            Double confidence,
            String suggestion,
            String snapshotUrl,
            String detectionSummary,
            boolean hasWorkOrder,
            boolean generatedWorkOrder,
            Long workOrderId,
            String workOrderCode,
            WorkOrderStatus status,
            DepartmentCode departmentCode,
            String departmentName,
            String assignee,
            LocalDateTime detectedAt,
            LocalDateTime workOrderCreatedAt,
            LocalDateTime workOrderDueAt
    ) {
    }
}
