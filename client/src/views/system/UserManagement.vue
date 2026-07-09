<template>
  <div class="um">
    <!-- Page Header -->
    <div class="um-top">
      <div class="um-top-left">
        <h2 class="um-title">用户管理</h2>
        <span class="um-subtitle">用户账户与权限管理</span>
      </div>
      <button class="um-create-btn" @click="openCreate()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新建用户
      </button>
    </div>

    <!-- Search Card -->
    <div class="card" style="margin-bottom:16px">
      <div class="card-head">
        <div class="card-search">
          <input class="search-input" v-model="query.username" placeholder="用户名" @keyup.enter="loadData" />
          <input class="search-input" v-model="query.realName" placeholder="姓名" @keyup.enter="loadData" />
          <button class="search-btn" @click="loadData">搜索</button>
        </div>
        <span class="card-count">{{ users.length }} 条</span>
      </div>
    </div>

    <!-- User List -->
    <div class="card">
      <div class="card-body card-body-nopad">
        <!-- Loading -->
        <div v-if="loading" class="act-loading"><div class="loader"></div></div>

        <!-- Empty -->
        <div v-else-if="users.length === 0" class="act-empty">暂无用户数据</div>

        <!-- Table Header -->
        <template v-else>
          <div class="act-header">
            <span class="act-cell act-id2">ID</span>
            <span class="act-cell act-nm">用户名</span>
            <span class="act-cell act-em">姓名</span>
            <span class="act-cell act-ph">邮箱</span>
            <span class="act-cell act-rl">角色</span>
            <span class="act-cell act-st2">状态</span>
            <span class="act-cell act-op2">操作</span>
          </div>

          <!-- Table Rows -->
          <div v-for="u in users" :key="u.id" class="act-row">
            <span class="act-cell act-id2 act-code">{{ u.id }}</span>
            <span class="act-cell act-nm" style="font-weight:500;color:#374151">{{ u.username }}</span>
            <span class="act-cell act-em">{{ u.realName || '-' }}</span>
            <span class="act-cell act-ph" style="font-size:11px;color:#6b7280">{{ u.email || '-' }}</span>
            <span class="act-cell act-rl"><span class="role-tag">{{ u.roleCode === 'ADMIN' ? '管理员' : u.roleCode === 'ROAD_ADMIN' ? '道路管理' : '巡检员' }}</span></span>
            <span class="act-cell act-st2">
              <span :class="['st-badge', u.status === 'ENABLED' ? 'st-ok' : 'st-fail']">{{ u.status === 'ENABLED' ? '启用' : '禁用' }}</span>
            </span>
            <span class="act-cell act-op2 act-actions">
              <button class="act-btn" @click="editUser(u)">编辑</button>
              <button class="act-btn" @click="toggleStatus(u)">{{ u.status === 'ENABLED' ? '禁用' : '启用' }}</button>
              <button class="act-btn act-danger" @click="handleDelete(u)">删除</button>
            </span>
          </div>
        </template>
      </div>
    </div>
  </div>

  <!-- Create/Edit User Modal -->
  <div v-if="showCreate" class="modal-overlay" @click.self="showCreate=false">
    <div class="modal-card">
      <div class="modal-head">
        <span>{{ editingUser ? '编辑用户' : '新建用户' }}</span>
        <button class="modal-close" @click="showCreate=false">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="modal-body">
        <div class="form-grid">
          <div class="form-group"><label>用户名</label><input v-model="userForm.username" placeholder="登录用户名" :disabled="!!editingUser" /></div>
          <div class="form-group" v-if="!editingUser"><label>密码</label><input v-model="userForm.password" type="password" placeholder="初始密码" /></div>
          <div class="form-group"><label>姓名</label><input v-model="userForm.realName" placeholder="真实姓名" /></div>
          <div class="form-group"><label>邮箱</label><input v-model="userForm.email" placeholder="电子邮箱" /></div>
          <div class="form-group"><label>手机</label><input v-model="userForm.phone" placeholder="手机号码" /></div>
          <div class="form-group"><label>角色</label>
            <select v-model="userForm.roleCode">
              <option value="ADMIN">管理员</option>
              <option value="ROAD_ADMIN">道路管理</option>
              <option value="SANITATION">环卫</option>
              <option value="TRAFFIC_POLICE">交警</option>
            </select>
          </div>
        </div>
      </div>
      <div class="modal-foot">
        <button class="modal-btn modal-btn-ghost" @click="showCreate=false">取消</button>
        <button class="modal-btn modal-btn-primary" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存' }}</button>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { userApi } from "@/api"
import type { UserEntity } from "@/types"

const users = ref<UserEntity[]>([])
const loading = ref(false)
const showCreate = ref(false)
const saving = ref(false)
const editingUser = ref<UserEntity | null>(null)
const query = reactive({ username: "", realName: "" })
const userForm = reactive({ username: "", password: "", realName: "", email: "", phone: "", roleCode: "ROAD_ADMIN" })

