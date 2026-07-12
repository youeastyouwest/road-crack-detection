<template>
  <div class="sys-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">操作日志</h2>
        <p class="page-desc">系统操作审计记录与追踪</p>
      </div>
      <button class="btn-ghost" @click="loadData">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg>刷新
      </button>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg></div><div><span class="stat-val">{{ total }}</span><span class="stat-lbl">日志总数</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ successCount }}</span><span class="stat-lbl">成功操作</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef2f2;color:#dc2626"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></div><div><span class="stat-val">{{ failedCount }}</span><span class="stat-lbl">失败操作</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fce7f3;color:#e11d48"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg></div><div><span class="stat-val">{{ todayCount }}</span><span class="stat-lbl">今日操作</span></div></div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="filter-group">
            <select v-model="filters.module" class="filter-select" @change="loadData">
              <option value="">全部模块</option>
              <option value="USER">用户管理</option>
              <option value="DEPARTMENT">部门管理</option>
              <option value="ROLE">角色管理</option>
              <option value="WORK_ORDER">工单管理</option>
              <option value="DETECTION">检测管理</option>
              <option value="AUTH">认证登录</option>
              <option value="SYSTEM">系统管理</option>
            </select>
            <select v-model="filters.status" class="filter-select" @change="loadData">
              <option value="">全部状态</option>
              <option value="SUCCESS">成功</option>
              <option value="FAILED">失败</option>
            </select>
            <div class="search-wrap"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="filters.keyword" placeholder="搜索操作人或操作内容..." @keyup.enter="loadData" /></div>
            <button class="btn-ghost" @click="resetFilters">重置</button>
          </div>
        </div>
        <button class="btn-ghost" @click="handleExport">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>导出
        </button>
      </div>

      <div class="table-wrap">
        <table class="ds-table">
          <thead><tr><th>操作人</th><th>操作</th><th>模块</th><th>描述</th><th>IP</th><th>耗时</th><th>状态</th><th>时间</th></tr></thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td class="td-title">{{ log.operator || log.username || '--' }}</td>
              <td>{{ log.action || '--' }}</td>
              <td><span class="module-tag">{{ moduleLabel(log.module) }}</span></td>
              <td class="td-muted td-desc" :title="log.description || ''">{{ log.description || '--' }}</td>
              <td class="td-muted" style="font-family:monospace">{{ log.ip || '--' }}</td>
              <td class="td-muted" style="font-family:monospace">{{ log.costTime != null ? log.costTime + 'ms' : (log.duration != null ? log.duration + 'ms' : '--') }}</td>
              <td><span :class="['status-tag', log.status === 'SUCCESS' || log.status === '1' ? 'stat-success' : 'stat-cancel']">{{ log.status === 'SUCCESS' || log.status === '1' ? '成功' : '失败' }}</span></td>
              <td class="td-muted" style="white-space:nowrap">{{ formatTime(log.createdAt) }}</td>
            </tr>
            <tr v-if="!loading && logs.length === 0"><td colspan="8" class="empty-row">暂无日志数据</td></tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span class="page-info">共 {{ total }} 条</span>
        <div class="page-btns">
          <button class="page-btn" :disabled="page<=1" @click="page--;loadData()">上一页</button>
          <span class="page-cur">{{ page }}</span>
          <button class="page-btn" :disabled="page*20>=total" @click="page++;loadData()">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue"
import { ElMessage } from "element-plus"
import http from "@/api/index"
import type { ApiResponse, PageResponse, AuditLogResponse } from "@/types"

const logs = ref<AuditLogResponse[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const filters = reactive({ module: "", status: "", keyword: "" })

const successCount = computed(() => logs.value.filter(l => l.status === 'SUCCESS' || l.status === '1').length)
const failedCount = computed(() => logs.value.filter(l => l.status === 'FAILED' || l.status === '0').length)
const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return logs.value.filter(l => l.createdAt?.startsWith(today)).length
})

