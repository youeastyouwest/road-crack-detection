<template>
  <div class="mr-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">维修报告</h2>
        <p class="page-desc">审核工人提交的维修报告，归档已确认的报告</p>
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
          <span class="toolbar-info">{{ displayReports.length }} 条记录</span>
        </div>
      </div>

      <div class="report-list">
        <div v-for="r in displayReports" :key="r.id" class="report-item">
          <div class="ri-left">
            <div class="ri-code">{{ r.reportCode }}</div>
            <div class="ri-info">
              <span class="ri-road">{{ r.roadName || r.workOrderTitle || '工单 #'+r.workOrderId }}</span>
              <span class="ri-meta">{{ r.executor }} · {{ r.repairMethod || '--' }} · {{ r.finishedAt }}</span>
            </div>
          </div>
          <div class="ri-center">
            <span :class="['ri-status', r.status==='待审核'?'ri-pending':'ri-archived']">{{ r.status }}</span>
            <div class="ri-materials" v-if="r.materials">材料: {{ r.materials }}</div>
          </div>
          <div class="ri-right">
            <button class="action-btn" @click="viewDetail(r)">详情</button>
            <button v-if="r.status==='待审核'" class="action-btn action-approve" @click="approveReport(r)">通过</button>
            <button v-if="r.status==='待审核'" class="action-btn action-reject" @click="rejectReport(r)">拒绝</button>
          </div>
        </div>
        <div v-if="displayReports.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          <span>暂无{{ activeTab==='pending'?'待审核':'' }}报告</span>
        </div>
      </div>
    </div>

    <!-- Detail/Review Modal -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail=false">
      <div class="modal-card">
        <div class="modal-head"><span>报告详情 - {{ detailTarget?.reportCode }}</span><button class="modal-close" @click="showDetail=false">✕</button></div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item"><label>报告编号</label><span>{{ detailTarget?.reportCode }}</span></div>
            <div class="detail-item"><label>状态</label><span :style="{color:detailTarget?.status==='待审核'?'#d97706':'#16a34a',fontWeight:600}">{{ detailTarget?.status }}</span></div>
            <div class="detail-item"><label>关联工单</label><span>#{{ detailTarget?.workOrderId }}</span></div>
            <div class="detail-item"><label>执行人</label><span>{{ detailTarget?.executor }}</span></div>
            <div class="detail-item"><label>维修方法</label><span>{{ detailTarget?.repairMethod || '--' }}</span></div>
            <div class="detail-item"><label>完成时间</label><span>{{ detailTarget?.finishedAt }}</span></div>
            <div class="detail-item" style="grid-column:1/-1"><label>使用材料</label><span>{{ detailTarget?.materials || '--' }}</span></div>
            <div class="detail-item" style="grid-column:1/-1"><label>维修描述</label><span style="line-height:1.6">{{ detailTarget?.description || '--' }}</span></div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showDetail=false">关闭</button>
          <button v-if="detailTarget?.status==='待审核'" class="btn-success" @click="approveReport(detailTarget)">通过审核</button>
          <button v-if="detailTarget?.status==='待审核'" class="btn-danger" @click="rejectReport(detailTarget)">拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"

const activeTab = ref("pending")
const reports = ref<any[]>([])
const loading = ref(false)
const showDetail = ref(false)
const detailTarget = ref<any>(null)

const pendingReports = computed(() => reports.value.filter(r => r.status === "待审核"))
const archivedReports = computed(() => reports.value.filter(r => r.status === "已归档"))
const displayReports = computed(() => {
  if (activeTab.value === "pending") return pendingReports.value
  if (activeTab.value === "archived") return archivedReports.value
  return reports.value
})
const thisMonth = computed(() => {
  const m = new Date().getMonth()
  return reports.value.filter(r => r.finishedAt && new Date(r.finishedAt).getMonth() === m).length
})

onMounted(() => {
  reports.value = []
})

function viewDetail(r: any) {
  detailTarget.value = r
  showDetail.value = true
}

function approveReport(r: any) {
  r.status = "已归档"
  ElMessage.success("已通过审核，报告已归档")
}

function rejectReport(r: any) {
  ElMessageBox.prompt("拒绝原因", "拒绝报告", { inputPlaceholder: "请输入拒绝原因" })
    .then(({ value }) => {
      reports.value = reports.value.filter(re => re.id !== r.id)
      ElMessage.success("已拒绝：" + value)
    }).catch(() => {})
}
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

.report-list { display:flex; flex-direction:column; }
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
.ri-archived { background:#f0fdf4; color:#16a34a; }
.ri-materials { font-size:10px; color:#94a3b8; }
.ri-right { display:flex; gap:4px; flex-shrink:0; }
.action-btn { padding:5px 12px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:11px; font-weight:500; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.action-btn:hover { border-color:#2563eb; color:#2563eb; }
.action-approve { color:#16a34a; border-color:#bbf7d0; }
.action-approve:hover { background:#f0fdf4; border-color:#16a34a; }
.action-reject { color:#dc2626; border-color:#fecaca; }
.action-reject:hover { background:#fef2f2; border-color:#dc2626; }
.empty-state { display:flex; flex-direction:column; align-items:center; gap:10px; padding:60px 0; color:#94a3b8; font-size:13px; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:540px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-body { padding:20px; }
.detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
.detail-item { display:flex; flex-direction:column; gap:4px; }
.detail-item label { font-size:11px; font-weight:600; color:#64748b; }
.detail-item span { font-size:13px; color:#0f172a; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
.btn-ghost { padding:7px 16px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.btn-success { padding:7px 16px; border:none; border-radius:6px; background:#16a34a; color:#fff; font-size:12px; font-weight:500; cursor:pointer; }
.btn-success:hover { background:#15803d; }
.btn-danger { padding:7px 16px; border:none; border-radius:6px; background:#dc2626; color:#fff; font-size:12px; font-weight:500; cursor:pointer; }
.btn-danger:hover { background:#b91c1c; }
</style>