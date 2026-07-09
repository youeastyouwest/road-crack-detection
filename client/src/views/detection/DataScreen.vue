<template>
  <div class="ds-container">
    <div class="ds-map-area">
      <div class="ds-float-stats">
        <div class="ds-float-stat"><span class="ds-fs-val">{{ totalRoad }}km</span><span class="ds-fs-lbl">总里程</span></div>
        <div class="ds-float-stat"><span class="ds-fs-val">{{ crackCount }}</span><span class="ds-fs-lbl">裂缝数</span></div>
        <div class="ds-float-stat"><span class="ds-fs-val">{{ repairedCount }}</span><span class="ds-fs-lbl">已修复</span></div>
        <div class="ds-float-stat"><span class="ds-fs-val">{{ alertCount }}</span><span class="ds-fs-lbl">告警</span></div>
        <div class="ds-float-time"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>&nbsp;{{ currentTime }}</div>
      </div>
      <div class="ds-view-controls">
        <button class="ds-vc-btn" @click="zoomIn" title="放大"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg></button>
        <div class="ds-vc-level">{{ zoomLevel }}x</div>
        <button class="ds-vc-btn" @click="zoomOut" title="缩小"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/></svg></button>
        <div class="ds-vc-sep"></div>
        <button class="ds-vc-btn" :class="{ active: viewMode3D }" @click="toggle3D" :title="viewMode3D ? '切换2D' : '切换3D'">
          <svg v-if="!viewMode3D" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 002 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
        </button>
        <div v-if="viewMode3D" class="ds-vc-pitch-row">
          <input type="range" min="0" max="60" v-model.number="pitchLevel" class="ds-vc-slider" @input="setPitch" />
          <span class="ds-vc-slider-label">{{ pitchLevel }}°</span>
        </div>
        <div class="ds-vc-sep"></div>
        <button class="ds-vc-btn ds-vc-locate" @click="locateMe" title="定位"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg></button>
      </div>
      <div ref="mapContainer" class="ds-map"></div>
      <div v-if="selectedDisease" class="ds-popup">
        <div class="ds-popup-head"><span>病害详情</span><button @click="selectedDisease = null">x</button></div>
        <div class="ds-popup-body">
          <div class="ds-popup-img"><svg width="100%" height="100" viewBox="0 0 200 100" fill="none"><rect width="200" height="100" rx="6" fill="#eef2ff"/><text x="100" y="45" text-anchor="middle" fill="#4361ee" font-size="12" font-weight="600">病害标注示意图</text><text x="100" y="65" text-anchor="middle" fill="#94a3b8" font-size="10">（此处展示 AI 识别分割掩码）</text></svg></div>
          <div class="ds-popup-info">
            <div class="ds-popup-row"><span class="ds-popup-label">病害类型</span><strong>{{ selectedDisease.type }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">严重等级</span><strong><span class="ds-sev-dot" :style="{background: severityColor(selectedDisease.severity)}"></span>{{ selectedDisease.severity }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">检测时间</span><strong>{{ selectedDisease.time }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">位置</span><strong>{{ selectedDisease.location }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">置信度</span><strong class="ds-conf">{{ selectedDisease.confidence }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">病害尺寸</span><strong class="ds-order">{{ selectedDisease.size || '--' }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">工单编号</span><strong>{{ selectedDisease.orderId || '未生成' }}</strong></div>
            <div class="ds-popup-row"><span>养护建议</span><strong style="font-size:11px;color:#64748b;text-align:right">建议一周内安排局部修补，防止裂缝扩展</strong></div>
          </div>
        </div>
        <div class="ds-popup-foot">
          <button class="ds-btn ds-btn-primary" @click="createWorkOrder">生成工单</button>
          <button class="ds-btn ds-btn-ghost" @click="selectedDisease = null">关闭</button>
        </div>
      </div>
      <div v-if="showCapacity" class="ds-popup" style="right:290px;top:80px">
        <div class="ds-popup-head"><span>点位容量概览</span><button @click="showCapacity = false">x</button></div>
        <div class="ds-popup-body" style="padding:0">
          <div class="ds-popup-row"><span>当前点位</span><strong>{{ currentPoints }} <span style="font-weight:400;color:#94a3b8;font-size:11px">个</span></strong></div>
          <div class="ds-popup-row"><span>系统上限</span><strong class="ds-conf">{{ maxPoints }} <span style="font-weight:400;color:#94a3b8;font-size:11px">个</span></strong></div>
          <div class="ds-popup-row"><span>剩余容量</span><strong class="ds-order">{{ maxPoints - currentPoints }} <span style="font-weight:400;color:#94a3b8;font-size:11px">个</span></strong></div>
          <div class="ds-popup-row" style="flex-direction:column;align-items:stretch;gap:4px;padding:10px 14px">
            <div style="display:flex;justify-content:space-between;font-size:10px;color:#94a3b8"><span>使用率</span><span style="font-weight:700;color:#4361ee">{{ (currentPoints / maxPoints * 100).toFixed(1) }}%</span></div>
            <div style="height:5px;background:#f1f5f9;border-radius:3px;overflow:hidden"><div :style="{height:'100%',width:(currentPoints / maxPoints * 100)+'%',background:'#4361ee',borderRadius:'3px'}"></div></div>
          </div>
          <div class="ds-popup-row" style="border-bottom:none"><span>存储建议</span><strong style="font-size:11px;line-height:1.5;color:#64748b;max-width:150px">建议定期导出历史数据，释放存储空间</strong></div>
        </div>
        <div class="ds-popup-foot">
          <button class="ds-btn ds-btn-ghost" @click="showCapacity = false">关闭</button>
          <button class="ds-btn ds-btn-primary" @click="exportReport">导出数据</button>
        </div>
      </div>
  </div>

  <div :class="['ds-sidebar', { collapsed: sidebarCollapsed }]">
      <div class="ds-sidebar-inner">
        <div class="ds-sb-header"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>地图面板</div>
                <div class="ds-sb-tabs">
          <button :class="['ds-sb-tab', { active: activeTab === 'map' }]" @click="activeTab = 'map'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 6v16l7-4 8 4 7-4V2l-7 4-8-4-7 4z"/><path d="M8 2v16"/><path d="M16 6v16"/></svg>
            <span>图层</span>
          </button>
          <button :class="['ds-sb-tab', { active: activeTab === 'analysis' }]" @click="activeTab = 'analysis'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-6"/><path d="M2 20h20"/></svg>
            <span>分析</span>
          </button>
          <button :class="['ds-sb-tab', { active: activeTab === 'chat' }]" @click="activeTab = 'chat'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/><path d="M8 10h8"/><path d="M8 14h5"/></svg>
            <span>AI 助手</span>
          </button>
        </div>
        <div v-if="activeTab === 'map'" class="ds-tab-panel">
          <div class="ds-sb-search"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="searchQuery" placeholder="搜索道路、地点..." /></div>
          <div class="ds-sb-section"><div class="ds-sb-section-title">图层</div>
            <div v-for="layer in layers" :key="layer.id" class="ds-layer-item">
              <label class="ds-layer-label"><input type="checkbox" v-model="layer.visible" /><span class="ds-layer-dot" :style="{ background: layer.color }"></span>{{ layer.label }}<span class="ds-layer-count">{{ layer.count }}</span></label>
            </div>
          </div>
          <div class="ds-sb-section"><div class="ds-sb-section-title">筛选条件</div>
            <div class="ds-filter-row"><label>严重程度</label><select v-model="filterSeverity"><option value="">全部</option><option value="严重">严重</option><option value="中等">中等</option><option value="轻微">轻微</option></select></div>
            <div class="ds-filter-row"><label>检测日期</label><input type="date" v-model="filterDate" /></div>
            <div class="ds-filter-row"><label>修复状态</label><select v-model="filterStatus"><option value="">全部</option><option value="待修复">待修复</option><option value="已派单">已派单</option><option value="已完成">已完成</option></select></div>
          </div>
          <div class="ds-sb-actions"><button class="ds-sb-btn primary" @click="refreshMapData">刷新数据</button><div style="display:flex;gap:6px"><button class="ds-sb-btn" style="flex:1" @click="showCapacity = true">存储</button><button class="ds-sb-btn" style="flex:1" @click="exportReport">导出</button></div></div>
        </div>
        <div v-if="activeTab === 'analysis'" class="ds-tab-panel">
          <div class="ds-an-section">
            <div class="ds-an-card"><div class="ds-an-card-icon" style="background:#eef2ff;color:#4361ee"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-6"/></svg></div><div class="ds-an-card-info"><span class="ds-an-card-val">{{ weeklyTotal }}</span><span class="ds-an-card-lbl">本周检测总数</span></div></div>
            <div class="ds-an-card" style="margin-top:6px"><div class="ds-an-card-icon" style="background:#fef3c7;color:#d97706"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div class="ds-an-card-info"><span class="ds-an-card-val">{{ pendingRepair }}</span><span class="ds-an-card-lbl">待维修</span></div></div>
          </div>
          <div class="ds-an-section"><div class="ds-sb-section-title">本周趋势</div><div ref="trendChartRef" style="height:110px;margin-top:4px"></div></div>
          <div class="ds-an-section" style="border-top:1px solid #eef0f4"><div class="ds-sb-section-title">病害类型占比</div><div ref="pieChartRef" style="height:130px;margin-top:4px"></div></div>
          <div class="ds-an-section"><div class="ds-sb-section-title">严重等级分布</div><div ref="severityChartRef" style="height:90px;margin-top:4px"></div></div>
        </div>
        <div v-if="activeTab === 'chat'" class="ds-tab-panel ds-chat-panel" style="display:flex;flex-direction:column;overflow:hidden;">
          <div class="ds-sb-header" style="flex-shrink:0"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>AI 助手<button class="ds-chat-clear" @click="chatMessages = [{role:'ai', text:'您好！我是道路病害AI助手，可以为您分析检测数据、查询病害详情或生成报告。'}]"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg></button></div>
          <div class="ds-chat-msgs" ref="chatMsgRef">
            <div v-for="(m, i) in chatMessages" :key="i" :class="['ds-chat-msg', m.role]">
              <div v-if="m.role === 'ai'" class="ds-chat-avatar"><div class="avatar-video-wrap"><img class="avatar-video" :src="avatarSrc" alt="AI助手" /></div></div>
              <div class="ds-chat-bubble" v-html="m.text"></div>
            </div>
            <div v-if="chatTyping" class="ds-chat-typing"><span></span><span></span><span></span></div>
          </div>
          <div class="ds-chat-input-bar"><input v-model="chatInput" placeholder="输入您的问题，例如：今日病害统计" :disabled="chatTyping" @keyup.enter="sendChat" /><button class="ds-chat-send" :disabled="!chatInput.trim() || chatTyping" @click="sendChat"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg></button></div>
        </div>
      </div>
    </div>
  </div>
  <button class="ds-toggle-btn" :style="{ right: sidebarCollapsed ? '14px' : '354px' }" @click="sidebarCollapsed = !sidebarCollapsed">
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline :points="sidebarCollapsed ? '9 18 15 12 9 6' : '15 18 9 12 15 6'" /></svg>
  </button>
</template>
<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from "vue"
import * as echarts from "echarts"

declare global {
  interface Window { AMap: any }
}

const mapContainer = ref()
const sidebarCollapsed = ref(false)
const showCapacity = ref(false)
const zoomLevel = ref(12)
const viewMode3D = ref(false)
const pitchLevel = ref(0)
const currentTime = ref("")
const activeTab = ref("map")
const filterSeverity = ref("")
const filterDate = ref("")
const filterStatus = ref("")
const searchQuery = ref("")
const layers = ref([
  { id: "crack", label: "裂缝", color: "#ef4444", count: 1258, visible: true },
  { id: "repair", label: "修复", color: "#10b981", count: 892, visible: true },
  { id: "alert", label: "告警", color: "#f59e0b", count: 378, visible: true },
])
const selectedDisease = ref(null)
const currentPoints = ref(3256)
const maxPoints = ref(5000)
const chatMessages = ref([{ role: "ai", text: "您好！我是道路病害AI助手，可以为您分析检测数据、查询病害详情或生成报告。" }])
const chatInput = ref("")
const chatTyping = ref(false)
const chatMsgRef = ref()
const trendChartRef = ref()
const pieChartRef = ref()
const severityChartRef = ref()
const weeklyTotal = ref(381)
const pendingRepair = ref(127)
const totalRoad = ref(0)
const crackCount = ref(0)
const repairedCount = ref(0)
const alertCount = ref(0)

let map = null
let trendChart = null
let pieChart = null
let severityChart = null
let timeInterval = null

const avatarSrc = computed(() => "/avatar-agent.png")

const trendOption = computed(() => ({
  grid: { left: 30, right: 8, top: 10, bottom: 20 },
  xAxis: { type: "category", color: ["#4361ee","#f72585","#06d6a0","#ffd166","#7209b7","#118ab2"], data: ["一", "二", "三", "四", "五", "六", "日"], axisLabel: { fontSize: 10, color: "#94a3b8" }, axisLine: { show: false }, axisTick: { show: false } },
  yAxis: { type: "value", splitLine: { lineStyle: { color: "#f1f5f9", type: "dashed" } }, axisLabel: { fontSize: 9, color: "#94a3b8" }, min: 0 },
  series: [{ type: "line", color: ["#4361ee","#f72585","#06d6a0","#ffd166","#7209b7","#118ab2"], data: [42, 38, 55, 48, 62, 45, 51], smooth: true, lineStyle: { color: "#4361ee", width: 2 }, itemStyle: { color: "#4361ee" }, areaStyle: { color: { type: "linear", x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: "rgba(67,97,238,0.25)" }, { offset: 1, color: "rgba(67,97,238,0.04)" }] } }, symbol: "circle", symbolSize: 5 }],
  tooltip: { trigger: "axis", backgroundColor: "rgba(255,255,255,0.95)", borderColor: "#eef0f4", textStyle: { fontSize: 11 } },
}))

const pieOption = computed(() => ({
  tooltip: { trigger: "item", backgroundColor: "rgba(255,255,255,0.95)", borderColor: "#eef0f4", textStyle: { fontSize: 11 }, formatter: "{b}: {c} ({d}%)" },
  series: [{
    type: "pie", radius: ["35%", "68%"], center: ["50%", "50%"],
    color: ["#4361ee","#f72585","#06d6a0","#ffd166","#7209b7","#118ab2"], data: [
      { name: "横向裂缝", value: 1258, itemStyle: { color: "#4361ee" } },
      { name: "纵向裂缝", value: 892, itemStyle: { color: "#f72585" } },
      { name: "网状裂缝", value: 534, itemStyle: { color: "#06d6a0" } },
      { name: "路面抛洒", value: 378, itemStyle: { color: "#ffd166" } },
      { name: "标志线损坏", value: 260, itemStyle: { color: "#7209b7" } },
    ],
    label: { show: false },
    emphasis: { label: { show: true, fontSize: 11, fontWeight: "bold" }, itemStyle: { shadowBlur: 4, shadowColor: "rgba(0,0,0,0.08)" } },
  }],
}))

const severityOption = computed(() => ({
  grid: { left: 50, right: 20, top: 5, bottom: 5 },
  xAxis: { type: "value", splitLine: { lineStyle: { color: "#f1f5f9" } }, axisLabel: { fontSize: 9, color: "#94a3b8" }, axisLine: { show: false }, axisTick: { show: false } },
  yAxis: { type: "category", color: ["#4361ee","#f72585","#06d6a0","#ffd166","#7209b7","#118ab2"], data: ["严重", "中等", "轻微"], axisLabel: { fontSize: 11, color: "#475569" }, axisLine: { show: false }, axisTick: { show: false } },
  series: [{ type: "bar", color: ["#4361ee","#f72585","#06d6a0","#ffd166","#7209b7","#118ab2"], data: [{ value: 127, itemStyle: { color: "#ee5a24", borderRadius: [0, 4, 4, 0] } }, { value: 892, itemStyle: { color: "#f0932b", borderRadius: [0, 4, 4, 0] } }, { value: 2103, itemStyle: { color: "#6ab04c", borderRadius: [0, 4, 4, 0] } }], barWidth: 12, label: { show: true, position: "right", fontSize: 10, color: "#475569", fontWeight: 600, formatter: (p) => p.value + "处" } }],
  tooltip: { trigger: "axis", backgroundColor: "rgba(255,255,255,0.95)", borderColor: "#eef0f4", textStyle: { fontSize: 11 } },
}))

function severityColor(sev) { const m = { 严重: "#ef4444", 中等: "#f59e0b", 轻微: "#10b981" }; return m[sev] || "#94a3b8" }
function zoomIn() { zoomLevel.value = Math.min(zoomLevel.value + 1, 20); if (map) map.setZoom(zoomLevel.value) }
function zoomOut() { zoomLevel.value = Math.max(zoomLevel.value - 1, 3); if (map) map.setZoom(zoomLevel.value) }
function toggle3D() {
  viewMode3D.value = !viewMode3D.value
  if (!map) return
  if (viewMode3D.value) {
    map.setPitch(30)
    pitchLevel.value = 30
    map.setRotation(0)
  } else {
    map.setPitch(0)
    pitchLevel.value = 0
    map.setRotation(0)
  }
}
function setPitch() {
  if (!map || !viewMode3D.value) return
  map.setPitch(pitchLevel.value)
}
function locateMe() {
  if (!map) return
  const fallback = () => { map.setCenter([116.397428, 39.90923]); map.setZoom(13); zoomLevel.value = 13 }
  if (navigator.geolocation) { navigator.geolocation.getCurrentPosition((p) => { map.setCenter([p.coords.longitude, p.coords.latitude]); map.setZoom(15); zoomLevel.value = 15 }, fallback) } else { fallback() }
}
function initMap() {
  if (!mapContainer.value || !window.AMap) return
  map = new window.AMap.Map(mapContainer.value, { zoom: zoomLevel.value, center: [116.397428, 39.90923], features: ["bg", "road", "building", "point"], resizeEnable: true, viewMode: "3D", pitch: 0,  })
  map.on("zoomchange", () => { zoomLevel.value = map.getZoom() })
  const positions = [[116.405285, 39.904989], [116.415, 39.91], [116.395, 39.92], [116.41, 39.898], [116.42, 39.915]]
  positions.forEach((p, i) => {
    const colors = ["#ef4444", "#f59e0b", "#22c55e", "#3b82f6", "#a855f7"]
    const types = ["横向裂缝", "纵向裂缝", "网状裂缝", "路面抛洒", "标志线损坏"]
    const sevs = ["严重", "中等", "轻微", "严重", "中等"]
    const marker = new window.AMap.Marker({
      position: p, offset: new window.AMap.Pixel(-8, -8),
      content: "<div style=\"width:18px;height:18px;background:" + colors[i] + ";border-radius:50%;border:2px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.2);cursor:pointer\"></div>"
    })
    marker.on("click", () => {
      selectedDisease.value = {
        type: types[i], severity: sevs[i],
        time: "2026-04-0" + (i + 8) + " 14:3" + i,
        location: "G" + (i + 1) + "0" + (i + 2) + "国道 K" + (12 + i) + "+" + (500 + i * 100),
        confidence: (96 - i * 3) + "%",
        size: (i + 1) * 0.5 + "m" + String.fromCharCode(178),
        orderId: "WO-2026" + (i + 1).toString().padStart(4, "0")
      }
    })
    map.add(marker)
  })
}
function initECharts() {
  if (trendChartRef.value) { trendChart = echarts.init(trendChartRef.value); trendChart.setOption(trendOption.value) }
  if (pieChartRef.value) { pieChart = echarts.init(pieChartRef.value); pieChart.setOption(pieOption.value) }
  if (severityChartRef.value) { severityChart = echarts.init(severityChartRef.value); severityChart.setOption(severityOption.value) }
}
function refreshMapData() { weeklyTotal.value = Math.floor(Math.random() * 100 + 350); pendingRepair.value = Math.floor(Math.random() * 50 + 100); if (trendChart) trendChart.setOption({ series: [{ color: ["#4361ee","#f72585","#06d6a0","#ffd166","#7209b7","#118ab2"], data: [42, 38, 55, 48, 62, 45, 51].map(v => v + Math.floor(Math.random() * 10 - 5)) }] }) }
function exportReport() { selectedDisease.value = null; showCapacity.value = false }
function createWorkOrder() { if (selectedDisease.value) selectedDisease.value.orderId = "WO-2026" + Date.now().toString().slice(-6) }
function scrollChatToBottom() { nextTick(() => { if (chatMsgRef.value) chatMsgRef.value.scrollTop = chatMsgRef.value.scrollHeight }) }
function sendChat() {
  const text = chatInput.value.trim()
  if (!text || chatTyping.value) return
  chatInput.value = ""
  chatMessages.value.push({ role: "user", text })
  scrollChatToBottom()
  chatTyping.value = true
  const resp = { "分析最新检测数据": "根据最新检测数据，本周共发现病害 <strong>381 处</strong>，其中严重病害 127 处，中等 182 处，轻微 72 处。", "高优先级工单": "当前共有 <strong>8 个</strong>高优先级工单待处理：<br>1. G102 K15+300 纵向裂缝（严重）<br>2. G101 K12+500 横向裂缝（严重）<br>3. G103 K17+200 网状裂缝（严重）", "生成周报": "正在为您生成本周检测周报... 完成！<br><strong>📊 本周检测概览</strong><br>- 检测总里程：2,847 km<br>- 发现裂缝：1,258 处<br>- 已修复：892 处", "裂缝趋势预测": "根据近4周数据分析：<br>📈 G102国道趋势：<strong>上升 12%</strong>（需重点关注）<br>📉 G101国道趋势：<strong>下降 5%</strong><br>📈 G105国道趋势：<strong>上升 3%</strong>" }
  setTimeout(() => { chatMessages.value.push({ role: "ai", text: resp[text] || "收到您的问题：" + text + "<br><br>请使用左侧快捷操作按钮快速查询。" }); chatTyping.value = false; scrollChatToBottom() }, 1200)
}
function startTimeUpdate() {
  const update = () => { const n = new Date(); currentTime.value = n.getFullYear() + "-" + String(n.getMonth() + 1).padStart(2, "0") + "-" + String(n.getDate()).padStart(2, "0") + " " + String(n.getHours()).padStart(2, "0") + ":" + String(n.getMinutes()).padStart(2, "0") + ":" + String(n.getSeconds()).padStart(2, "0") }
  update(); timeInterval = setInterval(update, 1000)
}

async function loadStats() {
  try {
    const r = await statisticsApi.getDashboard()
    const d = r.data.data
    if (d) {
      totalRoad.value = d.totalRoads || 0
      crackCount.value = d.totalCracksDetected || 0
      repairedCount.value = d.totalWorkOrders || 0
      alertCount.value = d.pendingAlerts || 0
    }
  } catch {}
}
onMounted(() => { initMap(); initECharts(); startTimeUpdate(); loadStats() })
onUnmounted(() => { if (timeInterval) clearInterval(timeInterval); if (trendChart) trendChart.dispose(); if (pieChart) pieChart.dispose(); if (severityChart) severityChart.dispose(); map = null })
</script>
<style scoped>
.ds-container { display:flex; height:100vh; margin:-24px; width:calc(100% + 48px); background:#f5f7fa; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; overflow:hidden; position:relative; }
.ds-map-area { flex:1; position:relative; }
.ds-float-stats { position:absolute; top:14px; left:14px; z-index:10; display:flex; background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.ds-float-stat { display:flex; flex-direction:column; align-items:center; padding:8px 14px; border-right:1px solid #eef0f4; }
.ds-float-stat:last-child { border-right:none; }
.ds-fs-val { font-size:17px; font-weight:800; color:#0f172a; font-variant-numeric:tabular-nums; }
.ds-fs-lbl { font-size:10px; color:#94a3b8; margin-top:1px; white-space:nowrap; }
.ds-float-time { display:flex; align-items:center; padding:0 14px; font-size:11px; color:#94a3b8; font-variant-numeric:tabular-nums; }
.ds-map { width:100%; height:100%; background:#f1f5f9; }
.ds-toggle-btn { position:fixed; top:50%; transform:translateY(-50%); z-index:100; width:28px; height:48px; display:flex; align-items:center; justify-content:center; background:#fff; border:1px solid #d0d5dd; border-radius:6px; cursor:pointer; color:#94a3b8; box-shadow:0 2px 6px rgba(0,0,0,0.1); padding:0; font-family:inherit; transition:right .25s ease,color .15s,border-color .15s; }
.ds-toggle-btn:hover { color:#2563eb; border-color:#2563eb; box-shadow:0 2px 8px rgba(37,99,235,0.15); }
.ds-view-controls { position:absolute; right:14px; bottom:30px; z-index:15; display:flex; flex-direction:column; gap:2px; background:#fff; border:1px solid #f0f2f5; border-radius:8px; padding:4px; }
.ds-vc-btn { width:32px; height:32px; display:flex; align-items:center; justify-content:center; border:none; background:transparent; border-radius:6px; cursor:pointer; color:#64748b; font-size:13px; transition:all .15s; }
.ds-vc-btn:hover { background:#f1f5f9; color:#2563eb; }
.ds-vc-level { text-align:center; font-size:10px; font-weight:700; color:#94a3b8; padding:2px 0; }
.ds-vc-locate { margin-top:2px; border-top:1px solid #eef0f4; padding-top:6px; }
.ds-sidebar { width:340px; flex-shrink:0; background:#fff; border-left:1px solid #f0f2f5; display:flex; flex-direction:column; transition:width .25s ease,margin .25s ease; overflow:hidden; }
.ds-sidebar.collapsed { width:0; margin-right:-340px; border-left:none; }
.ds-sidebar-inner { width:340px; height:100%; display:flex; flex-direction:column; overflow-y:auto; }
.ds-sidebar-inner::-webkit-scrollbar { width:3px; }
.ds-sidebar-inner::-webkit-scrollbar-thumb { background:#e2e8f0; border-radius:2px; }
.ds-sb-header { display:none; }
.ds-sb-header svg { color:#4361ee; }
.ds-sb-tabs { display:flex; margin:0 16px; padding:0; border-bottom:1px solid #f0f2f5; background:none; border-radius:0; gap:0; }
.ds-sb-tab { flex:1; display:flex; flex-direction:column; align-items:center; gap:4px; padding:12px 8px 10px; text-align:center; font-size:13px; font-weight:500; color:#94a3b8; cursor:pointer; transition:all .2s; background:transparent; font-family:inherit; border:none; border-bottom:3px solid transparent; margin-bottom:-1px; line-height:1.3; }
.ds-sb-tab.active { background:transparent; color:#2563eb; font-weight:600; border-bottom-color:#2563eb; box-shadow:none; }
.ds-sb-tab:hover:not(.active) { background:transparent; color:#64748b; }
.ds-tab-panel { overflow-y:auto; flex:1; display:flex; flex-direction:column; padding:16px; gap:0; }
.ds-tab-panel::-webkit-scrollbar { width:4px; }
.ds-tab-panel::-webkit-scrollbar-thumb { background:#d1d5db; border-radius:2px; }
.ds-sb-search { display:flex; align-items:center; gap:6px; margin:0 0 16px; padding:8px 12px; background:#f8f9fc; border:1px solid #eef0f4; border-radius:8px; }
.ds-sb-search svg { color:#94a3b8; flex-shrink:0; }
.ds-sb-search input { flex:1; border:none; background:transparent; outline:none; font-size:13px; color:#1e293b; font-family:inherit; }
.ds-sb-search input::placeholder { color:#94a3b8; }
.ds-sb-section { padding:0; margin-bottom:16px; }
.ds-sb-section-title { padding:0 0 0; margin-bottom:10px; font-size:11px; font-weight:600; color:#94a3b8; letter-spacing:0.04em; }
.ds-layer-item { margin-bottom:2px; }
.ds-layer-label { display:flex; align-items:center; gap:8px; font-size:13px; font-weight:500; color:#475569; cursor:pointer; padding:7px 10px; border-radius:6px; }
.ds-layer-label input[type=checkbox] { accent-color:#4361ee; }
.ds-layer-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.ds-layer-count { margin-left:auto; font-size:11px; color:#94a3b8; }
.ds-filter-row { margin-bottom:10px; }
.ds-filter-row:last-child { margin-bottom:0; }
.ds-filter-row label { font-size:11px; font-weight:500; color:#94a3b8; display:block; margin-bottom:6px; }
.ds-filter-row select, .ds-filter-row input[type=date] { width:100%; padding:7px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; color:#334155; background:#fafbfc; font-family:inherit; outline:none; transition:border-color .15s; }
.ds-filter-row select:focus, .ds-filter-row input[type=date]:focus { border-color:#2563eb; background:#fff; }
.ds-sb-actions { padding:12px 0 0; margin-top:auto; display:flex; flex-direction:column; gap:6px; border-top:1px solid #f0f2f5; }
.ds-sb-btn { display:flex; align-items:center; justify-content:center; gap:6px; padding:8px 12px; border:1px solid #e2e8f0; background:#fff; border-radius:8px; font-size:12px; font-weight:500; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; }
.ds-sb-btn:hover { border-color:#4361ee; color:#4361ee; }
.ds-sb-btn.primary { background:#2563eb; color:#fff; border-color:#2563eb; font-weight:500; }
.ds-sb-btn.primary:hover { background:#3651d4; }
.ds-popup { position:absolute; right:310px; top:14px; z-index:100; width:300px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.ds-popup-head { display:flex; align-items:center; justify-content:space-between; padding:12px 14px; border-bottom:1px solid #f1f5f9; font-size:13px; font-weight:700; color:#0f172a; }
.ds-popup-head button { width:24px; height:24px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; font-size:12px; cursor:pointer; }
.ds-popup-head button:hover { background:#e2e8f0; color:#475569; }
.ds-popup-body { padding:8px 0; }
.ds-popup-img { padding:8px 14px; }
.ds-popup-info { padding:0 14px; }
.ds-popup-row { display:flex; align-items:center; justify-content:space-between; padding:6px 0; border-bottom:1px solid #f8f9fc; font-size:12px; }
.ds-popup-row:last-child { border-bottom:none; }
.ds-popup-label { color:#64748b; flex-shrink:0; }
.ds-popup-row strong { color:#0f172a; text-align:right; display:flex; align-items:center; gap:4px; }
.ds-conf { color:#10b981 !important; }
.ds-order { color:#4361ee !important; }
.ds-sev-dot { width:6px; height:6px; border-radius:50%; display:inline-block; flex-shrink:0; }
.ds-popup-foot { display:flex; gap:8px; padding:10px 14px; border-top:1px solid #f1f5f9; }
.ds-btn { flex:1; padding:8px; border:1px solid #e2e8f0; border-radius:8px; font-size:12px; font-weight:600; cursor:pointer; font-family:inherit; text-align:center; }
.ds-btn-primary { background:#2563eb; color:#fff; border-color:#2563eb; }
.ds-btn-primary:hover { background:#1d4ed8; }
.ds-btn-ghost { background:transparent; color:#64748b; }
.ds-btn-ghost:hover { background:#f1f5f9; }
.ds-an-section { padding:0; margin-bottom:16px; }
.ds-an-section+.ds-an-section { border-top:1px solid #f0f2f5; padding-top:16px; }
.ds-an-card { display:flex; align-items:center; gap:12px; padding:12px 14px; background:#f8f9fc; border-radius:10px; }
.ds-an-card+.ds-an-card { margin-top:8px; }
.ds-an-card-icon { width:36px; height:36px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.ds-an-card-info { display:flex; flex-direction:column; }
.ds-an-card-val { font-size:22px; font-weight:800; color:#0f172a; line-height:1.1; font-variant-numeric:tabular-nums; }
.ds-an-card-lbl { font-size:11px; color:#94a3b8; margin-top:2px; }
.ds-chat-panel { overflow:hidden; padding:0; }
.ds-chat-clear { margin-left:auto; width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; flex-shrink:0; }
.ds-chat-clear:hover { background:#fef2f2; color:#ef4444; }
.ds-chat-msgs { flex:1; overflow-y:auto; padding:12px 16px; display:flex; flex-direction:column; gap:8px; }
.ds-chat-msgs::-webkit-scrollbar { width:3px; }
.ds-chat-msgs::-webkit-scrollbar-thumb { background:#e2e8f0; border-radius:2px; }
.ds-chat-msg { display:flex; gap:8px; max-width:100%; }
.ds-chat-msg.user { justify-content:flex-end; }
.ds-chat-avatar { width:28px; height:28px; display:flex; align-items:center; justify-content:center; background:#eef2ff; border-radius:8px; flex-shrink:0; color:#4361ee; }
.ds-chat-bubble { padding:9px 12px; border-radius:8px; font-size:13px; line-height:1.5; color:#334155; background:#f1f5f9; max-width:200px; word-break:break-word; }
.ds-chat-msg.user .ds-chat-bubble { background:#2563eb; color:#fff; border-bottom-right-radius:2px; }
.ds-chat-msg.ai .ds-chat-bubble { border-bottom-left-radius:2px; }
.ds-chat-bubble strong { font-weight:700; }
.ds-chat-msg.user .ds-chat-bubble strong { color:#fff; }
.ds-chat-typing { display:flex; gap:3px; align-items:center; padding:10px 14px; }
.ds-chat-typing span { width:5px; height:5px; border-radius:50%; background:#94a3b8; animation:ds-typing 1s infinite; }
.ds-chat-typing span:nth-child(2) { animation-delay:.2s; }
.ds-chat-typing span:nth-child(3) { animation-delay:.4s; }
@keyframes ds-typing { 0%,60%,to { opacity:.3; transform:scale(1) } 30% { opacity:1; transform:scale(1.2) } }
.ds-chat-input-bar { display:flex; gap:6px; padding:10px 16px 16px; border-top:1px solid #f0f2f5; background:#fff; }
.ds-chat-input-bar input { flex:1; padding:8px 12px; border:1px solid #e2e8f0; border-radius:8px; font-size:12px; outline:none; font-family:inherit; color:#334155; }
.ds-chat-input-bar input:focus { border-color:#4361ee; }
.ds-chat-input-bar input::placeholder { color:#94a3b8; }
.ds-chat-send { width:34px; height:34px; display:flex; align-items:center; justify-content:center; background:#4361ee; border:none; border-radius:8px; color:#fff; cursor:pointer; flex-shrink:0; }
.ds-chat-send:hover { background:#3651d4; }
.ds-chat-send:disabled { background:#e2e8f0; color:#94a3b8; cursor:not-allowed; }
.ds-chat-avatar .avatar-video-wrap { width:28px; height:28px; border-radius:8px; overflow:hidden; border:1px solid rgba(183,36,255,.25); box-shadow:0 0 4px #b724ff1f; flex-shrink:0; display:flex; }

.ds-vc-sep { height:1px; background:#eef0f4; margin:3px 6px; }
.ds-vc-btn.active { background:#eef2ff; color:#2563eb; }
.ds-vc-pitch-row { display:flex; align-items:center; gap:4px; padding:4px 8px; }
.ds-vc-slider { width:60px; height:3px; -webkit-appearance:none; appearance:none; background:#e2e8f0; border-radius:2px; outline:none; cursor:pointer; }
.ds-vc-slider::-webkit-slider-thumb { -webkit-appearance:none; width:12px; height:12px; background:#4361ee; border-radius:50%; cursor:pointer; border:2px solid #fff; box-shadow:0 1px 3px rgba(0,0,0,0.15); }
.ds-vc-slider-label { font-size:10px; font-weight:600; color:#94a3b8; min-width:22px; text-align:right; }
</style>









