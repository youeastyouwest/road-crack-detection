<template>
  <div class="crowd-records-page">
    <div class="cr-header">
      <h2>{{ t("crowd.recordsTitle") }}</h2>
    </div>

    <div class="records-list">
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
      <el-empty v-if="records.length === 0" :description="t('crowd.noRecords')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import { t } from "@/i18n"

const records = ref<any[]>([])

onMounted(() => {
  records.value = [
    { id: 1, roadName: "解放路", location: "K12+300", damageType: "坑槽", status: "ACCEPTED", createdAt: "2026-07-06 14:30" },
    { id: 2, roadName: "建设大道", location: "K5+800", damageType: "横向裂缝", status: "PENDING", createdAt: "2026-07-05 10:15" },
    { id: 3, roadName: "人民路", location: "K3+200", damageType: "标志损坏", status: "RESOLVED", createdAt: "2026-07-03 09:00" },
  ]
})

function statusType(s: string) {
  const map: Record<string, string> = { PENDING: "info", ACCEPTED: "warning", RESOLVED: "success", REJECTED: "danger" }
  return map[s] || "info"
}
function statusLabel(s: string) {
  const map: Record<string, () => string> = {
    PENDING: () => t("crowd.status_pending"),
    ACCEPTED: () => t("crowd.status_accepted"),
    RESOLVED: () => t("crowd.status_processed"),
    REJECTED: () => t("crowd.status_rejected"),
  }
  return map[s]?.() || s
}
</script>

<style scoped>
.crowd-records-page { max-width: 500px; margin: 0 auto; }
.cr-header { margin-bottom: 16px; }
.cr-header h2 { font-size: 20px; font-weight: 700; color: #1a202c; margin: 0; }
.records-list { display: flex; flex-direction: column; gap: 10px; }
.record-item { background: #fff; border-radius: 12px; padding: 14px; display: flex; align-items: center; gap: 12px; }
.record-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.record-icon.PENDING { background: #fef3c7; color: #d97706; }
.record-icon.ACCEPTED { background: #dbeafe; color: #2563eb; }
.record-icon.RESOLVED { background: #d1fae5; color: #059669; }
.record-icon.REJECTED { background: #fee2e2; color: #dc2626; }
.record-info { flex: 1; min-width: 0; }
.record-title { font-size: 14px; font-weight: 600; color: #1a202c; margin-bottom: 2px; }
.record-location { font-size: 12px; color: #64748b; }
.record-time { font-size: 11px; color: #94a3b8; margin-top: 2px; }
</style>
