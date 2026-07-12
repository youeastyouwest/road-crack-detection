<template>
  <div class="dash">
    <!-- Top Bar -->
    <div class="dash-top">
      <div class="dash-top-left">
        <h1 class="dash-title">实时仪表盘</h1>
        <span class="dash-date">{{ dateStr }}</span>
      </div>
      <div class="dash-top-right">
        <span class="dash-dot" :class="{ 'dot-pulse': polling }"></span>
        <span class="dash-status">{{ polling ? '实时更新中' : '系统正常' }}</span>
        <span class="dash-time" v-if="lastUpdate">{{ lastUpdate }}</span>
      </div>
    </div>

    <!-- KPI Row - 6 cards -->
    <div class="kpi-row">
      <div v-for="k in kpis" :key="k.label" class="kpi-card">
        <div class="kpi-top">
          <div class="kpi-icon" :style="{ background: k.bg, color: k.color }" v-html="k.icon"></div>
          <span class="kpi-label">{{ k.label }}</span>
        </div>
        <span class="kpi-val">{{ k.value }}</span>
        <span class="kpi-delta" :class="k.delta >= 0 ? 'up' : 'down'">{{ k.delta >= 0 ? '+' : '' }}{{ k.delta }}%</span>
      </div>
    </div>

    <!-- Chart Row 1: Trend + Type Pie -->
    <div class="dash-grid">
      <div class="card card-wide">
        <div class="card-head">
          <span class="card-title">检测趋势</span>
          <div class="card-tabs">
            <button v-for="t in tabs" :key="t.key" :class="['c-tab', { active: activeTab === t.key }]" @click="switchTab(t.key)">{{ t.label }}</button>
          </div>
        </div>
        <div class="card-body">
          <div ref="trendRef" class="chart-box"></div>
        </div>
      </div>
      <div class="card card-narrow">
        <div class="card-head">
          <span class="card-title">病害类型分布</span>
        </div>
        <div class="card-body">
          <div ref="pieRef" class="chart-box"></div>
        </div>
      </div>
    </div>

    <!-- Chart Row 2: Severity + Map Trend -->
    <div class="dash-grid dash-grid-2">
      <div class="card card-half">
        <div class="card-head">
          <span class="card-title">严重等级分布</span>
        </div>
        <div class="card-body">
          <div ref="severityRef" class="chart-box"></div>
        </div>
      </div>
      <div class="card card-half">
        <div class="card-head">
          <span class="card-title">地图标记趋势（近7天）</span>
        </div>
        <div class="card-body">
          <div ref="mapTrendRef" class="chart-box"></div>
        </div>
      </div>
    </div>

    <!-- Map Stats Row -->
    <div class="map-stat-row">
      <div v-for="m in mapStatsCards" :key="m.label" class="ms-card">
        <div class="ms-icon" :style="{ background: m.bg, color: m.color }" v-html="m.icon"></div>
        <div class="ms-body">
          <span class="ms-val">{{ m.value }}</span>
          <span class="ms-label">{{ m.label }}</span>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="dash-actions">
      <a v-for="a in actions" :key="a.label" class="da-item" :href="a.path" @click.prevent="$router.push(a.path)">
        <div class="da-icon" v-html="a.icon"></div>
        <div class="da-text">
          <span class="da-label">{{ a.label }}</span>
          <span class="da-desc">{{ a.desc }}</span>
        </div>
      </a>
    </div>

    <!-- Activity List with Tabs -->
    <div class="card">
      <div class="card-head">
        <div class="card-head-left">
          <span class="card-title">最近活动</span>
          <div class="act-tabs">
            <button :class="['at-tab', { active: actTab === 'detection' }]" @click="actTab = 'detection'">检测任务</button>
            <button :class="['at-tab', { active: actTab === 'alert' }]" @click="actTab = 'alert'">告警事件</button>
          </div>
        </div>
        <button class="card-link" @click="$router.push('/detection-results')">查看全部</button>
      </div>
      <div class="card-body card-body-nopad">
        <!-- Detection Tab -->
        <template v-if="actTab === 'detection'">
          <div class="act-header">
            <span class="act-cell act-id">编号</span>
            <span class="act-cell act-loc">位置</span>
            <span class="act-cell act-src">来源</span>
            <span class="act-cell act-st">状态</span>
            <span class="act-cell act-time">时间</span>
          </div>
          <div v-for="(t,i) in recentTasks" :key="i" class="act-row" :style="{ animationDelay: i * 0.04 + 's' }">
            <span class="act-cell act-id act-code">{{ t.taskCode }}</span>
            <span class="act-cell act-loc">{{ t.location }}</span>
            <span class="act-cell act-src"><span :class="['src-badge', srcCls(t.dataSourceType)]">{{ srcLbl(t.dataSourceType) }}</span></span>
            <span class="act-cell act-st"><span :class="['st-badge', stCls(t.status)]">{{ stLbl(t.status) }}</span></span>
            <span class="act-cell act-time">{{ formatTime(t.createdAt) }}</span>
          </div>
          <div v-if="loading" class="act-loading"><div class="loader"></div></div>
          <div v-if="!loading && recentTasks.length === 0" class="act-empty">暂无检测记录</div>
        </template>
        <!-- Alert Tab -->
        <template v-if="actTab === 'alert'">
          <div class="act-header">
            <span class="act-cell act-id">标记ID</span>
            <span class="act-cell act-loc">位置</span>
            <span class="act-cell act-src">类型</span>
            <span class="act-cell act-st">等级</span>
            <span class="act-cell act-time">状态</span>
          </div>
          <div v-for="(a,i) in alertList" :key="i" class="act-row" :style="{ animationDelay: i * 0.04 + 's' }">
            <span class="act-cell act-id act-code">#{{ a.id }}</span>
            <span class="act-cell act-loc">{{ a.roadName || a.address || `(${a.longitude?.toFixed(4)}, ${a.latitude?.toFixed(4)})` }}</span>
            <span class="act-cell act-src"><span class="src-badge s-drone">{{ damageTypeLabel(a.damageType) }}</span></span>
            <span class="act-cell act-st"><span :class="['st-badge', sevCls(a.severity)]">{{ sevLbl(a.severity) }}</span></span>
            <span class="act-cell act-time"><span :class="['st-badge', statusCls(a.status)]">{{ statusLbl(a.status) }}</span></span>
          </div>
          <div v-if="!loading && alertList.length === 0" class="act-empty">暂无告警事件</div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from "vue"
