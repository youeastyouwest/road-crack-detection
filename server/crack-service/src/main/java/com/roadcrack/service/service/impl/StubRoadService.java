package com.roadcrack.service.service.impl;

import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.RoadService;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class StubRoadService implements RoadService {
    @Override
    public PageResponse<RoadResponse> page(int page, int size, String roadName, String district, String roadGrade, String status) {
        return new PageResponse<>(Collections.emptyList(), 0L, size, page, 0L);
    }
}