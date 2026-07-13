<template>
  <div class="ds-page">
    <!-- Page Header -->
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ t('st.title') }}</h2>
        <p class="page-desc">{{ t('st.desc') }}</p>
      </div>
      <div class="head-right">
        <button class="btn-ghost" @click="refreshAll" :disabled="loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          {{ t('st.refresh') }}
        </button>
        <span class="update-time" v-if="lastUpdate">{{ t('st.updatedAt') }} {{ lastUpdate }}</span>
      </div>
    </div>

    <!-- KPI Cards Row -->
    <div class="kpi-row">
      <div class="kpi-card" v-for="k in kpiCards" :key="k.label">
        <div class="kpi-icon" :style="{ background: k.bg, color: k.color }">
          <span v-html="k.icon"></span>
        </div>
        <div class="kpi-body">
          <span class="kpi-val">{{ k.value }}</span>
          <span class="kpi-label">{{ k.label }}</span>
        </div>
      </div>
    </div>

    <!-- Charts Row 1: Trend + Type Pie -->
    <div class="chart-row">
      <div class="chart-card chart-wide">
        <div class="card-head">
          <span class="card-title">{{ t('st.detectionTrend') }}</span>
          <div class="card-tabs">
            <button v-for="t in trendTabs" :key="t.key" :class="['c-tab', { active: activeTrendTab === t.key }]" @click="switchTrend(t.key)">{{ t.label }}</button>
          </div>
        </div>
        <div class="card-body">
          <div ref="trendRef" class="chart-box"></div>
        </div>
      </div>
      <div class="chart-card chart-narrow">
        <div class="card-head">
          <span class="card-title">{{ t('st.crackTypeRatio') }}</span>
        </div>
        <div class="card-body">
          <div ref="typePieRef" class="chart-box"></div>
        </div>
      </div>
    </div>

    <!-- Charts Row 2: Severity Bar + Map Trend -->
    <div class="chart-row">
      <div class="chart-card chart-half">
        <div class="card-head">
          <span class="card-title">{{ t('st.severityDist') }}</span>
        </div>
        <div class="card-body">
          <div ref="severityRef" class="chart-box"></div>
        </div>
      </div>
      <div class="chart-card chart-half">
        <div class="card-head">
          <span class="card-title">{{ t('st.mapTrend') }}</span>
        </div>
        <div class="card-body">
          <div ref="mapTrendRef" class="chart-box"></div>
        </div>
      </div>
    </div>

    <!-- Map Statistics Cards -->
    <div class="section-head">
      <h3 class="section-title">{{ t('st.mapMarkerStats') }}</h3>
      <span class="section-sub">{{ t('st.mapMarkerSub') }}</span>
    </div>
    <div class="map-stat-row">
      <div class="map-stat-card" v-for="m in mapStatCards" :key="m.label">
        <div class="ms-icon" :style="{ background: m.bg, color: m.color }">
          <span v-html="m.icon"></span>
        </div>
        <div class="ms-body">
          <span class="ms-val">{{ m.value }}</span>
          <span class="ms-label">{{ m.label }}</span>
        </div>
      </div>
    </div>

    <!-- Tables Row: Department Workload + Damage Type Ratio -->
    <div class="table-row">
      <div class="table-card">
        <div class="card-head">
          <span class="card-title">{{ t('st.deptWorkload') }}</span>
        </div>
        <div class="table-wrap">
          <table class="ds-table">
            <thead>
              <tr>
                <th>{{ t('st.deptName') }}</th>
                <th>{{ t('st.totalCount') }}</th>
                <th>{{ t('st.completedCount') }}</th>
                <th>{{ t('st.pendingCount') }}</th>
                <th>{{ t('st.completionRate') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in deptWorkload" :key="d.deptName">
                <td class="td-dept">{{ d.deptName }}</td>
                <td><span class="num-tag">{{ d.total }}</span></td>
                <td><span class="num-tag num-success">{{ d.completed }}</span></td>
                <td><span class="num-tag num-warn">{{ d.pending }}</span></td>
                <td>
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: deptRate(d) + '%' }"></div>
                    <span class="progress-text">{{ deptRate(d) }}%</span>
                  </div>
                </td>
              </tr>
              <tr v-if="!loading && deptWorkload.length === 0">
                <td colspan="5" class="empty-row">{{ t('st.noDeptData') }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="table-card">
        <div class="card-head">
          <span class="card-title">{{ t('st.damageTypeRatio') }}</span>
        </div>
        <div class="table-wrap">
          <table class="ds-table">
            <thead>
              <tr>
                <th>{{ t('st.damageType') }}</th>
                <th>{{ t('st.count') }}</th>
                <th>{{ t('st.ratio') }}</th>
                <th>{{ t('st.distribution') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in damageTypes" :key="d.damageType">
                <td class="td-title">{{ damageTypeLabel(d.damageType) }}</td>
                <td><span class="num-tag">{{ d.count }}</span></td>
                <td>{{ (d.ratio * 100).toFixed(1) }}%</td>
                <td>
                  <div class="progress-bar">
                    <div class="progress-fill progress-blue" :style="{ width: (d.ratio * 100) + '%' }"></div>
                  </div>
                </td>
              </tr>
              <tr v-if="!loading && damageTypes.length === 0">
                <td colspan="4" class="empty-row">{{ t('st.noDamageTypeData') }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Road TOP5 Ranking -->
    <div class="section-head">
      <h3 class="section-title">{{ t('st.roadRanking') }}</h3>
      <span class="section-sub">{{ t('st.roadRankingSub') }}</span>
    </div>
    <div class="road-rank-row">
      <div class="road-rank-card" v-for="(r, idx) in roadTop5" :key="r.roadId">
        <div class="rank-badge" :class="rankClass(idx)">{{ idx + 1 }}</div>
        <div class="road-info">
          <span class="road-name">{{ r.roadName }}</span>
          <span class="road-sev" :class="severityClass(r.overallSeverity)">{{ severityLabel(r.overallSeverity) }}</span>
        </div>
        <div class="road-counts">
          <div class="rc-item">
            <span class="rc-num rc-total">{{ r.totalCount }}</span>
            <span class="rc-lbl">{{ t('st.total') }}</span>
          </div>
          <div class="rc-item">
            <span class="rc-num rc-high">{{ r.highCount }}</span>
            <span class="rc-lbl">{{ t('st.severity') }}</span>
          </div>
          <div class="rc-item">
            <span class="rc-num rc-med">{{ r.mediumCount }}</span>
            <span class="rc-lbl">{{ t('st.medium') }}</span>
          </div>
          <div class="rc-item">
            <span class="rc-num rc-low">{{ r.lowCount }}</span>
            <span class="rc-lbl">{{ t('st.low') }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading Overlay -->
    <div v-if="loading && !hasData" class="loading-state">
      <div class="loading-spinner"></div>
      <span>{{ t('st.loading') }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, computed } from "vue"
import * as echarts from "echarts"
import { ElMessage } from "element-plus"
import { statisticsApi } from "@/api/statistics"
import { mapApi } from "@/api/map"
import { t } from "@/i18n"
import type {
  DashboardStatsResponse,
  TrendStatsItem,
  CrackTypeStatItem,
  SeverityStatItem,
  DeptWorkloadItem,
  MapStatisticsResponse,
  MapTrendPointResponse,
  MapDamageTypeRatioResponse,
  RoadDiseaseSummaryResponse,
} from "@/types"

// --- Refs ---
const loading = ref(false)
const hasData = ref(false)
const lastUpdate = ref("")
const activeTrendTab = ref(30)

const trendRef = ref<HTMLElement>()
const typePieRef = ref<HTMLElement>()
const severityRef = ref<HTMLElement>()
const mapTrendRef = ref<HTMLElement>()

let trendChart: echarts.ECharts | null = null
let typePieChart: echarts.ECharts | null = null
let severityChart: echarts.ECharts | null = null
let mapTrendChart: echarts.ECharts | null = null

// --- Data ---
const dashboard = ref<DashboardStatsResponse>({})
const trendData = ref<TrendStatsItem[]>([])
const crackTypes = ref<CrackTypeStatItem[]>([])
const severityData = ref<SeverityStatItem[]>([])
const deptWorkload = ref<DeptWorkloadItem[]>([])
const mapStats = ref<MapStatisticsResponse | null>(null)
const mapTrend = ref<MapTrendPointResponse[]>([])
const damageTypes = ref<MapDamageTypeRatioResponse[]>([])
const roadDisease = ref<RoadDiseaseSummaryResponse[]>([])

const trendTabs = [
  { key: 7, label: t('st.last7days') },
  { key: 30, label: t('st.last30days') },
  { key: 90, label: t('st.last90days') },
]

// --- KPI Cards ---
const kpiCards = computed(() => {
  const d = dashboard.value
  return [
    {
      label: t('st.totalRoads'),
      value: d.totalRoads ?? 0,
      bg: "#eef2ff", color: "#4361ee",
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 12h18M3 6h18M3 18h18"/></svg>',
    },
    {
      label: t('st.monitoredRoads'),
      value: d.monitoredRoads ?? 0,
      bg: "#f0fdf4", color: "#16a34a",
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M12 1v6m0 10v6m11-11h-6M7 12H1"/></svg>',
    },
    {
      label: t('st.todayDetection'),
      value: d.detectionToday ?? 0,
      bg: "#fef3c7", color: "#d97706",
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>',
    },
    {
      label: t('st.pendingAlerts'),
      value: d.pendingAlerts ?? 0,
      bg: "#fef2f2", color: "#dc2626",
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
    },
    {
      label: t('st.totalDamages'),
      value: d.totalCracksDetected ?? 0,
      bg: "#fce7f3", color: "#e11d48",
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10M18 20V4M6 20v-6"/></svg>',
    },
    {
      label: t('st.totalOrders'),
      value: d.totalWorkOrders ?? 0,
      bg: "#f1f5f9", color: "#475569",
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>',
    },
  ]
})

// --- Map Stat Cards ---
const mapStatCards = computed(() => {
  const m = mapStats.value
  return [
    {
      label: t('st.totalMarkers'),
      value: m?.totalMarkers ?? 0,
      bg: "#eef2ff", color: "#4361ee",
      icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>',
    },
    {
      label: t('st.newMarkers'),
      value: m?.newMarkers ?? 0,
      bg: "#f0fdf4", color: "#16a34a",
      icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>',
    },
    {
      label: t('st.repaired'),
      value: m?.repairedCount ?? 0,
      bg: "#dcfce7", color: "#15803d",
      icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg>',
    },
    {
      label: t('st.pendingRepair'),
      value: m?.pendingRepair ?? 0,
      bg: "#fef3c7", color: "#d97706",
      icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
    },
    {
      label: t('st.severity'),
      value: m?.highSeverityCount ?? 0,
      bg: "#fef2f2", color: "#dc2626",
      icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
    },
    {
      label: t('st.medium'),
      value: m?.mediumSeverityCount ?? 0,
      bg: "#fffbeb", color: "#d97706",
      icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
    },
  ]
})

// --- Road TOP5 ---
const roadTop5 = computed(() => {
  return [...roadDisease.value]
    .sort((a, b) => b.totalCount - a.totalCount)
    .slice(0, 5)
})

// --- Helpers ---
function damageTypeLabel(tp: string) {
  return ({ CRACK: t('damage.crack'), TRANSVERSE_CRACK: t('damage.transverseCrack'), LONGITUDINAL_CRACK: t('damage.longitudinalCrack'), NET_CRACK: t('damage.netCrack'), POTHOLE: t('damage.pothole'), MARKING_DAMAGE: t('damage.markingDamage'), ROAD_SPILL: t('damage.roadSpill'), UNKNOWN: t('damage.unknown') } as any)[tp] || tp || "--"
}

function severityLabel(s: string) {
  return ({ HIGH: t('severity.high'), MEDIUM: t('severity.medium'), LOW: t('severity.low'), UNKNOWN: t('damage.unknown') } as any)[s] || s || "--"
}

function severityClass(s: string) {
  return ({ HIGH: "sev-high", MEDIUM: "sev-med", LOW: "sev-low" } as any)[s] || "sev-default"
}

function rankClass(idx: number) {
  return ["rank-1", "rank-2", "rank-3", "rank-4", "rank-5"][idx] || "rank-5"
}

function deptRate(d: DeptWorkloadItem) {
  if (!d.total) return 0
  return Math.round((d.completed / d.total) * 100)
}

// --- Data Loading ---
async function loadDashboard() {
  try {
    const r = await statisticsApi.getDashboard()
    dashboard.value = r.data.data || {}
  } catch { /* silent */ }
}

async function loadTrend() {
  try {
    const r = await statisticsApi.getTrend(activeTrendTab.value)
    trendData.value = r.data.data || []
    renderTrend()
  } catch { /* silent */ }
}

async function loadCrackTypes() {
  try {
    const r = await statisticsApi.getCrackType()
    crackTypes.value = r.data.data || []
    renderTypePie()
  } catch { /* silent */ }
}

async function loadSeverity() {
  try {
    const r = await statisticsApi.getSeverity()
    severityData.value = r.data.data || []
    renderSeverity()
  } catch { /* silent */ }
}

async function loadDeptWorkload() {
  try {
    const r = await statisticsApi.getDepartmentWorkload()
    deptWorkload.value = r.data.data || []
  } catch { /* silent */ }
}

async function loadMapStats() {
  try {
    const r = await mapApi.statistics()
    mapStats.value = r.data.data
  } catch { /* silent */ }
}

async function loadMapTrend() {
  try {
    const r = await mapApi.trend(7)
    mapTrend.value = r.data.data || []
    renderMapTrend()
  } catch { /* silent */ }
}

async function loadDamageTypes() {
  try {
    const r = await mapApi.damageTypes()
    damageTypes.value = r.data.data || []
  } catch { /* silent */ }
}

async function loadRoadDisease() {
  try {
    const r = await mapApi.roadsWithDisease()
    roadDisease.value = r.data.data || []
  } catch { /* silent */ }
}

async function refreshAll() {
  loading.value = true
  await Promise.allSettled([
    loadDashboard(),
    loadTrend(),
    loadCrackTypes(),
    loadSeverity(),
    loadDeptWorkload(),
    loadMapStats(),
    loadMapTrend(),
    loadDamageTypes(),
    loadRoadDisease(),
  ])
  lastUpdate.value = new Date().toLocaleTimeString("zh-CN", { hour12: false })
  hasData.value = true
  loading.value = false
}

function switchTrend(key: number) {
  activeTrendTab.value = key
  loadTrend()
}

// --- Chart Rendering ---
const chartTheme = {
  textColor: "#64748b",
  splitColor: "#f1f5f9",
  fontSize: 11,
  fontFamily: "Inter,-apple-system,sans-serif",
}

function renderTrend() {
  if (!trendRef.value) return
  if (!trendChart) trendChart = echarts.init(trendRef.value)
  const dates = trendData.value.map(d => d.date?.substring(5) || "")
  const counts = trendData.value.map(d => d.count || 0)
  trendChart.setOption({
    tooltip: { trigger: "axis", backgroundColor: "#fff", borderColor: "#e2e8f0", textStyle: { color: "#1e293b", fontSize: 12 } },
    grid: { left: 45, right: 20, bottom: 30, top: 20 },
    xAxis: { type: "category", data: dates, axisLabel: { color: chartTheme.textColor, fontSize: chartTheme.fontSize }, axisLine: { lineStyle: { color: "#e2e8f0" } } },
    yAxis: { type: "value", axisLabel: { color: chartTheme.textColor, fontSize: chartTheme.fontSize }, splitLine: { lineStyle: { color: chartTheme.splitColor } } },
    series: [{
      type: "bar",
      data: counts,
      barMaxWidth: 24,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: "#4361ee" },
          { offset: 1, color: "#93c5fd" },
        ]),
        borderRadius: [4, 4, 0, 0],
      },
    }],
  }, true)
}

function renderTypePie() {
  if (!typePieRef.value) return
  if (!typePieChart) typePieChart = echarts.init(typePieRef.value)
  const data = crackTypes.value.map(d => ({
    name: damageTypeLabel(d.type),
    value: d.count,
  }))
  const colors = ["#4361ee", "#16a34a", "#d97706", "#dc2626", "#e11d48", "#475569", "#2563eb", "#94a3b8"]
  typePieChart.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)", backgroundColor: "#fff", borderColor: "#e2e8f0", textStyle: { color: "#1e293b", fontSize: 12 } },
    legend: { orient: "vertical", right: 10, top: "center", textStyle: { color: chartTheme.textColor, fontSize: chartTheme.fontSize }, itemWidth: 10, itemHeight: 10 },
    color: colors,
    series: [{
      type: "pie",
      radius: ["38%", "65%"],
      center: ["38%", "50%"],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 600, color: "#1e293b" } },
      data,
    }],
  }, true)
}

