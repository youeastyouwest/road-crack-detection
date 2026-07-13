<template>
  <!--
    病害标记线图层 - Canvas 叠加层
    ──────────────────────────────────────────────────
    功能：
      1. 根据 DamageType 匹配颜色：重度破损→红色，中度裂缝→粉色，细微裂纹→蓝色
      2. 根据 SeverityLevel 控制线条粗细：严重→2px、中等→1.5px、轻微→1px
      3. 沿道路路径连续绘制彩色标记线
      4. 标记线置顶于道路底图上方，增加微弱发光渐变
      5. 鼠标 hover 弹出 tooltip（病害类型、严重等级、坐标、破损面积）
      6. 一键隐藏/显示切换
      7. 适配大屏，海量点位无卡顿
  -->
  <div v-if="visible" ref="layerContainer" class="disease-layer" :style="layerStyle">
    <canvas ref="canvasRef" :width="canvasWidth" :height="canvasHeight"
            style="display:block;width:100%;height:100%;pointer-events:none" />
    <!-- Hover Tooltip -->
    <div v-if="hoveredSegment" class="ds-tooltip" :style="tooltipStyle">
      <div class="ds-tooltip-head">{{ hoveredSegment.damageType || '未知病害' }}</div>
      <div class="ds-tooltip-body">
        <div class="ds-tooltip-row">
          <span class="ds-tt-lbl">严重等级</span>
          <span class="ds-tt-val">
            <span class="ds-sev-indicator" :style="{background: severityColor(hoveredSegment.severity)}"></span>
            {{ severityLabel(hoveredSegment.severity) }}
          </span>
        </div>
        <div class="ds-tooltip-row">
          <span class="ds-tt-lbl">坐标</span>
          <span class="ds-tt-val">{{ formatCoord(hoveredSegment.lng, hoveredSegment.lat) }}</span>
        </div>
        <div class="ds-tooltip-row">
          <span class="ds-tt-lbl">破损面积</span>
          <span class="ds-tt-val">{{ hoveredSegment.area ? hoveredSegment.area + ' m²' : '--' }}</span>
        </div>
        <div class="ds-tooltip-row" v-if="hoveredSegment.confidence != null">
          <span class="ds-tt-lbl">置信度</span>
          <span class="ds-tt-val">{{ (hoveredSegment.confidence * 100).toFixed(1) + '%' }}</span>
        </div>
        <div class="ds-tooltip-row" v-if="hoveredSegment.roadName">
          <span class="ds-tt-lbl">所属道路</span>
          <span class="ds-tt-val">{{ hoveredSegment.roadName }}</span>
        </div>
        <div class="ds-tooltip-row" v-if="hoveredSegment.detectionTime">
          <span class="ds-tt-lbl">检测时间</span>
          <span class="ds-tt-val">{{ hoveredSegment.detectionTime }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick, shallowRef } from 'vue'
import type { RoadDiseaseSummaryResponse, DiseasePoint } from '@/types'

/* ============================================================
   Props
   ============================================================ */
const props = withDefaults(defineProps<{
  /** 道路病害汇总数据（来自后端 /map/roads-with-disease） */
  roads: RoadDiseaseSummaryResponse[]
  /** 是否可见（切换开关用） */
  visible?: boolean
  /** 高德地图实例（父级传入） */
  mapInstance?: any
  /** Canvas 层 z-index（确保置顶） */
  zIndex?: number
}>(), {
  visible: true,
  zIndex: 1001,
})

/* ============================================================
   颜色映射表（DamageType → 颜色）
   ────────────────────────────────────────
   重度破损(POTHOLE) → #ef4444（红色）
   中度裂缝(CRACK)  → #ec4899（粉色）
   细微裂纹(MARKING_DAMAGE) → #3b82f6（蓝色）
   ============================================================ */
const DAMAGE_COLORS: Record<string, string> = {
  '重度破损': '#ef4444',
  '中度裂缝': '#ec4899',
  '细微裂纹': '#3b82f6',
  'POTHOLE': '#ef4444',
  'CRACK': '#ec4899',
  'MARKING_DAMAGE': '#3b82f6',
  'ROAD_SPILL': '#f97316',
  'UNKNOWN': '#94a3b8',
  default: '#6366f1',
}
function getDamageColor(type: string): string {
  return DAMAGE_COLORS[type] || DAMAGE_COLORS.default
}

/* ============================================================
   宽度映射表（SeverityLevel → 像素宽度）
   ────────────────────────────────────────
   严重(HIGH) → 2px，中等(MEDIUM) → 1.5px，轻微(LOW) → 1px
   ============================================================ */
