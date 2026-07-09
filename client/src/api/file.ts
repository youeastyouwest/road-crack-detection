import http from "./index"
import type { ApiResponse } from "@/types"

export const fileApi = {
  upload(file: File, type?: string) {
    const form = new FormData()
    form.append("file", file)
    if (type) form.append("type", type)
    return http.post<ApiResponse<{ fileId: number; fileUrl: string; fileName: string; fileSize: number; uploadedAt: string }>>("/file/upload", form, {
      headers: { "Content-Type": "multipart/form-data" },
    })
  },
}