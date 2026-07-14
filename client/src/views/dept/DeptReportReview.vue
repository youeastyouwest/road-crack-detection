<template>
  <div class="dr-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">报告审核</h2>
        <p class="page-desc">审核维修工提交的维修报告，通过后转交系统管理员终审</p>
      </div>
      <div class="tab-bar">
        <button :class="['tab-btn', activeTab==='pending'?'active':'']" @click="activeTab='pending'">
          待审核 <span class="tab-badge">{{ pendingReports.length }}</span>
        </button>
        <button :class="['tab-btn', activeTab==='approved'?'active':'']" @click="activeTab='approved'">
          已通过 <span class="tab-badge">{{ approvedReports.length }}</span>
        </button>
        <button :class="['tab-btn', activeTab==='rejected'?'active':'']" @click="activeTab='rejected'">
          已驳回 <span class="tab-badge">{{ rejectedReports.length }}</span>
        </button>
        <button :class="['tab-btn', activeTab==='all'?'active':'']" @click="activeTab='all'">
          全部 <span class="tab-badge">{{ reports.length }}</span>
        </button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></div><div><span class="stat-val">{{ reports.length }}</span><span class="stat-lbl">总报告数</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef3c7;color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg></div><div><span class="stat-val">{{ pendingReports.length }}</span><span class="stat-lbl">待审核</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ approvedReports.length }}</span><span class="stat-lbl">已通过</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef2f2;color:#dc2626"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></div><div><span class="stat-val">{{ rejectedReports.length }}</span><span class="stat-lbl">已驳回</span></div></div>
    </div>

    <div class="content-card">
      <div class="card-head">
        <div class="card-head-left">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          {{ activeTab === 'pending' ? '待审核报告' : activeTab === 'approved' ? '已通过报告' : activeTab === 'rejected' ? '已驳回报告' : '全部报告' }}
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
            <button v-if="r.status === 'PENDING'" class="action-btn action-approve" @click="openReview(r, true)" :disabled="reviewing">通过</button>
            <button v-if="r.status === 'PENDING'" class="action-btn action-reject" @click="openReview(r, false)" :disabled="reviewing">驳回</button>
          </div>
        </div>
        <div v-if="!loading && displayReports.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          <span>暂无{{ activeTab==='pending'?'待审核':'' }}报告</span>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
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
              <div class="detail-item"><label>执行人</label><span>{{ detailTarget.executor }}</span></div>
              <div class="detail-item"><label>完成时间</label><span>{{ formatDate(detailTarget.finishedAt) }}</span></div>
              <div class="detail-item"><label>提交时间</label><span>{{ formatDate(detailTarget.createdAt) }}</span></div>
              <div class="detail-item"><label>使用材料</label><span>{{ detailTarget.materials || '--' }}</span></div>
              <div class="detail-item" v-if="detailTarget.reviewer"><label>审核人</label><span>{{ detailTarget.reviewer }}</span></div>
              <div class="detail-item" v-if="detailTarget.reviewedAt"><label>审核时间</label><span>{{ formatDate(detailTarget.reviewedAt) }}</span></div>
              <div class="detail-item" style="grid-column:1/-1"><label>维修描述</label><span style="line-height:1.6">{{ detailTarget.description || '--' }}</span></div>
              <div class="detail-item" v-if="detailTarget.reviewRemark" style="grid-column:1/-1">
                <label>审核意见</label>
                <span style="line-height:1.6" :style="{color: detailTarget.status === 'REJECTED' ? '#dc2626' : '#475569'}">{{ detailTarget.reviewRemark }}</span>
              </div>
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
          <button v-if="detailTarget && detailTarget.status === 'PENDING'" class="btn-success" @click="openReview(detailTarget, true)" :disabled="reviewing">通过审核</button>
          <button v-if="detailTarget && detailTarget.status === 'PENDING'" class="btn-danger" @click="openReview(detailTarget, false)" :disabled="reviewing">驳回</button>
        </div>
      </div>
    </div>

    <!-- Review Modal -->
    <div v-if="showReview" class="modal-overlay" @click.self="showReview=false">
      <div class="modal-card" style="width:520px">
        <div class="modal-head">
          <span>{{ reviewForm.approved ? '通过审核' : '驳回报告' }} - {{ reviewTarget?.reportCode || '报告#' + reviewTarget?.id }}</span>
          <button class="modal-close" @click="showReview=false">✕</button>
        </div>
        <div class="modal-body">
          <div class="review-info" v-if="reviewTarget">
            <div class="review-info-row">
              <span class="ri-label">关联工单</span>
              <span class="ri-val">#{{ reviewTarget.workOrderId }} {{ getWorkOrderTitle(reviewTarget.workOrderId) }}</span>
            </div>
            <div class="review-info-row">
              <span class="ri-label">执行人</span>
              <span class="ri-val">{{ reviewTarget.executor }}</span>
            </div>
            <div class="review-info-row">
              <span class="ri-label">完成时间</span>
              <span class="ri-val">{{ formatDate(reviewTarget.finishedAt) }}</span>
            </div>
            <div class="review-info-row" v-if="reviewTarget.description">
              <span class="ri-label">维修描述</span>
              <span class="ri-val" style="line-height:1.5">{{ reviewTarget.description }}</span>
            </div>
          </div>
          <div class="form-group" style="margin-top:16px">
            <label>{{ reviewForm.approved ? '审核备注（可选）' : '驳回原因 *' }}</label>
            <textarea v-model="reviewForm.remark" rows="3" :placeholder="reviewForm.approved ? '审核备注...' : '请说明驳回原因...'"></textarea>
          </div>
          <div class="review-hint" v-if="reviewForm.approved">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#0369a1" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            <span>通过后报告将转交系统管理员进行终审</span>
          </div>
          <div class="review-hint hint-danger" v-else>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>驳回后报告将退回维修工重新提交</span>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showReview=false">取消</button>
          <button :class="reviewForm.approved ? 'btn-success' : 'btn-danger'" @click="confirmReview" :disabled="reviewing || (!reviewForm.approved && !reviewForm.remark)">
            {{ reviewing ? '提交中...' : reviewForm.approved ? '确认通过' : '确认驳回' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { reportApi, workOrderApi } from "@/api"
import { useAuthStore } from "@/stores/auth"
import type { MaintenanceReportResponse, WorkOrderResponse } from "@/types"
import { splitReportImageUrls } from "@/utils/reportImages"

const authStore = useAuthStore()
const activeTab = ref("pending")
const reports = ref<MaintenanceReportResponse[]>([])
const workOrderMap = ref<Map<number, WorkOrderResponse>>(new Map())
const loading = ref(false)
const showDetail = ref(false)
const detailTarget = ref<MaintenanceReportResponse | null>(null)
const detailLoading = ref(false)

// Review modal
const showReview = ref(false)
const reviewTarget = ref<MaintenanceReportResponse | null>(null)
const reviewing = ref(false)
const reviewForm = reactive({ approved: true, remark: "" })

const pendingReports = computed(() => reports.value.filter(r => r.status === "PENDING"))
const approvedReports = computed(() => reports.value.filter(r => r.status === "DEPT_APPROVED" || r.status === "APPROVED"))
const rejectedReports = computed(() => reports.value.filter(r => r.status === "REJECTED"))
const displayReports = computed(() => {
  if (activeTab.value === "pending") return pendingReports.value
  if (activeTab.value === "approved") return approvedReports.value
  if (activeTab.value === "rejected") return rejectedReports.value
  return reports.value
})

function getWorkOrderTitle(workOrderId: number): string {
  const wo = workOrderMap.value.get(workOrderId)
  return wo?.title || `工单 #${workOrderId}`
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
    PENDING: "待审核",
    DEPT_APPROVED: "已通过，待终审",
    APPROVED: "终审已通过",
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
  return splitReportImageUrls(urlStr)
}

function previewImage(url: string) {
  window.open(url, "_blank")
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
    const [reportRes, workOrderRes] = await Promise.all([
      reportApi.list({ page: 1, size: 100 }),
      workOrderApi.list({ page: 1, size: 200, departmentCode: authStore.deptCode } as any),
    ])
    reports.value = reportRes.data.data.records || []

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

function openReview(r: MaintenanceReportResponse, approved: boolean) {
  reviewTarget.value = r
  reviewForm.approved = approved
  reviewForm.remark = ""
  showReview.value = true
}

async function confirmReview() {
  if (!reviewTarget.value) return
  if (!reviewForm.approved && !reviewForm.remark) {
    ElMessage.warning("驳回时请填写原因")
    return
  }

  reviewing.value = true
  try {
    await reportApi.deptReview(reviewTarget.value.id, {
      approved: reviewForm.approved,
      remark: reviewForm.remark || undefined,
    })

    ElMessage.success(reviewForm.approved ? "审核已通过，报告已转交系统管理员终审" : "已驳回，维修工需重新提交报告")
    showReview.value = false
    // 更新本地状态
    reviewTarget.value.status = reviewForm.approved ? "DEPT_APPROVED" : "REJECTED"
    await loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "审核操作失败")
  } finally {
    reviewing.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dr-page { font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
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
.ri-center { display:flex; flex-direction:column; align-items:center; gap:4px; min-width:120px; }
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

/* Review Modal */
.review-info { background:#f8f9fc; border:1px solid #eef0f4; border-radius:8px; padding:14px 16px; }
.review-info-row { display:flex; gap:12px; margin-bottom:8px; }
.review-info-row:last-child { margin-bottom:0; }
.ri-label { font-size:10px; color:#94a3b8; font-weight:600; text-transform:uppercase; letter-spacing:0.03em; min-width:70px; padding-top:2px; }
.ri-val { font-size:13px; color:#475569; flex:1; }
.form-group { display:flex; flex-direction:column; gap:4px; }
.form-group label { font-size:11px; font-weight:600; color:#64748b; }
.form-group textarea { padding:8px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; font-family:inherit; color:#1e293b; outline:none; background:#fff; resize:vertical; min-height:60px; }
.form-group textarea:focus { border-color:#2563eb; }
.review-hint { display:flex; align-items:center; gap:8px; margin-top:12px; padding:10px 14px; background:#e0f2fe; border:1px solid #bae6fd; border-radius:8px; font-size:12px; color:#0369a1; }
.review-hint.hint-danger { background:#fef2f2; border-color:#fecaca; color:#dc2626; }
</style>