const SEVERITY_WIDTHS: Record<string, number> = {
  '严重': 2, '中等': 1.5, '轻微': 1,
  'HIGH': 2, 'MEDIUM': 1.5, 'LOW': 1,
  default: 1,
}
function getSeverityWidth(sev: string): number {
  return SEVERITY_WIDTHS[sev] || SEVERITY_WIDTHS.default
}

function severityLabel(sev: string): string {
  const map: Record<string, string> = {
    'HIGH': '严重', 'MEDIUM': '中等', 'LOW': '轻微',
    '严重': '严重', '中等': '中等', '轻微': '轻微',
  }
  return map[sev] || sev
}
function severityColor(sev: string): string {
  const map: Record<string, string> = {
    '严重': '#ef4444', '中等': '#f59e0b', '轻微': '#22c55e',
    'HIGH': '#ef4444', 'MEDIUM': '#f59e0b', 'LOW': '#22c55e',
  }
  return map[sev] || '#94a3b8'
}
function formatCoord(lng: number, lat: number): string {
  if (lng == null || lat == null) return '--'
  return lat.toFixed(4) + ', ' + lng.toFixed(4)
}

/* ============================================================
   状态
   ============================================================ */
const layerContainer = ref<HTMLDivElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const canvasWidth = ref(1920)
const canvasHeight = ref(1080)
const hoveredSegment = ref<(DiseasePoint & { area?: number; roadName?: string }) | null>(null)
const tooltipStyle = ref({ left: '0px', top: '0px' })
const renderSegments = shallowRef<any[]>([])

/* ============================================================
   样式
   ============================================================ */
const layerStyle = computed(() => ({
  position: 'absolute' as const,
  top: 0, left: 0, width: '100%', height: '100%',
  zIndex: props.zIndex,
  pointerEvents: 'none' as const,
}))

/* ============================================================
   工具 - 点到线段的距离
   ============================================================ */
function pointToSegmentDist(px: number, py: number, ax: number, ay: number, bx: number, by: number): number {
  const dx = bx - ax, dy = by - ay
  const lenSq = dx * dx + dy * dy
  if (lenSq === 0) return Math.hypot(px - ax, py - ay)
  let t = ((px - ax) * dx + (py - ay) * dy) / lenSq
  t = Math.max(0, Math.min(1, t))
  return Math.hypot(px - (ax + t * dx), py - (ay + t * dy))
}

/* ============================================================
   核心渲染 - 在 Canvas 上绘制彩色标记线段
   ────────────────────────────────────────
   使用两次绘制实现发光效果：
     1. 发光底层（lighter composite + 宽线 + 低透明度）
     2. 内部实线（正常绘制）
   ============================================================ */
function renderLayer() {
  const canvas = canvasRef.value
  const container = layerContainer.value
  const map = props.mapInstance
  if (!canvas || !container || !map) return

  const rect = container.getBoundingClientRect()
  const dpr = window.devicePixelRatio || 1
  const w = Math.round(rect.width * dpr)
  const h = Math.round(rect.height * dpr)
  if (w <= 0 || h <= 0) return

  if (canvas.width !== w || canvas.height !== h) {
    canvas.width = w
    canvas.height = h
    canvasWidth.value = w
    canvasHeight.value = h
  }
  canvas.style.width = rect.width + 'px'
  canvas.style.height = rect.height + 'px'

  const ctx = canvas.getContext('2d')!
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, rect.width, rect.height)

  // 收集所有可渲染的病害线段
  const segs: any[] = []
  const roads = props.roads || []
  for (const road of roads) {
    if (!road.pathPoints || road.pathPoints.length < 2) continue
    if (!road.diseasePoints || road.diseasePoints.length === 0) continue

    // 将道路投影到屏幕坐标
    type Point = { x: number; y: number }
    const pathPixels: Point[] = road.pathPoints.map((p: number[]) => {
      const px = map.lngLatToContainer(new (window as any).AMap.LngLat(p[0], p[1]))
      return { x: px.x, y: px.y }
    })

    for (const dp of road.diseasePoints) {
      const px = map.lngLatToContainer(new (window as any).AMap.LngLat(dp.lng, dp.lat))
      if (px.x < -80 || px.x > rect.width + 80 || px.y < -80 || px.y > rect.height + 80) continue

      // 找最近路径段
      let minDist = Infinity
      let segIdx = -1
      for (let i = 0; i < pathPixels.length - 1; i++) {
        const d = pointToSegmentDist(px.x, px.y, pathPixels[i].x, pathPixels[i].y, pathPixels[i + 1].x, pathPixels[i + 1].y)
        if (d < minDist) { minDist = d; segIdx = i }
      }
      if (minDist > 30) continue

      // 取以该病害为中心的局部线段
      const start = Math.max(0, segIdx - 1)
      const end = Math.min(pathPixels.length - 1, segIdx + 2)
      const pts: Point[] = []
      for (let j = start; j <= end; j++) pts.push(pathPixels[j])

      segs.push({
        points: pts, color: getDamageColor(dp.damageType),
        width: getSeverityWidth(dp.severity),
        data: dp, roadName: road.roadName,
      })
    }
  }

  // ---- 批量绘制 ----
  // 1) 发光底层
  ctx.save()
  ctx.globalCompositeOperation = 'lighter'
  for (const seg of segs) {
    if (!seg.points || seg.points.length < 2) continue
    ctx.beginPath()
    ctx.moveTo(seg.points[0].x, seg.points[0].y)
    for (let i = 1; i < seg.points.length; i++) ctx.lineTo(seg.points[i].x, seg.points[i].y)
    ctx.strokeStyle = seg.color
    ctx.lineWidth = seg.width + 4
    ctx.globalAlpha = 0.18
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.stroke()
  }
  ctx.restore()

  // 2) 内部实线
  for (const seg of segs) {
    if (!seg.points || seg.points.length < 2) continue
    ctx.beginPath()
    ctx.moveTo(seg.points[0].x, seg.points[0].y)
    for (let i = 1; i < seg.points.length; i++) ctx.lineTo(seg.points[i].x, seg.points[i].y)
    ctx.strokeStyle = seg.color
    ctx.lineWidth = seg.width
    ctx.globalAlpha = 0.92
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.stroke()
  }
  ctx.globalAlpha = 1

  renderSegments.value = segs
}

