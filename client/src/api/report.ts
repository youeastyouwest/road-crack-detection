import http from "./index"
import type { ApiResponse, PageResponse, MaintenanceReportResponse, CreateMaintenanceReportRequest } from "@/types"

export const reportApi = {
  create(data: CreateMaintenanceReportRequest) {
    return http.post<ApiResponse<MaintenanceReportResponse>>("/maintenance-reports", data)
  },
  list(params: { page?: number; size?: number; workOrderId?: number; executor?: string }) {
    return http.get<ApiResponse<PageResponse<MaintenanceReportResponse>>>("/maintenance-reports", { params })
  },
  get(reportId: number) {
    return http.get<ApiResponse<MaintenanceReportResponse>>("/maintenance-reports/" + reportId)
  },
}
