package com.roadcrack.service.service;

import com.roadcrack.api.response.road.RoadDiseaseSummaryResponse;
import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.PageResponse;

import java.util.List;

public interface RoadService {
    PageResponse<RoadResponse> page(int page, int size, String roadName, String district, String roadGrade, String status);
    
    List<RoadResponse> listAll();
    
    List<RoadDiseaseSummaryResponse> getRoadsWithDisease();
}