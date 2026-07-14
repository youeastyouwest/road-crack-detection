<template>
  <div class="rm-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ t('maint.title') }}</h2>
        <p class="page-desc">{{ t('maint.desc') }}</p>
      </div>
      <div class="header-actions">
        <button class="refresh-btn" @click="loadData" :disabled="loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" :class="{spinning: loading}"><path d="M23 4v6h-6"/><path d="M1 20v-6h6"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
          {{ t('common.refresh') }}
        </button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#eef2ff,#dbeafe);color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg></div><div><span class="stat-val">{{ stats.total }}</span><span class="stat-lbl">{{ t('maint.totalOrders') }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#fef3c7,#fde68a);color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div><span class="stat-val">{{ stats.pending }}</span><span class="stat-lbl">{{ t('maint.pendingExecute') }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#dbeafe,#bfdbfe);color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg></div><div><span class="stat-val">{{ stats.inProgress }}</span><span class="stat-lbl">{{ t('maint.inProgress') }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#dcfce7,#bbf7d0);color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ stats.completed }}</span><span class="stat-lbl">{{ t('status.completed') }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#fce7f3,#fbcfe8);color:#e11d48"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-6"/></svg></div><div><span class="stat-val">{{ stats.overdue }}</span><span class="stat-lbl">{{ t('maint.overdue') }}</span></div></div>
    </div>

    <div class="content-card">
      <div class="card-head">
        <div class="card-head-left">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          {{ t('maint.timeline') }}
          <div class="filter-bar">
            <select v-model="filterStatus" class="filter-select">
              <option value="">{{ t('maint.allStatus') }}</option>
              <option value="PENDING_ASSIGNMENT">{{ t('status.pendingAssignment') }}</option>
              <option value="ASSIGNED">{{ t('status.assigned') }}</option>
              <option value="IN_PROGRESS">{{ t('status.inProgress') }}</option>
              <option value="COMPLETED">{{ t('status.completed') }}</option>
              <option value="CLOSED">{{ t('maint.closed') }}</option>
            </select>
            <select v-model="filterDept" class="filter-select">
              <option value="">{{ t('maint.allDept') }}</option>
              <option value="ROAD_ADMIN">{{ t('maint.roadAdmin') }}</option>
              <option value="SANITATION">{{ t('maint.sanitation') }}</option>
              <option value="TRAFFIC_POLICE">{{ t('maint.trafficPolice') }}</option>
            </select>
          </div>
        </div>
        <span class="toolbar-info">{{ displayTasks.length }} {{ t('mreport.records') }}</span>
      </div>

      <div class="timeline" v-loading="loading">
        <div v-for="(t,i) in displayTasks" :key="t.id" class="tl-item">
          <div class="tl-dot" :class="dotClass(t)"></div>
          <div class="tl-line" v-if="i < displayTasks.length - 1"></div>
          <div class="tl-content">
            <div class="tl-head">
              <span class="tl-title">{{ t.title }}</span>
              <span :class="['tl-badge', badgeClass(t)]">{{ statusText(t.status) }}</span>
              <span v-if="isOverdue(t)" class="tl-badge badge-overdue">{{ translate('maint.overdue') }}</span>
            </div>
            <div class="tl-meta">
              <span class="tl-meta-item" v-if="t.location">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>{{ t.location }}
              </span>
              <span class="tl-meta-item">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>{{ formatDate(t.createdAt) }}
              </span>
              <span v-if="t.assignee" class="tl-meta-item">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>{{ t.assignee }}
              </span>
              <span v-if="t.dueAt" class="tl-meta-item">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>截止: {{ formatDate(t.dueAt) }}
              </span>
            </div>
            <div v-if="t.description" class="tl-desc">{{ t.description }}</div>

            <!-- Report info if exists -->
            <template v-if="getReport(t.id)">
              <div class="tl-report" v-if="getReport(t.id)" :key="'rpt-' + t.id">
                <div class="tl-report-head">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg>
                  <span class="tl-report-code">{{ getReport(t.id)!.reportCode || translate('maint.reportCode') + getReport(t.id)!.id }}</span>
                  <span class="tl-report-executor">{{ translate('maint.executor') }}: {{ getReport(t.id)!.executor }}</span>
                  <span class="tl-report-date">{{ formatDate(getReport(t.id)!.finishedAt) }}</span>
                </div>
                <div v-if="getReport(t.id)!.materials" class="tl-report-materials">{{ translate('maint.materials') }}: {{ getReport(t.id)!.materials }}</div>
                <div v-if="getReport(t.id)!.description" class="tl-report-desc">{{ getReport(t.id)!.description }}</div>
              </div>
            </template>
          </div>
        </div>
        <div v-if="!loading && displayTasks.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          <span>{{ t('maint.noMaintenance') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { workOrderApi, reportApi } from "@/api"
