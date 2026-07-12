package com.roadcrack.service.service.impl;

import com.roadcrack.api.response.road.RoadDiseaseSummaryResponse;
import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.RoadService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "jpa", matchIfMissing = false)
public class StubRoadService implements RoadService {
    @Override
    public PageResponse<RoadResponse> page(int page, int size, String roadName, String district, String roadGrade, String status) {
        return new PageResponse<>(Collections.emptyList(), 0L, size, page, 0L);
    }

    @Override
    public List<RoadResponse> listAll() {
        return new ArrayList<>();
    }

    @Override
    public List<RoadDiseaseSummaryResponse> getRoadsWithDisease() {
        return new ArrayList<>();
    }

    @Override
    public List<RoadResponse> listRoadsWithDetections() {
        return new ArrayList<>();
    }
}