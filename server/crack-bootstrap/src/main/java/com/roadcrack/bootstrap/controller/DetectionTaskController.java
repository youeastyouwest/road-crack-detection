package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.DetectionTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/detection-tasks")
public class DetectionTaskController {

    private final DetectionTaskService detectionTaskService;

    public DetectionTaskController(DetectionTaskService detectionTaskService) {
        this.detectionTaskService = detectionTaskService;
    }

    @PostMapping
    public ApiResponse<DetectionTaskResponse> createTask(@Valid @RequestBody CreateDetectionTaskRequest request) {
        return ApiResponse.success(detectionTaskService.createTask(request));
    }

    @PostMapping("/{taskId}/execute")
    public ApiResponse<Void> executeTask(@PathVariable Long taskId) {
        detectionTaskService.executeTask(taskId);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<PageResponse<DetectionTaskResponse>> listTasks(@RequestParam(defaultValue = "1") int page,
                                                                      @RequestParam(defaultValue = "10") int size,
                                                                      @RequestParam(required = false) DetectionTaskStatus status,
                                                                      @RequestParam(required = false) DataSourceType dataSourceType,
                                                                      @RequestParam(required = false) String location,
                                                                      @RequestParam(required = false) String submittedBy) {
        return ApiResponse.success(detectionTaskService.listTasks(page, size, status, dataSourceType, location, submittedBy));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<DetectionTaskResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(detectionTaskService.getTask(taskId));
    }

    @GetMapping("/{taskId}/result")
    public ApiResponse<DetectionResultResponse> getResult(@PathVariable Long taskId) {
        return ApiResponse.success(detectionTaskService.getResult(taskId));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
        detectionTaskService.deleteTask(taskId);
        return ApiResponse.success(null);
    }
}
