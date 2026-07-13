<template>
  <div v-if="visible" ref="layerContainer" class="rh-layer" :style="{ zIndex }">
    <canvas ref="canvasRef" class="rh-canvas" />
    <!-- Hover 悬浮信息弹窗 -->
    <div v-if="hoveredRoad" class="rh-tooltip" :style="tooltipStyle">
      <div class="rh-tt-head">{{ hoveredRoad.roadName }}</div>
      <div class="rh-tt-body">
        <div class="rh-tt-row"><span class="rh-tt-lbl">病害总数</span><span class="rh-tt-val">{{ hoveredRoad.totalCount }}</span></div>
        <div class="rh-tt-row"><span class="rh-tt-lbl">轻度 (LOW)</span><span class="rh-tt-val">{{ hoveredRoad.lowCount }} ({{ roadPercent(hoveredRoad.lowCount, hoveredRoad.totalCount) }})</span></div>
        <div class="rh-tt-row"><span class="rh-tt-lbl">中度 (MEDIUM)</span><span class="rh-tt-val">{{ hoveredRoad.mediumCount }} ({{ roadPercent(hoveredRoad.mediumCount, hoveredRoad.totalCount) }})</span></div>
        <div class="rh-tt-row"><span class="rh-tt-lbl">重度 (HIGH)</span><span class="rh-tt-val">{{ hoveredRoad.highCount }} ({{ roadPercent(hoveredRoad.highCount, hoveredRoad.totalCount) }})</span></div>
        <div class="rh-tt-row"><span class="rh-tt-lbl">高危 (CRITICAL)</span><span class="rh-tt-val">{{ hoveredRoad.criticalCount }} ({{ roadPercent(hoveredRoad.criticalCount, hoveredRoad.totalCount) }})</span></div>
        <div class="rh-tt-divider"></div>
        <div class="rh-tt-row"><span class="rh-tt-lbl">平均破损等级</span><span class="rh-tt-val"><span class="rh-tt-weight-dot" :style="{ background: weightToColor(hoveredRoad.avgWeight) }"></span>{{ hoveredRoad.avgWeight.toFixed(3) }}</span></div>
        <div class="rh-tt-breakdown" v-if="Object.keys(hoveredRoad.damageTypeBreakdown).length">
          <div class="rh-tt-row" v-for="(cnt, type) in hoveredRoad.damageTypeBreakdown" :key="type">
            <span class="rh-tt-lbl">{{ damageTypeLabel(type) }}</span><span class="rh-tt-val">{{ cnt }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import type { RoadDiseaseSummaryResponse } from '@/types'
import { weightToColor, SEVERITY_WEIGHTS, generateRoadSummaries } from '@/utils/roadHeatmapMockData'
import type { RoadSegmentSummary } from '@/utils/roadHeatmapMockData'

const props = withDefaults(defineProps<{
  roads: RoadDiseaseSummaryResponse[]
  visible?: boolean
  mapInstance?: any
  zIndex?: number
}>(), { visible: true, zIndex: 100 })

const emit = defineEmits<{
  (e: 'road-click', roadId: number, roadName: string): void
}>()

const layerContainer = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const hoveredRoad = ref<RoadSegmentSummary | null>(null)
const tooltipStyle = ref({ left: '0px', top: '0px' })

// 预计算每条道路的摘要
const roadSummaries = ref<RoadSegmentSummary[]>([])

watch(() => props.roads, () => {
  roadSummaries.value = generateRoadSummaries(props.roads)
}, { immediate: true })

/** 根据道路路径和病害列表计算每段路径的权重插值 */
function computePathWeights(road: RoadDiseaseSummaryResponse): { path: { x: number; y: number; weight: number; color: string }[] } {
  const path = road.pathPoints
  if (!path || path.length < 2) return { path: [] }
  const map = props.mapInstance
  if (!map) return { path: [] }

  // 1. 将经纬度转为像素坐标
  const pixelPath = path.map((p: number[]) => {
    const px = map.lngLatToContainer(new window.AMap.LngLat(p[0], p[1]))
    return { x: px.x, y: px.y, lng: p[0], lat: p[1] }
  })

  // 2. 计算每个path点的权重
  const weights = new Array(pixelPath.length).fill(0)
  const dp = road.diseasePoints || []
  if (dp.length === 0) {
    // 无病害数据时使用默认浅色
    return { path: pixelPath.map(p => ({ x: p.x, y: p.y, weight: 0, color: '#f8fafc' })) }
  }

  // 3. 每个病害点找到最近的path线段，分配权重
  for (const d of dp) {
    const weight = SEVERITY_WEIGHTS[d.severity] || 0.25
    const dLng = d.lng; const dLat = d.lat
    let minDist = Infinity; let closestIdx = 0
    // 找最近的点
    for (let i = 0; i < pixelPath.length; i++) {
      const dx = pixelPath[i].lng - dLng; const dy = pixelPath[i].lat - dLat
      const dist = dx * dx + dy * dy
      if (dist < minDist) { minDist = dist; closestIdx = i }
    }
    // 向周围扩散权重（高斯分布）
    const spread = Math.max(3, Math.floor(pixelPath.length * 0.12))
    for (let i = -spread; i <= spread; i++) {
      const idx = closestIdx + i
      if (idx >= 0 && idx < pixelPath.length) {
        const influence = Math.exp(-(i * i) / (2 * (spread / 2) * (spread / 2)))
        weights[idx] = Math.max(weights[idx], weight * influence)
      }
    }
  }

  // 4. 平滑权重（滑动平均）
  const smoothed = [...weights]
  const windowSize = 3
  for (let i = 0; i < weights.length; i++) {
    let sum = 0; let count = 0
    for (let j = -windowSize; j <= windowSize; j++) {
      const idx = i + j
      if (idx >= 0 && idx < weights.length) { sum += weights[idx]; count++ }
    }
    smoothed[i] = sum / count
  }

  return { path: pixelPath.map((p, i) => ({ x: p.x, y: p.y, weight: smoothed[i], color: weightToColor(smoothed[i]) })) }
}

/** 获取路段摘要（用于快速 hover 检测） */
function getRoadSegmentSummary(roadId: number): RoadSegmentSummary | undefined {
  return roadSummaries.value.find(s => s.roadId === roadId)
}

/** 查找鼠标悬停的道路 */
function findHoveredRoad(mx: number, my: number): { roadId: number; roadName: string } | null {
  const map = props.mapInstance
  if (!map) return null

  for (const road of props.roads) {
    const pp = road.pathPoints
    if (!pp || pp.length < 2) continue
    // 检查鼠标是否靠近这条路的任何点
    for (let i = 0; i < pp.length - 1; i++) {
      const p1 = map.lngLatToContainer(new window.AMap.LngLat(pp[i][0], pp[i][1]))
      const p2 = map.lngLatToContainer(new window.AMap.LngLat(pp[i + 1][0], pp[i + 1][1]))
      const d = pointToSegmentDist(mx, my, p1.x, p1.y, p2.x, p2.y)
      if (d < 12) return { roadId: road.roadId, roadName: road.roadName }
    }
  }
  return null
}

/** 点到线段的距离 */
function pointToSegmentDist(px: number, py: number, ax: number, ay: number, bx: number, by: number): number {
  const dx = bx - ax; const dy = by - ay
  const lenSq = dx * dx + dy * dy
  if (lenSq === 0) return Math.sqrt((px - ax) ** 2 + (py - ay) ** 2)
  let t = ((px - ax) * dx + (py - ay) * dy) / lenSq
  t = Math.max(0, Math.min(1, t))
  const nearX = ax + t * dx; const nearY = ay + t * dy
  return Math.sqrt((px - nearX) ** 2 + (py - nearY) ** 2)
}

function damageTypeLabel(type: string): string {
  const map: Record<string, string> = {
    POTHOLE: '重度破损', CRACK: '中度裂缝', MARKING_DAMAGE: '细微裂纹', ROAD_SPILL: '路面抛洒', UNKNOWN: '未知'
  }
  return map[type] || type
}

function roadPercent(count: number, total: number): string {
  return total > 0 ? (count / total * 100).toFixed(1) + '%' : '-'
}

/** ---- 核心渲染 ---- */
function renderLayer() {
  const canvas = canvasRef.value
  const container = layerContainer.value
  const map = props.mapInstance
  if (!canvas || !container || !map || !props.roads.length) return

  const rect = container.getBoundingClientRect()
  canvas.width = rect.width
  canvas.height = rect.height
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // --- 图层1: 底图道路矢量底色（灰色）---
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  for (const road of props.roads) {
    const pp = road.pathPoints
    if (!pp || pp.length < 2) continue
    ctx.beginPath()
    for (let i = 0; i < pp.length; i++) {
      const px = map.lngLatToContainer(new window.AMap.LngLat(pp[i][0], pp[i][1]))
      if (i === 0) ctx.moveTo(px.x, px.y); else ctx.lineTo(px.x, px.y)
    }
    ctx.strokeStyle = '#d1d5db'
    ctx.lineWidth = 10
    ctx.stroke()
  }

  // --- 图层2: 热力渐变层 ---
  for (const road of props.roads) {
    const result = computePathWeights(road)
    const compPath = result.path
    if (compPath.length < 2) continue

    // 2a. 发光底层（扩散辉光效果）
    for (const px of compPath) { ctx.shadowColor = px.color; break } // set first color
    ctx.shadowBlur = 20
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    for (let i = 0; i < compPath.length - 1; i++) {
      const p0 = compPath[i]; const p1 = compPath[i + 1]
      ctx.beginPath(); ctx.moveTo(p0.x, p0.y); ctx.lineTo(p1.x, p1.y)
      ctx.strokeStyle = p0.color
      ctx.lineWidth = 12
      ctx.globalAlpha = 0.25
      ctx.stroke()
    }
    ctx.shadowBlur = 0
    ctx.globalAlpha = 1

    // 2b. 主热力渐变线（逐段绘制实现渐变）
    for (let i = 0; i < compPath.length - 1; i++) {
      const p0 = compPath[i]; const p1 = compPath[i + 1]
      ctx.beginPath(); ctx.moveTo(p0.x, p0.y); ctx.lineTo(p1.x, p1.y)
      // 取前后两点的平均颜色渐变
      const midWeight = (p0.weight + p1.weight) / 2
      ctx.strokeStyle = weightToColor(midWeight)
      ctx.lineWidth = 6
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.globalAlpha = Math.max(0.5, Math.min(1, midWeight + 0.3))
      ctx.stroke()
    }
    ctx.globalAlpha = 1
  }

  // --- 图层3: 病害点标记 ---
  for (const road of props.roads) {
    for (const dp of (road.diseasePoints || [])) {
      const px = map.lngLatToContainer(new window.AMap.LngLat(dp.lng, dp.lat))
      const w = SEVERITY_WEIGHTS[dp.severity] || 0.25
      const color = weightToColor(w)

      // 标记外圈（光晕）
      ctx.beginPath(); ctx.arc(px.x, px.y, 6, 0, Math.PI * 2)
      ctx.fillStyle = color; ctx.globalAlpha = 0.3; ctx.fill()
      ctx.globalAlpha = 1

      // 标记中心点
      ctx.beginPath(); ctx.arc(px.x, px.y, 3, 0, Math.PI * 2)
      ctx.fillStyle = color; ctx.fill()
      ctx.strokeStyle = '#fff'; ctx.lineWidth = 1.5; ctx.stroke()
    }
  }
}

/** ---- Hover & Click ---- */
let lastHovered: { roadId: number; roadName: string } | null = null
function onMouseMove(e: MouseEvent) {
  const rect = (layerContainer.value as HTMLElement).getBoundingClientRect()
  const mx = e.clientX - rect.left; const my = e.clientY - rect.top
  const found = findHoveredRoad(mx, my)

  if (found) {
    const summary = getRoadSegmentSummary(found.roadId)
    if (summary) {
      hoveredRoad.value = summary
      tooltipStyle.value = {
        left: Math.min(mx + 14, rect.width - 240) + 'px',
        top: Math.min(my - 10, rect.height - 280) + 'px',
      }
    }
    if (layerContainer.value) layerContainer.value.style.cursor = 'pointer'
  } else {
    hoveredRoad.value = null
    if (layerContainer.value) layerContainer.value.style.cursor = 'default'
  }
  lastHovered = found
}

function onClick() {
  if (lastHovered) {
    emit('road-click', lastHovered.roadId, lastHovered.roadName)
  }
}

function onMouseLeave() { hoveredRoad.value = null }

/** ---- 地图事件绑定 ---- */
let animFrameId = 0
function scheduleRedraw() {
  if (animFrameId) cancelAnimationFrame(animFrameId)
  animFrameId = requestAnimationFrame(() => renderLayer())
}

watch(() => props.roads, () => nextTick(() => { roadSummaries.value = generateRoadSummaries(props.roads); scheduleRedraw() }), { deep: false })
watch(() => props.visible, (v) => { if (v) nextTick(scheduleRedraw) })

let cleanups: (() => void)[] = []
function bindMapEvents() {
  const map = props.mapInstance
  if (!map) return
  cleanups.forEach(fn => fn()); cleanups = []
  const events = ['mapmove', 'zoomchange', 'moveend', 'resize']
  for (const ev of events) {
    const h = () => scheduleRedraw()
    map.on(ev, h)
    cleanups.push(() => map.off(ev, h))
  }
  const ms = layerContainer.value
  if (ms) {
    ms.addEventListener('mousemove', onMouseMove)
    ms.addEventListener('click', onClick)
    ms.addEventListener('mouseleave', onMouseLeave)
    cleanups.push(() => { ms.removeEventListener('mousemove', onMouseMove); ms.removeEventListener('click', onClick); ms.removeEventListener('mouseleave', onMouseLeave) })
  }
  scheduleRedraw()
}

onMounted(() => nextTick(bindMapEvents))
onUnmounted(() => {
  cleanups.forEach(fn => fn()); cleanups = []
  if (animFrameId) cancelAnimationFrame(animFrameId)
})
</script>

<style scoped>
.rh-layer {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  overflow: hidden; pointer-events: auto;
}
.rh-canvas {
  display: block; width: 100%; height: 100%; pointer-events: none;
}
.rh-tooltip {
  position: absolute;
  background: rgba(15, 23, 42, 0.94);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px; padding: 12px 14px;
  min-width: 210px; max-width: 240px;
  pointer-events: none; z-index: 9999;
  box-shadow: 0 8px 24px rgba(0,0,0,0.35);
  color: #f1f5f9; font-size: 12px; line-height: 1.5;
}
.rh-tt-head {
  font-weight: 700; font-size: 14px; color: #fff;
  margin-bottom: 8px; padding-bottom: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.rh-tt-body { display: flex; flex-direction: column; gap: 3px; }
.rh-tt-row { display: flex; justify-content: space-between; align-items: center; font-size: 11px; }
.rh-tt-lbl { color: rgba(255,255,255,0.5); flex-shrink: 0; }
.rh-tt-val { color: #e2e8f0; text-align: right; display: flex; align-items: center; gap: 4px; }
.rh-tt-weight-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; display: inline-block; }
.rh-tt-divider { height: 1px; background: rgba(255,255,255,0.08); margin: 4px 0; }
.rh-tt-breakdown { margin-top: 2px; padding-top: 4px; border-top: 1px solid rgba(255,255,255,0.06); }
</style>
