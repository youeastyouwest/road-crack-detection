<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted, watch } from "vue"
import { useAppStore } from "@/stores/app"
import { configApi } from "@/api/config"
import { setLocale } from "@/i18n"

const appStore = useAppStore()

async function loadConfig() {
  try {
    const res = await configApi.get()
    if (res.data && res.data.code === 200 && res.data.data) {
      appStore.setSystemConfig(res.data.data)
      setLocale((res.data.data.language || "zh-CN") as "zh-CN" | "en")
    }
  } catch (err) {
    console.warn("[App] load system config failed", err)
  }
}

function updateTitle() {
  const name = appStore.siteName
  document.title = name || "途安智巡道路裂缝检测系统"
}

watch(() => appStore.siteName, updateTitle, { immediate: true })

onMounted(() => {
  const dark = localStorage.getItem("road_crack_dark_mode") === "1"
  document.documentElement.classList.toggle("dark", dark)
  loadConfig()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  font-family: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-thumb {
  background: #c1c7cd;
  border-radius: 3px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

/* Dark mode global overrides */
html.dark, html.dark body, html.dark #app {
  background: #0f172a;
  color: #e2e8f0;
}

html.dark .admin-layout { background: #0f172a !important; }
html.dark .sidebar { background: #1e293b !important; border-right-color: #334155 !important; }
html.dark .sidebar-head { border-bottom-color: #334155 !important; }
html.dark .sidebar-logo { color: #f8fafc !important; }
html.dark .sidebar-logo svg { color: #60a5fa !important; }
html.dark .nav-section-label { color: #94a3b8 !important; }
html.dark .nav-item { color: #cbd5e1 !important; }
html.dark .nav-item:hover { background: #334155 !important; color: #f8fafc !important; }
html.dark .nav-item.active { background: #2563eb33 !important; color: #60a5fa !important; }
html.dark .sidebar-footer { border-top-color: #334155 !important; }
html.dark .user-name { color: #f8fafc !important; }
html.dark .logout-btn { background: #1e293b !important; border-color: #334155 !important; color: #94a3b8 !important; }
html.dark .logout-btn:hover { color: #f87171 !important; border-color: #f87171 !important; }
html.dark .collapse-toggle { background: #1e293b !important; border-color: #334155 !important; color: #94a3b8 !important; }
html.dark .collapse-toggle:hover { color: #60a5fa !important; }
html.dark .content-area { background: #0f172a !important; }
html.dark ::-webkit-scrollbar-thumb { background: #475569; }
</style>
