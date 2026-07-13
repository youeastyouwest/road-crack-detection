import http from "./index"
import type { ApiResponse, DepartmentEntity } from "@/types"

export const departmentApi = {
  tree() {
    return http.get<ApiResponse<DepartmentEntity[]>>("/department/tree")
  },
  list() {
    return http.get<ApiResponse<DepartmentEntity[]>>("/department/list")
  },
  detail(id: number) {
    return http.get<ApiResponse<DepartmentEntity>>(`/department/${id}`)
  },
  create(data: DepartmentEntity) {
    return http.post<ApiResponse<null>>("/department", data)
  },
  update(id: number, data: DepartmentEntity) {
    return http.put<ApiResponse<null>>(`/department/${id}`, data)
  },
  remove(id: number) {
    return http.delete<ApiResponse<null>>(`/department/${id}`)
  },
}
