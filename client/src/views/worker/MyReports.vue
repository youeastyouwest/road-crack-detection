<template>
  <div class="mr-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ t("myreport.title") }}</h2>
        <p class="page-desc">{{ t("myreport.desc") }}</p>
      </div>
      <div class="tab-bar">
        <button :class="['tab-btn', activeTab==='all'?'active':'']" @click="activeTab='all'">
          {{ t("myreport.tabAll") }} <span class="tab-badge">{{ reports.length }}</span>
        </button>
        <button :class="['tab-btn', activeTab==='pending'?'active':'']" @click="activeTab='pending'">
          {{ t("myreport.tabPending") }} <span class="tab-badge">{{ pendingReports.length }}</span>
        </button>
        <button :class="['tab-btn', activeTab==='approved'?'active':'']" @click="activeTab='approved'">
          {{ t("myreport.tabApproved") }} <span class="tab-badge">{{ approvedReports.length }}</span>
        </button>
        <button :class="['tab-btn', activeTab==='rejected'?'active':'']" @click="activeTab='rejected'">
          {{ t("myreport.tabRejected") }} <span class="tab-badge">{{ rejectedReports.length }}</span>
        </button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></div><div><span class="stat-val">{{ reports.length }}</span><span class="stat-lbl">{{ t("myreport.totalReports") }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef3c7;color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg></div><div><span class="stat-val">{{ pendingReports.length }}</span><span class="stat-lbl">{{ t("myreport.tabPending") }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ approvedReports.length }}</span><span class="stat-lbl">{{ t("myreport.tabApproved") }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef2f2;color:#dc2626"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></div><div><span class="stat-val">{{ rejectedReports.length }}</span><span class="stat-lbl">{{ t("myreport.tabRejected") }}</span></div></div>
    </div>

    <div class="content-card">
      <div class="card-head">
        <div class="card-head-left">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          {{ activeTab === 'pending' ? t("myreport.pendingTitle") : activeTab === 'approved' ? t("myreport.approvedTitle") : activeTab === 'rejected' ? t("myreport.rejectedTitle") : t("myreport.allTitle") }}
        </div>
        <div class="card-head-right">
          <button class="refresh-btn" @click="loadData" :disabled="loading">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" :class="{spinning: loading}"><path d="M23 4v6h-6"/><path d="M1 20v-6h6"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
            {{ t("myreport.refresh") }}
          </button>
          <span class="toolbar-info">{{ displayReports.length }} {{ t("myreport.records") }}</span>
        </div>
      </div>

      <div class="report-list" v-loading="loading">
        <div v-for="r in displayReports" :key="r.id" class="report-item">
          <div class="ri-left">
            <div class="ri-code">{{ r.reportCode || t("myreport.reportCode") + r.id }}</div>
            <div class="ri-info">
              <span class="ri-road">{{ getWorkOrderTitle(r.workOrderId) }}</span>
              <span class="ri-meta">{{ r.executor }} · {{ r.materials || t("myreport.noMaterials") }} · {{ formatDate(r.finishedAt) }}</span>
            </div>
          </div>
          <div class="ri-center">
            <span :class="['ri-status', reportStatusCls(r.status)]">{{ reportStatusLabel(r.status) }}</span>
            <div class="ri-materials" v-if="r.description">{{ r.description.substring(0, 40) }}{{ r.description.length > 40 ? '...' : '' }}</div>
            <div class="ri-review-remark" v-if="r.status === 'REJECTED' && r.reviewRemark">
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              {{ r.reviewRemark }}
            </div>
          </div>
          <div class="ri-right">
            <button class="action-btn" @click="viewDetail(r)">{{ t("myreport.detail") }}</button>
            <button v-if="r.status === 'REJECTED'" class="action-btn action-resubmit" @click="goResubmit(r)">{{ t("myreport.resubmit") }}</button>
          </div>
        </div>
        <div v-if="!loading && displayReports.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          <span>{{ t("myreport.noReports") }}</span>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail=false">
      <div class="modal-card">
        <div class="modal-head">
          <span>{{ t("myreport.reportDetail") }}{{ detailTarget?.reportCode || t("myreport.reportCode") + detailTarget?.id }}</span>
          <button class="modal-close" @click="showDetail=false">✕</button>
        </div>
        <div class="modal-body" v-loading="detailLoading">
          <template v-if="detailTarget">
            <div class="detail-grid">
              <div class="detail-item"><label>{{ t("myreport.reportId") }}</label><span>{{ detailTarget.reportCode || '--' }}</span></div>
              <div class="detail-item"><label>{{ t("common.status") }}</label><span :style="{color: reportStatusColor(detailTarget.status), fontWeight:600}">{{ reportStatusLabel(detailTarget.status) }}</span></div>
              <div class="detail-item"><label>{{ t("myreport.relatedOrder") }}</label><span>#{{ detailTarget.workOrderId }} {{ getWorkOrderTitle(detailTarget.workOrderId) }}</span></div>
              <div class="detail-item"><label>{{ t("myreport.executor") }}</label><span>{{ detailTarget.executor }}</span></div>
              <div class="detail-item"><label>{{ t("myreport.finishedAt") }}</label><span>{{ formatDate(detailTarget.finishedAt) }}</span></div>
              <div class="detail-item"><label>{{ t("myreport.submittedAt") }}</label><span>{{ formatDate(detailTarget.createdAt) }}</span></div>
              <div class="detail-item"><label>{{ t("myreport.materials") }}</label><span>{{ detailTarget.materials || '--' }}</span></div>
              <div class="detail-item" v-if="detailTarget.reviewer"><label>{{ t("myreport.reviewer") }}</label><span>{{ detailTarget.reviewer }}</span></div>
              <div class="detail-item" v-if="detailTarget.reviewedAt"><label>{{ t("myreport.reviewedAt") }}</label><span>{{ formatDate(detailTarget.reviewedAt) }}</span></div>
              <div class="detail-item" style="grid-column:1/-1"><label>{{ t("common.description") }}</label><span style="line-height:1.6">{{ detailTarget.description || '--' }}</span></div>
              <div class="detail-item" v-if="detailTarget.reviewRemark" style="grid-column:1/-1">
                <label>{{ t("myreport.reviewOpinion") }}</label>
                <span style="line-height:1.6;color:#dc2626">{{ detailTarget.reviewRemark }}</span>
              </div>
              <div class="detail-item" v-if="resolvedBeforeImageUrl" style="grid-column:1/-1">
                <label>{{ t("myreport.beforeImg") }}</label>
                <img :src="resolvedBeforeImageUrl" class="report-img" :alt="t('myreport.beforeImg')" />
              </div>
              <div class="detail-item" v-if="resolvedAfterImageUrl" style="grid-column:1/-1">
                <label>{{ t("myreport.afterImg") }}</label>
                <img :src="resolvedAfterImageUrl" class="report-img" :alt="t('myreport.afterImg')" />
              </div>
            </div>
          </template>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showDetail=false">{{ t("myreport.close") }}</button>
          <button v-if="detailTarget && detailTarget.status === 'REJECTED'" class="btn-resubmit" @click="goResubmit(detailTarget)">{{ t("myreport.resubmitReport") }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { reportApi, workOrderApi } from "@/api"
import { useAuthStore } from "@/stores/auth"
import { t } from "@/i18n"
import type { MaintenanceReportResponse, WorkOrderResponse } from "@/types"
import { resolveReportImageUrl } from "@/utils/reportImages"

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref("all")
const reports = ref<MaintenanceReportResponse[]>([])
const workOrderMap = ref<Map<number, WorkOrderResponse>>(new Map())
const loading = ref(false)
const showDetail = ref(false)
const detailTarget = ref<MaintenanceReportResponse | null>(null)
const detailLoading = ref(false)
const resolvedBeforeImageUrl = computed(() => resolveReportImageUrl(detailTarget.value?.beforeImageUrl))
const resolvedAfterImageUrl = computed(() => resolveReportImageUrl(detailTarget.value?.afterImageUrl))

const pendingReports = computed(() => reports.value.filter(r => r.status === "PENDING" || r.status === "DEPT_APPROVED"))
const approvedReports = computed(() => reports.value.filter(r => r.status === "APPROVED"))
const rejectedReports = computed(() => reports.value.filter(r => r.status === "REJECTED"))
const displayReports = computed(() => {
  if (activeTab.value === "pending") return pendingReports.value
  if (activeTab.value === "approved") return approvedReports.value
  if (activeTab.value === "rejected") return rejectedReports.value
  return reports.value
})

function getWorkOrderTitle(workOrderId: number): string {
  const wo = workOrderMap.value.get(workOrderId)
  return wo?.title || `${t("wo.id")} #${workOrderId}`
}

function reportStatusCls(s?: string) {
  return ({
    PENDING: "ri-pending",
    DEPT_APPROVED: "ri-dept-approved",
    APPROVED: "ri-archived",
    REJECTED: "ri-rejected",
  } as any)[s || ""] || "ri-pending"
}
function reportStatusLabel(s?: string) {
  return ({
    PENDING: () => t("myreport.statusDeptReview"),
    DEPT_APPROVED: () => t("myreport.statusDeptApproved"),
    APPROVED: () => t("myreport.statusApproved"),
    REJECTED: () => t("myreport.statusRejected"),
  } as any)[s || ""]?.() || s || "--"
}
function reportStatusColor(s?: string) {
  return ({
    PENDING: "#d97706",
    DEPT_APPROVED: "#0369a1",
    APPROVED: "#16a34a",
    REJECTED: "#dc2626",
  } as any)[s || ""] || "#64748b"
}

function formatDate(dt?: string): string {
  if (!dt) return "--"
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
    const myName = authStore.realName || authStore.username || ""
    const [reportRes, workOrderRes] = await Promise.all([
      reportApi.list({ page: 1, size: 100, executor: myName }),
      workOrderApi.list({ page: 1, size: 200, assignee: myName } as any),
    ])
    reports.value = reportRes.data.data.records || []

    const wom = new Map<number, WorkOrderResponse>()
    for (const wo of workOrderRes.data.data.records || []) {
      wom.set(wo.id, wo)
    }
    workOrderMap.value = wom
  } catch (e: any) {
    // Fallback: load all reports and filter client-side
    try {
      const reportRes = await reportApi.list({ page: 1, size: 200 })
      const all = reportRes.data.data.records || []
      const myName = authStore.realName || authStore.username || ""
      reports.value = all.filter(r => r.executor === myName)

      const woRes = await workOrderApi.list({ page: 1, size: 200 } as any)
      const wom = new Map<number, WorkOrderResponse>()
      for (const wo of woRes.data.data.records || []) {
        wom.set(wo.id, wo)
      }
      workOrderMap.value = wom
    } catch {
      ElMessage.error(t("myreport.loadFailed"))
    }
  } finally {
    loading.value = false
  }
}

async function viewDetail(r: MaintenanceReportResponse) {
  showDetail.value = true
  detailTarget.value = r
  detailLoading.value = true
  try {
    const res = await reportApi.get(r.id)
    detailTarget.value = res.data.data
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t("myreport.detailFailed"))
  } finally {
    detailLoading.value = false
  }
}

