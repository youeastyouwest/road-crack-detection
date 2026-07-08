package com.roadcrack.service.service;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.workorder.AssignWorkOrderRequest;
import com.roadcrack.api.request.workorder.CancelWorkOrderRequest;
import com.roadcrack.api.request.workorder.CreateWorkOrderRequest;
import com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.common.model.PageResponse;

public interface WorkOrderService {

    WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request);

    WorkOrderResponse createFromDetection(Long detectionTaskId,
                                          DamageType damageType,
                                          SeverityLevel severityLevel,
                                          String location,
                                          String evidenceUrl);

    PageResponse<WorkOrderResponse> listWorkOrders(int page,
                                                   int size,
                                                   WorkOrderStatus status,
                                                   DepartmentCode departmentCode,
                                                   SeverityLevel severityLevel,
                                                   String assignee,
                                                   String keyword);

    WorkOrderResponse getWorkOrder(Long workOrderId);

    WorkOrderResponse assignWorkOrder(Long workOrderId, AssignWorkOrderRequest request);

    WorkOrderResponse updateStatus(Long workOrderId, UpdateWorkOrderStatusRequest request);

    WorkOrderResponse cancelWorkOrder(Long workOrderId, CancelWorkOrderRequest request);

    void closeByReport(Long workOrderId, String note);
}
