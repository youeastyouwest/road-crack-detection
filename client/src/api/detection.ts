import http from "./index"
import type {
  ApiResponse,
  PageResponse,
  DetectionTaskResponse,
  DetectionResultResponse,
  DetectionTaskStatus,
  DataSourceType,
  CreateDetectionTaskSubmitRequest,
} from "@/types"

export const detectionApi = {
  create(data: CreateDetectionTaskSubmitRequest) {
    return http.post<ApiResponse<DetectionTaskResponse>>("/detection-tasks", data)
  },
  execute(taskId: number) {
    return http.post<ApiResponse<null>>(`/detection-tasks/${taskId}/execute`)
  },
  list(params: {
    page?: number
    size?: number
    status?: DetectionTaskStatus
    dataSourceType?: DataSourceType
    location?: string
    submittedBy?: string
  }) {
    return http.get<ApiResponse<PageResponse<DetectionTaskResponse>>>("/detection-tasks", { params })
  },
  get(taskId: number) {
    return http.get<ApiResponse<DetectionTaskResponse>>(`/detection-tasks/${taskId}`)
  },
  getResult(taskId: number) {
    return http.get<ApiResponse<DetectionResultResponse>>(`/detection-tasks/${taskId}/result`)
  },
  remove(taskId: number) {
    return http.delete<ApiResponse<null>>(`/detection-tasks/${taskId}`)
  },
  /** 批量更新严重等级（仅超级管理员） */
  batchUpdateSeverity(data: { taskIds: number[]; newSeverity: string }) {
    return http.request<ApiResponse<null>>({ method: "PUT", url: "/detection-tasks/batch-severity", data })
  },
  /** 批量删除检测结果（仅超级管理员） */
  batchRemove(data: { taskIds: number[] }) {
    return http.request<ApiResponse<null>>({ method: "DELETE", url: "/detection-tasks/batch", data })
  },
  createWithFile(fd: FormData) {
    return http.post<ApiResponse<DetectionTaskResponse>>("/detection-tasks/upload", fd, {
      headers: { "Content-Type": "multipart/form-data" },
    })
  },
}