import { t } from "@/i18n"
import type { WorkOrderResponse, MaintenanceReportResponse, WorkOrderStatus } from "@/types"

const loading = ref(false)
const translate = t
const workOrders = ref<WorkOrderResponse[]>([])
const reportMap = ref<Map<number, MaintenanceReportResponse>>(new Map())
const filterStatus = ref("")
const filterDept = ref("")

const stats = computed(() => {
  const list = workOrders.value
  return {
    total: list.length,
    pending: list.filter(t => t.status === ("PENDING_ASSIGNMENT" as WorkOrderStatus) || t.status === ("ASSIGNED" as WorkOrderStatus)).length,
    inProgress: list.filter(t => t.status === ("IN_PROGRESS" as WorkOrderStatus)).length,
    completed: list.filter(t => t.status === ("COMPLETED" as WorkOrderStatus) || t.status === ("CLOSED" as WorkOrderStatus)).length,
    overdue: list.filter(t => isOverdue(t)).length,
  }
})

const displayTasks = computed(() => {
  let result = workOrders.value
  if (filterStatus.value) {
    result = result.filter(t => t.status === filterStatus.value)
  }
  if (filterDept.value) {
    result = result.filter(t => t.departmentCode === filterDept.value)
  }
  // Sort by createdAt descending (newest first)
  return [...result].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
})

function isOverdue(t: WorkOrderResponse): boolean {
  if (!t.dueAt) return false
  if (t.status === ("COMPLETED" as WorkOrderStatus) || t.status === ("CLOSED" as WorkOrderStatus) || t.status === ("CANCELLED" as WorkOrderStatus)) {
    return false
  }
  return new Date(t.dueAt).getTime() < Date.now()
}

function getReport(workOrderId: number): MaintenanceReportResponse | undefined {
  return reportMap.value.get(workOrderId)
}

function dotClass(t: WorkOrderResponse): string {
  if (t.status === ("CLOSED" as WorkOrderStatus) || t.status === ("COMPLETED" as WorkOrderStatus)) return "tl-dot-done"
  if (t.status === ("IN_PROGRESS" as WorkOrderStatus)) return "tl-dot-active"
  return "tl-dot-pending"
}

function badgeClass(t: WorkOrderResponse): string {
  if (t.status === ("CLOSED" as WorkOrderStatus) || t.status === ("COMPLETED" as WorkOrderStatus)) return "badge-done"
  if (t.status === ("IN_PROGRESS" as WorkOrderStatus)) return "badge-active"
  if (t.status === ("CANCELLED" as WorkOrderStatus)) return "badge-cancelled"
  return "badge-pending"
}

function statusText(status: string): string {
  const map: Record<string, string> = {
    PENDING_ASSIGNMENT: t('status.pendingAssignment'),
    ASSIGNED: t('status.assigned'),
    IN_PROGRESS: t('status.inProgress'),
    COMPLETED: t('status.completed'),
    CLOSED: t('maint.closed'),
    CANCELLED: t('maint.cancelled'),
  }
  return map[status] || status
}