async function loadData() {
  loading.value = true
  try { const r = await userApi.page({ page: 1, size: 20, username: query.username || undefined, realName: query.realName || undefined }); users.value = r.data.data.records } catch {}
  loading.value = false
}
function editUser(row: UserEntity) {
  editingUser.value = row
  userForm.username = row.username
  userForm.realName = row.realName || ""
  userForm.email = row.email || ""
  userForm.phone = row.phone || ""
  userForm.roleCode = row.roleCode || "ROAD_ADMIN"
  showCreate.value = true
}
function openCreate() {
  editingUser.value = null
  userForm.username = ""; userForm.password = ""; userForm.realName = ""; userForm.email = ""; userForm.phone = ""; userForm.roleCode = "ROAD_ADMIN"
  showCreate.value = true
}
async function handleSave() {
  if (!userForm.username) { ElMessage.warning("请输入用户名"); return }
  saving.value = true
  try {
    if (editingUser.value) {
      await userApi.update(editingUser.value.id!, { username: userForm.username, realName: userForm.realName, email: userForm.email, phone: userForm.phone, roleCode: userForm.roleCode } as any)
      ElMessage.success("用户已更新")
    } else {
      if (!userForm.password) { ElMessage.warning("请输入密码"); return }
      await userApi.create({ username: userForm.username, password: userForm.password, realName: userForm.realName, email: userForm.email, phone: userForm.phone, roleCode: userForm.roleCode } as any)
      ElMessage.success("用户已创建")
    }
    showCreate.value = false; await loadData()
  } catch { ElMessage.error("操作失败") }
  finally { saving.value = false }
}
async function toggleStatus(row: UserEntity) {
  if (!row.id) return
  try { await userApi.toggleStatus(row.id, row.status === "ENABLED" ? 0 : 1); await loadData() } catch {}
}
async function handleDelete(row: UserEntity) {
  if (!row.id) return
  try { await userApi.remove(row.id); ElMessage.success("已删除"); await loadData() } catch { ElMessage.error("删除失败") }
}
onMounted(loadData)
</script>

<style scoped>
.um { font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,sans-serif; }
.um-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:28px; }
.um-top-left { display:flex; align-items:baseline; gap:12px; }
.um-title { font-size:20px; font-weight:600; color:#111827; margin:0; }
.um-subtitle { font-size:13px; color:#9ca3af; }
.um-create-btn { display:flex; align-items:center; gap:6px; padding:8px 18px; background:#4338ca; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:600; font-family:inherit; cursor:pointer; transition:background .15s; }
.um-create-btn:hover { background:#3730a3; }

.card { background:#fff; border:1px solid #f3f4f6; border-radius:12px; overflow:hidden; }
.card-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f9fafb; }
.card-search { display:flex; gap:8px; align-items:center; }
.search-input { padding:7px 12px; border:1px solid #e5e7eb; border-radius:8px; font-size:12px; font-family:inherit; color:#111827; outline:none; width:140px; }
.search-input:focus { border-color:#4338ca; }
.search-btn { padding:7px 16px; background:#4338ca; color:#fff; border:none; border-radius:8px; font-size:12px; font-weight:600; font-family:inherit; cursor:pointer; }
.search-btn:hover { background:#3730a3; }
.card-count { font-size:12px; color:#9ca3af; }
.card-body-nopad { padding:0; }

.act-header { display:flex; padding:10px 20px; border-bottom:1px solid #f9fafb; background:#fafbfc; }
.act-cell { font-size:11px; color:#9ca3af; }
.act-id2 { width:100px; flex-shrink:0; }
.act-nm { width:80px; flex-shrink:0; }
.act-em { flex:1; }
.act-ph { width:120px; flex-shrink:0; }
.act-rl { width:90px; flex-shrink:0; }
.act-st2 { width:60px; flex-shrink:0; }
.act-op2 { width:120px; flex-shrink:0; }
.act-row { display:flex; padding:12px 20px; border-bottom:1px solid #f9fafb; align-items:center; }
.act-row:last-child { border-bottom:none; }
.act-row:hover { background:#fafbfc; }
.act-code { font-size:12px; font-weight:600; color:#4338ca; }
.act-actions { display:flex; gap:4px; }
.act-btn { padding:3px 10px; background:transparent; border:1px solid #e5e7eb; border-radius:6px; font-size:11px; color:#374151; cursor:pointer; font-family:inherit; transition:all .15s; }
.act-btn:hover { border-color:#4338ca; color:#4338ca; }
.role-tag { display:inline-block; padding:1px 7px; background:#eef2ff; color:#4338ca; border-radius:4px; font-size:10px; font-weight:600; }
.st-badge { display:inline-block; padding:1px 7px; border-radius:4px; font-size:10px; font-weight:600; }
.st-ok { background:#f0fdf4; color:#22c55e; }
.st-fail { background:#fef2f2; color:#ef4444; }
.act-loading { display:flex; justify-content:center; padding:40px 0; }
.loader { width:20px; height:20px; border:2px solid #f3f4f6; border-top-color:#4338ca; border-radius:50%; animation:spin .5s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
.act-empty { text-align:center; padding:48px 0; color:#9ca3af; font-size:13px; }
</style>