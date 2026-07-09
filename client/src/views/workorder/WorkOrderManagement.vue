<template>
  <div class="wo-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">工单管理</h2>
        <p class="page-desc">管理检测工单的创建、指派与流转</p>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div><span class="stat-val">{{ stats.pending }}</span><span class="stat-lbl">待指派</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef3c7;color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg></div><div><span class="stat-val">{{ stats.inProgress }}</span><span class="stat-lbl">进行中</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ stats.completed }}</span><span class="stat-lbl">已完成</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fce7f3;color:#e11d48"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-6"/></svg></div><div><span class="stat-val">{{ stats.todayNew }}</span><span class="stat-lbl">今日新增</span></div></div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="filter-group">
            <select v-model="filter.status" class="filter-select"><option value="">全部状态</option><option value="PENDING_ASSIGNMENT">待指派</option><option value="ASSIGNED">已指派</option><option value="IN_PROGRESS">进行中</option><option value="COMPLETED">已完成</option></select>
            <select v-model="filter.departmentCode" class="filter-select"><option value="">待指派</option><option value="ROAD_ADMIN">进行中</option><option value="SANITATION">已指派</option><option value="TRAFFIC_POLICE">已完成</option>
              <option value="CANCELLED">已取消</option>
            </select>
            <select v-model="filter.severityLevel" class="filter-select"><option value="">全部等级</option><option value="LOW">轻微</option><option value="MEDIUM">中等</option><option value="HIGH">严重</option></select>
            <div class="search-wrap"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="filter.keyword" placeholder="搜索工单编号、标题..." @keyup.enter="loadData" /></div>
            <button class="btn-ghost" @click="loadData">搜索</button>
          </div>
        </div>
        <button class="btn-primary" @click="showCreate=true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>新建工单</button>
      </div>

      <div class="table-wrap">
        <table class="ds-table">
          <thead><tr><th>编号</th><th>标题</th><th>位置</th><th>等级</th><th>状态</th><th>指派人</th><th style="width:160px">操作</th></tr></thead>
          <tbody>
            <tr v-for="row in orders" :key="row.id">
              <td><span class="code-tag">{{ row.workOrderCode }}</span></td>
              <td class="td-title">{{ row.title }}</td>
              <td class="td-muted">{{ row.location }}</td>
              <td><span :class="['sev-badge', row.severityLevel === 'HIGH' ? 'sev-high' : row.severityLevel === 'MEDIUM' ? 'sev-med' : 'sev-low']">{{ {LOW:'轻微',MEDIUM:'中等',HIGH:'严重'}[row.severityLevel] }}</span></td>
              <td class="td-dept">{{ deptLabel(row.departmentCode) }}</td>
              <td><span :class="['status-tag', statusCls(row.status)]">{{ statusLabel(row.status) }}</span></td>
              <td class="td-muted">{{ row.assignee || '--' }}</td>
              <td><div class="action-group"><button class="action-btn" @click="viewDetail(row)">详情</button><button class="action-btn" v-if="row.status==='PENDING_ASSIGNMENT'" @click="openAssign(row)">指派</button><button class="action-btn action-danger" v-if="row.status!=='CLOSED'&&row.status!=='CANCELLED'" @click="handleCancel(row)">取消</button></div></td>
            </tr>
            <tr v-if="!loading && orders.length === 0"><td colspan="8" class="empty-row">暂无工单数据</td></tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span class="page-info">共 {{ total }} 条</span>
        <div class="page-btns">
          <button class="page-btn" :disabled="page<=1" @click="page--;loadData()">上一页</button>
          <span class="page-cur">{{ page }}</span>
          <button class="page-btn" :disabled="page*10>=total" @click="page++;loadData()">下一页</button>
        </div>
      </div>
    </div>

    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate=false">
      <div class="modal-card">
        <div class="modal-head"><span>新建工单</span><button class="modal-close" @click="showCreate=false">✕</button></div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group"><label>标题</label><input v-model="createForm.title" placeholder="请输入工单标题" /></div>
            <div class="form-group"><label>病害类型</label><select v-model="createForm.damageType"><option value="CRACK">裂缝</option><option value="MARKING_DAMAGE">标线损坏</option><option value="ROAD_SPILL">路面抛洒</option><option value="POTHOLE">坑槽</option></select></div>
            <div class="form-group"><label>严重程度</label><select v-model="createForm.severityLevel"><option value="LOW">轻微</option><option value="MEDIUM">中等</option><option value="HIGH">严重</option></select></div>
            <div class="form-group"><label>位置</label><input v-model="createForm.location" placeholder="如：G102 K15+300" /></div>
            <div class="form-group"><label>负责部门</label><select v-model="createForm.departmentCode"><option value="ROAD_ADMIN">道路管理</option><option value="SANITATION">环卫</option><option value="TRAFFIC_POLICE">交警</option></select></div>
            <div class="form-group"><label>关联检测任务 ID</label><input v-model.number="createForm.detectionTaskId" type="number" min="1" /></div>
          </div>
          <div class="form-group" style="margin-top:12px"><label>描述</label><textarea v-model="createForm.description" rows="3" placeholder="请输入工单描述..."></textarea></div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showCreate=false">取消</button>
          <button class="btn-primary" :disabled="creating" @click="handleCreate">{{ creating ? '提交中...' : '提交' }}</button>
        </div>
      </div>
    </div>
  </div>

    <!-- Assign Modal -->
    <div v-if="showAssign" class="modal-overlay" @click.self="showAssign=false">
      <div class="modal-card">
        <div class="modal-head"><span>分配工单 #{{ assignTarget?.workOrderCode || assignTarget?.id }}</span><button class="modal-close" @click="showAssign=false">✕</button></div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group">
              <label>工单标题</label>
              <input :value="assignTarget?.title" disabled style="background:#f8f9fc" />
            </div>
            <div class="form-group">
              <label>当前状态</label>
              <input :value="statusLabel(assignTarget?.status || '')" disabled style="background:#f8f9fc" />
            </div>
            <div class="form-group">
              <label>目标部门</label>
              <select v-model="assignForm.departmentCode" @change="assignForm.assignee=''">
                <option v-for="d in deptOptions" :key="d.value" :value="d.value">{{ d.label }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>指派人</label>
              <select v-model="assignForm.assignee">
                <option value="">请选择</option>
                <option v-for="w in availableWorkers" :key="w.name" :value="w.name">{{ w.name }}（{{ w.dept }}）</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showAssign=false">取消</button>
          <button class="btn-primary" :disabled="assigning" @click="confirmAssign">{{ assigning ? '指派中...' : '确认指派' }}</button>
        </div>
      </div>
    </div>


    <!-- Detail Modal -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail=false">
      <div class="modal-card">
        <div class="modal-head"><span>工单详情 #{{ detailTarget?.workOrderCode || detailTarget?.id }}</span><button class="modal-close" @click="showDetail=false">✕</button></div>
        <div class="modal-body">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
            <div><label style="font-size:11px;color:#64748b;font-weight:600">标题</label><div style="font-size:13px;color:#1e293b;margin-top:4px">{{ detailTarget?.title }}</div></div>
            <div><label style="font-size:11px;color:#64748b;font-weight:600">负责部门</label><div style="font-size:13px;color:#1e293b;margin-top:4px">{{ deptLabel(detailTarget?.departmentCode || '') }}</div></div>
            <div><label style="font-size:11px;color:#64748b;font-weight:600">状态</label><div style="font-size:13px;color:#1e293b;margin-top:4px">{{ statusLabel(detailTarget?.status || '') }}</div></div>
            <div><label style="font-size:11px;color:#64748b;font-weight:600">等级</label><div style="font-size:13px;color:#1e293b;margin-top:4px">{{ {LOW:'轻微',MEDIUM:'中等',HIGH:'严重'}[detailTarget?.severityLevel || ''] || detailTarget?.severityLevel }}</div></div>
            <div><label style="font-size:11px;color:#64748b;font-weight:600">位置</label><div style="font-size:13px;color:#1e293b;margin-top:4px">{{ detailTarget?.location || '--' }}</div></div>
            <div><label style="font-size:11px;color:#64748b;font-weight:600">指派人</label><div style="font-size:13px;color:#1e293b;margin-top:4px">{{ detailTarget?.assignee || '--' }}</div></div>
            <div style="grid-column:1/-1"><label style="font-size:11px;color:#64748b;font-weight:600">创建时间</label><div style="font-size:13px;color:#1e293b;margin-top:4px">{{ detailTarget?.createdAt }}</div></div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-primary" @click="showDetail=false">关闭</button>
        </div>
      </div>
    </div>

  </template><script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { workOrderApi } from "@/api"
import type { WorkOrderResponse } from "@/types"

const orders = ref<WorkOrderResponse[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const showCreate = ref(false)
const showAssign = ref(false)
const detailTarget = ref<WorkOrderResponse | null>(null)
const showDetail = ref(false)
const assignTarget = ref<WorkOrderResponse | null>(null)
const creating = ref(false)
const assigning = ref(false)

const filter = reactive({ status: "", severityLevel: "", keyword: "", departmentCode: "" })
const createForm = reactive({
  title: "", damageType: "TRANSVERSE_CRACK", severityLevel: "MEDIUM",
  location: "", departmentCode: "ROAD_ADMIN", description: ""
})
const assignForm = reactive({ departmentCode: "ROAD_ADMIN", assignee: "" })
const stats = reactive({ pending: 0, inProgress: 0, completed: 0, todayNew: 0 })

const deptWorkers: Record<string, { name: string; dept: string }[]> = {}
const availableWorkers = computed(() => deptWorkers[assignForm.departmentCode] || [])
const deptOptions = [
  { value: "ROAD_ADMIN", label: "道路管理部" },
  { value: "SANITATION", label: "环卫部" },
  { value: "TRAFFIC_POLICE", label: "交警部" },
]

function statusCls(s: string) {
  return ({ PENDING_ASSIGNMENT: "stat-info", ASSIGNED: "stat-primary", IN_PROGRESS: "stat-warn", COMPLETED: "stat-success" } as any)[s] || "stat-default"
}
function statusLabel(s: string) {
  return ({ PENDING_ASSIGNMENT: "待指派", ASSIGNED: "已指派", IN_PROGRESS: "进行中", COMPLETED: "已完成", CANCELLED: "已取消" } as any)[s] || s
}
function deptLabel(code: string) {
  return ({ ROAD_ADMIN: "道路管理部", SANITATION: "环卫部", TRAFFIC_POLICE: "交警部" } as any)[code] || code
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: 10 }
    if (filter.status) params.status = filter.status
    if (filter.severityLevel) params.severityLevel = filter.severityLevel
    if (filter.departmentCode) params.departmentCode = filter.departmentCode
    if (filter.keyword) params.keyword = filter.keyword
    const r = await workOrderApi.list(params)
    orders.value = r.data.data.records
    total.value = r.data.data.total
  } catch {}
  loading.value = false
}

