package com.roadcrack.service.service;

import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.common.model.PageResponse;

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
}
