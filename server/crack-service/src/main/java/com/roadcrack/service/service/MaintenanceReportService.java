package com.roadcrack.service.service;

import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.response.report.MaintenanceReportResponse;
import com.roadcrack.common.model.PageResponse;

public interface MaintenanceReportService {

    MaintenanceReportResponse createReport(CreateMaintenanceReportRequest request);

    PageResponse<MaintenanceReportResponse> listReports(int page, int size, Long workOrderId, String executor);

    MaintenanceReportResponse getReport(Long reportId);
}