function moduleLabel(m?: string) {
  const map: Record<string, string> = {
    USER: "用户管理", DEPARTMENT: "部门管理", ROLE: "角色管理",
    WORK_ORDER: "工单管理", DETECTION: "检测管理", AUTH: "认证登录", SYSTEM: "系统管理",
  }
  return map[m || ""] || m || "--"
}

function formatTime(s?: string) {
  if (!s) return "--"
  return s.slice(0, 19).replace("T", " ")
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: 20 }
    if (filters.module) params.module = filters.module
    if (filters.status) params.status = filters.status
    if (filters.keyword) params.keyword = filters.keyword
    const r = await http.get<ApiResponse<PageResponse<AuditLogResponse>>>("/audit-log/page", { params })
    logs.value = (r.data.data.records || []).map((l: any) => ({
      ...l,
      module: l.module || l.target,
      description: l.description || l.detail,
      username: l.username || l.operator,
      costTime: l.costTime ?? l.duration,
    }))
    total.value = r.data.data.total || 0
  } catch {
    logs.value = []
    total.value = 0
  }
  loading.value = false
}

function resetFilters() {
  filters.module = ""
  filters.status = ""
  filters.keyword = ""
  page.value = 1
  loadData()
}

function handleExport() {
  const headers = ["操作人", "操作", "模块", "描述", "IP", "耗时", "状态", "时间"]
  const rows = logs.value.map(l => [
    l.operator || l.username || "", l.action || "", moduleLabel(l.module),
    l.description || "", l.ip || "",
    l.costTime != null ? l.costTime + 'ms' : (l.duration != null ? l.duration + 'ms' : ''),
    l.status === 'SUCCESS' || l.status === '1' ? '成功' : '失败',
    formatTime(l.createdAt),
  ])
  const csv = [headers, ...rows].map(r => r.map(c => `"${c}"`).join(",")).join("\n")
  const blob = new Blob(["\ufeff" + csv], { type: "text/csv;charset=utf-8;" })
  const link = document.createElement("a")
  link.href = URL.createObjectURL(blob)
  link.download = `操作日志_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success("已导出 " + rows.length + " 条日志")
}

onMounted(loadData)
</script>

<style scoped>
.sys-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
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
.search-wrap input { border:none; background:transparent; outline:none; font-size:12px; color:#1e293b; width:180px; font-family:inherit; }
.search-wrap input::placeholder { color:#94a3b8; }
.btn-ghost { display:inline-flex; align-items:center; gap:4px; padding:6px 14px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }

.table-wrap { overflow-x:auto; }
.ds-table { width:100%; border-collapse:collapse; font-size:13px; }
.ds-table thead { background:#f8f9fc; }
.ds-table th { padding:10px 14px; text-align:left; font-weight:600; font-size:11px; color:#64748b; letter-spacing:0.03em; border-bottom:1px solid #eef0f4; white-space:nowrap; }
.ds-table td { padding:10px 14px; border-bottom:1px solid #f5f6f8; color:#1e293b; }
.ds-table tbody tr:hover { background:#fafbfc; }
.td-title { font-weight:500; }
.td-muted { color:#94a3b8; font-size:12px; }
.td-desc { max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }

.module-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:500; background:#f1f5f9; color:#475569; }

.status-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.stat-success { background:#f0fdf4; color:#16a34a; }
.stat-cancel { background:#fef2f2; color:#dc2626; }

.empty-row { text-align:center; color:#94a3b8; padding:40px 0 !important; font-size:13px; }

.pagination { display:flex; align-items:center; justify-content:space-between; padding:12px 18px; border-top:1px solid #f0f2f5; }
.page-info { font-size:12px; color:#94a3b8; }
.page-btns { display:flex; align-items:center; gap:6px; }
.page-btn { padding:4px 10px; border:1px solid #e2e8f0; border-radius:5px; background:#fff; font-size:11px; color:#475569; cursor:pointer; font-family:inherit; }
.page-btn:hover:not(:disabled) { border-color:#2563eb; color:#2563eb; }
.page-btn:disabled { opacity:.4; cursor:not-allowed; }
.page-cur { font-size:12px; font-weight:600; color:#2563eb; min-width:20px; text-align:center; }
</style>