import { detectionApi, statisticsApi, mapApi } from "@/api"
import * as echarts from "echarts"
import type { DetectionTaskResponse, MapStatisticsResponse, MapTrendPointResponse, MapMarkerResponse } from "@/types"

const loading = ref(false)
const polling = ref(false)
const lastUpdate = ref("")
const recentTasks = ref<DetectionTaskResponse[]>([])
const alertList = ref<MapMarkerResponse[]>([])
const mapStats = ref<MapStatisticsResponse | null>(null)
const mapTrend = ref<MapTrendPointResponse[]>([])
const actTab = ref<"detection" | "alert">("detection")

const trendRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
const severityRef = ref<HTMLElement>()
const mapTrendRef = ref<HTMLElement>()
const activeTab = ref("week")
const trendDays = ref(7)

let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let severityChart: echarts.ECharts | null = null
let mapTrendChart: echarts.ECharts | null = null
let pollTimer: ReturnType<typeof setInterval> | null = null

const dateStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, "0")}/${String(d.getDate()).padStart(2, "0")}`
})

const tabs = [
  { key: "week", label: "本周", days: 7 },
  { key: "month", label: "本月", days: 30 },
]

// --- KPI Cards ---
const kpis = ref<{ label: string; value: string; delta: number; bg: string; color: string; icon: string }[]>([
  { label: "监控道路", value: "—", delta: 0, bg: "#eef2ff", color: "#4361ee", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M12 1v6m0 10v6m11-11h-6M7 12H1"/></svg>' },
  { label: "裂缝总数", value: "—", delta: 0, bg: "#fce7f3", color: "#e11d48", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10M18 20V4M6 20v-6"/></svg>' },
  { label: "今日检测", value: "—", delta: 0, bg: "#fef3c7", color: "#d97706", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>' },
  { label: "待处理告警", value: "—", delta: 0, bg: "#fef2f2", color: "#dc2626", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' },
  { label: "工单总数", value: "—", delta: 0, bg: "#f1f5f9", color: "#475569", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>' },
  { label: "标记总数", value: "—", delta: 0, bg: "#f0fdf4", color: "#16a34a", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>' },
])

// --- Map Stats Cards ---
const mapStatsCards = computed(() => {
  const m = mapStats.value
  return [
    { label: "标记总数", value: m?.totalMarkers ?? 0, bg: "#eef2ff", color: "#4361ee", icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>' },
    { label: "新增标记", value: m?.newMarkers ?? 0, bg: "#f0fdf4", color: "#16a34a", icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>' },
    { label: "已修复", value: m?.repairedCount ?? 0, bg: "#dcfce7", color: "#15803d", icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg>' },
    { label: "待修复", value: m?.pendingRepair ?? 0, bg: "#fef3c7", color: "#d97706", icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>' },
    { label: "严重", value: m?.highSeverityCount ?? 0, bg: "#fef2f2", color: "#dc2626", icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' },
    { label: "中等", value: m?.mediumSeverityCount ?? 0, bg: "#fffbeb", color: "#d97706", icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>' },
  ]
})

const actions = [
  { label: "数据大屏", desc: "全局地图监控", path: "/data-screen", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>' },
  { label: "AI 助手", desc: "智能问答分析", path: "/ai-agent", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>' },
  { label: "工单管理", desc: "任务流转跟踪", path: "/work-orders", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>' },
  { label: "数据统计", desc: "多维统计分析", path: "/data-statistics", icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 3v18h18"/><path d="M7 14l4-4 4 4 5-5"/></svg>' },
]

// --- Helpers ---
function srcCls(s: string) { return ({ DRONE_VIDEO: "s-drone", MANUAL_IMAGE: "s-img", MANUAL_VIDEO: "s-vid", CROWD_SOURCE: "s-crowd" } as any)[s] || "" }
function srcLbl(s: string) { return ({ DRONE_VIDEO: "无人机", MANUAL_IMAGE: "拍照", MANUAL_VIDEO: "录像", CROWD_SOURCE: "众包" } as any)[s] || s }
function stCls(s: string) { return ({ PENDING: "st-wait", PROCESSING: "st-run", COMPLETED: "st-ok", FAILED: "st-fail" } as any)[s] || "" }
function stLbl(s: string) { return ({ PENDING: "待处理", PROCESSING: "处理中", COMPLETED: "已完成", FAILED: "失败" } as any)[s] || s }
function formatTime(t: string) { return t ? t.replace("T", " ").substring(0, 16) : "--" }
function damageTypeLabel(t: string) { return ({ CRACK: "裂缝", TRANSVERSE_CRACK: "横向裂缝", LONGITUDINAL_CRACK: "纵向裂缝", NET_CRACK: "网状裂缝", POTHOLE: "坑槽", MARKING_DAMAGE: "标线损坏", ROAD_SPILL: "路面抛洒", UNKNOWN: "未知" } as any)[t] || t || "--" }
function sevCls(s: string) { return ({ HIGH: "st-fail", MEDIUM: "st-run", LOW: "st-ok" } as any)[s] || "st-wait" }
function sevLbl(s: string) { return ({ HIGH: "严重", MEDIUM: "中等", LOW: "轻微" } as any)[s] || s }
function statusCls(s: string) { return ({ PENDING: "st-wait", REPAIRED: "st-ok", NEW: "st-run" } as any)[s] || "st-wait" }
function statusLbl(s: string) { return ({ PENDING: "待修复", REPAIRED: "已修复", NEW: "新增" } as any)[s] || s }

// --- Data Loading ---
async function loadDashboard() {
  try {
    const res = await statisticsApi.getDashboard()
    const d = res.data.data
    if (d) {
      kpis.value[0].value = String(d.monitoredRoads ?? 0)
      kpis.value[1].value = String(d.totalCracksDetected ?? 0)
      kpis.value[2].value = String(d.detectionToday ?? 0)
      kpis.value[3].value = String(d.pendingAlerts ?? 0)
      kpis.value[4].value = String(d.totalWorkOrders ?? 0)
    }
  } catch (e) { console.error("Dashboard load error:", e) }
}

async function loadMapStats() {
  try {
    const res = await mapApi.statistics()
    mapStats.value = res.data.data
    if (mapStats.value) {
      kpis.value[5].value = String(mapStats.value.totalMarkers ?? 0)
    }
  } catch (e) { console.error("Map stats error:", e) }
}

async function loadActivities() {
  try {
    const res = await detectionApi.list({ page: 1, size: 10 })
    recentTasks.value = res.data.data.records || []
  } catch (e) { console.error("Activities error:", e) }
}

async function loadAlerts() {
  try {
    const res = await mapApi.markers({ onlyWithCoordinates: true })
    alertList.value = (res.data.data || []).slice(0, 10)
  } catch (e) { console.error("Alerts error:", e) }
}

async function loadTrend() {
  if (!trendRef.value) return
  try {
    const res = await statisticsApi.getTrend(trendDays.value)
    const items = res.data.data || []
    if (trendChart) {
      trendChart.setOption({
        xAxis: { data: items.map((x: any) => (x.date || "").slice(5)) },
        series: [{ data: items.map((x: any) => x.count) }],
      })
    }
  } catch (e) { console.error("Trend error:", e) }
}

async function loadPie() {
  if (!pieRef.value) return
  try {
    const res = await statisticsApi.getCrackType()
    const items = res.data.data || []
    if (pieChart) {
      const typeColors: Record<string, string> = { CRACK: "#4361ee", TRANSVERSE_CRACK: "#4361ee", LONGITUDINAL_CRACK: "#f59e0b", NET_CRACK: "#10b981", POTHOLE: "#ef4444", MARKING_DAMAGE: "#8b5cf6", ROAD_SPILL: "#ec4899", UNKNOWN: "#94a3b8" }
      const typeLabels: Record<string, string> = { CRACK: "裂缝", TRANSVERSE_CRACK: "横向裂缝", LONGITUDINAL_CRACK: "纵向裂缝", NET_CRACK: "网状裂缝", POTHOLE: "坑槽", MARKING_DAMAGE: "标线损坏", ROAD_SPILL: "路面抛洒", UNKNOWN: "未知" }
      pieChart.setOption({
        series: [{
          data: items.map((x: any) => ({
            name: typeLabels[x.type] || x.type,
            value: x.count,
            itemStyle: { color: typeColors[x.type] || "#94a3b8" }
          }))
        }]
      })
    }
  } catch (e) { console.error("Pie error:", e) }
}

async function loadSeverity() {
  if (!severityRef.value) return
  try {
    const res = await statisticsApi.getSeverity()
    const items = res.data.data || []
    if (severityChart) {
      const labels: Record<string, string> = { HIGH: "严重", MEDIUM: "中等", LOW: "轻微" }
      const colors: Record<string, string> = { HIGH: "#dc2626", MEDIUM: "#d97706", LOW: "#16a34a" }
      severityChart.setOption({
        xAxis: { data: items.map((x: any) => labels[x.level] || x.level) },
        series: [{
          data: items.map((x: any) => ({
            value: x.count,
            itemStyle: { color: colors[x.level] || "#94a3b8" }
          }))
        }]
      })
    }
  } catch (e) { console.error("Severity error:", e) }
}

async function loadMapTrend() {
  if (!mapTrendRef.value) return
  try {
    const res = await mapApi.trend(7)
    mapTrend.value = res.data.data || []
    if (mapTrendChart) {
      const dates = mapTrend.value.map(d => (d.date || "").slice(5))
      mapTrendChart.setOption({
        xAxis: { data: dates },
        series: [
          { name: "新增标记", data: mapTrend.value.map(d => d.count || 0) },
          { name: "已修复", data: mapTrend.value.map(d => d.repairedCount || 0) },
        ],
      })
    }
  } catch (e) { console.error("Map trend error:", e) }
}

function switchTab(key: string) {
  activeTab.value = key
  const tab = tabs.find(t => t.key === key)
  if (tab) {
    trendDays.value = tab.days
    loadTrend()
  }
}

// --- Polling ---
async function pollData() {
  polling.value = true
  await Promise.allSettled([
    loadDashboard(),
    loadMapStats(),
    loadActivities(),
    loadAlerts(),
  ])
  lastUpdate.value = new Date().toLocaleTimeString("zh-CN", { hour12: false })
  polling.value = false
}

// --- Resize ---
function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
  severityChart?.resize()
  mapTrendChart?.resize()
}

// --- Lifecycle ---
onMounted(async () => {
  loading.value = true
  await nextTick()
  // Init charts
  if (trendRef.value) {
    trendChart = echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: { trigger: "axis", backgroundColor: "#fff", borderColor: "#f3f4f6", borderWidth: 1, textStyle: { color: "#111827", fontSize: 11 } },
      grid: { left: 40, right: 16, bottom: 28, top: 12 },
      xAxis: { type: "category", data: [], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: "#94a3b8", fontSize: 11 } },
      yAxis: { type: "value", splitLine: { lineStyle: { color: "#f1f5f9" } }, axisLabel: { color: "#94a3b8", fontSize: 11 } },
      series: [{
        type: "bar",
        data: [],
        barMaxWidth: 20,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "#4361ee" }, { offset: 1, color: "#93c5fd" }]),
          borderRadius: [4, 4, 0, 0],
        }
      }],
    })
  }
  if (pieRef.value) {
    pieChart = echarts.init(pieRef.value)
    pieChart.setOption({
      tooltip: { trigger: "item", backgroundColor: "#fff", borderColor: "#f3f4f6", borderWidth: 1, textStyle: { color: "#111827", fontSize: 11 } },
      legend: { orient: "vertical", right: 10, top: "center", textStyle: { color: "#94a3b8", fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
      series: [{
        type: "pie", radius: ["38%", "65%"], center: ["38%", "50%"],
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 13, fontWeight: 600 } },
        data: []
      }],
    })
  }
  if (severityRef.value) {
    severityChart = echarts.init(severityRef.value)
    severityChart.setOption({
      tooltip: { trigger: "axis", backgroundColor: "#fff", borderColor: "#f3f4f6", borderWidth: 1, textStyle: { color: "#111827", fontSize: 11 } },
      grid: { left: 40, right: 16, bottom: 28, top: 12 },
      xAxis: { type: "category", data: [], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: "#94a3b8", fontSize: 11 } },
      yAxis: { type: "value", splitLine: { lineStyle: { color: "#f1f5f9" } }, axisLabel: { color: "#94a3b8", fontSize: 11 } },
      series: [{ type: "bar", data: [], barMaxWidth: 36, itemStyle: { borderRadius: [4, 4, 0, 0] } }],
    })
  }
  if (mapTrendRef.value) {
    mapTrendChart = echarts.init(mapTrendRef.value)
    mapTrendChart.setOption({
      tooltip: { trigger: "axis", backgroundColor: "#fff", borderColor: "#f3f4f6", borderWidth: 1, textStyle: { color: "#111827", fontSize: 11 } },
      legend: { data: ["新增标记", "已修复"], right: 10, top: 0, textStyle: { color: "#94a3b8", fontSize: 11 }, itemWidth: 12, itemHeight: 8 },
      grid: { left: 40, right: 16, bottom: 28, top: 35 },
      xAxis: { type: "category", data: [], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: "#94a3b8", fontSize: 11 } },
      yAxis: { type: "value", splitLine: { lineStyle: { color: "#f1f5f9" } }, axisLabel: { color: "#94a3b8", fontSize: 11 } },
      series: [
        {
          name: "新增标记", type: "line", data: [], smooth: true, symbol: "circle", symbolSize: 6,
          lineStyle: { width: 2, color: "#4361ee" }, itemStyle: { color: "#4361ee" },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "rgba(67,97,238,0.15)" }, { offset: 1, color: "rgba(67,97,238,0)" }]) },
        },
        {
          name: "已修复", type: "line", data: [], smooth: true, symbol: "circle", symbolSize: 6,
          lineStyle: { width: 2, color: "#16a34a" }, itemStyle: { color: "#16a34a" },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "rgba(22,163,74,0.15)" }, { offset: 1, color: "rgba(22,163,74,0)" }]) },
        },
      ],
    })
  }
  // Load all data
  await Promise.allSettled([loadDashboard(), loadMapStats(), loadActivities(), loadAlerts(), loadTrend(), loadPie(), loadSeverity(), loadMapTrend()])
  lastUpdate.value = new Date().toLocaleTimeString("zh-CN", { hour12: false })
  loading.value = false
  // Start 30s polling
  window.addEventListener("resize", handleResize)
  pollTimer = setInterval(pollData, 30000)
})

onUnmounted(() => {
  window.removeEventListener("resize", handleResize)
  if (pollTimer) clearInterval(pollTimer)
  trendChart?.dispose()
  pieChart?.dispose()
  severityChart?.dispose()
  mapTrendChart?.dispose()
})
</script>

<style scoped>
.dash { padding:0; font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,-apple-system,sans-serif; }

/* Top Bar */
.dash-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:28px; }
.dash-top-left { display:flex; align-items:baseline; gap:16px; }
.dash-title { font-size:22px; font-weight:600; color:#111827; margin:0; letter-spacing:-0.02em; }
.dash-date { font-size:13px; color:#9ca3af; font-variant-numeric:tabular-nums; }
.dash-top-right { display:flex; align-items:center; gap:8px; }
.dash-dot { width:6px; height:6px; border-radius:50%; background:#22c55e; }
.dot-pulse { animation:pulse 1.5s ease-in-out infinite; }
@keyframes pulse { 0%,100% { opacity:1; transform:scale(1); } 50% { opacity:.5; transform:scale(1.3); } }
.dash-status { font-size:12px; color:#6b7280; }
.dash-time { font-size:11px; color:#9ca3af; margin-left:4px; font-variant-numeric:tabular-nums; }

/* KPI Row */
.kpi-row { display:grid; grid-template-columns:repeat(6,1fr); gap:12px; margin-bottom:24px; }
.kpi-card { background:#fff; border:1px solid #f3f4f6; border-radius:10px; padding:16px; }
.kpi-top { display:flex; align-items:center; gap:8px; margin-bottom:10px; }
.kpi-icon { width:28px; height:28px; display:flex; align-items:center; justify-content:center; border-radius:7px; flex-shrink:0; }
.kpi-label { font-size:11px; font-weight:500; color:#9ca3af; letter-spacing:0.03em; }
.kpi-val { display:block; font-size:22px; font-weight:700; color:#111827; letter-spacing:-0.03em; line-height:1; font-variant-numeric:tabular-nums; margin-bottom:4px; }
.kpi-delta { display:inline-block; font-size:11px; font-weight:600; }
.kpi-delta.up { color:#22c55e; }
.kpi-delta.down { color:#ef4444; }

/* Grid */
.dash-grid { display:grid; grid-template-columns:1.6fr 1fr; gap:16px; margin-bottom:16px; }
.dash-grid-2 { grid-template-columns:1fr 1fr; }

/* Cards */
.card { background:#fff; border:1px solid #f3f4f6; border-radius:10px; overflow:hidden; }
.card-wide { grid-column:span 1; }
.card-narrow { grid-column:span 1; }
.card-half { grid-column:span 1; }
.card-head { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f9fafb; }
.card-head-left { display:flex; align-items:center; gap:16px; }
.card-title { font-size:13px; font-weight:600; color:#374151; }
.card-tabs { display:flex; gap:2px; background:#f9fafb; border-radius:6px; padding:2px; }
.c-tab { padding:4px 12px; border:none; border-radius:4px; font-size:11px; font-weight:500; color:#9ca3af; cursor:pointer; background:transparent; font-family:inherit; transition:all .15s; }
.c-tab.active { background:#fff; color:#4338ca; box-shadow:0 1px 2px rgba(0,0,0,0.04); }
.c-tab:hover:not(.active) { color:#6b7280; }
.card-body { padding:4px 4px 0; }
.card-body-nopad { padding:0; }
.chart-box { width:100%; height:260px; }
.card-link { background:none; border:none; font-size:12px; font-weight:500; color:#4338ca; cursor:pointer; font-family:inherit; padding:2px 0; }
.card-link:hover { opacity:.7; }

/* Map Stats Row */
.map-stat-row { display:grid; grid-template-columns:repeat(6,1fr); gap:12px; margin-bottom:20px; }
.ms-card { display:flex; align-items:center; gap:10px; padding:14px; background:#fff; border:1px solid #f3f4f6; border-radius:10px; }
.ms-icon { width:30px; height:30px; display:flex; align-items:center; justify-content:center; border-radius:7px; flex-shrink:0; }
.ms-body { display:flex; flex-direction:column; gap:1px; }
.ms-val { font-size:18px; font-weight:800; color:#111827; line-height:1; font-variant-numeric:tabular-nums; }
.ms-label { font-size:10px; color:#9ca3af; }

/* Quick Actions */
.dash-actions { display:grid; grid-template-columns:repeat(4,1fr); gap:16px; margin-bottom:20px; }
.da-item { display:flex; align-items:center; gap:12px; padding:14px 18px; background:#fff; border:1px solid #f3f4f6; border-radius:10px; text-decoration:none; cursor:pointer; transition:all .2s; }
.da-item:hover { border-color:#e5e7eb; background:#fafbfc; }
.da-icon { width:36px; height:36px; display:flex; align-items:center; justify-content:center; background:#f9fafb; border-radius:8px; color:#4338ca; flex-shrink:0; }
.da-text { display:flex; flex-direction:column; gap:1px; }
.da-label { font-size:13px; font-weight:600; color:#111827; }
.da-desc { font-size:11px; color:#9ca3af; }

/* Activity Tabs */
.act-tabs { display:flex; gap:2px; background:#f9fafb; border-radius:6px; padding:2px; }
.at-tab { padding:4px 12px; border:none; border-radius:4px; font-size:11px; font-weight:500; color:#9ca3af; cursor:pointer; background:transparent; font-family:inherit; transition:all .15s; }
.at-tab.active { background:#fff; color:#4338ca; box-shadow:0 1px 2px rgba(0,0,0,0.04); }
.at-tab:hover:not(.active) { color:#6b7280; }

/* Activity Table */
.act-header { display:flex; padding:10px 20px; border-bottom:1px solid #f9fafb; }
.act-cell { font-size:11px; color:#9ca3af; }
.act-id { width:140px; flex-shrink:0; }
.act-loc { flex:1; }
.act-src { width:70px; flex-shrink:0; }
.act-st { width:70px; flex-shrink:0; }
.act-time { width:110px; flex-shrink:0; }
.act-row { display:flex; padding:10px 20px; border-bottom:1px solid #f9fafb; align-items:center; animation:rowIn .35s ease both; }
.act-row:last-child { border-bottom:none; }
.act-row:hover { background:#fafbfc; }
@keyframes rowIn { from { opacity:0; transform:translateY(4px); } to { opacity:1; transform:translateY(0); } }
.act-code { font-family:monospace; font-size:11px; font-weight:600; color:#4338ca; }
.src-badge, .st-badge { display:inline-block; padding:1px 7px; border-radius:4px; font-size:10px; font-weight:600; }
.s-drone { background:#eef2ff; color:#4338ca; }
.s-img { background:#f0fdf4; color:#22c55e; }
.s-vid { background:#fffbeb; color:#d97706; }
.s-crowd { background:#f5f3ff; color:#8b5cf6; }
.st-wait { background:#f9fafb; color:#6b7280; }
.st-run { background:#fffbeb; color:#d97706; }
.st-ok { background:#f0fdf4; color:#22c55e; }
.st-fail { background:#fef2f2; color:#ef4444; }
.act-loading { display:flex; justify-content:center; padding:32px 0; }
.loader { width:20px; height:20px; border:2px solid #f3f4f6; border-top-color:#4338ca; border-radius:50%; animation:spin .5s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
.act-empty { text-align:center; padding:36px 0; color:#9ca3af; font-size:13px; }

/* Responsive */
@media(max-width:1200px) {
  .kpi-row { grid-template-columns:repeat(3,1fr); }
  .map-stat-row { grid-template-columns:repeat(3,1fr); }
}
@media(max-width:768px) {
  .kpi-row { grid-template-columns:repeat(2,1fr); }
  .dash-grid, .dash-grid-2 { grid-template-columns:1fr; }
  .map-stat-row { grid-template-columns:repeat(2,1fr); }
  .dash-actions { grid-template-columns:repeat(2,1fr); }
}
</style>
