<template>
  <div class="dr">
    <div class="dr-top">
      <div class="dr-top-left">
        <h2 class="dr-title">检测结果</h2>
        <span class="dr-subtitle">查看所有已完成的历史检测记录及病害详情</span>
      </div>
      <button class="dr-refresh" @click="loadTasks"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg> 刷新</button>
    </div>

    <!-- Category Tabs: 图片 / 视频 -->
    <div class="dr-cats">
      <button v-for="c in categories" :key="c.key" :class="['cat-btn', { active: activeCat === c.key }]" @click="activeCat = c.key">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <rect v-if="c.key==='MANUAL_IMAGE'" x="3" y="3" width="18" height="18" rx="2"/>
          <circle v-if="c.key==='MANUAL_IMAGE'" cx="8.5" cy="8.5" r="1.5"/>
          <path v-if="c.key==='MANUAL_IMAGE'" d="M21 15l-5-5L5 21"/>
          <polygon v-if="c.key==='MANUAL_VIDEO'" points="23 7 16 12 23 17 23 7"/>
          <rect v-if="c.key==='MANUAL_VIDEO'" x="1" y="5" width="15" height="14" rx="2"/>
        </svg>
        {{ c.label }}
        <span class="cat-count">{{ c.count }}</span>
      </button>
    </div>

    <!-- Severity Filter Tags -->
    <div v-if="tasks.length > 0" class="sev-tags">
      <button v-for="s in severityTags" :key="s.key" :class="['sev-tag', { active: activeSev === s.key }]" @click="activeSev = s.key">
        <span v-if="s.key !== ''" :class="['sev-dot', 'dot-'+s.key.toLowerCase()]"></span>
        {{ s.label }}
        <span class="sev-tag-count">{{ s.count }}</span>
      </button>
    </div>

    <!-- 超级管理员批量操作工具栏 -->
    <div v-if="authStore.isAdmin && filteredTasks.length > 0" class="batch-toolbar">
      <!-- 批量操作开关 -->
      <button
        :class="['batch-toggle', { 'batch-toggle-on': isBatchMode }]"
        @click="toggleBatchMode"
      >
        <svg v-if="!isBatchMode" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/></svg>
        <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        {{ isBatchMode ? '退出批量操作' : '批量操作' }}
      </button>

      <!-- 批量模式下的选择区 -->
      <template v-if="isBatchMode">
        <label class="batch-check-all">
          <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
          <span>全选</span>
        </label>
        <span v-if="selectedIds.size > 0" class="batch-count">已选 <strong>{{ selectedIds.size }}</strong> 条</span>
        <div v-if="selectedIds.size > 0" class="batch-actions">
          <div class="batch-severity-dropdown">
            <button class="batch-btn batch-btn-severity" @click="showSeverityMenu = !showSeverityMenu">
              调整严重等级
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </button>
            <div v-if="showSeverityMenu" class="severity-dropdown-mask" @click="showSeverityMenu = false"></div>
            <div v-if="showSeverityMenu" class="severity-dropdown-menu">
              <button v-for="opt in severityOptions" :key="opt.key" :class="['severity-option', 'sev-opt-'+opt.key.toLowerCase()]" @click="handleBatchSeverity(opt.key)">
                <span :class="['sev-dot-sm', 'dot-'+opt.key.toLowerCase()]"></span>
                {{ opt.label }}
              </button>
            </div>
          </div>
          <button class="batch-btn batch-btn-delete" @click="handleBatchDelete">删除</button>
        </div>
        <button v-if="selectedIds.size > 0" class="batch-btn-cancel" @click="clearSelection">取消选择</button>
      </template>
    </div>

    <!-- Results List -->
    <div v-if="loading" class="dr-loading"><div class="loader"></div></div>
    <div v-else-if="filteredTasks.length === 0" class="dr-empty">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#e5e7eb" stroke-width="1.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
      <div class="dr-empty-text">暂无检测结果</div>
      <div class="dr-empty-hint">前往「上传检测」提交检测任务，完成后即在此显示</div>
    </div>
    <div v-else class="dr-list">
      <div v-for="t in filteredTasks" :key="t.id" :class="['dr-card', taskSeverityLevel(t) ? 'card-sev-' + taskSeverityLevel(t)!.toLowerCase() : '', { 'card-selected': isBatchMode && selectedIds.has(t.id) }]" @click="onCardClick(t)">
        <!-- 批量模式下的卡片复选框 -->
        <div v-if="isBatchMode" class="dr-card-check" @click.stop>
          <input type="checkbox" :checked="selectedIds.has(t.id)" @change="toggleSelect(t.id)" />
        </div>
        <div class="dr-thumb">
          <img v-if="t.dataSourceType==='MANUAL_IMAGE'" :src="thumbSrc(t)" class="dr-thumb-img" @error="imgLoadError($event)" />
          <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2"/></svg>
          <!-- 左上角严重程度角标 -->
          <div v-if="taskSeverityLevel(t)" :class="['thumb-sev-badge', 'sev-'+taskSeverityLevel(t)!.toLowerCase()]">
            <svg v-if="taskSeverityLevel(t) === 'NORMAL'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="12 2 15 9 22 9 16 14 18 21 12 17 6 21 8 14 2 9 9 9"/></svg>
            {{ severityLabel(taskSeverityLevel(t)!) }}
          </div>
        </div>
        <div class="dr-card-content">
        <div class="dr-card-top">
          <span class="dr-code">{{ t.taskCode }}</span>
          <span class="dr-type-tag">{{ {MANUAL_IMAGE:'图片检测',MANUAL_VIDEO:'视频检测',DRONE_VIDEO:'无人机',CROWD_SOURCE:'众包'}[t.dataSourceType] || t.dataSourceType }}</span>
        </div>
        <div class="dr-card-body">
          <div class="dr-info"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg> {{ t.location || '未填写' }}</div>
          <div class="dr-info"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg> {{ formatTime(t.createdAt) }}</div>
          <div class="dr-stat"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg> 识别病害 <strong>{{ taskDamageCount(t) }}</strong> 处</div>
        </div>
        </div>
        <div class="dr-card-foot">
          <span v-if="taskSeverityLevel(t)" :class="['dr-sev', 'sev-'+taskSeverityLevel(t)?.toLowerCase()]">
            <svg v-if="taskSeverityLevel(t) === 'NORMAL'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="vertical-align:-1px;margin-right:2px"><polyline points="20 6 9 17 4 12"/></svg>
            <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="vertical-align:-1px;margin-right:2px"><polygon points="12 2 15 9 22 9 16 14 18 21 12 17 6 21 8 14 2 9 9 9"/></svg>
            {{ severityLabel(taskSeverityLevel(t)!) }}
          </span>
          <span class="dr-arrow"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></span>
        </div>
      </div>
    </div>

    <!-- Result Detail Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal=false">
      <div class="modal-card">
        <div class="modal-head">
          <span>检测详情 — {{ modalTask?.taskCode || modalTask?.id }}</span>
          <button class="modal-close" @click="showModal=false">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="resultLoading" class="modal-loading"><div class="loader"></div></div>
          <div v-else-if="resultData" class="res-detail">
            <div class="res-summary">{{ resultData.summary }}</div>
            <!-- Main Result: Damage Type + Severity -->
            <div v-if="resultData.items?.[0]" class="res-main">
              <div class="res-main-left">
                <div class="res-main-type">{{ damageTypeLabel(resultData.items[0].damageType) }}</div>
                <div class="res-main-desc">{{ damageTypeDesc(resultData.items[0].damageType) }}</div>
              </div>
              <div :class="['res-main-sev', 'sev-'+resultData.items[0].severityLevel.toLowerCase()]">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="12 2 15 9 22 9 16 14 18 21 12 17 6 21 8 14 2 9 9 9"/></svg>
                <span class="res-sev-label">{{ severityLabel(resultData.items[0].severityLevel) }}</span>
                <span class="res-sev-desc">{{ severityDesc(resultData.items[0].severityLevel) }}</span>
              </div>
            </div>
            <!-- File Preview -->
            <div v-if="resultImageUrl || modalTask?.fileUrl" class="res-file-preview">
              <img v-if="resultImageUrl" :src="resultImageUrl" class="res-preview-img" alt="Detection Result" @error="imgLoadError($event)" @click="openImagePreview(resultImageUrl)" />
              <img v-else-if="modalTask?.dataSourceType==='MANUAL_IMAGE'" :src="modalTask.fileUrl" class="res-preview-img" @error="imgLoadError($event)" @click="openImagePreview(modalTask.fileUrl)" />
              <video v-else-if="modalTask?.dataSourceType==='MANUAL_VIDEO'" :src="modalTask.fileUrl" class="res-preview-video" muted controls></video>
              <div class="res-file-name">{{ modalTask.fileName || (resultData.items?.length || 0) + ' 项检测' }}</div>
              <div v-if="resultImageUrl || (modalTask?.dataSourceType==='MANUAL_IMAGE' && modalTask?.fileUrl)" class="res-zoom-hint">🔍 点击图片查看大图</div>
            </div>
            <div class="res-items">
              <div v-for="(item, i) in resultData.items" :key="i" class="res-item">
                <div class="ri-sev-icon" :class="'sev-'+item.severityLevel.toLowerCase()">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15 9 22 9 16 14 18 21 12 17 6 21 8 14 2 9 9 9"/></svg>
                </div>
                <div class="ri-info">
                  <div class="ri-type-row">
                    <span class="ri-type">{{ damageTypeLabel(item.damageType) }}</span>
                    <span :class="['ri-badge', 'sev-'+item.severityLevel.toLowerCase()]">{{ severityLabel(item.severityLevel) }}</span>
                  </div>
                  <div class="ri-detail-row">
                    <span class="ri-detail-label">病害判定</span>
                    <span class="ri-detail-val">{{ damageTypeDesc(item.damageType) }}</span>
                  </div>
                  <div class="ri-detail-row">
                    <span class="ri-detail-label">严重程度</span>
                    <span class="ri-detail-val">{{ severityDesc(item.severityLevel) }}</span>
                  </div>
                  <div class="ri-detail-row">
                    <span class="ri-detail-label">置信度</span>
                    <span class="ri-detail-val"><span class="conf-bar-wrap"><span class="conf-bar" :style="{width: (item.confidence*100)+'%'}"></span></span> {{ (item.confidence*100).toFixed(0) }}%</span>
                  </div>
                  <div v-if="item.suggestion" class="ri-suggest">{{ item.suggestion }}</div>
                </div>
              </div>
            </div>
            <div class="res-foot">检测完成于 {{ formatTime(resultData.completedAt) }}</div>
            <div class="res-actions">
              <button v-if="canDispatch(resultData)" class="btn-dispatch" @click.stop="handleDispatch(modalTask?.id, resultData)">派单维修</button>
              <button v-if="authStore.isAdmin" class="btn-delete" @click.stop="handleDelete(modalTask?.id)">删除该检测结果</button>
            </div>
          </div>
          <div v-else class="modal-loading">暂无结果数据</div>
        </div>
      </div>
    </div>

    <!-- Full Screen Image Preview -->
    <div v-if="imagePreviewUrl" class="img-preview-overlay" @click="closeImagePreview">
      <div class="img-preview-toolbar" @click.stop>
        <span class="img-preview-info">检测图片预览</span>
        <div class="img-preview-actions">
          <button class="img-preview-btn" @click="zoomDelta(-0.2)" title="缩小"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8" y1="11" x2="14" y2="11"/></svg></button>
          <span class="img-preview-zoom">{{ Math.round(imgZoom * 100) }}%</span>
          <button class="img-preview-btn" @click="zoomDelta(0.2)" title="放大"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg></button>
          <button class="img-preview-btn" @click="resetZoom" title="重置"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg></button>
          <button class="img-preview-btn img-preview-close-btn" @click="closeImagePreview" title="关闭"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
        </div>
      </div>
      <div class="img-preview-container" @click.stop @wheel.prevent="onWheel">
        <img :src="imagePreviewUrl" class="img-preview-large" :style="{ transform: `scale(${imgZoom}) translate(${imgX}px, ${imgY}px)` }" @error="imgLoadError($event)" draggable="false" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue"
