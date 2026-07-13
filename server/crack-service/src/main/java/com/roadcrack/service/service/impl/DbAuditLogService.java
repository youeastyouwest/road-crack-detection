package com.roadcrack.service.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.response.auditlog.AuditLogResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.dao.entity.OperationLogEntity;
import com.roadcrack.dao.mapper.OperationLogMapper;
import com.roadcrack.service.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbAuditLogService implements AuditLogService {

    private final OperationLogMapper operationLogMapper;

    public DbAuditLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public PageResponse<AuditLogResponse> page(int page, int size, String module, String status) {
        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<OperationLogEntity>()
                .orderByDesc(OperationLogEntity::getCreateTime);

        if (module != null && !module.isBlank()) {
            wrapper.eq(OperationLogEntity::getModule, module);
        }
        if (status != null && !status.isBlank()) {
            if ("SUCCESS".equalsIgnoreCase(status) || "1".equals(status)) {
                wrapper.eq(OperationLogEntity::getStatus, 1);
            } else if ("FAIL".equalsIgnoreCase(status) || "0".equals(status)) {
                wrapper.eq(OperationLogEntity::getStatus, 0);
            }
        }

        Page<OperationLogEntity> result = operationLogMapper.selectPage(new Page<>(page, size), wrapper);

        List<AuditLogResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(records, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    @Override
    public void record(String operator, String module, String action, String description,
                       String ip, Long duration, String status, String errorMsg) {
        OperationLogEntity entity = new OperationLogEntity();
        entity.setUsername(operator);
        entity.setModule(module);
        entity.setAction(action);
        entity.setDescription(description);
        entity.setIp(ip);
        entity.setCostTime(duration != null ? duration : 0L);
        entity.setStatus("SUCCESS".equalsIgnoreCase(status) || "1".equals(status) ? 1 : 0);
        entity.setErrorMsg(errorMsg != null ? errorMsg : "");
        entity.setCreateTime(LocalDateTime.now());

        operationLogMapper.insert(entity);
    }

    private AuditLogResponse toResponse(OperationLogEntity entity) {
        AuditLogResponse resp = new AuditLogResponse();
        resp.setId(entity.getId());
        resp.setOperator(entity.getUsername());
        resp.setAction(entity.getAction());
        resp.setTarget(entity.getModule());
        resp.setDetail(entity.getDescription());
        resp.setIp(entity.getIp());
        resp.setDuration(entity.getCostTime());
        resp.setStatus(entity.getStatus() != null && entity.getStatus() == 1 ? "SUCCESS" : "FAIL");
        resp.setCreatedAt(entity.getCreateTime());
        return resp;
    }
}
