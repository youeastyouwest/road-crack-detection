<template>
  <div class="rp">
    <div class="page-head">
      <div class="page-head-left">
        <h2 class="page-title">角色权限</h2>
        <p class="page-desc">角色管理与权限分配</p>
      </div>
      <button class="btn-primary" @click="openCreate()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新建角色
      </button>
    </div>

    <div class="panel">
      <div class="panel-head">
        <span class="panel-title">角色列表</span>
        <span class="count-badge">{{ roles.length }} 个角色</span>
      </div>
      <div class="panel-body" style="padding:0">
        <table class="data-table">
          <thead>
            <tr>
              <th>角色名称</th>
              <th>角色编码</th>
              <th>描述</th>
              <th>状态</th>
              <th class="col-ops">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in roles" :key="r.id">
              <td class="cell-primary">{{ r.name }}</td>
              <td><code class="code-tag">{{ r.code }}</code></td>
              <td style="color:#9ca3af">{{ r.description || '-' }}</td>
              <td><span :class="['tag', r.status==='ENABLED'?'tag-green':'tag-gray']">{{ r.status==='ENABLED'?'启用':'禁用' }}</span></td>
              <td class="col-ops">
                <button class="act-btn" @click="editRole(r)">编辑</button>
                <button class="act-btn act-danger" @click="removeRole(r)">删除</button>
              </td>
            </tr>
            <tr v-if="roles.length===0"><td colspan="5" class="cell-empty">暂无角色数据</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showModal" class="modal-mask" @click.self="showModal=false">
        <div class="modal-card">
          <div class="modal-head">
            <span>{{ editingRole ? '编辑角色' : '新增角色' }}</span>
            <button class="modal-x" @click="showModal=false">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="form-group"><label>角色名称</label><input v-model="roleForm.name" placeholder="如：道路巡检员" /></div>
              <div class="form-group"><label>角色编码</label><input v-model="roleForm.code" placeholder="如：ROAD_INSPECTOR" :disabled="!!editingRole" /></div>
              <div class="form-group" style="grid-column:1/-1"><label>描述</label><input v-model="roleForm.description" placeholder="角色功能描述" /></div>
              <div class="form-group"><label>状态</label>
                <select v-model="roleForm.status">
                  <option value="ENABLED">启用</option>
                  <option value="DISABLED">禁用</option>
                </select>
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn-ghost" @click="showModal=false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { roleApi } from "@/api"
import type { RoleEntity } from "@/types"

const roles = ref<RoleEntity[]>([])
const loading = ref(false)
const showModal = ref(false)
const saving = ref(false)
const editingRole = ref<RoleEntity | null>(null)
const roleForm = reactive({ name: "", code: "", description: "", status: "ENABLED" })

