<template>
  <div class="login-page">
    <!-- 视频背景 -->
    <video class="video-bg" autoplay loop muted playsinline>
      <source
        src="https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260403_050628_c4e32401-fab4-4a27-b7a8-6e9291cd5959.mp4"
        type="video/mp4"
      />
    </video>
    <div class="auth-card">
      <div class="auth-header">
        <img src="/logo.png" alt="途安智巡" class="auth-logo" />
        <h1 class="auth-title">途安智巡</h1>
        <p class="auth-subtitle">全流程道路裂缝智能检测系统</p>
      </div>

      <div class="auth-tabs">
        <button
          :class="{ active: activeTab === 'login' }"
          @click="activeTab = 'login'"
        >
          登录
        </button>
        <button
          :class="{ active: activeTab === 'register' }"
          @click="activeTab = 'register'"
        >
          注册
        </button>
      </div>

      <!-- Login Form -->
      <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label>用户名</label>
          <input
            v-model="loginForm.username"
            type="text"
            placeholder="请输入用户名"
            required
          />
        </div>
        <div class="form-group">
          <label>密码</label>
          <div class="password-wrapper">
            <input
              v-model="loginForm.password"
              :type="showLoginPwd ? 'text' : 'password'"
              placeholder="请输入密码"
              required
            />
            <span class="eye-icon" @click="showLoginPwd = !showLoginPwd">
              <el-icon><component :is="showLoginPwd ? 'View' : 'Hide'" /></el-icon>
            </span>
          </div>
        </div>
        <div class="actions">
          <a @click="activeTab = 'forgot'">忘记密码？</a>
        </div>
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? "登录中..." : "登 录" }}
        </button>
      </form>


      <!-- Register Form -->
      <form v-if="activeTab === 'register'" @submit.prevent="handleRegister" class="auth-form">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="registerForm.username" type="text" placeholder="3-32 位字母或数字" required />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <div class="input-with-btn">
            <input v-model="registerForm.email" type="email" placeholder="请输入邮箱" required />
            <button type="button" :disabled="codeSending" @click="sendCode">
              {{ codeSending ? "发送中" : codeCountdown > 0 ? `${codeCountdown}s` : "获取验证码" }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label>验证码</label>
          <input v-model="registerForm.code" type="text" placeholder="请输入验证码" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <div class="password-wrapper">
            <input
              v-model="registerForm.password"
              :type="showRegPwd ? 'text' : 'password'"
              placeholder="6-32 位密码"
              required
            />
            <span class="eye-icon" @click="showRegPwd = !showRegPwd">
              <el-icon><component :is="showRegPwd ? 'View' : 'Hide'" /></el-icon>
            </span>
          </div>
        </div>
        <div class="form-group">
          <label>姓名</label>
          <input v-model="registerForm.realName" type="text" placeholder="请输入真实姓名" />
        </div>
        <button type="submit" class="submit-btn" :disabled="regLoading">
          {{ regLoading ? "注册中..." : "注 册" }}
        </button>
        <div class="back-link" @click="activeTab = 'login'">已有账号？返回登录</div>
      </form>

      <!-- Forgot Password -->
      <form v-if="activeTab === 'forgot'" @submit.prevent="handleResetPassword" class="auth-form">
        <div class="back-link" @click="activeTab = 'login'">← 返回登录</div>
        <div class="form-group">
          <label>邮箱</label>
          <div class="input-with-btn">
            <input v-model="resetForm.email" type="email" placeholder="请输入注册邮箱" required />
            <button type="button" :disabled="codeSending" @click="sendCode">
              {{ codeSending ? "发送中" : codeCountdown > 0 ? `${codeCountdown}s` : "获取验证码" }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label>验证码</label>
          <input v-model="resetForm.code" type="text" placeholder="请输入验证码" required />
        </div>
        <div class="form-group">
          <label>新密码</label>
          <input v-model="resetForm.newPassword" type="password" placeholder="6-32 位新密码" required />
        </div>
        <button type="submit" class="submit-btn" :disabled="resetLoading">
          {{ resetLoading ? "重置中..." : "重置密码" }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue"
import { ElMessage } from "element-plus"
import { useAuthStore } from "@/stores/auth"
import { authApi } from "@/api"

const authStore = useAuthStore()
const loading = ref(false)
const regLoading = ref(false)
const resetLoading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
const showLoginPwd = ref(false)
const showRegPwd = ref(false)

const activeTab = ref<"login" | "register" | "forgot">("login")

const loginForm = reactive({ username: "", password: "" })
const registerForm = reactive({
  username: "", email: "", code: "", password: "", realName: "", phone: "",
})
const resetForm = reactive({ email: "", code: "", newPassword: "" })

async function handleLogin() {
  loading.value = true
  try {
    await authStore.login(loginForm)
    ElMessage.success("登录成功")
  } catch {
    ElMessage.error("用户名或密码错误")
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  regLoading.value = true
  try {
    await authApi.register(registerForm)
    ElMessage.success("注册成功，请登录")
    activeTab.value = "login"
  } catch {
    ElMessage.error("注册失败")
  } finally {
    regLoading.value = false
  }
}

async function handleResetPassword() {
  resetLoading.value = true
  try {
    await authApi.resetPassword(resetForm)
    ElMessage.success("密码重置成功，请登录")
    activeTab.value = "login"
  } catch {
    ElMessage.error("重置失败")
  } finally {
    resetLoading.value = false
  }
}

async function sendCode() {
  const email = activeTab.value === "register" ? registerForm.email : resetForm.email
  if (!email) {
    ElMessage.warning("请先输入邮箱")
    return
  }
  codeSending.value = true
  try {
    await authApi.sendCode(email)
    ElMessage.success("验证码已发送")
    codeCountdown.value = 60
    const timer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch {
    ElMessage.error("发送失败")
  } finally {
    codeSending.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0c0e1a 0%, #1a1d2e 40%, #141726 100%);
  position: relative;
  overflow: hidden;
}

.video-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  object-fit: cover;
  z-index: 0;
}

.login-page::before {
  content: "";
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(67, 97, 238, 0.08) 0%, transparent 70%);
  top: -200px;
  right: -200px;
}

.login-page::after {
  content: "";
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(67, 97, 238, 0.05) 0%, transparent 70%);
  bottom: -100px;
  left: -100px;
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 2.5rem 2rem;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  margin: 1rem;
}

.auth-header {
  text-align: center;
  margin-bottom: 2rem;
}

.auth-logo {
  width: 56px;
  height: 56px;
  margin-bottom: 0.75rem;
}

.auth-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #fff;
  letter-spacing: -0.02em;
  margin-bottom: 0.25rem;
}

.auth-subtitle {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 400;
}

.auth-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 1.5rem;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.auth-tabs button {
  flex: 1;
  padding: 0.6rem;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.auth-tabs button.active {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.auth-form {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.85rem;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 0.9rem;
  font-family: "Inter", sans-serif;
  color: #fff;
  transition: all 0.2s;
  outline: none;
}

.form-group input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.form-group input:focus {
  border-color: rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.1);
}

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-wrapper input {
  padding-right: 40px;
}

.eye-icon {
  position: absolute;
  right: 12px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.4);
  transition: color 0.2s;
  display: flex;
  align-items: center;
}

.eye-icon:hover {
  color: rgba(255, 255, 255, 0.8);
}

.input-with-btn {
  display: flex;
  gap: 0.75rem;
}

.input-with-btn input {
  flex: 1;
}

.input-with-btn button {
  padding: 0 1.25rem;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  font-weight: 500;
  font-size: 0.85rem;
  font-family: "Inter", sans-serif;
  transition: all 0.2s;
}

.input-with-btn button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.18);
}

.input-with-btn button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.actions {
  text-align: right;
  margin-bottom: 1.5rem;
}

.actions a,
.back-link {
  color: rgba(255, 255, 255, 0.5);
  text-decoration: none;
  font-size: 0.85rem;
  font-weight: 500;
  transition: color 0.2s;
  cursor: pointer;
}

.actions a:hover,
.back-link:hover {
  color: #fff;
}

.back-link {
  display: block;
  margin-top: 1.25rem;
  text-align: center;
}

.submit-btn {
  width: 100%;
  padding: 0.8rem 1rem;
  background: #fff;
  color: #000;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  font-family: "Inter", sans-serif;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-btn:hover {
  background: rgba(255, 255, 255, 0.9);
}

.submit-btn:active {
  transform: translateY(1px);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
