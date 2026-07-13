import http from "./index"
import type { ApiResponse, DashboardStatsResponse, TrendStatsItem, CrackTypeStatItem, SeverityStatItem, DeptWorkloadItem } from "@/types"

export const statisticsApi = {
  getDashboard() {
    return http.get<ApiResponse<DashboardStatsResponse>>("/statistics/dashboard")
  },
  getTrend(days?: number) {
    return http.get<ApiResponse<TrendStatsItem[]>>("/statistics/trend", { params: { days: days || 30 } })
  },
  getCrackType() {
    return http.get<ApiResponse<CrackTypeStatItem[]>>("/statistics/crack-type")
  },
  getSeverity() {
    return http.get<ApiResponse<SeverityStatItem[]>>("/statistics/severity")
  },
  getDepartmentWorkload() {
    return http.get<ApiResponse<DeptWorkloadItem[]>>("/statistics/department-workload")
  },
}
