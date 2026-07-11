import http from "./index"
import type { ApiResponse, PageResponse, AlertResponse } from "@/types"

export const alertApi = {
  list(params: { page?: number; size?: number; alertLevel?: string; alertType?: string; status?: string }) {
    return http.get<ApiResponse<PageResponse<AlertResponse>>>("/alerts", { params })
  },
  recent(count?: number) {
    return http.get<ApiResponse<AlertResponse[]>>("/alerts/recent", { params: { count: count || 5 } })
  },
}
