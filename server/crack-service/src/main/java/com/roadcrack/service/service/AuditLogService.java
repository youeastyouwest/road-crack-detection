package com.roadcrack.service.service;

import com.roadcrack.api.request.auditlog.AuditLogPageQuery;
import com.roadcrack.api.response.auditlog.AuditLogResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.model.AuditLogRecord;

public interface AuditLogService {

    PageResponse<AuditLogResponse> pageQuery(AuditLogPageQuery query);

    void record(AuditLogRecord record);
}
