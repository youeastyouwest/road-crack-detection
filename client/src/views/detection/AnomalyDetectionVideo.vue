<template>
  <div class="advid">
    <div class="advid-top">
      <div class="advid-top-left">
        <h2 class="advid-title">视频异常检测</h2>
        <span class="advid-subtitle">上传道路视频进行实时裂缝检测</span>
      </div>
      <button class="top-btn" @click="showCreate=true">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新建任务
      </button>
    </div>

    <div class="card">
      <div class="card-head">
        <span class="card-title">检测任务列表</span>
      </div>
      <div class="card-body card-body-nopad">
        <div class="act-header">
          <span class="act-cell act-id">编号</span>
          <span class="act-cell act-loc">位置</span>
          <span class="act-cell act-src">来源</span>
          <span class="act-cell act-st">状态</span>
          <span class="act-cell act-op">操作</span>
        </div>
        <div v-if="loading" class="act-loading"><div class="loader"></div></div>
        <div v-for="t in tasks" :key="t.id" class="act-row">
          <span class="act-cell act-id act-code">{{ t.taskCode }}</span>
          <span class="act-cell act-loc">{{ t.location }}</span>
          <span class="act-cell act-src"><span :class="['src-badge', srcCls(t.dataSourceType)]">{{ srcLbl(t.dataSourceType) }}</span></span>
          <span class="act-cell act-st"><span :class="['st-badge', stCls(t.status)]">{{ stLbl(t.status) }}</span></span>
          <span class="act-cell act-op act-actions">
            <button class="act-btn" @click="viewResult(t)" :disabled="t.status!=='COMPLETED'">查看结果</button>
            <button class="act-btn" @click="execute(t.id!)" :disabled="t.status!=='PENDING'">执行</button>
          </span>
        </div>
        <div v-if="!loading && tasks.length === 0" class="act-empty">暂无任务</div>
      </div>
    </div>

    <!-- Create Dialog -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate=false">
      <div class="modal">
        <div class="modal-head">
          <span class="modal-title">新建检测任务</span>
          <button class="modal-close" @click="showCreate=false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">数据类型</label>
            <div class="select-wrap">
              <select v-model="createForm.dataSourceType" class="form-select">
                <option value="DRONE_VIDEO">无人机视频</option>
                <option value="MANUAL_VIDEO">人工录像</option>
              </select>
              <svg class="select-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">视频文件</label>
            <div class="upload-zone upload-zone-sm" @click="openVidInput">
              <input ref="vidInput" type="file" accept="video/*" hidden @change="handleVideoFileChange" />
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2"/></svg>
              <span class="uz-text-sm">{{ createForm.fileName || '选择文件' }}</span>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">位置</label>
            <input v-model="createForm.location" class="form-input" placeholder="可选" />
          </div>
        </div>
        <div class="modal-foot">
          <button class="modal-btn modal-btn-ghost" @click="showCreate=false">取消</button>
          <button class="modal-btn modal-btn-primary" :disabled="creating" @click="handleCreate">
            <span v-if="creating" class="btn-loader btn-loader-dark"></span>
            <span v-else>提交</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Result Dialog -->
    <div v-if="showResult && resultData" class="modal-overlay" @click.self="showResult=false">
      <div class="modal">
        <div class="modal-head">
          <span class="modal-title">检测结果</span>
          <button class="modal-close" @click="showResult=false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="res-summary">{{ resultData.summary || '检测完成' }}</div>
          <div class="result-list">
            <div v-for="item in resultData.items" :key="item.damageType" class="res-item">
              <span class="res-name">{{ {CRACK:'裂缝',TRANSVERSE_CRACK:'横向裂缝',LONGITUDINAL_CRACK:'纵向裂缝',NET_CRACK:'网状裂缝',MARKING_DAMAGE:'标线损坏',ROAD_SPILL:'路面抛洒',POTHOLE:'坑槽'}[item.damageType]||item.damageType }}</span>
              <span class="res-meta">
                <span :class="['res-badge', sevCls(item.severityLevel)]">{{ {LOW:'轻微',MEDIUM:'中等',HIGH:'严重'}[item.severityLevel] }}</span>
                <span class="res-conf">{{ (item.confidence*100).toFixed(0) }}%</span>
              </span>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button v-if="canDispatch(resultData)" class="modal-btn modal-btn-warn" @click="handleDispatch(selectedTask?.id ?? 0, resultData)">生成工单</button>
          <button class="modal-btn modal-btn-primary" @click="showResult=false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { detectionApi, workOrderApi } from "@/api"
