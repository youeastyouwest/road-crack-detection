<template>
  <div class="dc">
    <div class="dc-top">
      <div class="dc-top-left">
        <h2 class="dc-title">数据采集</h2>
        <span class="dc-subtitle">上传道路检测数据</span>
      </div>
    </div>

    <div class="dc-grid">
      <!-- Left: Upload Form -->
      <div class="card">
        <div class="card-head">
          <span class="card-title">上传文件</span>
        </div>
        <div class="card-body card-body-form">
          <div class="form-group">
            <label class="form-label">数据类型</label>
            <div class="select-wrap">
              <select v-model="form.dataSourceType" class="form-select">
                <option value="IMAGE">图片检测</option>
                <option value="VIDEO">视频检测</option>
              </select>
              <svg class="select-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">文件</label>
            <div class="upload-zone" @drop.prevent="handleDrop" @dragover.prevent @click="$refs.fileInput.click()">
              <input ref="fileInput" type="file" hidden @change="e => handleFileInput(e.target as HTMLInputElement)" accept="image/*,video/*" />
              <template v-if="form.fileUrl">
                <img v-if="form.dataSourceType==='IMAGE'" :src="form.fileUrl" class="preview-img" @error="handleImgError" />
                <video v-else :src="form.fileUrl" class="preview-video" muted controls></video>
              </template>
              <template v-else>
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                <div class="uz-text">拖拽或点击上传文件</div>
                <div class="uz-hint">支持 jpg/png/mp4，单文件不超过 500MB</div>
              </template>
            </div>
            <div v-if="form.fileName" class="file-name-tag">{{ form.fileName }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">位置</label>
            <input v-model="form.location" class="form-input" placeholder="如：G15 高速 K120+300" />
          </div>

          <div class="form-group">
            <label class="form-label">备注</label>
            <textarea v-model="form.remark" class="form-textarea" rows="3" placeholder="可选备注信息"></textarea>
          </div>

          <button class="form-btn" @click="submitTask" :disabled="submitting">
            <span v-if="submitting" class="btn-loader"></span>
            <span v-else>提交检测任务</span>
          </button>
        </div>
      </div>

      <!-- Right: Recent Tasks -->
      <div class="card">
        <div class="card-head">
          <span class="card-title">最近采集</span>
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
          <div v-if="!loading && tasks.length === 0" class="act-empty">暂无采集记录</div>
        </div>
      </div>
    </div>
  </div>
    <!-- Result Modal -->
    <div v-if="showResultModal" class="modal-overlay" @click.self="showResultModal=false">
      <div class="modal-card">
        <div class="modal-head">
          <span>检测结果 — {{ resultTask?.taskCode || resultTask?.id }}</span>
          <button class="modal-close" @click="showResultModal=false">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="resultLoading" class="modal-loading">加载中...</div>
          <div v-else-if="resultData" class="result-content">
            <div class="result-summary">{{ resultData.summary }}</div>
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
            <div v-if="modalTask?.fileUrl" class="res-file-preview">
              <img v-if="modalTask?.dataSourceType==='IMAGE'" :src="modalTask.fileUrl || 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNlMmU4ZjAiLz48cmVjdCB4PSIyMCIgeT0iNDAiIHdpZHRoPSIzNjAiIGhlaWdodD0iMjIwIiByeD0iOCIgZmlsbD0iI2Y4ZmFmYyIgc3Ryb2tlPSIjY2JkNWUxIiBzdHJva2Utd2lkdGg9IjEiLz48cGF0aCBkPSJNNjAgMTAwIFEyMDAgOTAgMzQwIDEwNSIgc3Ryb2tlPSIjOTRhM2I4IiBzdHJva2Utd2lkdGg9IjIuNSIgZmlsbD0ibm9uZSIvPjxwYXRoIGQ9Ik04MCAxNDAgUTIyMCAxMzAgMzMwIDE0NSIgc3Ryb2tlPSIjNjQ3NDhiIiBzdHJva2Utd2lkdGg9IjIiIGZpbGw9Im5vbmUiLz48cGF0aCBkPSJNNTAgMTgwIFExODAgMTcwIDM1MCAxODUiIHN0cm9rZT0iIzk0YTNiOCIgc3Ryb2tlLXdpZHRoPSIxLjUiIGZpbGw9Im5vbmUiIHN0cm9rZS1kYXNoYXJyYXk9IjYgNCIvPjxjaXJjbGUgY3g9IjE4MCIgY3k9IjkwIiByPSI4IiBmaWxsPSJub25lIiBzdHJva2U9IiNkYzI2MjYiIHN0cm9rZS13aWR0aD0iMiIvPjxjaXJjbGUgY3g9IjE4MCIgY3k9IjkwIiByPSIzIiBmaWxsPSIjZGMyNjI2Ii8+PGNpcmNsZSBjeD0iMjgwIiBjeT0iMTM1IiByPSI2IiBmaWxsPSJub25lIiBzdHJva2U9IiNmNTllMGIiIHN0cm9rZS13aWR0aD0iMiIvPjxjaXJjbGUgY3g9IjI4MCIgY3k9IjEzNSIgcj0iMiIgZmlsbD0iI2Y1OWUwYiIvPjx0ZXh0IHg9IjIwMCIgeT0iMjMwIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTRhM2I4IiBmb250LXNpemU9IjE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6YGT6Lev6KOC57yd5qOA5rWLPC90ZXh0Pjwvc3ZnPg=='" class="res-preview-img" @error="this.src='data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNlMmU4ZjAiLz48cmVjdCB4PSIyMCIgeT0iNDAiIHdpZHRoPSIzNjAiIGhlaWdodD0iMjIwIiByeD0iOCIgZmlsbD0iI2Y4ZmFmYyIgc3Ryb2tlPSIjY2JkNWUxIiBzdHJva2Utd2lkdGg9IjEiLz48cGF0aCBkPSJNNjAgMTAwIFEyMDAgOTAgMzQwIDEwNSIgc3Ryb2tlPSIjOTRhM2I4IiBzdHJva2Utd2lkdGg9IjIuNSIgZmlsbD0ibm9uZSIvPjxwYXRoIGQ9Ik04MCAxNDAgUTIyMCAxMzAgMzMwIDE0NSIgc3Ryb2tlPSIjNjQ3NDhiIiBzdHJva2Utd2lkdGg9IjIiIGZpbGw9Im5vbmUiLz48cGF0aCBkPSJNNTAgMTgwIFExODAgMTcwIDM1MCAxODUiIHN0cm9rZT0iIzk0YTNiOCIgc3Ryb2tlLXdpZHRoPSIxLjUiIGZpbGw9Im5vbmUiIHN0cm9rZS1kYXNoYXJyYXk9IjYgNCIvPjxjaXJjbGUgY3g9IjE4MCIgY3k9IjkwIiByPSI4IiBmaWxsPSJub25lIiBzdHJva2U9IiNkYzI2MjYiIHN0cm9rZS13aWR0aD0iMiIvPjxjaXJjbGUgY3g9IjE4MCIgY3k9IjkwIiByPSIzIiBmaWxsPSIjZGMyNjI2Ii8+PGNpcmNsZSBjeD0iMjgwIiBjeT0iMTM1IiByPSI2IiBmaWxsPSJub25lIiBzdHJva2U9IiNmNTllMGIiIHN0cm9rZS13aWR0aD0iMiIvPjxjaXJjbGUgY3g9IjI4MCIgY3k9IjEzNSIgcj0iMiIgZmlsbD0iI2Y1OWUwYiIvPjx0ZXh0IHg9IjIwMCIgeT0iMjMwIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTRhM2I4IiBmb250LXNpemU9IjE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6YGT6Lev6KOC57yd5qOA5rWLPC90ZXh0Pjwvc3ZnPg=='" />
              <video v-else :src="modalTask.fileUrl" class="res-preview-video" muted controls></video>
              <div class="res-file-name">{{ modalTask.fileName }}</div>
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
          </div>
          <div v-else class="modal-loading">暂无结果数据</div>
        </div>
        <div class="modal-foot">
                    <button class="btn-ghost" @click="showResultModal=false">关闭</button>
        </div>
      </div>
    </div></template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from "vue"
import { ElMessage } from "element-plus"
import { detectionApi, fileApi } from "@/api"
import type { DetectionTaskResponse } from "@/types"

const form = reactive({ dataSourceType: "IMAGE", fileName: "", fileUrl: "", location: "", remark: "" })
const tasks = ref<DetectionTaskResponse[]>([])
const loading = ref(false)
const submitting = ref(false)
const polling = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null
const fileInput = ref<HTMLInputElement>()

function handleFileInput(input: HTMLInputElement) {
  const f = input.files?.[0]
  if (f) { form.fileName = f.name; const url = URL.createObjectURL(f); form.fileUrl = url; uploadedFileUrl.value = url }
}
function handleDrop(e: DragEvent) {
  const f = e.dataTransfer?.files?.[0]
  if (f) { form.fileName = f.name; const url = URL.createObjectURL(f); form.fileUrl = url; uploadedFileUrl.value = url }
}
async function submitTask() {
  if (!form.location) { ElMessage.warning("请填写位置"); return }
  submitting.value = true
  try {
    const res = await detectionApi.create({ dataSourceType: form.dataSourceType as any, fileName: form.fileName || "unknown.jpg", fileUrl: form.fileUrl || "", location: form.location, remark: form.remark })
    const taskId = res.data.data?.id
    ElMessage.success("任务已提交，正在执行AI检测...")
    if (taskId) {
      await detectionApi.execute(taskId)
      ElMessage.success("AI检测已启动，正在等待检测完成...")
      startPolling(taskId)
    }
    form.location = ""; form.remark = ""; form.fileName = ""; form.fileUrl = ""
  } catch (e) { ElMessage.error("提交失败：" + (e?.response?.data?.message || "请重试")) }
  finally { submitting.value = false }
}
function startPolling(taskId: number) {
  polling.value = true
  let attempts = 0
  pollTimer = setInterval(async () => {
    attempts++
    try {
      await loadTasks()
      const updated = tasks.value.find(t => t.id === taskId)
      if (updated?.status === "COMPLETED") {
        stopPolling()
        ElMessage.success("AI检测完成！")
        viewResult(taskId)
      } else if (attempts >= 8) {
        stopPolling()
        ElMessage.info("检测仍在处理中，请稍后手动刷新查看结果")
      }
    } catch {
      if (attempts >= 8) stopPolling()
    }
  }, 2000)
}
function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  polling.value = false
}
async function loadTasks() {
  loading.value = true
  try { const r = await detectionApi.list({ page: 1, size: 5 }); tasks.value = r.data.data.records } catch {}
  loading.value = false
}
function stCls(s: string) { return ({PENDING:"st-wait",PROCESSING:"st-run",COMPLETED:"st-ok",FAILED:"st-fail"})[s]||"" }
function stLbl(s: string) { return ({PENDING:"待处理",PROCESSING:"处理中",COMPLETED:"已完成",FAILED:"失败"})[s]||s }
const uploadedFileUrl = ref(''); // Keep uploaded file URL for result modal
const showResultModal = ref(false)
const resultLoading = ref(false)
const resultTask = ref<any>(null)
const resultData = ref<any>(null)

