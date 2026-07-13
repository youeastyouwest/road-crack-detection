<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>{{ t("submit.title") }}</h2>
      <p class="page-desc">{{ t("submit.desc") }}</p>
    </div>

    <!-- 工单选择 -->
    <div class="report-form">
      <el-form label-width="110px">
        <el-form-item :label="t('submit.relatedOrder')" required>
          <el-select v-model="form.workOrderId" :placeholder="t('submit.selectOrder')" filterable style="width:100%" @change="onOrderChange">
            <el-option v-for="wo in availableOrders" :key="wo.id" :label="`#${wo.workOrderCode || wo.id} ${wo.title}`" :value="wo.id" />
          </el-select>
        </el-form-item>

        <!-- 选中工单信息 -->
        <div v-if="selectedOrder" class="order-info-card">
          <div class="order-info-row">
            <span class="oi-label">{{ t("ds.road") }}</span><span class="oi-val">{{ selectedOrder.location || '--' }}</span>
            <span class="oi-label">{{ t("wo.dept") }}</span><span class="oi-val">{{ deptLabel(selectedOrder.departmentCode) }}</span>
          </div>
          <div class="order-info-row">
            <span class="oi-label">{{ t("wo.level") }}</span><span class="oi-val">{{ sevLabel(selectedOrder.severityLevel) }}</span>
            <span class="oi-label">{{ t("common.status") }}</span><span class="oi-val">{{ statusLabel(selectedOrder.status) }}</span>
          </div>
        </div>

        <el-form-item :label="t('submit.executor')">
          <el-input v-model="form.executor" :placeholder="authStore.realName || t('submit.executorPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('submit.materials')">
          <el-input v-model="form.materials" :placeholder="t('submit.materialsPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('submit.beforePhoto')">
          <el-input v-model="form.beforeImageUrl" :placeholder="t('submit.beforePhotoPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('submit.afterPhoto')">
          <el-input v-model="form.afterImageUrl" :placeholder="t('submit.afterPhotoPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('submit.desc2')">
          <el-input v-model="form.description" type="textarea" :rows="4" :placeholder="t('submit.descPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('submit.finishTime')">
          <el-date-picker v-model="form.finishedAt" type="datetime" placeholder="选择完成时间" style="width:100%" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">{{ t("submit.submitBtn") }}</el-button>
          <el-button size="large" @click="handleReset">{{ t("common.reset") }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { workOrderApi, reportApi } from "@/api"
import type { WorkOrderResponse } from "@/types"
import { ElMessage } from "element-plus"
import { t } from "@/i18n"

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const submitting = ref(false)

const form = reactive({
  workOrderId: null as number | null,
  executor: authStore.realName || "",
  materials: "",
  beforeImageUrl: "",
  afterImageUrl: "",
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
  form.beforeImageUrl = ""
  form.afterImageUrl = ""
  form.description = ""
  form.finishedAt = null
}

function deptLabel(code?: string) {
  return ({ ROAD_ADMIN: t("user.roleRoadAdmin"), SANITATION: t("user.roleSanitAdmin"), TRAFFIC_POLICE: t("user.roleTrafficAdmin") } as any)[code || ""] || code || "--"
}

function sevLabel(s?: string) {
  return ({ LOW: t("severity.low"), MEDIUM: t("severity.medium"), HIGH: t("severity.high") } as any)[s || ""] || "--"
}

function statusLabel(s: string) {
  return ({ COMPLETED: t("status.completed"), REJECTED: t("wo.cancel") } as any)[s] || s
}

async function handleSubmit() {
  if (!form.workOrderId) return ElMessage.warning(t("submit.selectOrder"))
  if (!form.executor) return ElMessage.warning(t("submit.executor"))
  if (!form.description) return ElMessage.warning(t("submit.descPlaceholder"))
  if (!form.finishedAt) return ElMessage.warning(t("submit.finishTime"))

  submitting.value = true
  try {
    await reportApi.create({
      workOrderId: form.workOrderId,
      executor: form.executor,
      beforeImageUrl: form.beforeImageUrl || undefined,
      afterImageUrl: form.afterImageUrl || undefined,
      materials: form.materials || undefined,
      description: form.description,
      finishedAt: form.finishedAt,
    })
    ElMessage.success(t("submit.submitSuccess"))
    handleReset()
    // Navigate back to my work orders
    router.push("/my-work-orders")
  } catch (err: any) {
    const msg = err?.response?.data?.message || t("submit.submitFailed")
    ElMessage.error(msg)
  }
  submitting.value = false
}

function handleReset() {
  form.workOrderId = null
  form.materials = ""
  form.beforeImageUrl = ""
  form.afterImageUrl = ""
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
