<template>
  <div class="sys-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ t("role.title") }}</h2>
        <p class="page-desc">{{ t("role.desc") }}</p>
      </div>
      <button class="btn-primary" @click="openCreate()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>{{ t("role.createRole") }}
      </button>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 7h-9M14 17H5M17 17a3 3 0 100-6 3 3 0 000 6zM7 7a3 3 0 100-6 3 3 0 000 6z"/></svg></div><div><span class="stat-val">{{ roles.length }}</span><span class="stat-lbl">{{ t("role.totalRoles") }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ enabledCount }}</span><span class="stat-lbl">{{ t("common.enabled") }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#f1f5f9;color:#64748b"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg></div><div><span class="stat-val">{{ builtinCount }}</span><span class="stat-lbl">{{ t("role.builtin") }}</span></div></div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="filter-group">
            <div class="search-wrap"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="searchKey" :placeholder="t('role.searchPlaceholder')" /></div>
          </div>
        </div>
        <span class="page-info">{{ filteredRoles.length }} {{ t("role.totalRoles") }}</span>
      </div>

      <div class="table-wrap">
        <table class="ds-table">
          <thead><tr><th>{{ t("role.name") }}</th><th>{{ t("role.code") }}</th><th>{{ t("role.description") }}</th><th>{{ t("role.type") }}</th><th>{{ t("common.status") }}</th><th style="width:160px">{{ t("common.actions") }}</th></tr></thead>
          <tbody>
            <tr v-for="r in filteredRoles" :key="r.id">
              <td class="td-title">
                <span class="role-icon" :style="{ background: roleColor(r.code) + '15', color: roleColor(r.code) }">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                </span>
                {{ r.name }}
              </td>
              <td><span class="code-tag">{{ r.code }}</span></td>
              <td class="td-muted">{{ r.description || '--' }}</td>
              <td><span :class="['status-tag', isBuiltin(r.code) ? 'stat-primary' : 'stat-info']">{{ isBuiltin(r.code) ? t("role.builtin2") : t("role.custom") }}</span></td>
              <td><span :class="['status-tag', r.status === 1 ? 'stat-success' : 'stat-cancel']">{{ r.status === 1 ? t("common.enabled") : t("common.disabled") }}</span></td>
              <td><div class="action-group">
                <button class="action-btn" @click="editRole(r)">{{ t("common.edit") }}</button>
                <button class="action-btn" v-if="!isBuiltin(r.code)" @click="toggleStatus(r)">{{ r.status === 1 ? t("common.disabled") : t("common.enabled") }}</button>
                <button class="action-btn action-danger" v-if="!isBuiltin(r.code)" @click="removeRole(r)">{{ t("common.delete") }}</button>
              </div></td>
            </tr>
            <tr v-if="filteredRoles.length === 0"><td colspan="6" class="empty-row">{{ t("role.noRoles") }}</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Permission Matrix -->
    <div class="content-card" style="margin-top:16px">
      <div class="toolbar">
        <div class="toolbar-left"><span style="font-size:14px;font-weight:600;color:#0f172a">{{ t("role.permMatrix") }}</span></div>
      </div>
      <div class="table-wrap">
        <table class="ds-table">
          <thead><tr><th>{{ t("role.module") }}</th><th v-for="r in roles" :key="r.id" style="text-align:center">{{ r.name }}</th></tr></thead>
          <tbody>
            <tr v-for="perm in permissionMatrix" :key="perm.module">
              <td class="td-title">{{ perm.module }}</td>
              <td v-for="r in roles" :key="r.id" style="text-align:center">
                <span v-if="perm.codes.includes(r.code)" style="color:#16a34a"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg></span>
                <span v-else style="color:#cbd5e1">--</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal=false">
      <div class="modal-card">
        <div class="modal-head"><span>{{ editingRole ? t("role.editRole") : t("role.newRole") }}</span><button class="modal-close" @click="showModal=false">✕</button></div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group"><label>{{ t("role.name") }}</label><input v-model="roleForm.name" :placeholder="t('role.namePlaceholder')" /></div>
            <div class="form-group"><label>{{ t("role.code") }}</label><input v-model="roleForm.code" :placeholder="t('role.codePlaceholder')" :disabled="!!editingRole" /></div>
            <div class="form-group" style="grid-column:1/-1"><label>{{ t("role.description") }}</label><input v-model="roleForm.description" :placeholder="t('role.descPlaceholder')" /></div>
            <div class="form-group"><label>{{ t("common.status") }}</label>
              <select v-model="roleForm.status">
                <option :value="1">{{ t("common.enabled") }}</option>
                <option :value="0">{{ t("common.disabled") }}</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showModal=false">{{ t("common.cancel") }}</button>
          <button class="btn-primary" :disabled="saving" @click="handleSave">{{ saving ? t("common.saving") : t("common.save") }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { roleApi } from "@/api"
import type { RoleEntity } from "@/types"
import { t } from "@/i18n"

const roles = ref<RoleEntity[]>([])
const showModal = ref(false)
const saving = ref(false)
const editingRole = ref<RoleEntity | null>(null)
const searchKey = ref("")
const roleForm = reactive({ name: "", code: "", description: "", status: 1 })

const builtinCodes = ["ROLE_ADMIN", "ROLE_ROAD_ADMIN", "ROLE_SANIT_ADMIN", "ROLE_TRAFFIC_ADMIN", "ROLE_MAINTAINER", "ROLE_CROWDSOURCE"]

const permissionMatrix = [
  { module: t("role.perm_dataScreen"), codes: ["ROLE_ADMIN", "ROLE_ROAD_ADMIN", "ROLE_SANIT_ADMIN", "ROLE_TRAFFIC_ADMIN"] },
  { module: t("role.perm_map"), codes: ["ROLE_ADMIN", "ROLE_ROAD_ADMIN", "ROLE_SANIT_ADMIN", "ROLE_TRAFFIC_ADMIN", "ROLE_MAINTAINER", "ROLE_CROWDSOURCE"] },
  { module: t("role.perm_detectionUpload"), codes: ["ROLE_ADMIN", "ROLE_ROAD_ADMIN", "ROLE_CROWDSOURCE"] },
  { module: t("role.perm_detectionResult"), codes: ["ROLE_ADMIN", "ROLE_ROAD_ADMIN", "ROLE_SANIT_ADMIN", "ROLE_TRAFFIC_ADMIN"] },
  { module: t("role.perm_workOrder"), codes: ["ROLE_ADMIN", "ROLE_ROAD_ADMIN", "ROLE_SANIT_ADMIN", "ROLE_TRAFFIC_ADMIN"] },
  { module: t("role.perm_myWorkOrder"), codes: ["ROLE_MAINTAINER"] },
  { module: t("role.perm_repairReport"), codes: ["ROLE_ADMIN", "ROLE_MAINTAINER"] },
  { module: t("role.perm_crowdReport"), codes: ["ROLE_CROWDSOURCE"] },
  { module: t("role.perm_system"), codes: ["ROLE_ADMIN"] },
  { module: t("role.perm_statistics"), codes: ["ROLE_ADMIN", "ROLE_ROAD_ADMIN"] },
]

const filteredRoles = computed(() => {
  if (!searchKey.value) return roles.value
  const key = searchKey.value.toLowerCase()
  return roles.value.filter(r => r.name.toLowerCase().includes(key) || r.code.toLowerCase().includes(key))
})
const enabledCount = computed(() => roles.value.filter(r => r.status === 1).length)
const builtinCount = computed(() => roles.value.filter(r => isBuiltin(r.code)).length)

function isBuiltin(code: string) { return builtinCodes.includes(code) }
function roleColor(code: string) {
  const map: Record<string, string> = {
    ROLE_ADMIN: "#4361ee", ROLE_ROAD_ADMIN: "#059669", ROLE_SANIT_ADMIN: "#d97706",
    ROLE_TRAFFIC_ADMIN: "#dc2626", ROLE_MAINTAINER: "#2563eb", ROLE_CROWDSOURCE: "#8b5cf6",
  }
  return map[code] || "#64748b"
}

async function loadData() {
  try {
    const r = await roleApi.list()
    roles.value = r.data.data || []
  } catch { ElMessage.error(t("role.loadFailed")) }
}

function openCreate() {
  editingRole.value = null
  Object.assign(roleForm, { name: "", code: "", description: "", status: 1 })
  showModal.value = true
}

function editRole(r: RoleEntity) {
  editingRole.value = r
  Object.assign(roleForm, {
    name: r.name,
    code: r.code,
    description: r.description || "",
    status: r.status,
  })
  showModal.value = true
}

async function handleSave() {
  if (!roleForm.name) return ElMessage.warning(t("role.needName"))
  if (!roleForm.code) return ElMessage.warning(t("role.needCode"))
  saving.value = true
  try {
    const data: any = {
      name: roleForm.name,
      code: roleForm.code,
      description: roleForm.description,
      status: roleForm.status,
    }
    if (editingRole.value) {
      await roleApi.update(editingRole.value.id!, data)
      ElMessage.success(t("common.updated"))
    } else {
      await roleApi.create(data)
      ElMessage.success(t("common.created"))
    }
    showModal.value = false
    await loadData()
  } catch { ElMessage.error(t("common.operationFailed")) }
  finally { saving.value = false }
}

async function toggleStatus(r: RoleEntity) {
  try {
    await roleApi.update(r.id!, { status: r.status === 1 ? 0 : 1 } as any)
    ElMessage.success(r.status === 1 ? t("common.disabled") : t("common.enabled"))
    await loadData()
  } catch { ElMessage.error(t("common.operationFailed")) }
}

async function removeRole(r: RoleEntity) {
  ElMessageBox.confirm(t("role.confirmDelete", { name: r.name }), t("common.confirmDelete"), { type: "warning" })
    .then(async () => {
      try {
        await roleApi.remove(r.id!)
        ElMessage.success(t("common.deleted"))
        await loadData()
      } catch { ElMessage.error(t("role.deleteWithUsers")) }
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
.search-wrap { display:flex; align-items:center; gap:6px; padding:6px 10px; background:#f8f9fc; border:1px solid #eef0f4; border-radius:8px; }
.search-wrap input { border:none; background:transparent; outline:none; font-size:12px; color:#1e293b; width:200px; font-family:inherit; }
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
.td-title { font-weight:500; display:flex; align-items:center; gap:8px; }
.td-muted { color:#94a3b8; font-size:12px; }
.code-tag { font-family:monospace; font-size:12px; color:#2563eb; font-weight:600; }
.role-icon { display:inline-flex; align-items:center; justify-content:center; width:28px; height:28px; border-radius:6px; flex-shrink:0; }

.status-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.stat-success { background:#f0fdf4; color:#16a34a; }
.stat-cancel { background:#fef2f2; color:#dc2626; }
.stat-primary { background:#eef2ff; color:#4361ee; }
.stat-info { background:#f1f5f9; color:#64748b; }

.action-group { display:flex; gap:4px; flex-wrap:wrap; }
.action-btn { padding:4px 10px; border:1px solid #e2e8f0; border-radius:5px; background:#fff; font-size:11px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.action-btn:hover { border-color:#2563eb; color:#2563eb; }
.action-danger:hover { border-color:#ef4444; color:#ef4444; }
.empty-row { text-align:center; color:#94a3b8; padding:40px 0 !important; font-size:13px; }

.page-info { font-size:12px; color:#94a3b8; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:540px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-close:hover { background:#e2e8f0; color:#475569; }
.modal-body { padding:20px; }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.form-group { display:flex; flex-direction:column; gap:4px; }
.form-group label { font-size:11px; font-weight:600; color:#64748b; }
.form-group input,.form-group select { padding:8px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; font-family:inherit; color:#1e293b; outline:none; background:#fff; }
.form-group input:focus,.form-group select:focus { border-color:#2563eb; }
.form-group input:disabled { background:#f8f9fc; color:#94a3b8; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
</style>
