<template>
  <div class="div-wrap" ref="wrapper">
    <div v-if="!imageLoaded" class="div-loading">加载图片中...</div>
    <div v-show="imageLoaded" class="div-stage" :style="{ width: stageW + 'px', height: stageH + 'px' }">
      <img ref="imgEl" :src="imageUrl" class="div-img" @load="onImgLoad" @error="onImgError" crossorigin="anonymous" />
      <canvas v-show="imageLoaded" ref="canvasEl" class="div-canvas" :width="stageW" :height="stageH" :style="{ width: stageW + 'px', height: stageH + 'px' }" />
      <!-- 图例 -->
      <div class="div-legend">
        <div class="legend-item" v-for="l in legendItems" :key="l.label">
          <span class="legend-dot" :style="{ background: l.color }"></span>
          <span class="legend-label">{{ l.label }}</span>
        </div>
      </div>
    </div>
    <div v-if="imgError" class="div-error">图片加载失败</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, watchEffect, nextTick } from "vue"
import type { DetectionItemResponse } from "@/types"

const props = defineProps<{
  imageUrl: string
  detections: DetectionItemResponse[]
}>()

const wrapper = ref<HTMLElement>()
const imgEl = ref<HTMLImageElement>()
const canvasEl = ref<HTMLCanvasElement>()
const stageW = ref(600)
const stageH = ref(400)
const imageLoaded = ref(false)
const imgError = ref(false)

const DTYPE_COLORS: Record<string, string> = {
  // 纵向裂缝 → 红色
  LONGITUDINAL_CRACK: "#ef4444",
  NET_CRACK: "#ef4444",
  // 横向裂缝 → 黄色
  TRANSVERSE_CRACK: "#eab308",
  // 坑槽 → 橙色
  POTHOLE: "#f97316",
  // 标线损坏 → 蓝色
  MARKING_DAMAGE: "#3b82f6",
  // 路面抛洒 → 紫色
  ROAD_SPILL: "#a855f7",
  // 其他 → 灰色
  OTHER: "#6b7280",
  CRACK: "#6366f1",
}

const legendItems = [
  { label: "纵向裂缝", color: "#ef4444" },
  { label: "横向裂缝", color: "#eab308" },
  { label: "坑槽", color: "#f97316" },
  { label: "标线损坏", color: "#3b82f6" },
  { label: "路面抛洒", color: "#a855f7" },
]

function onImgLoad() {
  if (!imgEl.value) return
  const img = imgEl.value
  // 限制最大宽度不超过容器
  const maxW = wrapper.value?.clientWidth || 600
  const scale = Math.min(maxW / img.naturalWidth, 0.95)
  stageW.value = Math.round(img.naturalWidth * scale)
  stageH.value = Math.round(img.naturalHeight * scale)
  imageLoaded.value = true
  requestAnimationFrame(() => {
    requestAnimationFrame(drawBoxes)
  })
}

function onImgError() {
  imgError.value = true
}

