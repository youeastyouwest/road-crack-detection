import { defineStore } from "pinia"
import { ref, computed } from "vue"
import { authApi, userApi } from "@/api"
import type { LoginRequest } from "@/types"
import { RoleCode, MAINTENANCE_ROLES, DEPT_ADMIN_ROLES } from "@/types"
import router from "@/router"

export const useAuthStore = defineStore("auth", () => {
  const token = ref(localStorage.getItem("accessToken") || "")
  const refreshToken = ref(localStorage.getItem("refreshToken") || "")
  const userId = ref(Number(localStorage.getItem("userId")) || 0)
  const username = ref(localStorage.getItem("username") || "")
  const realName = ref(localStorage.getItem("realName") || "")
  const roles = ref<string[]>(JSON.parse(localStorage.getItem("roles") || "[]"))
  const userDetail = ref<any>(null)

  const isLoggedIn = computed(() => !!token.value)
  const primaryRole = computed(() => roles.value[0] || "")

  // ─── Role check helpers ───
  const isAdmin = computed(() => primaryRole.value === RoleCode.ADMIN)
  const isRoadAdmin = computed(() => primaryRole.value === RoleCode.ROAD_ADMIN)
  const isSanitAdmin = computed(() => primaryRole.value === RoleCode.SANIT_ADMIN)
  const isTrafficAdmin = computed(() => primaryRole.value === RoleCode.TRAFFIC_ADMIN)
  const isMaintainer = computed(() => primaryRole.value === RoleCode.MAINTAINER)
  const isCrowdsource = computed(() => primaryRole.value === RoleCode.CROWDSOURCE)
  const isViewer = computed(() => primaryRole.value === RoleCode.VIEWER)

  // ─── Group helpers ───
  const isDeptAdmin = computed(() => DEPT_ADMIN_ROLES.includes(primaryRole.value))
  const isMaintenance = computed(() => MAINTENANCE_ROLES.includes(primaryRole.value))

  const deptCode = computed(() => {
    const map: Record<string, string> = {
      ROLE_ROAD_ADMIN: "ROAD_ADMIN",
      ROLE_SANIT_ADMIN: "SANITATION",
      ROLE_TRAFFIC_ADMIN: "TRAFFIC_POLICE",
    }
    return map[primaryRole.value] || ""
  })

  const roleLabel = computed(() => {
    const map: Record<string, string> = {
      ROLE_ADMIN: "超级管理员",
      ROLE_ROAD_ADMIN: "道路管理员",
      ROLE_SANIT_ADMIN: "环卫管理员",
      ROLE_TRAFFIC_ADMIN: "交管管理员",
      ROLE_MAINTAINER: "维修工",
      ROLE_CROWDSOURCE: "众包人员",
      ROLE_VIEWER: "查看员",
    }
    return map[primaryRole.value] || primaryRole.value
  })

  const roleColor = computed(() => {
    const map: Record<string, string> = {
      ROLE_ADMIN: "#4361ee",
      ROLE_ROAD_ADMIN: "#059669",
      ROLE_SANIT_ADMIN: "#d97706",
      ROLE_TRAFFIC_ADMIN: "#dc2626",
      ROLE_MAINTAINER: "#2563eb",
      ROLE_CROWDSOURCE: "#8b5cf6",
      ROLE_VIEWER: "#7c3aed",
    }
    return map[primaryRole.value] || "#6366f1"
  })

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    // 校验业务返回码
    if (res.data.code !== 200 || !res.data.data) {
      throw new Error(res.data.message || "登录失败")
    }
    const d = res.data.data
    token.value = d.accessToken
    refreshToken.value = d.refreshToken
    userId.value = d.userId
    username.value = d.username
    realName.value = d.realName
    roles.value = d.roles
    localStorage.setItem("accessToken", d.accessToken)
    localStorage.setItem("refreshToken", d.refreshToken)
    localStorage.setItem("userId", String(d.userId))
    localStorage.setItem("username", d.username)
    localStorage.setItem("realName", d.realName)
    localStorage.setItem("roles", JSON.stringify(d.roles))
    await fetchUserDetail()
    if (isAdmin.value) router.push("/dashboard")
    else if (isDeptAdmin.value) router.push("/dept-orders")
    else if (isMaintainer.value) router.push("/my-work-orders")
    else if (isCrowdsource.value) router.push("/crowd-report")
    else router.push("/dashboard")
  }

  async function fetchUserDetail() {
    try {
      const res = await userApi.current()
      userDetail.value = res.data.data
    } catch { /* ignore */ }
  }

  function logout() {
    token.value = ""
    refreshToken.value = ""
    userId.value = 0
    username.value = ""
    realName.value = ""
    roles.value = []
    userDetail.value = null
    localStorage.clear()
    router.push("/login")
  }

  return {
    token, userId, username, realName, roles, userDetail,
    isLoggedIn,
    isAdmin, isRoadAdmin, isSanitAdmin, isTrafficAdmin,
    isMaintainer, isCrowdsource, isViewer,
    isDeptAdmin, isMaintenance, primaryRole, deptCode, roleLabel, roleColor,
    login, logout, fetchUserDetail,
  }
})