package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.workorder.AssignWorkOrderRequest;
import com.roadcrack.api.request.workorder.CancelWorkOrderRequest;
import com.roadcrack.api.request.workorder.CreateWorkOrderRequest;
import com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.service.model.WorkOrderAggregate;
import com.roadcrack.service.port.RealtimeMessagePublisher;
import com.roadcrack.service.service.WorkOrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
public class InMemoryWorkOrderService implements WorkOrderService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, WorkOrderAggregate> store = new ConcurrentHashMap<>();
    private final RealtimeMessagePublisher realtimeMessagePublisher;

    public InMemoryWorkOrderService(RealtimeMessagePublisher realtimeMessagePublisher) {
        this.realtimeMessagePublisher = realtimeMessagePublisher;
    }

    @Override
    public WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request) {
        long id = idGenerator.getAndIncrement();
        LocalDateTime now = LocalDateTime.now();
        WorkOrderAggregate aggregate = new WorkOrderAggregate(
                id,
                buildCode(id, now.toLocalDate()),
                request.detectionTaskId(),
                request.title(),
                request.damageType(),
                request.severityLevel(),
                request.location(),
                request.departmentCode(),
                request.evidenceUrl(),
                request.description(),
                calculateDueAt(request.severityLevel(), now),
                now
        );
        store.put(id, aggregate);
        return publishAndReturn(aggregate);
    }

    @Override
    public WorkOrderResponse createFromDetection(Long detectionTaskId,
                                                 DamageType damageType,
                                                 SeverityLevel severityLevel,
                                                 String location,
                                                 String evidenceUrl) {
        return createWorkOrder(new CreateWorkOrderRequest(
                detectionTaskId,
                "自动派单-" + damageType.name(),
                damageType,
                severityLevel,
                location,
                mapDepartment(damageType),
                evidenceUrl,
                "由检测任务自动生成工单"
        ));
    }

    @Override
    public PageResponse<WorkOrderResponse> listWorkOrders(int page,
                                                          int size,
                                                          WorkOrderStatus status,
                                                          DepartmentCode departmentCode,
                                                          SeverityLevel severityLevel,
                                                          String assignee,
                                                          String keyword) {
        List<WorkOrderResponse> filtered = store.values().stream()
                .map(WorkOrderAggregate::toResponse)
                .filter(item -> status == null || item.status() == status)
                .filter(item -> departmentCode == null || item.departmentCode() == departmentCode)
                .filter(item -> severityLevel == null || item.severityLevel() == severityLevel)
                .filter(item -> assignee == null || containsIgnoreCase(item.assignee(), assignee))
                .filter(item -> keyword == null || containsIgnoreCase(item.title(), keyword) || containsIgnoreCase(item.location(), keyword))
                .sorted(Comparator.comparing(WorkOrderResponse::createdAt).reversed())
                .toList();
        return paginate(filtered, page, size);
    }

    @Override
    public WorkOrderResponse getWorkOrder(Long workOrderId) {
        return getRequired(workOrderId).toResponse();
    }

    @Override
    public WorkOrderResponse assignWorkOrder(Long workOrderId, AssignWorkOrderRequest request) {
        WorkOrderAggregate aggregate = getRequired(workOrderId);
        aggregate.assign(request.departmentCode(), request.assignee());
        return publishAndReturn(aggregate);
    }

    @Override
    public WorkOrderResponse assignWorker(Long workOrderId, String assignee) {
        WorkOrderAggregate aggregate = getRequired(workOrderId);
        WorkOrderStatus currentStatus = aggregate.getStatus();
        if (currentStatus != WorkOrderStatus.ASSIGNED && currentStatus != WorkOrderStatus.REJECTED) {
            throw new BusinessException(ResultCode.CONFLICT, "仅已指派或已驳回的工单可重新分配维修工");
        }
        aggregate.assignWorker(assignee);
        return publishAndReturn(aggregate);
    }

    @Override
    public WorkOrderResponse updateStatus(Long workOrderId, UpdateWorkOrderStatusRequest request) {
        WorkOrderAggregate aggregate = getRequired(workOrderId);
        aggregate.updateStatus(request.status(), request.note());
        return publishAndReturn(aggregate);
    }

    @Override
    public WorkOrderResponse cancelWorkOrder(Long workOrderId, CancelWorkOrderRequest request) {
        WorkOrderAggregate aggregate = getRequired(workOrderId);
        aggregate.cancel(request.reason());
        return publishAndReturn(aggregate);
    }

    @Override
    public void closeByReport(Long workOrderId, String note) {
        WorkOrderAggregate aggregate = getRequired(workOrderId);
        aggregate.closeByReport(note);
        publishAndReturn(aggregate);
    }

    private WorkOrderAggregate getRequired(Long workOrderId) {
        WorkOrderAggregate aggregate = store.get(workOrderId);
        if (aggregate == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "工单不存在: " + workOrderId);
        }
        return aggregate;
    }

    private WorkOrderResponse publishAndReturn(WorkOrderAggregate aggregate) {
        WorkOrderResponse response = aggregate.toResponse();
        realtimeMessagePublisher.publishWorkOrderUpdate(response);
        return response;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }

    private PageResponse<WorkOrderResponse> paginate(List<WorkOrderResponse> items, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, items.size());
        int toIndex = Math.min(fromIndex + safeSize, items.size());
        long total = items.size();
        long pages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;
        return new PageResponse<>(items.subList(fromIndex, toIndex), total, safeSize, safePage, pages);
    }

    private String buildCode(long id, LocalDate date) {
        return "WO-" + date.format(CODE_DATE_FORMATTER) + "-" + String.format("%06d", id);
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
