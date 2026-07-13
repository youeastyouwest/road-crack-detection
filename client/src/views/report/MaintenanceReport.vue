<template>
  <div class="mr-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">维修报告</h2>
        <p class="page-desc">终审已通过部门审核的维修报告，归档已确认的报告</p>
      </div>
      <div class="tab-bar">
        <button :class="['tab-btn', activeTab==='pending'?'active':'']" @click="activeTab='pending'">
          待审核 <span class="tab-badge">{{ pendingReports.length }}</span>
        </button>
        <button :class="['tab-btn', activeTab==='archived'?'active':'']" @click="activeTab='archived'">
          已归档 <span class="tab-badge">{{ archivedReports.length }}</span>
        </button>
        <button :class="['tab-btn', activeTab==='all'?'active':'']" @click="activeTab='all'">
          全部 <span class="tab-badge">{{ reports.length }}</span>
        </button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></div><div><span class="stat-val">{{ reports.length }}</span><span class="stat-lbl">总报告数</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef3c7;color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-6"/></svg></div><div><span class="stat-val">{{ pendingReports.length }}</span><span class="stat-lbl">待审核</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ archivedReports.length }}</span><span class="stat-lbl">已归档</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fce7f3;color:#e11d48"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div><span class="stat-val">{{ thisMonth }}</span><span class="stat-lbl">本月新增</span></div></div>
    </div>

    <div class="content-card">
      <div class="card-head">
        <div class="card-head-left">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          {{ activeTab === 'pending' ? '待审核报告' : activeTab === 'archived' ? '已归档报告' : '全部报告' }}
        </div>
        <div class="card-head-right">
          <button class="refresh-btn" @click="loadData" :disabled="loading">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" :class="{spinning: loading}"><path d="M23 4v6h-6"/><path d="M1 20v-6h6"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
            刷新
          </button>
          <span class="toolbar-info">{{ displayReports.length }} 条记录</span>
        </div>
      </div>

      <div class="report-list" v-loading="loading">
        <div v-for="r in displayReports" :key="r.id" class="report-item">
          <div class="ri-left">
            <div class="ri-code">{{ r.reportCode || '报告#' + r.id }}</div>
            <div class="ri-info">
              <span class="ri-road">{{ getWorkOrderTitle(r.workOrderId) }}</span>
              <span class="ri-meta">{{ r.executor }} · {{ r.materials || '未记录材料' }} · {{ formatDate(r.finishedAt) }}</span>
            </div>
          </div>
          <div class="ri-center">
            <span :class="['ri-status', reportStatusCls(r.status)]">{{ reportStatusLabel(r.status) }}</span>
            <div class="ri-materials" v-if="r.description">{{ r.description.substring(0, 40) }}{{ r.description.length > 40 ? '...' : '' }}</div>
          </div>
          <div class="ri-right">
            <button class="action-btn" @click="viewDetail(r)">详情</button>
            <button v-if="r.status === 'DEPT_APPROVED'" class="action-btn action-approve" @click="approveReport(r)" :disabled="reviewing">通过</button>
            <button v-if="r.status === 'DEPT_APPROVED'" class="action-btn action-reject" @click="rejectReport(r)" :disabled="reviewing">拒绝</button>
          </div>
        </div>
        <div v-if="!loading && displayReports.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          <span>暂无{{ activeTab==='pending'?'待审核':'' }}报告</span>
        </div>
      </div>
    </div>

    <!-- Detail/Review Modal -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail=false">
      <div class="modal-card">
        <div class="modal-head">
          <span>报告详情 - {{ detailTarget?.reportCode || '报告#' + detailTarget?.id }}</span>
          <button class="modal-close" @click="showDetail=false">✕</button>
        </div>
        <div class="modal-body" v-loading="detailLoading">
          <template v-if="detailTarget">
            <div class="detail-grid">
              <div class="detail-item"><label>报告编号</label><span>{{ detailTarget.reportCode || '--' }}</span></div>
              <div class="detail-item"><label>状态</label><span :style="{color: reportStatusColor(detailTarget.status), fontWeight:600}">{{ reportStatusLabel(detailTarget.status) }}</span></div>
              <div class="detail-item"><label>关联工单</label><span>#{{ detailTarget.workOrderId }} {{ getWorkOrderTitle(detailTarget.workOrderId) }}</span></div>
              <div class="detail-item"><label>工单状态</label><span>{{ getWorkOrderStatusText(detailTarget.workOrderId) }}</span></div>
              <div class="detail-item"><label>执行人</label><span>{{ detailTarget.executor }}</span></div>
              <div class="detail-item"><label>完成时间</label><span>{{ formatDate(detailTarget.finishedAt) }}</span></div>
              <div class="detail-item"><label>提交时间</label><span>{{ formatDate(detailTarget.createdAt) }}</span></div>
              <div class="detail-item"><label>使用材料</label><span>{{ detailTarget.materials || '--' }}</span></div>
              <div class="detail-item" style="grid-column:1/-1"><label>维修描述</label><span style="line-height:1.6">{{ detailTarget.description || '--' }}</span></div>
              <div class="detail-item" v-if="getImageUrls(detailTarget.beforeImageUrl).length > 0" style="grid-column:1/-1">
                <label>维修前图片 ({{ getImageUrls(detailTarget.beforeImageUrl).length }}张)</label>
                <div class="report-img-grid">
                  <img v-for="(url, i) in getImageUrls(detailTarget.beforeImageUrl)" :key="'before-'+i" :src="url" class="report-img" alt="维修前" @click="previewImage(url)" />
                </div>
              </div>
              <div class="detail-item" v-if="getImageUrls(detailTarget.afterImageUrl).length > 0" style="grid-column:1/-1">
                <label>维修后图片 ({{ getImageUrls(detailTarget.afterImageUrl).length }}张)</label>
                <div class="report-img-grid">
                  <img v-for="(url, i) in getImageUrls(detailTarget.afterImageUrl)" :key="'after-'+i" :src="url" class="report-img" alt="维修后" @click="previewImage(url)" />
                </div>
              </div>
            </div>
          </template>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showDetail=false">关闭</button>
          <button v-if="detailTarget && detailTarget.status === 'DEPT_APPROVED'" class="btn-success" @click="approveReport(detailTarget)" :disabled="reviewing">通过审核</button>
          <button v-if="detailTarget && detailTarget.status === 'DEPT_APPROVED'" class="btn-danger" @click="rejectReport(detailTarget)" :disabled="reviewing">拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { reportApi, workOrderApi } from "@/api"
