package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.response.map.MapDamageTypeRatioResponse;
import com.roadcrack.api.response.map.MapMarkerDetailResponse;
import com.roadcrack.api.response.map.MapMarkerResponse;
import com.roadcrack.api.response.map.MapStatisticsResponse;
import com.roadcrack.api.response.map.MapTrendPointResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.service.service.MapDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapDataService mapDataService;

    public MapController(MapDataService mapDataService) {
        this.mapDataService = mapDataService;
    }

    @GetMapping("/markers")
    public ApiResponse<List<MapMarkerResponse>> listMarkers(@RequestParam(required = false) DamageType damageType,
                                                            @RequestParam(required = false) SeverityLevel severityLevel,
                                                            @RequestParam(required = false) WorkOrderStatus status,
                                                            @RequestParam(required = false) Boolean hasWorkOrder,
                                                            @RequestParam(required = false) Boolean onlyWithCoordinates,
                                                            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(mapDataService.listMarkers(
                damageType,
                severityLevel,
                status,
                hasWorkOrder,
                onlyWithCoordinates,
                keyword
        ));
    }

    @GetMapping("/markers/{markerId}")
    public ApiResponse<MapMarkerDetailResponse> getMarkerDetail(@PathVariable Long markerId) {
        return ApiResponse.success(mapDataService.getMarkerDetail(markerId));
    }

    @GetMapping("/statistics")
    public ApiResponse<MapStatisticsResponse> getStatistics() {
        return ApiResponse.success(mapDataService.getStatistics());
    }

    @GetMapping("/trend")
    public ApiResponse<List<MapTrendPointResponse>> getTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(mapDataService.getTrend(days));
    }

    @GetMapping("/damage-types")
    public ApiResponse<List<MapDamageTypeRatioResponse>> getDamageTypeRatios() {
        return ApiResponse.success(mapDataService.getDamageTypeRatios());
    }
}