function renderSeverity() {
  if (!severityRef.value) return
  if (!severityChart) severityChart = echarts.init(severityRef.value)
  const data = severityData.value.map(d => ({
    name: ({ HIGH: t('severity.high'), MEDIUM: t('severity.medium'), LOW: t('severity.low') } as any)[d.level] || d.level,
    value: d.count,
    percentage: d.percentage,
    level: d.level,
  }))
  const colors = data.map(d => {
    if (d.level === "HIGH") return "#dc2626"
    if (d.level === "MEDIUM") return "#d97706"
    return "#16a34a"
  })
  severityChart.setOption({
    tooltip: { trigger: "axis", backgroundColor: "#fff", borderColor: "#e2e8f0", textStyle: { color: "#1e293b", fontSize: 12 }, formatter: (params: any) => {
      const p = params[0]
      const item = data[p.dataIndex]
      return `${p.name}: ${p.value} (${(item?.percentage * 100).toFixed(1)}%)`
    }},
    grid: { left: 45, right: 20, bottom: 30, top: 20 },
    xAxis: { type: "category", data: data.map(d => d.name), axisLabel: { color: chartTheme.textColor, fontSize: chartTheme.fontSize }, axisLine: { lineStyle: { color: "#e2e8f0" } } },
    yAxis: { type: "value", axisLabel: { color: chartTheme.textColor, fontSize: chartTheme.fontSize }, splitLine: { lineStyle: { color: chartTheme.splitColor } } },
    series: [{
      type: "bar",
      data: data.map(d => d.value),
      barMaxWidth: 40,
      itemStyle: { color: (params: any) => colors[params.dataIndex], borderRadius: [4, 4, 0, 0] },
    }],
  }, true)
}

