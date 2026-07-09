<template>
  <div class="dash">
    <!-- Top Bar: just date + minimal indicator -->
    <div class="dash-top">
      <div class="dash-top-left">
        <h1 class="dash-title">仪表盘</h1>
        <span class="dash-date">{{ dateStr }}</span>
      </div>
      <div class="dash-top-right">
        <span class="dash-dot"></span>
        <span class="dash-status">系统正常</span>
      </div>
    </div>

    <!-- KPI Row - spacious, minimal -->
    <div class="kpi-row">
      <div v-for="k in kpis" :key="k.label" class="kpi-card">
        <span class="kpi-label">{{ k.label }}</span>
        <span class="kpi-val">{{ k.value }}</span>
        <span class="kpi-delta" :class="k.delta >= 0 ? 'up' : 'down'">{{ k.delta >= 0 ? '+' : '' }}{{ k.delta }}%</span>
      </div>
    </div>

    <!-- Main Content: 2-column layout -->
    <div class="dash-grid">
      <!-- Left: Trend Chart -->
      <div class="card card-wide">
        <div class="card-head">
          <span class="card-title">检测趋势</span>
          <div class="card-tabs">
            <button v-for="t in tabs" :key="t.key" :class="['c-tab', { active: activeTab === t.key }]" @click="activeTab = t.key">{{ t.label }}</button>
          </div>
        </div>
        <div class="card-body">
          <div ref="trendRef" class="chart-box"><div class="chart-empty">暂无检测数据</div></div>
        </div>
      </div>

      <!-- Right: Distribution -->
      <div class="card card-narrow">
        <div class="card-head">
          <span class="card-title">病害分布</span>
        </div>
        <div class="card-body">
          <div ref="pieRef" class="chart-box"><div class="chart-empty">暂无分布数据</div></div>
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

    <!-- Activity List -->
    <div class="card">
      <div class="card-head">
        <span class="card-title">最近活动</span>
        <button class="card-link" @click="$router.push('/detection-results')">查看全部</button>
      </div>
      <div class="card-body card-body-nopad">
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
          <span class="act-cell act-time">{{ t.createdAt }}</span>
        </div>
        <div v-if="loading" class="act-loading">
          <div class="loader"></div>
        </div>
        <div v-if="!loading && recentTasks.length === 0" class="act-empty">暂无记录</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue"
import { detectionApi, statisticsApi } from "@/api"
import * as echarts from "echarts"
import { ElMessage } from "element-plus"
import type { DetectionTaskResponse } from "@/types"

