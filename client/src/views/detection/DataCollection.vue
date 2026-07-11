<template>
  <div class="dc">
    <div class="dc-top">
      <div class="dc-top-left">
        <h2 class="dc-title">上传检测</h2>
        <span class="dc-subtitle">支持图片和视频AI识别</span>
      </div>
    </div>

    <div class="dc-grid">
      <!-- Left: Upload Form -->
      <div class="card form-card">
        <div class="card-head">
          <span class="card-title">上传检测</span>
        </div>
        <div class="card-body card-body-form">
          <div class="form-group">
            <label class="form-label">数据源类型</label>
            <div class="select-wrap">
              <select v-model="form.dataSourceType" class="form-select">
                <option value="MANUAL_IMAGE">图片检测</option>
                <option value="MANUAL_VIDEO">视频检测</option>
              </select>
              <svg class="select-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">文件</label>
            <div class="upload-zone" @drop.prevent="handleDrop" @dragover.prevent @click="$refs.fileInput.click()">
              <input ref="fileInput" type="file" hidden @change="e => handleFileInput(e.target as HTMLInputElement)" accept="image/*,video/*" />
              <template v-if="form.fileUrl">
                <img v-if="form.dataSourceType==='MANUAL_IMAGE'" :src="form.fileUrl" class="preview-img" @error="handleImgError" />
                <video v-else :src="form.fileUrl" class="preview-video" muted controls></video>
              </template>
              <template v-else>
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                <div class="uz-text">点击或拖拽上传文件</div>
                <div class="uz-hint">支持 jpg/png/mp4 格式，最大 500MB</div>
              </template>
            </div>
            <div v-if="form.fileName" class="file-name-tag">{{ form.fileName }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">
              位置
              <span v-if="form.location" class="loc-badge">已定位</span>
            </label>
            <div class="loc-row">
              <input v-model="form.location" class="form-input" placeholder="点击地图选择位置" readonly @click="focusMap" />
              <button v-if="form.location" class="loc-clear" @click="form.location='';selectedPos=null">清除</button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">文件</label>
            <textarea v-model="form.remark" class="form-textarea" rows="3" placeholder="输入备注信息..."></textarea>
          </div>

          <button class="form-btn" @click="submitTask" :disabled="submitting">
            <span v-if="submitting" class="btn-loader"></span>
            <span v-else>提交检测</span>
          </button>
        </div>
      </div>

      <!-- Right: AMap -->
      <div class="card map-card">
        <div class="map-search-bar">
          <input v-model="searchKeyword" class="map-search-input" placeholder="点击地图选择位置" @keyup.enter="doSearch" />
          <button class="map-search-btn" @click="doSearch">搜索</button>
        </div>
        <div id="collectionMap" class="map-container"></div>
        <div v-if="selectedPos" class="map-pos-info">
          <span>经度 {{ selectedPos.lng.toFixed(6) }}, {{ selectedPos.lat.toFixed(6) }}</span>
          <span v-if="selectedAddr" class="map-addr">{{ selectedAddr }}</span>
        </div>
      </div>
    </div>

    <!-- Recent Tasks -->
    <div class="card tasks-card" style="margin-top:20px">
      <div class="card-head">
        <span class="card-title">上传检测</span>
        <span v-if="tasks.length" class="card-count">{{ tasks.length }} 条</span>
      </div>
      <div class="card-body card-body-nopad">
        <div class="act-header">
          <span class="act-cell act-id">编号</span>
          <span class="act-cell act-loc">位置</span>
          <span class="act-cell act-st">状态</span>
          <span class="act-cell act-time">时间</span><span class="act-cell act-op">操作</span>
        </div>
        <div v-if="loading" class="act-loading"><div class="loader"></div></div>
        <div v-for="t in tasks" :key="t.id" class="act-row">
          <span class="act-cell act-id act-code">{{ t.taskCode }}</span>
          <span class="act-cell act-loc">{{ t.location }}</span>
          <span class="act-cell act-st"><span :class="['st-badge', stCls(t.status)]">{{ stLbl(t.status) }}</span></span>
          <span class="act-cell act-time">{{ t.createdAt }}</span><span class="act-cell act-op"><button v-if="t.status==='COMPLETED'" class="view-btn" @click="viewResult(t.id)">查看结果</button><span v-else class="op-na">--</span></span>
        </div>
        <div v-if="!loading && tasks.length === 0" class="act-empty">暂无检测记录</div>
      </div>
    </div>

    <!-- Result Modal -->
    <div v-if="showResultModal" class="modal-overlay" @click.self="showResultModal=false">
      <div class="modal-card">
        <div class="modal-head">
          <span>检测结果: {{ resultTask?.taskCode || resultTask?.id }} 
            <span v-if="resultTask?.location" style="font-size:12px;color:#94a3b8;margin-left:8px">{{ resultTask.location }}</span>
          </span>
          <button class="modal-close" @click="showResultModal=false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="resultLoading" class="modal-loading">加载中...</div>
          <div v-else-if="resultData" class="result-content">
            <div class="result-summary">{{ resultData.summary }}</div>
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
            <div v-if="resultData?.imageBase64" class="res-file-preview">
              <img :src="'data:image/jpeg;base64,' + resultData.imageBase64" class="res-preview-img" alt="Detection Result" />
              <div class="res-file-name">{{ resultData.items?.length || 0 }} 项检测</div>
            </div>
            <div class="result-items">
              <div v-for="(item, i) in resultData.items" :key="i" class="result-item">
                <div class="ri-sev-icon" :class="'sev-'+item.severityLevel.toLowerCase()">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15 9 22 9 16 14 18 21 12 17 6 21 8 14 2 9 9 9"/></svg>
                </div>
                <div class="ri-info">
                  <div class="ri-type-row">
                    <span class="ri-type">{{ damageTypeLabel(item.damageType) }}</span>
                    <span :class="['ri-badge', 'sev-'+item.severityLevel.toLowerCase()]">{{ severityLabel(item.severityLevel) }}</span>
                  </div>
                  <div class="ri-detail-row">
                    <span class="ri-detail-label">病害类型</span>                <span class="ri-detail-val">{{ damageTypeDesc(item.damageType) }}</span>
                  </div>
                  <div class="ri-detail-row">
                    <span class="ri-detail-label">严重程度</span>                <span class="ri-detail-val">{{ severityDesc(item.severityLevel) }}</span>
                  </div>
                  <div class="ri-detail-row">
                    <span class="ri-detail-label">置信度</span>                <span class="ri-detail-val"><span class="conf-bar-wrap"><span class="conf-bar" :style="{width: (item.confidence*100)+'%'}"></span></span> {{ (item.confidence*100).toFixed(0) }}%</span>
                  </div>
                  <div v-if="item.suggestion" class="ri-suggest">{{ item.suggestion }}</div>
                </div>
              </div>
            </div>
            <div v-if="canDispatch(resultData)" class="res-actions">
              <button class="btn-dispatch" @click="handleDispatch(resultTask?.id, resultData)">生成工单</button>
            </div>
          </div>
          <div v-else class="modal-loading">加载检测结果...</div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showResultModal=false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { detectionApi, workOrderApi } from "@/api"
import type { DetectionTaskResponse } from "@/types"

declare global { interface Window { AMap: any; _AMapSecurityConfig: any } }

const form = reactive({ dataSourceType: "MANUAL_IMAGE", fileName: "", fileUrl: "", location: "", remark: "" })
const tasks = ref<DetectionTaskResponse[]>([])
const loading = ref(false)
const submitting = ref(false)
const polling = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null
const fileInput = ref<HTMLInputElement>()
const uploadedFile = ref<File | null>(null)
const uploadedFileUrl = ref("")
const showResultModal = ref(false)
const resultLoading = ref(false)
const resultTask = ref<any>(null)
const resultData = ref<any>(null)

// AMap state
const searchKeyword = ref("")
const selectedPos = ref<{lng:number;lat:number} | null>(null)
const selectedAddr = ref("")
let map: any = null
let marker: any = null
let placeSearch: any = null
let geocoder: any = null

function handleImgError(e: Event) { (e.target as HTMLElement).style.display = "none" }

function handleFileInput(input: HTMLInputElement) {
  const f = input.files?.[0]
  if (f) { form.fileName = f.name; const url = URL.createObjectURL(f); form.fileUrl = url; uploadedFile.value = f; uploadedFileUrl.value = url }
}

function handleDrop(e: DragEvent) {
  const f = e.dataTransfer?.files?.[0]
  if (f) { form.fileName = f.name; const url = URL.createObjectURL(f); form.fileUrl = url; uploadedFile.value = f; uploadedFileUrl.value = url }
}

function initMap() {
  if (typeof window.AMap === "undefined") { setTimeout(initMap, 500); return }
  try {
    map = new window.AMap.Map("collectionMap", {
      zoom: 13, center: [116.397428, 39.90923],
      features: ["bg","road","building"],
    })
    map.addControl(new window.AMap.ToolBar())
    map.addControl(new window.AMap.Scale())
    map.on("click", (e: any) => {
      const lng = e.lnglat.getLng(), lat = e.lnglat.getLat()
      selectedPos.value = { lng, lat }
      form.location = lng.toFixed(6) + "," + lat.toFixed(6)
      placeMarker(lng, lat)
      reverseGeocode(lng, lat)
    })
    placeSearch = new window.AMap.PlaceSearch({ city: "北京", citylimit: true })
    geocoder = new window.AMap.Geocoder({ city: "北京", radius: 1000 })
  } catch (e) { console.error("AMap init error:", e) }
}

function placeMarker(lng: number, lat: number) {
  if (marker) map.remove(marker)
  marker = new window.AMap.Marker({ position: [lng, lat], map })
  map.setCenter([lng, lat])
}

function reverseGeocode(lng: number, lat: number) {
  if (!geocoder) return
  geocoder.getAddress([lng, lat], (status: string, result: any) => {
    if (status === "complete" && result.regeocode) {
      selectedAddr.value = result.regeocode.formattedAddress || ""
    }
  })
}

function doSearch() {
  if (!placeSearch || !searchKeyword.value.trim()) return
  placeSearch.search(searchKeyword.value.trim(), (status: string, result: any) => {
    if (status === "complete" && result.poiList?.pois?.length) {
      const poi = result.poiList.pois[0]
      const lng = poi.location.lng, lat = poi.location.lat
      selectedPos.value = { lng, lat }
      form.location = lng.toFixed(6) + "," + lat.toFixed(6)
      selectedAddr.value = poi.name + " ? " + (poi.address || "")
      placeMarker(lng, lat)
      map.setZoom(15)
    } else {
      ElMessage.warning("请先在地图上选择位置")
    }
  })
}

function focusMap() {
  if (map) { map.setZoom(15); setTimeout(() => { (document.getElementById("collectionMap") as HTMLElement)?.scrollIntoView({ behavior: "smooth" }) }, 100) }
}

async function submitTask() {
  if (!form.location) { ElMessage.warning("请先选择位置信息"); return }
  if (!uploadedFile.value) { ElMessage.warning("请选择文件"); return }
  submitting.value = true
  try {
    const fd = new FormData()
    fd.append("file", uploadedFile.value)
    fd.append("dataSourceType", form.dataSourceType)
    fd.append("location", form.location)
    fd.append("remark", form.remark)
    const res = await detectionApi.createWithFile(fd)
    const taskId = res.data.data?.id
    ElMessage.success("任务提交成功，AI 检测中...")
    if (taskId) { startPolling(taskId) }
    form.fileName = ""; form.fileUrl = ""; form.remark = ""; uploadedFile.value = null
  } catch (e: any) { ElMessage.error("提交失败: " + (e?.response?.data?.message || "未知错误")) }
  finally { submitting.value = false }
}

function startPolling(taskId: number) {
  polling.value = true; let attempts = 0
  pollTimer = setInterval(async () => {
    attempts++
    try {
      await loadTasks()
      const updated = tasks.value.find(t => t.id === taskId)
      if (updated?.status === "COMPLETED") { stopPolling(); ElMessage.success("AI 检测完成"); viewResult(taskId) }
      else if (attempts >= 8) { stopPolling(); ElMessage.info("检测超时，请稍后刷新查看") }
    } catch { if (attempts >= 8) stopPolling() }
  }, 2000)
}

function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null }; polling.value = false }

