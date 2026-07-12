<template>
  <div class="rm-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">道路管理</h2>
        <p class="page-desc">道路信息维护、健康评分与关联数据查看</p>
      </div>
      <div class="header-actions">
        <router-link to="/road-health" class="btn-ghost">健康档案</router-link>
        <router-link to="/road-maintenance" class="btn-ghost">养护记录</router-link>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg></div><div><span class="stat-val">{{ stats.totalRoads }}</span><span class="stat-lbl">道路总数</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ stats.healthy }}</span><span class="stat-lbl">健康良好</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef3c7;color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 9v4"/><path d="M12 17h.01"/></svg></div><div><span class="stat-val">{{ stats.warning }}</span><span class="stat-lbl">需要关注</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#fef2f2;color:#dc2626"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg></div><div><span class="stat-val">{{ stats.danger }}</span><span class="stat-lbl">严重病害</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:#f1f5f9;color:#64748b"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></div><div><span class="stat-val">{{ stats.totalDamages }}</span><span class="stat-lbl">当前病害总数</span></div></div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="filter-group">
            <div class="search-wrap"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="searchQuery" placeholder="搜索道路名称..." /></div>
            <select v-model="filterStatus" class="filter-select"><option value="">全部状态</option><option value="ACTIVE">运营中</option><option value="MAINTAINING">养护中</option><option value="CLOSED">封闭</option></select>
            <select v-model="filterGrade" class="filter-select"><option value="">全部等级</option><option value="主干道">主干道</option><option value="快速路">快速路</option><option value="次干道">次干道</option><option value="支路">支路</option></select>
          </div>
        </div>
        <span class="toolbar-info">共 {{ filteredRoads.length }} 条道路</span>
      </div>

      <div class="road-list" v-if="!loading">
        <div v-for="road in filteredRoads" :key="road.id" class="road-card">
          <div class="road-left">
            <div class="health-badge" :class="healthCls(road.healthScore, road.damageLevel)">{{ healthLabel(road.healthScore, road.damageLevel) }}</div>
            <div class="road-info">
              <div class="road-name">{{ road.roadName }}</div>
              <div class="road-meta">
                <span>编码: {{ road.roadCode || '--' }}</span>
                <span>长度: {{ road.lengthKm != null ? road.lengthKm + 'km' : '--' }}</span>
                <span>车道: {{ road.laneCount != null ? road.laneCount + ' 股' : '--' }}</span>
                <span>类型: {{ road.surfaceType || '--' }}</span>
                <span v-if="road.district">区域: {{ road.district }}</span>
                <span v-if="road.currentDamageCount != null && road.currentDamageCount > 0">病害: {{ road.currentDamageCount }} 处</span>
              </div>
            </div>
          </div>
          <div class="road-right">
            <span class="status-badge" :class="statusCls(road.status)">{{ statusLabel(road.status) }}</span>
            <div class="road-actions">
              <button class="action-btn" @click="viewRoad(road)">详情</button>
              <router-link :to="'/work-orders?keyword='+encodeURIComponent(road.roadName)" class="action-btn" style="text-decoration:none">工单</router-link>
              <router-link :to="'/road-health'" class="action-btn" style="text-decoration:none">档案</router-link>
            </div>
          </div>
        </div>
        <div v-if="filteredRoads.length === 0" class="empty-state">暂无匹配的道路数据</div>
      </div>
      <div v-else class="empty-state">加载中...</div>
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail=false">
      <div class="modal-card">
        <div class="modal-head"><span>道路详情 - {{ detailTarget?.roadName }}</span><button class="modal-close" @click="showDetail=false">✕</button></div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item"><label>道路名称</label><span>{{ detailTarget?.roadName }}</span></div>
            <div class="detail-item"><label>道路编码</label><span>{{ detailTarget?.roadCode || '--' }}</span></div>
            <div class="detail-item"><label>道路等级</label><span>{{ detailTarget?.roadGrade || '--' }}</span></div>
            <div class="detail-item"><label>所属区域</label><span>{{ detailTarget?.district || '--' }}</span></div>
            <div class="detail-item"><label>起点</label><span>{{ detailTarget?.startPoint || '--' }}</span></div>
            <div class="detail-item"><label>终点</label><span>{{ detailTarget?.endPoint || '--' }}</span></div>
            <div class="detail-item"><label>长度</label><span>{{ detailTarget?.lengthKm != null ? detailTarget?.lengthKm + ' km' : '--' }}</span></div>
            <div class="detail-item"><label>车道数</label><span>{{ detailTarget?.laneCount != null ? detailTarget?.laneCount + ' 股' : '--' }}</span></div>
            <div class="detail-item"><label>路面类型</label><span>{{ detailTarget?.surfaceType || '--' }}</span></div>
            <div class="detail-item"><label>建成年代</label><span>{{ detailTarget?.builtYear || '--' }}</span></div>
            <div class="detail-item"><label>健康评分</label><span :style="{color: detailTarget?.healthScore >= 80 ? '#16a34a' : detailTarget?.healthScore >= 60 ? '#d97706' : '#dc2626', fontWeight: 700}">{{ detailTarget?.healthScore != null ? detailTarget?.healthScore + ' (' + healthLabel(detailTarget?.healthScore) + ')' : '--' }}</span></div>
            <div class="detail-item"><label>病害等级</label><span>{{ damageLevelLabel(detailTarget?.damageLevel) }}</span></div>
            <div class="detail-item"><label>状态</label><span>{{ statusLabel(detailTarget?.status) }}</span></div>
            <div class="detail-item"><label>责任部门</label><span>{{ detailTarget?.departmentCode || '--' }}</span></div>
            <div class="detail-item"><label>累计检测</label><span>{{ detailTarget?.totalDetectionCount != null ? detailTarget?.totalDetectionCount + ' 次' : '--' }}</span></div>
            <div class="detail-item"><label>当前病害</label><span>{{ detailTarget?.currentDamageCount != null ? detailTarget?.currentDamageCount + ' 处' : '--' }}</span></div>
            <div class="detail-item"><label>最近检测</label><span>{{ detailTarget?.latestDetectionAt || '--' }}</span></div>
            <div class="detail-item"><label>上次养护</label><span>{{ detailTarget?.lastMaintained || '--' }}</span></div>
            <div v-if="detailTarget?.remark" class="detail-item" style="grid-column:1/-1"><label>备注</label><span>{{ detailTarget?.remark }}</span></div>
          </div>
        </div>
        <div class="modal-foot">
          <router-link :to="'/road-health'" class="btn-ghost" style="text-decoration:none;padding:7px 16px;display:inline-flex;align-items:center">健康档案</router-link>
          <router-link :to="'/road-maintenance'" class="btn-ghost" style="text-decoration:none;padding:7px 16px;display:inline-flex;align-items:center">养护记录</router-link>
          <button class="btn-primary" @click="showDetail=false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { roadApi } from "@/api"