async function viewResult(taskId: number) {
  showResultModal.value = true
  resultLoading.value = true
  resultTask.value = tasks.value.find(t => t.id === taskId) || { id: taskId }
  // Restore original uploaded file URL
  if (!resultTask.value.fileUrl && uploadedFileUrl.value) {
    resultTask.value.fileUrl = uploadedFileUrl.value;
  }
  try {
    const r = await detectionApi.getResult(taskId)
    resultData.value = r.data.data
  } catch {
    resultData.value = null
  }
  resultLoading.value = false
}

function damageTypeDesc(t: string) {
  const m: Record<string,string> = {
    TRANSVERSE_CRACK:"横向裂缝 - 路面出现垂直于行车方向的裂缝，通常由温度变化或路基不均匀沉降引起",
    LONGITUDINAL_CRACK:"纵向裂缝 - 路面出现平行于行车方向的裂缝，通常由施工接缝或荷载疲劳引起",
    NET_CRACK:"网状裂缝 - 路面呈龟壳状开裂，通常由路基强度不足或沥青老化引起",
    POTHOLE:"坑槽 - 路面局部凹陷或坑洞，影响行车安全",
    MARKING_DAMAGE:"标线损坏 - 交通标线磨损或脱落",
    ROAD_SPILL:"路面抛洒 - 路面有杂物或油污等抛洒物",
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
  const m: Record<string, string> = {TRANSVERSE_CRACK:"横向裂缝",LONGITUDINAL_CRACK:"纵向裂缝",NET_CRACK:"网状裂缝",POTHOLE:"坑槽",MARKING_DAMAGE:"标线损坏",ROAD_SPILL:"路面抛洒",OTHER:"其他病害"};
  return m[t] || t;
}
function severityLabel(s: string) {
  const m: Record<string, string> = {HIGH:"严重",MEDIUM:"中等",LOW:"轻微"};
  return m[s] || s;
}
onMounted(loadTasks)
onUnmounted(() => stopPolling())
</script>

<style scoped>
.dc { font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,sans-serif; }
.dc-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:28px; }
.dc-top-left { display:flex; align-items:baseline; gap:12px; }
.dc-title { font-size:20px; font-weight:600; color:#111827; margin:0; }
.dc-subtitle { font-size:13px; color:#9ca3af; }
.dc-grid { display:grid; grid-template-columns:1fr 1.1fr; gap:16px; }

.card { background:#fff; border:1px solid #f3f4f6; border-radius:10px; overflow:hidden; }
.card-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f9fafb; }
.card-title { font-size:13px; font-weight:600; color:#374151; }
.card-count { font-size:11px; color:#9ca3af; }
.card-body-nopad { padding:0; }
.card-body-form { padding:20px 22px; }

.form-group { margin-bottom:18px; }
.form-label { display:block; font-size:12px; font-weight:500; color:#374151; margin-bottom:6px; }
.form-input, .form-textarea, .form-select { width:100%; padding:9px 12px; border:1px solid #e5e7eb; border-radius:8px; font-size:13px; font-family:inherit; color:#111827; background:#fff; outline:none; transition:border-color .15s; box-sizing:border-box; }
.form-input:focus, .form-textarea:focus, .form-select:focus { border-color:#4338ca; }
.form-input::placeholder, .form-textarea::placeholder { color:#9ca3af; }
.form-textarea { resize:vertical; min-height:60px; }
.select-wrap { position:relative; }
.select-arrow { position:absolute; right:10px; top:50%; transform:translateY(-50%); pointer-events:none; color:#9ca3af; }
.form-select { appearance:none; padding-right:28px; cursor:pointer; }

.upload-zone { border:1.5px dashed #d1d5db; border-radius:10px; padding:28px 20px; text-align:center; cursor:pointer; transition:all .15s; background:#fafbfc; position:relative; }
.upload-zone:hover { border-color:#4361ee; background:#f8f9fc; }
.uz-text { font-size:13px; color:#6b7280; margin-top:8px; }
.uz-hint { font-size:11px; color:#9ca3af; margin-top:4px; }
/* Upload preview - scale down to fit container */
.preview-img { max-width:100%; max-height:140px; width:auto; height:auto; object-fit:contain; border-radius:4px; display:block; margin:0 auto; }
.preview-video { max-width:100%; max-height:140px; width:auto; height:auto; border-radius:4px; display:block; margin:0 auto; }
/* When file selected, shrink upload-zone padding */
.upload-zone:has(img.preview-img) { padding:10px; border-color:#d1d5db; background:#fff; }
.upload-zone:has(video.preview-video) { padding:10px; border-color:#d1d5db; background:#fff; }
.file-name-tag { margin-top:2px; padding:2px 6px; font-size:10px; color:#9ca3af; display:inline-block; }

.form-btn { width:100%; padding:10px 0; background:#4338ca; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:600; font-family:inherit; cursor:pointer; transition:background .15s; display:flex; align-items:center; justify-content:center; gap:6px; }
.form-btn:hover { background:#3730a3; }
.form-btn:disabled { background:#e5e7eb; color:#9ca3af; cursor:not-allowed; }
.btn-loader { width:16px; height:16px; border:2px solid rgba(255,255,255,.3); border-top-color:#fff; border-radius:50%; animation:spin .5s linear infinite; }

.act-header { display:flex; padding:10px 20px; border-bottom:1px solid #f9fafb; }
.act-cell { font-size:11px; color:#9ca3af; }
.act-id { width:130px; flex-shrink:0; }
.act-loc { flex:1; }
.act-st { width:72px; flex-shrink:0; }
.act-time { width:120px; flex-shrink:0; }
.act-row { display:flex; padding:10px 20px; border-bottom:1px solid #f9fafb; align-items:center; }
.act-row:last-child { border-bottom:none; }
.act-row:hover { background:#fafbfc; }
.act-code { font-family:monospace; font-size:11px; font-weight:600; color:#4338ca; }
.st-badge { display:inline-block; padding:1px 7px; border-radius:4px; font-size:10px; font-weight:600; }
.st-wait { background:#f9fafb; color:#6b7280; }
.st-run { background:#fffbeb; color:#d97706; }
.st-ok { background:#f0fdf4; color:#22c55e; }
.st-fail { background:#fef2f2; color:#ef4444; }
.act-loading { display:flex; justify-content:center; padding:32px 0; }
.loader { width:20px; height:20px; border:2px solid #f3f4f6; border-top-color:#4338ca; border-radius:50%; animation:spin .5s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
.act-empty { text-align:center; padding:36px 0; color:#9ca3af; font-size:13px; }

@media(max-width:1024px) { .dc-grid { grid-template-columns:1fr; } }
.act-op { width:80px; flex-shrink:0; text-align:center; }
.view-btn { padding:3px 10px; border:1px solid #2563eb; border-radius:4px; background:#eff6ff; color:#2563eb; font-size:11px; font-weight:500; cursor:pointer; font-family:inherit; transition:all .15s; }
.view-btn:hover { background:#2563eb; color:#fff; }
.op-na { color:#d1d5db; font-size:11px; }
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:600px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-body { padding:20px; }
.modal-loading { text-align:center; padding:40px 0; color:#94a3b8; font-size:13px; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
.btn-ghost { padding:7px 16px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.result-summary { font-size:14px; font-weight:600; color:#0f172a; margin-bottom:16px; padding:10px 14px; background:#f0fdf4; border-radius:8px; }
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

.result-items { display:flex; flex-direction:column; gap:10px; }
.result-item { display:flex; gap:14px; padding:14px 16px; border:1px solid #f0f2f5; border-radius:10px; background:#fafbfc; }
.ri-sev-icon { flex-shrink:0; width:36px; height:36px; display:flex; align-items:center; justify-content:center; border-radius:8px; }
.ri-sev-icon.sev-high { background:#fef2f2; color:#dc2626; }
.ri-sev-icon.sev-medium { background:#fffbeb; color:#d97706; }
.ri-sev-icon.sev-low { background:#f0fdf4; color:#16a34a; }
.ri-type-row { display:flex; align-items:center; justify-content:space-between; margin-bottom:8px; }
.ri-type { font-size:14px; font-weight:600; color:#0f172a; }
.ri-badge { font-size:10px; font-weight:700; padding:2px 8px; border-radius:4px; height:fit-content; white-space:nowrap; }
.ri-detail-row { display:flex; align-items:center; gap:8px; margin-bottom:4px; }
.ri-detail-label { font-size:11px; color:#94a3b8; font-weight:500; min-width:56px; flex-shrink:0; }
.ri-detail-val { font-size:12px; color:#334155; line-height:1.5; }
.conf-bar-wrap { display:inline-block; width:60px; height:6px; background:#f1f5f9; border-radius:3px; vertical-align:middle; overflow:hidden; }
.conf-bar { display:block; height:100%; background:#4338ca; border-radius:3px; transition:width .3s; }
.ri-suggest { margin-top:6px; font-size:11px; color:#2563eb; background:#eff6ff; padding:4px 10px; border-radius:4px; display:inline-block; }
.result-items { display:flex; flex-direction:column; gap:12px; }
.result-item { display:flex; gap:12px; padding:12px 14px; border:1px solid #f0f2f5; border-radius:8px; }
.ri-badge { font-size:10px; font-weight:700; padding:2px 8px; border-radius:4px; height:fit-content; white-space:nowrap; }
.sev-high { background:#fef2f2; color:#dc2626; }
.sev-medium { background:#fffbeb; color:#d97706; }
.sev-low { background:#f0fdf4; color:#16a34a; }
.ri-info { flex:1; }
.ri-type { font-size:13px; font-weight:600; color:#0f172a; margin-bottom:4px; }
.ri-detail { font-size:11px; color:#64748b; margin-bottom:2px; }
.ri-suggest { font-size:11px; color:#2563eb; margin-top:4px; background:#eff6ff; padding:4px 8px; border-radius:4px; }.btn-dispatch { padding:7px 16px; background:#4338ca; border:none; border-radius:6px; color:#fff; font-size:12px; font-weight:600; font-family:inherit; cursor:pointer; transition:background .15s; }
.btn-dispatch:hover { background:#3730a3; }
</style>




