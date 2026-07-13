<template>
  <div class="sys-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ t('dept.title') }}</h2>
        <p class="page-desc">{{ t('dept.desc') }}</p>
      </div>
      <button class="btn-primary" @click="openCreateDept()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>{{ t('dept.createDept') }}
      </button>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg></div><div><span class="stat-val">{{ depts.length }}</span><span class="stat-lbl">{{ t('dept.totalDepts') }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ enabledCount }}</span><span class="stat-lbl">{{ t('common.enabled') }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef2f2;color:#dc2626"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></div><div><span class="stat-val">{{ disabledCount }}</span><span class="stat-lbl">{{ t('common.disabled') }}</span></div></div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="filter-group">
            <div class="search-wrap"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="searchKey" :placeholder="t('dept.searchPlaceholder')" /></div>
            <button class="btn-ghost" @click="searchKey=''">{{ t('common.reset') }}</button>
          </div>
        </div>
        <span class="page-info">{{ t('common.total', { count: filteredDepts.length }) }}</span>
      </div>

      <div class="table-wrap">
        <table class="ds-table">
          <thead><tr><th>{{ t('dept.name') }}</th><th>{{ t('dept.code') }}</th><th>{{ t('common.description') }}</th><th>{{ t('dept.sortOrder') }}</th><th>{{ t('common.status') }}</th><th>{{ t('dept.createdAt') }}</th><th style="width:160px">{{ t('common.actions') }}</th></tr></thead>
          <tbody>
            <tr v-for="d in filteredDepts" :key="d.id">
              <td class="td-title">{{ d.name }}</td>
              <td><span class="code-tag">{{ d.code }}</span></td>
              <td class="td-muted">{{ d.description || '--' }}</td>
              <td class="td-muted">{{ d.sortOrder ?? 0 }}</td>
              <td><span :class="['status-tag', d.status === 1 ? 'stat-success' : 'stat-cancel']">{{ d.status === 1 ? t('common.enabled') : t('common.disabled') }}</span></td>
              <td class="td-muted">{{ d.createdAt || '--' }}</td>
              <td><div class="action-group">
                <button class="action-btn" @click="editDept(d)">{{ t('common.edit') }}</button>
                <button class="action-btn action-danger" @click="removeDept(d)">{{ t('common.delete') }}</button>
              </div></td>
            </tr>
            <tr v-if="filteredDepts.length === 0"><td colspan="7" class="empty-row">{{ t('dept.noDepts') }}</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal=false">
      <div class="modal-card">
        <div class="modal-head"><span>{{ editingDept ? t('dept.editDept') : t('dept.newDept') }}</span><button class="modal-close" @click="showModal=false">✕</button></div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group"><label>{{ t('dept.name') }}</label><input v-model="deptForm.name" :placeholder="t('dept.namePlaceholder')" /></div>
            <div class="form-group"><label>{{ t('dept.code') }}</label><input v-model="deptForm.code" :placeholder="t('dept.codePlaceholder')" :disabled="!!editingDept" /></div>
            <div class="form-group"><label>{{ t('dept.sortOrder') }}</label><input v-model.number="deptForm.sortOrder" type="number" min="0" placeholder="0" /></div>
            <div class="form-group"><label>{{ t('common.status') }}</label>
              <select v-model="deptForm.status">
                <option :value="1">{{ t('common.enabled') }}</option>
                <option :value="0">{{ t('common.disabled') }}</option>
              </select>
            </div>
            <div class="form-group" style="grid-column:1/-1"><label>{{ t('common.description') }}</label><input v-model="deptForm.description" :placeholder="t('dept.descPlaceholder')" /></div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showModal=false">{{ t('common.cancel') }}</button>
          <button class="btn-primary" :disabled="saving" @click="handleSaveDept">{{ saving ? t('common.saving') : t('common.save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { departmentApi } from "@/api"
import type { DepartmentEntity } from "@/types"
import { t } from "@/i18n"

const depts = ref<DepartmentEntity[]>([])
const showModal = ref(false)
const saving = ref(false)
const editingDept = ref<DepartmentEntity | null>(null)
const searchKey = ref("")
const deptForm = reactive({ name: "", code: "", description: "", sortOrder: 0, status: 1 })

const filteredDepts = computed(() => {
  if (!searchKey.value) return depts.value
  const key = searchKey.value.toLowerCase()
  return depts.value.filter(d => d.name.toLowerCase().includes(key) || d.code.toLowerCase().includes(key))
})

const enabledCount = computed(() => depts.value.filter(d => d.status === 1).length)
const disabledCount = computed(() => depts.value.filter(d => d.status === 0).length)

async function loadData() {
  try {
    const r = await departmentApi.list()
    depts.value = r.data.data || []
  } catch { ElMessage.error(t("dept.loadFailed")) }
}

function openCreateDept() {
  editingDept.value = null
  Object.assign(deptForm, { name: "", code: "", description: "", sortOrder: 0, status: 1 })
  showModal.value = true
}

function editDept(d: DepartmentEntity) {
  editingDept.value = d
  Object.assign(deptForm, {
    name: d.name,
    code: d.code,
    description: d.description || "",
    sortOrder: d.sortOrder ?? 0,
    status: d.status,
  })
  showModal.value = true
}

async function handleSaveDept() {
  if (!deptForm.name) return ElMessage.warning(t("dept.needName"))
  if (!deptForm.code) return ElMessage.warning(t("dept.needCode"))
  saving.value = true
  try {
    const data: any = {
      name: deptForm.name,
      code: deptForm.code,
      description: deptForm.description,
      sortOrder: deptForm.sortOrder,
      status: deptForm.status,
    }
    if (editingDept.value) {
      await departmentApi.update(editingDept.value.id!, data)
      ElMessage.success(t("common.updated"))
    } else {
      await departmentApi.create(data)
      ElMessage.success(t("common.created"))
    }
    showModal.value = false
    await loadData()
  } catch { ElMessage.error(t("common.operationFailed")) }
  finally { saving.value = false }
}

async function removeDept(d: DepartmentEntity) {
  ElMessageBox.confirm(t("dept.confirmDelete", { name: d.name }), t("common.warning"), { type: "warning" })
    .then(async () => {
      try {
        await departmentApi.remove(d.id!)
        ElMessage.success(t("common.deleted"))
        await loadData()
      } catch { ElMessage.error(t("dept.deleteWithUsers")) }
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
.search-wrap input { border:none; background:transparent; outline:none; font-size:12px; color:#1e293b; width:180px; font-family:inherit; }
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

.status-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.stat-success { background:#f0fdf4; color:#16a34a; }
.stat-cancel { background:#fef2f2; color:#dc2626; }

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