import type { RoadResponse } from "@/types"

const searchQuery = ref("")
const filterStatus = ref("")
const filterGrade = ref("")
const showDetail = ref(false)
const detailTarget = ref<RoadResponse | null>(null)
const loading = ref(false)

const roads = ref<RoadResponse[]>([])
const stats = reactive({ totalRoads: 0, healthy: 0, warning: 0, danger: 0, totalDamages: 0 })

const filteredRoads = computed(() => {
  return roads.value.filter(r => {
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase()
      if (!r.roadName?.toLowerCase().includes(q) && !r.roadCode?.toLowerCase().includes(q)) return false
    }
    if (filterStatus.value && r.status !== filterStatus.value) return false
    if (filterGrade.value && r.roadGrade !== filterGrade.value) return false
    return true
  })
})

function healthCls(score?: number, damageLevel?: string) {
  if (damageLevel === 'HEALTHY') return "health-good"
  if (damageLevel === 'SUB_HEALTHY') return "health-warn"
  if (damageLevel === 'UNHEALTHY') return "health-danger"
  // 回退到 score 判断
  if (score == null) return "health-danger"
  if (score >= 85) return "health-good"
  if (score >= 60) return "health-warn"
  return "health-danger"
}
function healthLabel(score?: number, damageLevel?: string) {
  if (damageLevel === 'HEALTHY') return "优"
  if (damageLevel === 'SUB_HEALTHY') return "良"
  if (damageLevel === 'UNHEALTHY') return "差"
  // 回退到 score 判断
  if (score == null) return "--"
  if (score >= 85) return "优"
  if (score >= 60) return "良"
  return "差"
}
function statusCls(s?: string) {
  if (s === 'ACTIVE') return 'sta-active'
  if (s === 'MAINTAINING') return 'sta-maint'
  return 'sta-closed'
}
function statusLabel(s?: string) {
  return ({ ACTIVE: '运营中', MAINTAINING: '养护中', CLOSED: '封闭' } as any)[s || ''] || '未知'
}
function damageLevelLabel(level?: string) {
  return ({ HEALTHY: '健康', SUB_HEALTHY: '亚健康', UNHEALTHY: '需维修' } as any)[level || ''] || level || '--'
}

