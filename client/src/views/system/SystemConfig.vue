<template>
  <div class="sc">
    <div class="page-head">
      <div class="page-head-left">
        <h2 class="page-title">系统配置</h2>
        <p class="page-desc">全局系统参数与偏好设置</p>
      </div>
      <button class="btn-primary" @click="handleSave" :disabled="saving">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
        {{ saving ? '保存中...' : '保存配置' }}
      </button>
    </div>

    <div class="config-grid">
      <div class="panel">
        <div class="panel-head"><span class="panel-title">基本设置</span></div>
        <div class="panel-body">
          <div class="cfg-row">
            <div class="cfg-label">站点名称</div>
            <input v-model="config.siteName" class="cfg-input" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">Logo</div>
            <input v-model="config.logo" class="cfg-input" placeholder="/logo.png" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">语言</div>
            <select v-model="config.language" class="cfg-input">
              <option value="zh-CN">简体中文</option>
              <option value="en">English</option>
            </select>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">允许注册</div>
            <label class="toggle">
              <input type="checkbox" v-model="config.allowRegister" />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head"><span class="panel-title">检测设置</span></div>
        <div class="panel-body">
          <div class="cfg-row">
            <div class="cfg-label">检测间隔（秒）</div>
            <input v-model.number="config.detectionInterval" type="number" class="cfg-input cfg-input-num" min="5" max="300" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">告警阈值</div>
            <input v-model.number="config.alertThreshold" type="number" class="cfg-input cfg-input-num" min="1" max="20" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">数据保留（天）</div>
            <input v-model.number="config.dataRetentionDays" type="number" class="cfg-input cfg-input-num" min="30" max="720" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">最大上传（MB）</div>
            <input v-model.number="config.maxUploadSize" type="number" class="cfg-input cfg-input-num" min="1" max="200" />
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head"><span class="panel-title">通知设置</span></div>
        <div class="panel-body">
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">邮件通知</div>
            <label class="toggle">
              <input type="checkbox" v-model="config.emailNotify" />
              <span class="toggle-slider"></span>
            </label>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">短信通知</div>
            <label class="toggle">
              <input type="checkbox" v-model="config.smsNotify" />
              <span class="toggle-slider"></span>
            </label>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">养护通知</div>
            <label class="toggle">
              <input type="checkbox" v-model="config.maintenanceNotify" />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { ElMessage } from "element-plus"
import http from "@/api/index"
import type { ApiResponse, SystemConfigResponse } from "@/types"

const saving = ref(false)
const config = reactive<SystemConfigResponse>({
  siteName: "途安智巡道路裂缝检测系统",
  logo: "/logo.png",
  detectionInterval: 30,
  alertThreshold: 3,
  maintenanceNotify: true,
  dataRetentionDays: 180,
  maxUploadSize: 50,
  allowRegister: false,
  emailNotify: true,
  smsNotify: false,
  darkMode: false,
  language: "zh-CN"
})

async function loadConfig() {
  try {
    const r = await http.get<ApiResponse<SystemConfigResponse>>("/system-config")
    Object.assign(config, r.data.data)
  } catch {}
}

async function handleSave() {
  saving.value = true
  try {
    await http.put("/system-config", { ...config })
    ElMessage.success("配置已保存")
  } catch { ElMessage.error("保存失败") }
  finally { saving.value = false }
}

onMounted(loadConfig)
</script>
<style scoped>
.sc { font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,sans-serif; max-width:1000px; margin:0 auto; }
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:32px; }
.page-title { font-size:22px; font-weight:600; color:#111827; margin:0 0 4px; letter-spacing:-.3px; }
.page-desc { font-size:13px; color:#9ca3af; margin:0; }
.config-grid { display:grid; grid-template-columns:1fr 1fr; gap:20px; }
.config-grid > .panel:last-child { grid-column:1/-1; max-width:520px; }
.panel { background:#fff; border:1px solid #f3f4f6; border-radius:12px; overflow:hidden; box-shadow:0 1px 3px rgba(0,0,0,.04); }
.panel-head { padding:16px 20px; border-bottom:1px solid #f9fafb; }
.panel-title { font-size:14px; font-weight:600; color:#374151; }
.panel-body { padding:8px 0; }
.cfg-row { display:flex; align-items:center; justify-content:space-between; padding:12px 20px; border-bottom:1px solid #fafbfc; }
.cfg-row:last-child { border-bottom:none; }
.cfg-label { font-size:13px; color:#374151; font-weight:500; }
.cfg-input { padding:8px 12px; border:1px solid #e5e7eb; border-radius:8px; font-size:13px; font-family:inherit; color:#111827; outline:none; width:200px; transition:border-color .15s; background:#fff; }
.cfg-input:focus { border-color:#4338ca; box-shadow:0 0 0 3px rgba(67,56,202,.08); }
.cfg-input-num { width:100px; text-align:center; }
.cfg-row-switch .cfg-input { width:auto; }
.toggle { position:relative; display:inline-block; width:40px; height:22px; flex-shrink:0; cursor:pointer; }
.toggle input { opacity:0; width:0; height:0; }
.toggle-slider { position:absolute; cursor:pointer; inset:0; background:#e5e7eb; border-radius:11px; transition:background .2s; }
.toggle-slider::before { content:""; position:absolute; width:18px; height:18px; left:2px; bottom:2px; background:#fff; border-radius:50%; transition:transform .2s; }
.toggle input:checked + .toggle-slider { background:#4338ca; }
.toggle input:checked + .toggle-slider::before { transform:translateX(18px); }
.btn-primary { display:inline-flex; align-items:center; gap:6px; padding:9px 22px; background:#4338ca; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:600; font-family:inherit; cursor:pointer; transition:all .15s; }
.btn-primary:hover { background:#3730a3; }
.btn-primary:disabled { background:#e5e7eb; color:#9ca3af; cursor:not-allowed; }
@media(max-width:800px) { .config-grid { grid-template-columns:1fr; } }
</style>