<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>检测记录</h2>
      <p class="page-desc">查看本部门相关的道路检测结果</p>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchQuery" placeholder="搜索道路名称" clearable style="width:240px" />
      <el-select v-model="statusFilter" placeholder="检测状态" clearable style="width:140px">
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="失败" value="FAILED" />
      </el-select>
    </div>

    <div class="records-list">
      <div v-for="item in records" :key="item.id" class="record-card">
        <div class="record-top">
          <span class="record-road">{{ item.roadName || "未知道路" }}</span>
          <el-tag :type="item.status === 'COMPLETED' ? 'success' : item.status === 'PROCESSING' ? 'warning' : 'danger'" size="small">
            {{ item.status === "COMPLETED" ? "已完成" : item.status === "PROCESSING" ? "检测中" : "失败" }}
          </el-tag>
        </div>
        <div class="record-meta">
          <span>位置: {{ item.location || "—" }}</span>
          <span>裂缝数: {{ item.crackCount || 0 }}</span>
          <span>{{ item.createdAt }}</span>
        </div>
      </div>
      <el-empty v-if="records.length === 0" description="暂无检测记录" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"

const searchQuery = ref("")
const statusFilter = ref("")
const records = ref<any[]>([])

onMounted(() => {
  records.value = [
    { id: 1, roadName: "解放路", location: "K12+300", status: "COMPLETED", crackCount: 5, createdAt: "2026-07-05" },
    { id: 2, roadName: "建设大道", location: "K5+800", status: "COMPLETED", crackCount: 3, createdAt: "2026-07-04" },
    { id: 3, roadName: "人民路", location: "K3+200", status: "PROCESSING", crackCount: 0, createdAt: "2026-07-06" },
  ]
})
</script>

<style scoped>
.worker-page { max-width: 1000px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 18px; font-weight: 700; color: #1a202c; margin: 0 0 6px 0; }
.page-desc { font-size: 13px; color: #64748b; margin: 0; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.records-list { display: flex; flex-direction: column; gap: 12px; }
.record-card { background: #fff; border: 1px solid #eef0f4; border-radius: 12px; padding: 16px; }
.record-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.record-road { font-size: 14px; font-weight: 600; color: #1a202c; }
.record-meta { display: flex; gap: 16px; font-size: 12px; color: #64748b; }
</style>
