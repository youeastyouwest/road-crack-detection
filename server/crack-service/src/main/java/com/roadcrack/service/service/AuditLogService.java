package com.roadcrack.service.service;

import com.roadcrack.api.response.auditlog.AuditLogResponse;
import com.roadcrack.common.model.PageResponse;

public interface AuditLogService {
    PageResponse<AuditLogResponse> page(int page, int size, String module, String status);
}
