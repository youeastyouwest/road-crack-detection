package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.request.report.ReviewRequest;
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
        WorkOrderStatus currentStatus = workOrder.status();

        // Allow creating report from COMPLETED status (first submission)
        // or from REJECTED status (re-submission after rejection)
        if (currentStatus != WorkOrderStatus.COMPLETED && currentStatus != WorkOrderStatus.REJECTED) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "only completed or rejected work orders can submit maintenance reports");
        }

        LocalDateTime now = LocalDateTime.now();

        // Check if there's an existing report for re-submission
        MaintenanceReportEntity existing = maintenanceReportMapper.selectOne(
                new LambdaQueryWrapper<MaintenanceReportEntity>()
                        .eq(MaintenanceReportEntity::getWorkOrderId, request.workOrderId())
                        .orderByDesc(MaintenanceReportEntity::getCreatedAt)
                        .last("LIMIT 1"));

        MaintenanceReportEntity entity;
        if (existing != null && currentStatus == WorkOrderStatus.REJECTED) {
            // Re-submission: update the existing report
            entity = existing;
            entity.setExecutor(request.executor());
            entity.setBeforeImageUrl(request.beforeImageUrl());
            entity.setAfterImageUrl(request.afterImageUrl());
            entity.setMaterials(request.materials());
            entity.setDescription(request.description());
            entity.setFinishedAt(request.finishedAt());
            entity.setStatus("PENDING");
            entity.setReviewRemark(null);
            entity.setReviewer(null);
            entity.setReviewedAt(null);
            entity.setUpdatedAt(now);
            maintenanceReportMapper.updateById(entity);
        } else {
            // First submission: create new report
            entity = new MaintenanceReportEntity();
            entity.setReportCode(buildCode(now.toLocalDate()));
            entity.setWorkOrderId(request.workOrderId());
            entity.setExecutor(request.executor());
            entity.setBeforeImageUrl(request.beforeImageUrl());
            entity.setAfterImageUrl(request.afterImageUrl());
            entity.setMaterials(request.materials());
            entity.setDescription(request.description());
            entity.setFinishedAt(request.finishedAt());
            entity.setStatus("PENDING");
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            maintenanceReportMapper.insert(entity);
        }

        // Move work order to PENDING_DEPT_REVIEW (instead of closing it)
        workOrderService.updateStatus(request.workOrderId(),
                new com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest(
                        WorkOrderStatus.PENDING_DEPT_REVIEW,
                        "maintenance report submitted, pending department review"));

        return toResponse(entity);
    }

    @Override
    @Transactional
    public MaintenanceReportResponse deptReview(Long reportId, ReviewRequest request, String reviewerName) {
        MaintenanceReportEntity entity = getRequiredReport(reportId);
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "only pending reports can be reviewed by department admin");
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setReviewer(reviewerName);
        entity.setReviewedAt(now);
        entity.setReviewRemark(request.remark());
        entity.setUpdatedAt(now);

        if (request.approved()) {
            entity.setStatus("DEPT_APPROVED");
            maintenanceReportMapper.updateById(entity);
            // Move work order to PENDING_ADMIN_REVIEW
            workOrderService.updateStatus(entity.getWorkOrderId(),
                    new com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest(
                            WorkOrderStatus.PENDING_ADMIN_REVIEW,
                            "department admin approved: " + (request.remark() == null ? "" : request.remark())));
        } else {
            entity.setStatus("REJECTED");
            maintenanceReportMapper.updateById(entity);
            // Move work order to REJECTED
            workOrderService.updateStatus(entity.getWorkOrderId(),
                    new com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest(
                            WorkOrderStatus.REJECTED,
                            "department admin rejected: " + (request.remark() == null ? "" : request.remark())));
        }

        return toResponse(entity);
    }

    @Override
    @Transactional
    public MaintenanceReportResponse adminReview(Long reportId, ReviewRequest request, String reviewerName) {
        MaintenanceReportEntity entity = getRequiredReport(reportId);
        if (!"DEPT_APPROVED".equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "only department-approved reports can be reviewed by admin");
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setReviewer(reviewerName);
        entity.setReviewedAt(now);
        entity.setReviewRemark(request.remark());
        entity.setUpdatedAt(now);

        if (request.approved()) {
            entity.setStatus("APPROVED");
            maintenanceReportMapper.updateById(entity);
            // Close the work order — this is the only path to CLOSED
            workOrderService.closeByReport(entity.getWorkOrderId(),
                    "admin approved: " + (request.remark() == null ? "" : request.remark()));
        } else {
            entity.setStatus("REJECTED");
            maintenanceReportMapper.updateById(entity);
            // Move work order to REJECTED
            workOrderService.updateStatus(entity.getWorkOrderId(),
                    new com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest(
                            WorkOrderStatus.REJECTED,
                            "admin rejected: " + (request.remark() == null ? "" : request.remark())));
        }

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
        return toResponse(getRequiredReport(reportId));
    }

    private MaintenanceReportEntity getRequiredReport(Long reportId) {
        MaintenanceReportEntity entity = maintenanceReportMapper.selectById(reportId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "maintenance report not found: " + reportId);
        }
        return entity;
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
                entity.getStatus(),
                entity.getReviewRemark(),
                entity.getReviewer(),
                entity.getReviewedAt(),
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
