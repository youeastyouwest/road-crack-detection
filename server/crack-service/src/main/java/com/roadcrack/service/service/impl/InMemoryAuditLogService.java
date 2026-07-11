package com.roadcrack.service.service.impl;

import com.roadcrack.api.response.auditlog.AuditLogResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.AuditLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuditLogService implements AuditLogService {

    private final List<AuditLogResponse> logs = Collections.synchronizedList(new ArrayList<>());

    @Override
    public PageResponse<AuditLogResponse> page(int page, int size, String module, String status) {
        int start = (page - 1) * size;
        int end = Math.min(start + size, logs.size());
        List<AuditLogResponse> list = start >= logs.size() ? Collections.emptyList() : logs.subList(start, end);
        return new PageResponse<>(list, logs.size(), size, page, (long) Math.ceil((double) logs.size() / size));
    }
}
