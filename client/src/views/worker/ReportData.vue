<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>上报数据</h2>
      <p class="page-desc">上传巡检过程中发现的病害照片和描述信息</p>
    </div>

    <div class="report-form">
      <el-form label-width="100px">
        <el-form-item label="道路名称">
          <el-select v-model="form.roadName" placeholder="选择道路" filterable style="width:100%">
            <el-option label="解放路" value="解放路" />
            <el-option label="建设大道" value="建设大道" />
            <el-option label="人民路" value="人民路" />
          </el-select>
        </el-form-item>
        <el-form-item label="病害位置">
          <el-input v-model="form.location" placeholder="如：K12+300 处" />
        </el-form-item>
        <el-form-item label="病害类型">
          <el-select v-model="form.damageType" placeholder="选择类型" style="width:100%">
            <el-option label="横向裂缝" value="TRANSVERSE_CRACK" />
            <el-option label="纵向裂缝" value="LONGITUDINAL_CRACK" />
            <el-option label="网状裂缝" value="NET_CRACK" />
            <el-option label="坑槽" value="POTHOLE" />
            <el-option label="标志损坏" value="MARKING_DAMAGE" />
            <el-option label="路面抛洒" value="ROAD_SPILL" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-radio-group v-model="form.severity">
            <el-radio value="LOW">轻微</el-radio>
            <el-radio value="MEDIUM">中等</el-radio>
            <el-radio value="HIGH">严重</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="照片">
          <el-upload :auto-upload="false" list-type="picture-card" multiple>
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="补充描述..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交上报</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue"
import { ElMessage } from "element-plus"

const form = reactive({
  roadName: "",
  location: "",
  damageType: "",
  severity: "LOW",
  description: "",
})
const submitting = ref(false)

async function handleSubmit() {
  if (!form.roadName) return ElMessage.warning("请选择道路名称")
  submitting.value = true
  await new Promise(r => setTimeout(r, 600))
  submitting.value = false
  ElMessage.success("上报成功")
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
