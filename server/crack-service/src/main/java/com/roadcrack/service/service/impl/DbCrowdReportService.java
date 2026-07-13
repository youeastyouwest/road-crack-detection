package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.enums.CrowdReportStatus;
import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.request.crowdreport.CreateCrowdReportRequest;
import com.roadcrack.api.request.crowdreport.ReviewCrowdReportRequest;
import com.roadcrack.api.response.crowdreport.CrowdReportResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.dao.entity.CrowdReportEntity;
import com.roadcrack.dao.mapper.CrowdReportMapper;
import com.roadcrack.service.service.CrowdReportService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbCrowdReportService implements CrowdReportService {

    private final CrowdReportMapper mapper;

    public DbCrowdReportService(CrowdReportMapper mapper) { this.mapper = mapper; }

    @Override
    public CrowdReportResponse submitReport(CreateCrowdReportRequest req) {
        CrowdReportEntity e = new CrowdReportEntity();
        e.setReportCode("CR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        e.setReporterName(req.reporterName()); e.setReporterPhone(req.reporterPhone());
        e.setLocation(req.location()); e.setLng(req.lng()); e.setLat(req.lat());
        e.setDamageType(req.damageType() != null ? req.damageType().name() : null);
        e.setSeverityLevel(req.severityLevel() != null ? req.severityLevel().name() : null);
        e.setDescription(req.description()); e.setImageUrl(req.imageUrl());
        e.setStatus(CrowdReportStatus.PENDING.name());
        e.setCreatedAt(LocalDateTime.now()); e.setUpdatedAt(LocalDateTime.now());
        mapper.insert(e);
        return toRes(e);
    }

    @Override
    public PageResponse<CrowdReportResponse> listReports(int page, int size, String status, String phone, String loc) {
        LambdaQueryWrapper<CrowdReportEntity> w = new LambdaQueryWrapper<CrowdReportEntity>().orderByDesc(CrowdReportEntity::getCreatedAt);
        if (status != null && !status.isBlank()) w.eq(CrowdReportEntity::getStatus, status);
        if (phone != null && !phone.isBlank()) w.eq(CrowdReportEntity::getReporterPhone, phone);
        if (loc != null && !loc.isBlank()) w.like(CrowdReportEntity::getLocation, loc);
        Page<CrowdReportEntity> r = mapper.selectPage(new Page<>(page, size), w);
        return new PageResponse<>(r.getRecords().stream().map(this::toRes).collect(Collectors.toList()),
            r.getTotal(), r.getSize(), r.getCurrent(), r.getPages());
    }

    @Override
    public CrowdReportResponse getReport(Long id) {
        CrowdReportEntity e = mapper.selectById(id);
        return e != null ? toRes(e) : null;
    }

    @Override
    public CrowdReportResponse reviewReport(Long id, ReviewCrowdReportRequest req, String reviewer) {
        CrowdReportEntity e = mapper.selectById(id);
        if (e == null) return null;
        e.setStatus(req.action()); e.setRemark(req.remark());
        e.setReviewedBy(reviewer); e.setReviewedAt(LocalDateTime.now()); e.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(e);
        return toRes(e);
    }

    private CrowdReportResponse toRes(CrowdReportEntity e) {
        DamageType dt = null; SeverityLevel sl = null; CrowdReportStatus crs = null;
        try { dt = DamageType.valueOf(e.getDamageType()); } catch (Exception ignored) {}
        try { sl = SeverityLevel.valueOf(e.getSeverityLevel()); } catch (Exception ignored) {}
        try { crs = CrowdReportStatus.valueOf(e.getStatus()); } catch (Exception ignored) {}
        return new CrowdReportResponse(e.getId(), e.getReportCode(), e.getReporterName(), e.getReporterPhone(),
            e.getLocation(), e.getLng(), e.getLat(), dt, sl, e.getDescription(), e.getImageUrl(),
            crs, e.getRemark(), e.getReviewedBy(), e.getReviewedAt(),
            e.getDetectionTaskId(), e.getWorkOrderId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
