<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="sidebar-head">
        <div class="sidebar-logo">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          <span v-if="!appStore.sidebarCollapsed">{{ titleText }}</span>
        </div>
      </div>
      <nav class="sidebar-nav">
        <template v-for="group in navGroups" :key="group.label">
          <div v-if="!appStore.sidebarCollapsed" class="nav-section-label">{{ group.label }}</div>
          <router-link v-for="item in group.items" :key="item.path" :to="item.path" class="nav-item" :class="{ active: isActive(item.path) }">
            <el-icon><component :is="item.icon" /></el-icon>
            <span v-if="!appStore.sidebarCollapsed">{{ item.title }}</span>
          </router-link>
        </template>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <el-avatar :size="32" :src="''">{{ authStore.realName?.[0] || authStore.username?.[0] || 'U' }}</el-avatar>
          <div v-if="!appStore.sidebarCollapsed" class="user-text">
            <span class="user-name">{{ authStore.realName || authStore.username }}</span>
            <span class="role-badge" :style="{ background: authStore.roleColor }">{{ authStore.roleLabel }}</span>
          </div>
        </div>
        <button v-if="!appStore.sidebarCollapsed" class="logout-btn" @click="handleLogout">{{ t('auth.logout') }}</button>
        <button v-else class="logout-btn compact" @click="handleLogout" :title="t('auth.logout')">
          <el-icon><SwitchButton /></el-icon>
        </button>
      </div>
    </aside>
    <button class="collapse-toggle" @click="appStore.toggleSidebar()">
      <el-icon><component :is="appStore.sidebarCollapsed ? 'ArrowRight' : 'ArrowLeft'" /></el-icon>
    </button>
    <main class="content-area">
      <div class="content-inner"><router-view /></div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { useAppStore } from "@/stores/app"
import { t } from "@/i18n"
import { ElMessageBox } from "element-plus"

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

interface NavItem { path: string; title: string; icon: string }
interface NavGroup { label: string; items: NavItem[] }

const coreGroup = computed<NavGroup>(() => ({
  label: t("nav.coreWork"),
  items: [
    { path: "/upload-data", title: t("nav.uploadDetection"), icon: "Upload" },
    { path: "/detection-results", title: t("nav.detectionResults"), icon: "List" },
    { path: "/statistics", title: t("nav.statistics"), icon: "Histogram" },
    { path: "/ai-agent", title: t("nav.aiAssistant"), icon: "ChatDotSquare" },
  ],
}))

const adminNav = computed<NavGroup[]>(() => [
  coreGroup.value,
  { label: t("nav.overview"), items: [
    { path: "/dashboard", title: t("nav.realtimeDashboard"), icon: "Monitor" },
    { path: "/data-screen", title: t("nav.dataScreen"), icon: "DataAnalysis" },
  ]},
  { label: t("nav.workOrdersRoads"), items: [
    { path: "/work-orders", title: t("nav.workOrderManagement"), icon: "List" },
    { path: "/road-management", title: t("nav.roadManagement"), icon: "MapLocation" },
    { path: "/road-health", title: t("nav.healthArchive"), icon: "Document" },
    { path: "/road-maintenance", title: t("nav.roadMaintenance"), icon: "Tools" },
    { path: "/reports", title: t("nav.reportReview"), icon: "EditPen" },
  ]},
  { label: t("nav.analysisIntelligence"), items: [
    { path: "/analysis-report", title: t("nav.analysisReport"), icon: "DataLine" },
    { path: "/alerts", title: t("nav.alertManagement"), icon: "WarningFilled" },
  ]},
  { label: t("nav.systemManagement"), items: [
    { path: "/user-management", title: t("nav.userManagement"), icon: "UserFilled" },
    { path: "/department-management", title: t("nav.departmentManagement"), icon: "OfficeBuilding" },
    { path: "/role-management", title: t("nav.rolePermission"), icon: "Setting" },
    { path: "/audit-log", title: t("nav.operationLog"), icon: "Clock" },
    { path: "/system-config", title: t("nav.systemConfig"), icon: "Tools" },
    { path: "/user-profile", title: t("nav.userProfile"), icon: "User" },
  ]},
])

const deptAdminNav = computed<NavGroup[]>(() => [
  coreGroup.value,
  { label: t("nav.deptWorkOrders"), items: [
    { path: "/dept-orders", title: t("nav.deptWorkOrderManagement"), icon: "List" },
    { path: "/dept-report-review", title: t("nav.deptReportReview"), icon: "EditPen" },
  ]},
  { label: t("nav.userProfile"), items: [
    { path: "/user-profile", title: t("nav.userProfile"), icon: "User" },
  ]},
])

const maintainerNav = computed<NavGroup[]>(() => [
  coreGroup.value,
  { label: t("nav.workOrderProcessing"), items: [
    { path: "/my-work-orders", title: t("nav.myWorkOrders"), icon: "List" },
    { path: "/submit-report", title: t("nav.submitReport"), icon: "EditPen" },
    { path: "/my-reports", title: t("nav.completedReports"), icon: "Document" },
  ]},
  { label: t("nav.userProfile"), items: [
    { path: "/user-profile", title: t("nav.userProfile"), icon: "User" },
  ]},
])

