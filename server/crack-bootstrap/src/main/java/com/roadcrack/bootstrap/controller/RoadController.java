package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.RoadEntity;
import com.roadcrack.service.service.RoadService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    private final RoadService roadService;

    public RoadController(RoadService roadService) {
        this.roadService = roadService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RoadResponse>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "roadName", required = false) String roadName,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "roadGrade", required = false) String roadGrade,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(roadService.page(page, size, roadName, district, roadGrade, status));
    }

    @GetMapping("/with-detections")
    public ApiResponse<List<RoadResponse>> listRoadsWithDetections() {
        return ApiResponse.success(roadService.listRoadsWithDetections());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoadEntity> detail(@PathVariable("id") Long id) {
        RoadEntity road = roadService.getById(id);
        if (road == null) {
            throw new BusinessException(ResultCode.ROAD_NOT_FOUND, "road not found: " + id);
        }
        return ApiResponse.success(road);
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody RoadEntity road) {
        roadService.createRoad(road);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable("id") Long id, @RequestBody RoadEntity road) {
        road.setId(id);
        roadService.updateRoad(road);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        roadService.deleteRoad(id);
        return ApiResponse.success(null);
    }
}