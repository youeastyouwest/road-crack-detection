<template>
  <div class="rm-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">道路养护</h2>
        <p class="page-desc">养护计划制定、执行跟踪与完成情况</p>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#eef2ff,#dbeafe);color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg></div><div><span class="stat-val">{{ stats.total }}</span><span class="stat-lbl">总计划</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#fef3c7,#fde68a);color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div><span class="stat-val">{{ stats.pending }}</span><span class="stat-lbl">待执行</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#dbeafe,#bfdbfe);color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg></div><div><span class="stat-val">{{ stats.inProgress }}</span><span class="stat-lbl">进行中</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#dcfce7,#bbf7d0);color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ stats.completed }}</span><span class="stat-lbl">已完成</span></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:linear-gradient(135deg,#fce7f3,#fbcfe8);color:#e11d48"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-6"/></svg></div><div><span class="stat-val">{{ stats.overdue }}</span><span class="stat-lbl">已逾期</span></div></div>
    </div>

    <div class="timeline-card">
      <div class="card-head"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>养护时间线<button class="btn-primary" style="margin-left:auto">+ 新建计划</button></div>
      <div class="timeline">
        <div v-for="(t,i) in tasks" :key="i" class="tl-item">
          <div class="tl-dot" :class="t.status === '已完成' ? 'tl-dot-done' : t.status === '进行中' ? 'tl-dot-active' : 'tl-dot-pending'"></div>
          <div class="tl-line" v-if="i < tasks.length - 1"></div>
          <div class="tl-content">
            <div class="tl-head">
              <span class="tl-title">{{ t.planName }}</span>
              <span :class="['tl-badge', t.status === '已完成' ? 'badge-done' : t.status === '进行中' ? 'badge-active' : 'badge-pending']">{{ t.status }}</span>
            </div>
            <div class="tl-meta">
              <span class="tl-meta-item"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>{{ t.roadName }}</span>
              <span class="tl-meta-item"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>{{ t.planDate }}</span>
              <span v-if="t.executor" class="tl-meta-item"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>{{ t.executor }}</span>
            </div>
            <div v-if="t.desc" class="tl-desc">{{ t.desc }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue"

const tasks = ref([])

const stats = computed(() => ({
  total: tasks.value.length,
  pending: tasks.value.filter(t => t.status === "待执行").length,
  inProgress: tasks.value.filter(t => t.status === "进行中").length,
  completed: tasks.value.filter(t => t.status === "已完成").length,
  overdue: 1
}))
</script>

<style scoped>
.rm-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }

.stat-row { display:flex; gap:12px; margin-bottom:16px; }
.stat-card { flex:1; display:flex; align-items:center; gap:14px; padding:14px 18px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.stat-icon { width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.stat-val { font-size:20px; font-weight:800; color:#0f172a; line-height:1; }
.stat-lbl { font-size:11px; color:#94a3b8; margin-top:2px; display:block; }

.timeline-card { background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.card-head { display:flex; align-items:center; gap:6px; padding:14px 18px; font-size:13px; font-weight:600; color:#0f172a; border-bottom:1px solid #f0f2f5; }
.btn-primary { display:inline-flex; align-items:center; gap:6px; padding:7px 16px; border:none; border-radius:6px; background:#2563eb; color:#fff; font-size:12px; font-weight:500; cursor:pointer; font-family:inherit; transition:background .15s; white-space:nowrap; }
.btn-primary:hover { background:#1d4ed8; }

.timeline { padding:20px 24px; }
.tl-item { position:relative; display:flex; gap:16px; padding-bottom:20px; }
.tl-item:last-child { padding-bottom:0; }
.tl-dot { width:12px; height:12px; border-radius:50%; flex-shrink:0; margin-top:4px; z-index:1; position:relative; }
.tl-dot-done { background:#22c55e; box-shadow:0 0 0 3px #dcfce7; }
.tl-dot-active { background:#2563eb; box-shadow:0 0 0 3px #dbeafe; animation:dotPulse 2s infinite; }
.tl-dot-pending { background:#d0d5dd; box-shadow:0 0 0 3px #f1f5f9; }
@keyframes dotPulse { 0%,to { box-shadow:0 0 0 3px #dbeafe; } 50% { box-shadow:0 0 0 5px #bfdbfe; } }
.tl-line { position:absolute; left:5px; top:18px; width:2px; bottom:0; background:#f0f2f5; }
.tl-content { flex:1; }
.tl-head { display:flex; align-items:center; gap:10px; margin-bottom:6px; }
.tl-title { font-size:14px; font-weight:600; color:#0f172a; }
.tl-badge { padding:2px 8px; border-radius:4px; font-size:10px; font-weight:600; }
.badge-done { background:#f0fdf4; color:#16a34a; }
.badge-active { background:#dbeafe; color:#2563eb; }
.badge-pending { background:#f1f5f9; color:#64748b; }
.tl-meta { display:flex; gap:16px; margin-bottom:6px; flex-wrap:wrap; }
.tl-meta-item { display:flex; align-items:center; gap:4px; font-size:11px; color:#94a3b8; }
.tl-desc { font-size:12px; color:#64748b; line-height:1.5; padding:8px 12px; background:#f8f9fc; border-radius:6px; margin-top:4px; }
</style>