async function handleCreate() {
  if (!createForm.title) return ElMessage.warning("请输入工单标题")
  creating.value = true
  try {
    await workOrderApi.create(createForm as any)
    ElMessage.success("工单已创建，等待指派")
    showCreate.value = false
    await loadData()
  } catch { ElMessage.error("创建失败") }
  finally { creating.value = false }
}

function openAssign(row: WorkOrderResponse) {
  assignTarget.value = row
  assignForm.departmentCode = row.departmentCode || "ROAD_ADMIN"
  assignForm.assignee = ""
  showAssign.value = true
}

async function confirmAssign() {
  if (!assignForm.assignee) return ElMessage.warning("请选择指派人")
  assigning.value = true
  try {
    await workOrderApi.assign(assignTarget.value!.id, { assignee: assignForm.assignee })
    ElMessage.success("已指派给 " + assignForm.assignee)
    showAssign.value = false
    await loadData()
  } catch { ElMessage.error("指派失败") }
  finally { assigning.value = false }
}

function viewDetail(row: WorkOrderResponse) {
  detailTarget.value = row
  showDetail.value = true
}

async function handleCancel(row: WorkOrderResponse) {
  ElMessageBox.prompt("取消原因", "取消工单", { inputPlaceholder: "请输入原因" })
    .then(async ({ value }) => {
      try {
        await workOrderApi.cancel(row.id, { reason: value });
        orders.value = orders.value.filter(o => o.id !== row.id);
        total.value--;
        ElMessage.success("已取消");
      } catch { ElMessage.error("取消失败") }
    }).catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.wo-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }

