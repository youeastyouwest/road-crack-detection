import http from "./index"
import type { ApiResponse, AgentChatResponse, AgentDetectImageResponse, AgentReportResponse, GenerateReportRequest } from "@/types"

export const agentApi = {
  chat(data: { sessionId?: string; message: string; includeContext?: boolean }) {
    return http.post<ApiResponse<AgentChatResponse>>("/agent/chat", data, {
      timeout: 90000,
    })
  },
  detectImage(file: File, options?: { question?: string; generateAdvice?: boolean; autoGenerateWorkOrder?: boolean; autoDispatch?: boolean; location?: string; confidenceThreshold?: number }) {
    const form = new FormData()
    form.append("image", file)
    if (options?.question) form.append("question", options.question)
    if (options?.generateAdvice !== undefined) form.append("generateAdvice", String(options.generateAdvice))
    if (options?.autoGenerateWorkOrder !== undefined) form.append("autoGenerateWorkOrder", String(options.autoGenerateWorkOrder))
    if (options?.autoDispatch !== undefined) form.append("autoDispatch", String(options.autoDispatch))
    if (options?.location) form.append("location", options.location)
    if (options?.confidenceThreshold !== undefined) form.append("confidenceThreshold", String(options.confidenceThreshold))
    return http.post<ApiResponse<AgentDetectImageResponse>>("/agent/detect-image", form, {
      headers: { "Content-Type": "multipart/form-data" },
    })
  },
  generateReport(data: GenerateReportRequest) {
    return http.post<ApiResponse<AgentReportResponse>>("/agent/report", data, {
      timeout: 90000,
    })
  },
}
