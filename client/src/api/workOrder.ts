import http from "./index"
import type {
  ApiResponse,
  PageResponse,
  WorkOrderResponse,
  CreateWorkOrderRequest,
  AssignWorkOrderRequest,
  UpdateWorkOrderStatusRequest,
  CancelWorkOrderRequest,
  WorkOrderStatus,
  DepartmentCode,
  SeverityLevel,
} from "@/types"

export const workOrderApi = {
  create(data: CreateWorkOrderRequest) {
    return http.post<ApiResponse<WorkOrderResponse>>("/work-orders", data)
  },
  list(params: {
    page?: number
    size?: number
    status?: WorkOrderStatus
    departmentCode?: DepartmentCode
    severityLevel?: SeverityLevel
    assignee?: string
    keyword?: string
  }) {
    return http.get<ApiResponse<PageResponse<WorkOrderResponse>>>("/work-orders", { params })
  },
  get(workOrderId: number) {
    return http.get<ApiResponse<WorkOrderResponse>>(`/work-orders/${workOrderId}`)
  },
  assign(workOrderId: number, data: AssignWorkOrderRequest) {
    return http.put<ApiResponse<WorkOrderResponse>>(`/work-orders/${workOrderId}/assign`, data)
  },
  assignWorker(workOrderId: number, assignee: string) {
    return http.put<ApiResponse<WorkOrderResponse>>(`/work-orders/${workOrderId}/assign-worker`, null, { params: { assignee } })
  },
  updateStatus(workOrderId: number, data: UpdateWorkOrderStatusRequest) {
    return http.put<ApiResponse<WorkOrderResponse>>(`/work-orders/${workOrderId}/status`, data)
  },
  cancel(workOrderId: number, data: CancelWorkOrderRequest) {
    return http.put<ApiResponse<WorkOrderResponse>>(`/work-orders/${workOrderId}/cancel`, data)
  },
}
