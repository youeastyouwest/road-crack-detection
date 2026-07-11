import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { RoleCode, MAINTENANCE_ROLES, DEPT_ADMIN_ROLES } from "@/types"

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/LoginView.vue"),
    meta: { title: "登录" },
  },
  {
    path: "/",
    component: () => import("@/layout/AdminLayout.vue"),
    redirect: "/dashboard",
    meta: { requiresAuth: true },
    children: [
      // ===== CORE USE CASES - shared by ALL roles =====
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/views/dashboard/RealTimeDashboard.vue"),
        meta: { title: "实时仪表盘", icon: "Monitor" },
      },
      {
        path: "upload-data",
        name: "UploadData",
        component: () => import("@/views/detection/DataCollection.vue"),
        meta: { title: "上传检测", icon: "Upload" },
      },
      {
        path: "detection-results",
        name: "DetectionResults",
        component: () => import("@/views/detection/AnomalyDetectionImage.vue"),
        meta: { title: "检测结果", icon: "List" },
      },
      {
        path: "statistics",
        name: "Statistics",
        component: () => import("@/views/analysis/DataStatistics.vue"),
        meta: { title: "数据统计", icon: "Histogram" },
      },
      {
        path: "ai-agent",
        name: "AgentChat",
        component: () => import("@/views/agent/AgentChat.vue"),
        meta: { title: "AI 助手", icon: "ChatDotSquare" },
      },

      // ===== ADMIN-ONLY routes =====
      {
        path: "map-view",
        name: "MapView",
        component: () => import("@/views/map/MapView.vue"),
        meta: { title: "地图点位", icon: "MapLocation", roles: [RoleCode.ADMIN] },
      },
      {
        path: "data-screen",
        name: "DataScreen",
        component: () => import("@/views/detection/DataScreen.vue"),
        meta: { title: "数据大屏", icon: "DataAnalysis", roles: [RoleCode.ADMIN] },
      },
      {
        path: "work-orders",
        name: "WorkOrderManagement",
        component: () => import("@/views/workorder/WorkOrderManagement.vue"),
        meta: { title: "工单管理", icon: "List", roles: [RoleCode.ADMIN] },
      },
      {
        path: "road-management",
        name: "RoadManagement",
        component: () => import("@/views/road/RoadManagement.vue"),
        meta: { title: "道路管理", icon: "MapLocation", roles: [RoleCode.ADMIN] },
      },
      {
        path: "road-health",
        name: "RoadHealthArchive",
        component: () => import("@/views/road/RoadHealthArchive.vue"),
        meta: { title: "健康档案", icon: "Document", roles: [RoleCode.ADMIN] },
      },
      {
        path: "road-maintenance",
        name: "RoadMaintenance",
        component: () => import("@/views/road/RoadMaintenance.vue"),
        meta: { title: "道路养护", icon: "Tools", roles: [RoleCode.ADMIN] },
      },
      {
        path: "reports",
        name: "MaintenanceReport",
        component: () => import("@/views/report/MaintenanceReport.vue"),
        meta: { title: "维修报告", icon: "EditPen", roles: [RoleCode.ADMIN] },
      },
      {
        path: "analysis-report",
        name: "AnalysisReport",
        component: () => import("@/views/analysis/AnalysisReport.vue"),
        meta: { title: "分析报告", icon: "DataLine", roles: [RoleCode.ADMIN] },
      },
      {
        path: "alerts",
        name: "AlertManagement",
        component: () => import("@/views/alert/AlertManagement.vue"),
        meta: { title: "预警管理", icon: "WarningFilled", roles: [RoleCode.ADMIN] },
      },
      // Admin system management
      {
        path: "user-management",
        name: "UserManagement",
        component: () => import("@/views/system/UserManagement.vue"),
        meta: { title: "用户管理", icon: "UserFilled", roles: [RoleCode.ADMIN] },
      },
      {
        path: "department-management",
        name: "DepartmentManagement",
        component: () => import("@/views/system/DepartmentManagement.vue"),
        meta: { title: "部门管理", icon: "OfficeBuilding", roles: [RoleCode.ADMIN] },
      },
      {
        path: "role-management",
        name: "RoleManagement",
        component: () => import("@/views/system/RolePermission.vue"),
        meta: { title: "角色权限", icon: "Avatar", roles: [RoleCode.ADMIN] },
      },
      {
        path: "audit-log",
        name: "AuditLog",
        component: () => import("@/views/system/AuditLog.vue"),
        meta: { title: "审计日志", icon: "Document", roles: [RoleCode.ADMIN] },
      },
      {
        path: "system-config",
        name: "SystemConfig",
        component: () => import("@/views/system/SystemConfig.vue"),
        meta: { title: "系统配置", icon: "Tools", roles: [RoleCode.ADMIN] },
      },

      // ===== DEPT ADMIN routes =====
      {
        path: "dept-orders",
        name: "DeptOrders",
        component: () => import("@/views/workorder/WorkOrderManagement.vue"),
        meta: { title: "部门工单", icon: "List", roles: DEPT_ADMIN_ROLES },
      },
      {
        path: "dept-orders-assign",
        name: "DeptOrdersAssign",
        component: () => import("@/views/worker/MyWorkOrders.vue"),
        meta: { title: "分配维修工", icon: "UserFilled", roles: DEPT_ADMIN_ROLES },
      },

      // ===== MAINTAINER routes =====
      {
        path: "my-work-orders",
        name: "MyWorkOrders",
        component: () => import("@/views/worker/MyWorkOrders.vue"),
        meta: { title: "我的工单", icon: "List", roles: [RoleCode.MAINTAINER] },
      },
      {
        path: "submit-report",
        name: "SubmitReport",
        component: () => import("@/views/worker/SubmitReport.vue"),
        meta: { title: "提交维修报告", icon: "EditPen", roles: [RoleCode.MAINTAINER] },
      },

      // ===== CROWDSOURCE routes =====
      {
        path: "crowd-report",
        name: "CrowdReport",
        component: () => import("@/views/crowd/CrowdReport.vue"),
        meta: { title: "上报问题", icon: "Upload", roles: [RoleCode.CROWDSOURCE] },
      },
      {
        path: "crowd-records",
        name: "CrowdRecords",
        component: () => import("@/views/crowd/CrowdRecords.vue"),
        meta: { title: "上报记录", icon: "List", roles: [RoleCode.CROWDSOURCE] },
      },

      // ===== SHARED =====
      {
        path: "user-profile",
        name: "UserProfile",
        component: () => import("@/views/system/UserProfile.vue"),
        meta: { title: "个人中心", icon: "User" },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title || "途安智巡"} - 途安智巡`
  if (to.meta.requiresAuth === false || to.name === "Login") return next()
  const token = localStorage.getItem("accessToken")
  if (!token) return next("/login")
  try {
    const authStore = useAuthStore()
    const userRoles = authStore.roles.length > 0 ? authStore.roles : JSON.parse(localStorage.getItem("roles") || "[]")
    const allRoles = to.matched.map(r => r.meta?.roles).filter(Boolean).flat() as string[]
    if (allRoles.length > 0) {
      const hasAccess = userRoles.some((r: string) => allRoles.includes(r))
      if (!hasAccess) {
        const firstRole = userRoles[0]
        if (firstRole === RoleCode.ADMIN) return next("/dashboard")
        if (RoleCode.CROWDSOURCE === firstRole) return next("/crowd-report")
        if (RoleCode.MAINTAINER === firstRole) return next("/my-work-orders")
        if ([RoleCode.ROAD_ADMIN, RoleCode.SANIT_ADMIN, RoleCode.TRAFFIC_ADMIN].includes(firstRole)) return next("/dept-orders")
        return next("/login")
      }
    }
  } catch { /* allow through */ }
  next()
})

export default router
