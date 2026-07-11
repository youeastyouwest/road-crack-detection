package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.CrowdReportStatus;
import com.roadcrack.api.request.crowdreport.CreateCrowdReportRequest;
import com.roadcrack.api.request.crowdreport.ReviewCrowdReportRequest;
import com.roadcrack.api.response.crowdreport.CrowdReportResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.CrowdReportService;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class StubCrowdReportService implements CrowdReportService {
    @Override
    public CrowdReportResponse submitReport(CreateCrowdReportRequest request) {
        return new CrowdReportResponse(
            System.currentTimeMillis(), null, null, null,
            request.location(), request.lng(), request.lat(),
            request.damageType(), request.severityLevel(),
            request.description(), request.imageUrl(),
            CrowdReportStatus.PENDING, null, null, null,
            null, null, null, null
        );
    }

    @Override
    public PageResponse<CrowdReportResponse> listReports(int page, int size, String status, String reporterPhone, String location) {
        return new PageResponse<>(Collections.emptyList(), size, page, 0L, 0L);
    }

    @Override
    public CrowdReportResponse getReport(Long reportId) {
        return new CrowdReportResponse(
            reportId, null, null, null,
            null, null, null, null, null,
            null, null, CrowdReportStatus.PENDING,
            null, null, null, null, null, null, null
        );
    }

    @Override
    public CrowdReportResponse reviewReport(Long reportId, ReviewCrowdReportRequest request, String reviewer) {
        return new CrowdReportResponse(
            reportId, null, null, null,
            null, null, null, null, null,
            null, null, CrowdReportStatus.APPROVED,
            request.remark(), reviewer, null,
            null, null, null, null
        );
    }
}