async function loadTasks() {
  loading.value = true
  try { const r = await detectionApi.list({ page: 1, size: 5 }); tasks.value = r.data.data.records } catch {}
  loading.value = false
}

function stCls(s: string) { return ({ PENDING: "st-wait", PROCESSING: "st-run", COMPLETED: "st-ok", FAILED: "st-fail" })[s] || "" }
function stLbl(s: string) { return ({ PENDING: "待处理", PROCESSING: "检测中", COMPLETED: "已完成", FAILED: "失败" })[s] || s }

async function viewResult(taskId: number) {
  showResultModal.value = true; resultLoading.value = true
  resultTask.value = tasks.value.find(t => t.id === taskId) || { id: taskId }
  try { const r = await detectionApi.getResult(taskId); resultData.value = r.data.data } catch { resultData.value = null }
  resultLoading.value = false
}

function damageTypeLabel(t: string) { return ({ CRACK: "裂缝", POTHOLE: "坑洞", MARKING_DAMAGE: "标线损坏", ROAD_SPILL: "路面抛洒" })[t] || t || "未知" }
function damageTypeDesc(t: string) { return ({ CRACK: "道路表面出现裂缝", POTHOLE: "路面出现坑洞", MARKING_DAMAGE: "标线磨损/缺失", ROAD_SPILL: "路面污染/杂物" })[t] || "" }
function severityLabel(s: string) { return ({ HIGH: "严重", MEDIUM: "中等", LOW: "轻微" })[s] || s }
function severityDesc(s: string) { return ({ HIGH: "需要立即处理", MEDIUM: "建议尽快修复", LOW: "可安排常规养护" })[s] || "" }