function calcStats() {
  stats.totalRoads = roads.value.length
  // 使用 damageLevel 字段精确判断（而不是用 healthScore 近似）
  stats.healthy = roads.value.filter(r => r.damageLevel === 'HEALTHY' || (r.healthScore ?? 100) >= 85).length
  stats.warning = roads.value.filter(r => r.damageLevel === 'SUB_HEALTHY' || ((r.healthScore ?? 100) >= 60 && (r.healthScore ?? 100) < 85)).length
  stats.danger = roads.value.filter(r => r.damageLevel === 'UNHEALTHY' || (r.healthScore ?? 100) < 60).length
  stats.totalDamages = roads.value.reduce((sum, r) => sum + (r.currentDamageCount || 0), 0)
}

async function loadData() {
  loading.value = true
  try {
    const res = await roadApi.list({ page: 1, size: 100 })
    roads.value = res.data.data.records
    calcStats()
  } catch {
    ElMessage.error("加载道路列表失败")
  }
  loading.value = false
}

function viewRoad(road: RoadResponse) {
  detailTarget.value = road
  showDetail.value = true
}

onMounted(loadData)
</script>

<style scoped>
.rm-page { font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; flex-wrap:wrap; gap:12px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#64748b; margin:0; }
.header-actions { display:flex; gap:8px; align-items:center; }
.btn-ghost { display:inline-flex; align-items:center; gap:6px; padding:7px 14px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.btn-primary { display:inline-flex; align-items:center; gap:6px; padding:7px 16px; border:none; border-radius:6px; background:#2563eb; color:#fff; font-size:12px; font-weight:500; cursor:pointer; font-family:inherit; transition:background .15s; white-space:nowrap; }
.btn-primary:hover { background:#1d4ed8; }

.stat-row { display:flex; gap:12px; margin-bottom:16px; }
.stat-card { flex:1; display:flex; align-items:center; gap:14px; padding:14px 18px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.stat-icon { width:38px; height:38px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.stat-val { font-size:22px; font-weight:800; color:#0f172a; line-height:1; }
.stat-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }

.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.toolbar { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f0f2f5; gap:12px; }
.toolbar-left { flex:1; }
.filter-group { display:flex; align-items:center; gap:8px; }
.filter-select { padding:6px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; color:#334155; background:#fff; font-family:inherit; outline:none; cursor:pointer; }
.search-wrap { display:flex; align-items:center; gap:6px; padding:6px 10px; background:#f8f9fc; border:1px solid #eef0f4; border-radius:8px; }
.search-wrap input { border:none; background:transparent; outline:none; font-size:12px; color:#1e293b; width:180px; font-family:inherit; }
.toolbar-info { font-size:12px; color:#94a3b8; }

.road-list { display:flex; flex-direction:column; }
.road-card { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #f5f6f8; transition:background .1s; }
.road-card:hover { background:#fafbfc; }
.road-left { display:flex; align-items:center; gap:14px; }
.health-badge { width:36px; height:36px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:12px; font-weight:700; color:#fff; flex-shrink:0; }
.health-good { background:#16a34a; }
.health-warn { background:#d97706; }
.health-danger { background:#dc2626; }
.road-info { }
.road-name { font-size:14px; font-weight:600; color:#0f172a; }
.road-meta { display:flex; gap:12px; font-size:11px; color:#94a3b8; margin-top:2px; flex-wrap:wrap; }
.road-right { display:flex; align-items:center; gap:10px; }
.status-badge { padding:3px 10px; border-radius:12px; font-size:11px; font-weight:600; }
.sta-active { background:#dcfce7; color:#16a34a; }
.sta-maint { background:#fef3c7; color:#d97706; }
.sta-closed { background:#f1f5f9; color:#64748b; }
.road-actions { display:flex; gap:4px; }
.action-btn { padding:4px 10px; border:1px solid #e2e8f0; border-radius:5px; background:#fff; font-size:11px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.action-btn:hover { border-color:#2563eb; color:#2563eb; }
.empty-state { text-align:center; padding:48px 0; color:#94a3b8; font-size:13px; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:560px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-body { padding:20px; }
.detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
.detail-item { display:flex; flex-direction:column; gap:4px; }
.detail-item label { font-size:11px; font-weight:600; color:#64748b; }
.detail-item span { font-size:14px; color:#0f172a; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
</style>
