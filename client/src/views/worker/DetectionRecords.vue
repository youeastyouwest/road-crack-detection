<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>{{ t("detect.title") }}</h2>
      <p class="page-desc">{{ t("detect.desc") }}</p>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchQuery" :placeholder="t('detect.searchPlaceholder')" clearable style="width:240px" />
      <el-select v-model="statusFilter" :placeholder="t('detect.statusFilter')" clearable style="width:140px">
        <el-option :label="t('status.completed')" value="COMPLETED" />
        <el-option :label="t('detect.processing')" value="PROCESSING" />
        <el-option :label="t('status.failed')" value="FAILED" />
      </el-select>
    </div>

    <div class="records-list">
      <div v-for="item in records" :key="item.id" class="record-card">
        <div class="record-top">
          <span class="record-road">{{ item.roadName || t("detect.unknownRoad") }}</span>
          <el-tag :type="item.status === 'COMPLETED' ? 'success' : item.status === 'PROCESSING' ? 'warning' : 'danger'" size="small">
            {{ item.status === "COMPLETED" ? t("status.completed") : item.status === "PROCESSING" ? t("detect.detecting") : t("status.failed") }}
          </el-tag>
        </div>
        <div class="record-meta">
          <span>{{ t("detect.location", { loc: item.location || "—" }) }}</span>
          <span>{{ t("detect.crackCount", { count: item.crackCount || 0 }) }}</span>
          <span>{{ item.createdAt }}</span>
        </div>
      </div>
      <el-empty v-if="records.length === 0" :description="t('detect.noRecords')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import { t } from "@/i18n"

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
