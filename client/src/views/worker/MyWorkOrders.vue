<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>{{ t("mywo.title") }}</h2>
      <div class="header-stats">
        <div class="stat-chip pending">{{ pendingCount }} {{ t("mywo.pending") }}</div>
        <div class="stat-chip progress">{{ inProgressCount }} {{ t("mywo.inProgress") }}</div>
        <div class="stat-chip review">{{ reviewCount }} {{ t("mywo.review") }}</div>
        <div class="stat-chip done">{{ doneCount }} {{ t("mywo.done") }}</div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <el-input v-model="searchQuery" :placeholder="t('wo.searchPlaceholder')" clearable style="width:260px" />
      <el-select v-model="statusFilter" :placeholder="t('common.status')" clearable style="width:160px">
        <el-option :label="t('status.pending')" value="ASSIGNED" />
        <el-option :label="t('status.inProgress')" value="IN_PROGRESS" />
        <el-option :label="t('status.completed')" value="COMPLETED" />
        <el-option :label="t('status.pendingDeptReview')" value="PENDING_DEPT_REVIEW" />
        <el-option :label="t('status.pendingAdminReview')" value="PENDING_ADMIN_REVIEW" />
        <el-option :label="t('status.rejected')" value="REJECTED" />
        <el-option :label="t('status.closed')" value="CLOSED" />
      </el-select>
    </div>

    <!-- Work order list -->
    <div class="work-order-list">
      <div v-for="item in filteredOrders" :key="item.id" class="wo-card">
        <div class="wo-top">
          <span class="wo-title">{{ item.title }}</span>
          <el-tag :type="statusType(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
        </div>
        <div class="wo-meta">
          <span><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg> {{ item.location || t("wo.pendingPosition") }}</span>
          <span v-if="item.departmentCode">{{ t("wo.department") }}: {{ deptLabel(item.departmentCode) }}</span>
          <span>{{ t("wo.createdPrefix") }}{{ formatDate(item.createdAt) }}</span>
        </div>
        <div class="wo-bottom">
          <el-tag v-if="item.severityLevel === 'HIGH'" type="danger" size="small">{{ t("severity.high") }}</el-tag>
          <el-tag v-else-if="item.severityLevel === 'MEDIUM'" type="warning" size="small">{{ t("severity.medium") }}</el-tag>
          <el-tag v-else type="info" size="small">{{ t("severity.low") }}</el-tag>
          <span class="wo-code">{{ item.workOrderCode || '#' + item.id }}</span>
          <div class="wo-actions">
            <button class="action-btn" @click="viewDetail(item)">{{ t("common.detail") }}</button>
            <button class="action-btn action-start" v-if="item.status==='ASSIGNED'" @click="handleStart(item)">{{ t("mywo.startWork") }}</button>
            <button class="action-btn action-complete" v-if="item.status==='IN_PROGRESS'" @click="handleComplete(item)">{{ t("mywo.markDone") }}</button>
            <button class="action-btn action-report" v-if="item.status==='COMPLETED' || item.status==='REJECTED'" @click="goSubmitReport(item)">
              {{ item.status === 'REJECTED' ? t("mywo.resubmitReport") : t("mywo.submitReport") }}
            </button>
          </div>
        </div>
      </div>
      <el-empty v-if="filteredOrders.length === 0" :description="t('mywo.noData')" />
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail=false">
      <div class="modal-card">
        <div class="modal-head"><span>{{ t("wo.detail") }} #{{ detailTarget?.workOrderCode || detailTarget?.id }}</span><button class="modal-close" @click="showDetail=false">x</button></div>
        <div class="modal-body">
          <div v-if="detailLoading" style="text-align:center;padding:30px;color:#94a3b8">{{ t("common.loading") }}</div>
          <template v-else>
            <div class="detail-grid">
              <div><label class="detail-label">{{ t("wo.title") }}</label><div class="detail-val">{{ detailTarget?.title }}</div></div>
              <div><label class="detail-label">{{ t("common.status") }}</label><div class="detail-val"><el-tag :type="statusType(detailTarget?.status || '')" size="small">{{ statusLabel(detailTarget?.status || '') }}</el-tag></div></div>
              <div><label class="detail-label">{{ t("wo.location") }}</label><div class="detail-val">{{ detailTarget?.location || '--' }}</div></div>
              <div><label class="detail-label">{{ t("wo.department") }}</label><div class="detail-val">{{ deptLabel(detailTarget?.departmentCode || '') }}</div></div>
              <div><label class="detail-label">{{ t("wo.level") }}</label><div class="detail-val">
                <el-tag v-if="detailTarget?.severityLevel === 'HIGH'" type="danger" size="small">{{ t("severity.high") }}</el-tag>
                <el-tag v-else-if="detailTarget?.severityLevel === 'MEDIUM'" type="warning" size="small">{{ t("severity.medium") }}</el-tag>
                <el-tag v-else type="info" size="small">{{ t("severity.low") }}</el-tag>
              </div></div>
              <div><label class="detail-label">{{ t("wo.damageType") }}</label><div class="detail-val">{{ damageTypeLabel(detailTarget?.damageType || '') }}</div></div>
              <div><label class="detail-label">{{ t("wo.createdAt") }}</label><div class="detail-val">{{ formatDate(detailTarget?.createdAt) || '--' }}</div></div>
              <div v-if="detailTarget?.description" style="grid-column:1/-1"><label class="detail-label">{{ t("wo.description") }}</label><div class="detail-val">{{ detailTarget.description }}</div></div>
            </div>
            <div v-if="detailTarget?.statusLogs?.length" style="margin-top:20px;border-top:1px solid #f0f2f5;padding-top:16px">
              <label class="detail-label">{{ t("wo.statusFlow") }}</label>
              <div class="status-timeline">
                <div v-for="(log, i) in detailTarget.statusLogs" :key="i" class="timeline-item">
                  <div class="timeline-dot"></div>
                  <div class="timeline-content">
                    <span class="timeline-status">{{ statusLabel(log.toStatus || '') }}</span>
                    <span class="timeline-time">{{ log.operatedAt || '--' }}</span>
                    <span v-if="log.note" class="timeline-note">{{ log.note }}</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showDetail=false">{{ t("common.close") }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue"
