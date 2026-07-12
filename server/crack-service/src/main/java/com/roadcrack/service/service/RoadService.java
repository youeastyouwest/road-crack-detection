package com.roadcrack.service.service;

import com.roadcrack.api.response.road.RoadDiseaseSummaryResponse;
import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.dao.entity.RoadEntity;

import java.util.List;

public interface RoadService {
    PageResponse<RoadResponse> page(int page, int size, String roadName, String district, String roadGrade, String status);
    
    List<RoadResponse> listAll();
    
    List<RoadDiseaseSummaryResponse> getRoadsWithDisease();

    /**
     * 获取所有有病害检测记录的道路（用于生成健康档案下拉框）。
     * 包含 road 表中的道路以及通过检测坐标匹配到的道路。
     */
    List<RoadResponse> listRoadsWithDetections();

    RoadEntity getById(Long id);

    void createRoad(RoadEntity road);

    void updateRoad(RoadEntity road);

    void deleteRoad(Long id);
}