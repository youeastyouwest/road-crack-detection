import http from "./index"
import type { ApiResponse, PageResponse, CrowdReportResponse, CreateCrowdReportRequest, ReviewCrowdReportRequest } from "@/types"

export const crowdReportApi = {
  submit(data: CreateCrowdReportRequest) {
    return http.post<ApiResponse<CrowdReportResponse>>("/crowd-reports", data)
  },
  list(params: { page?: number; size?: number; status?: string; reporterPhone?: string; location?: string }) {
    return http.get<ApiResponse<PageResponse<CrowdReportResponse>>>("/crowd-reports", { params })
  },
  get(reportId: number) {
    return http.get<ApiResponse<CrowdReportResponse>>("/crowd-reports/" + reportId)
  },
  review(reportId: number, data: ReviewCrowdReportRequest, reviewer?: string) {
    return http.put<ApiResponse<CrowdReportResponse>>("/crowd-reports/" + reportId + "/review", data, { params: { reviewer: reviewer || "admin" } })
  },
}
