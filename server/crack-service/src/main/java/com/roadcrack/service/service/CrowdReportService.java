package com.roadcrack.service.service;

import com.roadcrack.api.request.crowdreport.CreateCrowdReportRequest;
import com.roadcrack.api.request.crowdreport.ReviewCrowdReportRequest;
import com.roadcrack.api.response.crowdreport.CrowdReportResponse;
import com.roadcrack.common.model.PageResponse;

public interface CrowdReportService {
    CrowdReportResponse submitReport(CreateCrowdReportRequest request);
    PageResponse<CrowdReportResponse> listReports(int page, int size, String status, String reporterPhone, String location);
    CrowdReportResponse getReport(Long reportId);
    CrowdReportResponse reviewReport(Long reportId, ReviewCrowdReportRequest request, String reviewer);
}