import { defineStore } from "pinia"
import { ref } from "vue"

export const useAppStore = defineStore("app", () => {
  const sidebarCollapsed = ref(false)
  const currentRole = ref(localStorage.getItem("currentRole") || "")

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setRole(role: string) {
    currentRole.value = role
    localStorage.setItem("currentRole", role)
  }

  return { sidebarCollapsed, currentRole, toggleSidebar, setRole }
})