function renderMapTrend() {
  if (!mapTrendRef.value) return
  if (!mapTrendChart) mapTrendChart = echarts.init(mapTrendRef.value)
  const dates = mapTrend.value.map(d => d.date?.substring(5) || "")
  const newCounts = mapTrend.value.map(d => d.count || 0)
  const repairedCounts = mapTrend.value.map(d => d.repairedCount || 0)
  mapTrendChart.setOption({
    tooltip: { trigger: "axis", backgroundColor: "#fff", borderColor: "#e2e8f0", textStyle: { color: "#1e293b", fontSize: 12 } },
    legend: { data: [t('st.newMarkers'), t('st.repaired')], right: 10, top: 0, textStyle: { color: chartTheme.textColor, fontSize: chartTheme.fontSize }, itemWidth: 12, itemHeight: 8 },
    grid: { left: 45, right: 20, bottom: 30, top: 35 },
    xAxis: { type: "category", data: dates, axisLabel: { color: chartTheme.textColor, fontSize: chartTheme.fontSize }, axisLine: { lineStyle: { color: "#e2e8f0" } } },
    yAxis: { type: "value", axisLabel: { color: chartTheme.textColor, fontSize: chartTheme.fontSize }, splitLine: { lineStyle: { color: chartTheme.splitColor } } },
    series: [
      {
        name: t('st.newMarkers'),
        type: "line",
        data: newCounts,
        smooth: true,
        symbol: "circle",
        symbolSize: 6,
        lineStyle: { width: 2, color: "#4361ee" },
        itemStyle: { color: "#4361ee" },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "rgba(67,97,238,0.15)" }, { offset: 1, color: "rgba(67,97,238,0)" }]) },
      },
      {
        name: t('st.repaired'),
        type: "line",
        data: repairedCounts,
        smooth: true,
        symbol: "circle",
        symbolSize: 6,
        lineStyle: { width: 2, color: "#16a34a" },
        itemStyle: { color: "#16a34a" },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "rgba(22,163,74,0.15)" }, { offset: 1, color: "rgba(22,163,74,0)" }]) },
      },
    ],
  }, true)
}