import type { DetectionTaskResponse, DetectionResultResponse } from "@/types"

const tasks = ref<DetectionTaskResponse[]>([])
const loading = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const showResult = ref(false)
const selectedTask = ref<DetectionTaskResponse | null>(null)
const resultData = ref<DetectionResultResponse | null>(null)
const createForm = reactive({ dataSourceType: "DRONE_VIDEO", fileName: "", fileUrl: "", location: "" })
const vidInput = ref<HTMLInputElement | null>(null)

function openVidInput() {
  vidInput.value?.click()
}

function handleVideoFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  createForm.fileName = file.name
  createForm.fileUrl = window.URL.createObjectURL(file)
}

async function loadTasks() {
  loading.value = true
  try { const r = await detectionApi.list({ page: 1, size: 20 }); tasks.value = r.data.data.records } catch {}
  loading.value = false
}
async function handleCreate() {
  creating.value = true
  try {
    await detectionApi.create({ dataSourceType: createForm.dataSourceType as any, fileName: createForm.fileName || "video", fileUrl: createForm.fileUrl || "", location: createForm.location || "未知" })
    ElMessage.success("任务已创建"); showCreate.value = false; await loadTasks()
  } catch { ElMessage.error("创建失败") }
  finally { creating.value = false }
}
async function execute(taskId: number) {
  try { await detectionApi.execute(taskId); ElMessage.success("执行中"); await loadTasks() } catch { ElMessage.error("执行失败") }
}
async function viewResult(row: DetectionTaskResponse) {
  selectedTask.value = row
  try { const r = await detectionApi.getResult(row.id); resultData.value = r.data.data; showResult.value = true } catch { ElMessage.error("获取结果失败") }
}
function srcCls(s: string) { return ({DRONE_VIDEO:"s-drone",MANUAL_VIDEO:"s-vid"})[s]||"" }
function srcLbl(s: string) { return ({DRONE_VIDEO:"无人机",MANUAL_VIDEO:"人工"})[s]||s }
function stCls(s: string) { return ({PENDING:"st-wait",PROCESSING:"st-run",COMPLETED:"st-ok",FAILED:"st-fail"})[s]||"" }
function stLbl(s: string) { return ({PENDING:"待处理",PROCESSING:"处理中",COMPLETED:"已完成",FAILED:"失败"})[s]||s }
function sevCls(s: string) { return ({LOW:"sev-low",MEDIUM:"sev-med",HIGH:"sev-high"})[s]||"" }

function canDispatch(data: DetectionResultResponse | null) {
  return !!data?.items?.length
}

function hasHighSeverity(data: DetectionResultResponse | null) {
  return !!data?.items?.some(i => i.severityLevel === "HIGH")
}

