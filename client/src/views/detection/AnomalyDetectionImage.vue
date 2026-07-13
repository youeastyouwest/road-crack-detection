<template>
  <div class="dr">
    <div class="dr-top">
      <div class="dr-top-left">
        <h2 class="dr-title">{{ t('dr.title') }}</h2>
        <span class="dr-subtitle">{{ t('dr.subtitle') }}</span>
      </div>
      <button class="dr-refresh" @click="loadTasks"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg> {{ t('dr.refresh') }}</button>
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
      </button>
    </div>

    <!-- Results List -->
    <div v-if="loading" class="dr-loading"><div class="loader"></div></div>
    <div v-else-if="filteredTasks.length === 0" class="dr-empty">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#e5e7eb" stroke-width="1.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
      <div class="dr-empty-text">{{ t('dr.noResult') }}</div>
      <div class="dr-empty-hint">{{ t('dr.noResultHint') }}</div>
    </div>
    <div v-else class="dr-list">
      <div v-for="task in filteredTasks" :key="task.id" class="dr-card" @click="viewResult(task)">
        <div class="dr-thumb">
          <img v-if="task.dataSourceType==='MANUAL_IMAGE'" :src="thumbSrc(task)" class="dr-thumb-img" @error="imgLoadError($event)" />
          <video v-else-if="task.dataSourceType==='MANUAL_VIDEO' && isVideoUrl(thumbSrc(task))" :src="thumbSrc(task)" class="dr-thumb-img" muted preload="metadata"></video>
          <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2"/></svg>
        </div>
        <div class="dr-card-content">
        <div class="dr-card-top">
          <span class="dr-code">{{ task.taskCode }}</span>
          <span class="dr-type-tag">{{ ({MANUAL_IMAGE: t('dr.imageDetection'), MANUAL_VIDEO: t('dr.videoDetection'), DRONE_VIDEO: t('dr.drone'), CROWD_SOURCE: t('dr.crowdsource') })[task.dataSourceType] || task.dataSourceType }}</span>
        </div>
        <div class="dr-card-body">
          <div class="dr-info"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg> {{ task.location || t('dr.location') }}</div>
          <div class="dr-info"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg> {{ formatTime(task.createdAt) }}</div>
          <div v-if="taskDamageCount(task) > 0" class="dr-stat"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg> {{ t('dr.recognizedDamages') }} <strong>{{ taskDamageCount(task) }}</strong> {{ t('dr.damageUnit') }}</div>
        </div>
        </div>
        <div class="dr-card-foot">
          <span v-if="taskSeverityLevel(task)" :class="['dr-sev', 'sev-'+taskSeverityLevel(task)?.toLowerCase()]">{{ severityLabel(taskSeverityLevel(task)!) }}</span>
          <span class="dr-arrow"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></span>
        </div>
      </div>
    </div>

    <!-- Result Detail Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal=false">
      <div class="modal-card">
        <div class="modal-head">
          <span>{{ t('dr.detailTitle') }}{{ modalTask?.taskCode || modalTask?.id }}</span>
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
              <video v-if="resultImageUrl && resultIsVideo" :src="resultImageUrl" class="res-preview-video" muted controls></video>
              <img v-else-if="resultImageUrl" :src="resultImageUrl" class="res-preview-img" alt="Detection Result" @error="imgLoadError($event)" />
              <img v-else-if="modalTask?.dataSourceType==='MANUAL_IMAGE'" :src="modalTask.fileUrl" class="res-preview-img" @error="imgLoadError($event)" />
              <video v-else-if="modalTask?.dataSourceType==='MANUAL_VIDEO'" :src="modalTask.fileUrl" class="res-preview-video" muted controls></video>
              <div class="res-file-name">{{ modalTask.fileName || t('dr.detectionItems', { count: resultData.items?.length || 0 }) }}</div>
            </div>
            <!-- Keyframe previews for video detection -->
            <div v-if="keyframeUrls.length > 0" class="res-keyframes">
              <div class="kf-title">关键帧标注</div>
              <div class="kf-list">
                <div v-for="(url, idx) in keyframeUrls" :key="idx" class="kf-item">
                  <img :src="url" class="kf-img" alt="keyframe" @error="imgLoadError($event)" />
                </div>
              </div>
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
                    <span class="ri-detail-label">{{ t('dr.diseaseJudgement') }}</span>
                    <span class="ri-detail-val">{{ damageTypeDesc(item.damageType) }}</span>
                  </div>
                  <div class="ri-detail-row">
                    <span class="ri-detail-label">{{ t('dr.severityLevel') }}</span>
                    <span class="ri-detail-val">{{ severityDesc(item.severityLevel) }}</span>
                  </div>
                  <div class="ri-detail-row">
                    <span class="ri-detail-label">{{ t('dr.confidence') }}</span>
                    <span class="ri-detail-val"><span class="conf-bar-wrap"><span class="conf-bar" :style="{width: (item.confidence*100)+'%'}"></span></span> {{ (item.confidence*100).toFixed(0) }}%</span>
                  </div>
                  <div v-if="item.suggestion" class="ri-suggest">{{ item.suggestion }}</div>
                </div>
              </div>
            </div>
            <div class="res-foot">{{ t('dr.completedAt') }}{{ formatTime(resultData.completedAt) }}</div>
            <div class="res-actions">
              <button v-if="canDispatch(resultData)" class="btn-dispatch" @click.stop="handleDispatch(modalTask?.id, resultData)">{{ t('dr.dispatch') }}</button>
            </div>
          </div>
          <div v-else class="modal-loading">{{ t('dr.noData') }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue"
import { detectionApi, workOrderApi } from "@/api"
import { useAuthStore } from "@/stores/auth"
import { ElMessage, ElMessageBox } from "element-plus"
import type { DetectionTaskResponse, DetectionResultResponse } from "@/types"
import { t } from "@/i18n"

const defaultRoadImg = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNlMmU4ZjAiLz48cmVjdCB4PSIyMCIgeT0iNDAiIHdpZHRoPSIzNjAiIGhlaWdodD0iMjIwIiByeD0iOCIgZmlsbD0iI2Y4ZmFmYyIgc3Ryb2tlPSIjY2JkNWUxIiBzdHJva2Utd2lkdGg9IjEiLz48cGF0aCBkPSJNNjAgMTAwIFEyMDAgOTAgMzQwIDEwNSIgc3Ryb2tlPSIjOTRhM2I4IiBzdHJva2Utd2lkdGg9IjIuNSIgZmlsbD0ibm9uZSIvPjxwYXRoIGQ9Ik04MCAxNDAgUTIyMCAxMzAgMzMwIDE0NSIgc3Ryb2tlPSIjNjQ3NDhiIiBzdHJva2Utd2lkdGg9IjIiIGZpbGw9Im5vbmUiLz48cGF0aCBkPSJNNTAgMTgwIFExODAgMTcwIDM1MCAxODUiIHN0cm9rZT0iIzk0YTNiOCIgc3Ryb2tlLXdpZHRoPSIxLjUiIGZpbGw9Im5vbmUiIHN0cm9rZS1kYXNoYXJyYXk9IjYgNCIvPjxjaXJjbGUgY3g9IjE4MCIgY3k9IjkwIiByPSI4IiBmaWxsPSJub25lIiBzdHJva2U9IiNkYzI2MjYiIHN0cm9rZS13aWR0aD0iMiIvPjxjaXJjbGUgY3g9IjE4MCIgY3k9IjkwIiByPSIzIiBmaWxsPSIjZGMyNjI2Ii8+PGNpcmNsZSBjeD0iMjgwIiBjeT0iMTM1IiByPSI2IiBmaWxsPSJub25lIiBzdHJva2U9IiNmNTllMGIiIHN0cm9rZS13aWR0aD0iMiIvPjxjaXJjbGUgY3g9IjI4MCIgY3k9IjEzNSIgcj0iMiIgZmlsbD0iI2Y1OWUwYiIvPjx0ZXh0IHg9IjIwMCIgeT0iMjMwIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTRhM2I4IiBmb250LXNpemU9IjE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6YGT6Lev6KOC57yd5qOA5rWLPC90ZXh0Pjwvc3ZnPg==';
function imgLoadError(e: Event) {
  (e.target as HTMLImageElement).src = defaultRoadImg;
}
const authStore = useAuthStore()
const tasks = ref<DetectionTaskResponse[]>([])
const loading = ref(false)
const activeCat = ref("MANUAL_IMAGE")
const activeSev = ref("")

const categories = computed(() => {
  const img = tasks.value.filter(t => t.dataSourceType === "MANUAL_IMAGE").length
  const vid = tasks.value.filter(t => t.dataSourceType === "MANUAL_VIDEO").length
  return [
    { key: "MANUAL_IMAGE", label: t('dr.imageDetection'), count: img },
    { key: "MANUAL_VIDEO", label: t('dr.videoDetection'), count: vid },
  ]
})

const severityTags = [
  { key: "", label: t('dr.all') },
  { key: "HIGH", label: t('dr.severity') },
  { key: "MEDIUM", label: t('dr.medium') },
  { key: "LOW", label: t('dr.low') },
]

const filteredTasks = computed(() => {
  let list = tasks.value.filter(t => t.dataSourceType === activeCat.value)
  if (activeSev.value) {
    list = list.filter(t => taskSeverityLevel(t) === activeSev.value)
  }
  return list
})

function taskSeverityLevel(t: DetectionTaskResponse) {
  const items = t.result?.items
  if (!items || items.length === 0) return null
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
  return t.result?.items?.length ?? 0
}

function isVideoUrl(url: string) {
  return /\.(mp4|webm|mov|avi|mkv)(\?.*)?$/i.test(url)
}

function thumbSrc(t: DetectionTaskResponse) {
  const annotated = t.result?.imageBase64
  if (annotated) {
    // Python服务返回的完整URL转换为相对路径，让Vite代理转发
    if (annotated.startsWith('http://localhost:8000/results/')) {
      return annotated.replace('http://localhost:8000', '')
    }
    if (annotated.startsWith('/') || annotated.startsWith('http')) return annotated
    return 'data:image/jpeg;base64,' + annotated
  }
  return t.fileUrl || defaultRoadImg
}

/**
 * 模态框中的检测结果图/视频。兼容 base64 字符串和 URL 路径。
 */
const resultImageUrl = computed(() => {
  const v = resultData.value?.imageBase64
  if (!v) return ''
  if (v.startsWith('http://localhost:8000/results/')) {
    return v.replace('http://localhost:8000', '')
  }
  if (v.startsWith('/') || v.startsWith('http')) return v
  return 'data:image/jpeg;base64,' + v
})

const resultIsVideo = computed(() => isVideoUrl(resultImageUrl.value))

const keyframeUrls = computed(() => {
  return (resultData.value?.keyframeUrls?.filter(u => !!u) ?? []).map(u => {
    if (u.startsWith('http://localhost:8000/results/')) {
      return u.replace('http://localhost:8000', '')
    }
    return u
  })
})

onMounted(() => loadTasks())

async function loadTasks() {
  loading.value = true
  try {
    const r = await detectionApi.list({ page: 1, size: 100 })
    const all = r.data.data.records || []
    tasks.value = all.filter(t => t.status === "COMPLETED")
  } catch {
    ElMessage.error(t('dr.fetchFailed'))
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
  return ({ HIGH: t('dr.severity'), MEDIUM: t('dr.medium'), LOW: t('dr.low') })[s] || s
}

function damageTypeDesc(tp: string) {
  const m: Record<string,string> = {
    CRACK: t('dr.workOrderDesc'),
    TRANSVERSE_CRACK: t('dr.transverseCrackDesc'),
    LONGITUDINAL_CRACK: t('dr.longitudinalCrackDesc'),
    NET_CRACK: t('dr.netCrackDesc'),
    POTHOLE: t('dr.potholeDesc'),
    MARKING_DAMAGE: t('dr.markingDesc'),
    ROAD_SPILL: t('dr.spillDesc'),
    UNKNOWN: t('dr.unknownDesc'),
    OTHER: t('dr.otherDesc')
  };
  return m[tp] || tp;
}
function severityDesc(s: string) {
  const m: Record<string,string> = {
    HIGH: t('dr.severityHighDesc'),
    MEDIUM: t('dr.severityMediumDesc'),
    LOW: t('dr.severityLowDesc')
  };
  return m[s] || s;
}
function damageTypeLabel(tp: string) {
  return ({ CRACK: t('damage.crack'), TRANSVERSE_CRACK: t('damage.transverseCrack'), LONGITUDINAL_CRACK: t('damage.longitudinalCrack'), NET_CRACK: t('damage.netCrack'), POTHOLE: t('damage.pothole'), MARKING_DAMAGE: t('damage.markingDamage'), ROAD_SPILL: t('damage.roadSpill'), UNKNOWN: t('damage.unknown'), OTHER: t('damage.crack') })[tp] || tp
}

function canDispatch(data: DetectionResultResponse) {
  if (!authStore.isAdmin) return false
  return data.items?.some(i => i.severityLevel === "HIGH")
}

async function handleDispatch(taskId: number, data: DetectionResultResponse) {
  ElMessageBox.confirm(t('dr.dispatchConfirm'), t('dr.dispatchTitle'), { confirmButtonText: t('dr.dispatchBtn'), cancelButtonText: t('common.cancel'), type: "warning" })
    .then(async () => {
      try {
        await workOrderApi.create({
          detectionTaskId: taskId,
          title: t('dr.workOrderTitle'),
          damageType: data.items[0]?.damageType as any,
          severityLevel: data.items[0]?.severityLevel as any,
          departmentCode: "ROAD_ADMIN",
          location: modalTask.value?.location || "",
          description: data.summary || "",
        })
        ElMessage.success(t('dr.dispatchSuccess'))
        window.dispatchEvent(new CustomEvent("data-updated"))
      } catch {
        ElMessage.error(t('dr.dispatchFailed'))
      }
    })
    .catch(() => {})
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
.sev-tags { display:flex; gap:8px; margin-bottom:20px; }
.sev-tag { display:flex; align-items:center; gap:6px; padding:6px 14px; border:1px solid #e5e7eb; border-radius:20px; background:#fff; color:#6b7280; font-size:12px; font-weight:500; font-family:inherit; cursor:pointer; transition:all .15s; }
.sev-tag:hover { border-color:#9ca3af; color:#374151; }
.sev-tag.active { border-color:#4338ca; background:#eef2ff; color:#4338ca; font-weight:600; }
.sev-dot { width:8px; height:8px; border-radius:50%; }
.dot-high { background:#dc2626; }
.dot-medium { background:#d97706; }
.dot-low { background:#16a34a; }

.dr-loading { display:flex; justify-content:center; padding:60px 0; }
.loader { width:24px; height:24px; border:2px solid #f3f4f6; border-top-color:#4338ca; border-radius:50%; animation:spin .5s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
.dr-empty { text-align:center; padding:60px 16px; }
.dr-empty-text { font-size:14px; color:#9ca3af; margin-top:12px; }
.dr-empty-hint { font-size:12px; color:#d1d5db; margin-top:4px; }
.dr-list { display:grid; grid-template-columns:repeat(auto-fill,minmax(340px,1fr)); gap:14px; }
.dr-card { background:#fff; border:1px solid #f3f4f6; border-radius:10px; padding:0; cursor:pointer; transition:all .15s; display:flex; overflow:hidden; }
.dr-card:hover { border-color:#4338ca; box-shadow:0 2px 8px rgba(67,56,202,0.06); }
.dr-thumb { width:140px; min-height:120px; flex-shrink:0; background:#f3f4f6; display:flex; align-items:center; justify-content:center; overflow:hidden; border-right:1px solid #f3f4f6; }
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
.dr-sev { font-size:11px; font-weight:600; padding:2px 8px; border-radius:4px; }
.sev-high { background:#fef2f2; color:#dc2626; }
.sev-medium { background:#fffbeb; color:#d97706; }
.sev-low { background:#f0fdf4; color:#16a34a; }
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

.res-keyframes { margin-bottom:16px; }
.kf-title { font-size:13px; font-weight:600; color:#334155; margin-bottom:8px; }
.kf-list { display:flex; gap:8px; overflow-x:auto; padding-bottom:4px; }
.kf-item { flex:0 0 auto; width:140px; height:100px; border-radius:8px; overflow:hidden; border:1px solid #e2e8f0; background:#f8fafc; }
.kf-img { width:100%; height:100%; object-fit:cover; display:block; }

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
</style>