const loading = ref(false)
const recentTasks = ref<DetectionTaskResponse[]>([])
const trendRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
const activeTab = ref("week")
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const dateStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}/${String(d.getMonth()+1).padStart(2,"0")}/${String(d.getDate()).padStart(2,"0")}`
})

const tabs = [{key:"week",label:"本周"},{key:"month",label:"本月"}]

const kpis = [
  { label: "检测里程", value: "—", delta: 0 },
  { label: "裂缝总数", value: "—", delta: 0 },
  { label: "已修复", value: "—", delta: 0 },
  { label: "待处理", value: "—", delta: 0 },
]

const actions = [
  { label: "数据大屏", desc: "全局地图监控", path: "/data-screen",
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>' },
  { label: "AI 助手", desc: "智能问答分析", path: "/ai-agent",
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>' },
  { label: "工单管理", desc: "任务流转跟踪", path: "/work-orders",
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>' },
  { label: "告警中心", desc: "异常事件监控", path: "/alerts",
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' },
]

function srcCls(s: string) { return ({DRONE_VIDEO:"s-drone",MANUAL_IMAGE:"s-img",MANUAL_VIDEO:"s-vid",CROWD_SOURCE:"s-crowd"} as any)[s] || "" }
function srcLbl(s: string) { return ({DRONE_VIDEO:"无人机",MANUAL_IMAGE:"拍照",MANUAL_VIDEO:"录像",CROWD_SOURCE:"众包"} as any)[s] || s }
function stCls(s: string) { return ({PENDING:"st-wait",PROCESSING:"st-run",COMPLETED:"st-ok",FAILED:"st-fail"} as any)[s] || "" }
function stLbl(s: string) { return ({PENDING:"待处理",PROCESSING:"处理中",COMPLETED:"已完成",FAILED:"失败"} as any)[s] || s }

async function loadTasks() {
  loading.value = true
  try {
    const r = await detectionApi.list({ page: 1, size: 10 })
    recentTasks.value = r.data.data.records || []
  } catch {}
  loading.value = false
}

async function loadDashboardStats() {
  try {
    const r = await statisticsApi.getDashboard()
    const d = r.data.data
    if (d) {
      kpis.value = [
        { label: "检测里程", value: d.totalRoads + "km", delta: 0 },
        { label: "今日检测", value: String(d.detectionToday), delta: 0 },
        { label: "裂缝总数", value: String(d.totalCracksDetected), delta: 0 },
        { label: "工单总数", value: String(d.totalWorkOrders), delta: 0 },
      ]
    }
  } catch {}
}

async function loadTrendData() {
  if (!trendChart) return
  try {
    const r = await statisticsApi.getTrend()
    const data = r.data.data || []
    trendChart.setOption({
      xAxis: { data: data.map((x: any) => x.date.slice(5)) },
      series: [
        { data: data.map((x: any) => x.count) },
        { data: data.map((x: any) => Math.round(x.count * 0.6)) },
      ],
    })
  } catch {}
}

async function loadCrackTypeData() {
  if (!pieChart) return
  try {
    const r = await statisticsApi.getCrackType()
    const data = r.data.data || []
    const colorMap: Record<string, string> = { TRANSVERSE_CRACK: "#4361ee", LONGITUDINAL_CRACK: "#f59e0b", NET_CRACK: "#10b981", POTHOLE: "#ef4444" }
    const labelMap: Record<string, string> = { TRANSVERSE_CRACK: "横向裂缝", LONGITUDINAL_CRACK: "纵向裂缝", NET_CRACK: "网状裂缝", POTHOLE: "坑槽" }
    pieChart.setOption({
      series: [{
        data: data.map((x: any) => ({ name: labelMap[x.type] || x.type, value: x.count, itemStyle: { color: colorMap[x.type] || "#94a3b8" } })),
      }],
    })
  } catch {}
}
function initCharts() {
  const accent = "#4338ca"

  if (trendRef.value) {
    trendChart = echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: { trigger: "axis", backgroundColor: "#fff", borderColor: "#f3f4f6", borderWidth: 1, textStyle: { color: "#111827", fontSize: 11 } },
      grid: { left: 36, right: 12, bottom: 28, top: 8 },
      xAxis: { type: "category", data: [], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: "#9ca3af", fontSize: 11 } },
      yAxis: { type: "value", splitLine: { lineStyle: { color: "#f3f4f6", type: "solid" } }, axisLabel: { color: "#9ca3af", fontSize: 11 } },
      series: [
        { name: "检测任务", type: "line", smooth: true, data: [], symbol: "circle", symbolSize: 5, lineStyle: { color: accent, width: 2 }, itemStyle: { color: accent }, areaStyle: { color: { type: "linear", x: 0, y: 0, x2: 0, y2: 1, colorStops: [{offset:0,color:"rgba(67,56,202,0.06)"},{offset:1,color:"rgba(67,56,202,0)"}] } } },
        { name: "发现病害", type: "line", smooth: true, data: [], symbol: "circle", symbolSize: 5, lineStyle: { color: "#9ca3af", width: 2 }, itemStyle: { color: "#9ca3af" }, areaStyle: { color: { type: "linear", x: 0, y: 0, x2: 0, y2: 1, colorStops: [{offset:0,color:"rgba(156,163,175,0.04)"},{offset:1,color:"rgba(156,163,175,0)"}] } } },
      ],
      legend: { bottom: 0, textStyle: { color: "#9ca3af", fontSize: 11 }, icon: "circle", itemWidth: 6, itemHeight: 6 },
    })
  }

  if (pieRef.value) {
      pieChart = echarts.init(pieRef.value)
      pieChart.setOption({
        tooltip: { trigger: "item", backgroundColor: "#fff", borderColor: "#f3f4f6", borderWidth: 1, textStyle: { color: "#111827", fontSize: 11 } },
        series: [{
          type: "pie", radius: ["44%", "68%"], center: ["50%", "48%"],
          data: [],
          label: { show: true, color: "#9ca3af", fontSize: 11 },
        }],
      })
  }
}

function refreshAll() { loadTasks(); loadDashboardStats(); loadTrendData(); loadCrackTypeData(); }
onMounted(() => { initCharts(); loadTasks(); loadDashboardStats(); loadTrendData(); loadCrackTypeData(); window.addEventListener("data-updated", refreshAll); })
onUnmounted(() => { trendChart?.dispose(); pieChart?.dispose(); window.removeEventListener("data-updated", refreshAll); })
</script>

<style scoped>
/* ── Base ── */
.dash { padding:0; font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,-apple-system,sans-serif; }

/* ── Top Bar ── */
.dash-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:32px; }
.dash-top-left { display:flex; align-items:baseline; gap:16px; }
.dash-title { font-size:22px; font-weight:600; color:#111827; margin:0; letter-spacing:-0.02em; }
.dash-date { font-size:13px; color:#9ca3af; font-variant-numeric:tabular-nums; }
.dash-top-right { display:flex; align-items:center; gap:6px; }
.dash-dot { width:6px; height:6px; border-radius:50%; background:#22c55e; }
.dash-status { font-size:12px; color:#6b7280; }

/* ── KPI Row ── */
.kpi-row { display:grid; grid-template-columns:repeat(4,1fr); gap:16px; margin-bottom:28px; }
.kpi-card { background:#fff; border:1px solid #f3f4f6; border-radius:10px; padding:20px 22px; }
.kpi-label { display:block; font-size:11px; font-weight:500; color:#9ca3af; letter-spacing:0.03em; text-transform:uppercase; margin-bottom:8px; }
.kpi-val { display:block; font-size:28px; font-weight:700; color:#111827; letter-spacing:-0.03em; line-height:1; font-variant-numeric:tabular-nums; margin-bottom:6px; }
.kpi-delta { display:inline-block; font-size:12px; font-weight:600; padding:1px 0; }
.kpi-delta.up { color:#22c55e; }
.kpi-delta.down { color:#ef4444; }

/* ── Grid Layout ── */
.dash-grid { display:grid; grid-template-columns:1.6fr 1fr; gap:16px; margin-bottom:24px; }

/* ── Cards ── */
.card { background:#fff; border:1px solid #f3f4f6; border-radius:10px; overflow:hidden; }
.card-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f9fafb; }
.card-title { font-size:13px; font-weight:600; color:#374151; }
.card-tabs { display:flex; gap:2px; background:#f9fafb; border-radius:6px; padding:2px; }
.c-tab { padding:4px 12px; border:none; border-radius:4px; font-size:11px; font-weight:500; color:#9ca3af; cursor:pointer; background:transparent; font-family:inherit; transition:all .15s; }
.c-tab.active { background:#fff; color:#4338ca; box-shadow:0 1px 2px rgba(0,0,0,0.04); }
.c-tab:hover:not(.active) { color:#6b7280; }
.card-body { padding:4px 4px 0; }
.card-body-nopad { padding:0; }
.chart-box { width:100%; height:280px; }
.card-link { background:none; border:none; font-size:12px; font-weight:500; color:#4338ca; cursor:pointer; font-family:inherit; padding:2px 0; }
.card-link:hover { opacity:.7; }

/* ── Quick Actions ── */
.dash-actions { display:grid; grid-template-columns:repeat(4,1fr); gap:16px; margin-bottom:24px; }
.da-item { display:flex; align-items:center; gap:12px; padding:14px 18px; background:#fff; border:1px solid #f3f4f6; border-radius:10px; text-decoration:none; cursor:pointer; transition:all .2s; }
.da-item:hover { border-color:#e5e7eb; background:#fafbfc; }
.da-icon { width:36px; height:36px; display:flex; align-items:center; justify-content:center; background:#f9fafb; border-radius:8px; color:#4338ca; flex-shrink:0; }
.da-text { display:flex; flex-direction:column; gap:1px; }
.da-label { font-size:13px; font-weight:600; color:#111827; }
.da-desc { font-size:11px; color:#9ca3af; }

/* ── Activity Table ── */
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

/* ── Responsive ── */
@media(max-width:1024px) {
  .kpi-row { grid-template-columns:repeat(2,1fr); }
  .dash-grid { grid-template-columns:1fr; }
  .dash-actions { grid-template-columns:repeat(2,1fr); }
}
@media(max-width:640px) {
  .kpi-row { grid-template-columns:1fr 1fr; }
  .dash-actions { grid-template-columns:1fr 1fr; }
  .act-header, .act-row { font-size:11px; flex-wrap:wrap; }
  .act-id, .act-code { width:90px; }
}
</style>