import { detectionApi, workOrderApi } from "@/api"
import { useAuthStore } from "@/stores/auth"
import { ElMessage, ElMessageBox } from "element-plus"
import type { DetectionTaskResponse, DetectionResultResponse } from "@/types"

const defaultRoadImg = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNlMmU4ZjAiLz48cmVjdCB4PSIyMCIgeT0iNDAiIHdpZHRoPSIzNjAiIGhlaWdodD0iMjIwIiByeD0iOCIgZmlsbD0iI2Y4ZmFmYyIgc3Ryb2tlPSIjY2JkNWUxIiBzdHJva2Utd2lkdGg9IjEiLz48cGF0aCBkPSJNNjAgMTAwIFEyMDAgOTAgMzQwIDEwNSIgc3Ryb2tlPSIjOTRhM2I4IiBzdHJva2Utd2lkdGg9IjIuNSIgZmlsbD0ibm9uZSIvPjxwYXRoIGQ9Ik04MCAxNDAgUTIyMCAxMzAgMzMwIDE0NSIgc3Ryb2tlPSIjNjQ3NDhiIiBzdHJva2Utd2lkdGg9IjIiIGZpbGw9Im5vbmUiLz48cGF0aCBkPSJNNTAgMTgwIFExODAgMTcwIDM1MCAxODUiIHN0cm9rZT0iIzk0YTNiOCIgc3Ryb2tlLXdpZHRoPSIxLjUiIGZpbGw9Im5vbmUiIHN0cm9rZS1kYXNoYXJyYXk9IjYgNCIvPjxjaXJjbGUgY3g9IjE4MCIgY3k9IjkwIiByPSI4IiBmaWxsPSJub25lIiBzdHJva2U9IiNkYzI2MjYiIHN0cm9rZS13aWR0aD0iMiIvPjxjaXJjbGUgY3g9IjE4MCIgY3k9IjkwIiByPSIzIiBmaWxsPSIjZGMyNjI2Ii8+PGNpcmNsZSBjeD0iMjgwIiBjeT0iMTM1IiByPSI2IiBmaWxsPSJub25lIiBzdHJva2U9IiNmNTllMGIiIHN0cm9rZS13aWR0aD0iMiIvPjxjaXJjbGUgY3g9IjI4MCIgY3k9IjEzNSIgcj0iMiIgZmlsbD0iI2Y1OWUwYiIvPjx0ZXh0IHg9IjIwMCIgeT0iMjMwIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTRhM2I4IiBmb250LXNpemU9IjE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6YGT6Lev6KOC57yd5qOA5rWLPC90ZXh0Pjwvc3ZnPg==';
function imgLoadError(e: Event) {
  (e.target as HTMLImageElement).src = defaultRoadImg;
}
const authStore = useAuthStore()
const tasks = ref<DetectionTaskResponse[]>([])
const loading = ref(false)
const activeCat = ref("MANUAL_IMAGE")
const activeSev = ref("")

