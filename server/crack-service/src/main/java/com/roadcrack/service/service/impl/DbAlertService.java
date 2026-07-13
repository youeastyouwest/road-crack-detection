package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.response.alert.AlertResponse;
import com.roadcrack.api.response.alert.AlertStatsResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.dao.entity.AlertEntity;
import com.roadcrack.dao.mapper.AlertMapper;
import com.roadcrack.service.service.AlertService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbAlertService implements AlertService {

    private final AlertMapper alertMapper;

    public DbAlertService(AlertMapper alertMapper) { this.alertMapper = alertMapper; }

    @Override
    public PageResponse<AlertResponse> page(int page, int size, String alertLevel, String alertType, String status) {
        LambdaQueryWrapper<AlertEntity> w = new LambdaQueryWrapper<AlertEntity>().orderByDesc(AlertEntity::getCreatedAt);
        if (alertLevel != null && !alertLevel.isBlank()) w.eq(AlertEntity::getAlertLevel, alertLevel);
        if (alertType != null && !alertType.isBlank()) w.eq(AlertEntity::getAlertType, alertType);
        if (status != null && !status.isBlank()) w.eq(AlertEntity::getStatus, status);
        Page<AlertEntity> r = alertMapper.selectPage(new Page<>(page, size), w);
        return new PageResponse<>(r.getRecords().stream().map(this::toRes).collect(Collectors.toList()),
            r.getTotal(), r.getSize(), r.getCurrent(), r.getPages());
    }

    @Override
    public List<AlertResponse> recent(int count) {
        return alertMapper.selectList(new LambdaQueryWrapper<AlertEntity>()
            .orderByDesc(AlertEntity::getCreatedAt).last("LIMIT " + count))
            .stream().map(this::toRes).collect(Collectors.toList());
    }

    @Override
    public AlertStatsResponse stats() {
        long total = alertMapper.selectCount(null);
        long high = alertMapper.selectCount(new LambdaQueryWrapper<AlertEntity>().eq(AlertEntity::getAlertLevel, "HIGH"));
        long medium = alertMapper.selectCount(new LambdaQueryWrapper<AlertEntity>().eq(AlertEntity::getAlertLevel, "MEDIUM"));
        long low = alertMapper.selectCount(new LambdaQueryWrapper<AlertEntity>().eq(AlertEntity::getAlertLevel, "LOW"));
        long pending = alertMapper.selectCount(new LambdaQueryWrapper<AlertEntity>().eq(AlertEntity::getStatus, "PENDING"));
        long handled = alertMapper.selectCount(new LambdaQueryWrapper<AlertEntity>().eq(AlertEntity::getStatus, "HANDLED"));
        return new AlertStatsResponse((int) high, (int) medium, (int) low, (int) pending, (int) handled, (int) total);
    }

    @Override
    public AlertResponse handle(Long id, String handledBy, String handleRemark) {
        AlertEntity entity = alertMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("告警不存在: " + id);
        }
        entity.setStatus("HANDLED");
        entity.setHandledBy(handledBy);
        entity.setHandleRemark(handleRemark);
        entity.setHandledAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        alertMapper.updateById(entity);
        return toRes(entity);
    }

    private AlertResponse toRes(AlertEntity e) {
        return new AlertResponse(e.getId(), e.getAlertCode(), e.getAlertType(), e.getAlertLevel(),
            e.getTitle(), e.getContent(), e.getDamageType(), e.getLocation(),
            e.getWorkOrderId(), e.getDetectionTaskId(), e.getStatus(),
            e.getHandledBy(), e.getHandledAt(), e.getHandleRemark(), e.getCreatedAt());
    }
}
