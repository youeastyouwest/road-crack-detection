package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.request.detection.BatchDeleteRequest;
import com.roadcrack.api.request.detection.BatchSeverityUpdateRequest;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.UserEntity;
import com.roadcrack.dao.mapper.UserMapper;
import com.roadcrack.service.service.DetectionTaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/detection-tasks")
public class DetectionTaskController {

    private final DetectionTaskService detectionTaskService;
    private final UserMapper userMapper;

    @Value("${crack.upload.dir:uploads}")
    private String uploadDir;

    public DetectionTaskController(DetectionTaskService detectionTaskService, UserMapper userMapper) {
        this.detectionTaskService = detectionTaskService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public ApiResponse<DetectionTaskResponse> createTask(
            @RequestBody CreateDetectionTaskRequest request,
            HttpServletRequest httpRequest) {
        CreateDetectionTaskRequest payload = new CreateDetectionTaskRequest(
                request.dataSourceType(),
                request.fileName(),
                request.fileUrl(),
                request.location(),
                request.remark(),
                request.roadName(),
                resolveSubmittedBy(httpRequest)
        );
        DetectionTaskResponse task = detectionTaskService.createTask(payload);
        detectionTaskService.executeTask(task.id());
        return ApiResponse.success(task);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DetectionTaskResponse> createTaskWithFile(
            @RequestParam("dataSourceType") DataSourceType dataSourceType,
            @RequestParam("location") String location,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "roadName", required = false) String roadName,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) throws IOException {

        String fileName = file != null ? file.getOriginalFilename() : "unknown";
        String fileUrl = "";

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + (fileName != null ? fileName : "upload");
            Path targetPath = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            fileUrl = "/uploads/" + storedName;
        }

        CreateDetectionTaskRequest req = new CreateDetectionTaskRequest(
                dataSourceType, fileName, fileUrl, location, remark, roadName, resolveSubmittedBy(request)
        );
        DetectionTaskResponse task = detectionTaskService.createTask(req);
        detectionTaskService.executeTask(task.id());
        return ApiResponse.success(task);
    }

    @PostMapping("/{taskId}/execute")
    public ApiResponse<Void> executeTask(@PathVariable("taskId") Long taskId) {
        detectionTaskService.executeTask(taskId);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<PageResponse<DetectionTaskResponse>> listTasks(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) DetectionTaskStatus status,
            @RequestParam(value = "dataSourceType", required = false) DataSourceType dataSourceType,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "submittedBy", required = false) String submittedBy) {
        return ApiResponse.success(detectionTaskService.listTasks(page, size, status, dataSourceType, location, submittedBy));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<DetectionTaskResponse> getTask(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(detectionTaskService.getTask(taskId));
    }

    @GetMapping("/{taskId}/result")
    public ApiResponse<DetectionResultResponse> getResult(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(detectionTaskService.getResult(taskId));
    }

    /**
     * 批量更新检测结果严重等级（仅超级管理员）
     */
    @PutMapping("/batch-severity")
    public ApiResponse<Void> batchUpdateSeverity(@RequestBody BatchSeverityUpdateRequest req,
                                                  HttpServletRequest request) {
        checkAdminRole(request);
        String operator = getOperatorName(request);
        detectionTaskService.batchUpdateSeverity(req.taskIds(), req.newSeverity(), operator);
        return ApiResponse.success(null);
    }

    /**
     * 批量删除检测结果（仅超级管理员）
     * 注意：此端点必须在 @DeleteMapping("/{taskId}") 之前声明，
     * 否则 Spring MVC 会将 /batch 路径匹配到 /{taskId}，导致 400。
     */
    @DeleteMapping("/batch")
    public ApiResponse<Void> batchDelete(@RequestBody BatchDeleteRequest req,
                                          HttpServletRequest request) {
        checkAdminRole(request);
        String operator = getOperatorName(request);
        detectionTaskService.batchDeleteTasks(req.taskIds(), operator);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable("taskId") Long taskId, HttpServletRequest request) {
        // 仅超级管理员可删除检测结果
        checkAdminRole(request);
        detectionTaskService.deleteTask(taskId);
        return ApiResponse.success(null);
    }

    /**
     * 校验当前用户是否为超级管理员
     */
    private void checkAdminRole(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            UserEntity user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可执行此操作");
            }
            List<String> roleCodes = userMapper.selectRoleCodesByUserId(userId);
            if (roleCodes == null || !roleCodes.contains("ROLE_ADMIN")) {
                throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可执行此操作");
            }
        }
    }

    /**
     * 从请求中获取操作人名称
     */
    private String getOperatorName(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return username != null ? username : "admin";
    }

    private String resolveSubmittedBy(HttpServletRequest request) {
        String userNameHeader = request.getHeader("X-User-Name");
        if (userNameHeader != null && !userNameHeader.isBlank()) {
            return userNameHeader;
        }
        String username = (String) request.getAttribute("username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        return "admin";
    }
}