// 批量选择状态
const selectedIds = ref<Set<number>>(new Set())
const showSeverityMenu = ref(false)
const isBatchMode = ref(false)

const severityOptions = [
  { key: "HIGH", label: "严重" },
  { key: "MEDIUM", label: "中等" },
  { key: "LOW", label: "轻微" },
  { key: "NORMAL", label: "无病害" },
]

const isAllSelected = computed(() => {
  return filteredTasks.value.length > 0 && filteredTasks.value.every(t => selectedIds.value.has(t.id))
})

function toggleBatchMode() {
  if (isBatchMode.value) {
    // 退出批量模式，清除选择
    clearSelection()
  }
  isBatchMode.value = !isBatchMode.value
}

function toggleSelect(taskId: number) {
  const next = new Set(selectedIds.value)
  if (next.has(taskId)) {
    next.delete(taskId)
  } else {
    next.add(taskId)
  }
  selectedIds.value = next
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(filteredTasks.value.map(t => t.id))
  }
}

function clearSelection() {
  selectedIds.value = new Set()
  showSeverityMenu.value = false
}

function onCardClick(t: DetectionTaskResponse) {
  if (isBatchMode.value) {
    // 批量选择模式下，点击卡片切换选中状态
    toggleSelect(t.id)
  } else {
    viewResult(t)
  }
}

// 切换分类/过滤时：退出批量模式并清除选择
watch([activeCat, activeSev], () => {
  isBatchMode.value = false
  clearSelection()
})

const categories = computed(() => {
  const img = tasks.value.filter(t => t.dataSourceType === "MANUAL_IMAGE").length
  const vid = tasks.value.filter(t => t.dataSourceType === "MANUAL_VIDEO").length
  return [
    { key: "MANUAL_IMAGE", label: "图片检测", count: img },
    { key: "MANUAL_VIDEO", label: "视频检测", count: vid },
  ]
})

