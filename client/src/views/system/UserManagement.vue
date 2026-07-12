<template>
  <div class="sys-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">管理用户账户、角色分配与启用状态</p>
      </div>
      <button class="btn-primary" @click="openCreate()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>新建用户
      </button>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg></div><div><span class="stat-val">{{ users.length }}</span><span class="stat-lbl">总用户数</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ enabledCount }}</span><span class="stat-lbl">已启用</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef2f2;color:#dc2626"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></div><div><span class="stat-val">{{ disabledCount }}</span><span class="stat-lbl">已禁用</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fce7f3;color:#e11d48"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" y1="8" x2="19" y2="14"/><line x1="22" y1="11" x2="16" y2="11"/></svg></div><div><span class="stat-val">{{ adminCount }}</span><span class="stat-lbl">管理员数</span></div></div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="filter-group">
            <select v-model="query.roleCode" class="filter-select" @change="loadData">
              <option value="">全部角色</option>
              <option v-for="r in roleOptions" :key="r.code" :value="r.code">{{ r.label }}</option>
            </select>
            <select v-model="query.status" class="filter-select" @change="loadData">
              <option value="">全部状态</option>
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </select>
            <div class="search-wrap"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="query.username" placeholder="搜索用户名..." @keyup.enter="loadData" /></div>
            <div class="search-wrap"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="query.realName" placeholder="搜索姓名..." @keyup.enter="loadData" /></div>
            <button class="btn-ghost" @click="loadData">搜索</button>
          </div>
        </div>
      </div>

      <div class="table-wrap">
        <table class="ds-table">
          <thead><tr><th>ID</th><th>用户名</th><th>姓名</th><th>邮箱</th><th>手机</th><th>角色</th><th>状态</th><th style="width:200px">操作</th></tr></thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td><span class="code-tag">{{ u.id }}</span></td>
              <td class="td-title">{{ u.username }}</td>
              <td>{{ u.realName || '--' }}</td>
              <td class="td-muted">{{ u.email || '--' }}</td>
              <td class="td-muted">{{ u.phone || '--' }}</td>
              <td><span :class="['role-badge', roleBadgeClass(u.roleCode)]">{{ roleLabel(u.roleCode) }}</span></td>
              <td><span :class="['status-tag', u.status === 1 ? 'stat-success' : 'stat-cancel']">{{ u.status === 1 ? '启用' : '禁用' }}</span></td>
              <td><div class="action-group">
                <button class="action-btn" @click="editUser(u)">编辑</button>
                <button class="action-btn" @click="toggleStatus(u)">{{ u.status === 1 ? '禁用' : '启用' }}</button>
                <button class="action-btn" @click="resetPwd(u)">重置密码</button>
                <button class="action-btn action-danger" @click="handleDelete(u)">删除</button>
              </div></td>
            </tr>
            <tr v-if="!loading && users.length === 0"><td colspan="8" class="empty-row">暂无用户数据</td></tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span class="page-info">共 {{ total }} 条</span>
        <div class="page-btns">
          <button class="page-btn" :disabled="page<=1" @click="page--;loadData()">上一页</button>
          <span class="page-cur">{{ page }}</span>
          <button class="page-btn" :disabled="page*20>=total" @click="page++;loadData()">下一页</button>
        </div>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate=false">
      <div class="modal-card">
        <div class="modal-head"><span>{{ editingUser ? '编辑用户' : '新建用户' }}</span><button class="modal-close" @click="showCreate=false">✕</button></div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group"><label>用户名</label><input v-model="userForm.username" placeholder="登录用户名" :disabled="!!editingUser" /></div>
            <div class="form-group" v-if="!editingUser"><label>密码</label><input v-model="userForm.password" type="password" placeholder="初始密码" /></div>
            <div class="form-group"><label>姓名</label><input v-model="userForm.realName" placeholder="真实姓名" /></div>
            <div class="form-group"><label>邮箱</label><input v-model="userForm.email" placeholder="电子邮箱" /></div>
            <div class="form-group"><label>手机</label><input v-model="userForm.phone" placeholder="手机号码" /></div>
            <div class="form-group"><label>角色</label>
              <select v-model="userForm.roleCode">
                <option v-for="r in roleOptions" :key="r.code" :value="r.code">{{ r.label }}</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showCreate=false">取消</button>
          <button class="btn-primary" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { userApi } from "@/api"
import type { UserEntity } from "@/types"

