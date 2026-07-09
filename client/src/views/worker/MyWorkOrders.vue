<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>我的工单</h2>
      <div class="header-stats">
        <div class="stat-chip pending">{{ pendingCount }} 待处理</div>
        <div class="stat-chip progress">{{ inProgressCount }} 进行中</div>
        <div class="stat-chip done">{{ completedCount }} 已完成</div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <el-input v-model="searchQuery" placeholder="搜索工单标题 / 道路名称" clearable style="width:260px" />
      <el-select v-model="statusFilter" placeholder="工单状态" clearable style="width:140px">
        <el-option label="待处理" value="PENDING" />
        <el-option label="处理中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
    </div>

    <!-- Work order list -->
    <div class="work-order-list">
      <div v-for="item in filteredOrders" :key="item.id" class="wo-card" @click="openDetail(item)">
        <div class="wo-top">
          <span class="wo-title">{{ item.title }}</span>
          <el-tag :type="statusType(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
        </div>
        <div class="wo-meta">
          <span>{{ item.roadName }}</span>
          <span>{{ item.location || "位置待定" }}</span>
          <span>截止: {{ item.deadline || "未设置" }}</span>
        </div>
        <div class="wo-bottom">
          <el-tag v-if="item.severityLevel === 'HIGH'" type="danger" size="small">严重</el-tag>
          <el-tag v-else-if="item.severityLevel === 'MEDIUM'" type="warning" size="small">中等</el-tag>
          <el-tag v-else type="info" size="small">轻微</el-tag>
          <span class="wo-time">{{ item.createdAt }}</span>
        </div>
      </div>
      <el-empty v-if="filteredOrders.length === 0" description="暂无工单" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue"
import { useAuthStore } from "@/stores/auth"
import { workOrderApi } from "@/api"
import type { WorkOrderResponse } from "@/types"
import { ElMessage, ElMessageBox } from "element-plus"

const authStore = useAuthStore()
const searchQuery = ref("")
const statusFilter = ref("")
const orders = ref<WorkOrderResponse[]>([])

const pendingCount = computed(() => orders.value.filter(o => o.status === "PENDING" || o.status === "ASSIGNED").length)
const inProgressCount = computed(() => orders.value.filter(o => o.status === "IN_PROGRESS").length)
const completedCount = computed(() => orders.value.filter(o => o.status === "COMPLETED").length)

const filteredOrders = computed(() => {
  return orders.value.filter(o => {
    if (searchQuery.value && !o.title.includes(searchQuery.value) && !o.roadName?.includes(searchQuery.value)) return false
    if (statusFilter.value && o.status !== statusFilter.value) return false
    return true
  })
})

onMounted(async () => {
  try {
    const params: any = {}
    if (authStore.deptCode) params.departmentCode = authStore.deptCode
    const res = await workOrderApi.list(params)
    orders.value = res.data.data.records || res.data.data || []
  } catch {
    // fallback mock
    orders.value = [
      { id: 1, title: "解放路裂缝修补", roadName: "解放路", severityLevel: "MEDIUM", status: "ASSIGNED", assignee: "张三", location: "K12+300", createdAt: "2026-07-01" },
      { id: 2, title: "建设大道坑槽修复", roadName: "建设大道", severityLevel: "HIGH", status: "PENDING", location: "K5+800", createdAt: "2026-07-02" },
    ] as WorkOrderResponse[]
  }
})

function statusType(s: string) {
  const map: Record<string, string> = { PENDING: "info", ASSIGNED: "warning", IN_PROGRESS: "primary", COMPLETED: "success", CANCELLED: "info" }
  return map[s] || "info"
}
function statusLabel(s: string) {
  const map: Record<string, string> = { PENDING: "待处理", ASSIGNED: "已派单", IN_PROGRESS: "处理中", COMPLETED: "已完成", CANCELLED: "已取消" }
  return map[s] || s
}
function openDetail(item: WorkOrderResponse) {
  ElMessage.info(`工单 #${item.id}: ${item.title}`)
}
</script>

<style scoped>
.worker-page { max-width: 1000px; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 18px; font-weight: 700; color: #1a202c; margin: 0; }
.header-stats { display: flex; gap: 8px; }
.stat-chip { padding: 4px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.stat-chip.pending { background: #fef3c7; color: #d97706; }
.stat-chip.progress { background: #dbeafe; color: #2563eb; }
.stat-chip.done { background: #d1fae5; color: #059669; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.work-order-list { display: flex; flex-direction: column; gap: 12px; }
.wo-card { background: #fff; border: 1px solid #eef0f4; border-radius: 12px; padding: 16px; cursor: pointer; transition: all 0.15s; }
.wo-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); border-color: #cbd5e1; }
.wo-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.wo-title { font-size: 14px; font-weight: 600; color: #1a202c; }
.wo-meta { display: flex; gap: 16px; font-size: 12px; color: #64748b; margin-bottom: 8px; }
.wo-bottom { display: flex; align-items: center; gap: 10px; }
.wo-time { font-size: 11px; color: #94a3b8; margin-left: auto; }
.complete-btn { padding: 4px 14px; background: #059669; color: #fff; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.15s; margin-left: 8px; font-family: inherit; }
.complete-btn:hover { background: #047857; }
</style>