function goResubmit(r: MaintenanceReportResponse) {
  showDetail.value = false
  router.push({ path: "/submit-report", query: { workOrderId: String(r.workOrderId) } })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.mr-page { font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:16px; flex-wrap:wrap; gap:12px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#64748b; margin:0; }

.tab-bar { display:flex; gap:4px; background:#f1f5f9; border-radius:8px; padding:3px; }
.tab-btn { display:flex; align-items:center; gap:6px; padding:7px 14px; border:none; border-radius:6px; background:transparent; font-size:12px; font-weight:500; color:#64748b; cursor:pointer; font-family:inherit; transition:all .15s; }
.tab-btn.active { background:#fff; color:#2563eb; box-shadow:0 1px 3px rgba(0,0,0,0.08); }
.tab-badge { font-size:10px; font-weight:700; padding:1px 6px; border-radius:10px; background:rgba(37,99,235,0.1); color:#2563eb; }
.tab-btn.active .tab-badge { background:rgba(37,99,235,0.12); }

.stat-row { display:flex; gap:12px; margin-bottom:16px; }
.stat-card { flex:1; display:flex; align-items:center; gap:14px; padding:14px 18px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.stat-icon { width:38px; height:38px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.stat-val { font-size:22px; font-weight:800; color:#0f172a; line-height:1; }
.stat-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }

.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.card-head { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f0f2f5; }
.card-head-left { display:flex; align-items:center; gap:6px; font-size:13px; font-weight:600; color:#334155; }
.card-head-right { display:flex; align-items:center; gap:12px; }
.toolbar-info { font-size:12px; color:#94a3b8; }
.refresh-btn { display:flex; align-items:center; gap:4px; padding:4px 10px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:11px; color:#64748b; cursor:pointer; font-family:inherit; transition:all .15s; }
.refresh-btn:hover { border-color:#2563eb; color:#2563eb; }
.refresh-btn:disabled { opacity:0.5; cursor:not-allowed; }
.spinning { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.report-list { display:flex; flex-direction:column; min-height:200px; }
.report-item { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f5f6f8; gap:16px; }
.report-item:hover { background:#fafbfc; }
.ri-left { display:flex; align-items:center; gap:14px; flex:1; min-width:0; }
.ri-code { font-family:monospace; font-size:12px; font-weight:700; color:#2563eb; white-space:nowrap; }
.ri-info { display:flex; flex-direction:column; gap:2px; min-width:0; }
.ri-road { font-size:13px; font-weight:500; color:#0f172a; }
.ri-meta { font-size:11px; color:#94a3b8; }
.ri-center { display:flex; flex-direction:column; align-items:center; gap:4px; min-width:140px; }
.ri-status { font-size:11px; font-weight:600; padding:2px 10px; border-radius:10px; }
.ri-pending { background:#fffbeb; color:#d97706; }
.ri-dept-approved { background:#e0f2fe; color:#0369a1; }
.ri-archived { background:#f0fdf4; color:#16a34a; }
.ri-rejected { background:#fef2f2; color:#dc2626; }
.ri-materials { font-size:10px; color:#94a3b8; max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.ri-review-remark { font-size:10px; color:#dc2626; max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; display:flex; align-items:center; gap:4px; }
.ri-right { display:flex; gap:4px; flex-shrink:0; }
.action-btn { padding:5px 12px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:11px; font-weight:500; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.action-btn:hover { border-color:#2563eb; color:#2563eb; }
.action-btn:disabled { opacity:0.5; cursor:not-allowed; }
.action-resubmit { color:#d97706; border-color:#fde68a; }
.action-resubmit:hover { background:#fffbeb; border-color:#d97706; }
.empty-state { display:flex; flex-direction:column; align-items:center; gap:10px; padding:60px 0; color:#94a3b8; font-size:13px; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:580px; max-height:85vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-body { padding:20px; min-height:100px; }
.detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
.detail-item { display:flex; flex-direction:column; gap:4px; }
.detail-item label { font-size:11px; font-weight:600; color:#64748b; }
.detail-item span { font-size:13px; color:#0f172a; }
.report-img { max-width:100%; border-radius:8px; border:1px solid #f0f2f5; margin-top:4px; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
.btn-ghost { padding:7px 16px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.btn-resubmit { padding:7px 16px; border:none; border-radius:6px; background:#d97706; color:#fff; font-size:12px; font-weight:500; cursor:pointer; }
.btn-resubmit:hover { background:#b45309; }
</style>
