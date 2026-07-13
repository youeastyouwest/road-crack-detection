<template>
  <div class="map-view">
    <!-- 统计概览 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4" v-for="s in statistics" :key="s.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" size="small">
        <el-form-item label="损坏类型">
          <el-select v-model="filters.damageType" clearable placeholder="全部" style="width:140px">
            <el-option v-for="d in damageTypes" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="filters.severityLevel" clearable placeholder="全部" style="width:120px">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="工单状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width:140px">
            <el-option label="待指派" value="PENDING_ASSIGNMENT" />
            <el-option label="已指派" value="ASSIGNED" />
            <el-option label="处理中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="道路名称/地址" clearable style="width:180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadMarkers">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 地图 + 列表侧栏 -->
    <div class="map-container">
      <div ref="mapRef" class="amap-wrapper"></div>
      <el-card shadow="never" class="marker-sidebar">
        <template #header>
          <span>点位列表 ({{ markers.length }})</span>
        </template>
        <div class="marker-list" v-loading="loading">
          <div
            v-for="m in markers"
            :key="m.id"
            class="marker-item"
            :class="{ active: selectedId === m.id }"
            @click="focusMarker(m)"
          >
            <div class="marker-title">
              <el-tag :type="severityTagType(m.severity)" size="small">{{ m.severity }}</el-tag>
              <span>{{ m.roadName || m.address || '未知位置' }}</span>
            </div>
            <div class="marker-meta">
              <span>{{ m.damageType }}</span>
              <span v-if="m.status" class="status-badge">{{ statusLabel(m.status) }}</span>
            </div>
          </div>
          <el-empty v-if="!loading && markers.length === 0" description="暂无点位数据" />
        </div>
      </el-card>
    </div>

    <!-- 点位详情对话框 -->
    <el-dialog v-model="detailVisible" title="点位详情" width="600px" :close-on-click-modal="false">
      <template v-if="detail">
        <descriptions :column="2" border size="small">
          <el-descriptions-item label="损坏类型">{{ detail.damageType }}</el-descriptions-item>
          <el-descriptions-item label="严重程度">{{ detail.severity }}</el-descriptions-item>
          <el-descriptions-item label="道路名称">{{ detail.roadName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="路段">{{ detail.roadSegment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="置信度">{{ detail.confidence ? (detail.confidence * 100).toFixed(1) + '%' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="责任部门">{{ detail.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检测时间">{{ detail.detectedAt ? formatTime(detail.detectedAt) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="工单状态">
            <el-tag v-if="detail.workOrderStatus" :type="woStatusType(detail.workOrderStatus)" size="small">
              {{ statusLabel(detail.workOrderStatus) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        </descriptions>
        <div v-if="detail.imageUrls && detail.imageUrls.length" class="detail-images">
          <el-image v-for="(url, i) in detail.imageUrls" :key="i" :src="url" :preview-src-list="detail.imageUrls" style="width:100px;height:80px;margin-right:8px;border-radius:4px;object-fit:cover" />
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from "vue"
import { mapApi } from "@/api/map"
import type { MapMarkerResponse, MapMarkerDetailResponse, MapStatisticsResponse } from "@/types"

// --- Stats ---
const statistics = ref([
  { label: "总点位", value: 0, key: "totalMarkers" },
  { label: "新增", value: 0, key: "newMarkers" },
  { label: "已修复", value: 0, key: "repairedCount" },
  { label: "待维修", value: 0, key: "pendingRepair" },
  { label: "高严重度", value: 0, key: "highSeverityCount" },
])

const damageTypes = [
  { value: "CRACK", label: "裂缝" },
  { value: "POTHOLE", label: "坑槽" },
  { value: "MARKING_DAMAGE", label: "标线损坏" },
  { value: "ROAD_SPILL", label: "路面抛洒" },
  { value: "UNKNOWN", label: "未知" },
]

// --- State ---
const mapRef = ref<HTMLDivElement>()
const markers = ref<MapMarkerResponse[]>([])
const loading = ref(false)
const selectedId = ref<number | null>(null)
const detailVisible = ref(false)
const detail = ref<MapMarkerDetailResponse | null>(null)

const filters = reactive({
  damageType: undefined as string | undefined,
  severityLevel: undefined as string | undefined,
  status: undefined as string | undefined,
  keyword: undefined as string | undefined,
})

let mapInstance: any = null
let amapMarkers: any[] = []

// --- Load ---
async function loadMarkers() {
  loading.value = true
  try {
    const params: any = {}
    if (filters.damageType) params.damageType = filters.damageType
    if (filters.severityLevel) params.severityLevel = filters.severityLevel
    if (filters.status) params.status = filters.status
    if (filters.keyword) params.keyword = filters.keyword

    const [markerRes, statsRes] = await Promise.all([
      mapApi.markers(params),
      mapApi.statistics(),
    ])
    markers.value = markerRes.data.data || []
    updateStats(statsRes.data.data)
    await nextTick()
    renderMarkers()
  } catch (e) {
    console.error("加载地图数据失败:", e)
  } finally {
    loading.value = false
  }
}

function updateStats(s: MapStatisticsResponse) {
  if (!s) return
  statistics.value = [
    { label: "总点位", value: s.totalMarkers ?? 0, key: "totalMarkers" },
    { label: "新增", value: s.newMarkers ?? 0, key: "newMarkers" },
    { label: "已修复", value: s.repairedCount ?? 0, key: "repairedCount" },
    { label: "待维修", value: s.pendingRepair ?? 0, key: "pendingRepair" },
    { label: "高严重度", value: s.highSeverityCount ?? 0, key: "highSeverityCount" },
  ]
}

// --- Amap ---
function initMap() {
  if (!mapRef.value || !window.AMap) return
  mapInstance = new window.AMap.Map(mapRef.value, {
    zoom: 12,
    center: [116.397428, 39.90923],
    mapStyle: "amap://styles/light",
  })
  mapInstance.addControl(new window.AMap.ToolBar())
  mapInstance.addControl(new window.AMap.Scale())
  loadMarkers()
}

function renderMarkers() {
  if (!mapInstance) return
  // Clear old markers
  amapMarkers.forEach((m: any) => mapInstance.remove(m))
  amapMarkers = []

  markers.value.forEach((m) => {
    if (!m.longitude || !m.latitude) return
    const marker = new window.AMap.Marker({
      position: [m.longitude, m.latitude],
      title: m.roadName || m.address || "",
      label: { content: `<div style="background:#409eff;color:#fff;padding:2px 6px;border-radius:4px;font-size:12px">${m.damageType}</div>`, direction: "top", offset: new window.AMap.Pixel(0, -5) },
    })
    marker._data = m
    marker.on("click", () => focusMarker(m))
    mapInstance.add(marker)
    amapMarkers.push(marker)
  })

  // Fit bounds
  const positions = amapMarkers.filter((m: any) => m._data.longitude).map((m: any) => [m._data.longitude, m._data.latitude])
  if (positions.length > 0) {
    mapInstance.setFitView(null, false, [50, 50, 350, 50])
  }
}

function focusMarker(m: MapMarkerResponse) {
  selectedId.value = m.id
  if (mapInstance && m.longitude && m.latitude) {
    mapInstance.setCenter([m.longitude, m.latitude])
    mapInstance.setZoom(15)
  }
  // Load detail
  mapApi.markerDetail(m.id).then((res) => {
    detail.value = res.data.data
    detailVisible.value = true
  }).catch(() => {})
}

// --- Helpers ---
function resetFilters() {
  filters.damageType = undefined
  filters.severityLevel = undefined
  filters.status = undefined
  filters.keyword = undefined
  loadMarkers()
}

function severityTagType(s: string) {
  if (s === "HIGH") return "danger"
  if (s === "MEDIUM") return "warning"
  return "info"
}

function woStatusType(s: string) {
  if (s === "COMPLETED" || s === "CLOSED") return "success"
  if (s === "IN_PROGRESS") return "warning"
  if (s === "ASSIGNED") return "primary"
  return "info"
}

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING_ASSIGNMENT: "待指派", ASSIGNED: "已指派", IN_PROGRESS: "处理中",
    COMPLETED: "已完成", CLOSED: "已关闭", CANCELLED: "已取消",
    LOW: "低", MEDIUM: "中", HIGH: "高",
  }
  return map[s] || s
}

function formatTime(t: string) {
  return t?.replace("T", " ").substring(0, 19) || "-"
}

// --- Lifecycle ---
onMounted(() => {
  const tryInit = () => {
    if (mapRef.value && window.AMap) {
      initMap()
    } else {
      setTimeout(tryInit, 300)
    }
  }
  nextTick(() => tryInit())
})

onBeforeUnmount(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<script lang="ts">
// Amap global type
declare global {
  interface Window {
    AMap: any
  }
}
</script>

<style scoped>
.map-view { padding: 16px; height: calc(100vh - 100px); display: flex; flex-direction: column; }
.stats-row { margin-bottom: 12px; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: 700; color: #409eff; }
.stat-label { font-size: 13px; color: #666; margin-top: 4px; }
.filter-card { margin-bottom: 12px; }
.map-container { flex: 1; display: flex; gap: 12px; min-height: 0; }
.amap-wrapper { flex: 1; border-radius: 8px; overflow: hidden; }
.marker-sidebar { width: 320px; flex-shrink: 0; display: flex; flex-direction: column; overflow: hidden; }
.marker-list { flex: 1; overflow-y: auto; }
.marker-item { padding: 10px 12px; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background .2s; }
.marker-item:hover { background: #f5f7fa; }
.marker-item.active { background: #ecf5ff; border-left: 3px solid #409eff; }
.marker-title { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; font-size: 14px; }
.marker-meta { display: flex; justify-content: space-between; font-size: 12px; color: #999; }
.status-badge { background: #e6f7ff; color: #1890ff; padding: 1px 6px; border-radius: 3px; }
.detail-images { margin-top: 12px; display: flex; flex-wrap: wrap; }
</style>
