<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>{{ t("reportData.title") }}</h2>
      <p class="page-desc">{{ t("reportData.desc") }}</p>
    </div>

    <div class="report-form">
      <el-form label-width="100px">
        <el-form-item :label="t('reportData.roadName')">
          <el-select v-model="form.roadName" :placeholder="t('reportData.selectRoad')" filterable style="width:100%">
            <el-option label="解放路" value="解放路" />
            <el-option label="建设大道" value="建设大道" />
            <el-option label="人民路" value="人民路" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('reportData.damageLocation')">
          <el-input v-model="form.location" :placeholder="t('reportData.locPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('reportData.damageType')">
          <el-select v-model="form.damageType" :placeholder="t('reportData.selectType')" style="width:100%">
            <el-option :label="t('damage.transverseCrack')" value="TRANSVERSE_CRACK" />
            <el-option :label="t('damage.longitudinalCrack')" value="LONGITUDINAL_CRACK" />
            <el-option :label="t('damage.netCrack')" value="NET_CRACK" />
            <el-option :label="t('damage.pothole')" value="POTHOLE" />
            <el-option :label="t('damage.markingDamage')" value="MARKING_DAMAGE" />
            <el-option :label="t('damage.roadSpill')" value="ROAD_SPILL" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('reportData.severity')">
          <el-radio-group v-model="form.severity">
            <el-radio value="LOW">{{ t("severity.low") }}</el-radio>
            <el-radio value="MEDIUM">{{ t("severity.medium") }}</el-radio>
            <el-radio value="HIGH">{{ t("severity.high") }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('reportData.photo')">
          <el-upload :auto-upload="false" list-type="picture-card" multiple>
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item :label="t('reportData.remark')">
          <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="t('reportData.remarkPlaceholder')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ t("reportData.submit") }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue"
import { ElMessage } from "element-plus"
import { t } from "@/i18n"

const form = reactive({
  roadName: "",
  location: "",
  damageType: "",
  severity: "LOW",
  description: "",
})
const submitting = ref(false)

async function handleSubmit() {
  if (!form.roadName) return ElMessage.warning(t("reportData.noRoadSelected"))
  submitting.value = true
  await new Promise(r => setTimeout(r, 600))
  submitting.value = false
  ElMessage.success(t("reportData.submitSuccess"))
  form.roadName = ""
  form.location = ""
  form.damageType = ""
  form.severity = "LOW"
  form.description = ""
}
</script>

<style scoped>
.worker-page { max-width: 700px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 18px; font-weight: 700; color: #1a202c; margin: 0 0 6px 0; }
.page-desc { font-size: 13px; color: #64748b; margin: 0; }
.report-form { background: #fff; border: 1px solid #eef0f4; border-radius: 12px; padding: 24px; }
</style>
