<template>
  <div class="sys-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">系统配置</h2>
        <p class="page-desc">全局系统参数与功能偏好设置</p>
      </div>
      <button class="btn-primary" @click="handleSave" :disabled="saving">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
        {{ saving ? '保存中...' : '保存配置' }}
      </button>
    </div>

    <div class="config-grid">
      <!-- 基本设置 -->
      <div class="content-card">
        <div class="toolbar"><span style="font-size:14px;font-weight:600;color:#0f172a;display:flex;align-items:center;gap:8px"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/></svg>基本设置</span></div>
        <div class="cfg-body">
          <div class="cfg-row">
            <div class="cfg-label">站点名称</div>
            <input v-model="config.siteName" class="cfg-input" placeholder="系统名称" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">界面语言</div>
            <select v-model="config.language" class="cfg-input">
              <option value="zh-CN">简体中文</option>
              <option value="en">English</option>
            </select>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">允许注册<div class="cfg-hint">开放用户自助注册功能</div></div>
            <label class="toggle"><input type="checkbox" v-model="config.allowRegister" /><span class="toggle-slider"></span></label>
          </div>
        </div>
      </div>

      <!-- 检测设置 -->
      <div class="content-card">
        <div class="toolbar"><span style="font-size:14px;font-weight:600;color:#0f172a;display:flex;align-items:center;gap:8px"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="1.5"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></svg>检测设置</span></div>
        <div class="cfg-body">
          <div class="cfg-row">
            <div class="cfg-label">检测间隔（秒）<div class="cfg-hint">两次自动检测之间的间隔</div></div>
            <input v-model.number="config.detectionInterval" type="number" class="cfg-input cfg-input-num" min="5" max="300" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">告警阈值<div class="cfg-hint">检测到几处病害后触发告警</div></div>
            <input v-model.number="config.alertThreshold" type="number" class="cfg-input cfg-input-num" min="1" max="20" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">数据保留（天）<div class="cfg-hint">检测数据自动清理周期</div></div>
            <input v-model.number="config.dataRetentionDays" type="number" class="cfg-input cfg-input-num" min="30" max="720" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">最大上传（MB）<div class="cfg-hint">单次上传文件大小限制</div></div>
            <input v-model.number="config.maxUploadSize" type="number" class="cfg-input cfg-input-num" min="1" max="200" />
          </div>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="content-card">
        <div class="toolbar"><span style="font-size:14px;font-weight:600;color:#0f172a;display:flex;align-items:center;gap:8px"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="1.5"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>通知设置</span></div>
        <div class="cfg-body">
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">邮件通知<div class="cfg-hint">严重病害通过邮件推送</div></div>
            <label class="toggle"><input type="checkbox" v-model="config.emailNotification" /><span class="toggle-slider"></span></label>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">短信通知<div class="cfg-hint">紧急告警通过短信推送</div></div>
            <label class="toggle"><input type="checkbox" v-model="config.smsNotification" /><span class="toggle-slider"></span></label>
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">养护通知<div class="cfg-hint">工单状态变更通知维修工</div></div>
            <label class="toggle"><input type="checkbox" v-model="config.maintenanceAlert" /><span class="toggle-slider"></span></label>
          </div>
        </div>
      </div>

      <!-- 安全设置 -->
      <div class="content-card">
        <div class="toolbar"><span style="font-size:14px;font-weight:600;color:#0f172a;display:flex;align-items:center;gap:8px"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>安全设置</span></div>
        <div class="cfg-body">
          <div class="cfg-row">
            <div class="cfg-label">密码最小长度<div class="cfg-hint">用户密码最低字符数</div></div>
            <input v-model.number="config.minPasswordLength" type="number" class="cfg-input cfg-input-num" min="6" max="32" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">登录失败锁定<div class="cfg-hint">连续失败几次后锁定账户</div></div>
            <input v-model.number="config.maxLoginAttempts" type="number" class="cfg-input cfg-input-num" min="3" max="10" />
          </div>
          <div class="cfg-row">
            <div class="cfg-label">会话超时（分钟）<div class="cfg-hint">无操作后自动退出</div></div>
            <input v-model.number="config.sessionTimeout" type="number" class="cfg-input cfg-input-num" min="5" max="1440" />
          </div>
          <div class="cfg-row cfg-row-switch">
            <div class="cfg-label">验证码登录<div class="cfg-hint">登录时需要输入图形验证码</div></div>
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
import type { SystemConfigResponse } from "@/types"

const saving = ref(false)
const STORAGE_KEY = "road_crack_system_config"

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
}

const config = reactive<SystemConfigResponse>({ ...defaultConfig })

function loadConfig() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      Object.assign(config, JSON.parse(saved))
    }
  } catch {}
}

async function handleSave() {
  saving.value = true
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(config))
    await new Promise(r => setTimeout(r, 300))
    ElMessage.success("配置已保存")
  } catch {
    ElMessage.error("保存失败")
  }
  finally { saving.value = false }
}

function resetConfig() {
  Object.assign(config, defaultConfig)
  ElMessage.info("已重置为默认配置")
}

onMounted(loadConfig)
</script>

<style scoped>
.sys-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }

.config-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.toolbar { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f0f2f5; }

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

.toggle { position:relative; display:inline-block; width:40px; height:22px; flex-shrink:0; cursor:pointer; }
.toggle input { opacity:0; width:0; height:0; }
.toggle-slider { position:absolute; cursor:pointer; inset:0; background:#e2e8f0; border-radius:11px; transition:background .2s; }
.toggle-slider::before { content:""; position:absolute; width:18px; height:18px; left:2px; bottom:2px; background:#fff; border-radius:50%; transition:transform .2s; }
.toggle input:checked + .toggle-slider { background:#2563eb; }
.toggle input:checked + .toggle-slider::before { transform:translateX(18px); }

@media(max-width:800px) { .config-grid { grid-template-columns:1fr; } }
</style>
