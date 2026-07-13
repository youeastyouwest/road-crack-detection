import { defineStore } from "pinia"
import { ref, computed } from "vue"
import type { SystemConfigResponse } from "@/types"

const defaultSiteName = "途安智巡道路裂缝检测系统"

export const useAppStore = defineStore("app", () => {
  const sidebarCollapsed = ref(false)
  const currentRole = ref(localStorage.getItem("currentRole") || "")

  // 系统配置：全局共享，供布局、标题、语言等使用
  const systemConfig = ref<SystemConfigResponse>({
    siteName: defaultSiteName,
    language: "zh-CN",
    allowRegister: false,
    detectionInterval: 30,
    alertThreshold: 3,
    dataRetentionDays: 180,
    maxUploadSize: 50,
    emailNotification: true,
    smsNotification: false,
    maintenanceAlert: true,
    minPasswordLength: 6,
    maxLoginAttempts: 5,
    sessionTimeout: 30,
    captchaEnabled: false,
    darkMode: false,
  })

  const siteName = computed(() => systemConfig.value.siteName || defaultSiteName)
  const language = computed(() => systemConfig.value.language || "zh-CN")

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setRole(role: string) {
    currentRole.value = role
    localStorage.setItem("currentRole", role)
  }

  function setSystemConfig(config: SystemConfigResponse) {
    systemConfig.value = { ...systemConfig.value, ...config }
  }

  return {
    sidebarCollapsed,
    currentRole,
    toggleSidebar,
    setRole,
    systemConfig,
    siteName,
    language,
    setSystemConfig,
  }
})