function drawBoxes() {
  if (!canvasEl.value || !imgEl.value) return
  const canvas = canvasEl.value
  const ctx = canvas.getContext("2d")
  if (!ctx) return

  ctx.clearRect(0, 0, stageW.value, stageH.value)

  const img = imgEl.value
  const scaleX = stageW.value / img.naturalWidth
  const scaleY = stageH.value / img.naturalHeight

  for (const item of props.detections) {
    if (!item.boundingBox) continue
    const { x, y, width, height } = item.boundingBox
    const rx = x * scaleX
    const ry = y * scaleY
    const rw = width * scaleX
    const rh = height * scaleY

    const color = DTYPE_COLORS[item.damageType] || "#6366f1"
    const dmgLabel = {
      LONGITUDINAL_CRACK: "纵向裂缝",
      TRANSVERSE_CRACK: "横向裂缝",
      NET_CRACK: "网状裂缝",
      POTHOLE: "坑槽",
      MARKING_DAMAGE: "标线损坏",
      ROAD_SPILL: "路面抛洒",
      CRACK: "裂缝",
      OTHER: "其他病害",
    }[item.damageType] || item.damageType
    const label = `${dmgLabel} ${(item.confidence * 100).toFixed(0)}%`

    // 边框
    ctx.strokeStyle = color
    ctx.lineWidth = 3
    ctx.strokeRect(rx, ry, rw, rh)

    // 边角标记
    const cornerLen = Math.min(12, rw * 0.2)
    ctx.lineWidth = 2
    // 左上
    ctx.beginPath(); ctx.moveTo(rx, ry + cornerLen); ctx.lineTo(rx, ry); ctx.lineTo(rx + cornerLen, ry); ctx.stroke()
    // 右上
    ctx.beginPath(); ctx.moveTo(rx + rw - cornerLen, ry); ctx.lineTo(rx + rw, ry); ctx.lineTo(rx + rw, ry + cornerLen); ctx.stroke()
    // 左下
    ctx.beginPath(); ctx.moveTo(rx, ry + rh - cornerLen); ctx.lineTo(rx, ry + rh); ctx.lineTo(rx + cornerLen, ry + rh); ctx.stroke()
    // 右下
    ctx.beginPath(); ctx.moveTo(rx + rw - cornerLen, ry + rh); ctx.lineTo(rx + rw, ry + rh); ctx.lineTo(rx + rw, ry + rh - cornerLen); ctx.stroke()

    // 标签背景
    ctx.font = "bold 13px Inter, system-ui, sans-serif"
    const textW = ctx.measureText(label).width
    const padX = 6
    const padY = 4
    ctx.fillStyle = color
    // 标签在边框上方，如果位置不够则放在框内下方
    let labelY = ry - 22
    if (labelY < 0) labelY = ry + rh + 4
    roundRect(ctx, rx, labelY, textW + padX * 2, 20, 6)
    ctx.fill()

    // 标签文字
    ctx.fillStyle = "#fff"
    ctx.fillText(label, rx + padX, labelY + 14)

    // 轻微半透明覆盖
    ctx.fillStyle = color + "15"
    ctx.fillRect(rx, ry, rw, rh)
  }
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.arcTo(x + w, y, x + w, y + r, r)
  ctx.lineTo(x + w, y + h - r)
  ctx.arcTo(x + w, y + h, x + w - r, y + h, r)
  ctx.lineTo(x + r, y + h)
  ctx.arcTo(x, y + h, x, y + h - r, r)
  ctx.lineTo(x, y + r)
  ctx.arcTo(x, y, x + r, y, r)
  ctx.closePath()
}

watchEffect(() => {
  // Force dependency tracking of detections (deep)
  JSON.stringify(props.detections)
  if (imageLoaded.value && canvasEl.value) {
    requestAnimationFrame(() => {
    requestAnimationFrame(drawBoxes)
  })
}
})
</script>

<style scoped>
.div-wrap { width: 100%; background: #f8fafc; border-radius: 10px; overflow: hidden; position: relative; }
.div-loading { text-align: center; padding: 60px 0; color: #94a3b8; font-size: 13px; }
.div-error { text-align: center; padding: 60px 0; color: #ef4444; font-size: 13px; }
.div-stage { position: relative; margin: 0 auto; }
.div-img { display: block; width: 100%; height: auto; }
.div-canvas { position: absolute; top: 0; left: 0; pointer-events: none; }
.div-legend { position: absolute; top: 10px; right: 10px; background: rgba(255,255,255,0.92); border-radius: 8px; padding: 8px 12px; box-shadow: 0 2px 8px rgba(0,0,0,.08); backdrop-filter: blur(4px); }
.legend-item { display: flex; align-items: center; gap: 6px; padding: 2px 0; }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.legend-label { font-size: 11px; color: #475569; white-space: nowrap; }
</style>
