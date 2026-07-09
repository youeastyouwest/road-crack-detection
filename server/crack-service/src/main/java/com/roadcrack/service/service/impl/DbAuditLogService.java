package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.request.auditlog.AuditLogPageQuery;
import com.roadcrack.api.response.auditlog.AuditLogResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.dao.entity.OperationLogEntity;
import com.roadcrack.dao.mapper.OperationLogMapper;
import com.roadcrack.service.model.AuditLogRecord;
import com.roadcrack.service.service.AuditLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbAuditLogService implements AuditLogService {

    private final OperationLogMapper operationLogMapper;

    public DbAuditLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public PageResponse<AuditLogResponse> pageQuery(AuditLogPageQuery query) {
        int page = query.getPage() == null ? 1 : Math.max(query.getPage(), 1);
        int size = query.getSize() == null ? 10 : Math.max(query.getSize(), 1);

        Page<OperationLogEntity> queryPage = new Page<>(page, size);
        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<OperationLogEntity>()
                .orderByDesc(OperationLogEntity::getCreateTime)
                .orderByDesc(OperationLogEntity::getId);
        if (hasText(query.getModule())) {
            wrapper.eq(OperationLogEntity::getModule, query.getModule());
        }
        if (hasText(query.getAction())) {
            wrapper.eq(OperationLogEntity::getAction, query.getAction());
        }
        if (hasText(query.getUsername())) {
            wrapper.like(OperationLogEntity::getUsername, query.getUsername());
        }
        if (query.getStatus() != null) {
            wrapper.eq(OperationLogEntity::getStatus, query.getStatus());
        }
        if (hasText(query.getKeyword())) {
            wrapper.and(item -> item.like(OperationLogEntity::getDescription, query.getKeyword())
                    .or()
                    .like(OperationLogEntity::getParams, query.getKeyword())
                    .or()
                    .like(OperationLogEntity::getUsername, query.getKeyword()));
        }

        operationLogMapper.selectPage(queryPage, wrapper);
        java.util.List<AuditLogResponse> records = queryPage.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, queryPage.getTotal(), queryPage.getSize(), queryPage.getCurrent(), queryPage.getPages());
    }

    @Override
    public void record(AuditLogRecord record) {
        OperationLogEntity entity = new OperationLogEntity();
        applyRecord(record, entity);
        operationLogMapper.insert(entity);
    }

    private void applyRecord(AuditLogRecord record, OperationLogEntity entity) {
        AuditLogRequestContext.ResolvedContext context = AuditLogRequestContext.resolve();
        entity.setUserId(record.getUserId() != null ? record.getUserId() : context.userId());
        entity.setUsername(firstNonBlank(record.getUsername(), context.username(), "system"));
        entity.setModule(record.getModule());
        entity.setAction(record.getAction());
        entity.setDescription(record.getDescription());
        entity.setIp(firstNonBlank(record.getIp(), context.ip()));
        entity.setParams(record.getParams());
        entity.setStatus(record.getStatus() == null ? 1 : record.getStatus());
        entity.setErrorMsg(record.getErrorMsg());
        entity.setCostTime(record.getCostTime());
        entity.setCreateTime(record.getCreateTime() == null ? LocalDateTime.now() : record.getCreateTime());
    }

    private AuditLogResponse toResponse(OperationLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getUsername(),
                entity.getModule(),
                entity.getAction(),
                entity.getDescription(),
                entity.getIp(),
                entity.getParams(),
                entity.getStatus(),
                entity.getErrorMsg(),
                entity.getCostTime(),
                entity.getCreateTime()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