function canDispatch(data: any) { return data?.items?.some((i: any) => i.severityLevel === "HIGH") }

async function handleDispatch(taskId: number, data: any) {
  ElMessageBox.confirm("确定要为该检测结果创建维修工单吗？", "生成工单", { confirmButtonText: "确认生成", cancelButtonText: "取消", type: "warning" })
    .then(async () => {
      try {
        await workOrderApi.create({ detectionTaskId: taskId })
        ElMessage.success("工单已生成")
        window.dispatchEvent(new CustomEvent("data-updated"))
      } catch { ElMessage.error("生成失败") }
    }).catch(() => {})
}

onMounted(() => {
  loadTasks()
  setTimeout(initMap, 1000)
})

onUnmounted(() => {
  stopPolling()
  if (marker && map) map.remove(marker)
})
</script>
<style scoped>
.dc { font-family: Inter, "PingFang SC", "Noto Sans SC", system-ui, sans-serif; }
.dc-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.dc-top-left { display: flex; align-items: baseline; gap: 12px; }
.dc-title { font-size: 20px; font-weight: 700; color: #0f172a; margin: 0; }
.dc-subtitle { font-size: 13px; color: #94a3b8; }
.dc-grid { display: grid; grid-template-columns: 380px 1fr; gap: 20px; }
.card { background: #fff; border-radius: 12px; border: 1px solid #eef0f4; overflow: hidden; }
.card-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-bottom: 1px solid #f0f2f5; font-size: 14px; font-weight: 600; color: #0f172a; }
.card-title { font-size: 14px; font-weight: 600; color: #0f172a; }
.card-count { font-size: 11px; color: #94a3b8; font-weight: 400; }
.card-body { padding: 18px; }
.card-body-form { display: flex; flex-direction: column; gap: 14px; }
.card-body-nopad { padding: 0; }
.form-group { display: flex; flex-direction: column; gap: 5px; }
.form-label { font-size: 12px; font-weight: 500; color: #475569; display: flex; align-items: center; gap: 6px; }
.loc-badge { font-size: 10px; color: #059669; background: #ecfdf5; padding: 1px 6px; border-radius: 4px; font-weight: 400; }
.loc-row { display: flex; gap: 6px; }
.loc-clear { padding: 0 10px; font-size: 11px; color: #ef4444; background: #fef2f2; border: none; border-radius: 6px; cursor: pointer; white-space: nowrap; }
.form-input, .form-select, .form-textarea { width: 100%; padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; font-family: inherit; color: #0f172a; background: #fff; outline: none; transition: border-color 0.15s; box-sizing: border-box; }
.form-input:focus, .form-select:focus, .form-textarea:focus { border-color: #2563eb; }
.form-textarea { resize: vertical; min-height: 60px; }
.select-wrap { position: relative; }
.select-arrow { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); pointer-events: none; }
.form-select { appearance: none; padding-right: 30px; cursor: pointer; }
.file-name-tag { font-size: 11px; color: #2563eb; background: #eff6ff; padding: 3px 8px; border-radius: 4px; }
.form-btn { padding: 10px; background: #2563eb; border: none; border-radius: 8px; color: #fff; font-size: 13px; font-weight: 600; font-family: inherit; cursor: pointer; transition: background 0.15s; display: flex; align-items: center; justify-content: center; gap: 6px; }
.form-btn:hover { background: #1d4ed8; }
.form-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-loader { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg) } }

/* Upload Zone */
.upload-zone { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px 16px; border: 2px dashed #e2e8f0; border-radius: 10px; cursor: pointer; transition: border-color 0.15s, background 0.15s; text-align: center; }
.upload-zone:hover { border-color: #2563eb; background: #f8faff; }
.uz-text { font-size: 12px; color: #64748b; margin-top: 6px; }
.uz-hint { font-size: 10px; color: #cbd5e1; margin-top: 3px; }
.preview-img { max-width: 100%; max-height: 120px; object-fit: contain; border-radius: 4px; }
.preview-video { max-width: 100%; max-height: 120px; border-radius: 4px; }

/* Map Card */
.map-card { display: flex; flex-direction: column; }
.map-search-bar { display: flex; gap: 6px; padding: 12px 14px; border-bottom: 1px solid #f0f2f5; }
.map-search-input { flex: 1; padding: 7px 12px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; font-family: inherit; outline: none; }
.map-search-input:focus { border-color: #2563eb; }
.map-search-btn { padding: 7px 16px; background: #2563eb; border: none; border-radius: 8px; color: #fff; font-size: 12px; font-weight: 500; font-family: inherit; cursor: pointer; white-space: nowrap; }
.map-search-btn:hover { background: #1d4ed8; }
.map-container { flex: 1; min-height: 500px; border-radius: 0 0 12px 12px; }
.map-pos-info { padding: 8px 14px; background: #f0f9ff; border-top: 1px solid #e0f2fe; font-size: 12px; color: #0369a1; display: flex; flex-direction: column; gap: 2px; }
.map-addr { font-size: 11px; color: #64748b; }

/* Tasks Table */
.act-header, .act-row { display: flex; align-items: center; padding: 10px 18px; font-size: 12px; }
.act-header { background: #f8f9fc; color: #64748b; font-weight: 500; border-bottom: 1px solid #f0f2f5; }
.act-row { border-bottom: 1px solid #f8f9fc; }
.act-row:hover { background: #fafbfc; }
.act-id { width: 100px; flex-shrink: 0; }
.act-loc { flex: 1; }
.act-st { width: 80px; text-align: center; }
.act-time { width: 160px; }
.act-op { width: 80px; text-align: right; }
.act-code { font-family: monospace; font-size: 11px; color: #475569; }
.st-badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
.st-wait { background: #fef3c7; color: #d97706; }
.st-run { background: #dbeafe; color: #2563eb; }
.st-ok { background: #d1fae5; color: #059669; }
.st-fail { background: #fee2e2; color: #dc2626; }
.view-btn { padding: 4px 10px; background: transparent; border: 1px solid #dbeafe; border-radius: 6px; color: #2563eb; font-size: 11px; font-family: inherit; cursor: pointer; }
.view-btn:hover { background: #2563eb; color: #fff; }
.op-na { color: #d1d5db; font-size: 11px; }
.act-loading { text-align: center; padding: 30px; }
.act-empty { text-align: center; padding: 30px; color: #94a3b8; font-size: 13px; }

/* Loader */
.loader { width: 20px; height: 20px; border: 2px solid #e2e8f0; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.6s linear infinite; margin: 0 auto; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 12px; width: 600px; max-height: 80vh; overflow-y: auto; box-shadow: 0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid #f0f2f5; font-size: 15px; font-weight: 600; color: #0f172a; }
.modal-close { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: #f1f5f9; border: none; border-radius: 6px; color: #94a3b8; cursor: pointer; font-size: 12px; }
.modal-body { padding: 20px; }
.modal-loading { text-align: center; padding: 40px 0; color: #94a3b8; font-size: 13px; }
.modal-foot { display: flex; justify-content: flex-end; gap: 8px; padding: 14px 20px; border-top: 1px solid #f0f2f5; }
.btn-ghost { padding: 7px 16px; border: 1px solid #e2e8f0; border-radius: 6px; background: #fff; font-size: 12px; color: #475569; cursor: pointer; font-family: inherit; }
.btn-ghost:hover { border-color: #2563eb; color: #2563eb; }

/* Result Content */
.result-summary { font-size: 14px; font-weight: 600; color: #0f172a; margin-bottom: 16px; padding: 10px 14px; background: #f0fdf4; border-radius: 8px; }
.res-main { display: flex; gap: 16px; margin-bottom: 16px; padding: 16px; background: #f8f9fc; border: 1px solid #eef0f4; border-radius: 10px; align-items: flex-start; }
.res-main-left { flex: 1; }
.res-main-type { font-size: 18px; font-weight: 700; color: #0f172a; }
.res-main-desc { font-size: 12px; color: #64748b; margin-top: 4px; }
.res-main-sev { display: flex; flex-direction: column; align-items: center; padding: 8px 16px; border-radius: 8px; min-width: 80px; }
.sev-high { background: #fef2f2; color: #dc2626; }
.sev-medium { background: #fffbeb; color: #d97706; }
.sev-low { background: #eff6ff; color: #2563eb; }
.res-sev-label { font-size: 16px; font-weight: 700; margin-top: 4px; }
.res-sev-desc { font-size: 10px; opacity: 0.7; margin-top: 2px; }
.res-file-preview { margin: 12px 0; border: 1px solid #eef0f4; border-radius: 8px; overflow: hidden; }
.res-preview-img { width: 100%; display: block; }
.res-file-name { padding: 6px 12px; font-size: 11px; color: #64748b; background: #f8f9fc; border-top: 1px solid #eef0f4; }
.result-items { display: flex; flex-direction: column; gap: 8px; }
.result-item { display: flex; gap: 10px; padding: 10px; background: #f8f9fc; border-radius: 8px; }
.ri-sev-icon { width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 50%; flex-shrink: 0; }
.ri-info { flex: 1; min-width: 0; }
.ri-type-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.ri-type { font-size: 13px; font-weight: 600; color: #0f172a; }
.ri-badge { font-size: 10px; padding: 1px 6px; border-radius: 4px; font-weight: 500; }
.ri-detail-row { display: flex; font-size: 11px; color: #64748b; margin-bottom: 3px; }
.ri-detail-label { width: 60px; flex-shrink: 0; color: #94a3b8; }
.ri-detail-val { flex: 1; }
.conf-bar-wrap { display: inline-block; width: 50px; height: 4px; background: #e2e8f0; border-radius: 2px; vertical-align: middle; margin-right: 4px; overflow: hidden; }
.conf-bar { height: 100%; background: #2563eb; border-radius: 2px; transition: width 0.3s; }
.ri-suggest { margin-top: 4px; font-size: 11px; color: #2563eb; background: #eff6ff; padding: 4px 8px; border-radius: 4px; }

/* Dispatch */
.res-actions { margin-top: 14px; text-align: center; padding-top: 12px; border-top: 1px solid #f0f2f5; }
.btn-dispatch { padding: 8px 20px; background: #4338ca; border: none; border-radius: 6px; color: #fff; font-size: 12px; font-weight: 600; font-family: inherit; cursor: pointer; transition: background .15s; }
.btn-dispatch:hover { background: #3730a3; }
</style>