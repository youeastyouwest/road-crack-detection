import http from "./index"
import type { ApiResponse, PageResponse, AuditLogResponse } from "@/types"

export const auditLogApi = {
  page(params: { page?: number; size?: number; module?: string; status?: string }) {
    return http.get<ApiResponse<PageResponse<AuditLogResponse>>>("/audit-log/page", { params })
  },
}