const users = ref<UserEntity[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const showCreate = ref(false)
const saving = ref(false)
const editingUser = ref<UserEntity | null>(null)
const query = reactive({ username: "", realName: "", roleCode: "", status: "" as string })
const userForm = reactive({ username: "", password: "", realName: "", email: "", phone: "", roleCode: "ROLE_ROAD_ADMIN" })

const roleOptions = [
  { id: 1, code: "ROLE_ADMIN", label: "超级管理员" },
  { id: 2, code: "ROLE_ROAD_ADMIN", label: "道路管理员" },
  { id: 3, code: "ROLE_SANIT_ADMIN", label: "环卫管理员" },
  { id: 4, code: "ROLE_INSPECTOR", label: "巡检员" },
  { id: 5, code: "ROLE_MAINTAINER", label: "维修工" },
  { id: 6, code: "ROLE_VIEWER", label: "查看员" },
]

const enabledCount = computed(() => users.value.filter(u => u.status === 1).length)
const disabledCount = computed(() => users.value.filter(u => u.status === 0).length)
const adminCount = computed(() => users.value.filter(u => u.roleCode === "ROLE_ADMIN").length)

function roleLabel(code?: string) {
  const r = roleOptions.find(r => r.code === code)
  return r ? r.label : code || "--"
}
function roleBadgeClass(code?: string) {
  const map: Record<string, string> = {
    ROLE_ADMIN: "role-admin",
    ROLE_ROAD_ADMIN: "role-road",
    ROLE_SANIT_ADMIN: "role-sanit",
    ROLE_TRAFFIC_ADMIN: "role-traffic",
    ROLE_MAINTAINER: "role-maintainer",
    ROLE_CROWDSOURCE: "role-crowd",
  }
  return map[code || ""] || "role-default"
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: 20 }
    if (query.username) params.username = query.username
    if (query.realName) params.realName = query.realName
    if (query.status !== "") params.status = Number(query.status)
    const r = await userApi.page(params)
    users.value = r.data.data.records
    total.value = r.data.data.total
  } catch { ElMessage.error("加载用户列表失败") }
  loading.value = false
}

function openCreate() {
  editingUser.value = null
  Object.assign(userForm, { username: "", password: "", realName: "", email: "", phone: "", roleCode: "ROLE_ROAD_ADMIN" })
  showCreate.value = true
}

function editUser(row: UserEntity) {
  editingUser.value = row
  Object.assign(userForm, {
    username: row.username,
    password: "",
    realName: row.realName || "",
    email: row.email || "",
    phone: row.phone || "",
    roleCode: row.roleCode || "ROLE_ROAD_ADMIN",
  })
  showCreate.value = true
}

async function handleSave() {
  if (!userForm.username) return ElMessage.warning("请输入用户名")
  if (!editingUser.value && !userForm.password) return ElMessage.warning("请输入密码")
  saving.value = true
  try {
    const data: any = {
      username: userForm.username,
      realName: userForm.realName,
      email: userForm.email,
      phone: userForm.phone,
    }
    if (!editingUser.value) data.password = userForm.password
    const role = roleOptions.find(r => r.code === userForm.roleCode)
    if (editingUser.value) {
      await userApi.update(editingUser.value.id!, data, role ? [role.id] : undefined)
      ElMessage.success("用户已更新")
    } else {
      await userApi.create(data, role ? [role.id] : undefined)
      ElMessage.success("用户已创建")
    }
    showCreate.value = false
    await loadData()
  } catch { ElMessage.error("操作失败") }
  finally { saving.value = false }
}

async function toggleStatus(row: UserEntity) {
  if (!row.id) return
  try {
    await userApi.toggleStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? "已禁用" : "已启用")
    await loadData()
  } catch { ElMessage.error("操作失败") }
}

async function resetPwd(row: UserEntity) {
  ElMessageBox.confirm(`确认重置用户「${row.username}」的密码为默认密码？`, "重置密码", { type: "warning" })
    .then(async () => {
      try {
        await userApi.resetPassword(row.id!)
        ElMessage.success("密码已重置")
      } catch { ElMessage.error("重置失败") }
    }).catch(() => {})
}

async function handleDelete(row: UserEntity) {
  if (!row.id) return
  ElMessageBox.confirm(`确认删除用户「${row.username}」？此操作不可逆。`, "确认删除", { type: "warning" })
    .then(async () => {
      try {
        await userApi.remove(row.id!)
        ElMessage.success("已删除")
        await loadData()
      } catch { ElMessage.error("删除失败") }
    }).catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.sys-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }

