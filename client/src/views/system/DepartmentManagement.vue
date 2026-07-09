<template>
  <div class="dm">
    <div class="page-head">
      <div class="page-head-left">
        <h2 class="page-title">部门管理</h2>
        <p class="page-desc">部门结构与人员管理</p>
      </div>
    </div>

    <div class="dm-grid">
      <div class="panel">
        <div class="panel-head">
          <span class="panel-title">部门树</span>
          <button class="btn-sm" @click="openCreateDept()">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            新增
          </button>
        </div>
        <div class="panel-body tree-body">
          <div v-for="(d,i) in deptTree" :key="i">
            <div class="tree-node" :class="{ active: selectedDept?.id === d.id }" @click="selectedDept = d">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
              <span>{{ d.name }}</span>
            </div>
            <div v-if="d.children?.length" class="tree-children">
              <div v-for="c in d.children" :key="c.id" class="tree-node tree-child-node" :class="{ active: selectedDept?.id === c.id }" @click="selectedDept = c">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
                <span>{{ c.name }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <span class="panel-title">部门列表</span>
          <div class="panel-head-right">
            <span class="count-badge">{{ depts.length }} 条</span>
            <button class="btn-sm" @click="openCreateDept()">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              新增
            </button>
          </div>
        </div>
        <div class="panel-body" style="padding:0">
          <table class="data-table">
            <thead>
              <tr>
                <th>名称</th>
                <th>编码</th>
                <th>状态</th>
                <th class="col-ops">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in depts" :key="d.id">
                <td class="cell-primary">{{ d.name }}</td>
                <td><code class="code-tag">{{ d.code }}</code></td>
                <td><span :class="['tag', d.status==='ENABLED'?'tag-green':'tag-gray']">{{ d.status==='ENABLED'?'启用':'禁用' }}</span></td>
                <td class="col-ops">
                  <button class="act-btn" @click="editDept(d)">编辑</button>
                  <button class="act-btn act-danger" @click="removeDept(d)">删除</button>
                </td>
              </tr>
              <tr v-if="depts.length===0"><td colspan="4" class="cell-empty">暂无部门数据</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showModal" class="modal-mask" @click.self="showModal=false">
        <div class="modal-card">
          <div class="modal-head">
            <span>{{ editingDept ? '编辑部门' : '新增部门' }}</span>
            <button class="modal-x" @click="showModal=false">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="form-group"><label>部门名称</label><input v-model="deptForm.name" placeholder="如：道路管理部" /></div>
              <div class="form-group"><label>部门编码</label><input v-model="deptForm.code" placeholder="如：ROAD_MGMT" /></div>
              <div class="form-group" style="grid-column:1/-1"><label>上级部门</label>
                <select v-model.number="deptForm.parentId">
                  <option :value="0">无（顶级部门）</option>
                  <option v-for="d in departments" :key="d.id" :value="d.id" :disabled="d.id===editingDept?.id">{{ d.name }}</option>
                </select>
              </div>
              <div class="form-group"><label>状态</label>
                <select v-model="deptForm.status">
                  <option value="ENABLED">启用</option>
                  <option value="DISABLED">禁用</option>
                </select>
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn-ghost" @click="showModal=false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="handleSaveDept">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { departmentApi } from "@/api"
import type { DepartmentEntity } from "@/types"

const deptTree = ref<DepartmentEntity[]>([])
const depts = ref<DepartmentEntity[]>([])
const selectedDept = ref<DepartmentEntity | null>(null)
const showModal = ref(false)
const saving = ref(false)
const editingDept = ref<DepartmentEntity | null>(null)
const deptForm = reactive({ name: "", code: "", parentId: 0, status: "ENABLED" })

async function loadData() {
  try { const r = await departmentApi.tree(); deptTree.value = r.data.data } catch {}
  try { const r = await departmentApi.list(); depts.value = r.data.data } catch {}
}
function openCreateDept() {
  editingDept.value = null
  deptForm.name = ""; deptForm.code = ""; deptForm.parentId = 0; deptForm.status = "ENABLED"
  showModal.value = true
}
function editDept(d: DepartmentEntity) {
  editingDept.value = d
  deptForm.name = d.name; deptForm.code = d.code; deptForm.parentId = d.parentId || 0; deptForm.status = d.status
  showModal.value = true
}
async function handleSaveDept() {
  if (!deptForm.name || !deptForm.code) { ElMessage.warning("请填写部门名称和编码"); return }
  saving.value = true
  try {
    if (editingDept.value) {
      await departmentApi.update(editingDept.value.id!, { name: deptForm.name, code: deptForm.code, parentId: deptForm.parentId || undefined, status: deptForm.status } as any)
      ElMessage.success("部门已更新")
    } else {
      await departmentApi.create({ name: deptForm.name, code: deptForm.code, parentId: deptForm.parentId || undefined, status: deptForm.status } as any)
      ElMessage.success("部门已创建")
    }
    showModal.value = false; await loadData()
  } catch { ElMessage.error("操作失败") }
  finally { saving.value = false }
}
async function removeDept(d: DepartmentEntity) {
  ElMessageBox.confirm("确定删除部门？", "确认删除", { confirmButtonText: "删除", cancelButtonText: "取消", type: "warning" })
    .then(async () => {
      try { await departmentApi.remove(d.id!); ElMessage.success("已删除"); await loadData() } catch { ElMessage.error("删除失败") }
    }).catch(() => {})
}
onMounted(loadData)
</script>
<style scoped>
.dm { font-family:Inter,"PingFang SC","Noto Sans SC",system-ui,sans-serif; max-width:1200px; margin:0 auto; }
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:32px; }
.page-title { font-size:22px; font-weight:600; color:#111827; margin:0 0 4px; letter-spacing:-.3px; }
.page-desc { font-size:13px; color:#9ca3af; margin:0; }
.dm-grid { display:grid; grid-template-columns:1fr 1.8fr; gap:20px; }

.panel { background:#fff; border:1px solid #f3f4f6; border-radius:12px; overflow:hidden; box-shadow:0 1px 3px rgba(0,0,0,.04); }
.panel-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f9fafb; }
.panel-head-right { display:flex; align-items:center; gap:12px; }
.panel-title { font-size:14px; font-weight:600; color:#374151; }
.panel-body { padding:12px; }
.count-badge { font-size:12px; color:#9ca3af; }
.btn-sm { display:inline-flex; align-items:center; gap:6px; padding:6px 14px; background:#4338ca; color:#fff; border:none; border-radius:8px; font-size:12px; font-weight:600; font-family:inherit; cursor:pointer; transition:all .15s; }
.btn-sm:hover { background:#3730a3; }
.tree-body { padding:8px 0; }
.tree-node { display:flex; align-items:center; gap:8px; padding:9px 14px; border-radius:8px; font-size:13px; color:#374151; cursor:pointer; transition:all .15s; margin:0 8px; }
.tree-node:hover { background:#f5f7fa; }
.tree-node.active { background:#eef2ff; color:#4338ca; font-weight:600; }
.tree-children { margin:2px 0 2px 16px; }
.tree-child-node { font-weight:400; font-size:12px; }

.data-table { width:100%; border-collapse:collapse; }
.data-table th { text-align:left; padding:12px 20px; font-size:11px; font-weight:600; color:#9ca3af; background:#fafbfc; border-bottom:1px solid #f9fafb; }
.data-table td { padding:12px 20px; font-size:13px; color:#6b7280; border-bottom:1px solid #fafbfc; }
.data-table tr:last-child td { border-bottom:none; }
.data-table tr:hover td { background:#fafbfc; }
.cell-primary { font-weight:600; color:#111827; }
.cell-empty { text-align:center; padding:40px 0 !important; color:#9ca3af; font-size:13px; }
.col-ops { width:160px; }
.code-tag { font-size:12px; color:#4338ca; background:#f5f3ff; padding:2px 8px; border-radius:4px; }
.tag { display:inline-block; padding:2px 10px; border-radius:12px; font-size:11px; font-weight:600; }
.tag-green { background:#f0fdf4; color:#16a34a; }
.tag-gray { background:#f9fafb; color:#6b7280; }
.act-btn { padding:4px 12px; background:transparent; border:1px solid #e5e7eb; border-radius:6px; font-size:11px; color:#374151; cursor:pointer; font-family:inherit; transition:all .15s; margin-right:4px; }
.act-btn:hover { border-color:#4338ca; color:#4338ca; }
.act-danger { color:#dc2626; border-color:#fecaca; }
.act-danger:hover { background:#fef2f2; border-color:#dc2626 !important; color:#dc2626 !important; }

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
.btn-primary { padding:9px 24px; background:#4338ca; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:600; font-family:inherit; cursor:pointer; transition:all .15s; }
.btn-primary:hover { background:#3730a3; }
.btn-primary:disabled { background:#e5e7eb; color:#9ca3af; cursor:not-allowed; }

@media(max-width:900px) { .dm-grid { grid-template-columns:1fr; } }
</style>