function formatDate(dt: string): string {
  if (!dt) return '--'
  try {
    const d = new Date(dt)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch {
    return dt
  }
}

async function loadData() {
  loading.value = true
  try {
    const [woRes, reportRes] = await Promise.all([
      workOrderApi.list({ page: 1, size: 200 }),
      reportApi.list({ page: 1, size: 200 }),
    ])
    workOrders.value = woRes.data.data.records || []

    const rm = new Map<number, MaintenanceReportResponse>()
    for (const r of reportRes.data.data.records || []) {
      rm.set(r.workOrderId, r)
    }
    reportMap.value = rm
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t("maint.loadFailed"))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.rm-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }

.header-actions { display:flex; gap:8px; }
.refresh-btn { display:flex; align-items:center; gap:4px; padding:7px 14px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; }
.refresh-btn:hover { border-color:#2563eb; color:#2563eb; }
.refresh-btn:disabled { opacity:0.5; cursor:not-allowed; }
.spinning { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.stat-row { display:flex; gap:12px; margin-bottom:16px; }
.stat-card { flex:1; display:flex; align-items:center; gap:14px; padding:14px 18px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.stat-icon { width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.stat-val { font-size:20px; font-weight:800; color:#0f172a; line-height:1; }
.stat-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }

.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.card-head { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; font-size:13px; font-weight:600; color:#0f172a; border-bottom:1px solid #f0f2f5; }
.card-head-left { display:flex; align-items:center; gap:6px; flex:1; flex-wrap:wrap; }
.card-head-right { display:flex; align-items:center; gap:12px; }
.toolbar-info { font-size:12px; color:#94a3b8; }
.filter-bar { display:flex; gap:8px; margin-left:12px; }
.filter-select { padding:4px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:11px; color:#475569; background:#fff; cursor:pointer; font-family:inherit; outline:none; }
.filter-select:focus { border-color:#2563eb; }

.timeline { padding:20px 24px; min-height:200px; }
.tl-item { position:relative; display:flex; gap:16px; padding-bottom:20px; }
.tl-item:last-child { padding-bottom:0; }
.tl-dot { width:12px; height:12px; border-radius:50%; flex-shrink:0; margin-top:4px; z-index:1; position:relative; }
.tl-dot-done { background:#22c55e; box-shadow:0 0 0 3px #dcfce7; }
.tl-dot-active { background:#2563eb; box-shadow:0 0 0 3px #dbeafe; animation:dotPulse 2s infinite; }
.tl-dot-pending { background:#d0d5dd; box-shadow:0 0 0 3px #f1f5f9; }
@keyframes dotPulse { 0%,to { box-shadow:0 0 0 3px #dbeafe; } 50% { box-shadow:0 0 0 5px #bfdbfe; } }
.tl-line { position:absolute; left:5px; top:18px; width:2px; bottom:0; background:#f0f2f5; }
.tl-content { flex:1; }
.tl-head { display:flex; align-items:center; gap:10px; margin-bottom:6px; flex-wrap:wrap; }
.tl-title { font-size:14px; font-weight:600; color:#0f172a; }
.tl-badge { padding:2px 8px; border-radius:4px; font-size:10px; font-weight:600; }
.badge-done { background:#f0fdf4; color:#16a34a; }
.badge-active { background:#dbeafe; color:#2563eb; }
.badge-pending { background:#f1f5f9; color:#64748b; }
.badge-cancelled { background:#fef2f2; color:#dc2626; }
.badge-overdue { background:#fee2e2; color:#e11d48; }
.tl-meta { display:flex; gap:16px; margin-bottom:6px; flex-wrap:wrap; }
.tl-meta-item { display:flex; align-items:center; gap:4px; font-size:11px; color:#94a3b8; }
.tl-desc { font-size:12px; color:#64748b; line-height:1.5; padding:8px 12px; background:#f8f9fc; border-radius:6px; margin-top:4px; }

.tl-report { margin-top:8px; padding:12px 14px; background:#f0fdf4; border:1px solid #bbf7d0; border-radius:8px; }
.tl-report-head { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.tl-report-code { font-family:monospace; font-size:12px; font-weight:700; color:#16a34a; }
.tl-report-executor { font-size:11px; color:#475569; }
.tl-report-date { font-size:11px; color:#94a3b8; margin-left:auto; }
.tl-report-materials { font-size:11px; color:#64748b; margin-top:6px; }
.tl-report-desc { font-size:11px; color:#64748b; line-height:1.5; margin-top:4px; }

.empty-state { display:flex; flex-direction:column; align-items:center; gap:10px; padding:60px 0; color:#94a3b8; font-size:13px; }
</style>
