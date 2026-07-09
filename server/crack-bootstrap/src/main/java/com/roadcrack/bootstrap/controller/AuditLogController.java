package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.request.auditlog.AuditLogPageQuery;
import com.roadcrack.api.response.auditlog.AuditLogResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<AuditLogResponse>> page(AuditLogPageQuery query) {
        return ApiResponse.success(auditLogService.pageQuery(query));
    }
}
