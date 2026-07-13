package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.request.healtharchive.GenerateRoadHealthArchiveRequest;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveDashboardResponse;
import com.roadcrack.api.response.healtharchive.RoadHealthArchiveResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.RoadHealthArchiveService;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/road-health-archives")
public class RoadHealthArchiveController {

    private final RoadHealthArchiveService archiveService;

    public RoadHealthArchiveController(RoadHealthArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @PostMapping("/generate")
    public ApiResponse<RoadHealthArchiveResponse> generate(@Valid @RequestBody GenerateRoadHealthArchiveRequest request) {
        return ApiResponse.success(archiveService.generateArchive(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<RoadHealthArchiveResponse>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "roadId", required = false) Long roadId,
            @RequestParam(value = "damageLevel", required = false) String damageLevel,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(archiveService.listArchives(page, size, roadId, damageLevel, startDate, endDate));
    }

    @GetMapping("/{archiveId}")
    public ApiResponse<RoadHealthArchiveResponse> get(@PathVariable("archiveId") Long archiveId) {
        return ApiResponse.success(archiveService.getArchive(archiveId));
    }

    @GetMapping("/dashboard")
    public ApiResponse<RoadHealthArchiveDashboardResponse> dashboard() {
        return ApiResponse.success(archiveService.dashboard());
    }
}
