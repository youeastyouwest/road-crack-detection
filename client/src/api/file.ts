import http from "./index"
import type { ApiResponse, FileUploadResponse } from "@/types"

export const fileApi = {
  upload(file: File) {
    const form = new FormData()
    form.append("file", file)
    return http.post<ApiResponse<FileUploadResponse>>("/file/upload", form, {
      headers: { "Content-Type": "multipart/form-data" },
    })
  },
}
