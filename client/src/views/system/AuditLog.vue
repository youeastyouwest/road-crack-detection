<template>
  <div class="al">
    <div class="page-head">
      <div class="page-head-left">
        <h2 class="page-title">操作日志</h2>
        <p class="page-desc">系统操作审计与追踪</p>
      </div>
    </div>

    <div class="panel" style="margin-bottom:16px">
      <div class="panel-head">
        <div class="filter-bar">
          <select v-model="filters.module" class="filter-select" @change="loadData">
            <option value="">全部模块</option>
            <option value="系统管理">系统管理</option>
            <option value="用户管理">用户管理</option>
            <option value="工单管理">工单管理</option>
            <option value="检测管理">检测管理</option>
          </select>
          <select v-model="filters.status" class="filter-select" @change="loadData">
            <option value="">全部状态</option>
            <option value="SUCCESS">成功</option>
            <option value="FAILED">失败</option>
          </select>
        </div>
        <span class="count-badge">{{ logs.length }} 条</span>
      </div>
    </div>

    <div class="panel">
      <div class="panel-body" style="padding:0">
        <table class="data-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>操作</th>
              <th>模块</th>
              <th>IP</th>
              <th>耗时</th>
              <th>状态</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td class="cell-primary">{{ log.username }}</td>
              <td>{{ log.action }}</td>
              <td><span class="module-tag">{{ log.module }}</span></td>
              <td style="font-family:monospace;font-size:12px;color:#9ca3af">{{ log.ip }}</td>
              <td style="font-family:monospace;font-size:12px;color:#6b7280">{{ log.duration }}ms</td>
              <td><span :class="['tag', log.status==='SUCCESS'?'tag-green':'tag-red']">{{ log.status==='SUCCESS'?'成功':'失败' }}</span></td>
              <td style="font-size:12px;color:#9ca3af;white-space:nowrap">{{ new Date(log.createdAt).toLocaleString('zh-CN') }}</td>
            </tr>
            <tr v-if="logs.length===0"><td colspan="7" class="cell-empty">暂无日志数据</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import http from "@/api/index"
import type { ApiResponse, PageResponse, AuditLogResponse } from "@/types"

const logs = ref<AuditLogResponse[]>([])
const loading = ref(false)
const filters = reactive({ module: "", status: "" })

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: 1, size: 50 }
    if (filters.module) params.module = filters.module
    if (filters.status) params.status = filters.status
    const r = await http.get<ApiResponse<PageResponse<AuditLogResponse>>>("/audit-log/page", { params })
    logs.value = r.data.data.records
  } catch {}
  loading.value = false
}

onMounted(loadData)
</script>
<style scoped>
.al { font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,sans-serif; max-width:1100px; margin:0 auto; }
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:32px; }
.page-title { font-size:22px; font-weight:600; color:#111827; margin:0 0 4px; letter-spacing:-.3px; }
.page-desc { font-size:13px; color:#9ca3af; margin:0; }
.panel { background:#fff; border:1px solid #f3f4f6; border-radius:12px; overflow:hidden; box-shadow:0 1px 3px rgba(0,0,0,.04); }
.panel-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f9fafb; }
.count-badge { font-size:12px; color:#9ca3af; }
.filter-bar { display:flex; gap:10px; }
.filter-select { padding:7px 12px; border:1px solid #e5e7eb; border-radius:8px; font-size:12px; font-family:inherit; color:#374151; outline:none; background:#fff; cursor:pointer; }
.filter-select:focus { border-color:#4338ca; }
.data-table { width:100%; border-collapse:collapse; }
.data-table th { text-align:left; padding:12px 20px; font-size:11px; font-weight:600; color:#9ca3af; background:#fafbfc; border-bottom:1px solid #f9fafb; white-space:nowrap; }
.data-table td { padding:12px 20px; font-size:13px; color:#6b7280; border-bottom:1px solid #fafbfc; }
.data-table tr:last-child td { border-bottom:none; }
.data-table tr:hover td { background:#fafbfc; }
.cell-primary { font-weight:600; color:#111827; }
.cell-empty { text-align:center; padding:40px 0 !important; color:#9ca3af; }
.tag { display:inline-block; padding:2px 10px; border-radius:12px; font-size:11px; font-weight:600; }
.tag-green { background:#f0fdf4; color:#16a34a; }
.tag-red { background:#fef2f2; color:#dc2626; }
.module-tag { display:inline-block; padding:2px 8px; background:#f5f7fa; border-radius:4px; font-size:11px; color:#6b7280; }
</style>