const severityTags = computed(() => {
  const list = tasks.value.filter(t => t.dataSourceType === activeCat.value)
  return [
    { key: "", label: "全部", count: list.length, color: "#4338ca" },
    { key: "HIGH", label: "严重", count: list.filter(t => taskSeverityLevel(t) === "HIGH").length, color: "#dc2626" },
    { key: "MEDIUM", label: "中等", count: list.filter(t => taskSeverityLevel(t) === "MEDIUM").length, color: "#d97706" },
    { key: "LOW", label: "轻微", count: list.filter(t => taskSeverityLevel(t) === "LOW").length, color: "#16a34a" },
    { key: "NORMAL", label: "无病害", count: list.filter(t => taskSeverityLevel(t) === "NORMAL").length, color: "#0891b2" },
  ]
})

const filteredTasks = computed(() => {
  let list = tasks.value.filter(t => t.dataSourceType === activeCat.value)
  if (activeSev.value) {
    list = list.filter(t => taskSeverityLevel(t) === activeSev.value)
  }
  return list
})

function taskSeverityLevel(t: DetectionTaskResponse) {
  // 优先使用后端列表接口返回的 highestSeverity 字段
  if (t.highestSeverity) return t.highestSeverity
  // 兜底：从 result.items 中计算（详情页场景）
  const items = t.result?.items
  if (!items || items.length === 0) {
    // 已完成但无病害 → 归类为"无病害"
    if (t.status === "COMPLETED") return "NORMAL"
    return null
  }
  const score: Record<string, number> = { HIGH: 3, MEDIUM: 2, LOW: 1 }
  let top = items[0]
  for (const item of items) {
    if ((score[item.severityLevel] || 0) > (score[top.severityLevel] || 0)) {
      top = item
    }
  }
  return top.severityLevel
}

function taskDamageCount(t: DetectionTaskResponse) {
  // 优先使用后端列表接口返回的 damageCount 字段
  if (t.damageCount != null) return t.damageCount
  // 兜底：从 result.items 中计算
  return t.result?.items?.length ?? 0
}

function thumbSrc(t: DetectionTaskResponse) {
  const annotated = t.result?.imageBase64
  if (annotated) {
    // 后端可能返回 base64 字符串，也可能返回 /uploads/result/xxx.png 形式的 URL
    if (annotated.startsWith('/') || annotated.startsWith('http')) return annotated
    return 'data:image/jpeg;base64,' + annotated
  }
  return t.fileUrl || defaultRoadImg
}

/**
 * 模态框中的检测结果图。兼容 base64 字符串和 /uploads/... 形式的 URL。
 */
const resultImageUrl = computed(() => {
  const v = resultData.value?.imageBase64
  if (!v) return ''
  if (v.startsWith('/') || v.startsWith('http')) return v
  return 'data:image/jpeg;base64,' + v
})

onMounted(() => loadTasks())

async function loadTasks() {
  loading.value = true
  try {
    const r = await detectionApi.list({ page: 1, size: 100 })
    const all = r.data.data.records || []
    tasks.value = all.filter(t => t.status === "COMPLETED")
  } catch {
    ElMessage.error("获取检测记录失败")
  }
  loading.value = false
}

function formatTime(s: string) {
  if (!s) return "--"
  return s.slice(0, 16).replace("T", " ")
}

// Modal
const showModal = ref(false)
const resultLoading = ref(false)
const modalTask = ref<DetectionTaskResponse | null>(null)
const resultData = ref<DetectionResultResponse | null>(null)

async function viewResult(t: DetectionTaskResponse) {
  showModal.value = true
  resultLoading.value = true
  modalTask.value = t
  try {
    const r = await detectionApi.getResult(t.id)
    resultData.value = r.data.data
  } catch {
    resultData.value = null
  }
  resultLoading.value = false
}

function severityLabel(s: string) {
  return { HIGH: "严重", MEDIUM: "中等", LOW: "轻微", NORMAL: "无病害" }[s] || s
}

function damageTypeDesc(t: string) {
  const m: Record<string,string> = {
    CRACK:"裂缝 - 路面出现裂缝，需进一步判定裂缝类型",
    TRANSVERSE_CRACK:"横向裂缝 - 路面出现垂直于行车方向的裂缝，通常由温度变化或路基不均匀沉降引起",
    LONGITUDINAL_CRACK:"纵向裂缝 - 路面出现平行于行车方向的裂缝，通常由施工接缝或荷载疲劳引起",
    NET_CRACK:"网状裂缝 - 路面呈龟壳状开裂，通常由路基强度不足或沥青老化引起",
    POTHOLE:"坑槽 - 路面局部凹陷或坑洞，影响行车安全",
    MARKING_DAMAGE:"标线损坏 - 交通标线磨损或脱落",
    ROAD_SPILL:"路面抛洒 - 路面有杂物或油污等抛洒物",
    UNKNOWN:"未知病害",
    OTHER:"其他病害"
  };
  return m[t] || t;
}
function severityDesc(s: string) {
  const m: Record<string,string> = {
    HIGH:"严重病害 - 需立即维修处理，存在安全隐患",
    MEDIUM:"中等病害 - 需尽快安排养护，防止病害扩展",
    LOW:"轻微病害 - 继续观察，纳入日常养护计划"
  };
  return m[s] || s;
}
function damageTypeLabel(t: string) {
  return { CRACK: "裂缝", TRANSVERSE_CRACK: "横向裂缝", LONGITUDINAL_CRACK: "纵向裂缝", NET_CRACK: "网状裂缝", POTHOLE: "坑槽", MARKING_DAMAGE: "标线损坏", ROAD_SPILL: "路面抛洒", UNKNOWN: "未知", OTHER: "其他" }[t] || t
}

