package com.roadcrack.service.service;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.response.map.MapDamageTypeRatioResponse;
import com.roadcrack.api.response.map.MapMarkerDetailResponse;
import com.roadcrack.api.response.map.MapMarkerResponse;
import com.roadcrack.api.response.map.MapStatisticsResponse;
import com.roadcrack.api.response.map.MapTrendPointResponse;

import java.util.List;

public interface MapDataService {

    List<MapMarkerResponse> listMarkers(DamageType damageType,
                                        SeverityLevel severityLevel,
                                        WorkOrderStatus status,
                                        Boolean hasWorkOrder,
                                        Boolean onlyWithCoordinates,
                                        String keyword);

    MapMarkerDetailResponse getMarkerDetail(Long markerId);

    MapStatisticsResponse getStatistics();

    List<MapTrendPointResponse> getTrend(int days);

    List<MapDamageTypeRatioResponse> getDamageTypeRatios();
}
