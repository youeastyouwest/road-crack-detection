import http from "./index"
import type { ApiResponse, SystemConfigRequest, SystemConfigResponse } from "@/types"

export const configApi = {
  get() {
    return http.get<ApiResponse<SystemConfigResponse>>("/config")
  },
  update(data: SystemConfigRequest) {
    return http.put<ApiResponse<SystemConfigResponse>>("/config", data)
  },
}
