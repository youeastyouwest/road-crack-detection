package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.DetectionTaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/detection-tasks")
public class DetectionTaskController {

    private final DetectionTaskService detectionTaskService;

    @Value("${crack.upload.dir:uploads}")
    private String uploadDir;

    public DetectionTaskController(DetectionTaskService detectionTaskService) {
        this.detectionTaskService = detectionTaskService;
    }

    @PostMapping
    public ApiResponse<DetectionTaskResponse> createTask(
            @RequestBody CreateDetectionTaskRequest request) {
        DetectionTaskResponse task = detectionTaskService.createTask(request);
        detectionTaskService.executeTask(task.id());
        return ApiResponse.success(task);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DetectionTaskResponse> createTaskWithFile(
            @RequestParam("dataSourceType") DataSourceType dataSourceType,
            @RequestParam("location") String location,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {

        String fileName = file != null ? file.getOriginalFilename() : "unknown";
        String fileUrl = "";

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + (fileName != null ? fileName : "upload");
            Path targetPath = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            fileUrl = targetPath.toAbsolutePath().toString();
        }

        CreateDetectionTaskRequest req = new CreateDetectionTaskRequest(
                dataSourceType, fileName, fileUrl, location, remark
        );
        DetectionTaskResponse task = detectionTaskService.createTask(req);
        detectionTaskService.executeTask(task.id());
        return ApiResponse.success(task);
    }

    @PostMapping("/{taskId}/execute")
    public ApiResponse<Void> executeTask(@PathVariable Long taskId) {
        detectionTaskService.executeTask(taskId);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<PageResponse<DetectionTaskResponse>> listTasks(
            @RequestParam(defaultValue = "1") int page,
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