.stat-row { display:flex; gap:12px; margin-bottom:16px; }
.stat-card { flex:1; display:flex; align-items:center; gap:14px; padding:14px 18px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.stat-icon { width:38px; height:38px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.stat-val { font-size:22px; font-weight:800; color:#0f172a; line-height:1; }
.stat-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }

.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.toolbar { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f0f2f5; gap:12px; }
.toolbar-left { flex:1; }
.filter-group { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.filter-select { padding:6px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; color:#334155; background:#fff; font-family:inherit; outline:none; cursor:pointer; }
.filter-select:focus { border-color:#2563eb; }
.search-wrap { display:flex; align-items:center; gap:6px; padding:6px 10px; background:#f8f9fc; border:1px solid #eef0f4; border-radius:8px; }
.search-wrap input { border:none; background:transparent; outline:none; font-size:12px; color:#1e293b; width:130px; font-family:inherit; }
.search-wrap input::placeholder { color:#94a3b8; }
.btn-ghost { display:inline-flex; align-items:center; gap:4px; padding:6px 14px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.btn-primary { display:inline-flex; align-items:center; gap:6px; padding:7px 16px; border:none; border-radius:6px; background:#2563eb; color:#fff; font-size:12px; font-weight:500; cursor:pointer; font-family:inherit; transition:background .15s; white-space:nowrap; }
.btn-primary:hover { background:#1d4ed8; }
.btn-primary:disabled { background:#93c5fd; cursor:not-allowed; }

.table-wrap { overflow-x:auto; }
.ds-table { width:100%; border-collapse:collapse; font-size:13px; }
.ds-table thead { background:#f8f9fc; }
.ds-table th { padding:10px 14px; text-align:left; font-weight:600; font-size:11px; color:#64748b; letter-spacing:0.03em; border-bottom:1px solid #eef0f4; }
.ds-table td { padding:10px 14px; border-bottom:1px solid #f5f6f8; color:#1e293b; }
.ds-table tbody tr:hover { background:#fafbfc; }
.td-title { font-weight:500; }
.td-muted { color:#94a3b8; font-size:12px; }
.code-tag { font-family:monospace; font-size:12px; color:#2563eb; font-weight:600; }

.role-badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.role-admin { background:#eef2ff; color:#4361ee; }
.role-road { background:#f0fdf4; color:#059669; }
.role-sanit { background:#fffbeb; color:#d97706; }
.role-traffic { background:#fef2f2; color:#dc2626; }
.role-maintainer { background:#eff6ff; color:#2563eb; }
.role-crowd { background:#faf5ff; color:#8b5cf6; }
.role-default { background:#f1f5f9; color:#64748b; }

.status-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.stat-success { background:#f0fdf4; color:#16a34a; }
.stat-cancel { background:#fef2f2; color:#dc2626; }
.stat-info { background:#f1f5f9; color:#64748b; }

.action-group { display:flex; gap:4px; flex-wrap:wrap; }
.action-btn { padding:4px 10px; border:1px solid #e2e8f0; border-radius:5px; background:#fff; font-size:11px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.action-btn:hover { border-color:#2563eb; color:#2563eb; }
.action-danger:hover { border-color:#ef4444; color:#ef4444; }
.empty-row { text-align:center; color:#94a3b8; padding:40px 0 !important; font-size:13px; }

.pagination { display:flex; align-items:center; justify-content:space-between; padding:12px 18px; border-top:1px solid #f0f2f5; }
.page-info { font-size:12px; color:#94a3b8; }
.page-btns { display:flex; align-items:center; gap:6px; }
.page-btn { padding:4px 10px; border:1px solid #e2e8f0; border-radius:5px; background:#fff; font-size:11px; color:#475569; cursor:pointer; font-family:inherit; }
.page-btn:hover:not(:disabled) { border-color:#2563eb; color:#2563eb; }
.page-btn:disabled { opacity:.4; cursor:not-allowed; }
.page-cur { font-size:12px; font-weight:600; color:#2563eb; min-width:20px; text-align:center; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:540px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-close:hover { background:#e2e8f0; color:#475569; }
.modal-body { padding:20px; }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.form-group { display:flex; flex-direction:column; gap:4px; }
.form-group label { font-size:11px; font-weight:600; color:#64748b; }
.form-group input,.form-group select,.form-group textarea { padding:8px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; font-family:inherit; color:#1e293b; outline:none; background:#fff; }
.form-group input:focus,.form-group select:focus,.form-group textarea:focus { border-color:#2563eb; }
.form-group input:disabled { background:#f8f9fc; color:#94a3b8; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
</style>
