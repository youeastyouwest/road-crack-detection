<template>
  <div class="alert-page">
    <!-- Page Header -->
    <div class="page-head">
      <div>
        <h2 class="page-title">告警管理</h2>
        <p class="page-desc">路面异常预警与处理跟踪</p>
      </div>
      <div class="head-right">
        <button class="btn-ghost" @click="loadData" :disabled="loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          刷新
        </button>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-row">
      <div class="stat-card" v-for="s in alertStats" :key="s.label">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
          <span v-html="s.icon"></span>
        </div>
        <div class="stat-body">
          <span class="stat-val">{{ s.value }}</span>
          <span class="stat-label">{{ s.label }}</span>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <select v-model="filters.level" @change="loadData" class="sel">
        <option value="">全部等级</option>
        <option value="HIGH">严重</option>
        <option value="MEDIUM">中等</option>
        <option value="LOW">一般</option>
      </select>
      <select v-model="filters.type" @change="loadData" class="sel">
        <option value="">全部类型</option>
        <option value="SEVERITY_DAMAGE">严重损坏</option>
        <option value="OVERDUE_WORKORDER">工单超时</option>
        <option value="SUDDEN_CRACK">突发裂缝</option>
      </select>
      <select v-model="filters.status" @change="loadData" class="sel">
        <option value="">全部状态</option>
        <option value="PENDING">待处理</option>
        <option value="HANDLED">已处理</option>
      </select>
      <span class="filter-count" v-if="total > 0">共 {{ total }} 条告警</span>
    </div>

    <!-- Alert List -->
    <div class="alert-list" v-loading="loading">
      <div v-if="alerts.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无告警记录" :image-size="80" />
      </div>

      <div v-for="a in alerts" :key="a.id" :class="['alert-item', `level-${a.alertLevel?.toLowerCase()}`]">
        <div class="alert-left">
          <div :class="['level-badge', `lv-${a.alertLevel?.toLowerCase()}`]">
            {{ levelLabel(a.alertLevel) }}
          </div>
          <div class="alert-main">
            <div class="alert-title-row">
              <span class="alert-title">{{ a.title }}</span>
              <span :class="['status-tag', a.status === 'HANDLED' ? 'st-handled' : 'st-pending']">
                {{ a.status === 'HANDLED' ? '已处理' : '待处理' }}
              </span>
            </div>
            <div class="alert-content" v-if="a.content">{{ a.content }}</div>
            <div class="alert-meta">
              <span class="meta-item"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>{{ a.location || '—' }}</span>
              <span class="meta-item"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>{{ typeLabel(a.alertType) }}</span>
              <span class="meta-item" v-if="a.damageType"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>{{ a.damageType }}</span>
              <span class="meta-item"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>{{ formatDate(a.createdAt) }}</span>
              <span class="meta-item code" v-if="a.alertCode">{{ a.alertCode }}</span>
            </div>
            <div class="alert-handle" v-if="a.status === 'HANDLED'">
              <span class="handle-info">处理人: {{ a.handledBy || '—' }}</span>
              <span class="handle-info" v-if="a.handleRemark">备注: {{ a.handleRemark }}</span>
              <span class="handle-info" v-if="a.handledAt">{{ formatDate(a.handledAt) }}</span>
            </div>
          </div>
        </div>
        <div class="alert-actions" v-if="a.status === 'PENDING'">
          <button class="btn-handle" @click="openHandleDialog(a)">处理</button>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pager" v-if="total > pageSize">
      <button class="pg-btn" :disabled="page <= 1" @click="page--; loadData()">上一页</button>
      <span class="pg-info">第 {{ page }} / {{ totalPages }} 页</span>
      <button class="pg-btn" :disabled="page >= totalPages" @click="page++; loadData()">下一页</button>
    </div>

    <!-- Handle Dialog -->
    <div v-if="handleDialog.visible" class="dialog-overlay" @click.self="handleDialog.visible = false">
      <div class="dialog-box">
        <h3 class="dialog-title">处理告警</h3>
        <div class="dialog-alert-info" v-if="handleDialog.alert">
          <span class="dia-label">告警标题:</span> {{ handleDialog.alert.title }}
        </div>
        <div class="dialog-field">
          <label class="dia-label">处理备注</label>
          <textarea v-model="handleDialog.remark" class="dia-textarea" rows="3"
            placeholder="请输入处理说明..."></textarea>
        </div>
        <div class="dialog-actions">
          <button class="btn-cancel" @click="handleDialog.visible = false">取消</button>
          <button class="btn-confirm" @click="submitHandle" :disabled="handleDialog.submitting">
            {{ handleDialog.submitting ? '提交中...' : '确认处理' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { alertApi } from "@/api"
import type { AlertResponse } from "@/types"

const loading = ref(false)
const alerts = ref<AlertResponse[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const totalPages = computed(() => Math.ceil(total.value / pageSize) || 1)

const filters = ref({
  level: "",
  type: "",
  status: "",
})

const alertStats = ref([
  { label: "严重告警", value: 0, color: "#dc2626", bg: "#fef2f2", icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' },
  { label: "中等告警", value: 0, color: "#d97706", bg: "#fffbeb", icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>' },
  { label: "一般告警", value: 0, color: "#6366f1", bg: "#eef2ff", icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>' },
])

const handleDialog = ref({
  visible: false,
  alert: null as AlertResponse | null,
  remark: "",
  submitting: false,
})

function levelLabel(level: string) {
  return ({ HIGH: "严重", MEDIUM: "中等", LOW: "一般" } as any)[level] || level
}
function typeLabel(type: string) {
  return ({ SEVERITY_DAMAGE: "严重损坏", OVERDUE_WORKORDER: "工单超时", SUDDEN_CRACK: "突发裂缝" } as any)[type] || type
}
function formatDate(dt: string) {
  if (!dt) return "—"
  return dt.replace("T", " ").substring(0, 16)
}

async function loadData() {
  loading.value = true
  try {
    const [res, statsRes] = await Promise.all([
      alertApi.list({
        page: page.value,
        size: pageSize,
        alertLevel: filters.value.level || undefined,
        alertType: filters.value.type || undefined,
        status: filters.value.status || undefined,
      }),
      alertApi.stats(),
    ])
    alerts.value = res.data.data.records
    total.value = res.data.data.total

    // 使用后端统计接口获取按等级分组的数量
    const stats = statsRes.data.data
    alertStats.value[0].value = stats.highCount
    alertStats.value[1].value = stats.mediumCount
    alertStats.value[2].value = stats.lowCount
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "加载告警数据失败")
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())

function openHandleDialog(alert: AlertResponse) {
  handleDialog.value.alert = alert
  handleDialog.value.remark = ""
  handleDialog.value.submitting = false
  handleDialog.value.visible = true
}

async function submitHandle() {
  if (!handleDialog.value.alert) return
  handleDialog.value.submitting = true
  try {
    await alertApi.handle(handleDialog.value.alert.id, handleDialog.value.remark)
    ElMessage.success("告警处理成功")
    handleDialog.value.visible = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "处理告警失败")
  } finally {
    handleDialog.value.submitting = false
  }
}
</script>

<style scoped>
.alert-page { padding: 0; }

.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 20px; font-weight: 600; color: #1a202c; margin: 0 0 4px 0; }
.page-desc { font-size: 13px; color: #94a3b8; margin: 0; }
.head-right { display: flex; gap: 12px; align-items: center; }
.btn-ghost { display: inline-flex; align-items: center; gap: 6px; padding: 7px 14px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; color: #475569; font-size: 13px; cursor: pointer; transition: all .2s; }
.btn-ghost:hover { border-color: #3b82f6; color: #3b82f6; }
.btn-ghost:disabled { opacity: .5; cursor: not-allowed; }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { display: flex; align-items: center; gap: 14px; padding: 18px 20px; background: #fff; border: 1px solid #eef0f4; border-radius: 10px; }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-body { display: flex; flex-direction: column; }
.stat-val { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 12px; color: #94a3b8; margin-top: 2px; }

.filter-bar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; flex-wrap: wrap; }
.sel { padding: 7px 12px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; color: #475569; background: #fff; cursor: pointer; outline: none; transition: border-color .2s; }
.sel:hover { border-color: #93c5fd; }
.sel:focus { border-color: #3b82f6; }
.filter-count { font-size: 13px; color: #94a3b8; margin-left: auto; }

.alert-list { display: flex; flex-direction: column; gap: 12px; }
.empty-state { padding: 40px 0; }

.alert-item { display: flex; align-items: flex-start; gap: 14px; padding: 16px 20px; background: #fff; border: 1px solid #eef0f4; border-radius: 10px; transition: box-shadow .2s; }
.alert-item:hover { box-shadow: 0 2px 12px rgba(0,0,0,.06); }
.alert-item.level-high { border-left: 3px solid #dc2626; }
.alert-item.level-medium { border-left: 3px solid #d97706; }
.alert-item.level-low { border-left: 3px solid #6366f1; }

.alert-left { display: flex; gap: 14px; width: 100%; }
.alert-actions { flex-shrink: 0; display: flex; align-items: center; }
.btn-handle { padding: 5px 14px; border: 1px solid #3b82f6; border-radius: 6px; background: #3b82f6; color: #fff; font-size: 12px; cursor: pointer; transition: all .2s; white-space: nowrap; }
.btn-handle:hover { background: #2563eb; border-color: #2563eb; }
.level-badge { flex-shrink: 0; padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; white-space: nowrap; }
.lv-high { background: #fef2f2; color: #dc2626; }
.lv-medium { background: #fffbeb; color: #d97706; }
.lv-low { background: #eef2ff; color: #6366f1; }

.alert-main { flex: 1; min-width: 0; }
.alert-title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.alert-title { font-size: 14px; font-weight: 600; color: #1a202c; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
.st-pending { background: #fffbeb; color: #d97706; }
.st-handled { background: #f0fdf4; color: #16a34a; }

.alert-content { font-size: 13px; color: #64748b; line-height: 1.6; margin-bottom: 8px; }

.alert-meta { display: flex; flex-wrap: wrap; gap: 14px; font-size: 12px; color: #94a3b8; }
.meta-item { display: inline-flex; align-items: center; gap: 4px; }
.meta-item.code { font-family: monospace; }

.alert-handle { margin-top: 8px; padding: 8px 12px; background: #f8fafc; border-radius: 6px; display: flex; gap: 16px; font-size: 12px; color: #64748b; }
.handle-info { }

.pager { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 20px; }
.pg-btn { padding: 6px 16px; border: 1px solid #e2e8f0; border-radius: 6px; background: #fff; color: #475569; font-size: 13px; cursor: pointer; transition: all .2s; }
.pg-btn:hover:not(:disabled) { border-color: #3b82f6; color: #3b82f6; }
.pg-btn:disabled { opacity: .4; cursor: not-allowed; }
.pg-info { font-size: 13px; color: #94a3b8; }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 9999; }
.dialog-box { background: #fff; border-radius: 12px; padding: 24px; width: 460px; max-width: 90vw; box-shadow: 0 8px 32px rgba(0,0,0,.12); }
.dialog-title { font-size: 17px; font-weight: 600; color: #1a202c; margin: 0 0 16px 0; }
.dialog-alert-info { font-size: 13px; color: #64748b; margin-bottom: 16px; padding: 10px 12px; background: #f8fafc; border-radius: 8px; }
.dialog-field { margin-bottom: 16px; }
.dia-label { display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px; }
.dia-textarea { width: 100%; padding: 10px 12px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; color: #1a202c; outline: none; resize: vertical; font-family: inherit; transition: border-color .2s; box-sizing: border-box; }
.dia-textarea:focus { border-color: #3b82f6; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn-cancel { padding: 8px 18px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; color: #64748b; font-size: 13px; cursor: pointer; transition: all .2s; }
.btn-cancel:hover { border-color: #cbd5e1; }
.btn-confirm { padding: 8px 18px; border: none; border-radius: 8px; background: #3b82f6; color: #fff; font-size: 13px; cursor: pointer; transition: all .2s; }
.btn-confirm:hover { background: #2563eb; }
.btn-confirm:disabled { opacity: .5; cursor: not-allowed; }
</style>
