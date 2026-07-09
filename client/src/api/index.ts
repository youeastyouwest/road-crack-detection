import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from "axios"
import { ElMessage } from "element-plus"
import router from "@/router"

const http: AxiosInstance = axios.create({
  baseURL: "/api",
  timeout: 30000,
  headers: { "Content-Type": "application/json" },
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem("accessToken")
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const userId = localStorage.getItem("userId")
  if (userId && config.headers) {
    config.headers["X-User-Id"] = userId
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.clear()
      router.push("/login")
      ElMessage.error("登录已过期，请重新登录")
    } else if (error.response?.status === 403) {
      ElMessage.error("无权限访问")
    } else if (error.response?.status === 500) {
      ElMessage.error("服务器内部错误")
    }
    return Promise.reject(error)
  }
)

export default http
export * from "./auth"
export * from "./user"
export * from "./detection"
export * from "./workOrder"
export * from "./department"
export * from "./role"
export * from "./report"
export * from "./file"
export * from "./statistics"