<template>
  <div class="rha-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">道路健康档案</h2>
        <p class="page-desc">路段健康评分、历史追溯与趋势分析</p>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#eef2ff,#dbeafe);color:#2563eb"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg></div><div><span class="stat-val">12</span><span class="stat-lbl">总道路数</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#dcfce7,#bbf7d0);color:#16a34a"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg></div><div><span class="stat-val">8</span><span class="stat-lbl">健康路段</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#fef3c7,#fde68a);color:#d97706"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></div><div><span class="stat-val">3</span><span class="stat-lbl">待维修路段</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#fce7f3,#fbcfe8);color:#e11d48"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg></div><div><span class="stat-val">76.8</span><span class="stat-lbl">平均评分</span></div></div>
    </div>

    <div class="content-card">
      <div class="card-head"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>路段健康档案</div>
      <div class="table-wrap">
        <table class="ds-table">
          <thead><tr><th>道路</th><th>路段</th><th>健康评分</th><th>评分趋势</th><th>最近检测</th><th>待处理工单</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="(a,i) in archives" :key="i">
              <td class="td-primary">{{ a.roadName }}</td>
              <td class="td-title">{{ a.segmentName }}</td>
              <td>
                <div class="score-row">
                  <span :class="['score-val', scoreCls(a.healthScore)]">{{ a.healthScore }}</span>
                  <div class="score-bar"><div class="score-fill" :style="{width:a.healthScore+'%',background:scoreColor(a.healthScore)}"></div></div>
                </div>
              </td>
              <td><span :class="['trend-badge', a.trend === 'up' ? 'trend-up' : a.trend === 'down' ? 'trend-down' : 'trend-flat']">{{ a.trend === 'up' ? '↑' : a.trend === 'down' ? '↓' : '→' }} {{ a.trendVal }}</span></td>
              <td class="td-muted">{{ a.lastDetection }}</td>
              <td><span class="num-badge" :class="a.pendingOrders > 0 ? 'badge-warn' : 'badge-ok'">{{ a.pendingOrders }}</span></td>
              <td><span :class="['stat-tag', a.healthScore >= 80 ? 'tag-green' : a.healthScore >= 60 ? 'tag-yellow' : 'tag-red']">{{ a.healthScore >= 80 ? '健康' : a.healthScore >= 60 ? '需关注' : '病害' }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="insight-row">
      <div class="insight-card"><div class="insight-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></div><div><span class="insight-val">+5.2%</span><span class="insight-lbl">本月健康度变化</span></div></div>
      <div class="insight-card"><div class="insight-icon" style="background:#fef3c7;color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div><span class="insight-val">3 段</span><span class="insight-lbl">需优先养护路段</span></div></div>
      <div class="insight-card"><div class="insight-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg></div><div><span class="insight-val">{{ archives.filter(a => a.healthScore >= 80).length }}/{{ archives.length }}</span><span class="insight-lbl">达标路段比例</span></div></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"

function scoreCls(s: number) { return s >= 80 ? 'sc-high' : s >= 60 ? 'sc-mid' : 'sc-low' }
function scoreColor(s: number) { return s >= 80 ? '#22c55e' : s >= 60 ? '#f59e0b' : '#ef4444' }

const archives = ref([])
</script>

<style scoped>
.rha-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }

.stat-row { display:flex; gap:12px; margin-bottom:16px; }
.stat-card { flex:1; display:flex; align-items:center; gap:14px; padding:14px 18px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.stat-icon { width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.stat-val { font-size:22px; font-weight:800; color:#0f172a; line-height:1; }
.stat-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }

.content-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.card-head { display:flex; align-items:center; gap:6px; padding:12px 16px; font-size:13px; font-weight:600; color:#0f172a; border-bottom:1px solid #f0f2f5; }
.table-wrap { overflow-x:auto; }
.ds-table { width:100%; border-collapse:collapse; font-size:13px; }
.ds-table thead { background:#f8f9fc; }
.ds-table th { padding:10px 14px; text-align:left; font-weight:600; font-size:11px; color:#64748b; letter-spacing:0.03em; border-bottom:1px solid #eef0f4; }
.ds-table td { padding:10px 14px; border-bottom:1px solid #f5f6f8; color:#1e293b; }
.ds-table tbody tr:hover { background:#fafbfc; }
.td-primary { font-weight:600; color:#2563eb; }
.td-title { font-weight:500; }
.td-muted { color:#94a3b8; font-size:12px; }

.score-row { display:flex; align-items:center; gap:8px; }
.score-val { font-weight:700; font-size:15px; min-width:26px; }
.sc-high { color:#16a34a; }
.sc-mid { color:#d97706; }
.sc-low { color:#dc2626; }
.score-bar { width:60px; height:5px; background:#f1f5f9; border-radius:3px; overflow:hidden; }
.score-fill { height:100%; border-radius:3px; transition:width .5s ease; }
.trend-badge { font-size:11px; font-weight:600; padding:2px 6px; border-radius:4px; }
.trend-up { color:#16a34a; background:#f0fdf4; }
.trend-down { color:#dc2626; background:#fef2f2; }
.trend-flat { color:#94a3b8; background:#f1f5f9; }
.num-badge { display:inline-flex; align-items:center; justify-content:center; min-width:22px; height:22px; border-radius:6px; font-size:11px; font-weight:700; }
.badge-ok { background:#f0fdf4; color:#16a34a; }
.badge-warn { background:#fef2f2; color:#dc2626; }
.stat-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.tag-green { background:#f0fdf4; color:#16a34a; }
.tag-yellow { background:#fffbeb; color:#d97706; }
.tag-red { background:#fef2f2; color:#dc2626; }

.insight-row { display:flex; gap:12px; margin-top:16px; }
.insight-card { flex:1; display:flex; align-items:center; gap:12px; padding:12px 16px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.insight-icon { width:36px; height:36px; display:flex; align-items:center; justify-content:center; border-radius:8px; flex-shrink:0; }
.insight-val { font-size:16px; font-weight:700; color:#0f172a; line-height:1; }
.insight-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }
</style>