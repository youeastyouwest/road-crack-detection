import http from "./index"
import type {
  LoginRequest,
  LoginResponse,
  ChangePasswordRequest,
  ResetPasswordRequest,
  RegisterRequest,
  ApiResponse,
} from "@/types"

export const authApi = {
  login(data: LoginRequest) {
    return http.post<ApiResponse<LoginResponse>>("/auth/login", data)
  },
  refresh(refreshToken: string) {
    return http.post<ApiResponse<LoginResponse>>("/auth/refresh", null, { params: { refreshToken } })
  },
  register(data: RegisterRequest) {
    return http.post<ApiResponse<null>>("/auth/register", data)
  },
  sendCode(email: string, type = 1) {
    return http.post<ApiResponse<string>>("/auth/send-code", null, { params: { email, type } })
  },
  changePassword(data: ChangePasswordRequest) {
    return http.put<ApiResponse<null>>("/auth/change-password", data)
  },
  resetPassword(data: ResetPasswordRequest) {
    return http.post<ApiResponse<null>>("/auth/reset-password", data)
  },
}