async function loadData() {
  loading.value = true
  try { const r = await roleApi.list(); roles.value = r.data.data } catch {}
  loading.value = false
}
function openCreateRole() {
  editingRole.value = null
  roleForm.name = ""; roleForm.code = ""; roleForm.description = ""; roleForm.status = "ENABLED"
  showModal.value = true
}
function editRole(r: RoleEntity) {
  editingRole.value = r
  roleForm.name = r.name; roleForm.code = r.code; roleForm.description = r.description || ""; roleForm.status = r.status
  showModal.value = true
}
async function handleSaveRole() {
  if (!roleForm.name || !roleForm.code) { ElMessage.warning("请填写角色名称和编码"); return }
  saving.value = true
  try {
    if (editingRole.value) {
      await roleApi.update(editingRole.value.id!, { name: roleForm.name, code: roleForm.code, description: roleForm.description, status: roleForm.status } as any)
      ElMessage.success("角色已更新")
    } else {
      await roleApi.create({ name: roleForm.name, code: roleForm.code, description: roleForm.description, status: roleForm.status } as any)
      ElMessage.success("角色已创建")
    }
    showModal.value = false; await loadData()
  } catch { ElMessage.error("操作失败") }
  finally { saving.value = false }
}
async function removeRole(r: RoleEntity) {
  ElMessageBox.confirm("确定删除角色？", "确认删除", { confirmButtonText: "删除", cancelButtonText: "取消", type: "warning" })
    .then(async () => {
      try { await roleApi.remove(r.id!); ElMessage.success("已删除"); await loadData() } catch { ElMessage.error("删除失败") }
    }).catch(() => {})
}
onMounted(loadData)
</script>
<style scoped>
.rp { font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,sans-serif; max-width:1000px; margin:0 auto; }
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:32px; }
.page-title { font-size:22px; font-weight:600; color:#111827; margin:0 0 4px; letter-spacing:-.3px; }
.page-desc { font-size:13px; color:#9ca3af; margin:0; }
.panel { background:#fff; border:1px solid #f3f4f6; border-radius:12px; overflow:hidden; box-shadow:0 1px 3px rgba(0,0,0,.04); }
.panel-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f9fafb; }
.panel-title { font-size:14px; font-weight:600; color:#374151; }
.count-badge { font-size:12px; color:#9ca3af; }
.data-table { width:100%; border-collapse:collapse; }
.data-table th { text-align:left; padding:12px 20px; font-size:11px; font-weight:600; color:#9ca3af; background:#fafbfc; border-bottom:1px solid #f9fafb; }
.data-table td { padding:12px 20px; font-size:13px; color:#6b7280; border-bottom:1px solid #fafbfc; }
.data-table tr:last-child td { border-bottom:none; }
.data-table tr:hover td { background:#fafbfc; }
.cell-primary { font-weight:600; color:#111827; }
.cell-empty { text-align:center; padding:40px 0 !important; color:#9ca3af; }
.col-ops { width:140px; }
.code-tag { font-size:12px; color:#4338ca; background:#f5f3ff; padding:2px 8px; border-radius:4px; }
.tag { display:inline-block; padding:2px 10px; border-radius:12px; font-size:11px; font-weight:600; }
.tag-green { background:#f0fdf4; color:#16a34a; }
.tag-gray { background:#f9fafb; color:#6b7280; }
.act-btn { padding:4px 12px; background:transparent; border:1px solid #e5e7eb; border-radius:6px; font-size:11px; color:#374151; cursor:pointer; font-family:inherit; transition:all .15s; margin-right:4px; }
.act-btn:hover { border-color:#4338ca; color:#4338ca; }
.act-danger { color:#dc2626; border-color:#fecaca; }
.act-danger:hover { background:#fef2f2; border-color:#dc2626 !important; color:#dc2626 !important; }
.btn-primary { display:inline-flex; align-items:center; gap:6px; padding:9px 22px; background:#4338ca; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:600; font-family:inherit; cursor:pointer; transition:all .15s; }
.btn-primary:hover { background:#3730a3; }
.btn-primary:disabled { background:#e5e7eb; color:#9ca3af; cursor:not-allowed; }
.modal-mask { position:fixed; inset:0; background:rgba(0,0,0,.3); z-index:1000; display:flex; align-items:center; justify-content:center; backdrop-filter:blur(2px); }
.modal-card { background:#fff; border-radius:14px; width:500px; max-height:80vh; overflow-y:auto; box-shadow:0 8px 40px rgba(0,0,0,.12); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:18px 24px; border-bottom:1px solid #f3f4f6; font-size:16px; font-weight:600; color:#111827; }
.modal-x { width:28px; height:28px; display:flex; align-items:center; justify-content:center; background:#f9fafb; border:none; border-radius:8px; color:#9ca3af; cursor:pointer; }
.modal-x:hover { background:#f3f4f6; color:#374151; }
.modal-body { padding:24px; }
.modal-foot { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px; border-top:1px solid #f3f4f6; }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
.form-group { display:flex; flex-direction:column; gap:6px; }
.form-group label { font-size:12px; font-weight:600; color:#374151; }
.form-group input,.form-group select { padding:9px 12px; border:1px solid #e5e7eb; border-radius:8px; font-size:13px; font-family:inherit; color:#111827; outline:none; background:#fff; transition:border-color .15s; }
.form-group input:focus,.form-group select:focus { border-color:#4338ca; box-shadow:0 0 0 3px rgba(67,56,202,.08); }
.btn-ghost { padding:9px 20px; background:#fff; color:#6b7280; border:1px solid #e5e7eb; border-radius:8px; font-size:13px; font-weight:600; font-family:inherit; cursor:pointer; transition:all .15s; }
.btn-ghost:hover { background:#f9fafb; }
</style>