function canDispatch(data: DetectionResultResponse) {
  if (!authStore.isAdmin) return false
  return data.items?.some(i => i.severityLevel === "HIGH")
}

async function handleDispatch(taskId: number, data: DetectionResultResponse) {
  ElMessageBox.confirm("此检测结果包含严重病害，确认派发维修工单？", "派单确认", { confirmButtonText: "确认派单", cancelButtonText: "取消", type: "warning" })
    .then(async () => {
      try {
        await workOrderApi.create({
          detectionTaskId: taskId,
          title: "病害维修工单",
          damageType: data.items[0]?.damageType as any,
          severityLevel: data.items[0]?.severityLevel as any,
          departmentCode: "ROAD_ADMIN",
          location: modalTask.value?.location || "",
          description: data.summary || "",
        })
        ElMessage.success("工单已派发")
        window.dispatchEvent(new CustomEvent("data-updated"))
      } catch {
        ElMessage.error("派单失败")
      }
    })
    .catch(() => {})
}

async function handleDelete(taskId?: number) {
  if (!taskId) return
  ElMessageBox.confirm(
    "删除该检测结果后，与之关联的工单也将被同步删除，此操作不可撤销。确认删除？",
    "删除确认",
    { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "error" }
  )
    .then(async () => {
      try {
        await detectionApi.remove(taskId)
        ElMessage.success("检测结果已删除")
        showModal.value = false
        selectedIds.value.delete(taskId)
        loadTasks()
        window.dispatchEvent(new CustomEvent("data-updated"))
      } catch {
        ElMessage.error("删除失败，可能没有权限或任务状态不允许")
      }
    })
    .catch(() => {})
}

// ====== 批量操作 ======
async function handleBatchSeverity(newSeverity: string) {
  showSeverityMenu.value = false
  const ids = Array.from(selectedIds.value)
  if (ids.length === 0) {
    ElMessage.warning("请先选择需要调整的检测结果")
    return
  }
  const label = severityLabel(newSeverity)
  ElMessageBox.confirm(
    `确认将已选的 ${ids.length} 条检测结果的严重等级调整为「${label}」？`,
    "批量调整确认",
    { confirmButtonText: "确认调整", cancelButtonText: "取消", type: "warning" }
  )
    .then(async () => {
      try {
        await detectionApi.batchUpdateSeverity({ taskIds: ids, newSeverity })
        ElMessage.success(`已成功将 ${ids.length} 条检测结果的严重等级调整为「${label}」`)
        clearSelection()
        loadTasks()
        window.dispatchEvent(new CustomEvent("data-updated"))
      } catch {
        ElMessage.error("批量调整失败，请稍后重试")
      }
    })
    .catch(() => {})
}

async function handleBatchDelete() {
  const ids = Array.from(selectedIds.value)
  if (ids.length === 0) {
    ElMessage.warning("请先选择需要删除的检测结果")
    return
  }
  ElMessageBox.confirm(
    `确认删除已选的 ${ids.length} 条检测结果？关联工单也将被同步删除，此操作不可撤销。`,
    "批量删除确认",
    { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "error" }
  )
    .then(async () => {
      try {
        await detectionApi.batchRemove({ taskIds: ids })
        ElMessage.success(`已成功删除 ${ids.length} 条检测结果`)
        clearSelection()
        loadTasks()
        window.dispatchEvent(new CustomEvent("data-updated"))
      } catch {
        ElMessage.error("批量删除失败，请稍后重试")
      }
    })
    .catch(() => {})
}

// ====== 全屏图片预览 ======
const imagePreviewUrl = ref("")
const imgZoom = ref(1)
const imgX = ref(0)
const imgY = ref(0)

function openImagePreview(url: string) {
  if (!url) return
  imagePreviewUrl.value = url
  imgZoom.value = 1
  imgX.value = 0
  imgY.value = 0
}

function closeImagePreview() {
  imagePreviewUrl.value = ""
}

function zoomDelta(delta: number) {
  imgZoom.value = Math.max(0.2, Math.min(5, imgZoom.value + delta))
}

function resetZoom() {
  imgZoom.value = 1
  imgX.value = 0
  imgY.value = 0
}

function onWheel(e: WheelEvent) {
  zoomDelta(e.deltaY > 0 ? -0.1 : 0.1)
}
</script>

