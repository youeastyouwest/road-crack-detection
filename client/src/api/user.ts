import http from "./index"
import type {
  ApiResponse,
  PageResponse,
  UserEntity,
  UserDetailResponse,
  UserPageQuery,
} from "@/types"

export const userApi = {
  page(query: UserPageQuery) {
    return http.get<ApiResponse<PageResponse<UserEntity>>>("/user/page", { params: query })
  },
  detail(id: number) {
    return http.get<ApiResponse<UserDetailResponse>>(`/user/${id}`)
  },
  create(data: UserEntity, roleIds?: number[]) {
    return http.post<ApiResponse<null>>("/user", data, { params: { roleIds: roleIds?.join(",") } })
  },
  update(id: number, data: UserEntity, roleIds?: number[]) {
    return http.put<ApiResponse<null>>(`/user/${id}`, data, { params: { roleIds: roleIds?.join(",") } })
  },
  remove(id: number) {
    return http.delete<ApiResponse<null>>(`/user/${id}`)
  },
  toggleStatus(id: number, status: number) {
    return http.put<ApiResponse<null>>(`/user/${id}/status`, null, { params: { status } })
  },
  resetPassword(id: number) {
    return http.put<ApiResponse<null>>(`/user/${id}/reset-password`)
  },
  current() {
    return http.get<ApiResponse<UserDetailResponse>>("/user/current")
  },
}
