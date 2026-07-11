package com.roadcrack.service.service;

import com.roadcrack.api.request.healtharchive.GenerateRoadHealthArchiveRequest;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveDashboardResponse;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveResponse;
import com.roadcrack.common.model.PageResponse;
import java.time.LocalDate;

public interface RoadHealthArchiveService {
    RoadHealthArchiveResponse generateArchive(GenerateRoadHealthArchiveRequest request);
    PageResponse<RoadHealthArchiveResponse> listArchives(int page, int size, Long roadId, String damageLevel, LocalDate startDate, LocalDate endDate);
    RoadHealthArchiveResponse getArchive(Long archiveId);
    RoadHealthArchiveDashboardResponse dashboard();
}