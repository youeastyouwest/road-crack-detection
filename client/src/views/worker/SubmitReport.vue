<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>上报维修报告</h2>
      <p class="page-desc">完成工单处理后，提交维修报告给部门管理员审核</p>
    </div>

    <!-- 工单选择 -->
    <div class="report-form">
      <el-form label-width="110px">
        <el-form-item label="关联工单" required>
          <el-select v-model="form.workOrderId" placeholder="选择已完成的工单" filterable style="width:100%" @change="onOrderChange">
            <el-option v-for="wo in availableOrders" :key="wo.id" :label="`#${wo.workOrderCode || wo.id} ${wo.title}`" :value="wo.id" />
          </el-select>
        </el-form-item>

        <!-- 选中工单信息 -->
        <div v-if="selectedOrder" class="order-info-card">
          <div class="order-info-row">
            <span class="oi-label">位置</span><span class="oi-val">{{ selectedOrder.location || '--' }}</span>
            <span class="oi-label">部门</span><span class="oi-val">{{ deptLabel(selectedOrder.departmentCode) }}</span>
          </div>
          <div class="order-info-row">
            <span class="oi-label">等级</span><span class="oi-val">{{ sevLabel(selectedOrder.severityLevel) }}</span>
            <span class="oi-label">状态</span><span class="oi-val">{{ statusLabel(selectedOrder.status) }}</span>
          </div>
        </div>

        <el-form-item label="执行人员">
          <el-input v-model="form.executor" :placeholder="authStore.realName || '请输入姓名'" />
        </el-form-item>

        <el-form-item label="使用材料">
          <el-input v-model="form.materials" placeholder="如：沥青混凝土 5吨, 灌缝胶 20kg" />
        </el-form-item>

        <el-form-item label="维修前照片">
          <el-upload
            ref="beforeUploadRef"
            :auto-upload="false"
            list-type="picture-card"
            :limit="3"
            :file-list="beforeFileList"
            :on-change="(file: any) => handleFileChange(file, 'before')"
            :on-remove="(file: any) => handleFileRemove(file, 'before')"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="维修后照片">
          <el-upload
            ref="afterUploadRef"
            :auto-upload="false"
            list-type="picture-card"
            :limit="3"
            :file-list="afterFileList"
            :on-change="(file: any) => handleFileChange(file, 'after')"
            :on-remove="(file: any) => handleFileRemove(file, 'after')"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="维修描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述维修过程和结果..." />
        </el-form-item>

        <el-form-item label="完成时间">
          <el-date-picker v-model="form.finishedAt" type="datetime" placeholder="选择完成时间" style="width:100%" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" />
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
import { reactive, ref, computed, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { workOrderApi, reportApi, fileApi } from "@/api"
import type { WorkOrderResponse } from "@/types"
import { ElMessage } from "element-plus"
import { Plus } from "@element-plus/icons-vue"
import type { UploadFile, UploadUserFile } from "element-plus"

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const submitting = ref(false)

const beforeUploadRef = ref()
const afterUploadRef = ref()
const beforeFileList = ref<UploadUserFile[]>([])
const afterFileList = ref<UploadUserFile[]>([])

const form = reactive({
  workOrderId: null as number | null,
  executor: authStore.realName || "",
  materials: "",
  beforeImageUrls: [] as string[],
  afterImageUrls: [] as string[],
  description: "",
  finishedAt: null as string | null,
})

const availableOrders = ref<WorkOrderResponse[]>([])

const selectedOrder = computed(() => {
  if (!form.workOrderId) return null
  return availableOrders.value.find(o => o.id === form.workOrderId) || null
})

onMounted(async () => {
  // Load work orders assigned to this user that are COMPLETED or REJECTED
  try {
    const res = await workOrderApi.list({
      assignee: authStore.realName || authStore.username,
    } as any)
    const all = res.data.data.records || res.data.data || []
    availableOrders.value = all.filter(o =>
      o.status === "COMPLETED" || o.status === "REJECTED"
    )
  } catch {
    // Fallback: load all and filter
    try {
      const res = await workOrderApi.list({} as any)
      const all = res.data.data.records || res.data.data || []
      const myName = authStore.realName || authStore.username
      availableOrders.value = all.filter(o =>
        o.assignee === myName &&
        (o.status === "COMPLETED" || o.status === "REJECTED")
      )
    } catch {
      availableOrders.value = []
    }
  }

  // If workOrderId is in the URL query, pre-select it
  const qId = route.query.workOrderId
  if (qId) {
    form.workOrderId = Number(qId)
    onOrderChange()
  }
})

function onOrderChange() {
  // Reset fields when order changes but keep executor
  form.materials = ""
  form.beforeImageUrls = []
  form.afterImageUrls = []
  beforeFileList.value = []
  afterFileList.value = []
  form.description = ""
  form.finishedAt = null
}

function handleFileChange(file: UploadFile, type: "before" | "after") {
  if (type === "before") {
    beforeFileList.value.push(file)
  } else {
    afterFileList.value.push(file)
  }
}

function handleFileRemove(file: UploadFile, type: "before" | "after") {
  const list = type === "before" ? beforeFileList.value : afterFileList.value
  const idx = list.findIndex(f => f.uid === file.uid)
  if (idx > -1) list.splice(idx, 1)
}

// 上传图片到服务器，返回 URL 列表
async function uploadImages(files: UploadUserFile[]): Promise<string[]> {
  const urls: string[] = []
  for (const f of files) {
    if (f.raw) {
      try {
        const res = await fileApi.upload(f.raw)
        urls.push(res.data.data.url)
      } catch {
        ElMessage.warning(`图片 ${f.name} 上传失败`)
      }
    }
  }
  return urls
}

function deptLabel(code?: string) {
  return ({ ROAD_ADMIN: "道路管理部", SANITATION: "环卫部", TRAFFIC_POLICE: "交警部" } as any)[code || ""] || code || "--"
}

function sevLabel(s?: string) {
  return ({ LOW: "轻微", MEDIUM: "中等", HIGH: "严重" } as any)[s || ""] || "--"
}

function statusLabel(s: string) {
  return ({ COMPLETED: "已完成", REJECTED: "已驳回（需重新提交）" } as any)[s] || s
}

async function handleSubmit() {
  if (!form.workOrderId) return ElMessage.warning("请选择关联工单")
  if (!form.executor) return ElMessage.warning("请输入执行人员")
  if (!form.description) return ElMessage.warning("请填写维修描述")
  if (!form.finishedAt) return ElMessage.warning("请选择完成时间")

  submitting.value = true
  try {
    // 先上传图片
    const beforeUrls = beforeFileList.value.length > 0
      ? await uploadImages(beforeFileList.value)
      : []
    const afterUrls = afterFileList.value.length > 0
      ? await uploadImages(afterFileList.value)
      : []

    await reportApi.create({
      workOrderId: form.workOrderId,
      executor: form.executor,
      beforeImageUrl: beforeUrls.join(",") || undefined,
      afterImageUrl: afterUrls.join(",") || undefined,
      materials: form.materials || undefined,
      description: form.description,
      finishedAt: form.finishedAt,
    })
    ElMessage.success("维修报告已提交，等待部门管理员审核")
    handleReset()
    router.push("/my-work-orders")
  } catch (err: any) {
    const msg = err?.response?.data?.message || "提交失败"
    ElMessage.error(msg)
  }
  submitting.value = false
}

function handleReset() {
  form.workOrderId = null
  form.materials = ""
  form.beforeImageUrls = []
  form.afterImageUrls = []
  beforeFileList.value = []
  afterFileList.value = []
  form.description = ""
  form.finishedAt = null
}
</script>

<style scoped>
.worker-page { max-width: 800px; margin: 0 auto; font-family: Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 18px; font-weight: 700; color: #1a202c; margin: 0 0 6px 0; }
.page-desc { font-size: 13px; color: #64748b; margin: 0; }
.report-form { background: #fff; border: 1px solid #eef0f4; border-radius: 12px; padding: 28px; }
.order-info-card { background: #f8f9fc; border: 1px solid #eef0f4; border-radius: 8px; padding: 14px 16px; margin-bottom: 20px; }
.order-info-row { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; }
.order-info-row:last-child { margin-bottom: 0; }
.oi-label { font-size: 11px; color: #94a3b8; font-weight: 600; min-width: 40px; }
.oi-val { font-size: 13px; color: #475569; }
</style>