.stat-row { display:flex; gap:12px; margin-bottom:16px; }
.stat-card { flex:1; display:flex; align-items:center; gap:14px; padding:14px 18px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.stat-icon { width:38px; height:38px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.stat-val { font-size:22px; font-weight:800; color:#0f172a; line-height:1; }
.stat-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }

.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.toolbar { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f0f2f5; gap:12px; }
.toolbar-left { flex:1; }
.filter-group { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.filter-select { padding:6px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; color:#334155; background:#fff; font-family:inherit; outline:none; cursor:pointer; }
.filter-select:focus { border-color:#2563eb; }
.search-wrap { display:flex; align-items:center; gap:6px; padding:6px 10px; background:#f8f9fc; border:1px solid #eef0f4; border-radius:8px; }
.search-wrap input { border:none; background:transparent; outline:none; font-size:12px; color:#1e293b; width:160px; font-family:inherit; }
.search-wrap input::placeholder { color:#94a3b8; }
.btn-ghost { padding:6px 14px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.btn-primary { display:inline-flex; align-items:center; gap:6px; padding:7px 16px; border:none; border-radius:6px; background:#2563eb; color:#fff; font-size:12px; font-weight:500; cursor:pointer; font-family:inherit; transition:background .15s; white-space:nowrap; }
.btn-primary:hover { background:#1d4ed8; }
.btn-primary:disabled { background:#93c5fd; cursor:not-allowed; }

.table-wrap { overflow-x:auto; }
.ds-table { width:100%; border-collapse:collapse; font-size:13px; }
.ds-table thead { background:#f8f9fc; }
.ds-table th { padding:10px 14px; text-align:left; font-weight:600; font-size:11px; color:#64748b; letter-spacing:0.03em; border-bottom:1px solid #eef0f4; }
.ds-table td { padding:10px 14px; border-bottom:1px solid #f5f6f8; color:#1e293b; }
.ds-table tbody tr:hover { background:#fafbfc; }
.td-title { font-weight:500; }
.td-muted { color:#94a3b8; font-size:12px; }
.td-dept { font-size:12px; color:#4361ee; font-weight:500; }
.code-tag { font-family:monospace; font-size:12px; color:#2563eb; font-weight:600; }
.sev-badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.sev-high { background:#fef2f2; color:#dc2626; }
.sev-med { background:#fffbeb; color:#d97706; }
.sev-low { background:#f0fdf4; color:#16a34a; }
.status-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.stat-info { background:#f1f5f9; color:#64748b; }
.stat-primary { background:#eef2ff; color:#4361ee; }
.stat-warn { background:#fffbeb; color:#d97706; }
.stat-success { background:#f0fdf4; color:#16a34a; }
.stat-default { background:#f8f9fc; color:#94a3b8; }
.action-group { display:flex; gap:4px; }
.action-btn { padding:4px 10px; border:1px solid #e2e8f0; border-radius:5px; background:#fff; font-size:11px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; }
.action-btn:hover { border-color:#2563eb; color:#2563eb; }
.action-danger:hover { border-color:#ef4444; color:#ef4444; }
.empty-row { text-align:center; color:#94a3b8; padding:40px 0 !important; font-size:13px; }

.pagination { display:flex; align-items:center; justify-content:space-between; padding:12px 18px; border-top:1px solid #f0f2f5; }
.page-info { font-size:12px; color:#94a3b8; }
.page-btns { display:flex; align-items:center; gap:6px; }
.page-btn { padding:4px 10px; border:1px solid #e2e8f0; border-radius:5px; background:#fff; font-size:11px; color:#475569; cursor:pointer; font-family:inherit; }
.page-btn:hover:not(:disabled) { border-color:#2563eb; color:#2563eb; }
.page-btn:disabled { opacity:.4; cursor:not-allowed; }
.page-cur { font-size:12px; font-weight:600; color:#2563eb; min-width:20px; text-align:center; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:540px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-close:hover { background:#e2e8f0; color:#475569; }
.modal-body { padding:20px; }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.form-group { display:flex; flex-direction:column; gap:4px; }
.form-group label { font-size:11px; font-weight:600; color:#64748b; }
.form-group input,.form-group select,.form-group textarea { padding:8px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; font-family:inherit; color:#1e293b; outline:none; background:#fff; }
.form-group input:focus,.form-group select:focus,.form-group textarea:focus { border-color:#2563eb; }
.form-group textarea { resize:vertical; min-height:60px; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
</style>