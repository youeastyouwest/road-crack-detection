import http from "./index"
import type { ApiResponse, RoleEntity } from "@/types"

export const roleApi = {
  list() {
    return http.get<ApiResponse<RoleEntity[]>>("/role/list")
  },
  detail(id: number) {
    return http.get<ApiResponse<RoleEntity>>(`/role/${id}`)
  },
  create(data: RoleEntity) {
    return http.post<ApiResponse<null>>("/role", data)
  },
  update(id: number, data: RoleEntity) {
    return http.put<ApiResponse<null>>(`/role/${id}`, data)
  },
  remove(id: number) {
    return http.delete<ApiResponse<null>>(`/role/${id}`)
  },
}
