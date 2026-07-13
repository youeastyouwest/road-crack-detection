<template>
  <div class="report-page">
    <!-- Page Header -->
    <div class="page-head">
      <div>
        <h2 class="page-title">分析报告</h2>
        <p class="page-desc">基于检测数据的智能分析与评估报告</p>
      </div>
      <div class="head-right">
        <span class="report-date">{{ reportDate }}</span>
        <button class="btn-ghost" @click="loadData" :disabled="loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          刷新
        </button>
      </div>
    </div>

    <div v-loading="loading">
      <!-- Overview KPI -->
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

      <!-- Charts Row 1: Trend + Crack Type -->
      <div class="chart-row">
        <div class="chart-card chart-wide">
          <div class="card-head">
            <span class="card-title">检测趋势（近30天）</span>
          </div>
          <div class="card-body">
            <div ref="trendRef" class="chart-box"></div>
          </div>
        </div>
        <div class="chart-card chart-narrow">
          <div class="card-head">
            <span class="card-title">病害类型占比</span>
          </div>
          <div class="card-body">
            <div ref="typeRef" class="chart-box"></div>
          </div>
        </div>
      </div>

      <!-- Charts Row 2: Severity + Dept Workload -->
      <div class="chart-row">
        <div class="chart-card chart-half">
          <div class="card-head">
            <span class="card-title">严重等级分布</span>
          </div>
          <div class="card-body">
            <div ref="severityRef" class="chart-box"></div>
          </div>
        </div>
        <div class="chart-card chart-half">
          <div class="card-head">
            <span class="card-title">部门工作负载</span>
          </div>
          <div class="card-body">
            <div ref="deptRef" class="chart-box"></div>
          </div>
        </div>
      </div>

      <!-- Analysis Summary -->
      <div class="summary-section">
        <div class="summary-head">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          <span>智能分析结论</span>
        </div>
        <div class="summary-body">
          <div class="summary-item" v-for="(item, idx) in analysisConclusions" :key="idx">
            <div :class="['conclusion-icon', item.type]">
              <span v-if="item.type === 'warning'">!</span>
              <span v-else-if="item.type === 'success'">&#10003;</span>
              <span v-else-if="item.type === 'info'">i</span>
              <span v-else>&#9654;</span>
            </div>
            <div class="conclusion-content">
              <div class="conclusion-title">{{ item.title }}</div>
              <div class="conclusion-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from "vue"
import { ElMessage } from "element-plus"
import * as echarts from "echarts"
import { statisticsApi } from "@/api"
import type { DashboardStatsResponse, TrendStatsItem, CrackTypeStatItem, SeverityStatItem, DeptWorkloadItem } from "@/types"

const loading = ref(false)
const reportDate = new Date().toLocaleDateString("zh-CN", { year: "numeric", month: "long", day: "numeric" })

const dashboard = ref<DashboardStatsResponse>({})
const trendData = ref<TrendStatsItem[]>([])
const crackTypeData = ref<CrackTypeStatItem[]>([])
const severityData = ref<SeverityStatItem[]>([])
const deptData = ref<DeptWorkloadItem[]>([])

const trendRef = ref<HTMLElement>()
const typeRef = ref<HTMLElement>()
const severityRef = ref<HTMLElement>()
const deptRef = ref<HTMLElement>()