/* ============================================================
   Hover 检测
   ============================================================ */
function onMouseMove(e: MouseEvent) {
  if (!renderSegments.value.length) { hoveredSegment.value = null; return }
  const rect = (layerContainer.value as HTMLElement).getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top

  let found: any = null
  let minDist = 8
  for (const seg of renderSegments.value) {
    if (!seg.points || seg.points.length < 2) continue
    for (let i = 0; i < seg.points.length - 1; i++) {
      const d = pointToSegmentDist(mx, my, seg.points[i].x, seg.points[i].y, seg.points[i + 1].x, seg.points[i + 1].y)
      if (d < minDist) { minDist = d; found = seg }
    }
  }

  if (found) {
    hoveredSegment.value = { ...found.data, area: found.data.area, roadName: found.roadName }
    tooltipStyle.value = {
      left: Math.min(e.clientX - rect.left + 12, rect.width - 220) + 'px',
      top: Math.min(e.clientY - rect.top - 10, rect.height - 200) + 'px',
    }
  } else {
    hoveredSegment.value = null
  }
}
function onMouseLeave() { hoveredSegment.value = null }

/* ============================================================
   地图事件绑定 → 重绘
   ============================================================ */
let animFrameId = 0
function scheduleRedraw() {
  if (animFrameId) cancelAnimationFrame(animFrameId)
  animFrameId = requestAnimationFrame(() => renderLayer())
}

watch(() => props.roads, () => nextTick(scheduleRedraw), { deep: false })
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
  scheduleRedraw()
}

onMounted(() => nextTick(bindMapEvents))
onUnmounted(() => { cleanups.forEach(fn => fn()); cleanups = []; if (animFrameId) cancelAnimationFrame(animFrameId) })
</script>

<style scoped>
.disease-layer {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  overflow: hidden;
}
.ds-tooltip {
  position: absolute;
  background: rgba(15, 23, 42, 0.92);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px; padding: 10px 14px;
  min-width: 180px; max-width: 220px;
  pointer-events: none; z-index: 9999;
  box-shadow: 0 8px 24px rgba(0,0,0,0.35);
  color: #f1f5f9; font-size: 12px; line-height: 1.5;
}
.ds-tooltip-head {
  font-weight: 700; font-size: 13px; color: #fff;
  margin-bottom: 6px; padding-bottom: 6px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.ds-tooltip-body { display: flex; flex-direction: column; gap: 3px; }
.ds-tooltip-row {
  display: flex; justify-content: space-between; align-items: center; font-size: 11px;
}
.ds-tt-lbl { color: rgba(255,255,255,0.5); flex-shrink: 0; }
.ds-tt-val { color: #e2e8f0; text-align: right; display: flex; align-items: center; gap: 4px; }
.ds-sev-indicator { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
</style>
