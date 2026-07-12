package com.roadcrack.service.service;

import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.common.model.PageResponse;

import java.util.List;

public interface DetectionTaskService {

    DetectionTaskResponse createTask(CreateDetectionTaskRequest request);

    void executeTask(Long taskId);

    PageResponse<DetectionTaskResponse> listTasks(int page,
                                                  int size,
                                                  DetectionTaskStatus status,
                                                  DataSourceType dataSourceType,
                                                  String location,
                                                  String submittedBy);

    DetectionTaskResponse getTask(Long taskId);

    DetectionResultResponse getResult(Long taskId);

    void deleteTask(Long taskId);

    /**
     * 批量更新检测结果的严重等级。仅超级管理员可调用。
     * @param taskIds 需要更新的检测任务 ID 列表
     * @param newSeverity 新的严重等级 (HIGH / MEDIUM / LOW / NORMAL)
     * @param operator 操作人用户名（用于审计日志）
     */
    void batchUpdateSeverity(List<Long> taskIds, String newSeverity, String operator);

    /**
     * 批量删除检测任务及其关联数据。仅超级管理员可调用。
     * @param taskIds 需要删除的检测任务 ID 列表
     * @param operator 操作人用户名（用于审计日志）
     */
    void batchDeleteTasks(List<Long> taskIds, String operator);
}