// --- Resize Handler ---
function handleResize() {
  trendChart?.resize()
  typePieChart?.resize()
  severityChart?.resize()
  mapTrendChart?.resize()
}

// --- Lifecycle ---
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await refreshAll()
  await nextTick()
  // Re-render after DOM is ready
  renderTrend()
  renderTypePie()
  renderSeverity()
  renderMapTrend()
  window.addEventListener("resize", handleResize)
  // Auto-refresh every 60s
  pollTimer = setInterval(refreshAll, 60000)
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  if (pollTimer) clearInterval(pollTimer)
  trendChart?.dispose()
  typePieChart?.dispose()
  severityChart?.dispose()
  mapTrendChart?.dispose()
})
</script>

<style scoped>
.ds-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }

.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }
.head-right { display:flex; align-items:center; gap:12px; }
.update-time { font-size:11px; color:#94a3b8; }

/* KPI Cards */
.kpi-row { display:grid; grid-template-columns:repeat(6,1fr); gap:12px; margin-bottom:20px; }
.kpi-card { display:flex; align-items:center; gap:12px; padding:16px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; transition:border-color .15s; }
.kpi-card:hover { border-color:#d0d5e0; }
.kpi-icon { width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.kpi-body { display:flex; flex-direction:column; gap:2px; min-width:0; }
.kpi-val { font-size:24px; font-weight:800; color:#0f172a; line-height:1; }
.kpi-label { font-size:11px; color:#94a3b8; white-space:nowrap; }

/* Chart Rows */
.chart-row { display:flex; gap:16px; margin-bottom:20px; }
.chart-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.chart-wide { flex:2; }
.chart-narrow { flex:1; }
.chart-half { flex:1; }
.card-head { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f0f2f5; }
.card-title { font-size:14px; font-weight:600; color:#1a202c; }
.card-tabs { display:flex; gap:4px; }
.c-tab { padding:4px 12px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:11px; color:#64748b; cursor:pointer; font-family:inherit; transition:all .15s; }
.c-tab:hover { border-color:#2563eb; color:#2563eb; }
.c-tab.active { background:#2563eb; border-color:#2563eb; color:#fff; }
.card-body { padding:12px 18px 18px; }
.chart-box { height:300px; }

/* Section Head */
.section-head { display:flex; align-items:baseline; gap:10px; margin-bottom:14px; }
.section-title { font-size:15px; font-weight:700; color:#0f172a; margin:0; }
.section-sub { font-size:12px; color:#94a3b8; }

/* Map Stat Cards */
.map-stat-row { display:grid; grid-template-columns:repeat(6,1fr); gap:12px; margin-bottom:20px; }
.map-stat-card { display:flex; align-items:center; gap:10px; padding:14px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.ms-icon { width:34px; height:34px; display:flex; align-items:center; justify-content:center; border-radius:8px; flex-shrink:0; }
.ms-body { display:flex; flex-direction:column; gap:1px; }
.ms-val { font-size:20px; font-weight:800; color:#0f172a; line-height:1; }
.ms-label { font-size:10px; color:#94a3b8; }

/* Table Row */
.table-row { display:flex; gap:16px; margin-bottom:20px; }
.table-card { flex:1; background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.table-wrap { overflow-x:auto; }
.ds-table { width:100%; border-collapse:collapse; font-size:13px; }
.ds-table thead { background:#f8f9fc; }
.ds-table th { padding:10px 14px; text-align:left; font-weight:600; font-size:11px; color:#64748b; letter-spacing:0.03em; border-bottom:1px solid #eef0f4; }
.ds-table td { padding:10px 14px; border-bottom:1px solid #f5f6f8; color:#1e293b; }
.ds-table tbody tr:hover { background:#fafbfc; }
.td-title { font-weight:500; }
.td-dept { font-size:12px; color:#4361ee; font-weight:500; }
.num-tag { display:inline-block; min-width:28px; text-align:center; padding:2px 8px; border-radius:4px; background:#f1f5f9; font-size:12px; font-weight:600; color:#475569; }
.num-success { background:#f0fdf4; color:#16a34a; }
.num-warn { background:#fffbeb; color:#d97706; }
.empty-row { text-align:center; color:#94a3b8; padding:40px 0 !important; font-size:13px; }

/* Progress Bar */
.progress-bar { position:relative; width:100%; height:18px; background:#f1f5f9; border-radius:4px; overflow:hidden; }
.progress-fill { height:100%; background:linear-gradient(90deg,#4361ee,#93c5fd); border-radius:4px; transition:width .3s; }
.progress-blue { background:linear-gradient(90deg,#2563eb,#93c5fd); }
.progress-text { position:absolute; right:6px; top:50%; transform:translateY(-50%); font-size:10px; font-weight:600; color:#1e293b; }

/* Road Ranking */
.road-rank-row { display:grid; grid-template-columns:repeat(5,1fr); gap:12px; }
.road-rank-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; padding:16px; display:flex; flex-direction:column; gap:12px; }
.rank-badge { width:28px; height:28px; display:flex; align-items:center; justify-content:center; border-radius:8px; font-size:14px; font-weight:800; color:#fff; }
.rank-1 { background:#dc2626; }
.rank-2 { background:#ea580c; }
.rank-3 { background:#d97706; }
.rank-4 { background:#64748b; }
.rank-5 { background:#94a3b8; }
.road-info { display:flex; flex-direction:column; gap:4px; }
.road-name { font-size:13px; font-weight:600; color:#0f172a; }
.road-sev { display:inline-block; width:fit-content; padding:1px 8px; border-radius:4px; font-size:10px; font-weight:600; }
.sev-high { background:#fef2f2; color:#dc2626; }
.sev-med { background:#fffbeb; color:#d97706; }
.sev-low { background:#f0fdf4; color:#16a34a; }
.sev-default { background:#f8f9fc; color:#94a3b8; }
.road-counts { display:grid; grid-template-columns:repeat(4,1fr); gap:6px; }
.rc-item { display:flex; flex-direction:column; align-items:center; gap:1px; }
.rc-num { font-size:16px; font-weight:800; line-height:1; }
.rc-total { color:#475569; }
.rc-high { color:#dc2626; }
.rc-med { color:#d97706; }
.rc-low { color:#16a34a; }
.rc-lbl { font-size:10px; color:#94a3b8; }

/* Buttons */
.btn-ghost { display:inline-flex; align-items:center; gap:4px; padding:6px 14px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.btn-ghost:disabled { opacity:.5; cursor:not-allowed; }

/* Loading */
.loading-state { display:flex; flex-direction:column; align-items:center; gap:12px; padding:60px 0; color:#94a3b8; font-size:13px; }
.loading-spinner { width:28px; height:28px; border:3px solid #e2e8f0; border-top-color:#4361ee; border-radius:50%; animation:spin .8s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
</style>