import { useRouter } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { workOrderApi } from "@/api"
import type { WorkOrderResponse } from "@/types"
import { ElMessage, ElMessageBox } from "element-plus"
import { t } from "@/i18n"

const router = useRouter()
const authStore = useAuthStore()
const searchQuery = ref("")
const statusFilter = ref("")
const orders = ref<WorkOrderResponse[]>([])
const loading = ref(false)

const showDetail = ref(false)
const detailLoading = ref(false)
const detailTarget = ref<WorkOrderResponse | null>(null)

const pendingCount = computed(() => orders.value.filter(o => o.status === "ASSIGNED").length)
const inProgressCount = computed(() => orders.value.filter(o => o.status === "IN_PROGRESS").length)
const reviewCount = computed(() => orders.value.filter(o => o.status === "PENDING_DEPT_REVIEW" || o.status === "PENDING_ADMIN_REVIEW").length)
const doneCount = computed(() => orders.value.filter(o => o.status === "CLOSED").length)

const filteredOrders = computed(() => {
  return orders.value.filter(o => {
    if (searchQuery.value && !o.title?.includes(searchQuery.value) && !o.location?.includes(searchQuery.value)) return false
    if (statusFilter.value && o.status !== statusFilter.value) return false
    return true
  })
})

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await workOrderApi.list({
      assignee: authStore.realName || authStore.username,
    } as any)
    orders.value = res.data.data.records || res.data.data || []
  } catch {
    // Fallback: load all and filter client-side
    try {
      const res = await workOrderApi.list({} as any)
      const all = res.data.data.records || res.data.data || []
      const myName = authStore.realName || authStore.username
      orders.value = all.filter(o => o.assignee === myName)
    } catch {
      orders.value = []
    }
  }
  loading.value = false
}

function statusType(s: string) {
  const map: Record<string, string> = {
    PENDING_ASSIGNMENT: "info", ASSIGNED: "warning", IN_PROGRESS: "primary",
    COMPLETED: "success", PENDING_DEPT_REVIEW: "warning",
    PENDING_ADMIN_REVIEW: "warning", REJECTED: "danger",
    CLOSED: "info", CANCELLED: "info",
  }
  return map[s] || "info"
}

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING_ASSIGNMENT: t("status.pendingAssignment"),
    ASSIGNED: t("status.pending"),
    IN_PROGRESS: t("status.inProgress"),
    COMPLETED: t("status.completed"),
    PENDING_DEPT_REVIEW: t("status.pendingDeptReview"),
    PENDING_ADMIN_REVIEW: t("status.pendingAdminReview"),
    REJECTED: t("status.rejected"),
    CLOSED: t("status.closed"),
    CANCELLED: t("status.cancelled"),
  }
  return map[s] || s
}

function deptLabel(code: string) {
  return ({ ROAD_ADMIN: t("user.roleRoadAdmin"), SANITATION: t("user.roleSanitAdmin"), TRAFFIC_POLICE: t("user.roleTrafficAdmin") } as any)[code] || code || "--"
}