import type { MaintenanceReportResponse, WorkOrderResponse } from "@/types"

const activeTab = ref("pending")
const reports = ref<MaintenanceReportResponse[]>([])
const workOrderMap = ref<Map<number, WorkOrderResponse>>(new Map())
const loading = ref(false)
const reviewing = ref(false)
const showDetail = ref(false)
const detailTarget = ref<MaintenanceReportResponse | null>(null)
const detailLoading = ref(false)

// 判断报告是否已归档：报告自身状态为 APPROVED
function isArchived(r: MaintenanceReportResponse): boolean {
  return r.status === "APPROVED"
}

// 道路管理员可审核的报告：只包含 DEPT_APPROVED（已通过部门审核，待终审）
const pendingReports = computed(() => reports.value.filter(r => r.status === "DEPT_APPROVED"))
const archivedReports = computed(() => reports.value.filter(r => r.status === "APPROVED"))
const displayReports = computed(() => {
  if (activeTab.value === "pending") return pendingReports.value
  if (activeTab.value === "archived") return archivedReports.value
  return reports.value
})
const thisMonth = computed(() => {
  const now = new Date()
  const m = now.getMonth()
  const y = now.getFullYear()
  return reports.value.filter(r => {
    if (!r.finishedAt) return false
    const d = new Date(r.finishedAt)
    return d.getMonth() === m && d.getFullYear() === y
  }).length
})

function getWorkOrderTitle(workOrderId: number): string {
  const wo = workOrderMap.value.get(workOrderId)
  return wo?.title || `工单 #${workOrderId}`
}

function getWorkOrderStatusText(workOrderId: number): string {
  const wo = workOrderMap.value.get(workOrderId)
  if (!wo) return '--'
  const map: Record<string, string> = {
    PENDING_ASSIGNMENT: '待分配',
    ASSIGNED: '已分配',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    CLOSED: '已关闭',
    CANCELLED: '已取消',
  }
  return map[wo.status] || wo.status
}

// ─── Report status helpers ───
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
    PENDING: "待部门审核",
    DEPT_APPROVED: "部门已通过",
    APPROVED: "已通过",
    REJECTED: "已驳回",
  } as any)[s || ""] || s || "--"
}
function reportStatusColor(s?: string) {
  return ({
    PENDING: "#d97706",
    DEPT_APPROVED: "#0369a1",
    APPROVED: "#16a34a",
    REJECTED: "#dc2626",
  } as any)[s || ""] || "#64748b"
}

function getImageUrls(urlStr?: string): string[] {
  if (!urlStr) return []
  return urlStr.split(",").map(u => {
    const trimmed = u.trim()
    if (!trimmed) return ""
    if (trimmed.startsWith("/api/file/download/")) {
      return "/uploads/" + trimmed.replace("/api/file/download/", "")
    }
    if (trimmed.startsWith("/api/")) {
      return window.location.origin + trimmed
    }
    return trimmed
  }).filter(Boolean)
}

