<template>
  <div class="rh-view">
    <!-- 统计概要 -->
    <el-row :gutter="12" class="rh-stats">
      <el-col :span="3" v-for="s in stats" :key="s.label">
        <div class="rh-stat-card">
          <div class="rh-stat-val" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="rh-stat-lbl">{{ s.label }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card shadow="never" class="rh-filter-card">
      <el-form :inline="true" size="small">
        <el-form-item label="道路">
          <el-select v-model="filterRoad" clearable placeholder="全部道路" style="width:140px">
            <el-option v-for="r in roadOptions" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="病害严重程度">
          <el-select v-model="filterSeverity" clearable placeholder="全部" style="width:140px">
            <el-option label="轻度 (LOW)" value="LOW" />
            <el-option label="中度 (MEDIUM)" value="MEDIUM" />
            <el-option label="重度 (HIGH)" value="HIGH" />
            <el-option label="破损高危 (CRITICAL)" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilter">筛选</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 地图区域 -->
    <div class="rh-map-area">
      <div ref="mapRef" class="rh-map"></div>

      <!-- 热力图例 -->
      <div class="rh-legend">
        <div class="rh-leg-title">病害热力</div>
        <div class="rh-leg-items">
          <div class="rh-leg-item"><span class="rh-leg-bar" style="background:rgb(252,224,60)"></span><span>轻度</span></div>
          <div class="rh-leg-item"><span class="rh-leg-bar" style="background:rgb(249,115,22)"></span><span>中度</span></div>
          <div class="rh-leg-item"><span class="rh-leg-bar" style="background:rgb(220,38,38)"></span><span>重度</span></div>
          <div class="rh-leg-item"><span class="rh-leg-bar" style="background:#7F1D1D"></span><span>高危</span></div>
        </div>
      </div>

      <!-- 病害标记线层 -->
      <RoadHeatmapLayer
        :roads="filteredRoads"
        :visible="showHeatmap"
        :map-instance="mapInstance"
        :z-index="100"
        @road-click="onRoadClick"
      />

      <!-- 右侧道路列表 -->
      <el-card shadow="never" class="rh-sidebar">
        <template #header>
          <div class="rh-sidebar-head">
            <span>道路列表 ({{ filteredRoads.length }})</span>
            <el-switch v-model="showHeatmap" size="small" active-text="热力" inactive-text="隐藏" />
          </div>
        </template>
        <div class="rh-sidebar-list" v-loading="loading">
          <div
            v-for="r in roadSummaries"
            :key="r.roadId"
            class="rh-road-item"
            :class="{ active: activeRoadId === r.roadId }"
            @click="focusRoad(r.roadId)"
          >
            <div class="rh-ri-head">
              <span class="rh-ri-name">{{ r.roadName }}</span>
              <span class="rh-ri-count">病害 {{ r.totalCount }}</span>
            </div>
            <div class="rh-ri-strip">
              <span class="rh-ri-seg" v-for="i in severitySegments(r)" :key="i.label"
                    :style="{ background: i.color, flex: i.pct }" :title="i.label + ': ' + (i.pct * 100).toFixed(0) + '%'"></span>
            </div>
            <div class="rh-ri-meta">
              <span>平均等级: <strong>{{ r.avgWeight.toFixed(2) }}</strong></span>
              <span class="rh-ri-weight-bar">
                <span class="rh-ri-weight-fill" :style="{ width: (r.avgWeight * 100) + '%', background: weightToColor(r.avgWeight) }"></span>
              </span>
            </div>
          </div>
          <el-empty v-if="!loading && roadSummaries.length === 0" description="暂无道路数据" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { generateHeatmapMockData, generateRoadSummaries, weightToColor } from '@/utils/roadHeatmapMockData'
import type { RoadSegmentSummary } from '@/utils/roadHeatmapMockData'
import type { RoadDiseaseSummaryResponse } from '@/types'
import RoadHeatmapLayer from '@/components/RoadHeatmapLayer.vue'

const router = useRouter()

const mapRef = ref<HTMLElement | null>(null)
const mapInstance = ref<any>(null)
const loading = ref(false)
const showHeatmap = ref(true)
const activeRoadId = ref<number | null>(null)

// 数据
const allRoads = ref<RoadDiseaseSummaryResponse[]>([])
const filterRoad = ref<number | undefined>(undefined)
const filterSeverity = ref<string | undefined>(undefined)

const stats = ref([
  { label: '总道路数', value: 0, color: '#4361ee' },
  { label: '病害总数', value: 0, color: '#dc2626' },
  { label: '高危路段', value: 0, color: '#7F1D1D' },
  { label: '重度病害', value: 0, color: '#f97316' },
  { label: '平均破损', value: 0, color: '#6366f1' },
])

const roadOptions = computed(() => allRoads.value.map(r => ({ value: r.roadId, label: r.roadName })))

const filteredRoads = computed(() => {
  let list = allRoads.value
  if (filterRoad.value) list = list.filter(r => r.roadId === filterRoad.value)
  if (filterSeverity.value) {
    list = list.filter(r => r.diseasePoints?.some(d => d.severity === filterSeverity.value))
  }
  return list
})

const roadSummaries = computed(() => generateRoadSummaries(filteredRoads.value))

function severitySegments(r: RoadSegmentSummary): { label: string; color: string; pct: number }[] {
  const t = r.totalCount || 1
  return [
    { label: '高危', color: '#7F1D1D', pct: r.criticalCount / t },
    { label: '重度', color: '#DC2626', pct: r.highCount / t },
    { label: '中度', color: '#F97316', pct: r.mediumCount / t },
    { label: '轻度', color: '#FDE047', pct: r.lowCount / t },
  ]
}

function applyFilter() {}
function resetFilter() { filterRoad.value = undefined; filterSeverity.value = undefined }

function updateStats() {
  const r = allRoads.value
  let total = 0; let high = 0; let crit = 0; let critRoads = 0; let totalW = 0; let totalDp = 0
  for (const road of r) {
    for (const dp of (road.diseasePoints || [])) {
      total++
      if (dp.severity === 'HIGH') high++
      if (dp.severity === 'CRITICAL') { crit++; critRoads++ }
      totalW += (dp.severity === 'LOW' ? 0.25 : dp.severity === 'MEDIUM' ? 0.5 : dp.severity === 'HIGH' ? 0.75 : 1.0)
    }
    if (road.diseasePoints?.some(d => d.severity === 'CRITICAL')) critRoads++
    totalDp += (road.diseasePoints?.length || 0)
  }
  stats.value = [
    { label: '总道路数', value: r.length, color: '#4361ee' },
    { label: '病害总数', value: total, color: '#dc2626' },
    { label: '高危路段', value: critRoads, color: '#7F1D1D' },
    { label: '重度病害', value: high + crit, color: '#f97316' },
    { label: '平均破损', value: totalDp > 0 ? (totalW / totalDp).toFixed(2) : '0', color: '#6366f1' },
  ]
}

function focusRoad(roadId: number) {
  activeRoadId.value = roadId
  const road = allRoads.value.find(r => r.roadId === roadId)
  if (road && mapInstance.value) {
    const pp = road.pathPoints
    if (pp && pp.length > 0) {
      const lngs = pp.map((p: number[]) => p[0])
      const lats = pp.map((p: number[]) => p[1])
      const centerLng = (Math.min(...lngs) + Math.max(...lngs)) / 2
      const centerLat = (Math.min(...lats) + Math.max(...lats)) / 2
      mapInstance.value.setCenter([centerLng, centerLat])
      mapInstance.value.setZoom(14)
    }
  }
}

function onRoadClick(roadId: number, roadName: string) {
  activeRoadId.value = roadId
  // 跳转到该道路的工单列表页
  router.push({ name: 'WorkOrderManagement', query: { roadId: roadId.toString(), roadName } })
}

/** ---- 加载数据 ---- */
function loadData() {
  loading.value = true
  // 使用模拟数据
  allRoads.value = generateHeatmapMockData()
  updateStats()
  loading.value = false
}

/** ---- 地图初始化 ---- */
function initMap() {
  if (!mapRef.value || !window.AMap) {
    setTimeout(() => initMap(), 300)
    return
  }
  mapInstance.value = new window.AMap.Map(mapRef.value, {
    zoom: 12,
    center: [116.435, 39.935],
    mapStyle: 'amap://styles/light',
    layers: [new window.AMap.TileLayer.Satellite()], // 使用卫星底图更适配路网
  })
  mapInstance.value.addControl(new window.AMap.ToolBar())
  mapInstance.value.addControl(new window.AMap.Scale())
  // 加载数据
  loadData()
}

onMounted(() => nextTick(() => initMap()))
onBeforeUnmount(() => {
  if (mapInstance.value) { mapInstance.value.destroy(); mapInstance.value = null }
})
</script>

<style scoped>
.rh-view { padding: 16px; height: calc(100vh - 100px); display: flex; flex-direction: column; background: #f8f9fc; }
.rh-stats { margin-bottom: 10px; }
.rh-stat-card { background: #fff; border-radius: 8px; padding: 12px 10px; text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.rh-stat-val { font-size: 26px; font-weight: 800; line-height: 1.1; font-variant-numeric: tabular-nums; }
.rh-stat-lbl { font-size: 12px; color: #64748b; margin-top: 4px; }
.rh-filter-card { margin-bottom: 10px; padding: 0; }
.rh-map-area { flex: 1; display: flex; gap: 12px; min-height: 0; position: relative; }
.rh-map { flex: 1; border-radius: 10px; overflow: hidden; }
.rh-legend {
  position: absolute; left: 14px; bottom: 24px; z-index: 50;
  background: rgba(255,255,255,0.95); border: 1px solid #e2e8f0;
  border-radius: 10px; padding: 10px 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  backdrop-filter: blur(8px);
}
.rh-leg-title { font-size: 11px; font-weight: 700; color: #0f172a; margin-bottom: 6px; }
.rh-leg-items { display: flex; gap: 10px; }
.rh-leg-item { display: flex; align-items: center; gap: 4px; font-size: 10px; color: #64748b; }
.rh-leg-bar { display: inline-block; width: 18px; height: 4px; border-radius: 2px; }
.rh-sidebar { width: 300px; flex-shrink: 0; display: flex; flex-direction: column; overflow: hidden; border-radius: 10px; }
.rh-sidebar-head { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.rh-sidebar-list { flex: 1; overflow-y: auto; }
.rh-road-item { padding: 12px 14px; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: all 0.2s; }
.rh-road-item:hover { background: #f5f7fa; }
.rh-road-item.active { background: #eef2ff; border-left: 3px solid #4361ee; }
.rh-ri-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.rh-ri-name { font-size: 13px; font-weight: 600; color: #0f172a; }
.rh-ri-count { font-size: 11px; color: #94a3b8; }
.rh-ri-strip { display: flex; height: 4px; border-radius: 2px; overflow: hidden; margin-bottom: 6px; }
.rh-ri-seg { height: 100%; }
.rh-ri-meta { display: flex; justify-content: space-between; align-items: center; font-size: 11px; color: #64748b; }
.rh-ri-weight-bar { width: 60px; height: 4px; background: #e2e8f0; border-radius: 2px; overflow: hidden; display: inline-block; }
.rh-ri-weight-fill { height: 100%; border-radius: 2px; display: block; }
</style>