async function handleDispatch(taskId: number, data: DetectionResultResponse) {
  if (!taskId) return
  if (data?.generatedWorkOrderId) {
    ElMessage.info(hasHighSeverity(data)
      ? "由于检测出严重病害，系统已自动生成工单，请前往工单管理查看"
      : "该检测结果已生成工单，请前往工单管理查看")
    return
  }

  const firstItem = data.items?.[0]
  if (!firstItem) {
    ElMessage.warning("当前检测结果缺少病害明细，暂时无法生成工单")
    return
  }

  ElMessageBox.confirm("确定要为该检测结果创建维修工单吗？", "生成工单", {
    confirmButtonText: "确认生成",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        const res = await workOrderApi.create({
          detectionTaskId: taskId,
          title: "病害维修工单",
          damageType: firstItem.damageType as any,
          severityLevel: firstItem.severityLevel as any,
          location: selectedTask.value?.location || "",
          departmentCode: "ROAD_ADMIN" as any,
          evidenceUrl: selectedTask.value?.fileUrl || "",
          description: data.summary || "",
        })
        const workOrderId = res.data.data?.id
        if (workOrderId) {
          data.generatedWorkOrderId = workOrderId
        }
        ElMessage.success("工单已生成")
        window.dispatchEvent(new CustomEvent("data-updated"))
      } catch (e: any) {
        const message = e?.response?.data?.message || ""
        if (message.includes("自动") || message.includes("已生成") || message.includes("已存在")) {
          ElMessage.info(hasHighSeverity(data)
            ? "由于检测出严重病害，系统已自动生成工单，请前往工单管理查看"
            : "该检测结果已生成工单，请前往工单管理查看")
          return
        }
        ElMessage.error("生成失败")
      }
    })
    .catch(() => {})
}

onMounted(loadTasks)
</script>

