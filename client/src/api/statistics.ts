import http from "./index"
import type { ApiResponse, StatisticsSummary, StatisticsTrend, CrackTypeDistribution } from "@/types"

export const statisticsApi = {
  getDashboard() {
    return http.get<ApiResponse<StatisticsSummary>>("/statistics/dashboard")
  },
  getTrend() {
    return http.get<ApiResponse<StatisticsTrend[]>>("/statistics/trend")
  },
  getCrackType() {
    return http.get<ApiResponse<CrackTypeDistribution[]>>("/statistics/crack-type")
  },
  getSeverity() {
    return http.get<ApiResponse<{ level: string; count: number; percentage: number }[]>>("/statistics/severity")
  },
  getDepartmentWorkload() {
    return http.get<ApiResponse<{ deptName: string; total: number; completed: number; pending: number }[]>>("/statistics/department-workload")
  },
}