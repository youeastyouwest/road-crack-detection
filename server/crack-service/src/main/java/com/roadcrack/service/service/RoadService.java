package com.roadcrack.service.service;

import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.PageResponse;

public interface RoadService {
    PageResponse<RoadResponse> page(int page, int size, String roadName, String district, String roadGrade, String status);
}