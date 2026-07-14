<template>
  <div class="rha-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ t("road.healthArchive") }}</h2>
        <p class="page-desc">{{ t("road.desc") }}</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="showGenerate=true">{{ t("road.generate") }}</button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#eef2ff,#dbeafe);color:#2563eb"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg></div><div><span class="stat-val">{{ dashboard.totalRoads }}</span><span class="stat-lbl">{{ t("road.totalRoads") }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#dcfce7,#bbf7d0);color:#16a34a"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg></div><div><span class="stat-val">{{ dashboard.healthyRoads }}</span><span class="stat-lbl">{{ t("road.healthy") }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#fef3c7,#fde68a);color:#d97706"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></div><div><span class="stat-val">{{ dashboard.subHealthyRoads }}</span><span class="stat-lbl">{{ t("road.subHealthy") }}</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#fce7f3,#fbcfe8);color:#e11d48"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg></div><div><span class="stat-val">{{ dashboard.averageHealthScore?.toFixed(1) ?? '0.0' }}</span><span class="stat-lbl">{{ t("road.avgScore") }}</span></div></div>
    </div>

    <div class="content-card">
      <div class="card-head">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        路段健康档案
        <div style="margin-left:auto;display:flex;gap:8px;align-items:center">
          <select v-model="filterLevel" class="filter-select"><option value="">{{ t("road.allLevels") }}</option><option value="HEALTHY">{{ t("road.healthStatus") }}</option><option value="SUB_HEALTHY">{{ t("road.needAttentionStatus") }}</option><option value="UNHEALTHY">{{ t("road.damageStatus") }}</option></select>
        </div>
      </div>
      <div class="table-wrap">
        <table class="ds-table">
          <thead><tr><th>{{ t("road.roadCode") }}</th><th>{{ t("road.archiveDate") }}</th><th>{{ t("road.healthScore") }}</th><th>{{ t("road.damageLevel") }}</th><th>{{ t("road.detectionCount") }}</th><th>{{ t("road.totalDamages") }}</th><th>{{ t("road.severeDamages") }}</th><th>{{ t("common.status") }}</th><th>{{ t("common.actions") }}</th></tr></thead>
          <tbody>
            <tr v-for="a in filteredArchives" :key="a.id" @click="viewDetail(a)" style="cursor:pointer">
              <td class="td-primary">{{ a.road?.roadName || '道路 #' + a.roadId }}</td>
              <td class="td-muted">{{ a.archiveDate }}</td>
              <td>
                <div class="score-row">
                  <span :class="['score-val', scoreCls(a.healthScore)]">{{ a.healthScore ?? '--' }}</span>
                  <div class="score-bar"><div class="score-fill" :style="{width:(a.healthScore||0)+'%',background:scoreColor(a.healthScore)}"></div></div>
                </div>
              </td>
              <td><span :class="['sev-badge', damageLevelClass(a.damageLevel)]">{{ damageLevelLabel(a.damageLevel) }}</span></td>
              <td>{{ a.totalDetectionCount ?? 0 }}</td>
              <td>{{ a.totalDamageCount ?? 0 }}</td>
              <td><span :class="{'td-danger': (a.severityHighCount ?? 0) > 0}">{{ a.severityHighCount ?? 0 }}</span></td>
              <td><span :class="['stat-tag', (a.healthScore ?? 0) >= 80 ? 'tag-green' : (a.healthScore ?? 0) >= 60 ? 'tag-yellow' : 'tag-red']">{{ (a.healthScore ?? 0) >= 80 ? t("road.healthStatus") : (a.healthScore ?? 0) >= 60 ? t("road.needAttentionStatus") : t("road.damageStatus") }}</span></td>
              <td><button class="action-btn" @click.stop="viewDetail(a)">{{ t("common.detail") }}</button></td>
            </tr>
            <tr v-if="!loading && filteredArchives.length === 0"><td colspan="9" class="empty-row">{{ t("road.noArchiveData") }}</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="insight-row">
      <div class="insight-card"><div class="insight-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></div><div><span class="insight-val">{{ dashboard.archivedRoads }}/{{ dashboard.totalRoads }}</span><span class="insight-lbl">{{ t("road.archivedRatio") }}</span></div></div>
      <div class="insight-card"><div class="insight-icon" style="background:#fef3c7;color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div><span class="insight-val">{{ dashboard.unhealthyRoads }}</span><span class="insight-lbl">{{ t("road.priorityRepair") }}</span></div></div>
      <div class="insight-card"><div class="insight-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg></div><div><span class="insight-val">{{ dashboard.totalRoads > 0 ? ((dashboard.healthyRoads / dashboard.totalRoads) * 100).toFixed(0) + '%' : '0%' }}</span><span class="insight-lbl">{{ t("road.qualifiedRatio") }}</span></div></div>
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail=false">
      <div class="modal-card">
        <div class="modal-head"><span>{{ t("road.archiveDetail") }}{{ detailTarget?.road?.roadName || '道路 #' + detailTarget?.roadId }}</span><button class="modal-close" @click="showDetail=false">✕</button></div>
        <div class="modal-body">
          <div v-if="detailLoading" style="text-align:center;padding:30px;color:#94a3b8">{{ t("common.loading") }}</div>
          <template v-else>
            <div class="detail-grid">
              <div class="detail-item"><label>{{ t("road.roadCode") }}</label><span>{{ detailTarget?.road?.roadName || '--' }}</span></div>
              <div class="detail-item"><label>{{ t("road.archiveDate") }}</label><span>{{ detailTarget?.archiveDate || '--' }}</span></div>
              <div class="detail-item"><label>{{ t("road.healthScore") }}</label><span :style="{color: scoreColor(detailTarget?.healthScore), fontWeight:700}">{{ detailTarget?.healthScore ?? '--' }}</span></div>
              <div class="detail-item"><label>{{ t("road.damageLevel") }}</label><span>{{ damageLevelLabel(detailTarget?.damageLevel) }}</span></div>
              <div class="detail-item"><label>{{ t("road.totalDetections") }}</label><span>{{ detailTarget?.totalDetectionCount ?? 0 }}{{ t("road.totalSpots") }}</span></div>
              <div class="detail-item"><label>{{ t("road.totalDamagesCount") }}</label><span>{{ detailTarget?.totalDamageCount ?? 0 }}{{ t("road.totalSpots2") }}</span></div>
            </div>
            <div style="margin-top:16px;border-top:1px solid #f0f2f5;padding-top:14px">
              <label class="detail-label">{{ t("road.damageCategoryStats") }}</label>
              <div class="disease-stats">
                <div class="ds-item"><span class="ds-label">{{ t("damage.crack") }}</span><span class="ds-val">{{ detailTarget?.crackCount ?? 0 }}</span></div>
                <div class="ds-item"><span class="ds-label">{{ t("damage.pothole") }}</span><span class="ds-val">{{ detailTarget?.potholeCount ?? 0 }}</span></div>
                <div class="ds-item"><span class="ds-label">{{ t("damage.markingDamage") }}</span><span class="ds-val">{{ detailTarget?.markingDamageCount ?? 0 }}</span></div>
                <div class="ds-item"><span class="ds-label">{{ t("damage.roadSpill") }}</span><span class="ds-val">{{ detailTarget?.roadSpillCount ?? 0 }}</span></div>
                <div class="ds-item"><span class="ds-label">{{ t("damage.unknown") }}</span><span class="ds-val">{{ detailTarget?.unknownCount ?? 0 }}</span></div>
              </div>
            </div>
            <div style="margin-top:16px;border-top:1px solid #f0f2f5;padding-top:14px">
              <label class="detail-label">{{ t("road.severityDistribution") }}</label>
              <div class="disease-stats">
                <div class="ds-item"><span class="ds-label" style="color:#16a34a">{{ t("severity.low") }}</span><span class="ds-val">{{ detailTarget?.severityLowCount ?? 0 }}</span></div>
                <div class="ds-item"><span class="ds-label" style="color:#d97706">{{ t("severity.medium") }}</span><span class="ds-val">{{ detailTarget?.severityMediumCount ?? 0 }}</span></div>
                <div class="ds-item"><span class="ds-label" style="color:#dc2626">{{ t("severity.high") }}</span><span class="ds-val">{{ detailTarget?.severityHighCount ?? 0 }}</span></div>
              </div>
            </div>
            <div v-if="detailTarget?.evaluation" style="margin-top:16px;border-top:1px solid #f0f2f5;padding-top:14px">
              <label class="detail-label">{{ t("road.evaluation") }}</label>
              <div style="font-size:13px;color:#1e293b;line-height:1.6;margin-top:4px">{{ detailTarget.evaluation }}</div>
            </div>
            <div v-if="detailTarget?.suggestion" style="margin-top:12px">
              <label class="detail-label">{{ t("road.maintenanceSuggestion") }}</label>
              <div style="font-size:13px;color:#1e293b;line-height:1.6;margin-top:4px">{{ detailTarget.suggestion }}</div>
            </div>
          </template>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showDetail=false">{{ t("common.close") }}</button>
        </div>
      </div>
    </div>

    <!-- Generate Modal -->
    <div v-if="showGenerate" class="modal-overlay" @click.self="showGenerate=false">
      <div class="modal-card" style="width:420px">
        <div class="modal-head"><span>{{ t("road.generateArchive") }}</span><button class="modal-close" @click="showGenerate=false">✕</button></div>
        <div class="modal-body">
          <div class="form-group" style="margin-bottom:14px">
            <label style="font-size:11px;font-weight:600;color:#64748b;margin-bottom:4px;display:block">{{ t("road.selectRoad") }}</label>
            <select v-model="genForm.roadId" style="width:100%;padding:8px 10px;border:1px solid #e2e8f0;border-radius:6px;font-size:13px">
              <option :value="0" disabled>{{ t("road.selectRoadPlaceholder") }}</option>
              <option v-for="r in roadOptions" :key="r.id" :value="r.id">{{ r.roadName }}</option>
            </select>
          </div>
          <div class="form-group">
            <label style="font-size:11px;font-weight:600;color:#64748b;margin-bottom:4px;display:block">{{ t("road.archiveDate") }}</label>
            <input v-model="genForm.archiveDate" type="date" style="width:100%;padding:8px 10px;border:1px solid #e2e8f0;border-radius:6px;font-size:13px" />
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showGenerate=false">{{ t("road.cancel") }}</button>
          <button class="btn-primary" :disabled="generating" @click="handleGenerate">{{ generating ? t("road.generating") : t("road.confirmGenerate") }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { roadHealthArchiveApi, roadApi } from "@/api"
import type { RoadHealthArchiveResponse, RoadHealthArchiveDashboardResponse, RoadResponse } from "@/types"
import { t } from "@/i18n"

const loading = ref(false)
const archives = ref<RoadHealthArchiveResponse[]>([])
const dashboard = ref<RoadHealthArchiveDashboardResponse>({ totalRoads: 0, archivedRoads: 0, averageHealthScore: 0, healthyRoads: 0, subHealthyRoads: 0, unhealthyRoads: 0 })
const filterLevel = ref("")
const showDetail = ref(false)
const detailTarget = ref<RoadHealthArchiveResponse | null>(null)
const detailLoading = ref(false)
const showGenerate = ref(false)
const generating = ref(false)
const roadOptions = ref<RoadResponse[]>([])
const genForm = reactive({ roadId: 0, archiveDate: new Date().toISOString().slice(0, 10) })

const filteredArchives = computed(() => {
  if (!filterLevel.value) return archives.value
  return archives.value.filter(a => a.damageLevel === filterLevel.value)
})

function scoreCls(s?: number) { return s == null ? 'sc-low' : s >= 80 ? 'sc-high' : s >= 60 ? 'sc-mid' : 'sc-low' }
function scoreColor(s?: number) { return s == null ? '#ef4444' : s >= 80 ? '#22c55e' : s >= 60 ? '#f59e0b' : '#ef4444' }
function damageLevelClass(level?: string) {
  return ({
    HEALTHY: "sev-low",
    SUB_HEALTHY: "sev-med",
    UNHEALTHY: "sev-high",
  } as Record<string, string>)[level || ""] || "sev-low"
}
function damageLevelLabel(level?: string) {
  return ({
    HEALTHY: t("road.healthStatus"),
    SUB_HEALTHY: t("road.needAttentionStatus"),
    UNHEALTHY: t("road.damageStatus"),
  } as Record<string, string>)[level || ""] || "--"
}

async function loadData() {
  loading.value = true
  try {
    const [dashRes, listRes] = await Promise.all([
      roadHealthArchiveApi.dashboard(),
      roadHealthArchiveApi.list({ page: 1, size: 50 })
    ])
    dashboard.value = dashRes.data.data
    archives.value = listRes.data.data.records
  } catch {
    ElMessage.error(t("road.loadArchiveFailed"))
  }
  loading.value = false
}

async function viewDetail(a: RoadHealthArchiveResponse) {
  showDetail.value = true
  detailLoading.value = true
  detailTarget.value = a
  try {
    const res = await roadHealthArchiveApi.get(a.id)
    detailTarget.value = res.data.data
  } catch {
    // 保留已有数据
  }
  detailLoading.value = false
}

async function loadRoadOptions() {
  try {
    const res = await roadApi.list({ page: 1, size: 100 })
    roadOptions.value = res.data.data.records
  } catch { /* 忽略 */ }
}

async function handleGenerate() {
  if (!genForm.roadId) return ElMessage.warning(t("road.pleaseSelectRoad"))
  if (!genForm.archiveDate) return ElMessage.warning(t("road.pleaseSelectDate"))
  generating.value = true
  try {
    await roadHealthArchiveApi.generate({ roadId: genForm.roadId, archiveDate: genForm.archiveDate })
    ElMessage.success(t("road.archiveGenerated"))
    showGenerate.value = false
    await loadData()
  } catch {
    ElMessage.error(t("road.generateFailed"))
  }
  generating.value = false
}

onMounted(() => {
  loadData()
  loadRoadOptions()
})
</script>

<style scoped>
.rha-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }
.header-actions { display:flex; gap:8px; }
.btn-primary { display:inline-flex; align-items:center; gap:6px; padding:7px 16px; border:none; border-radius:6px; background:#2563eb; color:#fff; font-size:12px; font-weight:500; cursor:pointer; font-family:inherit; transition:background .15s; white-space:nowrap; }
.btn-primary:hover { background:#1d4ed8; }
.btn-primary:disabled { background:#93c5fd; cursor:not-allowed; }
.btn-ghost { padding:7px 16px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }

.stat-row { display:flex; gap:12px; margin-bottom:16px; }
.stat-card { flex:1; display:flex; align-items:center; gap:14px; padding:14px 18px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.stat-icon { width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.stat-val { font-size:22px; font-weight:800; color:#0f172a; line-height:1; }
.stat-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }

.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.card-head { display:flex; align-items:center; gap:6px; padding:12px 16px; font-size:13px; font-weight:600; color:#0f172a; border-bottom:1px solid #f0f2f5; }
.filter-select { padding:5px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; color:#334155; background:#fff; font-family:inherit; outline:none; cursor:pointer; }
.table-wrap { overflow-x:auto; }
.ds-table { width:100%; border-collapse:collapse; font-size:13px; }
.ds-table thead { background:#f8f9fc; }
.ds-table th { padding:10px 14px; text-align:left; font-weight:600; font-size:11px; color:#64748b; letter-spacing:0.03em; border-bottom:1px solid #eef0f4; }
.ds-table td { padding:10px 14px; border-bottom:1px solid #f5f6f8; color:#1e293b; }
.ds-table tbody tr:hover { background:#fafbfc; }
.td-primary { font-weight:600; color:#2563eb; }
.td-muted { color:#94a3b8; font-size:12px; }
.td-danger { color:#dc2626; font-weight:700; }
.empty-row { text-align:center; color:#94a3b8; padding:40px 0 !important; font-size:13px; }

.score-row { display:flex; align-items:center; gap:8px; }
.score-val { font-weight:700; font-size:15px; min-width:26px; }
.sc-high { color:#16a34a; }
.sc-mid { color:#d97706; }
.sc-low { color:#dc2626; }
.score-bar { width:60px; height:5px; background:#f1f5f9; border-radius:3px; overflow:hidden; }
.score-fill { height:100%; border-radius:3px; transition:width .5s ease; }
.sev-badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.sev-high { background:#fef2f2; color:#dc2626; }
.sev-med { background:#fffbeb; color:#d97706; }
.sev-low { background:#f0fdf4; color:#16a34a; }
.stat-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.tag-green { background:#f0fdf4; color:#16a34a; }
.tag-yellow { background:#fffbeb; color:#d97706; }
.tag-red { background:#fef2f2; color:#dc2626; }
.action-btn { padding:4px 10px; border:1px solid #e2e8f0; border-radius:5px; background:#fff; font-size:11px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.action-btn:hover { border-color:#2563eb; color:#2563eb; }

.insight-row { display:flex; gap:12px; margin-top:16px; }
.insight-card { flex:1; display:flex; align-items:center; gap:12px; padding:12px 16px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.insight-icon { width:36px; height:36px; display:flex; align-items:center; justify-content:center; border-radius:8px; flex-shrink:0; }
.insight-val { font-size:16px; font-weight:700; color:#0f172a; line-height:1; }
.insight-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:560px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-body { padding:20px; }
.detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
.detail-item { display:flex; flex-direction:column; gap:4px; }
.detail-item label { font-size:11px; font-weight:600; color:#64748b; }
.detail-item span { font-size:13px; color:#0f172a; }
.detail-label { font-size:11px; color:#64748b; font-weight:600; display:block; margin-bottom:8px; }
.disease-stats { display:flex; gap:12px; flex-wrap:wrap; }
.ds-item { display:flex; flex-direction:column; align-items:center; padding:6px 14px; background:#f8f9fc; border-radius:8px; min-width:60px; }
.ds-label { font-size:11px; color:#64748b; }
.ds-val { font-size:16px; font-weight:700; color:#0f172a; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
</style>
