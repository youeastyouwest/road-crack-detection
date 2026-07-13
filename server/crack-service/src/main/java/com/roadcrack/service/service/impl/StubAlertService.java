package com.roadcrack.service.service.impl;

import com.roadcrack.api.response.alert.AlertResponse;
import com.roadcrack.api.response.alert.AlertStatsResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.AlertService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = false)
public class StubAlertService implements AlertService {
    @Override
    public PageResponse<AlertResponse> page(int page, int size, String alertLevel, String alertType, String status) {
        return new PageResponse<>(Collections.emptyList(), size, page, 0L, 0L);
    }

    @Override
    public List<AlertResponse> recent(int count) {
        return new ArrayList<>();
    }

    @Override
    public AlertStatsResponse stats() {
        return new AlertStatsResponse(0, 0, 0, 0, 0, 0);
    }

    @Override
    public AlertResponse handle(Long id, String handledBy, String handleRemark) {
        return new AlertResponse(id, null, null, null, null, null, null, null, null, null, "HANDLED",
            handledBy, null, handleRemark, null);
    }
}