function damageTypeLabel(t2: string) {
  return ({
    CRACK: t("damage.crack"),
    TRANSVERSE_CRACK: t("damage.transverseCrack"),
    LONGITUDINAL_CRACK: t("damage.longitudinalCrack"),
    NET_CRACK: t("damage.netCrack"),
    POTHOLE: t("damage.pothole"),
    MARKING_DAMAGE: t("damage.markingDamage"),
    ROAD_SPILL: t("damage.roadSpill"),
    UNKNOWN: t("damage.unknown")
  } as any)[t2] || t2 || "--"
}

function formatDate(dt?: string): string {
  if (!dt) return ""
  try {
    return new Date(dt).toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
  } catch { return dt || "" }
}

async function viewDetail(row: WorkOrderResponse) {
  showDetail.value = true
  detailLoading.value = true
  try {
    const r = await workOrderApi.get(row.id)
    detailTarget.value = r.data.data
  } catch {
    detailTarget.value = row
  }
  detailLoading.value = false
}

async function handleStart(row: WorkOrderResponse) {
  try {
    await ElMessageBox.confirm(t("mywo.startConfirm"), t("mywo.startTitle"), { type: "info" })
    await workOrderApi.updateStatus(row.id, { status: "IN_PROGRESS" } as any)
    ElMessage.success(t("mywo.started"))
    await loadData()
  } catch { /* cancelled */ }
}

async function handleComplete(row: WorkOrderResponse) {
  try {
    await ElMessageBox.confirm(t("mywo.completeConfirm"), t("mywo.completeTitle"), { type: "warning" })
    await workOrderApi.updateStatus(row.id, { status: "COMPLETED" } as any)
    ElMessage.success(t("mywo.completedHint"))
    await loadData()
  } catch { /* cancelled */ }
}

function goSubmitReport(row: WorkOrderResponse) {
  // Navigate to submit report page with work order id
  router.push({ path: "/submit-report", query: { workOrderId: String(row.id) } })
}
</script>

<style scoped>
.worker-page { max-width: 1000px; margin: 0 auto; font-family: Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 18px; font-weight: 700; color: #1a202c; margin: 0; }
.header-stats { display: flex; gap: 8px; }
.stat-chip { padding: 4px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.stat-chip.pending { background: #fef3c7; color: #d97706; }
.stat-chip.progress { background: #dbeafe; color: #2563eb; }
.stat-chip.review { background: #fef2f2; color: #dc2626; }
.stat-chip.done { background: #d1fae5; color: #059669; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.work-order-list { display: flex; flex-direction: column; gap: 12px; }
.wo-card { background: #fff; border: 1px solid #eef0f4; border-radius: 12px; padding: 16px; transition: all 0.15s; }
.wo-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); border-color: #cbd5e1; }
.wo-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.wo-title { font-size: 14px; font-weight: 600; color: #1a202c; }
.wo-meta { display: flex; gap: 16px; font-size: 12px; color: #64748b; margin-bottom: 8px; flex-wrap: wrap; }
.wo-meta span { display: flex; align-items: center; gap: 4px; }
.wo-bottom { display: flex; align-items: center; gap: 10px; }
.wo-code { font-family: monospace; font-size: 11px; color: #94a3b8; }
.wo-time { font-size: 11px; color: #94a3b8; margin-left: auto; }
.wo-actions { display: flex; gap: 6px; margin-left: auto; }
.action-btn { padding: 5px 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: #fff; font-size: 12px; color: #475569; cursor: pointer; font-family: inherit; transition: all 0.15s; white-space: nowrap; }
.action-btn:hover { border-color: #2563eb; color: #2563eb; }
.action-start { border-color: #2563eb; color: #2563eb; }
.action-start:hover { background: #eef2ff; }
.action-complete { border-color: #059669; color: #059669; }
.action-complete:hover { background: #f0fdf4; }
.action-report { border-color: #d97706; color: #d97706; }
.action-report:hover { background: #fffbeb; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:540px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-close:hover { background:#e2e8f0; color:#475569; }
.modal-body { padding:20px; }
.detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.detail-label { font-size:11px; color:#64748b; font-weight:600; display:block; }
.detail-val { font-size:13px; color:#1e293b; margin-top:4px; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
.btn-ghost { padding:6px 14px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.status-timeline { position:relative; padding-left:8px; margin-top:10px; }
.timeline-item { position:relative; padding:0 0 16px 20px; border-left:2px solid #e2e8f0; }
.timeline-item:last-child { border-left-color:transparent; padding-bottom:0; }
.timeline-dot { position:absolute; left:-5px; top:2px; width:8px; height:8px; border-radius:50%; background:#2563eb; border:2px solid #fff; box-shadow:0 0 0 1px #2563eb; }
.timeline-content { display:flex; flex-direction:column; gap:2px; }
.timeline-status { font-size:12px; font-weight:600; color:#1e293b; }
.timeline-time { font-size:11px; color:#94a3b8; }
.timeline-note { font-size:11px; color:#64748b; }
</style>