const kpiCards = computed(() => [
  { label: "道路总数", value: dashboard.value.totalRoads ?? "—", color: "#3b82f6", bg: "#eff6ff",
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12h18M3 6h18M3 18h18"/></svg>' },
  { label: "今日检测", value: dashboard.value.detectionToday ?? "—", color: "#8b5cf6", bg: "#f5f3ff",
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>' },
  { label: "病害检出", value: dashboard.value.totalCracksDetected ?? "—", color: "#f59e0b", bg: "#fffbeb",
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5M2 12l10 5 10-5"/></svg>' },
  { label: "工单总数", value: dashboard.value.totalWorkOrders ?? "—", color: "#10b981", bg: "#ecfdf5",
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>' },
])

const analysisConclusions = computed(() => {
  const conclusions: { type: string; title: string; desc: string }[] = []

  // Severity analysis
  const high = severityData.value.find(s => s.level === "HIGH")
  const low = severityData.value.find(s => s.level === "LOW")
  const totalCracks = severityData.value.reduce((sum, s) => sum + s.count, 0)
  if (high && high.count > 0) {
    const pct = (high.percentage || 0).toFixed(1)
    conclusions.push({
      type: "warning",
      title: `严重损坏占比 ${pct}%`,
      desc: `当前检出严重等级病害 ${high.count} 处（占比 ${pct}%），建议优先安排维修资源处理 HIGH 级别损坏路段。`
    })
  }
  if (low && low.count > 0 && totalCracks > 0) {
    const pct = (low.percentage || 0).toFixed(1)
    conclusions.push({
      type: "info",
      title: `轻微损坏 ${low.count} 处`,
      desc: `一般级别病害占比 ${pct}%，暂不影响通行安全，建议纳入定期巡查计划持续观察。`
    })
  }

  // Trend analysis
  const recentTrend = trendData.value.slice(-7)
  const recentTotal = recentTrend.reduce((sum, t) => sum + t.count, 0)
  const prevTrend = trendData.value.slice(-14, -7)
  const prevTotal = prevTrend.reduce((sum, t) => sum + t.count, 0)
  if (recentTotal > prevTotal && prevTotal > 0) {
    const increase = ((recentTotal - prevTotal) / prevTotal * 100).toFixed(1)
    conclusions.push({
      type: "warning",
      title: `近7天检测量上升 ${increase}%`,
      desc: `本周检测任务量较上周增加 ${increase}%，检测频率提升有助于更早发现路面病害。`
    })
  } else if (recentTotal < prevTotal && recentTotal > 0) {
    const decrease = ((prevTotal - recentTotal) / prevTotal * 100).toFixed(1)
    conclusions.push({
      type: "success",
      title: `近7天检测量下降 ${decrease}%`,
      desc: `本周检测任务量较上周减少 ${decrease}%，建议保持检测频率，确保道路覆盖率。`
    })
  } else if (recentTotal > 0) {
    conclusions.push({
      type: "info",
      title: "检测量保持稳定",
      desc: `近7天共完成 ${recentTotal} 次检测，检测频率保持稳定。`
    })
  }

  // Department workload analysis
  for (const dept of deptData.value) {
    if (dept.pending > 0) {
      conclusions.push({
        type: dept.pending > 5 ? "warning" : "info",
        title: `${deptName(dept.deptName)} 待处理 ${dept.pending} 件`,
        desc: `该部门共有 ${dept.total} 件工单，已完成 ${dept.completed} 件，待处理 ${dept.pending} 件，完成率 ${dept.total > 0 ? (dept.completed / dept.total * 100).toFixed(1) : 0}%。`
      })
    } else if (dept.total > 0) {
      conclusions.push({
        type: "success",
        title: `${deptName(dept.deptName)} 工单全部完成`,
        desc: `该部门 ${dept.total} 件工单已全部处理完毕，工作效率良好。`
      })
    }
  }

  // Crack type analysis
  if (crackTypeData.value.length > 0) {
    const top = [...crackTypeData.value].sort((a, b) => b.count - a.count)[0]
    conclusions.push({
      type: "info",
      title: `主要病害类型: ${damageTypeLabel(top.type)}`,
      desc: `${damageTypeLabel(top.type)}占比最高，共检出 ${top.count} 处（${(top.percentage || 0).toFixed(1)}%），建议针对该类型病害制定专项维修方案。`
    })
  }

  if (conclusions.length === 0) {
    conclusions.push({
      type: "info",
      title: "暂无足够数据生成分析结论",
      desc: "请先进行路面检测任务，系统将自动生成分析报告。"
    })
  }

  return conclusions
})

function deptName(code: string) {
  return ({ ROAD_ADMIN: "公路管理部门", SANIT_ADMIN: "环卫管理部门", TRAFFIC_ADMIN: "交通管理部门" } as any)[code] || code
}
function damageTypeLabel(type: string) {
  return ({ CRACK: "裂缝", POTHOLE: "坑槽", ALLIGATOR: "龟裂", BLOCK: "块裂", RUTTING: "车辙", REPAIR: "修补" } as any)[type] || type
}

async function loadData() {
  loading.value = true
  try {
    const [dash, trend, crack, sev, dept] = await Promise.all([
      statisticsApi.getDashboard(),
      statisticsApi.getTrend(30),
      statisticsApi.getCrackType(),
      statisticsApi.getSeverity(),
      statisticsApi.getDepartmentWorkload(),
    ])
    dashboard.value = dash.data.data
    trendData.value = trend.data.data
    crackTypeData.value = crack.data.data
    severityData.value = sev.data.data
    deptData.value = dept.data.data

    await nextTick()
    renderCharts()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "加载分析数据失败")
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  // Trend chart
  if (trendRef.value) {
    const chart = echarts.init(trendRef.value)
    chart.setOption({
      tooltip: { trigger: "axis" },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: {
        type: "category",
        data: trendData.value.map(t => t.date.substring(5)),
        axisLine: { lineStyle: { color: "#e2e8f0" } },
        axisLabel: { color: "#94a3b8", fontSize: 11 },
      },
      yAxis: {
        type: "value",
        axisLine: { show: false },
        splitLine: { lineStyle: { color: "#f1f5f9" } },
        axisLabel: { color: "#94a3b8", fontSize: 11 },
      },
      series: [{
        type: "line",
        data: trendData.value.map(t => t.count),
        smooth: true,
        symbol: "circle",
        symbolSize: 4,
        lineStyle: { color: "#3b82f6", width: 2 },
        itemStyle: { color: "#3b82f6" },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(59,130,246,0.2)" },
            { offset: 1, color: "rgba(59,130,246,0)" },
          ]),
        },
      }],
    })
    window.addEventListener("resize", () => chart.resize())
  }

  // Crack type pie
  if (typeRef.value) {
    const chart = echarts.init(typeRef.value)
    chart.setOption({
      tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
      legend: { bottom: 0, textStyle: { color: "#94a3b8", fontSize: 11 } },
      series: [{
        type: "pie",
        radius: ["40%", "65%"],
        center: ["50%", "45%"],
        label: { show: false },
        data: crackTypeData.value.map(c => ({
          name: damageTypeLabel(c.type),
          value: c.count,
        })),
        color: ["#3b82f6", "#f59e0b", "#10b981", "#8b5cf6", "#ef4444", "#6366f1"],
      }],
    })
    window.addEventListener("resize", () => chart.resize())
  }

  // Severity bar
  if (severityRef.value) {
    const chart = echarts.init(severityRef.value)
    chart.setOption({
      tooltip: { trigger: "axis" },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: {
        type: "category",
        data: severityData.value.map(s => ({ HIGH: "严重", MEDIUM: "中等", LOW: "一般" } as any)[s.level] || s.level),
        axisLine: { lineStyle: { color: "#e2e8f0" } },
        axisLabel: { color: "#94a3b8", fontSize: 11 },
      },
      yAxis: {
        type: "value",
        axisLine: { show: false },
        splitLine: { lineStyle: { color: "#f1f5f9" } },
        axisLabel: { color: "#94a3b8", fontSize: 11 },
      },
      series: [{
        type: "bar",
        data: severityData.value.map(s => ({
          value: s.count,
          itemStyle: {
            color: s.level === "HIGH" ? "#dc2626" : s.level === "MEDIUM" ? "#d97706" : "#6366f1",
          },
        })),
        barWidth: "40%",
        label: { show: true, position: "top", color: "#64748b", fontSize: 11 },
      }],
    })
    window.addEventListener("resize", () => chart.resize())
  }

  // Dept workload bar
  if (deptRef.value) {
    const chart = echarts.init(deptRef.value)
    chart.setOption({
      tooltip: { trigger: "axis" },
      legend: { bottom: 0, textStyle: { color: "#94a3b8", fontSize: 11 } },
      grid: { left: 40, right: 20, top: 20, bottom: 40 },
      xAxis: {
        type: "category",
        data: deptData.value.map(d => deptName(d.deptName)),
        axisLine: { lineStyle: { color: "#e2e8f0" } },
        axisLabel: { color: "#94a3b8", fontSize: 11 },
      },
      yAxis: {
        type: "value",
        axisLine: { show: false },
        splitLine: { lineStyle: { color: "#f1f5f9" } },
        axisLabel: { color: "#94a3b8", fontSize: 11 },
      },
      series: [
        { name: "已完成", type: "bar", data: deptData.value.map(d => d.completed), color: "#10b981", barWidth: "30%" },
        { name: "待处理", type: "bar", data: deptData.value.map(d => d.pending), color: "#f59e0b", barWidth: "30%" },
      ],
    })
    window.addEventListener("resize", () => chart.resize())
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.report-page { padding: 0; }

.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 20px; font-weight: 600; color: #1a202c; margin: 0 0 4px 0; }
.page-desc { font-size: 13px; color: #94a3b8; margin: 0; }
.head-right { display: flex; gap: 12px; align-items: center; }
.report-date { font-size: 13px; color: #64748b; }
.btn-ghost { display: inline-flex; align-items: center; gap: 6px; padding: 7px 14px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; color: #475569; font-size: 13px; cursor: pointer; transition: all .2s; }
.btn-ghost:hover { border-color: #3b82f6; color: #3b82f6; }
.btn-ghost:disabled { opacity: .5; cursor: not-allowed; }

.kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.kpi-card { display: flex; align-items: center; gap: 14px; padding: 18px 20px; background: #fff; border: 1px solid #eef0f4; border-radius: 10px; }
.kpi-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.kpi-body { display: flex; flex-direction: column; }
.kpi-val { font-size: 26px; font-weight: 700; line-height: 1.2; }
.kpi-label { font-size: 12px; color: #94a3b8; margin-top: 2px; }

.chart-row { display: flex; gap: 16px; margin-bottom: 20px; }
.chart-card { background: #fff; border: 1px solid #eef0f4; border-radius: 10px; overflow: hidden; }
.chart-wide { flex: 2; }
.chart-narrow { flex: 1; }
.chart-half { flex: 1; }
.card-head { padding: 14px 20px; border-bottom: 1px solid #f1f5f9; }
.card-title { font-size: 14px; font-weight: 600; color: #1a202c; }
.card-body { padding: 16px 20px; }
.chart-box { width: 100%; height: 260px; }

.summary-section { background: #fff; border: 1px solid #eef0f4; border-radius: 10px; overflow: hidden; }
.summary-head { display: flex; align-items: center; gap: 8px; padding: 14px 20px; border-bottom: 1px solid #f1f5f9; font-size: 14px; font-weight: 600; color: #1a202c; }
.summary-body { padding: 16px 20px; display: flex; flex-direction: column; gap: 14px; }
.summary-item { display: flex; gap: 12px; }
.conclusion-icon { flex-shrink: 0; width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; }
.conclusion-icon.warning { background: #fef2f2; color: #dc2626; }
.conclusion-icon.success { background: #f0fdf4; color: #16a34a; }
.conclusion-icon.info { background: #eff6ff; color: #3b82f6; }
.conclusion-icon.default { background: #f5f3ff; color: #8b5cf6; }
.conclusion-content { flex: 1; }
.conclusion-title { font-size: 13px; font-weight: 600; color: #1a202c; margin-bottom: 4px; }
.conclusion-desc { font-size: 13px; color: #64748b; line-height: 1.6; }
</style>
