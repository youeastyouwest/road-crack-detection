import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from "axios"
import { ElMessage } from "element-plus"
import router from "@/router"
import { clearAuthStorage, getAuthItem } from "@/utils/authStorage"

const http: AxiosInstance = axios.create({
  baseURL: "/api",
  timeout: 30000,
  headers: { "Content-Type": "application/json" },
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAuthItem("accessToken")
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const userId = getAuthItem("userId")
  if (userId && config.headers) {
    config.headers["X-User-Id"] = userId
  }
  const userName = getAuthItem("realName") || getAuthItem("username")
  if (userName && config.headers) {
    config.headers["X-User-Name"] = userName
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || ""
    const isAuthEndpoint = url.includes("/auth/login") || url.includes("/auth/register") || url.includes("/auth/send-code") || url.includes("/auth/reset-password")
    if (error.response?.status === 401) {
      // 登录/注册接口的 401 由业务代码自行处理，不做全局拦截
      if (!isAuthEndpoint) {
        clearAuthStorage()
        router.push("/login")
        ElMessage.error("登录已过期，请重新登录")
      }
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
export * from "./agent"
export * from "./alert"
export * from "./auditLog"
export * from "./crowd"
export * from "./map"
export * from "./road"
