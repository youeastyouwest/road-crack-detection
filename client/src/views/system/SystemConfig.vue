<template>
  <div :class="['sys-page', { dark: isDark }]">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ t('sys.pageTitle') }}</h2>
        <p class="page-desc">{{ t('sys.pageDesc') }}</p>
      </div>
      <div class="page-actions">
        <button class="btn-secondary" @click="resetConfig" :disabled="saving || loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12a9 9 0 109-9 9.75 9.75 0 00-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg>
          {{ t('sys.resetDefault') }}
        </button>
        <button class="btn-primary" @click="handleSave" :disabled="saving || loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
          {{ saving ? t('sys.saving') : t('sys.save') }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap">
      <div class="spinner"></div>
      <p>{{ t('sys.loading') }}</p>
    </div>

    <div v-else class="config-grid">
      <!-- 基本设置 -->
      <div class="content-card">
        <div class="toolbar"><span class="card-title"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/></svg>{{ t('sys.basicSettings') }}</span></div>
        <div class="cfg-body">
          <div class="cfg-row">
            <div class="cfg-label">{{ t('sys.siteName') }}</div>
            <input v-model="config.siteName" class="cfg-input" :placeholder="t('sys.siteNamePlaceholder')" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">{{ t('sys.language') }}</div>
            <select v-model="config.language" class="cfg-input">
            <option value="zh-CN">{{ t('sys.languageZh') }}</option>
            <option value="en">{{ t('sys.languageEn') }}</option>
            </select>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">{{ t('sys.darkMode') }}<div class="cfg-hint">{{ t('sys.darkModeHint') }}</div></div>
            <label class="toggle"><input type="checkbox" v-model="isDark" @change="applyDarkMode" /><span class="toggle-slider"></span></label>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">{{ t('sys.allowRegister') }}<div class="cfg-hint">{{ t('sys.allowRegisterHint') }}</div></div>
            <label class="toggle"><input type="checkbox" v-model="config.allowRegister" /><span class="toggle-slider"></span></label>
          </div>
        </div>
      </div>

      <!-- 检测设置 -->
      <div class="content-card">
        <div class="toolbar"><span class="card-title"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></svg>{{ t('sys.detectionSettings') }}</span></div>
        <div class="cfg-body">
          <div class="cfg-row">
            <div class="cfg-label">{{ t('sys.detectionInterval') }}<div class="cfg-hint">{{ t('sys.detectionIntervalHint') }}</div></div>
            <input v-model.number="config.detectionInterval" type="number" class="cfg-input cfg-input-num" min="5" max="300" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">{{ t('sys.alertThreshold') }}<div class="cfg-hint">{{ t('sys.alertThresholdHint') }}</div></div>
            <input v-model.number="config.alertThreshold" type="number" class="cfg-input cfg-input-num" min="1" max="20" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">{{ t('sys.dataRetention') }}<div class="cfg-hint">{{ t('sys.dataRetentionHint') }}</div></div>
            <input v-model.number="config.dataRetentionDays" type="number" class="cfg-input cfg-input-num" min="30" max="720" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">{{ t('sys.maxUpload') }}<div class="cfg-hint">{{ t('sys.maxUploadHint') }}</div></div>
            <input v-model.number="config.maxUploadSize" type="number" class="cfg-input cfg-input-num" min="1" max="200" />
          </div>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="content-card">
        <div class="toolbar"><span class="card-title"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>{{ t('sys.notificationSettings') }}</span></div>
        <div class="cfg-body">
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">{{ t('sys.emailNotification') }}<div class="cfg-hint">{{ t('sys.emailNotificationHint') }}</div></div>
            <label class="toggle"><input type="checkbox" v-model="config.emailNotification" /><span class="toggle-slider"></span></label>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">{{ t('sys.smsNotification') }}<div class="cfg-hint">{{ t('sys.smsNotificationHint') }}</div></div>
            <label class="toggle"><input type="checkbox" v-model="config.smsNotification" /><span class="toggle-slider"></span></label>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">{{ t('sys.maintenanceAlert') }}<div class="cfg-hint">{{ t('sys.maintenanceAlertHint') }}</div></div>
            <label class="toggle"><input type="checkbox" v-model="config.maintenanceAlert" /><span class="toggle-slider"></span></label>
          </div>
        </div>
      </div>

      <!-- 安全设置 -->
      <div class="content-card">
        <div class="toolbar"><span class="card-title"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>{{ t('sys.securitySettings') }}</span></div>
        <div class="cfg-body">
          <div class="cfg-row">
            <div class="cfg-label">{{ t('sys.minPasswordLength') }}<div class="cfg-hint">{{ t('sys.minPasswordLengthHint') }}</div></div>
            <input v-model.number="config.minPasswordLength" type="number" class="cfg-input cfg-input-num" min="6" max="32" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">{{ t('sys.maxLoginAttempts') }}<div class="cfg-hint">{{ t('sys.maxLoginAttemptsHint') }}</div></div>
            <input v-model.number="config.maxLoginAttempts" type="number" class="cfg-input cfg-input-num" min="3" max="10" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">{{ t('sys.sessionTimeout') }}<div class="cfg-hint">{{ t('sys.sessionTimeoutHint') }}</div></div>
            <input v-model.number="config.sessionTimeout" type="number" class="cfg-input cfg-input-num" min="5" max="1440" />
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">{{ t('sys.captchaEnabled') }}<div class="cfg-hint">{{ t('sys.captchaEnabledHint') }}</div></div>
            <label class="toggle"><input type="checkbox" v-model="config.captchaEnabled" /><span class="toggle-slider"></span></label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { configApi } from "@/api/config"
import { useAppStore } from "@/stores/app"
import { t, setLocale } from "@/i18n"
import type { SystemConfigResponse, SystemConfigRequest } from "@/types"

const loading = ref(false)
const saving = ref(false)
const isDark = ref(false)
const THEME_KEY = "road_crack_dark_mode"
const appStore = useAppStore()


const defaultConfig: SystemConfigResponse = {
  siteName: "途安智巡道路裂缝检测系统",
  language: "zh-CN",
  allowRegister: false,
  detectionInterval: 30,
  alertThreshold: 3,
  dataRetentionDays: 180,
  maxUploadSize: 50,
  emailNotification: true,
  smsNotification: false,
  maintenanceAlert: true,
  minPasswordLength: 6,
  maxLoginAttempts: 5,
  sessionTimeout: 30,
  captchaEnabled: false,
  darkMode: false,
}

const config = reactive<SystemConfigResponse>({ ...defaultConfig })

function applyDarkMode() {
  document.documentElement.classList.toggle("dark", isDark.value)
  localStorage.setItem(THEME_KEY, isDark.value ? "1" : "0")
}

async function loadConfig() {
  loading.value = true
  try {
    const res = await configApi.get()
    if (res.data && res.data.code === 200 && res.data.data) {
      Object.assign(config, res.data.data)
      isDark.value = !!config.darkMode
      appStore.setSystemConfig(res.data.data)
      setLocale((config.language || "zh-CN") as "zh-CN" | "en")
    } else {
      ElMessage.warning(res.data?.message || t("sys.loadFailed"))
      Object.assign(config, defaultConfig)
      isDark.value = false
      setLocale((defaultConfig.language || "zh-CN") as "zh-CN" | "en")
    }
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || t("sys.loadFailed")
    ElMessage.error(msg)
    Object.assign(config, defaultConfig)
    isDark.value = false
    setLocale((defaultConfig.language || "zh-CN") as "zh-CN" | "en")
  } finally {
    loading.value = false
    applyDarkMode()
  }
}

async function handleSave() {
  saving.value = true
  try {
    config.darkMode = isDark.value
    const request: SystemConfigRequest = {
      siteName: config.siteName || "途安智巡道路裂缝检测系统",
      language: config.language || "zh-CN",
      allowRegister: !!config.allowRegister,
      detectionInterval: config.detectionInterval ?? 30,
      alertThreshold: config.alertThreshold ?? 3,
      dataRetentionDays: config.dataRetentionDays ?? 180,
      maxUploadSize: config.maxUploadSize ?? 50,
      emailNotification: !!config.emailNotification,
      smsNotification: !!config.smsNotification,
      maintenanceAlert: !!config.maintenanceAlert,
      minPasswordLength: config.minPasswordLength ?? 6,
      maxLoginAttempts: config.maxLoginAttempts ?? 5,
      sessionTimeout: config.sessionTimeout ?? 30,
      captchaEnabled: !!config.captchaEnabled,
      darkMode: isDark.value,
    }
    const res = await configApi.update(request)
    if (res.data && res.data.code === 200) {
      Object.assign(config, res.data.data)
      appStore.setSystemConfig(res.data.data)
      setLocale((config.language || "zh-CN") as "zh-CN" | "en")
      applyDarkMode()
      ElMessage.success(t("sys.saveSuccess"))
    } else {
      ElMessage.error(res.data?.message || t("sys.saveFailed"))
    }
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || t("sys.saveFailed")
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}

function resetConfig() {
  Object.assign(config, defaultConfig)
  isDark.value = defaultConfig.darkMode ?? false
  appStore.setSystemConfig(defaultConfig)
  setLocale((defaultConfig.language || "zh-CN") as "zh-CN" | "en")
  applyDarkMode()
  ElMessage.info(t("sys.resetHint"))
}


onMounted(() => {
  const savedTheme = localStorage.getItem(THEME_KEY)
  if (savedTheme !== null) {
    isDark.value = savedTheme === "1"
  }
  applyDarkMode()
  loadConfig()
})
</script>

<style scoped>
.sys-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }
.page-actions { display:flex; gap:10px; }

.config-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.toolbar { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f0f2f5; }
.card-title { font-size:14px;font-weight:600;color:#0f172a;display:flex;align-items:center;gap:8px; }

.cfg-body { padding:0; }
.cfg-row { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f5f6f8; gap:16px; }
.cfg-row:last-child { border-bottom:none; }
.cfg-label { font-size:13px; color:#1e293b; font-weight:500; flex:1; }
.cfg-hint { font-size:11px; color:#94a3b8; font-weight:400; margin-top:2px; }
.cfg-input { padding:8px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; font-family:inherit; color:#1e293b; outline:none; width:200px; background:#fff; }
.cfg-input:focus { border-color:#2563eb; }
.cfg-input-num { width:80px; text-align:center; }

.btn-primary { display:inline-flex; align-items:center; gap:6px; padding:7px 16px; border:none; border-radius:6px; background:#2563eb; color:#fff; font-size:12px; font-weight:500; cursor:pointer; font-family:inherit; transition:background .15s; white-space:nowrap; }
.btn-primary:hover { background:#1d4ed8; }
.btn-primary:disabled { background:#93c5fd; cursor:not-allowed; }
.btn-secondary { display:inline-flex; align-items:center; gap:6px; padding:7px 16px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; color:#475569; font-size:12px; font-weight:500; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.btn-secondary:hover { background:#f8fafc; border-color:#cbd5e1; }
.btn-secondary:disabled { opacity:0.6; cursor:not-allowed; }

.toggle { position:relative; display:inline-block; width:40px; height:22px; flex-shrink:0; cursor:pointer; }
.toggle input { opacity:0; width:0; height:0; }
.toggle-slider { position:absolute; cursor:pointer; inset:0; background:#e2e8f0; border-radius:11px; transition:background .2s; }
.toggle-slider::before { content:""; position:absolute; width:18px; height:18px; left:2px; bottom:2px; background:#fff; border-radius:50%; transition:transform .2s; }
.toggle input:checked + .toggle-slider { background:#2563eb; }
.toggle input:checked + .toggle-slider::before { transform:translateX(18px); }

.loading-wrap { display:flex; flex-direction:column; align-items:center; justify-content:center; padding:80px; color:#64748b; gap:12px; }
.spinner { width:32px; height:32px; border:3px solid #e2e8f0; border-top-color:#2563eb; border-radius:50%; animation:spin 1s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }

/* Dark mode styles */
.dark .page-title { color:#f8fafc; }
.dark .page-desc { color:#94a3b8; }
.dark .content-card { background:#1e293b; border-color:#334155; }
.dark .toolbar { border-bottom-color:#334155; }
.dark .card-title { color:#f8fafc; }
.dark .cfg-label { color:#e2e8f0; }
.dark .cfg-hint { color:#94a3b8; }
.dark .cfg-input { background:#0f172a; border-color:#334155; color:#f8fafc; }
.dark .cfg-input:focus { border-color:#60a5fa; }
.dark .btn-secondary { background:#1e293b; border-color:#334155; color:#cbd5e1; }
.dark .btn-secondary:hover { background:#334155; }
.dark .toggle-slider { background:#475569; }
.dark .toggle input:checked + .toggle-slider { background:#3b82f6; }

@media(max-width:800px) { .config-grid { grid-template-columns:1fr; } }
</style>