const crowdsourceNav = computed<NavGroup[]>(() => [
  coreGroup.value,
  { label: t("nav.myReports"), items: [
    { path: "/crowd-report", title: t("nav.reportProblem"), icon: "Upload" },
    { path: "/crowd-records", title: t("nav.reportRecords"), icon: "List" },
  ]},
  { label: t("nav.userProfile"), items: [
    { path: "/user-profile", title: t("nav.userProfile"), icon: "User" },
  ]},
])

const viewerNav = computed<NavGroup[]>(() => [
  coreGroup.value,
  { label: t("nav.overview"), items: [
    { path: "/dashboard", title: t("nav.realtimeDashboard"), icon: "Monitor" },
    { path: "/data-screen", title: t("nav.dataScreen"), icon: "DataAnalysis" },
  ]},
  { label: t("nav.userProfile"), items: [
    { path: "/user-profile", title: t("nav.userProfile"), icon: "User" },
  ]},
])

const titleText = computed(() => {
  if (authStore.isAdmin) return appStore.siteName || t("site.adminPanel")
  if (authStore.isDeptAdmin) return t("site.deptPanel")
  if (authStore.isMaintainer) return t("site.maintainerPanel")
  if (authStore.isCrowdsource) return t("site.crowdPanel")
  return appStore.siteName || t("site.viewerPanel")
})

const navGroups = computed<NavGroup[]>(() => {
  if (authStore.isAdmin) return adminNav.value
  if (authStore.isDeptAdmin) return deptAdminNav.value
  if (authStore.isMaintainer) return maintainerNav.value
  if (authStore.isCrowdsource) return crowdsourceNav.value
  if (authStore.isViewer) return viewerNav.value
  return []
})

function isActive(path: string) { return route.path === path }

function handleLogout() {
  ElMessageBox.confirm(t("auth.logoutConfirm"), t("common.tip"), { confirmButtonText: t("auth.confirm"), cancelButtonText: t("auth.cancel"), type: "warning" })
    .then(() => authStore.logout())
    .catch(() => {})
}
</script>

<style scoped>
.admin-layout { display:flex; height:100vh; overflow:hidden; background:#fff; font-family:"Inter",-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; }
.sidebar { width:230px; height:100vh; background:#fff; border-right:1px solid #eef0f4; display:flex; flex-direction:column; flex-shrink:0; transition:width .2s; }
.sidebar.collapsed { width:60px; }
.sidebar-head { padding:20px 18px; border-bottom:1px solid #eef0f4; }
.sidebar-logo { display:flex; align-items:center; gap:10px; font-size:15px; font-weight:700; color:#1a202c; }
.sidebar-logo svg { color:#4361ee; flex-shrink:0; }
.sidebar-nav { flex:1; overflow-y:auto; padding:10px; }
.nav-section-label { padding:14px 12px 6px; font-size:10px; font-weight:700; color:#94a3b8; text-transform:uppercase; letter-spacing:.08em; }
.nav-item { display:flex; align-items:center; gap:10px; padding:9px 12px; font-size:13px; font-weight:500; color:#4a5568; text-decoration:none; border-radius:8px; transition:all .15s; }
.nav-item:hover { background:#f1f5f9; color:#1a202c; }
.nav-item.active { background:#eef2ff; color:#4361ee; font-weight:600; }
.nav-item svg { flex-shrink:0; }
.sidebar-footer { padding:12px 16px; border-top:1px solid #eef0f4; display:flex; flex-direction:column; gap:6px; }
.user-info { display:flex; align-items:center; gap:6px; }
.user-text { display:flex; align-items:center; gap:6px; overflow:hidden; }
.user-name { font-size:13px; font-weight:600; color:#1a202c; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.role-badge { display:inline-block; font-size:10px; font-weight:700; padding:1px 8px; border-radius:6px; color:#fff; white-space:nowrap; }
.logout-btn { padding:4px 12px; border:1px solid #e2e8f0; background:#fff; border-radius:6px; font-size:12px; color:#94a3b8; cursor:pointer; transition:all .2s; font-family:inherit; }
.logout-btn:hover { color:#dc2626; border-color:#dc2626; }
.logout-btn.compact { padding:4px 8px; display:flex; align-items:center; justify-content:center; }
.collapse-toggle { position:fixed; left:230px; top:50%; transform:translateY(-50%); z-index:100; width:20px; height:36px; display:flex; align-items:center; justify-content:center; border:1px solid #eef0f4; border-left:none; background:#fff; border-radius:0 6px 6px 0; cursor:pointer; color:#94a3b8; transition:left .2s,color .2s; padding:0; }
.sidebar.collapsed ~ .collapse-toggle { left:60px; }
.collapse-toggle:hover { color:#4361ee; }
.content-area { flex:1; height:100vh; overflow-y:auto; background:#f8f9fc; }
.content-inner { max-width:1440px; margin:0 auto; padding:24px; }
</style>
