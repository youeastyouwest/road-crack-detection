package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.request.healtharchive.GenerateRoadHealthArchiveRequest;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveDashboardResponse;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.dao.entity.RoadEntity;
import com.roadcrack.dao.entity.RoadHealthArchiveEntity;
import com.roadcrack.dao.mapper.RoadHealthArchiveMapper;
import com.roadcrack.dao.mapper.RoadMapper;
import com.roadcrack.service.service.RoadHealthArchiveService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbRoadHealthArchiveService implements RoadHealthArchiveService {

    private final RoadHealthArchiveMapper archiveMapper;
    private final RoadMapper roadMapper;

    public DbRoadHealthArchiveService(RoadHealthArchiveMapper archiveMapper, RoadMapper roadMapper) {
        this.archiveMapper = archiveMapper; this.roadMapper = roadMapper;
    }

    @Override
    public RoadHealthArchiveResponse generateArchive(GenerateRoadHealthArchiveRequest req) {
        RoadEntity road = roadMapper.selectById(req.roadId());
        if (road == null) return null;
        RoadHealthArchiveEntity e = new RoadHealthArchiveEntity();
        e.setRoadId(req.roadId());
        e.setArchiveDate(req.archiveDate() != null ? req.archiveDate() : LocalDate.now());
        e.setHealthScore(road.getHealthScore()); e.setDamageLevel(road.getDamageLevel());
        e.setTotalDetectionCount(road.getTotalDetectionCount()); e.setTotalDamageCount(road.getCurrentDamageCount());
        e.setCreatedAt(LocalDateTime.now()); e.setUpdatedAt(LocalDateTime.now());
        archiveMapper.insert(e);
        return toRes(e);
    }

    @Override
    public PageResponse<RoadHealthArchiveResponse> listArchives(int page, int size, Long roadId, String dl, LocalDate sd, LocalDate ed) {
        LambdaQueryWrapper<RoadHealthArchiveEntity> w = new LambdaQueryWrapper<RoadHealthArchiveEntity>().orderByDesc(RoadHealthArchiveEntity::getCreatedAt);
        if (roadId != null) w.eq(RoadHealthArchiveEntity::getRoadId, roadId);
        if (dl != null && !dl.isBlank()) w.eq(RoadHealthArchiveEntity::getDamageLevel, dl);
        if (sd != null) w.ge(RoadHealthArchiveEntity::getArchiveDate, sd);
        if (ed != null) w.le(RoadHealthArchiveEntity::getArchiveDate, ed);
        Page<RoadHealthArchiveEntity> r = archiveMapper.selectPage(new Page<>(page, size), w);
        return new PageResponse<>(r.getRecords().stream().map(this::toRes).collect(Collectors.toList()),
            r.getTotal(), r.getSize(), r.getCurrent(), r.getPages());
    }

    @Override
    public RoadHealthArchiveResponse getArchive(Long id) {
        RoadHealthArchiveEntity e = archiveMapper.selectById(id);
        return e != null ? toRes(e) : null;
    }

    @Override
    public RoadHealthArchiveDashboardResponse dashboard() {
        List<RoadHealthArchiveEntity> all = archiveMapper.selectList(null);
        List<RoadEntity> roads = roadMapper.selectList(null);
        long archived = all.stream().map(RoadHealthArchiveEntity::getRoadId).distinct().count();
        int healthy = 0, sub = 0, unhealthy = 0;
        double avg = 0;
        for (RoadHealthArchiveEntity a : all) {
            if (a.getHealthScore() != null) avg += a.getHealthScore().doubleValue();
            String l = a.getDamageLevel();
            if ("HEALTHY".equals(l)) healthy++; else if ("SUB_HEALTHY".equals(l)) sub++; else unhealthy++;
        }
        if (!all.isEmpty()) avg /= all.size();
        return new RoadHealthArchiveDashboardResponse(
            (long) roads.size(), archived, BigDecimal.valueOf(avg),
            (long) healthy, (long) sub, (long) unhealthy
        );
    }

    private RoadHealthArchiveResponse toRes(RoadHealthArchiveEntity e) {
        return new RoadHealthArchiveResponse(
            e.getId(), e.getRoadId(), null, e.getArchiveDate(),
            e.getHealthScore(), e.getDamageLevel(),
            e.getTotalDetectionCount(), e.getTotalDamageCount(),
            e.getCrackCount(), e.getPotholeCount(),
            e.getMarkingDamageCount(), e.getRoadSpillCount(), e.getUnknownCount(),
            e.getSeverityLowCount(), e.getSeverityMediumCount(), e.getSeverityHighCount(),
            e.getEvaluation(), e.getSuggestion(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
