package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.response.report.MaintenanceReportResponse;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.MaintenanceReportEntity;
import com.roadcrack.dao.mapper.MaintenanceReportMapper;
import com.roadcrack.service.service.MaintenanceReportService;
import com.roadcrack.service.service.WorkOrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbMaintenanceReportService implements MaintenanceReportService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MaintenanceReportMapper maintenanceReportMapper;
    private final WorkOrderService workOrderService;

    public DbMaintenanceReportService(MaintenanceReportMapper maintenanceReportMapper,
                                      WorkOrderService workOrderService) {
        this.maintenanceReportMapper = maintenanceReportMapper;
        this.workOrderService = workOrderService;
    }

    @Override
    @Transactional
    public MaintenanceReportResponse createReport(CreateMaintenanceReportRequest request) {
        WorkOrderResponse workOrder = workOrderService.getWorkOrder(request.workOrderId());
        if (workOrder.status() != WorkOrderStatus.COMPLETED) {
            throw new BusinessException(ResultCode.CONFLICT, "only completed work orders can create maintenance reports");
        }

        LocalDateTime now = LocalDateTime.now();
        MaintenanceReportEntity entity = new MaintenanceReportEntity();
        entity.setReportCode(buildCode(now.toLocalDate()));
        entity.setWorkOrderId(request.workOrderId());
        entity.setExecutor(request.executor());
        entity.setBeforeImageUrl(request.beforeImageUrl());
        entity.setAfterImageUrl(request.afterImageUrl());
        entity.setMaterials(request.materials());
        entity.setDescription(request.description());
        entity.setFinishedAt(request.finishedAt());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        maintenanceReportMapper.insert(entity);

        workOrderService.closeByReport(request.workOrderId(), "maintenance report submitted, work order closed");
        return toResponse(entity);
    }

    @Override
    public PageResponse<MaintenanceReportResponse> listReports(int page, int size, Long workOrderId, String executor) {
        Page<MaintenanceReportEntity> queryPage = new Page<>(page, size);
        LambdaQueryWrapper<MaintenanceReportEntity> wrapper = new LambdaQueryWrapper<MaintenanceReportEntity>()
                .orderByDesc(MaintenanceReportEntity::getCreatedAt);
        if (workOrderId != null) {
            wrapper.eq(MaintenanceReportEntity::getWorkOrderId, workOrderId);
        }
        if (executor != null && !executor.isBlank()) {
            wrapper.like(MaintenanceReportEntity::getExecutor, executor);
        }
        maintenanceReportMapper.selectPage(queryPage, wrapper);

        java.util.List<MaintenanceReportResponse> records = queryPage.getRecords().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(records, queryPage.getTotal(), queryPage.getSize(), queryPage.getCurrent(), queryPage.getPages());
    }

    @Override
    public MaintenanceReportResponse getReport(Long reportId) {
        MaintenanceReportEntity entity = maintenanceReportMapper.selectById(reportId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "maintenance report not found: " + reportId);
        }
        return toResponse(entity);
    }

    private MaintenanceReportResponse toResponse(MaintenanceReportEntity entity) {
        return new MaintenanceReportResponse(
                entity.getId(),
                entity.getReportCode(),
                entity.getWorkOrderId(),
                entity.getExecutor(),
                entity.getBeforeImageUrl(),
                entity.getAfterImageUrl(),
                entity.getMaterials(),
                entity.getDescription(),
                entity.getFinishedAt(),
                entity.getCreatedAt()
        );
    }

    private String buildCode(LocalDate date) {
        long count = maintenanceReportMapper.selectCount(new LambdaQueryWrapper<MaintenanceReportEntity>()
                .ge(MaintenanceReportEntity::getCreatedAt, date.atStartOfDay())
                .lt(MaintenanceReportEntity::getCreatedAt, date.plusDays(1).atStartOfDay()));
        return "MR-" + date.format(CODE_DATE_FORMATTER) + "-" + String.format("%06d", count + 1);
    }
}
