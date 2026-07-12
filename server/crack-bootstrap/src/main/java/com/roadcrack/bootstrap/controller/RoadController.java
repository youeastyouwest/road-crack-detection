package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.RoadService;

import org.springframework.web.bind.annotation.GetMapping;
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
}