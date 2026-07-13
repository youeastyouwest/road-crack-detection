<template>
  <div class="crowd-report-page">
    <div class="cr-header">
      <h2>{{ t("crowd.reportTitle") }}</h2>
      <p>{{ t("crowd.reportDesc") }}</p>
    </div>

    <div class="cr-card">
      <div class="photo-upload" @click="handleTakePhoto">
        <div class="photo-placeholder">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
            <circle cx="12" cy="13" r="4"/>
          </svg>
          <span>{{ t("crowd.takePhoto") }}</span>
        </div>
        <img v-if="photoPreview" :src="photoPreview" class="photo-preview" />
      </div>

      <div class="cr-form">
        <div class="form-row">
          <label>{{ t("crowd.roadName") }}</label>
          <input v-model="form.roadName" :placeholder='t("crowd.roadPlaceholder")' />
        </div>
        <div class="form-row">
          <label>{{ t("crowd.address") }}</label>
          <input v-model="form.location" :placeholder='t("crowd.addressPlaceholder")' />
        </div>
        <div class="form-row">
          <label>{{ t("crowd.damageType") }}</label>
          <select v-model="form.damageType">
            <option value="">{{ t("crowd.selectType") }}</option>
            <option value="TRANSVERSE_CRACK">{{ t("crowd.type_transverse") }}</option>
            <option value="LONGITUDINAL_CRACK">{{ t("crowd.type_longitudinal") }}</option>
            <option value="NET_CRACK">{{ t("crowd.type_net") }}</option>
            <option value="POTHOLE">{{ t("crowd.type_pothole") }}</option>
            <option value="MARKING_DAMAGE">{{ t("crowd.type_marking") }}</option>
            <option value="ROAD_SPILL">{{ t("crowd.type_spill") }}</option>
            <option value="OTHER">{{ t("crowd.type_other") }}</option>
          </select>
        </div>
        <div class="form-row">
          <label>{{ t("crowd.remark") }}</label>
          <textarea v-model="form.description" rows="3" :placeholder='t("crowd.remarkPlaceholder")'></textarea>
        </div>
        <button class="submit-btn" :disabled="!canSubmit" @click="handleSubmit">
          {{ t("crowd.submit") }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from "vue"
import { ElMessage } from "element-plus"
import { t } from "@/i18n"

const form = reactive({
  roadName: "",
  location: "",
  damageType: "",
  description: "",
})
const photoPreview = ref("")
const submitting = ref(false)

const canSubmit = computed(() => form.roadName && form.location && form.damageType)

function handleTakePhoto() {
  // In a real app, would use navigator.mediaDevices or file input
  ElMessage.info(t("crowd.cameraNotSupported"))
}

async function handleSubmit() {
  submitting.value = true
  await new Promise(r => setTimeout(r, 800))
  submitting.value = false
  ElMessage.success(t("crowd.submitSuccess"))
  form.roadName = ""
  form.location = ""
  form.damageType = ""
  form.description = ""
  photoPreview.value = ""
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
.form-row input, .form-row select, .form-row textarea { width: 100%; padding: 10px 12px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 14px; font-family: inherit; background: #fff; outline: none; transition: border-color 0.15s; box-sizing: border-box; }
.form-row input:focus, .form-row select:focus, .form-row textarea:focus { border-color: #4361ee; }
.submit-btn { width: 100%; padding: 12px; background: #4361ee; color: #fff; border: none; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.15s; margin-top: 8px; }
.submit-btn:hover:not(:disabled) { background: #3651d4; }
.submit-btn:active:not(:disabled) { transform: translateY(1px); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
