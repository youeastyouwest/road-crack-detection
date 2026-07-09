package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.response.map.MapDamageTypeRatioResponse;
import com.roadcrack.api.response.map.MapMarkerDetailResponse;
import com.roadcrack.api.response.map.MapMarkerResponse;
import com.roadcrack.api.response.map.MapStatisticsResponse;
import com.roadcrack.api.response.map.MapTrendPointResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.DepartmentEntity;
import com.roadcrack.dao.entity.DetectionResultEntity;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.entity.WorkOrderEntity;
import com.roadcrack.dao.mapper.DepartmentMapper;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionResultMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.WorkOrderMapper;
import com.roadcrack.service.service.MapDataService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbMapDataService implements MapDataService {

    private final DetectionResultItemMapper detectionResultItemMapper;
    private final DetectionResultMapper detectionResultMapper;
    private final DetectionTaskMapper detectionTaskMapper;
    private final WorkOrderMapper workOrderMapper;
    private final DepartmentMapper departmentMapper;

    public DbMapDataService(DetectionResultItemMapper detectionResultItemMapper,
                            DetectionResultMapper detectionResultMapper,
                            DetectionTaskMapper detectionTaskMapper,
                            WorkOrderMapper workOrderMapper,
                            DepartmentMapper departmentMapper) {
        this.detectionResultItemMapper = detectionResultItemMapper;
        this.detectionResultMapper = detectionResultMapper;
        this.detectionTaskMapper = detectionTaskMapper;
        this.workOrderMapper = workOrderMapper;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public List<MapMarkerResponse> listMarkers(DamageType damageType,
                                               SeverityLevel severityLevel,
                                               WorkOrderStatus status,
                                               Boolean hasWorkOrder,
                                               Boolean onlyWithCoordinates,
                                               String keyword) {
        List<MarkerView> views = loadMarkerViews(damageType, severityLevel);
        return views.stream()
                .filter(view -> status == null || view.status() == status)
                .filter(view -> hasWorkOrder == null || view.hasWorkOrder() == hasWorkOrder)
                .filter(view -> !Boolean.TRUE.equals(onlyWithCoordinates) || view.hasCoordinates())
                .filter(view -> keyword == null || keyword.isBlank() || matchesKeyword(view, keyword))
                .sorted(Comparator.comparing(MarkerView::detectedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(MarkerView::id, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toMarkerResponse)
                .toList();
    }

    @Override
    public MapMarkerDetailResponse getMarkerDetail(Long markerId) {
        DetectionResultItemEntity itemEntity = detectionResultItemMapper.selectById(markerId);
        if (itemEntity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "地图标记不存在: " + markerId);
        }
        MarkerView view = buildMarkerView(
                itemEntity,
                singleTaskMap(itemEntity.getTaskId()),
                singleResultMap(itemEntity.getResultId()),
                loadLatestWorkOrderMap(List.of(itemEntity.getTaskId())),
                loadWorkOrderByIdMap(itemEntity.getTaskId()),
                loadDepartmentNameMap()
        );
        return toMarkerDetailResponse(view);
    }

    @Override
    public MapStatisticsResponse getStatistics() {
        List<DetectionResultItemEntity> items = detectionResultItemMapper.selectList(
                new LambdaQueryWrapper<DetectionResultItemEntity>().orderByDesc(DetectionResultItemEntity::getDetectedAt)
        );
        List<WorkOrderEntity> workOrders = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrderEntity>());

        long mappedPointCount = items.stream()
                .filter(this::hasRenderableCoordinates)
                .count();
        long highSeverityCount = items.stream()
                .filter(item -> SeverityLevel.HIGH.name().equals(item.getSeverityLevel()))
                .count();
        LocalDate today = LocalDate.now();
        long todayDamageCount = items.stream()
                .map(DetectionResultItemEntity::getDetectedAt)
                .filter(Objects::nonNull)
                .filter(detectedAt -> detectedAt.toLocalDate().equals(today))
                .count();
        long pendingWorkOrderCount = countByStatus(workOrders, WorkOrderStatus.PENDING_ASSIGNMENT);
        long processingWorkOrderCount = workOrders.stream()
                .filter(workOrder -> WorkOrderStatus.ASSIGNED.name().equals(workOrder.getStatus())
                        || WorkOrderStatus.IN_PROGRESS.name().equals(workOrder.getStatus()))
                .count();
        long completedWorkOrderCount = countByStatus(workOrders, WorkOrderStatus.COMPLETED);
        long closedWorkOrderCount = countByStatus(workOrders, WorkOrderStatus.CLOSED);
        long cancelledWorkOrderCount = countByStatus(workOrders, WorkOrderStatus.CANCELLED);

        return new MapStatisticsResponse(
                items.size(),
                mappedPointCount,
                highSeverityCount,
                todayDamageCount,
                workOrders.size(),
                pendingWorkOrderCount,
                processingWorkOrderCount,
                completedWorkOrderCount,
                closedWorkOrderCount,
                cancelledWorkOrderCount
        );
    }

    @Override
    public List<MapTrendPointResponse> getTrend(int days) {
        int safeDays = Math.max(1, Math.min(days, 30));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(safeDays - 1L);

        Map<LocalDate, Long> countByDate = detectionResultItemMapper.selectList(
                        new LambdaQueryWrapper<DetectionResultItemEntity>()
                                .ge(DetectionResultItemEntity::getDetectedAt, start.atStartOfDay())
                                .lt(DetectionResultItemEntity::getDetectedAt, end.plusDays(1).atStartOfDay()))
                .stream()
                .map(DetectionResultItemEntity::getDetectedAt)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));

        List<MapTrendPointResponse> result = new ArrayList<>(safeDays);
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = start.plusDays(i);
            result.add(new MapTrendPointResponse(date, countByDate.getOrDefault(date, 0L)));
        }
        return result;
    }

    @Override
    public List<MapDamageTypeRatioResponse> getDamageTypeRatios() {
        List<DetectionResultItemEntity> items = detectionResultItemMapper.selectList(new LambdaQueryWrapper<DetectionResultItemEntity>());
        long total = items.size();
        if (total == 0) {
            return List.of();
        }
        Map<DamageType, Long> countByType = items.stream()
                .map(item -> DamageType.valueOf(item.getDamageType()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return countByType.entrySet().stream()
                .sorted(Map.Entry.<DamageType, Long>comparingByValue().reversed())
                .map(entry -> new MapDamageTypeRatioResponse(
                        entry.getKey(),
                        MapDataSupport.damageTypeLabel(entry.getKey()),
                        entry.getValue(),
                        MapDataSupport.ratio(entry.getValue(), total)
                ))
                .toList();
    }

    private List<MarkerView> loadMarkerViews(DamageType damageType, SeverityLevel severityLevel) {
        LambdaQueryWrapper<DetectionResultItemEntity> wrapper = new LambdaQueryWrapper<DetectionResultItemEntity>()
                .orderByDesc(DetectionResultItemEntity::getDetectedAt)
                .orderByDesc(DetectionResultItemEntity::getId);
        if (damageType != null) {
            wrapper.eq(DetectionResultItemEntity::getDamageType, damageType.name());
        }
        if (severityLevel != null) {
            wrapper.eq(DetectionResultItemEntity::getSeverityLevel, severityLevel.name());
        }

        List<DetectionResultItemEntity> items = detectionResultItemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, DetectionTaskEntity> taskMap = loadTaskMap(items.stream()
                .map(DetectionResultItemEntity::getTaskId)
                .toList());
        Map<Long, DetectionResultEntity> resultMap = loadResultMap(items.stream()
                .map(DetectionResultItemEntity::getResultId)
                .toList());
        Map<Long, WorkOrderEntity> latestWorkOrderMap = loadLatestWorkOrderMap(items.stream()
                .map(DetectionResultItemEntity::getTaskId)
                .toList());
        Map<Long, WorkOrderEntity> workOrderByIdMap = loadWorkOrderByIdMap(items.stream()
                .map(DetectionResultItemEntity::getTaskId)
                .toList());
        Map<String, String> departmentNameMap = loadDepartmentNameMap();

        return items.stream()
                .map(item -> buildMarkerView(item, taskMap, resultMap, latestWorkOrderMap, workOrderByIdMap, departmentNameMap))
                .toList();
    }

    private Map<Long, DetectionTaskEntity> singleTaskMap(Long taskId) {
        DetectionTaskEntity taskEntity = detectionTaskMapper.selectById(taskId);
        if (taskEntity == null) {
            return Collections.emptyMap();
        }
        return Map.of(taskId, taskEntity);
    }

    private Map<Long, DetectionResultEntity> singleResultMap(Long resultId) {
        DetectionResultEntity resultEntity = detectionResultMapper.selectById(resultId);
        if (resultEntity == null) {
            return Collections.emptyMap();
        }
        return Map.of(resultId, resultEntity);
    }

    private Map<Long, DetectionTaskEntity> loadTaskMap(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return detectionTaskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(DetectionTaskEntity::getId, Function.identity()));
    }

    private Map<Long, DetectionResultEntity> loadResultMap(Collection<Long> resultIds) {
        if (resultIds == null || resultIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return detectionResultMapper.selectBatchIds(resultIds).stream()
                .collect(Collectors.toMap(DetectionResultEntity::getId, Function.identity()));
    }

    private Map<Long, WorkOrderEntity> loadLatestWorkOrderMap(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrderEntity>()
                        .in(WorkOrderEntity::getDetectionTaskId, taskIds)
                        .orderByDesc(WorkOrderEntity::getCreatedAt)
                        .orderByDesc(WorkOrderEntity::getId))
                .stream()
                .filter(workOrder -> workOrder.getDetectionTaskId() != null)
                .collect(Collectors.toMap(
                        WorkOrderEntity::getDetectionTaskId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, WorkOrderEntity> loadWorkOrderByIdMap(Long taskId) {
        return loadWorkOrderByIdMap(List.of(taskId));
    }

    private Map<Long, WorkOrderEntity> loadWorkOrderByIdMap(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrderEntity>()
                        .in(WorkOrderEntity::getDetectionTaskId, taskIds))
                .stream()
                .collect(Collectors.toMap(WorkOrderEntity::getId, Function.identity(), (left, right) -> left));
    }

    private Map<String, String> loadDepartmentNameMap() {
        return departmentMapper.selectList(new LambdaQueryWrapper<DepartmentEntity>()).stream()
                .collect(Collectors.toMap(DepartmentEntity::getCode, DepartmentEntity::getName, (left, right) -> left));
    }

    private MarkerView buildMarkerView(DetectionResultItemEntity item,
                                       Map<Long, DetectionTaskEntity> taskMap,
                                       Map<Long, DetectionResultEntity> resultMap,
                                       Map<Long, WorkOrderEntity> latestWorkOrderMap,
                                       Map<Long, WorkOrderEntity> workOrderByIdMap,
                                       Map<String, String> departmentNameMap) {
        DetectionTaskEntity taskEntity = taskMap.get(item.getTaskId());
        DetectionResultEntity resultEntity = resultMap.get(item.getResultId());
        WorkOrderEntity workOrderEntity = resolveWorkOrder(item.getTaskId(), resultEntity, latestWorkOrderMap, workOrderByIdMap);

        DamageType damageType = DamageType.valueOf(item.getDamageType());
        SeverityLevel severityLevel = SeverityLevel.valueOf(item.getSeverityLevel());
        WorkOrderStatus workOrderStatus = workOrderEntity == null ? null : WorkOrderStatus.valueOf(workOrderEntity.getStatus());
        DepartmentCode departmentCode = workOrderEntity == null || workOrderEntity.getDepartmentCode() == null
                ? null
                : DepartmentCode.valueOf(workOrderEntity.getDepartmentCode());

        java.math.BigDecimal lng = resolveLng(item);
        java.math.BigDecimal lat = resolveLat(item);
        boolean hasCoordinates = lng != null && lat != null;
        boolean hasWorkOrder = workOrderEntity != null;
        boolean generatedWorkOrder = resultEntity != null
                && resultEntity.getGeneratedWorkOrderId() != null
                && workOrderEntity != null
                && resultEntity.getGeneratedWorkOrderId().equals(workOrderEntity.getId());

        String location = MapDataSupport.firstNonBlank(
                workOrderEntity == null ? null : workOrderEntity.getLocation(),
                taskEntity == null ? null : taskEntity.getLocation()
        );
        String departmentName = departmentCode == null
                ? MapDataSupport.departmentLabel(null)
                : departmentNameMap.getOrDefault(departmentCode.name(), MapDataSupport.departmentLabel(departmentCode));

        return new MarkerView(
                item.getId(),
                item.getTaskId(),
                taskEntity == null ? null : taskEntity.getTaskCode(),
                location,
                lng,
                lat,
                hasCoordinates,
                damageType,
                severityLevel,
                item.getConfidence() == null ? null : item.getConfidence().doubleValue(),
                item.getSuggestion(),
                item.getSnapshotUrl(),
                resultEntity == null ? null : resultEntity.getSummary(),
                hasWorkOrder,
                generatedWorkOrder,
                workOrderEntity == null ? null : workOrderEntity.getId(),
                workOrderEntity == null ? null : workOrderEntity.getWorkOrderCode(),
                workOrderStatus,
                departmentCode,
                departmentName,
                workOrderEntity == null ? null : workOrderEntity.getAssignee(),
                item.getDetectedAt(),
                workOrderEntity == null ? null : workOrderEntity.getCreatedAt(),
                workOrderEntity == null ? null : workOrderEntity.getDueAt()
        );
    }

    private WorkOrderEntity resolveWorkOrder(Long taskId,
                                             DetectionResultEntity resultEntity,
                                             Map<Long, WorkOrderEntity> latestWorkOrderMap,
                                             Map<Long, WorkOrderEntity> workOrderByIdMap) {
        if (resultEntity != null && resultEntity.getGeneratedWorkOrderId() != null) {
            WorkOrderEntity generated = workOrderByIdMap.get(resultEntity.getGeneratedWorkOrderId());
            if (generated != null) {
                return generated;
            }
        }
        return latestWorkOrderMap.get(taskId);
    }

    private boolean matchesKeyword(MarkerView view, String keyword) {
        return MapDataSupport.containsIgnoreCase(view.location(), keyword)
                || MapDataSupport.containsIgnoreCase(view.taskCode(), keyword)
                || MapDataSupport.containsIgnoreCase(view.departmentName(), keyword)
                || MapDataSupport.containsIgnoreCase(MapDataSupport.damageTypeLabel(view.damageType()), keyword);
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

    private long countByStatus(List<WorkOrderEntity> workOrders, WorkOrderStatus status) {
        return workOrders.stream()
                .filter(workOrder -> status.name().equals(workOrder.getStatus()))
                .count();
    }

    private boolean hasRenderableCoordinates(DetectionResultItemEntity item) {
        return resolveLng(item) != null && resolveLat(item) != null;
    }

    private java.math.BigDecimal resolveLng(DetectionResultItemEntity item) {
        if (item.getLng() != null) {
            return item.getLng();
        }
        return MapDataSupport.demoLng(item.getTaskId() == null ? 0L : item.getTaskId(), resolveItemIndex(item));
    }

    private java.math.BigDecimal resolveLat(DetectionResultItemEntity item) {
        if (item.getLat() != null) {
            return item.getLat();
        }
        return MapDataSupport.demoLat(item.getTaskId() == null ? 0L : item.getTaskId(), resolveItemIndex(item));
    }

    private int resolveItemIndex(DetectionResultItemEntity item) {
        if (item.getId() == null) {
            return 0;
        }
        return (int) (Math.abs(item.getId()) % 1000);
    }

    private record MarkerView(
            Long id,
            Long taskId,
            String taskCode,
            String location,
            java.math.BigDecimal lng,
            java.math.BigDecimal lat,
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
