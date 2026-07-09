package com.roadcrack.api.response.auditlog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long userId,
        String username,
        String module,
        String action,
        String description,
        String ip,
        String params,
        Integer status,
        String errorMsg,
        Long costTime,
        LocalDateTime createTime
) {
}
