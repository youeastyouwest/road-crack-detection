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
  createWithFile(fd: FormData) {
    return http.post<ApiResponse<DetectionTaskResponse>>("/detection-tasks/upload", fd, {
      headers: { "Content-Type": "multipart/form-data" },
      timeout: 300000, // 视频上传最多5分钟
    })
  },
}
