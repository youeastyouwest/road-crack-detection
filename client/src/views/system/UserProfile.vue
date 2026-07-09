<template>
  <div class="up">
    <div class="page-head">
      <div class="page-head-left">
        <h2 class="page-title">个人中心</h2>
        <p class="page-desc">个人信息与密码管理</p>
      </div>
    </div>

    <div class="profile-card">
      <div class="profile-left">
        <div class="avatar-ring">
          <div class="avatar-bg">
            <span class="avatar-initials">{{ authStore.realName?.[0] || authStore.username?.[0] || "U" }}</span>
          </div>
        </div>
        <div class="profile-name">{{ authStore.realName || authStore.username }}</div>
        <div class="profile-role">{{ roleLabel }}</div>
        <div class="profile-meta">
          <div class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span>{{ authStore.username }}</span>
          </div>
          <div v-if="userDetail?.user?.email || authStore.email" class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
            <span>{{ userDetail?.user?.email || authStore.email }}</span>
          </div>
        </div>
      </div>

      <div class="profile-divider"></div>

      <div class="profile-right">
        <div class="section-head">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4338ca" stroke-width="1.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
          修改密码
        </div>
        <div class="form-row">
          <label class="form-label">原密码</label>
          <input v-model="pwdForm.oldPassword" type="password" class="form-input" placeholder="输入当前密码" />
        </div>
        <div class="form-row">
          <label class="form-label">新密码</label>
          <input v-model="pwdForm.newPassword" type="password" class="form-input" placeholder="输入新密码（至少 6 位）" />
        </div>
        <button class="btn-primary" @click="handleChangePwd" :disabled="changing">
          <span v-if="changing" class="btn-loader"></span>
          <span v-else>更新密码</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue"
import { ElMessage } from "element-plus"
import { useAuthStore } from "@/stores/auth"
import { authApi } from "@/api"

const authStore = useAuthStore()
const userDetail = computed(() => authStore.userDetail)
const changing = ref(false)
const pwdForm = ref({ oldPassword: "", newPassword: "" })
const roleLabel = computed(() => ({ADMIN:"管理员",ROAD_ADMIN:"道路管理员",SANITATION:"环卫部门",TRAFFIC_POLICE:"交警部门"}[authStore.roles[0]] || authStore.roles[0]))
async function handleChangePwd() {
  changing.value = true
  try { await authApi.changePassword(pwdForm.value); ElMessage.success("密码已修改"); pwdForm.value = {oldPassword:"",newPassword:""} } catch { ElMessage.error("修改失败") }
  finally { changing.value = false }
}
</script>

<style scoped>
.up { font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,sans-serif; max-width:960px; margin:0 auto; }
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:32px; }
.page-title { font-size:22px; font-weight:600; color:#111827; margin:0 0 4px; letter-spacing:-.3px; }
.page-desc { font-size:13px; color:#9ca3af; margin:0; }

.profile-card { display:flex; background:#fff; border:1px solid #f3f4f6; border-radius:16px; padding:48px 52px; gap:48px; box-shadow:0 1px 3px rgba(0,0,0,.04),0 1px 2px rgba(0,0,0,.06); }
.profile-left { flex:1; display:flex; flex-direction:column; align-items:center; padding:12px 0; }
.avatar-ring { margin-bottom:24px; position:relative; }
.avatar-ring::after { content:""; position:absolute; inset:-4px; border-radius:50%; background:linear-gradient(135deg,#4361ee,#7c3aed,#db2777); opacity:.25; }
.avatar-bg { width:104px; height:104px; border-radius:50%; background:linear-gradient(135deg,#4361ee,#7c3aed); display:flex; align-items:center; justify-content:center; position:relative; z-index:1; }
.avatar-initials { font-size:40px; font-weight:700; color:#fff; letter-spacing:1px; }
.profile-name { font-size:22px; font-weight:700; color:#111827; margin-bottom:6px; }
.profile-role { display:inline-block; padding:4px 16px; background:#eef2ff; color:#4338ca; border-radius:20px; font-size:12px; font-weight:600; margin-bottom:28px; }
.profile-meta { display:flex; flex-direction:column; gap:10px; }
.meta-item { display:flex; align-items:center; gap:8px; font-size:13px; color:#6b7280; }

.profile-divider { width:1px; background:#f3f4f6; flex-shrink:0; }

.profile-right { flex:1; display:flex; flex-direction:column; justify-content:center; padding:12px 0 12px 8px; }
.section-head { display:flex; align-items:center; gap:10px; font-size:16px; font-weight:600; color:#111827; margin-bottom:28px; }

.form-row { margin-bottom:20px; }
.form-label { display:block; font-size:13px; font-weight:500; color:#374151; margin-bottom:6px; }
.form-input { width:100%; padding:11px 16px; border:1px solid #e5e7eb; border-radius:10px; font-size:14px; font-family:inherit; color:#111827; background:#fff; outline:none; transition:border-color .2s,box-shadow .2s; box-sizing:border-box; }
.form-input:focus { border-color:#4338ca; box-shadow:0 0 0 3px rgba(67,56,202,.1); }
.form-input::placeholder { color:#9ca3af; }

.btn-primary { padding:11px 32px; background:#4338ca; color:#fff; border:none; border-radius:10px; font-size:14px; font-weight:600; font-family:inherit; cursor:pointer; transition:all .2s; display:inline-flex; align-items:center; gap:8px; align-self:flex-start; }
.btn-primary:hover { background:#3730a3; transform:translateY(-1px); box-shadow:0 2px 8px rgba(67,56,202,.25); }
.btn-primary:disabled { background:#e5e7eb; color:#9ca3af; cursor:not-allowed; transform:none; box-shadow:none; }
.btn-loader { width:16px; height:16px; border:2px solid rgba(255,255,255,.3); border-top-color:#fff; border-radius:50%; animation:sp .5s linear infinite; display:inline-block; }
@keyframes sp { to { transform:rotate(360deg); } }

@media(max-width:768px) { .profile-card { flex-direction:column; padding:28px 24px; gap:24px; } .profile-divider { width:100%; height:1px; } }
</style>