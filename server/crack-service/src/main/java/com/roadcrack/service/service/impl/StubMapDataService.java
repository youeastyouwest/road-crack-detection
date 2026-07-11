package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.response.map.MapDamageTypeRatioResponse;
import com.roadcrack.api.response.map.MapMarkerDetailResponse;
import com.roadcrack.api.response.map.MapMarkerResponse;
import com.roadcrack.api.response.map.MapStatisticsResponse;
import com.roadcrack.api.response.map.MapTrendPointResponse;
import com.roadcrack.service.service.MapDataService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class StubMapDataService implements MapDataService {
    @Override
    public List<MapMarkerResponse> listMarkers(DamageType damageType, SeverityLevel severityLevel, WorkOrderStatus status, Boolean hasWorkOrder, Boolean onlyWithCoordinates, String keyword) {
        return new ArrayList<>();
    }

    @Override
    public MapMarkerDetailResponse getMarkerDetail(Long markerId) {
        return new MapMarkerDetailResponse();
    }

    @Override
    public MapStatisticsResponse getStatistics() {
        return new MapStatisticsResponse();
    }

    @Override
    public List<MapTrendPointResponse> getTrend(int days) {
        return new ArrayList<>();
    }

    @Override
    public List<MapDamageTypeRatioResponse> getDamageTypeRatios() {
        return new ArrayList<>();
    }
}