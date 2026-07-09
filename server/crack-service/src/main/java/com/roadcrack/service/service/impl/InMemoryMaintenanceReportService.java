package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.response.report.MaintenanceReportResponse;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.service.model.AuditLogRecord;
import com.roadcrack.service.model.MaintenanceReportAggregate;
import com.roadcrack.service.service.AuditLogService;
import com.roadcrack.service.service.MaintenanceReportService;
import com.roadcrack.service.service.WorkOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(InMemoryMaintenanceReportService.class);
    private static final String MODULE_MAINTENANCE_REPORT = "MAINTENANCE_REPORT";
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, MaintenanceReportAggregate> store = new ConcurrentHashMap<>();
    private final AuditLogService auditLogService;
    private final WorkOrderService workOrderService;

    public InMemoryMaintenanceReportService(AuditLogService auditLogService,
                                            WorkOrderService workOrderService) {
        this.auditLogService = auditLogService;
        this.workOrderService = workOrderService;
    }

    @Override
    public MaintenanceReportResponse createReport(CreateMaintenanceReportRequest request) {
        WorkOrderResponse workOrder = workOrderService.getWorkOrder(request.workOrderId());
        if (workOrder.status() != WorkOrderStatus.COMPLETED) {
            throw new BusinessException(ResultCode.CONFLICT, "仅已完成工单可以创建维修报告");
        }

        long id = idGenerator.getAndIncrement();
        LocalDateTime now = LocalDateTime.now();
        MaintenanceReportAggregate aggregate = new MaintenanceReportAggregate(
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
        workOrderService.closeByReport(request.workOrderId(), "维修报告已上传，工单自动关闭");
        log.info("Maintenance report created in memory: reportId={}, reportCode={}, workOrderId={}, executor={}",
                aggregate.getId(),
                aggregate.getReportCode(),
                aggregate.getWorkOrderId(),
                aggregate.getExecutor());
        auditLogService.record(AuditLogRecord.success(
                        MODULE_MAINTENANCE_REPORT,
                        "CREATE",
                        "Created maintenance report " + aggregate.getReportCode())
                .setUsername(aggregate.getExecutor())
                .setParams(buildCreateParams(aggregate))
                .setCreateTime(now));
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
        MaintenanceReportAggregate aggregate = store.get(reportId);
        if (aggregate == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "维修报告不存在: " + reportId);
        }
        return aggregate.toResponse();
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

    private String buildCreateParams(MaintenanceReportAggregate aggregate) {
        return "reportId=" + aggregate.getId()
                + ", reportCode=" + aggregate.getReportCode()
                + ", workOrderId=" + aggregate.getWorkOrderId()
                + ", executor=" + safeValue(aggregate.getExecutor());
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
