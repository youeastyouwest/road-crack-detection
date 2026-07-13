package com.roadcrack.service.service;

import com.roadcrack.api.response.auditlog.AuditLogResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.model.AuditLogRecord;

public interface AuditLogService {
    PageResponse<AuditLogResponse> page(int page, int size, String module, String status);

    void record(String operator, String module, String action, String description,
                String ip, Long duration, String status, String errorMsg);

    default void record(AuditLogRecord record) {
        if (record == null) {
            return;
        }
        record(
                record.getUsername(),
                record.getModule(),
                record.getAction(),
                mergeDescription(record.getDescription(), record.getParams()),
                record.getIp(),
                record.getCostTime(),
                record.getStatus() != null && record.getStatus() == 1 ? "SUCCESS" : "FAIL",
                record.getErrorMsg()
        );
    }

    private static String mergeDescription(String description, String params) {
        if (params == null || params.isBlank()) {
            return description;
        }
        if (description == null || description.isBlank()) {
            return params;
        }
        return description + " | " + params;
    }
}