function previewImage(url: string) {
  window.open(url, "_blank")
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
    // 并行获取报告列表和工单列表
    const [reportRes, workOrderRes] = await Promise.all([
      reportApi.list({ page: 1, size: 100 }),
      workOrderApi.list({ page: 1, size: 200 }),
    ])
    reports.value = reportRes.data.data.records || []

    // 构建 workOrderMap 用于查找工单状态
    const wom = new Map<number, WorkOrderResponse>()
    for (const wo of workOrderRes.data.data.records || []) {
      wom.set(wo.id, wo)
    }
    workOrderMap.value = wom
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "加载报告数据失败")
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
    ElMessage.error(e?.response?.data?.message || "加载报告详情失败")
  } finally {
    detailLoading.value = false
  }
}

async function approveReport(r: MaintenanceReportResponse) {
  try {
    await ElMessageBox.confirm(
      `确认通过报告 ${r.reportCode || '#' + r.id} 的终审？通过后关联工单 #${r.workOrderId} 将被关闭归档。`,
      "终审确认",
      { type: "warning", confirmButtonText: "确认通过", cancelButtonText: "取消" }
    )
  } catch {
    return // 用户取消
  }

  reviewing.value = true
  try {
    // 终审通过 → 调用 adminReview API
    await reportApi.adminReview(r.id, { approved: true, remark: "终审通过" })

    ElMessage.success("终审已通过，报告已归档，关联工单已关闭")
    // 更新本地报告状态
    r.status = "APPROVED"
    // 如果详情弹窗打开，也更新
    if (detailTarget.value?.id === r.id) {
      detailTarget.value = { ...detailTarget.value, status: "APPROVED" }
    }
    await loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "审核操作失败")
  } finally {
    reviewing.value = false
  }
}

async function rejectReport(r: MaintenanceReportResponse) {
  let reason = ""
  try {
    const result = await ElMessageBox.prompt("请输入驳回原因", "驳回报告", {
      inputPlaceholder: "请输入驳回原因",
      inputValidator: (val: string) => (val && val.trim().length > 0) || "请输入驳回原因",
    })
    reason = result.value
  } catch {
    return // 用户取消
  }

  reviewing.value = true
  try {
    // 终审驳回 → 调用 adminReview API，报告状态变为 REJECTED，工单退回维修工重新提交
    await reportApi.adminReview(r.id, { approved: false, remark: reason })

    ElMessage.success(`已驳回报告，维修工需重新提交报告`)
    // 更新本地报告状态
    r.status = "REJECTED"
    if (detailTarget.value?.id === r.id) {
      detailTarget.value = { ...detailTarget.value, status: "REJECTED" }
    }
    await loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "驳回操作失败")
  } finally {
    reviewing.value = false
  }
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
.ri-center { display:flex; flex-direction:column; align-items:center; gap:4px; min-width:100px; }
.ri-status { font-size:11px; font-weight:600; padding:2px 10px; border-radius:10px; }
.ri-pending { background:#fffbeb; color:#d97706; }
.ri-dept-approved { background:#e0f2fe; color:#0369a1; }
.ri-archived { background:#f0fdf4; color:#16a34a; }
.ri-rejected { background:#fef2f2; color:#dc2626; }
.ri-materials { font-size:10px; color:#94a3b8; max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.ri-right { display:flex; gap:4px; flex-shrink:0; }
.action-btn { padding:5px 12px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:11px; font-weight:500; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.action-btn:hover { border-color:#2563eb; color:#2563eb; }
.action-btn:disabled { opacity:0.5; cursor:not-allowed; }
.action-approve { color:#16a34a; border-color:#bbf7d0; }
.action-approve:hover { background:#f0fdf4; border-color:#16a34a; }
.action-reject { color:#dc2626; border-color:#fecaca; }
.action-reject:hover { background:#fef2f2; border-color:#dc2626; }
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
.report-img { max-width:100%; border-radius:8px; border:1px solid #f0f2f5; margin-top:4px; cursor:pointer; }
.report-img-grid { display:flex; flex-wrap:wrap; gap:8px; margin-top:4px; }
.report-img-grid .report-img { max-width:240px; max-height:180px; object-fit:cover; margin-top:0; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
.btn-ghost { padding:7px 16px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.btn-success { padding:7px 16px; border:none; border-radius:6px; background:#16a34a; color:#fff; font-size:12px; font-weight:500; cursor:pointer; }
.btn-success:hover { background:#15803d; }
.btn-success:disabled { opacity:0.5; cursor:not-allowed; }
.btn-danger { padding:7px 16px; border:none; border-radius:6px; background:#dc2626; color:#fff; font-size:12px; font-weight:500; cursor:pointer; }
.btn-danger:hover { background:#b91c1c; }
.btn-danger:disabled { opacity:0.5; cursor:not-allowed; }
</style>
