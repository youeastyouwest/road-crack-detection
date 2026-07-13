<template>
  <div class="crowd-profile-page">
    <div class="profile-header">
      <div class="avatar-circle">{{ (authStore.realName || authStore.username || "?")[0] }}</div>
      <h2>{{ authStore.realName || authStore.username || t("auth.logout") }}</h2>
      <p>{{ t("crowd.report") }}</p>
    </div>

    <div class="profile-stats">
      <div class="stat-item">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">{{ t("crowd.records") }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-num">{{ stats.accepted }}</span>
        <span class="stat-label">{{ t("crowd.status_accepted") }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-num">{{ stats.resolved }}</span>
        <span class="stat-label">{{ t("crowd.status_processed") }}</span>
      </div>
    </div>

    <div class="profile-actions">
      <button class="action-btn" @click="handleLogout">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
          <polyline points="16 17 21 12 16 7"/>
          <line x1="21" y1="12" x2="9" y2="12"/>
        </svg>
        {{ t("auth.logout") }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue"
import { useAuthStore } from "@/stores/auth"
import { ElMessageBox } from "element-plus"
import { t } from "@/i18n"

const authStore = useAuthStore()

const stats = reactive({
  total: 23,
  accepted: 18,
  resolved: 12,
})

function handleLogout() {
  ElMessageBox.confirm(t("auth.logoutConfirm"), t("common.tip"), { confirmButtonText: t("auth.confirm"), cancelButtonText: t("auth.cancel"), type: "warning" })
    .then(() => authStore.logout())
    .catch(() => {})
}
</script>

<style scoped>
.crowd-profile-page { max-width: 500px; margin: 0 auto; }
.profile-header { text-align: center; padding: 32px 0; }
.avatar-circle { width: 72px; height: 72px; border-radius: 50%; background: linear-gradient(135deg, #4361ee, #8b5cf6); color: #fff; font-size: 28px; font-weight: 700; display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; }
.profile-header h2 { font-size: 20px; font-weight: 700; color: #1a202c; margin: 0 0 4px 0; }
.profile-header p { font-size: 13px; color: #64748b; margin: 0; }
.profile-stats { display: flex; background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
.stat-item { flex: 1; text-align: center; display: flex; flex-direction: column; gap: 4px; }
.stat-num { font-size: 22px; font-weight: 700; color: #1a202c; }
.stat-label { font-size: 12px; color: #94a3b8; }
.profile-actions { display: flex; flex-direction: column; gap: 8px; }
.action-btn { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px; background: #fff; border: 1px solid #eef0f4; border-radius: 12px; color: #dc2626; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.15s; }
.action-btn:hover { border-color: #dc2626; }
</style>
