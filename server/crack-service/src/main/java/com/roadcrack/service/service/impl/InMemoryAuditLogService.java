package com.roadcrack.service.service.impl;

import com.roadcrack.api.request.auditlog.AuditLogPageQuery;
import com.roadcrack.api.response.auditlog.AuditLogResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.model.AuditLogRecord;
import com.roadcrack.service.service.AuditLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuditLogService implements AuditLogService {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<AuditLogResponse> store = new CopyOnWriteArrayList<>();

    @Override
    public PageResponse<AuditLogResponse> pageQuery(AuditLogPageQuery query) {
        int page = query.getPage() == null ? 1 : Math.max(query.getPage(), 1);
        int size = query.getSize() == null ? 10 : Math.max(query.getSize(), 1);

        List<AuditLogResponse> filtered = store.stream()
                .filter(item -> !hasText(query.getModule()) || query.getModule().equals(item.module()))
                .filter(item -> !hasText(query.getAction()) || query.getAction().equals(item.action()))
                .filter(item -> !hasText(query.getUsername()) || containsIgnoreCase(item.username(), query.getUsername()))
                .filter(item -> query.getStatus() == null || query.getStatus().equals(item.status()))
                .filter(item -> !hasText(query.getKeyword()) || matchesKeyword(item, query.getKeyword()))
                .sorted(Comparator.comparing(AuditLogResponse::createTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AuditLogResponse::id, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int fromIndex = Math.min((page - 1) * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        long total = filtered.size();
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        return new PageResponse<>(filtered.subList(fromIndex, toIndex), total, size, page, pages);
    }

    @Override
    public void record(AuditLogRecord record) {
        AuditLogRequestContext.ResolvedContext context = AuditLogRequestContext.resolve();
        store.add(new AuditLogResponse(
                idGenerator.getAndIncrement(),
                record.getUserId() != null ? record.getUserId() : context.userId(),
                firstNonBlank(record.getUsername(), context.username(), "system"),
                record.getModule(),
                record.getAction(),
                record.getDescription(),
                firstNonBlank(record.getIp(), context.ip()),
                record.getParams(),
                record.getStatus() == null ? 1 : record.getStatus(),
                record.getErrorMsg(),
                record.getCostTime(),
                record.getCreateTime() == null ? LocalDateTime.now() : record.getCreateTime()
        ));
    }

    private boolean matchesKeyword(AuditLogResponse item, String keyword) {
        return containsIgnoreCase(item.description(), keyword)
                || containsIgnoreCase(item.params(), keyword)
                || containsIgnoreCase(item.username(), keyword);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null
                && keyword != null
                && source.toLowerCase().contains(keyword.toLowerCase());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