<style scoped>
.advid { font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,sans-serif; }
.advid-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:28px; }
.advid-top-left { display:flex; align-items:baseline; gap:12px; }
.advid-title { font-size:20px; font-weight:600; color:#111827; margin:0; }
.advid-subtitle { font-size:13px; color:#9ca3af; }
.top-btn { display:flex; align-items:center; gap:6px; padding:8px 16px; background:#4338ca; color:#fff; border:none; border-radius:8px; font-size:12px; font-weight:600; font-family:inherit; cursor:pointer; transition:background .15s; }
.top-btn:hover { background:#3730a3; }

.card { background:#fff; border:1px solid #f3f4f6; border-radius:10px; overflow:hidden; }
.card-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f9fafb; }
.card-title { font-size:13px; font-weight:600; color:#374151; }
.card-body-nopad { padding:0; }

.act-header { display:flex; padding:10px 20px; border-bottom:1px solid #f9fafb; }
.act-cell { font-size:11px; color:#9ca3af; }
.act-id { width:140px; flex-shrink:0; }
.act-loc { flex:1; }
.act-src { width:70px; flex-shrink:0; }
.act-st { width:72px; flex-shrink:0; }
.act-op { width:150px; flex-shrink:0; }
.act-row { display:flex; padding:10px 20px; border-bottom:1px solid #f9fafb; align-items:center; }
.act-row:last-child { border-bottom:none; }
.act-row:hover { background:#fafbfc; }
.act-code { font-family:monospace; font-size:11px; font-weight:600; color:#4338ca; }
.act-actions { display:flex; gap:6px; }
.act-btn { padding:3px 10px; background:transparent; border:1px solid #e5e7eb; border-radius:6px; font-size:11px; font-weight:500; color:#374151; cursor:pointer; font-family:inherit; transition:all .15s; }
.act-btn:hover:not(:disabled) { border-color:#4338ca; color:#4338ca; }
.act-btn:disabled { opacity:.4; cursor:not-allowed; }

.src-badge, .st-badge { display:inline-block; padding:1px 7px; border-radius:4px; font-size:10px; font-weight:600; }
.s-drone { background:#eef2ff; color:#4338ca; }
.s-vid { background:#fffbeb; color:#d97706; }
.st-wait { background:#f9fafb; color:#6b7280; }
.st-run { background:#fffbeb; color:#d97706; }
.st-ok { background:#f0fdf4; color:#22c55e; }
.st-fail { background:#fef2f2; color:#ef4444; }
.act-loading { display:flex; justify-content:center; padding:32px 0; }
.loader { width:20px; height:20px; border:2px solid #f3f4f6; border-top-color:#4338ca; border-radius:50%; animation:spin .5s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
.act-empty { text-align:center; padding:36px 0; color:#9ca3af; font-size:13px; }

/* Modal */
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.3); display:flex; align-items:center; justify-content:center; z-index:1000; }
.modal { background:#fff; border-radius:12px; width:480px; max-width:90vw; max-height:80vh; overflow-y:auto; }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:18px 22px; border-bottom:1px solid #f3f4f6; }
.modal-title { font-size:15px; font-weight:600; color:#111827; }
.modal-close { width:28px; height:28px; display:flex; align-items:center; justify-content:center; background:transparent; border:none; border-radius:6px; color:#9ca3af; cursor:pointer; }
.modal-close:hover { background:#f3f4f6; color:#374151; }
.modal-body { padding:20px 22px; }
.modal-foot { display:flex; gap:8px; justify-content:flex-end; padding:14px 22px; border-top:1px solid #f3f4f6; }
.modal-btn { padding:8px 20px; border-radius:8px; font-size:13px; font-weight:600; font-family:inherit; cursor:pointer; border:1px solid transparent; transition:all .15s; display:flex; align-items:center; gap:6px; }
.modal-btn-primary { background:#4338ca; color:#fff; border-color:#4338ca; }
.modal-btn-primary:hover { background:#3730a3; }
.modal-btn-primary:disabled { background:#e5e7eb; color:#9ca3af; border-color:#e5e7eb; cursor:not-allowed; }
.modal-btn-warn { background:#f59e0b; color:#fff; border-color:#f59e0b; }
.modal-btn-warn:hover { background:#d97706; }
.modal-btn-ghost { background:transparent; color:#6b7280; border-color:#e5e7eb; }
.modal-btn-ghost:hover { background:#f9fafb; }

.form-group { margin-bottom:18px; }
.form-label { display:block; font-size:12px; font-weight:500; color:#374151; margin-bottom:6px; }
.form-input, .form-select { width:100%; padding:9px 12px; border:1px solid #e5e7eb; border-radius:8px; font-size:13px; font-family:inherit; color:#111827; background:#fff; outline:none; transition:border-color .15s; box-sizing:border-box; }
.form-input:focus, .form-select:focus { border-color:#4338ca; }
.form-input::placeholder { color:#9ca3af; }
.select-wrap { position:relative; }
.select-arrow { position:absolute; right:10px; top:50%; transform:translateY(-50%); pointer-events:none; color:#9ca3af; }
.form-select { appearance:none; padding-right:28px; cursor:pointer; }

.upload-zone-sm { display:flex; align-items:center; gap:8px; padding:10px 14px; border:1.5px dashed #e5e7eb; border-radius:8px; cursor:pointer; transition:all .15s; }
.upload-zone-sm:hover { border-color:#4338ca; background:#fafbfc; }
.uz-text-sm { font-size:12px; color:#6b7280; }
.res-summary { font-size:13px; color:#6b7280; margin-bottom:14px; }
.result-list { border-top:1px solid #f9fafb; }
.res-item { display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid #f9fafb; font-size:13px; }
.res-item:last-child { border-bottom:none; }
.res-name { color:#374151; font-weight:500; }
.res-meta { display:flex; align-items:center; gap:8px; }
.res-badge { display:inline-block; padding:1px 7px; border-radius:4px; font-size:10px; font-weight:600; }
.sev-low { background:#f0fdf4; color:#22c55e; }
.sev-med { background:#fffbeb; color:#d97706; }
.sev-high { background:#fef2f2; color:#ef4444; }
.res-conf { font-size:12px; color:#9ca3af; font-variant-numeric:tabular-nums; }
.btn-loader-dark { width:14px; height:14px; border:2px solid rgba(255,255,255,.3); border-top-color:#fff; border-radius:50%; animation:spin .5s linear infinite; display:inline-block; }
</style>
