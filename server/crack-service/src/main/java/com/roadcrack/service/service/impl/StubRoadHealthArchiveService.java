package com.roadcrack.service.service.impl;

import com.roadcrack.api.request.healtharchive.GenerateRoadHealthArchiveRequest;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveDashboardResponse;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.RoadHealthArchiveService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Collections;

@Service
public class StubRoadHealthArchiveService implements RoadHealthArchiveService {
    @Override
    public RoadHealthArchiveResponse generateArchive(GenerateRoadHealthArchiveRequest request) {
        return new RoadHealthArchiveResponse(
            null, null, null, LocalDate.now(), null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null
        );
    }

    @Override
    public PageResponse<RoadHealthArchiveResponse> listArchives(int page, int size, Long roadId, String damageLevel, LocalDate startDate, LocalDate endDate) {
        return new PageResponse<>(Collections.emptyList(), 0L, size, page, 0L);
    }

    @Override
    public RoadHealthArchiveResponse getArchive(Long archiveId) {
        return new RoadHealthArchiveResponse(
            null, null, null, LocalDate.now(), null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null
        );
    }

    @Override
    public RoadHealthArchiveDashboardResponse dashboard() {
        return new RoadHealthArchiveDashboardResponse(null, null, null, null, null, null);
    }
}