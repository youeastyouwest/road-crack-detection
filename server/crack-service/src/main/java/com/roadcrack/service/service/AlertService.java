package com.roadcrack.service.service;

import com.roadcrack.api.response.alert.AlertResponse;
import com.roadcrack.common.model.PageResponse;
import java.util.List;

public interface AlertService {
    PageResponse<AlertResponse> page(int page, int size, String alertLevel, String alertType, String status);
    List<AlertResponse> recent(int count);
}