package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.request.report.ReviewRequest;
import com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest;
import com.roadcrack.api.response.report.MaintenanceReportResponse;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.service.model.MaintenanceReportAggregate;
import com.roadcrack.service.service.MaintenanceReportService;
import com.roadcrack.service.service.WorkOrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryMaintenanceReportService implements MaintenanceReportService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, MaintenanceReportAggregate> store = new ConcurrentHashMap<>();
    private final WorkOrderService workOrderService;

    public InMemoryMaintenanceReportService(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @Override
    public MaintenanceReportResponse createReport(CreateMaintenanceReportRequest request) {
        WorkOrderResponse workOrder = workOrderService.getWorkOrder(request.workOrderId());
        WorkOrderStatus currentStatus = workOrder.status();

        // Allow creating report from COMPLETED status (first submission)
        // or from REJECTED status (re-submission after rejection)
        if (currentStatus != WorkOrderStatus.COMPLETED && currentStatus != WorkOrderStatus.REJECTED) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "only completed or rejected work orders can submit maintenance reports");
        }

        LocalDateTime now = LocalDateTime.now();

        MaintenanceReportAggregate existing = findLatestByWorkOrderId(request.workOrderId());
        MaintenanceReportAggregate aggregate;
        if (existing != null && currentStatus == WorkOrderStatus.REJECTED) {
            // Re-submission: create a new aggregate with the same id and replace the old one
            aggregate = new MaintenanceReportAggregate(
                    existing.getId(),
                    existing.getReportCode(),
                    request.workOrderId(),
                    request.executor(),
                    request.beforeImageUrl(),
                    request.afterImageUrl(),
                    request.materials(),
                    request.description(),
                    request.finishedAt(),
                    existing.getCreatedAt()
            );
            store.put(existing.getId(), aggregate);
        } else {
            long id = idGenerator.getAndIncrement();
            aggregate = new MaintenanceReportAggregate(
                    id,
                    buildCode(id, now.toLocalDate()),
                    request.workOrderId(),
                    request.executor(),
                    request.beforeImageUrl(),
                    request.afterImageUrl(),
                    request.materials(),
                    request.description(),
                    request.finishedAt(),
                    now
            );
            store.put(id, aggregate);
        }

        // Move work order to PENDING_DEPT_REVIEW
        workOrderService.updateStatus(request.workOrderId(),
                new UpdateWorkOrderStatusRequest(
                        WorkOrderStatus.PENDING_DEPT_REVIEW,
                        "maintenance report submitted, pending department review"));

        return aggregate.toResponse();
    }

    @Override
    public MaintenanceReportResponse deptReview(Long reportId, ReviewRequest request, String reviewerName) {
        MaintenanceReportAggregate aggregate = getRequiredReport(reportId);
        if (!"PENDING".equals(aggregate.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "only pending reports can be reviewed by department admin");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.approved()) {
            aggregate.review("DEPT_APPROVED", reviewerName, request.remark(), now);
            workOrderService.updateStatus(aggregate.getWorkOrderId(),
                    new UpdateWorkOrderStatusRequest(
                            WorkOrderStatus.PENDING_ADMIN_REVIEW,
                            "department admin approved: " + (request.remark() == null ? "" : request.remark())));
        } else {
            aggregate.review("REJECTED", reviewerName, request.remark(), now);
            workOrderService.updateStatus(aggregate.getWorkOrderId(),
                    new UpdateWorkOrderStatusRequest(
                            WorkOrderStatus.REJECTED,
                            "department admin rejected: " + (request.remark() == null ? "" : request.remark())));
        }

        return aggregate.toResponse();
    }

    @Override
    public MaintenanceReportResponse adminReview(Long reportId, ReviewRequest request, String reviewerName) {
        MaintenanceReportAggregate aggregate = getRequiredReport(reportId);
        if (!"DEPT_APPROVED".equals(aggregate.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "only department-approved reports can be reviewed by admin");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.approved()) {
            aggregate.review("APPROVED", reviewerName, request.remark(), now);
            workOrderService.closeByReport(aggregate.getWorkOrderId(),
                    "admin approved: " + (request.remark() == null ? "" : request.remark()));
        } else {
            aggregate.review("REJECTED", reviewerName, request.remark(), now);
            workOrderService.updateStatus(aggregate.getWorkOrderId(),
                    new UpdateWorkOrderStatusRequest(
                            WorkOrderStatus.REJECTED,
                            "admin rejected: " + (request.remark() == null ? "" : request.remark())));
        }

        return aggregate.toResponse();
    }

    @Override
    public PageResponse<MaintenanceReportResponse> listReports(int page, int size, Long workOrderId, String executor) {
        java.util.List<MaintenanceReportResponse> filtered = store.values().stream()
                .filter(item -> workOrderId == null || item.getWorkOrderId().equals(workOrderId))
                .filter(item -> executor == null || containsIgnoreCase(item.getExecutor(), executor))
                .map(MaintenanceReportAggregate::toResponse)
                .sorted(Comparator.comparing(MaintenanceReportResponse::createdAt).reversed())
                .toList();
        return paginate(filtered, page, size);
    }

    @Override
    public MaintenanceReportResponse getReport(Long reportId) {
        MaintenanceReportAggregate aggregate = getRequiredReport(reportId);
        return aggregate.toResponse();
    }

    private MaintenanceReportAggregate getRequiredReport(Long reportId) {
        MaintenanceReportAggregate aggregate = store.get(reportId);
        if (aggregate == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "维修报告不存在: " + reportId);
        }
        return aggregate;
    }

    private MaintenanceReportAggregate findLatestByWorkOrderId(Long workOrderId) {
        return store.values().stream()
                .filter(item -> item.getWorkOrderId().equals(workOrderId))
                .max(Comparator.comparing(MaintenanceReportAggregate::getUpdatedAt))
                .orElse(null);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }

    private PageResponse<MaintenanceReportResponse> paginate(java.util.List<MaintenanceReportResponse> items, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, items.size());
        int toIndex = Math.min(fromIndex + safeSize, items.size());
        long total = items.size();
        long pages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;
        return new PageResponse<>(items.subList(fromIndex, toIndex), total, safeSize, safePage, pages);
    }

    private String buildCode(long id, LocalDate date) {
        return "MR-" + date.format(CODE_DATE_FORMATTER) + "-" + String.format("%06d", id);
    }
}
