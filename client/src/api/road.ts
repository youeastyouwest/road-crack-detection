import http from "./index"
import type { ApiResponse, PageResponse, RoadResponse, RoadHealthArchiveResponse, RoadHealthArchiveDashboardResponse, GenerateArchiveRequest } from "@/types"

export const roadApi = {
  list(params: { page?: number; size?: number; roadName?: string; district?: string; roadGrade?: string; status?: string }) {
    return http.get<ApiResponse<PageResponse<RoadResponse>>>("/roads", { params })
  },
  listWithDetections() {
    return http.get<ApiResponse<RoadResponse[]>>("/roads/with-detections")
  },
  detail(id: number) {
    return http.get<ApiResponse<RoadResponse>>("/roads/" + id)
  },
  create(data: Partial<RoadResponse>) {
    return http.post<ApiResponse<null>>("/roads", data)
  },
  update(id: number, data: Partial<RoadResponse>) {
    return http.put<ApiResponse<null>>("/roads/" + id, data)
  },
  remove(id: number) {
    return http.delete<ApiResponse<null>>("/roads/" + id)
  },
}

export const roadHealthArchiveApi = {
  generate(data: GenerateArchiveRequest) {
    return http.post<ApiResponse<RoadHealthArchiveResponse>>("/road-health-archives/generate", data)
  },
  list(params: { page?: number; size?: number; roadId?: number; damageLevel?: string; startDate?: string; endDate?: string }) {
    return http.get<ApiResponse<PageResponse<RoadHealthArchiveResponse>>>("/road-health-archives", { params })
  },
  get(archiveId: number) {
    return http.get<ApiResponse<RoadHealthArchiveResponse>>("/road-health-archives/" + archiveId)
  },
  dashboard() {
    return http.get<ApiResponse<RoadHealthArchiveDashboardResponse>>("/road-health-archives/dashboard")
  },
}
