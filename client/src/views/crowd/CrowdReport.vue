<template>
  <div class="crowd-report-page">
    <div class="cr-header">
      <h2>路面病害上报</h2>
      <p>拍照记录路面问题，提交后系统将自动处理</p>
    </div>

    <div class="cr-card">
      <div class="photo-upload" @click="triggerFileInput">
        <div v-if="!photoPreview" class="photo-placeholder">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
            <circle cx="12" cy="13" r="4"/>
          </svg>
          <span>点击拍照 / 选择图片</span>
        </div>
        <img v-if="photoPreview" :src="photoPreview" class="photo-preview" />
        <input ref="fileInputRef" type="file" accept="image/*" capture="environment" style="display:none" @change="handleFileChange" />
      </div>

      <div class="cr-form">
        <div class="form-row">
          <label>所在道路</label>
          <input v-model="form.roadName" placeholder="输入道路名称（如：解放路）" />
        </div>
        <div class="form-row">
          <label>具体位置 <span class="req">*</span></label>
          <input v-model="form.location" placeholder="描述位置（如：XX路口北50米）" />
        </div>
        <div class="form-row">
          <label>病害类型</label>
          <select v-model="form.damageType">
            <option value="">选择类型</option>
            <option value="TRANSVERSE_CRACK">横向裂缝</option>
            <option value="LONGITUDINAL_CRACK">纵向裂缝</option>
            <option value="NET_CRACK">网状裂缝</option>
            <option value="POTHOLE">坑槽</option>
            <option value="MARKING_DAMAGE">标志损坏</option>
            <option value="ROAD_SPILL">路面抛洒</option>
            <option value="OTHER">其他</option>
          </select>
        </div>
        <div class="form-row">
          <label>补充描述</label>
          <textarea v-model="form.description" rows="3" placeholder="补充描述（可选）"></textarea>
        </div>
        <button class="submit-btn" :disabled="!canSubmit || submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交上报' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from "vue"
import { ElMessage } from "element-plus"
import { crowdReportApi } from "@/api"

const form = reactive({
  roadName: "",
  location: "",
  damageType: "",
  description: "",
})
const photoPreview = ref("")
const photoBase64 = ref("")
const submitting = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

const canSubmit = computed(() => form.location && photoBase64.value)

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning("图片大小不能超过 5MB")
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    const result = reader.result as string
    photoPreview.value = result
    photoBase64.value = result
  }
  reader.readAsDataURL(file)
}

async function handleSubmit() {
  if (!form.location.trim()) {
    ElMessage.warning("请填写具体位置")
    return
  }
  if (!photoBase64.value) {
    ElMessage.warning("请先拍照或选择图片")
    return
  }
  submitting.value = true
  try {
    // 将道路名和位置合并为 location 字段
    const fullLocation = form.roadName.trim()
      ? `${form.roadName.trim()} - ${form.location.trim()}`
      : form.location.trim()

    await crowdReportApi.submit({
      location: fullLocation,
      imageUrl: photoBase64.value,
      damageType: form.damageType || undefined,
      description: form.description.trim() || undefined,
    })
    ElMessage.success("上报成功！系统已收到您的反馈")
    // 清空表单
    form.roadName = ""
    form.location = ""
    form.damageType = ""
    form.description = ""
    photoPreview.value = ""
    photoBase64.value = ""
    if (fileInputRef.value) fileInputRef.value.value = ""
  } catch (err: any) {
    const msg = err?.response?.data?.message || "上报失败，请稍后重试"
    ElMessage.error(msg)
  }
  submitting.value = false
}
</script>

<style scoped>
.crowd-report-page { max-width: 500px; margin: 0 auto; }
.cr-header { text-align: center; margin-bottom: 20px; }
.cr-header h2 { font-size: 20px; font-weight: 700; color: #1a202c; margin: 0 0 6px 0; }
.cr-header p { font-size: 13px; color: #64748b; margin: 0; }
.cr-card { background: #fff; border-radius: 16px; overflow: hidden; }
.photo-upload { position: relative; width: 100%; height: 200px; background: #f8f9fc; display: flex; align-items: center; justify-content: center; cursor: pointer; border-bottom: 1px solid #eef0f4; overflow: hidden; }
.photo-placeholder { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #94a3b8; }
.photo-placeholder svg { color: #cbd5e1; }
.photo-preview { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.cr-form { padding: 20px; display: flex; flex-direction: column; gap: 14px; }
.form-row label { display: block; font-size: 13px; font-weight: 600; color: #4a5568; margin-bottom: 6px; }
.form-row .req { color: #dc2626; }
.form-row input, .form-row select, .form-row textarea { width: 100%; padding: 10px 12px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 14px; font-family: inherit; background: #fff; outline: none; transition: border-color 0.15s; box-sizing: border-box; }
.form-row input:focus, .form-row select:focus, .form-row textarea:focus { border-color: #4361ee; }
.submit-btn { width: 100%; padding: 12px; background: #4361ee; color: #fff; border: none; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.15s; margin-top: 8px; }
.submit-btn:hover:not(:disabled) { background: #3651d4; }
.submit-btn:active:not(:disabled) { transform: translateY(1px); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
