<template>
  <div class="crowd-records-page">
    <div class="cr-header">
      <h2>{{ t("crowd.recordsTitle") }}</h2>
    </div>

    <div class="records-list">
      <div v-if="loading" class="records-loading">正在加载上报记录...</div>
      <div v-for="item in records" :key="item.id" class="record-item">
        <div class="record-icon" :class="item.status">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
          </svg>
        </div>
        <div class="record-info">
          <div class="record-title">{{ item.roadName }} · {{ item.damageType }}</div>
          <div class="record-location">{{ item.location }}</div>
          <div class="record-time">{{ item.createdAt }}</div>
        </div>
        <div class="record-status">
          <el-tag :type="statusType(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
        </div>
      </div>
      <el-empty v-if="!loading && records.length === 0" :description="t('crowd.noRecords')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { detectionApi, workOrderApi } from "@/api"
import { useAuthStore } from "@/stores/auth"
import { t } from "@/i18n"
import type { DetectionResultResponse, DetectionTaskResponse, WorkOrderResponse } from "@/types"

interface CrowdRecordItem {
  id: number
  roadName: string
  location: string
  damageType: string
  status: string
  createdAt: string
}

const authStore = useAuthStore()
const loading = ref(false)
const records = ref<CrowdRecordItem[]>([])

onMounted(() => {
  loadRecords()
})

function statusType(s: string) {
  const map: Record<string, string> = {
    PENDING: "warning",
    PROCESSING: "warning",
    COMPLETED: "success",
    FAILED: "danger",
  }
  return map[s] || "info"
}

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING: "处理中",
    PROCESSING: "处理中",
    COMPLETED: "已完成",
    FAILED: "检测失败",
  }
  return map[s] || s
}

async function loadRecords() {
  loading.value = true
  try {
    const submittedBy = authStore.username || authStore.realName || undefined
    const res = await detectionApi.list({ page: 1, size: 50, submittedBy })
    const taskList = res.data.data.records
    const resultMap = new Map<number, DetectionResultResponse | null>()
    const workOrderMap = new Map<number, WorkOrderResponse | null>()

    await Promise.all(taskList
      .filter(task => task.status === "COMPLETED")
      .map(async task => {
        try {
          const resultRes = await detectionApi.getResult(task.id)
          const result = resultRes.data.data
          resultMap.set(task.id, result)
          if (result?.generatedWorkOrderId) {
            try {
              const workOrderRes = await workOrderApi.get(result.generatedWorkOrderId)
              workOrderMap.set(task.id, workOrderRes.data.data)
            } catch {
              workOrderMap.set(task.id, null)
            }
          }
        } catch {
          resultMap.set(task.id, null)
        }
      }))

    records.value = taskList.map(task => toRecordItem(task, resultMap.get(task.id) || null, workOrderMap.get(task.id) || null))
  } catch {
    records.value = []
    ElMessage.error("加载上报记录失败")
  } finally {
    loading.value = false
  }
}

function toRecordItem(task: DetectionTaskResponse, result: DetectionResultResponse | null, workOrder: WorkOrderResponse | null): CrowdRecordItem {
  return {
    id: task.id,
    roadName: extractRoadName(task.location),
    location: task.location || "--",
    damageType: resolveDamageType(task, result),
    status: resolveDisplayStatus(task.status, workOrder),
    createdAt: formatDateTime(task.createdAt),
  }
}

function extractRoadName(location?: string) {
  if (!location) return "位置上报"
  const roadName = location.split("(")[0]?.trim()
  return roadName && roadName !== location ? roadName : "位置上报"
}

function resolveDamageType(task: DetectionTaskResponse, result: DetectionResultResponse | null) {
  const items = result?.items || []
  if (items.length > 0) {
    const topItem = [...items].sort((a, b) => severityScore(b.severityLevel) - severityScore(a.severityLevel))[0]
    return damageTypeLabel(topItem.damageType)
  }
  if (task.status === "FAILED") return "识别失败"
  if (task.status === "COMPLETED") return "未识别到病害"
  return "待识别"
}

function severityScore(level?: string) {
  return ({ HIGH: 3, MEDIUM: 2, LOW: 1 })[level || ""] || 0
}

function damageTypeLabel(type?: string) {
  return ({
    CRACK: "裂缝",
    TRANSVERSE_CRACK: "横向裂缝",
    LONGITUDINAL_CRACK: "纵向裂缝",
    NET_CRACK: "网状裂缝",
    POTHOLE: "坑洞",
    MARKING_DAMAGE: "标线损坏",
    ROAD_SPILL: "路面抛洒",
  })[type || ""] || (type || "待识别")
}

function formatDateTime(value?: string) {
  if (!value) return "--"
  return value.replace("T", " ").slice(0, 16)
}

function resolveDisplayStatus(taskStatus?: string, workOrder?: WorkOrderResponse | null) {
  if (taskStatus === "FAILED") return "FAILED"
  if (!workOrder) {
    return taskStatus === "COMPLETED" ? "COMPLETED" : "PROCESSING"
  }

  const finishedStatuses = ["COMPLETED", "CLOSED"]
  if (finishedStatuses.includes(workOrder.status)) {
    return "COMPLETED"
  }

  return "PROCESSING"
}
</script>

<style scoped>
.crowd-records-page { max-width: 500px; margin: 0 auto; }
.cr-header { margin-bottom: 16px; }
.cr-header h2 { font-size: 20px; font-weight: 700; color: #1a202c; margin: 0; }
.records-list { display: flex; flex-direction: column; gap: 10px; }
.records-loading { padding: 18px; text-align: center; color: #64748b; font-size: 13px; }
.record-item { background: #fff; border-radius: 12px; padding: 14px; display: flex; align-items: center; gap: 12px; }
.record-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.record-icon.PENDING { background: #fef3c7; color: #d97706; }
.record-icon.PROCESSING { background: #dbeafe; color: #2563eb; }
.record-icon.COMPLETED { background: #d1fae5; color: #059669; }
.record-icon.FAILED { background: #fee2e2; color: #dc2626; }
.record-info { flex: 1; min-width: 0; }
.record-title { font-size: 14px; font-weight: 600; color: #1a202c; margin-bottom: 2px; }
.record-location { font-size: 12px; color: #64748b; }
.record-time { font-size: 11px; color: #94a3b8; margin-top: 2px; }
</style>