<style scoped>
.dr { font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,sans-serif; }
.dr-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:24px; }
.dr-top-left { display:flex; align-items:baseline; gap:12px; }
.dr-title { font-size:20px; font-weight:600; color:#111827; margin:0; }
.dr-subtitle { font-size:13px; color:#9ca3af; }
.dr-refresh { display:flex; align-items:center; gap:5px; padding:7px 14px; border:1px solid #e5e7eb; border-radius:8px; background:#fff; color:#374151; font-size:12px; font-family:inherit; cursor:pointer; transition:all .15s; }
.dr-refresh:hover { border-color:#4338ca; color:#4338ca; }

/* ── Category Tabs ── */
.dr-cats { display:flex; gap:10px; margin-bottom:20px; }
.cat-btn { display:flex; align-items:center; gap:8px; padding:10px 20px; border:1px solid #e5e7eb; border-radius:10px; background:#fff; color:#4a5568; font-size:14px; font-weight:500; font-family:inherit; cursor:pointer; transition:all .2s; }
.cat-btn:hover { border-color:#4338ca; color:#4338ca; }
.cat-btn.active { border-color:#4338ca; background:#eef2ff; color:#4338ca; font-weight:600; }
.cat-count { display:inline-flex; align-items:center; justify-content:center; min-width:20px; height:20px; padding:0 6px; border-radius:10px; background:#f3f4f6; font-size:11px; font-weight:600; color:#6b7280; }
.cat-btn.active .cat-count { background:#4338ca; color:#fff; }

/* ── Severity Tags ── */
.sev-tags { display:flex; gap:10px; margin-bottom:20px; }
.sev-tag { display:flex; align-items:center; gap:8px; padding:8px 18px; border:2px solid #e5e7eb; border-radius:24px; background:#fff; color:#6b7280; font-size:13px; font-weight:500; font-family:inherit; cursor:pointer; transition:all .2s; }
.sev-tag:hover { border-color:#c7d2fe; background:#f8faff; }
.sev-tag.active { border-color:#4338ca; background:#eef2ff; color:#4338ca; font-weight:700; box-shadow:0 2px 8px rgba(67,56,202,0.12); }
.sev-tag .sev-dot { width:10px; height:10px; border-radius:50%; }
.dot-high { background:#dc2626; box-shadow:0 0 0 2px rgba(220,38,38,0.15); }
.dot-medium { background:#d97706; box-shadow:0 0 0 2px rgba(217,119,6,0.15); }
.dot-low { background:#16a34a; box-shadow:0 0 0 2px rgba(22,163,74,0.15); }
.dot-normal { background:#0891b2; box-shadow:0 0 0 2px rgba(8,145,178,0.15); }
.sev-tag-count { display:inline-flex; align-items:center; justify-content:center; min-width:22px; height:20px; padding:0 6px; border-radius:10px; background:#f3f4f6; font-size:11px; font-weight:600; color:#6b7280; }
.sev-tag.active .sev-tag-count { background:#4338ca; color:#fff; }

/* 非选中状态下，各等级标签也显示对应色调，便于一眼区分 */
.sev-tag:has(.dot-high) { color:#dc2626; border-color:#fecaca; }
.sev-tag:has(.dot-high) .sev-tag-count { background:#fef2f2; color:#dc2626; }
.sev-tag:has(.dot-medium) { color:#d97706; border-color:#fde68a; }
.sev-tag:has(.dot-medium) .sev-tag-count { background:#fffbeb; color:#d97706; }
.sev-tag:has(.dot-low) { color:#16a34a; border-color:#bbf7d0; }
.sev-tag:has(.dot-low) .sev-tag-count { background:#f0fdf4; color:#16a34a; }
.sev-tag:has(.dot-normal) { color:#0891b2; border-color:#a5f3fc; }
.sev-tag:has(.dot-normal) .sev-tag-count { background:#ecfeff; color:#0891b2; }

/* 选中状态下，各等级标签用对应深色 */
.sev-tag:has(.dot-high).active { border-color:#dc2626; background:#fef2f2; color:#dc2626; }
.sev-tag:has(.dot-high).active .sev-tag-count { background:#dc2626; color:#fff; }
.sev-tag:has(.dot-medium).active { border-color:#d97706; background:#fffbeb; color:#d97706; }
.sev-tag:has(.dot-medium).active .sev-tag-count { background:#d97706; color:#fff; }
.sev-tag:has(.dot-low).active { border-color:#16a34a; background:#f0fdf4; color:#16a34a; }
.sev-tag:has(.dot-low).active .sev-tag-count { background:#16a34a; color:#fff; }
.sev-tag:has(.dot-normal).active { border-color:#0891b2; background:#ecfeff; color:#0891b2; }
.sev-tag:has(.dot-normal).active .sev-tag-count { background:#0891b2; color:#fff; }

.dr-loading { display:flex; justify-content:center; padding:60px 0; }
.loader { width:24px; height:24px; border:2px solid #f3f4f6; border-top-color:#4338ca; border-radius:50%; animation:spin .5s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
.dr-empty { text-align:center; padding:60px 16px; }
.dr-empty-text { font-size:14px; color:#9ca3af; margin-top:12px; }
.dr-empty-hint { font-size:12px; color:#d1d5db; margin-top:4px; }
.dr-list { display:grid; grid-template-columns:repeat(auto-fill,minmax(340px,1fr)); gap:14px; }
.dr-card { background:#fff; border:1px solid #f3f4f6; border-radius:10px; padding:0; cursor:pointer; transition:all .15s; display:flex; overflow:hidden; position:relative; }
.dr-card:hover { border-color:#4338ca; box-shadow:0 2px 8px rgba(67,56,202,0.06); }

/* ── 严重程度卡片左侧色条 ── */
.dr-card.card-sev-high { border-left:4px solid #dc2626; }
.dr-card.card-sev-medium { border-left:4px solid #d97706; }
.dr-card.card-sev-low { border-left:4px solid #16a34a; }
.dr-card.card-sev-normal { border-left:4px solid #0891b2; }

/* ── 缩略图左上角严重程度角标 ── */
.thumb-sev-badge { position:absolute; top:0; left:0; display:flex; align-items:center; gap:3px; padding:3px 8px; font-size:11px; font-weight:700; border-bottom-right-radius:8px; }
.thumb-sev-badge.sev-high { background:#dc2626; color:#fff; }
.thumb-sev-badge.sev-medium { background:#d97706; color:#fff; }
.thumb-sev-badge.sev-low { background:#16a34a; color:#fff; }
.thumb-sev-badge.sev-normal { background:#0891b2; color:#fff; }

.dr-thumb { width:140px; min-height:120px; flex-shrink:0; background:#f3f4f6; display:flex; align-items:center; justify-content:center; overflow:hidden; border-right:1px solid #f3f4f6; position:relative; }
.dr-thumb-img { width:100%; height:100%; object-fit:cover; display:block; }
.dr-card-content { flex:1; padding:14px 18px; display:flex; flex-direction:column; justify-content:space-between; }
.dr-card-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.dr-code { font-family:monospace; font-size:11px; font-weight:600; color:#4338ca; }
.dr-type-tag { padding:1px 8px; border-radius:4px; background:#f3f4f6; color:#6b7280; font-size:10px; font-weight:500; }
.dr-card-body { display:flex; flex-direction:column; gap:6px; margin-bottom:12px; }
.dr-info { display:flex; align-items:center; gap:6px; font-size:12px; color:#6b7280; }
.dr-info strong { color:#111827; }
.dr-stat { display:flex; align-items:center; gap:6px; font-size:12px; color:#6b7280; }
.dr-stat strong { color:#111827; }
.dr-card-foot { display:flex; align-items:center; justify-content:space-between; padding-top:10px; border-top:1px solid #f9fafb; }
.dr-sev { display:inline-flex; align-items:center; gap:3px; font-size:13px; font-weight:700; padding:4px 12px; border-radius:6px; }
.sev-high { background:#fef2f2; color:#dc2626; }
.sev-medium { background:#fffbeb; color:#d97706; }
.sev-low { background:#f0fdf4; color:#16a34a; }
.sev-normal { background:#ecfeff; color:#0891b2; }
.dr-arrow { color:#d1d5db; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:600px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-body { padding:20px; }
.modal-loading { text-align:center; padding:40px 0; color:#94a3b8; font-size:13px; display:flex; justify-content:center; }
.res-summary { font-size:14px; font-weight:600; color:#0f172a; margin-bottom:16px; padding:10px 14px; background:#f0fdf4; border-radius:8px; }
.res-main { display:flex; gap:16px; margin-bottom:16px; padding:16px; background:#f8f9fc; border:1px solid #eef0f4; border-radius:10px; align-items:flex-start; }
.res-main-left { flex:1; }
.res-main-type { font-size:18px; font-weight:700; color:#0f172a; margin-bottom:4px; }
.res-main-desc { font-size:12px; color:#64748b; line-height:1.5; }
.res-main-sev { display:flex; align-items:center; gap:6px; padding:10px 14px; border-radius:8px; flex-shrink:0; white-space:nowrap; }
.res-sev-label { font-size:15px; font-weight:700; }
.res-sev-desc { font-size:11px; }
.res-main-sev.sev-high { background:#fef2f2; color:#dc2626; }
.res-main-sev.sev-medium { background:#fffbeb; color:#d97706; }
.res-main-sev.sev-low { background:#f0fdf4; color:#16a34a; }
.res-file-preview { margin-bottom:16px; border:1px solid #f0f2f5; border-radius:8px; overflow:hidden; background:#fafbfc; }
.res-preview-img { max-width:100%; max-height:200px; width:auto; height:auto; object-fit:contain; display:block; margin:0 auto; }
.res-preview-video { max-width:100%; max-height:200px; width:auto; height:auto; display:block; margin:0 auto; }
.res-file-name { padding:6px 12px; font-size:11px; color:#64748b; background:#fff; border-top:1px solid #f0f2f5; }

.res-items { display:flex; flex-direction:column; gap:10px; }
.res-item { display:flex; gap:14px; padding:14px 16px; border:1px solid #f0f2f5; border-radius:10px; background:#fafbfc; }
.ri-sev-icon { flex-shrink:0; width:36px; height:36px; display:flex; align-items:center; justify-content:center; border-radius:8px; }
.ri-sev-icon.sev-high { background:#fef2f2; color:#dc2626; }
.ri-sev-icon.sev-medium { background:#fffbeb; color:#d97706; }
.ri-sev-icon.sev-low { background:#f0fdf4; color:#16a34a; }
.ri-type-row { display:flex; align-items:center; justify-content:space-between; margin-bottom:8px; }
.ri-type { font-size:14px; font-weight:600; color:#0f172a; }

.sev-high { background:#fef2f2; color:#dc2626; }
.sev-medium { background:#fffbeb; color:#d97706; }
.sev-low { background:#f0fdf4; color:#16a34a; }
.ri-detail-row { display:flex; align-items:center; gap:8px; margin-bottom:4px; }
.ri-detail-label { font-size:11px; color:#94a3b8; font-weight:500; min-width:56px; flex-shrink:0; }
.ri-detail-val { font-size:12px; color:#334155; line-height:1.5; }
.conf-bar-wrap { display:inline-block; width:60px; height:6px; background:#f1f5f9; border-radius:3px; vertical-align:middle; overflow:hidden; }
.conf-bar { display:block; height:100%; background:#4338ca; border-radius:3px; transition:width .3s; }
.ri-suggest { margin-top:6px; font-size:11px; color:#2563eb; background:#eff6ff; padding:4px 10px; border-radius:4px; display:inline-block; }


.sev-high { background:#fef2f2; color:#dc2626; }
.sev-medium { background:#fffbeb; color:#d97706; }
.sev-low { background:#f0fdf4; color:#16a34a; }
.ri-info { flex:1; }

.ri-detail { font-size:11px; color:#64748b; }
.ri-suggest { font-size:11px; color:#2563eb; margin-top:4px; background:#eff6ff; padding:4px 8px; border-radius:4px; }
.res-foot { margin-top:16px; padding-top:12px; border-top:1px solid #f0f2f5; font-size:11px; color:#94a3b8; text-align:center; }
.res-actions { margin-top:12px; text-align:center; }
.btn-dispatch { padding:8px 20px; background:#4338ca; border:none; border-radius:6px; color:#fff; font-size:12px; font-weight:600; font-family:inherit; cursor:pointer; transition:background .15s; }
.btn-dispatch:hover { background:#3730a3; }
.btn-delete { padding:8px 20px; background:#fff; border:1px solid #fecaca; border-radius:6px; color:#dc2626; font-size:12px; font-weight:600; font-family:inherit; cursor:pointer; transition:all .15s; margin-left:8px; }
.btn-delete:hover { background:#dc2626; border-color:#dc2626; color:#fff; }

/* ── 图片预览提示 ── */
.res-preview-img { cursor:zoom-in; transition:opacity .15s; }
.res-preview-img:hover { opacity:0.85; }
.res-zoom-hint { padding:4px 12px 6px; font-size:11px; color:#6366f1; background:#eef2ff; text-align:center; }

/* ── 全屏图片预览 ── */
.img-preview-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.88); z-index:2000; display:flex; flex-direction:column; }
.img-preview-toolbar { display:flex; align-items:center; justify-content:space-between; padding:12px 20px; background:rgba(0,0,0,0.6); backdrop-filter:blur(8px); flex-shrink:0; }
.img-preview-info { color:#fff; font-size:14px; font-weight:500; }
.img-preview-actions { display:flex; align-items:center; gap:8px; }
.img-preview-btn { display:flex; align-items:center; justify-content:center; width:36px; height:36px; border:none; border-radius:8px; background:rgba(255,255,255,0.1); color:#fff; cursor:pointer; transition:background .15s; }
.img-preview-btn:hover { background:rgba(255,255,255,0.25); }
.img-preview-close-btn:hover { background:#dc2626; }
.img-preview-zoom { color:#fff; font-size:13px; font-weight:500; min-width:50px; text-align:center; }
.img-preview-container { flex:1; display:flex; align-items:center; justify-content:center; overflow:hidden; cursor:grab; }
.img-preview-container:active { cursor:grabbing; }
.img-preview-large { max-width:90vw; max-height:90vh; object-fit:contain; transition:transform .1s ease-out; user-select:none; -webkit-user-drag:none; }

/* ── 批量操作工具栏 ── */
.batch-toolbar { display:flex; align-items:center; gap:14px; padding:10px 16px; background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:16px; flex-wrap:wrap; }

/* 批量操作开关按钮 */
.batch-toggle { display:flex; align-items:center; gap:6px; padding:7px 16px; border:1px solid #e5e7eb; border-radius:8px; background:#fff; color:#374151; font-size:13px; font-weight:600; font-family:inherit; cursor:pointer; transition:all .15s; }
.batch-toggle:hover { border-color:#4338ca; color:#4338ca; }
.batch-toggle-on { background:#4338ca; border-color:#4338ca; color:#fff; }
.batch-toggle-on:hover { background:#3730a3; border-color:#3730a3; color:#fff; }

.batch-check-all { display:flex; align-items:center; gap:6px; font-size:13px; color:#374151; cursor:pointer; user-select:none; }
.batch-check-all input[type="checkbox"] { width:16px; height:16px; cursor:pointer; accent-color:#4338ca; }
.batch-count { font-size:13px; color:#6b7280; }
.batch-count strong { color:#4338ca; }
.batch-actions { display:flex; align-items:center; gap:8px; margin-left:auto; }
.batch-severity-dropdown { position:relative; }
.batch-btn { display:flex; align-items:center; gap:5px; padding:6px 14px; border:none; border-radius:6px; font-size:12px; font-weight:600; font-family:inherit; cursor:pointer; transition:all .15s; }
.batch-btn-severity { background:#eef2ff; color:#4338ca; }
.batch-btn-severity:hover { background:#4338ca; color:#fff; }
.batch-btn-delete { background:#fef2f2; color:#dc2626; }
.batch-btn-delete:hover { background:#dc2626; color:#fff; }
.batch-btn-cancel { padding:6px 14px; border:1px solid #e5e7eb; border-radius:6px; background:#fff; color:#6b7280; font-size:12px; font-family:inherit; cursor:pointer; transition:all .15s; }
.batch-btn-cancel:hover { border-color:#d1d5db; color:#374151; }

/* ── 严重等级下拉菜单 ── */
.severity-dropdown-mask { position:fixed; inset:0; z-index:99; }
.severity-dropdown-menu { position:absolute; top:100%; left:0; margin-top:4px; background:#fff; border:1px solid #e2e8f0; border-radius:8px; box-shadow:0 4px 16px rgba(0,0,0,0.1); z-index:100; min-width:160px; overflow:hidden; }
.severity-option { display:flex; align-items:center; gap:8px; width:100%; padding:10px 14px; border:none; background:#fff; color:#374151; font-size:13px; font-family:inherit; cursor:pointer; transition:background .12s; text-align:left; }
.severity-option:hover { background:#f8fafc; }
.sev-opt-high:hover { background:#fef2f2; color:#dc2626; }
.sev-opt-medium:hover { background:#fffbeb; color:#d97706; }
.sev-opt-low:hover { background:#f0fdf4; color:#16a34a; }
.sev-opt-normal:hover { background:#ecfeff; color:#0891b2; }
.sev-dot-sm { width:10px; height:10px; border-radius:50%; flex-shrink:0; }

/* ── 卡片复选框 ── */
.dr-card-check { position:absolute; top:8px; right:8px; z-index:5; display:flex; align-items:center; justify-content:center; width:22px; height:22px; background:rgba(255,255,255,0.9); border-radius:4px; }
.dr-card-check input[type="checkbox"] { width:15px; height:15px; cursor:pointer; accent-color:#4338ca; }
.card-selected { border-color:#4338ca !important; box-shadow:0 0 0 2px rgba(67,56,202,0.15); }
</style>
