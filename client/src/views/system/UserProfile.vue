<template>
  <div class="sys-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">个人中心</h2>
        <p class="page-desc">个人信息管理与密码修改</p>
      </div>
    </div>

    <div class="profile-grid">
      <!-- Left: Profile Card -->
      <div class="content-card profile-left">
        <div class="avatar-section">
          <div class="avatar-ring">
            <div class="avatar-bg" :style="{ background: `linear-gradient(135deg, ${authStore.roleColor}, #7c3aed)` }">
              <span class="avatar-initials">{{ (authStore.realName || authStore.username || "U")?.[0]?.toUpperCase() }}</span>
            </div>
          </div>
          <div class="profile-name">{{ authStore.realName || authStore.username }}</div>
          <div class="profile-role" :style="{ background: authStore.roleColor + '15', color: authStore.roleColor }">{{ authStore.roleLabel }}</div>
        </div>

        <div class="info-section">
          <div class="info-row">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <div class="info-content">
              <span class="info-label">用户名</span>
              <span class="info-val">{{ authStore.username }}</span>
            </div>
          </div>
          <div class="info-row" v-if="userDetail?.user?.email">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
            <div class="info-content">
              <span class="info-label">邮箱</span>
              <span class="info-val">{{ userDetail.user.email }}</span>
            </div>
          </div>
          <div class="info-row" v-if="userDetail?.user?.phone">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 16.92z"/></svg>
            <div class="info-content">
              <span class="info-label">手机</span>
              <span class="info-val">{{ userDetail.user.phone }}</span>
            </div>
          </div>
          <div class="info-row" v-if="userDetail?.user?.deptName">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
            <div class="info-content">
              <span class="info-label">部门</span>
              <span class="info-val">{{ userDetail.user.deptName }}</span>
            </div>
          </div>
          <div class="info-row" v-if="userDetail?.user?.lastLoginTime">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            <div class="info-content">
              <span class="info-label">最近登录</span>
              <span class="info-val">{{ formatTime(userDetail.user.lastLoginTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Edit + Password -->
      <div class="profile-right">
        <!-- Edit Info -->
        <div class="content-card">
          <div class="toolbar"><span style="font-size:14px;font-weight:600;color:#0f172a;display:flex;align-items:center;gap:8px"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="1.5"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>个人信息</span></div>
          <div style="padding:20px">
            <div class="form-grid">
              <div class="form-group"><label>姓名</label><input v-model="profileForm.realName" placeholder="真实姓名" /></div>
              <div class="form-group"><label>邮箱</label><input v-model="profileForm.email" placeholder="电子邮箱" /></div>
              <div class="form-group"><label>手机</label><input v-model="profileForm.phone" placeholder="手机号码" /></div>
              <div class="form-group"><label>用户名</label><input :value="authStore.username" disabled /></div>
            </div>
            <div style="margin-top:16px">
              <button class="btn-primary" :disabled="savingProfile" @click="handleSaveProfile">
                {{ savingProfile ? '保存中...' : '保存信息' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Change Password -->
        <div class="content-card" style="margin-top:16px">
          <div class="toolbar"><span style="font-size:14px;font-weight:600;color:#0f172a;display:flex;align-items:center;gap:8px"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="1.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>修改密码</span></div>
          <div style="padding:20px">
            <div class="form-group" style="margin-bottom:14px"><label>原密码</label><input v-model="pwdForm.oldPassword" type="password" placeholder="输入当前密码" /></div>
            <div class="form-group" style="margin-bottom:14px"><label>新密码</label><input v-model="pwdForm.newPassword" type="password" placeholder="输入新密码（至少 6 位）" /></div>
            <div class="form-group" style="margin-bottom:16px"><label>确认新密码</label><input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" /></div>
            <div class="pwd-strength" v-if="pwdForm.newPassword">
              <div class="pwd-bar">
                <div class="pwd-bar-fill" :class="pwdStrengthClass" :style="{ width: pwdStrengthPercent + '%' }"></div>
              </div>
              <span class="pwd-strength-text" :class="pwdStrengthClass">{{ pwdStrengthText }}</span>
            </div>
            <button class="btn-primary" :disabled="changingPwd" @click="handleChangePwd" style="margin-top:12px">
              {{ changingPwd ? '修改中...' : '更新密码' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from "vue"
import { ElMessage } from "element-plus"
import { useAuthStore } from "@/stores/auth"
import { authApi, userApi } from "@/api"

const authStore = useAuthStore()
const userDetail = computed(() => authStore.userDetail)

const profileForm = reactive({
  realName: authStore.userDetail?.user?.realName || authStore.realName || "",
  email: authStore.userDetail?.user?.email || "",
  phone: authStore.userDetail?.user?.phone || "",
})
const savingProfile = ref(false)

const pwdForm = reactive({ oldPassword: "", newPassword: "", confirmPassword: "" })
const changingPwd = ref(false)

const pwdStrengthPercent = computed(() => {
  const pwd = pwdForm.newPassword
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 6) score += 25
  if (pwd.length >= 10) score += 25
  if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) score += 25
  if (/\d/.test(pwd) && /[^a-zA-Z0-9]/.test(pwd)) score += 25
  return score
})
const pwdStrengthClass = computed(() => {
  const s = pwdStrengthPercent.value
  if (s <= 25) return "pwd-weak"
  if (s <= 50) return "pwd-fair"
  if (s <= 75) return "pwd-good"
  return "pwd-strong"
})
const pwdStrengthText = computed(() => {
  const s = pwdStrengthPercent.value
  if (s <= 25) return "弱"
  if (s <= 50) return "一般"
  if (s <= 75) return "良好"
  return "强"
})

function formatTime(s?: string) {
  if (!s) return "--"
  return s.slice(0, 19).replace("T", " ")
}

async function handleSaveProfile() {
  if (!profileForm.realName) return ElMessage.warning("请输入姓名")
  savingProfile.value = true
  try {
    const userId = authStore.userId
    await userApi.update(userId, {
      realName: profileForm.realName,
      email: profileForm.email,
      phone: profileForm.phone,
    } as any)
    ElMessage.success("个人信息已更新")
    await authStore.fetchUserDetail()
  } catch { ElMessage.error("更新失败") }
  finally { savingProfile.value = false }
}

async function handleChangePwd() {
  if (!pwdForm.oldPassword) return ElMessage.warning("请输入原密码")
  if (!pwdForm.newPassword) return ElMessage.warning("请输入新密码")
  if (pwdForm.newPassword.length < 6) return ElMessage.warning("新密码至少 6 位")
  if (pwdForm.newPassword !== pwdForm.confirmPassword) return ElMessage.warning("两次密码不一致")
  changingPwd.value = true
  try {
    await authApi.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success("密码已修改")
    Object.assign(pwdForm, { oldPassword: "", newPassword: "", confirmPassword: "" })
  } catch { ElMessage.error("修改失败，请检查原密码是否正确") }
  finally { changingPwd.value = false }
}
</script>

<style scoped>
.sys-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }

.profile-grid { display:grid; grid-template-columns:360px 1fr; gap:16px; align-items:flex-start; }

.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.toolbar { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f0f2f5; }

/* Left Profile Card */
.profile-left { padding:0; }
.avatar-section { display:flex; flex-direction:column; align-items:center; padding:36px 20px 24px; border-bottom:1px solid #f0f2f5; }
.avatar-ring { margin-bottom:16px; position:relative; }
.avatar-ring::after { content:""; position:absolute; inset:-4px; border-radius:50%; background:linear-gradient(135deg,#2563eb,#7c3aed,#db2777); opacity:.2; }
.avatar-bg { width:88px; height:88px; border-radius:50%; display:flex; align-items:center; justify-content:center; position:relative; z-index:1; }
.avatar-initials { font-size:36px; font-weight:700; color:#fff; letter-spacing:1px; }
.profile-name { font-size:18px; font-weight:700; color:#0f172a; margin-bottom:8px; }
.profile-role { display:inline-block; padding:4px 14px; border-radius:20px; font-size:12px; font-weight:600; }

.info-section { padding:8px 0; }
.info-row { display:flex; align-items:center; gap:12px; padding:12px 20px; }
.info-row:hover { background:#fafbfc; }
.info-content { display:flex; flex-direction:column; gap:2px; }
.info-label { font-size:11px; color:#94a3b8; font-weight:500; }
.info-val { font-size:13px; color:#1e293b; font-weight:500; }

/* Right Side */
.profile-right { display:flex; flex-direction:column; }

.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.form-group { display:flex; flex-direction:column; gap:4px; }
.form-group label { font-size:11px; font-weight:600; color:#64748b; }
.form-group input { padding:8px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; font-family:inherit; color:#1e293b; outline:none; background:#fff; }
.form-group input:focus { border-color:#2563eb; }
.form-group input:disabled { background:#f8f9fc; color:#94a3b8; }

.btn-primary { display:inline-flex; align-items:center; gap:6px; padding:7px 16px; border:none; border-radius:6px; background:#2563eb; color:#fff; font-size:12px; font-weight:500; cursor:pointer; font-family:inherit; transition:background .15s; white-space:nowrap; }
.btn-primary:hover { background:#1d4ed8; }
.btn-primary:disabled { background:#93c5fd; cursor:not-allowed; }

.pwd-strength { display:flex; align-items:center; gap:8px; }
.pwd-bar { flex:1; height:4px; background:#f1f5f9; border-radius:2px; overflow:hidden; }
.pwd-bar-fill { height:100%; border-radius:2px; transition:width .3s; }
.pwd-weak { background:#ef4444; }
.pwd-fair { background:#f59e0b; }
.pwd-good { background:#3b82f6; }
.pwd-strong { background:#16a34a; }
.pwd-strength-text { font-size:11px; font-weight:600; min-width:24px; text-align:center; }
.pwd-strength-text.pwd-weak { color:#ef4444; }
.pwd-strength-text.pwd-fair { color:#f59e0b; }
.pwd-strength-text.pwd-good { color:#3b82f6; }
.pwd-strength-text.pwd-strong { color:#16a34a; }

@media(max-width:768px) {
  .profile-grid { grid-template-columns:1fr; }
  .form-grid { grid-template-columns:1fr; }
}
</style>
