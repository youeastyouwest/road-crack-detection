package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.workorder.AssignWorkOrderRequest;
import com.roadcrack.api.request.workorder.CancelWorkOrderRequest;
import com.roadcrack.api.request.workorder.CreateWorkOrderRequest;
import com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.api.response.workorder.WorkOrderStatusLogResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.DetectionTaskEntity;
import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.RoadEntity;
import com.roadcrack.dao.entity.WorkOrderEntity;
import com.roadcrack.dao.entity.WorkOrderFlowEntity;
import com.roadcrack.dao.mapper.DetectionResultItemMapper;
import com.roadcrack.dao.mapper.DetectionTaskMapper;
import com.roadcrack.dao.mapper.RoadMapper;
import com.roadcrack.dao.mapper.WorkOrderFlowMapper;
import com.roadcrack.dao.mapper.WorkOrderMapper;
import com.roadcrack.service.port.RealtimeMessagePublisher;
import com.roadcrack.service.service.WorkOrderService;
import com.roadcrack.service.util.RoadHealthScoreCalculator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class DbWorkOrderService implements WorkOrderService {

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_AUTO = "AUTO";
    private static final String DEFAULT_OPERATOR = "system";
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderFlowMapper workOrderFlowMapper;
    private final RealtimeMessagePublisher realtimeMessagePublisher;
    private final RoadMapper roadMapper;
    private final DetectionTaskMapper detectionTaskMapper;
    private final DetectionResultItemMapper detectionResultItemMapper;

    public DbWorkOrderService(WorkOrderMapper workOrderMapper,
                              WorkOrderFlowMapper workOrderFlowMapper,
                              RealtimeMessagePublisher realtimeMessagePublisher,
                              RoadMapper roadMapper,
                              DetectionTaskMapper detectionTaskMapper,
                              DetectionResultItemMapper detectionResultItemMapper) {
        this.workOrderMapper = workOrderMapper;
        this.workOrderFlowMapper = workOrderFlowMapper;
        this.realtimeMessagePublisher = realtimeMessagePublisher;
        this.roadMapper = roadMapper;
        this.detectionTaskMapper = detectionTaskMapper;
        this.detectionResultItemMapper = detectionResultItemMapper;
    }

    @Override
    @Transactional
    public WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request) {
        return createWorkOrderInternal(request, SOURCE_MANUAL, DEFAULT_OPERATOR);
    }

    @Override
    @Transactional
    public WorkOrderResponse createFromDetection(Long detectionTaskId,
                                                 DamageType damageType,
                                                 SeverityLevel severityLevel,
                                                 String location,
                                                 String evidenceUrl) {
        CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                detectionTaskId,
                "AUTO-" + damageType.name(),
                damageType,
                severityLevel,
                location == null ? "unknown" : location,
                mapDepartment(damageType),
                evidenceUrl == null ? "" : evidenceUrl,
                "generated from detection task"
        );
        return createWorkOrderInternal(request, SOURCE_AUTO, DEFAULT_OPERATOR);
    }

    @Override
    public PageResponse<WorkOrderResponse> listWorkOrders(int page,
                                                          int size,
                                                          WorkOrderStatus status,
                                                          DepartmentCode departmentCode,
                                                          SeverityLevel severityLevel,
                                                          String assignee,
                                                          String keyword) {
        Page<WorkOrderEntity> queryPage = new Page<>(page, size);
        LambdaQueryWrapper<WorkOrderEntity> wrapper = new LambdaQueryWrapper<WorkOrderEntity>()
                .orderByDesc(WorkOrderEntity::getCreatedAt);
        if (status != null) {
            wrapper.eq(WorkOrderEntity::getStatus, status.name());
        }
        if (departmentCode != null) {
            wrapper.eq(WorkOrderEntity::getDepartmentCode, departmentCode.name());
        }
        if (severityLevel != null) {
            wrapper.eq(WorkOrderEntity::getSeverityLevel, severityLevel.name());
        }
        if (assignee != null && !assignee.isBlank()) {
            wrapper.like(WorkOrderEntity::getAssignee, assignee);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(item -> item.like(WorkOrderEntity::getTitle, keyword)
                    .or()
                    .like(WorkOrderEntity::getLocation, keyword));
        }
        workOrderMapper.selectPage(queryPage, wrapper);

        List<WorkOrderEntity> workOrders = queryPage.getRecords();
        Map<Long, List<WorkOrderStatusLogResponse>> statusLogMap = loadStatusLogMap(workOrders.stream()
                .map(WorkOrderEntity::getId)
                .toList());

        List<WorkOrderResponse> records = workOrders.stream()
                .map(entity -> toResponse(entity, statusLogMap.getOrDefault(entity.getId(), List.of())))
                .toList();

        return new PageResponse<>(records, queryPage.getTotal(), queryPage.getSize(), queryPage.getCurrent(), queryPage.getPages());
    }

    @Override
    public WorkOrderResponse getWorkOrder(Long workOrderId) {
        WorkOrderEntity entity = getRequired(workOrderId);
        List<WorkOrderStatusLogResponse> statusLogs = loadStatusLogMap(List.of(workOrderId))
                .getOrDefault(workOrderId, List.of());
        return toResponse(entity, statusLogs);
    }

    @Override
    @Transactional
    public WorkOrderResponse assignWorkOrder(Long workOrderId, AssignWorkOrderRequest request) {
        WorkOrderEntity entity = getRequired(workOrderId);
        ensureStatus(entity, WorkOrderStatus.PENDING_ASSIGNMENT, "only pending work orders can be assigned");

        LocalDateTime now = LocalDateTime.now();
        entity.setDepartmentCode(request.departmentCode().name());
        entity.setAssignee(request.assignee());
        entity.setStatus(WorkOrderStatus.ASSIGNED.name());
        entity.setUpdatedAt(now);
        workOrderMapper.updateById(entity);

        String assignee = request.assignee();
        String assignNote = (assignee != null && !assignee.isBlank())
                ? "assigned to " + assignee
                : "assigned to department " + request.departmentCode();

        insertFlow(entity.getId(),
                WorkOrderStatus.PENDING_ASSIGNMENT,
                WorkOrderStatus.ASSIGNED,
                "ASSIGN",
                DEFAULT_OPERATOR,
                request.departmentCode(),
                assignee,
                assignNote,
                now);

        return publishAndReturn(entity.getId());
    }

    @Override
    @Transactional
    public WorkOrderResponse assignWorker(Long workOrderId, String assignee) {
        WorkOrderEntity entity = getRequired(workOrderId);
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(entity.getStatus());
        if (currentStatus != WorkOrderStatus.ASSIGNED && currentStatus != WorkOrderStatus.REJECTED) {
            throw new BusinessException(ResultCode.CONFLICT, "only ASSIGNED or REJECTED work orders can have worker reassigned");
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setAssignee(assignee);
        entity.setUpdatedAt(now);
        workOrderMapper.updateById(entity);

        insertFlow(entity.getId(),
                currentStatus,
                currentStatus,
                "ASSIGN_WORKER",
                DEFAULT_OPERATOR,
                entity.getDepartmentCode() == null ? null : DepartmentCode.valueOf(entity.getDepartmentCode()),
                assignee,
                "assigned worker: " + assignee,
                now);

        return publishAndReturn(entity.getId());
    }

    @Override
    @Transactional
    public WorkOrderResponse updateStatus(Long workOrderId, UpdateWorkOrderStatusRequest request) {
        WorkOrderEntity entity = getRequired(workOrderId);
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(entity.getStatus());
        WorkOrderStatus targetStatus = request.status();

        if (targetStatus == WorkOrderStatus.CANCELLED) {
            throw new BusinessException(ResultCode.CONFLICT, "use the cancel endpoint for cancellation");
        }
        if (targetStatus == WorkOrderStatus.ASSIGNED && currentStatus == WorkOrderStatus.PENDING_ASSIGNMENT) {
            throw new BusinessException(ResultCode.CONFLICT, "use the assign endpoint before moving to ASSIGNED");
        }
        if (targetStatus == WorkOrderStatus.CLOSED) {
            throw new BusinessException(ResultCode.CONFLICT, "work orders can only be closed by maintenance report flow");
        }
        if (!isAllowedTransition(currentStatus, targetStatus)) {
            throw new BusinessException(ResultCode.CONFLICT, "invalid work order status transition: " + currentStatus + " -> " + targetStatus);
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(targetStatus.name());
        entity.setUpdatedAt(now);
        if (targetStatus == WorkOrderStatus.IN_PROGRESS && entity.getAcceptedAt() == null) {
            entity.setAcceptedAt(now);
        }
        if (targetStatus == WorkOrderStatus.COMPLETED) {
            entity.setFinishedAt(now);
        }
        workOrderMapper.updateById(entity);

        insertFlow(entity.getId(),
                currentStatus,
                targetStatus,
                "STATUS_CHANGE",
                DEFAULT_OPERATOR,
                entity.getDepartmentCode() == null ? null : DepartmentCode.valueOf(entity.getDepartmentCode()),
                entity.getAssignee(),
                request.note(),
                now);

        return publishAndReturn(entity.getId());
    }

    @Override
    @Transactional
    public WorkOrderResponse cancelWorkOrder(Long workOrderId, CancelWorkOrderRequest request) {
        WorkOrderEntity entity = getRequired(workOrderId);
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(entity.getStatus());
        if (currentStatus == WorkOrderStatus.CLOSED || currentStatus == WorkOrderStatus.CANCELLED) {
            throw new BusinessException(ResultCode.CONFLICT, "closed or cancelled work orders cannot be cancelled again");
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(WorkOrderStatus.CANCELLED.name());
        entity.setUpdatedAt(now);
        workOrderMapper.updateById(entity);

        insertFlow(entity.getId(),
                currentStatus,
                WorkOrderStatus.CANCELLED,
                "CANCEL",
                DEFAULT_OPERATOR,
                entity.getDepartmentCode() == null ? null : DepartmentCode.valueOf(entity.getDepartmentCode()),
                entity.getAssignee(),
                request.reason(),
                now);

        return publishAndReturn(entity.getId());
    }

    @Override
    @Transactional
    public void closeByReport(Long workOrderId, String note) {
        WorkOrderEntity entity = getRequired(workOrderId);
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(entity.getStatus());
        if (currentStatus != WorkOrderStatus.PENDING_ADMIN_REVIEW) {
            throw new BusinessException(ResultCode.CONFLICT, "only admin-reviewed work orders can be closed by maintenance report");
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(WorkOrderStatus.CLOSED.name());
        entity.setUpdatedAt(now);
        workOrderMapper.updateById(entity);

        insertFlow(entity.getId(),
                currentStatus,
                WorkOrderStatus.CLOSED,
                "CLOSE_BY_REPORT",
                DEFAULT_OPERATOR,
                entity.getDepartmentCode() == null ? null : DepartmentCode.valueOf(entity.getDepartmentCode()),
                entity.getAssignee(),
                note,
                now);

        // 工单关闭后，更新关联道路的统计信息
        updateRoadStatsOnClose(entity, now);

        publishAndReturn(entity.getId());
    }

    /**
     * 工单关闭时更新道路统计：
     * - current_damage_count 减 1（不低于 0）
     * - health_score 使用 RoadHealthScoreCalculator 基于该道路所有剩余病害重新计算
     * - last_maintained 设为当前时间
     * - status 如果之前是 MAINTAINING 则改为 ACTIVE
     * - damage_level 根据新的 health_score 重新评估
     */
    private void updateRoadStatsOnClose(WorkOrderEntity workOrder, LocalDateTime now) {
        if (workOrder.getDetectionTaskId() == null) return;
        DetectionTaskEntity task = detectionTaskMapper.selectById(workOrder.getDetectionTaskId());
        if (task == null || task.getRoadId() == null) return;
        RoadEntity road = roadMapper.selectById(task.getRoadId());
        if (road == null) return;

        int currentDamage = road.getCurrentDamageCount() != null ? road.getCurrentDamageCount() : 0;
        currentDamage = Math.max(0, currentDamage - 1);
        road.setCurrentDamageCount(currentDamage);

        // 使用 RoadHealthScoreCalculator 基于该道路所有剩余病害重新计算健康评分
        List<DetectionTaskEntity> roadTasks = detectionTaskMapper.selectList(
                new LambdaQueryWrapper<DetectionTaskEntity>()
                        .eq(DetectionTaskEntity::getRoadId, task.getRoadId())
                        .eq(DetectionTaskEntity::getStatus, "COMPLETED"));
        List<Long> taskIds = roadTasks.stream().map(DetectionTaskEntity::getId).toList();
        List<DetectionResultItemEntity> allItems = taskIds.isEmpty()
                ? Collections.emptyList()
                : detectionResultItemMapper.selectList(
                        new LambdaQueryWrapper<DetectionResultItemEntity>()
                                .in(DetectionResultItemEntity::getTaskId, taskIds));

        java.math.BigDecimal healthScore = RoadHealthScoreCalculator.calculate(road, allItems);
        String damageLevel = RoadHealthScoreCalculator.resolveDamageLevel(healthScore);

        road.setHealthScore(healthScore);
        road.setDamageLevel(damageLevel);
        road.setLastMaintained(now);
        road.setUpdatedAt(now);
        if ("MAINTAINING".equals(road.getStatus())) {
            road.setStatus("ACTIVE");
        }
        roadMapper.updateById(road);
    }

    private WorkOrderResponse createWorkOrderInternal(CreateWorkOrderRequest request, String sourceType, String operatorName) {
        LocalDateTime now = LocalDateTime.now();

        WorkOrderEntity entity = new WorkOrderEntity();
        entity.setWorkOrderCode(buildCode(now.toLocalDate()));
        entity.setSourceType(sourceType);
        entity.setDetectionTaskId(request.detectionTaskId());
        entity.setTitle(request.title());
        entity.setDamageType(request.damageType().name());
        entity.setSeverityLevel(request.severityLevel().name());
        entity.setLocation(request.location());
        entity.setDepartmentCode(request.departmentCode().name());
        entity.setStatus(WorkOrderStatus.PENDING_ASSIGNMENT.name());
        entity.setEvidenceUrl(request.evidenceUrl());
        entity.setDescription(request.description());
        entity.setDueAt(calculateDueAt(request.severityLevel(), now));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        workOrderMapper.insert(entity);

        insertFlow(entity.getId(),
                null,
                WorkOrderStatus.PENDING_ASSIGNMENT,
                "CREATE",
                operatorName,
                request.departmentCode(),
                null,
                sourceType.equals(SOURCE_AUTO) ? "generated from detection task" : "work order created",
                now);

        return publishAndReturn(entity.getId());
    }

    private void ensureStatus(WorkOrderEntity entity, WorkOrderStatus expectedStatus, String message) {
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(entity.getStatus());
        if (currentStatus != expectedStatus) {
            throw new BusinessException(ResultCode.CONFLICT, message);
        }
    }

    private boolean isAllowedTransition(WorkOrderStatus currentStatus, WorkOrderStatus targetStatus) {
        return switch (currentStatus) {
            case PENDING_ASSIGNMENT -> targetStatus == WorkOrderStatus.CANCELLED;
            case ASSIGNED -> targetStatus == WorkOrderStatus.IN_PROGRESS || targetStatus == WorkOrderStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == WorkOrderStatus.COMPLETED || targetStatus == WorkOrderStatus.CANCELLED;
            case COMPLETED -> targetStatus == WorkOrderStatus.PENDING_DEPT_REVIEW || targetStatus == WorkOrderStatus.CANCELLED;
            case PENDING_DEPT_REVIEW -> targetStatus == WorkOrderStatus.PENDING_ADMIN_REVIEW || targetStatus == WorkOrderStatus.REJECTED;
            case PENDING_ADMIN_REVIEW -> targetStatus == WorkOrderStatus.REJECTED;
            case REJECTED -> targetStatus == WorkOrderStatus.PENDING_DEPT_REVIEW;
            case CLOSED, CANCELLED -> false;
        };
    }

    private WorkOrderEntity getRequired(Long workOrderId) {
        WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "work order not found: " + workOrderId);
        }
        return entity;
    }

    private WorkOrderResponse publishAndReturn(Long workOrderId) {
        WorkOrderResponse response = getWorkOrder(workOrderId);
        realtimeMessagePublisher.publishWorkOrderUpdate(response);
        return response;
    }

    private Map<Long, List<WorkOrderStatusLogResponse>> loadStatusLogMap(List<Long> workOrderIds) {
        if (workOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return workOrderFlowMapper.selectList(new LambdaQueryWrapper<WorkOrderFlowEntity>()
                        .in(WorkOrderFlowEntity::getWorkOrderId, workOrderIds)
                        .orderByAsc(WorkOrderFlowEntity::getCreatedAt)
                        .orderByAsc(WorkOrderFlowEntity::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        WorkOrderFlowEntity::getWorkOrderId,
                        Collectors.mapping(this::toStatusLog, Collectors.toList())
                ));
    }

    private WorkOrderStatusLogResponse toStatusLog(WorkOrderFlowEntity entity) {
        return new WorkOrderStatusLogResponse(
                entity.getFromStatus() == null ? null : WorkOrderStatus.valueOf(entity.getFromStatus()),
                WorkOrderStatus.valueOf(entity.getToStatus()),
                entity.getRemark(),
                entity.getCreatedAt()
        );
    }

    private WorkOrderResponse toResponse(WorkOrderEntity entity, List<WorkOrderStatusLogResponse> statusLogs) {
        List<WorkOrderStatusLogResponse> sortedLogs = statusLogs.stream()
                .sorted(Comparator.comparing(WorkOrderStatusLogResponse::operatedAt))
                .toList();
        return new WorkOrderResponse(
                entity.getId(),
                entity.getWorkOrderCode(),
                entity.getDetectionTaskId(),
                entity.getTitle(),
                DamageType.valueOf(entity.getDamageType()),
                SeverityLevel.valueOf(entity.getSeverityLevel()),
                entity.getLocation(),
                entity.getDepartmentCode() == null ? null : DepartmentCode.valueOf(entity.getDepartmentCode()),
                entity.getAssignee(),
                WorkOrderStatus.valueOf(entity.getStatus()),
                entity.getEvidenceUrl(),
                entity.getDescription(),
                entity.getDueAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                sortedLogs
        );
    }

    private void insertFlow(Long workOrderId,
                            WorkOrderStatus fromStatus,
                            WorkOrderStatus toStatus,
                            String operationType,
                            String operatorName,
                            DepartmentCode targetDepartmentCode,
                            String targetAssignee,
                            String remark,
                            LocalDateTime createdAt) {
        WorkOrderFlowEntity flowEntity = new WorkOrderFlowEntity();
        flowEntity.setWorkOrderId(workOrderId);
        flowEntity.setFromStatus(fromStatus == null ? null : fromStatus.name());
        flowEntity.setToStatus(toStatus.name());
        flowEntity.setOperationType(operationType);
        flowEntity.setOperatorName(operatorName);
        flowEntity.setTargetDepartmentCode(targetDepartmentCode == null ? null : targetDepartmentCode.name());
        flowEntity.setTargetAssignee(targetAssignee);
        flowEntity.setRemark(remark);
        flowEntity.setCreatedAt(createdAt);
        workOrderFlowMapper.insert(flowEntity);
    }

    private String buildCode(LocalDate date) {
        long count = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrderEntity>()
                .ge(WorkOrderEntity::getCreatedAt, date.atStartOfDay())
                .lt(WorkOrderEntity::getCreatedAt, date.plusDays(1).atStartOfDay()));
        return "WO-" + date.format(CODE_DATE_FORMATTER) + "-" + String.format("%06d", count + 1);
    }

    private LocalDateTime calculateDueAt(SeverityLevel severityLevel, LocalDateTime now) {
        return switch (severityLevel) {
            case HIGH -> now.plusHours(24);
            case MEDIUM -> now.plusHours(48);
            case LOW -> now.plusHours(72);
        };
    }

    private DepartmentCode mapDepartment(DamageType damageType) {
        return switch (damageType) {
            case ROAD_SPILL -> DepartmentCode.SANITATION;
            case MARKING_DAMAGE, CRACK, POTHOLE, UNKNOWN -> DepartmentCode.ROAD_ADMIN;
        };
    }
}
