<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>上报维修报告</h2>
      <p class="page-desc">完成工单处理后，提交维修报告给管理员审核</p>
    </div>

    <div class="report-form">
      <el-form label-width="110px">
        <el-form-item label="关联工单" required>
          <el-select v-model="form.workOrderId" placeholder="选择已完成的工单" filterable style="width:100%">
            <el-option v-for="wo in completedOrders" :key="wo.id" :label="`#${wo.id} ${wo.title} - ${wo.roadName}`" :value="wo.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="执行人员">
          <el-input v-model="form.executor" :placeholder="authStore.realName || '请输入姓名'" />
        </el-form-item>

        <el-form-item label="维修方法">
          <el-select v-model="form.repairMethod" placeholder="选择维修方法" style="width:100%">
            <el-option label="裂缝灌缝" value="裂缝灌缝" />
            <el-option label="坑槽修补" value="坑槽修补" />
            <el-option label="路面铣刨" value="路面铣刨" />
            <el-option label="标线重划" value="标线重划" />
            <el-option label="结构补强" value="结构补强" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="使用材料">
          <el-input v-model="form.materials" placeholder="如：沥青混凝土 5吨, 灌缝胶 20kg" />
        </el-form-item>

        <el-form-item label="维修前照片">
          <el-upload :auto-upload="false" list-type="picture-card" multiple>
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="维修后照片">
          <el-upload :auto-upload="false" list-type="picture-card" multiple>
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="维修描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述维修过程和结果..." />
        </el-form-item>

        <el-form-item label="完成时间">
          <el-date-picker v-model="form.finishedAt" type="datetime" placeholder="选择完成时间" style="width:100%" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">提交报告</el-button>
          <el-button size="large" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from "vue"
import { useAuthStore } from "@/stores/auth"
import { ElMessage } from "element-plus"

const authStore = useAuthStore()
const submitting = ref(false)

const form = reactive({
  workOrderId: null as number | null,
  executor: authStore.realName || "",
  repairMethod: "",
  materials: "",
  description: "",
  finishedAt: null as string | null,
})

const completedOrders = ref([
  { id: 3, title: "人民路路面维修", roadName: "人民路" },
  { id: 5, title: "解放路裂缝应急处理", roadName: "解放路" },
])

onMounted(() => {
  // In real app, fetch completed/available work orders from API
  form.executor = authStore.realName || ""
})

async function handleSubmit() {
  if (!form.workOrderId) return ElMessage.warning("请选择关联工单")
  if (!form.repairMethod) return ElMessage.warning("请选择维修方法")
  submitting.value = true
  await new Promise(r => setTimeout(r, 800))
  submitting.value = false
  ElMessage.success("维修报告已提交，等待管理员审核")
  handleReset()
}

function handleReset() {
  form.workOrderId = null
  form.repairMethod = ""
  form.materials = ""
  form.description = ""
  form.finishedAt = null
}
</script>

<style scoped>
.worker-page { max-width: 800px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 18px; font-weight: 700; color: #1a202c; margin: 0 0 6px 0; }
.page-desc { font-size: 13px; color: #64748b; margin: 0; }
.report-form { background: #fff; border: 1px solid #eef0f4; border-radius: 12px; padding: 28px; }
</style>
