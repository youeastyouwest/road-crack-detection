package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.workorder.AssignWorkOrderRequest;
import com.roadcrack.api.request.workorder.CancelWorkOrderRequest;
import com.roadcrack.api.request.workorder.CreateWorkOrderRequest;
import com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    public ApiResponse<WorkOrderResponse> createWorkOrder(@Valid @RequestBody CreateWorkOrderRequest request) {
        return ApiResponse.success(workOrderService.createWorkOrder(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<WorkOrderResponse>> listWorkOrders(@RequestParam(defaultValue = "1") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @RequestParam(required = false) WorkOrderStatus status,
                                                                       @RequestParam(required = false) DepartmentCode departmentCode,
                                                                       @RequestParam(required = false) SeverityLevel severityLevel,
                                                                       @RequestParam(required = false) String assignee,
                                                                       @RequestParam(required = false) String keyword) {
        return ApiResponse.success(workOrderService.listWorkOrders(page, size, status, departmentCode, severityLevel, assignee, keyword));
    }

    @GetMapping("/{workOrderId}")
    public ApiResponse<WorkOrderResponse> getWorkOrder(@PathVariable Long workOrderId) {
        return ApiResponse.success(workOrderService.getWorkOrder(workOrderId));
    }

    @PutMapping("/{workOrderId}/assign")
    public ApiResponse<WorkOrderResponse> assignWorkOrder(@PathVariable Long workOrderId,
                                                          @Valid @RequestBody AssignWorkOrderRequest request) {
        return ApiResponse.success(workOrderService.assignWorkOrder(workOrderId, request));
    }

    @PutMapping("/{workOrderId}/status")
    public ApiResponse<WorkOrderResponse> updateStatus(@PathVariable Long workOrderId,
                                                       @Valid @RequestBody UpdateWorkOrderStatusRequest request) {
        return ApiResponse.success(workOrderService.updateStatus(workOrderId, request));
    }

    @PutMapping("/{workOrderId}/cancel")
    public ApiResponse<WorkOrderResponse> cancelWorkOrder(@PathVariable Long workOrderId,
                                                          @Valid @RequestBody CancelWorkOrderRequest request) {
        return ApiResponse.success(workOrderService.cancelWorkOrder(workOrderId, request));
    }
}
