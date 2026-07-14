<template>
  <div class="ds-container">
    <div class="ds-map-area">
      <div class="ds-float-stats">
        <div class="ds-float-stat"><span class="ds-fs-val">{{ totalRoad }}km</span><span class="ds-fs-lbl">总里程</span></div>
        <div class="ds-float-stat"><span class="ds-fs-val">{{ crackCount }}</span><span class="ds-fs-lbl">裂缝数</span></div>
        <div class="ds-float-stat"><span class="ds-fs-val">{{ repairedCount }}</span><span class="ds-fs-lbl">已修复</span></div>
        <div class="ds-float-stat"><span class="ds-fs-val">{{ alertCount }}</span><span class="ds-fs-lbl">告警</span></div>
        <div class="ds-float-time"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>&nbsp;{{ currentTime }}</div>
      </div>
      <div class="ds-view-controls">
        <button class="ds-vc-btn" @click="zoomIn" title="放大"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg></button>
        <div class="ds-vc-level">{{ zoomLevel }}x</div>
        <button class="ds-vc-btn" @click="zoomOut" title="缩小"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/></svg></button>
        <div class="ds-vc-sep"></div>
        <button class="ds-vc-btn" :class="{ active: viewMode3D }" @click="toggle3D" :title="viewMode3D ? '切换2D' : '切换3D'">
          <svg v-if="!viewMode3D" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 002 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
        </button>
        <div v-if="viewMode3D" class="ds-vc-pitch-row">
          <input type="range" min="0" max="60" v-model.number="pitchLevel" class="ds-vc-slider" @input="setPitch" />
          <span class="ds-vc-slider-label">{{ pitchLevel }}°</span>
        </div>
        <div class="ds-vc-sep"></div>
        <button class="ds-vc-btn ds-vc-locate" @click="locateMe" title="定位"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg></button>
      </div>
      <div ref="mapContainer" class="ds-map"></div>
      <!-- 病害点分布图例 -->
      <div class="ds-disease-legend">
        <div class="ds-legend-title">病害点分布</div>
        <div class="ds-legend-items">
          <div class="ds-legend-item"><span class="ds-legend-bar" style="background:#ef4444"></span><span>严重</span></div>
          <div class="ds-legend-item"><span class="ds-legend-bar" style="background:#f59e0b"></span><span>中等</span></div>
          <div class="ds-legend-item"><span class="ds-legend-bar" style="background:#22c55e"></span><span>轻微</span></div>
          <div class="ds-legend-item"><span class="ds-legend-bar" style="background:#cbd5e1"></span><span>畅通</span></div>
        </div>
      </div>
      <DiseaseMarkerLayer
        :roads="roadDiseaseData"
        :visible="false"
        :map-instance="map"
      />
      <div v-if="selectedDisease" class="ds-popup">
        <div class="ds-popup-head"><span>病害详情</span><span style="font-size:11px;color:#64748b;font-weight:400">{{ (selectedDisease as any)?._roadName || (selectedDisease as any)?.location || "" }}</span><button @click="selectedDisease = null">x</button></div>
        <div class="ds-popup-body">
          <div v-if="(selectedDisease as any)?._loading" style="text-align:center;padding:30px;color:#94a3b8">加载中...</div>
          <div v-else class="ds-popup-img">
            <img v-if="(selectedDisease as any)?._imageSrc" :src="(selectedDisease as any)?._imageSrc" alt="AI识别结果图" style="width:100%;height:100%;object-fit:cover;border-radius:6px" />
            <svg v-else width="100%" height="100" viewBox="0 0 200 100" fill="none"><rect width="200" height="100" rx="6" fill="#eef2ff"/><text x="100" y="45" text-anchor="middle" fill="#4361ee" font-size="12" font-weight="600">暂无图片</text><text x="100" y="65" text-anchor="middle" fill="#94a3b8" font-size="10">该病害点未上传图片</text></svg>
          </div>
          <div class="ds-popup-info">
            <div class="ds-popup-row"><span class="ds-popup-label">病害类型</span><strong>{{ damageTypeLabel(selectedDisease.type) }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">严重等级</span><strong><span class="ds-sev-dot" :style="{background: severityColor(selectedDisease.severity)}"></span>{{ selectedDisease.severity }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">检测编号</span><strong class="ds-link" :class="{ 'ds-link-disabled': !selectedDisease.taskCode || selectedDisease.taskCode === '--' }" @click="goToDetection(selectedDisease.taskCode)">{{ selectedDisease.taskCode || '--' }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">工单编号</span><strong class="ds-link" :class="{ 'ds-link-disabled': !selectedDisease.workOrderCode || selectedDisease.workOrderCode === '未生成' }" @click="goToWorkOrder(selectedDisease.workOrderCode)">{{ selectedDisease.workOrderCode || '未生成' }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">工单状态</span><strong :class="'ds-wo-status-' + (selectedDisease.workOrderStatus || 'pending')">{{ workOrderStatusLabel(selectedDisease.workOrderStatus) }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">检测时间</span><strong>{{ selectedDisease.time }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">道路</span><strong>{{ selectedDisease.location }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">置信度</span><strong class="ds-conf">{{ selectedDisease.confidence }}</strong></div>
            <div class="ds-popup-row"><span class="ds-popup-label">病害尺寸</span><strong class="ds-order">{{ selectedDisease.size || '--' }}</strong></div>
            <div class="ds-popup-row"><span>养护建议</span><strong style="font-size:11px;color:#64748b;text-align:right">{{ (selectedDisease as any)?._suggestion || "建议尽快安排修复处理" }}</strong></div>
          </div>
        </div>
        <div class="ds-popup-foot">
          <button class="ds-btn ds-btn-ghost" @click="selectedDisease = null">关闭</button>
        </div>
      </div>
      <div v-if="showCapacity" class="ds-popup" style="right:290px;top:80px">
        <div class="ds-popup-head"><span>点位容量概览</span><button @click="showCapacity = false">x</button></div>
        <div class="ds-popup-body" style="padding:0">
          <div class="ds-popup-row"><span>当前点位</span><strong>{{ currentPoints }} <span style="font-weight:400;color:#94a3b8;font-size:11px">个</span></strong></div>
          <div class="ds-popup-row"><span>系统上限</span><strong class="ds-conf">{{ maxPoints }} <span style="font-weight:400;color:#94a3b8;font-size:11px">个</span></strong></div>
          <div class="ds-popup-row"><span>剩余容量</span><strong class="ds-order">{{ maxPoints - currentPoints }} <span style="font-weight:400;color:#94a3b8;font-size:11px">个</span></strong></div>
          <div class="ds-popup-row" style="flex-direction:column;align-items:stretch;gap:4px;padding:10px 14px">
            <div style="display:flex;justify-content:space-between;font-size:10px;color:#94a3b8"><span>使用率</span><span style="font-weight:700;color:#4361ee">{{ (currentPoints / maxPoints * 100).toFixed(1) }}%</span></div>
            <div style="height:5px;background:#f1f5f9;border-radius:3px;overflow:hidden"><div :style="{height:'100%',width:(currentPoints / maxPoints * 100)+'%',background:'#4361ee',borderRadius:'3px'}"></div></div>
          </div>
          <div class="ds-popup-row" style="border-bottom:none"><span>存储建议</span><strong style="font-size:11px;line-height:1.5;color:#64748b;max-width:150px">建议定期导出历史数据，释放存储空间</strong></div>
        </div>
        <div class="ds-popup-foot">
          <button class="ds-btn ds-btn-ghost" @click="showCapacity = false">关闭</button>
          <button class="ds-btn ds-btn-primary" @click="exportReport">导出数据</button>
        </div>
      </div>
    </div>

    <div :class="['ds-sidebar', { collapsed: sidebarCollapsed }]">
      <div class="ds-sidebar-inner">
        <div class="ds-sb-header"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>地图面板</div>
        <div class="ds-sb-tabs">
          <button :class="['ds-sb-tab', { active: activeTab === 'map' }]" @click="activeTab = 'map'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 6v16l7-4 8 4 7-4V2l-7 4-8-4-7 4z"/><path d="M8 2v16"/><path d="M16 6v16"/></svg>
            <span>图层</span>
          </button>
          <button :class="['ds-sb-tab', { active: activeTab === 'analysis' }]" @click="activeTab = 'analysis'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-6"/><path d="M2 20h20"/></svg>
            <span>分析</span>
          </button>
          <button :class="['ds-sb-tab', { active: activeTab === 'chat' }]" @click="activeTab = 'chat'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/><path d="M8 10h8"/><path d="M8 14h5"/></svg>
            <span>AI 助手</span>
          </button>
        </div>
        <div v-if="activeTab === 'map'" class="ds-tab-panel">
          <div class="ds-sb-search-wrap">
            <div class="ds-sb-search"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="searchQuery" placeholder="搜索道路、地点..." @input="onSearchInput" /></div>
            <div v-if="searchQuery && searchResults.length > 0" class="ds-search-results">
              <div v-for="(r, idx) in searchResults" :key="idx" class="ds-search-item" @click="focusSearchResult(r)">
                <span class="ds-search-dot" :style="{ background: r.color }"></span>
                <div class="ds-search-info">
                  <div class="ds-search-name">{{ r.label }}</div>
                  <div class="ds-search-meta">{{ r.severityLabel }} · {{ r.count }}处病害</div>
                </div>
                <svg class="ds-search-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
              </div>
            </div>
            <div v-if="searchQuery && searchResults.length === 0 && searchDone" class="ds-search-empty">未找到匹配结果</div>
          </div>
          <div class="ds-sb-section"><div class="ds-sb-section-title">图层</div>
            <div class="ds-layer-item">
              <div class="ds-layer-toggle">
                <span class="ds-tt-label" style="font-size:12px;color:#0f172a;font-weight:600">病害标注点</span>
                <span class="ds-layer-count">{{ diseasePointTotal }}</span>
              </div>
            </div>
            <div v-for="layer in layers" :key="layer.id" class="ds-layer-item">
              <label class="ds-layer-label"><input type="checkbox" :checked="layer.visible" @change="toggleLayerVisibility(layer.id)" /><span class="ds-layer-dot" :style="{ background: layer.color }"></span>{{ layer.label }}<span class="ds-layer-count">{{ layer.count }}</span></label>
            </div>
          </div>
          <div class="ds-sb-section">
            <div class="ds-sb-section-title">工单状态</div>
            <div class="ds-layer-item">
              <label class="ds-layer-label">
                <input type="checkbox" :checked="workOrderVis.pending" @change="toggleWorkOrderVis('pending')" />
                <span class="ds-layer-dot" style="background:#ef4444"></span>
                <span>待修复</span>
                <span class="ds-layer-count">{{ workOrderStatusCounts.pending }}</span>
              </label>
            </div>
            <div class="ds-layer-item">
              <label class="ds-layer-label">
                <input type="checkbox" :checked="workOrderVis.assigned" @change="toggleWorkOrderVis('assigned')" />
                <span class="ds-layer-dot" style="background:#f59e0b"></span>
                <span>已派单</span>
                <span class="ds-layer-count">{{ workOrderStatusCounts.assigned }}</span>
              </label>
            </div>
            <div class="ds-layer-item">
              <label class="ds-layer-label">
                <input type="checkbox" :checked="workOrderVis.completed" @change="toggleWorkOrderVis('completed')" />
                <span class="ds-layer-dot" style="background:#2563eb"></span>
                <span>已修复</span>
                <span class="ds-layer-count">{{ workOrderStatusCounts.completed }}</span>
              </label>
            </div>
          </div>
          <div class="ds-sb-section"><div class="ds-sb-section-title">筛选条件</div>
            <div class="ds-filter-row"><label>严重程度</label><select v-model="filterSeverity"><option value="">全部</option><option value="HIGH">严重</option><option value="MEDIUM">中等</option><option value="LOW">轻微</option></select></div>
            <div class="ds-filter-row"><label>检测日期</label><input type="date" v-model="filterDate" /></div>
            <div class="ds-filter-row"><label>修复状态</label><select v-model="filterStatus"><option value="">全部</option><option value="PENDING">待修复</option><option value="ASSIGNED">已派单</option><option value="COMPLETED">已完成</option></select></div>
            <div class="ds-filter-row" v-if="filterSeverity || filterDate || filterStatus" style="margin-top:8px"><button class="ds-sb-btn" style="width:100%;font-size:11px" @click="filterSeverity = ''; filterDate = ''; filterStatus = ''">清除筛选</button></div>
          </div>
          <div class="ds-sb-actions"><button class="ds-sb-btn primary" @click="refreshMapData">刷新数据</button><div style="display:flex;gap:6px"><button class="ds-sb-btn" style="flex:1" @click="showCapacity = true">存储</button><button class="ds-sb-btn" style="flex:1" @click="exportReport">导出</button></div></div>
        </div>
        <div v-if="activeTab === 'analysis'" class="ds-tab-panel">
          <!-- 病害总数统计卡片 -->
          <div class="ds-an-section">
            <div class="ds-sb-section-title">病害统计</div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
              <div class="ds-an-card"><div class="ds-an-card-icon" style="background:#eef2ff;color:#4361ee"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg></div><div class="ds-an-card-info"><span class="ds-an-card-val">{{ totalDiseaseCount }}</span><span class="ds-an-card-lbl">病害总数</span></div></div>
              <div class="ds-an-card"><div class="ds-an-card-icon" style="background:#fef2f2;color:#f53f3f"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div class="ds-an-card-info"><span class="ds-an-card-val" style="color:#f53f3f">{{ highCount }}</span><span class="ds-an-card-lbl">严重</span></div></div>
              <div class="ds-an-card"><div class="ds-an-card-icon" style="background:#fefce8;color:#f59e0b"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div class="ds-an-card-info"><span class="ds-an-card-val" style="color:#f59e0b">{{ mediumCount }}</span><span class="ds-an-card-lbl">中等</span></div></div>
              <div class="ds-an-card"><div class="ds-an-card-icon" style="background:#eff6ff;color:#1677ff"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div class="ds-an-card-info"><span class="ds-an-card-val" style="color:#1677ff">{{ lowCount }}</span><span class="ds-an-card-lbl">轻微</span></div></div>
            </div>
          </div>
          <!-- 病害类型分布饼图 -->
          <div class="ds-an-section"><div class="ds-sb-section-title">病害类型分布</div><div ref="pieChartRef" style="height:130px;margin-top:4px"></div></div>
          <!-- 严重等级分布柱图 -->
          <div class="ds-an-section"><div class="ds-sb-section-title">严重等级分布</div><div ref="severityChartRef" style="height:90px;margin-top:4px"></div></div>
          <!-- 道路病害排行 TOP5 -->
          <div class="ds-an-section" style="border-top:1px solid #eef0f4;padding-top:16px">
            <div class="ds-sb-section-title">道路病害排行 TOP5</div>
            <div v-if="!roadNameResolved" style="text-align:center;padding:12px;color:#94a3b8;font-size:12px">正在解析道路名称...</div>
            <div v-else-if="roadRanking.length === 0" style="text-align:center;padding:12px;color:#94a3b8;font-size:12px">暂无数据</div>
            <div v-for="(r, ri) in roadRanking" :key="r.roadId" class="ds-rank-item" @click="focusRoad(r)">
              <div style="display:flex;justify-content:space-between;font-size:11px;margin-bottom:2px">
                <span style="color:#64748b">#{{ ri+1 }} {{ roadNameCn(r.roadName) }}</span>
                <span v-if="!r._roadNameReady" style="font-size:9px;color:#cbd5e1;margin-left:2px">...</span>
                <span style="font-weight:700;color:#0f172a">{{ r.totalCount }}处</span>
              </div>
              <div style="display:flex;gap:2px;height:6px;border-radius:3px;overflow:hidden;background:#f1f5f9">
                <div :style="{flex:r.highCount,background:'#f53f3f',minWidth:r.highCount>0?'2px':'0'}"></div>
                <div :style="{flex:r.mediumCount,background:'#f59e0b',minWidth:r.mediumCount>0?'2px':'0'}"></div>
                <div :style="{flex:r.lowCount,background:'#1677ff',minWidth:r.lowCount>0?'2px':'0'}"></div>
              </div>
              <div style="display:flex;gap:8px;margin-top:2px;font-size:10px;color:#94a3b8">
                <span>重{{ r.highCount }}</span><span>中{{ r.mediumCount }}</span><span>轻{{ r.lowCount }}</span>
              </div>
            </div>
          </div>
          <!-- 病害趋势图 -->
          <div class="ds-an-section"><div class="ds-sb-section-title">病害趋势</div><div ref="trendChartRef" style="height:110px;margin-top:4px"></div></div>
          <!-- 地图标记统计卡片 -->
          <div class="ds-an-section" style="border-top:1px solid #eef0f4;padding-top:16px">
            <div class="ds-sb-section-title">地图标记统计</div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
              <div class="ds-an-card-mini"><span class="ds-an-mini-val" style="color:#4361ee">{{ mapStatsData?.totalMarkers ?? 0 }}</span><span class="ds-an-mini-lbl">标记总数</span></div>
              <div class="ds-an-card-mini"><span class="ds-an-mini-val" style="color:#16a34a">{{ mapStatsData?.newMarkers ?? 0 }}</span><span class="ds-an-mini-lbl">新增标记</span></div>
              <div class="ds-an-card-mini"><span class="ds-an-mini-val" style="color:#15803d">{{ mapStatsData?.repairedCount ?? 0 }}</span><span class="ds-an-mini-lbl">已修复</span></div>
              <div class="ds-an-card-mini"><span class="ds-an-mini-val" style="color:#d97706">{{ mapStatsData?.pendingRepair ?? 0 }}</span><span class="ds-an-mini-lbl">待修复</span></div>
            </div>
          </div>
          <!-- 病害类型占比明细 -->
          <div class="ds-an-section">
            <div class="ds-sb-section-title">病害类型占比</div>
            <div v-if="damageTypeRatios.length === 0" style="text-align:center;padding:12px;color:#94a3b8;font-size:12px">暂无数据</div>
            <div v-for="dt in damageTypeRatios" :key="dt.damageType" style="margin-bottom:8px">
              <div style="display:flex;justify-content:space-between;font-size:11px;margin-bottom:3px">
                <span style="color:#475569;font-weight:500">{{ damageTypeLabel(dt.damageType) }}</span>
                <span style="color:#94a3b8">{{ dt.count }}处 ({{ (dt.ratio * 100).toFixed(1) }}%)</span>
              </div>
              <div style="height:5px;background:#f1f5f9;border-radius:3px;overflow:hidden">
                <div :style="{height:'100%',width:(dt.ratio*100)+'%',background:'#4361ee',borderRadius:'3px'}"></div>
              </div>
            </div>
          </div>
          <!-- 道路健康度列表 -->
          <div class="ds-an-section" style="border-top:1px solid #eef0f4;padding-top:16px">
            <div class="ds-sb-section-title">道路健康度</div>
            <div v-if="!roadNameResolved" style="text-align:center;padding:12px;color:#94a3b8;font-size:12px">正在解析道路名称...</div>
            <div v-else-if="roadHealthList.length === 0" style="text-align:center;padding:12px;color:#94a3b8;font-size:12px">暂无数据</div>
            <div v-for="r in roadHealthList" :key="r.roadId" class="ds-rank-item" style="display:flex;align-items:center;gap:8px" @click="focusRoad(r)">
              <span class="ds-health-dot" :style="{ background: healthColor(r.overallSeverity) }"></span>
              <span style="flex:1;font-size:11px;color:#475569;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ roadNameCn(r.roadName) }}</span>
              <span style="font-size:10px;color:#94a3b8">{{ r.totalCount }}处</span>
              <span class="ds-health-badge" :style="{ background: healthBg(r.overallSeverity), color: healthColor(r.overallSeverity) }">{{ healthLabel(r.overallSeverity) }}</span>
            </div>
          </div>
          <!-- 实时告警列表 -->
          <div class="ds-an-section" style="border-top:1px solid #eef0f4;padding-top:16px">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
              <span class="ds-sb-section-title" style="margin-bottom:0">实时告警</span>
              <span v-if="alertMarkers.length > 0" style="font-size:10px;color:#ef4444;font-weight:600">{{ alertMarkers.length }}条</span>
            </div>
            <div v-if="alertMarkers.length === 0" style="text-align:center;padding:12px;color:#94a3b8;font-size:12px">暂无告警</div>
            <div v-for="(am, ai) in alertMarkers.slice(0, 8)" :key="am.id" class="ds-rank-item" style="display:flex;align-items:center;gap:8px;animation:rowIn .35s ease both" :style="{ animationDelay: ai * 0.05 + 's' }" @click="focusAlert(am)">
              <span class="ds-alert-dot" :style="{ background: sevDotColor(am.severity) }"></span>
              <span style="flex:1;font-size:11px;color:#475569;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ am.roadName || am.address || (am.longitude ? '(' + Number(am.longitude).toFixed(4) + ', ' + Number(am.latitude).toFixed(4) + ')' : '--') }}</span>
              <span class="ds-alert-type" :style="{ background: sevBgColor(am.severity), color: sevTextColor(am.severity) }">{{ damageTypeLabel(am.damageType) }}</span>
            </div>
          </div>
        </div>
        <div v-if="activeTab === 'chat'" class="ds-tab-panel ds-chat-panel" style="display:flex;flex-direction:column;overflow:hidden;">
          <div class="ds-sb-header" style="flex-shrink:0"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>AI 助手<button class="ds-chat-clear" @click="chatMessages = [{role:'ai', text:'您好！我是道路病害AI助手，可以为您分析检测数据、查询病害详情或生成报告。'}]"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg></button></div>
          <div class="ds-chat-msgs" ref="chatMsgRef">
            <div v-for="(m, i) in chatMessages" :key="i" :class="['ds-chat-msg', m.role]">
              <div v-if="m.role === 'ai'" class="ds-chat-avatar"><div class="avatar-video-wrap"><img class="avatar-video" :src="avatarSrc" alt="AI助手" /></div></div>
              <div class="ds-chat-bubble" v-html="m.text"></div>
            </div>
            <div v-if="chatTyping" class="ds-chat-typing"><span></span><span></span><span></span></div>
          </div>
          <div class="ds-chat-input-bar"><input v-model="chatInput" placeholder="输入您的问题，例如：今日病害统计" :disabled="chatTyping" @keyup.enter="sendChat" /><button class="ds-chat-send" :disabled="!chatInput.trim() || chatTyping" @click="sendChat"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg></button></div>
        </div>
      </div>
    </div>
  </div>
  <button class="ds-toggle-btn" :style="{ right: sidebarCollapsed ? '14px' : '354px' }" @click="sidebarCollapsed = !sidebarCollapsed">
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline :points="sidebarCollapsed ? '9 18 15 12 9 6' : '15 18 9 12 15 6'" /></svg>
  </button>
</template>
<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from "vue"
import { detectionApi, statisticsApi, mapApi, agentApi } from "@/api"
import * as echarts from "echarts"
import DiseaseMarkerLayer from "@/components/DiseaseMarkerLayer.vue"
import { useRouter } from "vue-router"
import { DetectionTaskStatus } from "@/types"

import { getBatchGeocoder, disposeBatchGeocoder, type BatchGeocoderOptions } from "@/utils/batchGeocoder"

const router = useRouter()

declare global {
  interface Window { AMap: any }
}

const mapContainer = ref()
const sidebarCollapsed = ref(false)
const showCapacity = ref(false)
const zoomLevel = ref(12)
const viewMode3D = ref(false)
const pitchLevel = ref(0)
const currentTime = ref("")
const activeTab = ref("map")
const filterSeverity = ref("")
const filterDate = ref("")
const filterStatus = ref("")
const searchQuery = ref("")
const searchDone = ref(false)

interface SearchResult {
  label: string
  lng: number
  lat: number
  color: string
  severityLabel: string
  count: number
  roadId?: number
}

const SEV_LABEL: Record<string, string> = { HIGH: "严重", MEDIUM: "中等", LOW: "轻微", REPAIRED: "已维修" }
const SEV_COLOR: Record<string, string> = { HIGH: "#ef4444", MEDIUM: "#f59e0b", LOW: "#22c55e", REPAIRED: "#3b82f6" }

const searchResults = computed<SearchResult[]>(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return []
  const results: SearchResult[] = []
  const seen = new Set<string>()
  for (const road of (roadDiseaseData.value || [])) {
    const roadName = roadNameCn(road.roadName || road.name || "")
    if (roadName.toLowerCase().includes(q)) {
      const key = `road-${road.roadId || roadName}`
      if (!seen.has(key)) {
        seen.add(key)
        const topSev = (road.highCount > 0 ? "HIGH" : road.mediumCount > 0 ? "MEDIUM" : "LOW") as string
        results.push({
          label: roadName,
          lng: road.centerLng || (road.diseasePoints?.[0]?.lng ?? 116.397),
          lat: road.centerLat || (road.diseasePoints?.[0]?.lat ?? 39.909),
          color: SEV_COLOR[topSev] || "#f59e0b",
          severityLabel: SEV_LABEL[topSev] || "中等",
          count: road.totalCount || road.diseasePoints?.length || 0,
          roadId: road.roadId,
        })
      }
    }
    for (const dp of (road.diseasePoints || [])) {
      const dpRoadName = geocodeCache.get(`${Number(dp.lng).toFixed(6)},${Number(dp.lat).toFixed(6)}`) || roadName
      if (dpRoadName.toLowerCase().includes(q) || (dp.damageType || "").toLowerCase().includes(q)) {
        const key = `dp-${dp.lng}-${dp.lat}`
        if (!seen.has(key)) {
          seen.add(key)
          const sev = dp.severity || "MEDIUM"
          const sevKey = isRepairedStatus(dp.workOrderNo) ? "REPAIRED" : sev
          results.push({
            label: dpRoadName,
            lng: dp.lng,
            lat: dp.lat,
            color: SEV_COLOR[sevKey] || SEV_COLOR[sev] || "#f59e0b",
            severityLabel: SEV_LABEL[sevKey] || SEV_LABEL[sev] || "中等",
            count: 1,
          })
        }
      }
    }
  }
  return results.slice(0, 20)
})

let searchDebounce: number | undefined
function onSearchInput() {
  searchDone.value = false
  if (searchDebounce) clearTimeout(searchDebounce)
  searchDebounce = window.setTimeout(() => {
    searchDone.value = true
  }, 300)
}

function focusSearchResult(r: SearchResult) {
  if (!map) return
  map.setZoomAndCenter(15, [r.lng, r.lat])
  searchQuery.value = ""
  searchDone.value = false
}

// 点击道路排行项 → 锁定到该道路
const focusedRoadId = ref<number | null>(null)
function focusRoad(r: any) {
  if (!map) return
  // 优先用 centerLng/centerLat，否则用病害点坐标的质心
  let lng = r.centerLng
  let lat = r.centerLat
  if (!lng || !lat) {
    const dps = r.diseasePoints || []
    if (dps.length > 0) {
      lng = dps.reduce((s: number, d: any) => s + (d.lng || 0), 0) / dps.length
      lat = dps.reduce((s: number, d: any) => s + (d.lat || 0), 0) / dps.length
    } else {
      return // 没有坐标，无法定位
    }
  }

  // 如果该道路有多个病害点，用 setFitView 适配所有点；否则直接缩放定位
  const dps = (r.diseasePoints || []).filter((d: any) => d.lng && d.lat)
  if (dps.length > 1) {
    // 先飞到中心，再适配视野
    map.setZoomAndCenter(16, [lng, lat])
    // 用病害点坐标构建临时 Bounds 适配
    setTimeout(() => {
      try {
        const bounds = new window.AMap.Bounds(
          [Math.min(...dps.map((d: any) => d.lng)), Math.min(...dps.map((d: any) => d.lat))],
          [Math.max(...dps.map((d: any) => d.lng)), Math.max(...dps.map((d: any) => d.lat))]
        )
        map.setBounds(bounds, false, [120, 120, 120, 120])
      } catch(e) {
        map.setZoomAndCenter(15, [lng, lat])
      }
    }, 300)
  } else {
    map.setZoomAndCenter(17, [lng, lat])
  }

  // 高亮该道路的标记：先清除之前的高亮，再标记当前道路
  focusedRoadId.value = r.roadId ?? null
  highlightRoadMarkers(r)
}

// 点击告警项 → 定位到告警坐标并高亮对应道路
function focusAlert(am: any) {
  if (!map) return
  const lng = am.longitude || am.lng
  const lat = am.latitude || am.lat
  if (!lng || !lat) return

  // 缩放到 17 级精准定位
  map.setZoomAndCenter(17, [lng, lat])

  // 找到告警对应的道路，高亮其标记
  const roadName = am.roadName || ""
  if (roadName && roadName !== "未知道路") {
    // 按路名匹配道路数据
    const matchedRoad = (roadDiseaseData.value || []).find((r: any) => {
      const rn = roadNameCn(r.roadName || "")
      return rn === roadName || roadNameCn(roadName) === rn
    })
    if (matchedRoad) {
      focusedRoadId.value = matchedRoad.roadId ?? null
      highlightRoadMarkers(matchedRoad)
      return
    }
  }

  // 没匹配到道路，只高亮该坐标附近的标记
  focusedRoadId.value = null
  const targetKey = `${Number(lng).toFixed(6)},${Number(lat).toFixed(6)}`
  roadPolylines.forEach((pl: any) => {
    try {
      const ext = pl.getExtData?.() || {}
      const dp = ext.dp
      if (!dp) return
      const key = `${Number(dp.lng).toFixed(6)},${Number(dp.lat).toFixed(6)}`
      const isTarget = key === targetKey
      if (pl.setOptions) {
        if (isTarget) {
          pl.setOptions({ strokeOpacity: 1, fillOpacity: ext._ringType === 'inner' ? 0.35 : 0, strokeWeight: 2, zIndex: 200 })
        } else {
          pl.setOptions({ strokeOpacity: 0.15, fillOpacity: ext._ringType === 'inner' ? 0.04 : 0, strokeWeight: 0.5, zIndex: 10 })
        }
      } else if (pl.setOpacity) {
        pl.setOpacity(isTarget ? 1 : 0.2)
      }
    } catch(e) {}
  })
}

// 高亮指定道路的地图标记，其他道路标记降低透明度
function highlightRoadMarkers(road: any) {
  const targetDps = new Set<string>()
  for (const dp of (road.diseasePoints || [])) {
    if (dp.lng && dp.lat) {
      targetDps.add(`${Number(dp.lng).toFixed(6)},${Number(dp.lat).toFixed(6)}`)
    }
  }
  roadPolylines.forEach((pl: any) => {
    try {
      const ext = pl.getExtData?.() || {}
      const dp = ext.dp
      if (!dp) return
      const key = `${Number(dp.lng).toFixed(6)},${Number(dp.lat).toFixed(6)}`
      const isTarget = targetDps.has(key)
      if (pl.setOptions) {
        if (isTarget) {
          // 目标道路标记：提升透明度和权重
          pl.setOptions({ strokeOpacity: 1, fillOpacity: ext._ringType === 'inner' ? 0.35 : 0, strokeWeight: 2, zIndex: 200 })
        } else {
          // 非目标道路标记：降低透明度
          pl.setOptions({ strokeOpacity: 0.15, fillOpacity: ext._ringType === 'inner' ? 0.04 : 0, strokeWeight: 0.5, zIndex: 10 })
        }
      } else if (pl.setOpacity) {
        // Marker 类型
        pl.setOpacity(isTarget ? 1 : 0.2)
      }
    } catch(e) {}
  })
}

// 清除高亮，恢复所有标记到正常状态
function clearRoadHighlight() {
  focusedRoadId.value = null
  roadPolylines.forEach((pl: any) => {
    try {
      const ext = pl.getExtData?.() || {}
      const colorInfo = ext.colorInfo
      const sev = ext._sevLevel
      if (pl.setOptions && colorInfo) {
        if (ext._ringType === 'outer') {
          pl.setOptions({ strokeOpacity: 0.55, fillOpacity: 0, strokeWeight: 1.2 })
        } else if (ext._ringType === 'inner') {
          pl.setOptions({ strokeOpacity: 0.45, fillOpacity: 0.16, strokeWeight: 1 })
        }
      } else if (pl.setOpacity) {
        pl.setOpacity(1)
      }
    } catch(e) {}
  })
}
const layers = ref([
  { id: "sev_high", label: "严重", color: "#ef4444", count: 0, visible: true },
  { id: "sev_medium", label: "中等", color: "#f59e0b", count: 0, visible: true },
  { id: "sev_low", label: "轻微", color: "#22c55e", count: 0, visible: true },
])
const selectedDisease = ref<any>(null)
const currentPoints = ref(0)
const maxPoints = ref(5000)
const chatMessages = ref([{ role: "ai", text: "您好！我是道路病害AI助手，可以为您分析检测数据、查询病害详情或生成报告。" }])
const chatInput = ref("")
const chatTyping = ref(false)
const diseaseMarkers = ref<any[]>([])
const diseaseList = ref<any[]>([])
const roadDiseaseData = ref<any[]>([])

const totalDiseaseCount = computed(() => {
  let count = 0
  for (const road of (roadDiseaseData.value || [])) {
    for (const dp of (road.diseasePoints || [])) {
      const woStatus = normalizeWoStatus(dp.workOrderNo)
      if (woStatus !== "completed") count++
    }
  }
  return count
})
const highCount = computed(() => {
  let count = 0
  for (const road of (roadDiseaseData.value || [])) {
    for (const dp of (road.diseasePoints || [])) {
      const woStatus = normalizeWoStatus(dp.workOrderNo)
      if (woStatus !== "completed" && dp.severity === "HIGH") count++
    }
  }
  return count
})
const mediumCount = computed(() => {
  let count = 0
  for (const road of (roadDiseaseData.value || [])) {
    for (const dp of (road.diseasePoints || [])) {
      const woStatus = normalizeWoStatus(dp.workOrderNo)
      if (woStatus !== "completed" && dp.severity === "MEDIUM") count++
    }
  }
  return count
})
const lowCount = computed(() => {
  let count = 0
  for (const road of (roadDiseaseData.value || [])) {
    for (const dp of (road.diseasePoints || [])) {
      const woStatus = normalizeWoStatus(dp.workOrderNo)
      if (woStatus !== "completed" && dp.severity === "LOW") count++
    }
  }
  return count
})
const roadRanking = computed(() => [...(roadDiseaseData.value || [])].sort((a: any, b: any) => (b.totalCount || 0) - (a.totalCount || 0)).slice(0, 5))

/**
 * 地图上实际绘制的病害标注点数量（按坐标去重后的数量，只统计未修复的点）
 */
const diseasePointTotal = computed(() => {
  const coordSet = new Set<string>()
  for (const road of (roadDiseaseData.value || [])) {
    for (const dp of (road.diseasePoints || [])) {
      if (dp.lng && dp.lat) {
        const woStatus = normalizeWoStatus(dp.workOrderNo)
        // 只统计未修复的点
        if (woStatus !== "completed") {
          coordSet.add(`${Number(dp.lng).toFixed(6)},${Number(dp.lat).toFixed(6)}`)
        }
      }
    }
  }
  return coordSet.size
})

const mapMarkers: any[] = []
let roadPolylines: any[] = []
const realStats = ref<any>(null)
const chatMsgRef = ref()
const trendChartRef = ref()
const pieChartRef = ref()
const severityChartRef = ref()
const weeklyTotal = ref(0)
const pendingRepair = ref(0)
const totalRoad = ref(0)
const crackCount = ref(0)
const repairedCount = ref(0)
const alertCount = ref(0)
const workOrderStatusCounts = ref({ pending: 0, assigned: 0, completed: 0 })
const workOrderVis = ref({ pending: true, assigned: true, completed: true })

let map: any = null
let trendChart: any = null
let pieChart: any = null
let severityChart: any = null
let timeInterval: any = null
let refreshTimer: any = null

const mapStatsData = ref<any>(null)
const damageTypeRatios = ref<any[]>([])
const alertMarkers = ref<any[]>([])
const roadHealthList = computed(() => {
  return [...(roadDiseaseData.value || [])].sort((a: any, b: any) => {
    const order: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 }
    return (order[a.overallSeverity] ?? 3) - (order[b.overallSeverity] ?? 3)
  }).slice(0, 8)
})

/** 道路名称解析状态：true 表示所有"未知道路"已通过逆地理完成兜底 */
const roadNameResolved = ref(false)

const avatarSrc = computed(() => "/avatar-agent.png")

/**
 * 解析后端返回的检测结果图字段。
 * 后端可能返回 base64 字符串，也可能返回 /uploads/result/xxx.jpg 形式的 URL。
 */
function resolveImgSrc(imageBase64?: string, fileUrl?: string): string {
  if (imageBase64) {
    if (imageBase64.startsWith('/') || imageBase64.startsWith('http')) return imageBase64
    return 'data:image/jpeg;base64,' + imageBase64
  }
  return fileUrl || ''
}

const trendOption = computed(() => ({
  grid: { left: 30, right: 8, top: 10, bottom: 20 },
  xAxis: { type: "category", data: [], axisLabel: { fontSize: 10, color: "#94a3b8" }, axisLine: { show: false }, axisTick: { show: false } },
  yAxis: { type: "value", splitLine: { lineStyle: { color: "#f1f5f9", type: "dashed" } }, axisLabel: { fontSize: 9, color: "#94a3b8" }, min: 0, minInterval: 1 },
  series: [{ type: "line", data: [], smooth: true, lineStyle: { color: "#4361ee", width: 2 }, itemStyle: { color: "#4361ee" }, areaStyle: { color: { type: "linear", x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: "rgba(67,97,238,0.25)" }, { offset: 1, color: "rgba(67,97,238,0.04)" }] } }, symbol: "circle", symbolSize: 5 }],
  tooltip: { trigger: "axis", backgroundColor: "rgba(255,255,255,0.95)", borderColor: "#eef0f4", textStyle: { fontSize: 11 }, formatter: (p: any) => p[0]?.name + '<br/>' + p[0]?.value + '处' },
}))

const pieOption = computed(() => ({
  tooltip: { trigger: "item", backgroundColor: "rgba(255,255,255,0.95)", borderColor: "#eef0f4", textStyle: { fontSize: 11 }, formatter: "{b}: {c}处 ({d}%)" },
  legend: { orient: "vertical", right: 0, top: "center", itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10, color: "#64748b" } },
  series: [{
    type: "pie", radius: ["30%", "60%"], center: ["35%", "50%"],
    color: ["#4361ee","#f72585","#06d6a0","#ffd166","#7209b7","#118ab2"], data: [],
    label: { show: true, fontSize: 10, color: "#475569", formatter: "{c}处" },
    labelLine: { show: true, length: 5, length2: 8 },
    emphasis: { label: { show: true, fontSize: 11, fontWeight: "bold" }, itemStyle: { shadowBlur: 4, shadowColor: "rgba(0,0,0,0.08)" } },
  }],
}))

const severityOption = computed(() => ({
  grid: { left: 45, right: 35, top: 5, bottom: 5 },
  xAxis: { type: "value", splitLine: { lineStyle: { color: "#f1f5f9" } }, axisLabel: { fontSize: 9, color: "#94a3b8" }, axisLine: { show: false }, axisTick: { show: false } },
  yAxis: { type: "category", data: [], axisLabel: { fontSize: 11, color: "#475569", fontWeight: 600 }, axisLine: { show: false }, axisTick: { show: false } },
  series: [{ type: "bar", data: [], barWidth: 14, label: { show: true, position: "right", fontSize: 10, color: "#475569", fontWeight: 600, formatter: (p: any) => p.value + "处" }, itemStyle: { borderRadius: [0, 4, 4, 0] } }],
  tooltip: { trigger: "axis", backgroundColor: "rgba(255,255,255,0.95)", borderColor: "#eef0f4", textStyle: { fontSize: 11 } },
}))

function workOrderStatusLabel(status?: string) {
  const map: Record<string, string> = {
    pending: "待修复", assigned: "已派单", completed: "已修复"
  }
  return map[status || "pending"] || "待修复"
}

function goToDetection(taskCode?: string) {
  if (!taskCode || taskCode === '--') return
  console.log("Navigating to detection result:", taskCode)
  router.push({ path: "/detection-results", query: { highlight: taskCode } })
}

function goToWorkOrder(workOrderCode?: string) {
  if (!workOrderCode || workOrderCode === '未生成') return
  console.log("Navigating to work order:", workOrderCode)
  router.push({ path: "/work-orders", query: { highlight: workOrderCode } })
}

// --- 地图状态保持 ---
let savedMapState: { center: [number, number]; zoom: number; pitch: number; rotation: number } | null = null

function saveMapState() {
  if (!map) return
  savedMapState = {
    center: map.getCenter().toArray() as [number, number],
    zoom: map.getZoom(),
    pitch: map.getPitch(),
    rotation: map.getRotation()
  }
}

function restoreMapState() {
  if (!map || !savedMapState) return
  map.setZoomAndCenter(savedMapState.zoom, savedMapState.center)
  map.setPitch(savedMapState.pitch)
  map.setRotation(savedMapState.rotation)
}

// --- 原有函数 ---
function severityColor(sev: string) { const m: Record<string, string> = { "严重": "#ef4444", "中等": "#f59e0b", "轻微": "#10b981" }; return m[sev] || "#94a3b8" }
function zoomIn() { zoomLevel.value = Math.min(zoomLevel.value + 1, 20); if (map) map.setZoom(zoomLevel.value) }
function zoomOut() { zoomLevel.value = Math.max(zoomLevel.value - 1, 3); if (map) map.setZoom(zoomLevel.value) }
function toggle3D() {
  viewMode3D.value = !viewMode3D.value
  if (!map) return
  if (viewMode3D.value) {
    map.setPitch(30)
    pitchLevel.value = 30
    map.setRotation(0)
  } else {
    map.setPitch(0)
    pitchLevel.value = 0
    map.setRotation(0)
  }
}
function setPitch() {
  if (!map || !viewMode3D.value) return
  map.setPitch(pitchLevel.value)
}
function locateMe() {
  if (!map) return
  const fallback = () => { map.setCenter([116.397428, 39.90923]); map.setZoom(13); zoomLevel.value = 13 }
  if (navigator.geolocation) { navigator.geolocation.getCurrentPosition((p) => { map.setCenter([p.coords.longitude, p.coords.latitude]); map.setZoom(15); zoomLevel.value = 15 }, fallback) } else { fallback() }
}
function initMap() {
  if (!mapContainer.value || !window.AMap) return
  // 如果有保存的状态，恢复它
  const center = savedMapState ? savedMapState.center : [116.397428, 39.90923]
  const zoom = savedMapState ? savedMapState.zoom : zoomLevel.value
  map = new window.AMap.Map(mapContainer.value, {
    zoom: zoom,
    center: center,
    features: ["bg", "road", "building", "point"],
    resizeEnable: true,
    viewMode: "3D",
    pitch: savedMapState ? savedMapState.pitch : 0,
  })

  // 加载高德地图热力图插件
  map.plugin(["AMap.Heatmap"], () => {
    console.log("AMap.Heatmap plugin loaded")
  })

  map.on("zoomchange", () => { zoomLevel.value = map.getZoom(); saveMapState() })
  map.on("moveend", () => { saveMapState() })
  // 点击地图空白处清除道路高亮
  map.on("click", () => {
    if (focusedRoadId.value !== null) {
      clearRoadHighlight()
    }
  })
  // 初始化批量逆地理编码器（复用 Geocoder 单例）
  batchGeocoder.init()
}
function initECharts() {
  // 分析面板使用 v-if 渲染，切换到 analysis 标签页时 DOM 才存在
  // 每次调用都检查 refs 是否可用，避免重复初始化
  if (trendChartRef.value && !trendChart) { trendChart = echarts.init(trendChartRef.value); trendChart.setOption(trendOption.value) }
  if (pieChartRef.value && !pieChart) { pieChart = echarts.init(pieChartRef.value); pieChart.setOption(pieOption.value) }
  if (severityChartRef.value && !severityChart) { severityChart = echarts.init(severityChartRef.value); severityChart.setOption(severityOption.value) }
  // 如果图表已存在但 DOM 重新渲染了（v-if 切换），需要重新创建
  if (trendChartRef.value && trendChart && trendChart.isDisposed()) { trendChart = echarts.init(trendChartRef.value); trendChart.setOption(trendOption.value) }
  if (pieChartRef.value && pieChart && pieChart.isDisposed()) { pieChart = echarts.init(pieChartRef.value); pieChart.setOption(pieOption.value) }
  if (severityChartRef.value && severityChart && severityChart.isDisposed()) { severityChart = echarts.init(severityChartRef.value); severityChart.setOption(severityOption.value) }
}

// 监听标签页切换：当离开"分析"面板时销毁图表实例（v-if 会销毁 DOM）
watch(activeTab, (newTab, oldTab) => {
  if (oldTab === 'analysis' && newTab !== 'analysis') {
    // 离开分析面板：销毁图表，下次回来时重新创建
    if (trendChart) { trendChart.dispose(); trendChart = null }
    if (pieChart) { pieChart.dispose(); pieChart = null }
    if (severityChart) { severityChart.dispose(); severityChart = null }
  }
  if (newTab === 'analysis') {
    nextTick(() => {
      initECharts()
      refreshChartsFromData()
    })
  }
})

/**
 * 用已加载的统计数据刷新所有图表（不重新请求后端）
 * 在图表刚初始化、需要把已有数据灌入时调用
 */
function refreshChartsFromData() {
  if (!realStats.value) return
  const d = realStats.value
  // 趋势图
  if (d.trend && d.trend.length && trendChart) {
    trendChart.setOption({
      xAxis: { data: d.trend.map((t: any) => t.date?.slice(5) || "") },
      series: [{ data: d.trend.map((t: any) => t.count) }]
    })
  }
  // 病害类型饼图
  if (d.crackTypes && d.crackTypes.length && pieChart) {
    const colors = ["#4361ee","#f72585","#06d6a0","#ffd166","#7209b7","#118ab2"]
    pieChart.setOption({
      series: [{
        data: d.crackTypes.map((t: any, i: number) => ({
          name: damageTypeLabel(t.type), value: t.count,
          itemStyle: { color: colors[i % colors.length] }
        }))
      }]
    })
  }
  // 严重等级柱图
  if (d.severity && d.severity.length && severityChart) {
    const sevColors: Record<string, string> = { HIGH: "#ef4444", MEDIUM: "#f59e0b", LOW: "#22c55e" }
    const sevLabel: Record<string, string> = { HIGH: "严重", MEDIUM: "中等", LOW: "轻微" }
    // 确保顺序为 严重 > 中等 > 轻微
    const ordered = [...d.severity].sort((a: any, b: any) => {
      const order: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 }
      return (order[a.level] ?? 3) - (order[b.level] ?? 3)
    })
    severityChart.setOption({
      yAxis: { data: ordered.map((s: any) => sevLabel[s.level] || s.level) },
      series: [{
        data: ordered.map((s: any) => ({
          value: s.count,
          itemStyle: { color: sevColors[s.level] || "#6ab04c", borderRadius: [0, 4, 4, 0] }
        }))
      }]
    })
  }
}
function refreshMapData() {
  loadStats()
  loadDiseaseData()
  loadRoadDiseaseData()
  loadMapAnalysisData()
}
function exportReport() { selectedDisease.value = null; showCapacity.value = false }


function startTimeUpdate() {
  const update = () => {
    const n = new Date()
    currentTime.value = n.getFullYear() + "-" + String(n.getMonth() + 1).padStart(2, "0") + "-" + String(n.getDate()).padStart(2, "0") + " " + String(n.getHours()).padStart(2, "0") + ":" + String(n.getMinutes()).padStart(2, "0") + ":" + String(n.getSeconds()).padStart(2, "0")
  }
  update()
  timeInterval = setInterval(update, 1000)
}

let mapReady = false
function waitForMap(cb: () => void, attempts = 30) {
  if (map) { cb(); return }
  if (attempts <= 0) { console.error("Map not ready after 30 attempts"); return }
  setTimeout(() => waitForMap(cb, attempts - 1), 200)
}

async function loadStats() {
  try {
    const [dashR, trendR, typeR, sevR] = await Promise.all([
      statisticsApi.getDashboard(),
      statisticsApi.getTrend(),
      statisticsApi.getCrackType(),
      statisticsApi.getSeverity()
    ])
    const d = dashR.data.data
    if (d) {
      totalRoad.value = d.totalRoads || 0
      crackCount.value = d.totalCracksDetected || 0
      // totalWorkOrders 是工单总数（含所有状态），不等于已修复数
      // 已修复数由 mapApi.statistics() 接口返回（status=CLOSED 的工单数），在 loadMapAnalysisData 中更新
      alertCount.value = d.pendingAlerts || 0
    }
    // 缓存统计数据供图表延迟初始化时使用
    realStats.value = {
      dashboard: d,
      trend: trendR.data.data || [],
      crackTypes: typeR.data.data || [],
      severity: sevR.data.data || [],
    }
    // 如果图表已经初始化（用户在分析面板），直接更新
    if (trendR.data.data?.length && trendChart) {
      const trend = trendR.data.data
      trendChart.setOption({
        xAxis: { data: trend.map((t: any) => t.date?.slice(5) || "") },
        series: [{ data: trend.map((t: any) => t.count) }]
      })
    }
    if (typeR.data.data?.length && pieChart) {
      const types = typeR.data.data
      const colors = ["#4361ee","#f72585","#06d6a0","#ffd166","#7209b7","#118ab2"]
      pieChart.setOption({
        series: [{
          data: types.map((t: any, i: number) => ({
            name: damageTypeLabel(t.type), value: t.count,
            itemStyle: { color: colors[i % colors.length] }
          }))
        }]
      })
    }
    if (sevR.data.data?.length && severityChart) {
      const sv = sevR.data.data
      const sevColors: Record<string, string> = { HIGH: "#ef4444", MEDIUM: "#f59e0b", LOW: "#22c55e" }
      const sevLabel: Record<string, string> = { HIGH: "严重", MEDIUM: "中等", LOW: "轻微" }
      const ordered = [...sv].sort((a: any, b: any) => {
        const order: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 }
        return (order[a.level] ?? 3) - (order[b.level] ?? 3)
      })
      severityChart.setOption({
        yAxis: { data: ordered.map((s: any) => sevLabel[s.level] || s.level) },
        series: [{
          data: ordered.map((s: any) => ({
            value: s.count,
            itemStyle: { color: sevColors[s.level] || "#6ab04c", borderRadius: [0, 4, 4, 0] }
          }))
        }]
      })
    }
  } catch {}
}

async function loadDiseaseData() {
  try {
    const res = await detectionApi.list({ status: DetectionTaskStatus.COMPLETED, size: 100 })
    const tasks = res.data.data?.records || []
    diseaseList.value = tasks
    console.log("loadDiseaseData: got", tasks.length, "completed tasks")

    // 旧版小圆点标记已下线：病害点现在统一由 loadRoadDiseaseData 用兰图绘风格水滴标记渲染
    waitForMap(() => {
      mapMarkers.forEach((m) => { try { map?.remove(m) } catch(e) {} })
      mapMarkers.length = 0
    })
  } catch(e) {
    console.error("loadDiseaseData error:", e)
  }
}

/**
 * 兰图绘风格：病害点标记 + 辐射范围
 * 颜色按病害严重等级区分
 */
const MARKER_COLORS: Record<string, { fill: string; stroke: string; ring: string; label: string; radius: number }> = {
  HIGH:     { fill: "#ef4444", stroke: "#b91c1c", ring: "rgba(239,68,68,0.35)",  label: "严重", radius: 1000 },
  MEDIUM:   { fill: "#f59e0b", stroke: "#b45309", ring: "rgba(245,158,11,0.30)", label: "中等", radius: 700  },
  LOW:      { fill: "#22c55e", stroke: "#15803d", ring: "rgba(34,197,94,0.28)",  label: "轻微", radius: 500  },
  REPAIRED: { fill: "#3b82f6", stroke: "#1d4ed8", ring: "rgba(59,130,246,0.30)", label: "已维修", radius: 600 },
}

/**
 * 道路名展示：后端已返回真实道路名（来自 road 表），这里只做兜底显示。
 */
function roadNameCn(name: string): string {
  if (!name) return "未知道路"
  if (name === "未知道路" && !roadNameResolved.value) return "解析中..."
  return name
}

/**
 * 批量逆地理编码器实例（全局单例，复用 Geocoder 实例）
 * 配置说明：
 *  - maxConcurrency: 4 — 避免触发高德 QPS 限制（浏览器 HTTP/1.1 同域名最大 6 并发）
 *  - cacheTTL: 30min — 同坐标缓存有效期
 *  - maxRetries: 2 — 失败自动重试
 *  - timeout: 8000ms — 单次请求超时
 */
const batchGeocoderOptions: BatchGeocoderOptions = {
  maxConcurrency: 4,
  maxCacheSize: 2000,
  cacheTTL: 30 * 60 * 1000,
  maxRetries: 2,
  retryBaseDelay: 1000,
  timeout: 8000,
  city: '全国',
  radius: 1000,
}
let batchGeocoder = getBatchGeocoder(batchGeocoderOptions)

/**
 * 调用批量逆地理编码获取坐标所在的真实道路名（使用全局单例，避免每次 new Geocoder）
 */
function reverseGeocodeRoad(lng: number, lat: number): Promise<string> {
  return batchGeocoder.getRoadName(lng, lat)
}

/**
 * 获取逆地理缓存中已解析的道路名（同步方法，用于 click 事件等场景）
 */
function getCachedRoadName(lng: number, lat: number): string {
  const key = `${lng.toFixed(6)},${lat.toFixed(6)}`
  // 通过内部缓存直接获取
  // 注意：这里我们依赖 batchGeocoder 的内部 LRU 缓存
  // 如果未缓存，返回空字符串，由异步更新填充
  return '' // 由异步回调更新
}

/**
 * 兼容旧的 geocodeCache 引用（用于 click 事件中获取已缓存的路径名）
 * 现在由 batchGeocoder 内部的 LRU 缓存提供数据
 */
const geocodeCache = {
  _fallback: new Map<string, string>(),
  get(key: string): string | undefined {
    // 优先从本地同步缓存读取（label 更新时写入）
    return this._fallback.get(key)
  },
  set(key: string, value: string): void {
    this._fallback.set(key, value)
    // 限制同步缓存大小，防止内存泄漏
    if (this._fallback.size > 5000) {
      const firstKey = this._fallback.keys().next().value
      if (firstKey) this._fallback.delete(firstKey)
    }
  },
}

/**
 * 生成自定义水滴标记的 SVG 字符串
 */
/**
 * 生成水滴形标记的 SVG data URL
 * - 保留经典水滴外观，含阴影 + 白色内圆
 * - 用 encodeURIComponent 编码以支持 AMap.Icon image 字段
 *
 * @param fill   主色（实心填充）
 * @param stroke 描边色（深色，用于内圆描边以增强对比）
 */
function createPinIcon(fill: string, stroke: string): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="30" height="40" viewBox="0 0 30 40">
    <defs>
      <filter id="ds" x="-30%" y="-20%" width="160%" height="140%">
        <feDropShadow dx="0" dy="2" stdDeviation="2.5" flood-color="rgba(0,0,0,0.32)"/>
      </filter>
    </defs>
    <path fill="${fill}" d="M15 38C5 27 0 21 0 13C0 5.8 6.7 0 15 0C23.3 0 30 5.8 30 13C30 21 25 27 15 38Z" filter="url(#ds)"/>
    <circle cx="15" cy="13" r="6" fill="#fff"/>
    <circle cx="15" cy="13" r="6" fill="none" stroke="${stroke}" stroke-width="1.2" stroke-opacity="0.55"/>
  </svg>`
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

/**
 * 生成圆框道路信息标签的 HTML
 * - 药丸形（border-radius: 999px），与水滴同一色系
 * - 左侧小色块 + 道路名文字
 * - 阴影 + 半透明白底，保证在地图上的可读性
 *
 * @param roadName 道路名（"加载中..." 或真实名称）
 * @param fill     等级主色
 * @param stroke   等级描边色
 */
function createRoadLabel(roadName: string, fill: string, stroke: string): string {
  const safe = (roadName || "未知道路").replace(/</g, "&lt;").replace(/>/g, "&gt;")
  return `<div class="ds-road-label" style="
    display:inline-flex;align-items:center;gap:5px;
    background:rgba(255,255,255,0.96);
    border:1.5px solid ${fill};
    border-radius:999px;
    padding:3px 10px 3px 6px;
    font-size:11px;font-weight:600;
    color:#1e293b;
    box-shadow:0 2px 8px rgba(0,0,0,0.12);
    white-space:nowrap;max-width:150px;
    overflow:hidden;text-overflow:ellipsis;
    line-height:1.4;
  "><span style="
    display:inline-block;width:7px;height:7px;border-radius:50%;
    background:${fill};border:1px solid ${stroke};flex-shrink:0;
  "></span><span style="overflow:hidden;text-overflow:ellipsis;">${safe}</span></div>`
}

/**
 * 加载道路病害数据，并用兰图绘风格渲染：
 * 每个病害点 → 水滴标记 + 半透明辐射圆
 */
async function loadRoadDiseaseData() {
  try {
    const res = await mapApi.roadsWithDisease()
    const roads = res.data.data || []
    roadDiseaseData.value = roads
    console.log("loadRoadDiseaseData: got", roads.length, "roads")

    // ====== 核心修复：对"未知道路"条目用逆地理重新分组聚合 ======
    // 问题：后端按 road_id 聚合，当 road_id 为 NULL 时返回 roadName="未知道路"
    // 修复：前端拿到这些条目后，用其 diseasePoints 坐标批量逆地理，
    //       按真实路名重新分组，消除"未知道路"分类
    roadNameResolved.value = false

    const unknownRoads = roads.filter((r: any) => !r.roadName || r.roadName === "未知道路")
    const knownRoads = roads.filter((r: any) => r.roadName && r.roadName !== "未知道路")

    if (unknownRoads.length > 0) {
      console.log("loadRoadDiseaseData: found", unknownRoads.length, "unknown roads, resolving via geocode...")

      // 收集所有未知道路的病害点坐标（去重）
      const coordToDp = new Map<string, { dp: any; sourceRoad: any }>()
      for (const road of unknownRoads) {
        const dps = road.diseasePoints || []
        for (const dp of dps) {
          if (!dp.lng || !dp.lat) continue
          const key = `${Number(dp.lng).toFixed(6)},${Number(dp.lat).toFixed(6)}`
          if (!coordToDp.has(key)) {
            coordToDp.set(key, { dp, sourceRoad: road })
          }
        }
      }

      // 批量逆地理获取真实路名
      const points = Array.from(coordToDp.entries()).map(([key, val]) => ({
        lng: val.dp.lng,
        lat: val.dp.lat,
        key,
      }))

      try {
        const roadNameMap = await batchGeocoder.getRoadNames(points.map(p => ({ lng: p.lng, lat: p.lat })))

        // 按真实路名重新分组
        const regrouped = new Map<string, {
          roadName: string
          totalCount: number
          highCount: number
          mediumCount: number
          lowCount: number
          diseasePoints: any[]
          sumLng: number
          sumLat: number
          coordCount: number
        }>()

        for (const [key, val] of coordToDp) {
          const realName = roadNameMap.get(key)
          const effectiveName = (realName && realName !== "未知道路") ? realName : null

          if (realName) {
            const group = regrouped.get(realName) || {
              roadName: realName,
              totalCount: 0,
              highCount: 0,
              mediumCount: 0,
              lowCount: 0,
              diseasePoints: [],
              sumLng: 0,
              sumLat: 0,
              coordCount: 0,
            }
            const dp = val.dp
            group.totalCount++
            const sev = dp.severity || "MEDIUM"
            if (sev === "HIGH") group.highCount++
            else if (sev === "MEDIUM") group.mediumCount++
            else group.lowCount++
            group.diseasePoints.push(dp)
            group.sumLng += Number(dp.lng)
            group.sumLat += Number(dp.lat)
            group.coordCount++
            regrouped.set(realName, group)
          }
        }

        console.log("loadRoadDiseaseData: regrouped unknown roads into", regrouped.size, "named roads")

        // 将重新分组的道路合并到已知道路中
        const regroupedRoads: any[] = []
        for (const [name, group] of regrouped) {
          const overall = group.highCount > 0 ? "HIGH" : group.mediumCount > 0 ? "MEDIUM" : "LOW"
          regroupedRoads.push({
            roadId: -(regroupedRoads.length + 1), // 负 ID 表示前端动态分组
            roadName: name,
            centerLng: group.coordCount > 0 ? group.sumLng / group.coordCount : 0,
            centerLat: group.coordCount > 0 ? group.sumLat / group.coordCount : 0,
            totalCount: group.totalCount,
            highCount: group.highCount,
            mediumCount: group.mediumCount,
            lowCount: group.lowCount,
            overallSeverity: overall,
            diseasePoints: group.diseasePoints,
          })
        }

        // 合并：已知道路 + 重新分组后的道路
        roadDiseaseData.value = [...knownRoads, ...regroupedRoads]
        console.log("loadRoadDiseaseData: final road count", roadDiseaseData.value.length,
          "(known:", knownRoads.length, "regrouped:", regroupedRoads.length, ")")
      } catch (geocodeErr) {
        console.warn("loadRoadDiseaseData: geocode regroup failed, keeping original data", geocodeErr)
      }
    }

    roadNameResolved.value = true

    // 异步为道路名称缺失的条目兜底获取真实路名（保留作为二次兜底）
    for (const road of roadDiseaseData.value) {
      (road as any)._roadNameReady = false
      if (!road.roadName || road.roadName === "未知道路") {
        if (road.centerLng && road.centerLat) {
          reverseGeocodeRoad(road.centerLng, road.centerLat).then((realName) => {
            if (realName && (!road.roadName || road.roadName === "未知道路")) {
              road.roadName = realName
            }
            (road as any)._roadNameReady = true
            roadDiseaseData.value = [...roadDiseaseData.value]
          })
        } else {
          (road as any)._roadNameReady = true
        }
      } else {
        (road as any)._roadNameReady = true
      }
    }

    // 各等级图层数字在下方去重计算后更新

    waitForMap(async () => {
      // 清除旧的标记和辐射圆
      roadPolylines.forEach((pl) => { try { map?.remove(pl) } catch(e) {} })
      roadPolylines.length = 0

      // 使用已合并的道路数据（包含逆地理重新分组后的道路）
      const currentRoads = roadDiseaseData.value || []
      if (currentRoads.length === 0) {
        console.log("loadRoadDiseaseData: no roads to display")
        return
      }

      // 后端已按 task/result_item 的 road_id 真实关联道路，roadName 即为真实道路名。
      // 前端这里拍平所有病害点用于地图渲染，并在地图上通过逆地理编码显示每个点的真实路名。
      const sevRank: Record<string, number> = { HIGH: 3, MEDIUM: 2, LOW: 1, UNKNOWN: 0 }
      const coordMap = new Map<string, any>()
      for (const road of currentRoads) {
        const dps = road.diseasePoints || []
        for (const dp of dps) {
          if (!dp.lng || !dp.lat) continue
          const key = `${dp.lng.toFixed(6)},${dp.lat.toFixed(6)}`
          const existing = coordMap.get(key)
          if (!existing) {
            coordMap.set(key, dp)
          } else {
            const existingSev = existing.severity || "MEDIUM"
            const currentSev = dp.severity || "MEDIUM"
            if ((sevRank[currentSev] ?? 0) > (sevRank[existingSev] ?? 0)) {
              coordMap.set(key, dp)
            }
          }
        }
      }
      const dedupedDps = Array.from(coordMap.values())
      console.log("loadRoadDiseaseData: deduped to", dedupedDps.length, "unique points")

      // === 筛选条件过滤 ===
      // 1. 严重程度：filterSeverity 值为 HIGH / MEDIUM / LOW（空=全部）
      // 2. 检测日期：filterDate 格式 yyyy-mm-dd，匹配 detectionTime 的日期部分
      // 3. 修复状态：filterStatus 值为 PENDING / ASSIGNED / COMPLETED
      // 4. 默认不显示已修复的病害点（可通过工单状态筛选显示）
      const filteredDps = dedupedDps.filter((dp: any) => {
        const woStatus = normalizeWoStatus(dp.workOrderNo)
        // 严重程度筛选
        if (filterSeverity.value) {
          const dpSev = dp.severity || "MEDIUM"
          if (dpSev !== filterSeverity.value) return false
        }
        // 检测日期筛选
        if (filterDate.value) {
          const dpTime = dp.detectionTime || ""
          const dpDate = dpTime.substring(0, 10)
          if (dpDate !== filterDate.value) return false
        }
        // 修复状态筛选：默认显示全部（包括已修复），只有选择特定状态时才过滤
        if (filterStatus.value) {
          if (filterStatus.value === "PENDING" && woStatus !== "pending") return false
          if (filterStatus.value === "ASSIGNED" && woStatus !== "assigned") return false
          if (filterStatus.value === "COMPLETED" && woStatus !== "completed") return false
        }
        return true
      })
      console.log("loadRoadDiseaseData: filtered to", filteredDps.length, "points (from", dedupedDps.length, ")")

      // 按严重等级统计数量（基于筛选后数据，且只统计未修复的点）
      const sevCounts: Record<string, number> = { HIGH: 0, MEDIUM: 0, LOW: 0 }
      for (const dp of filteredDps) {
        const woStatus = normalizeWoStatus(dp.workOrderNo)
        // 病害标注点只统计未修复的点
        if (woStatus === "completed") continue
        const sev = dp.severity || "MEDIUM"
        if (sevCounts[sev] !== undefined) sevCounts[sev]++
      }
      const highLayer = layers.value.find(l => l.id === "sev_high")
      if (highLayer) highLayer.count = sevCounts.HIGH
      const mediumLayer = layers.value.find(l => l.id === "sev_medium")
      if (mediumLayer) mediumLayer.count = sevCounts.MEDIUM
      const lowLayer = layers.value.find(l => l.id === "sev_low")
      if (lowLayer) lowLayer.count = sevCounts.LOW

      // 按修复状态统计当前地图可见点（用于侧边栏工单状态）
      const woCounts = { pending: 0, assigned: 0, completed: 0 }
      for (const dp of dedupedDps) {
        const status = normalizeWoStatus(dp.workOrderNo)
        woCounts[status]++
      }
      workOrderStatusCounts.value = woCounts

      const allMarkers: any[] = []

      for (const dp of filteredDps) {
        const sev = dp.severity || "MEDIUM"
        const woStatus = normalizeWoStatus(dp.workOrderNo)
        const isRepaired = woStatus === "completed"
        const colorInfo = isRepaired ? MARKER_COLORS.REPAIRED : (MARKER_COLORS[sev] || MARKER_COLORS.MEDIUM)

        // 1. 辐射范围：双层同心圆（外圈虚线 + 内圈实心），半径随等级区分
        const baseRadius = (dp as any).radius ? (dp as any).radius : colorInfo.radius
        const outerRing = new window.AMap.Circle({
          center: [dp.lng, dp.lat],
          radius: baseRadius * 1.15,
          fillColor: "transparent",
          fillOpacity: 0,
          strokeColor: colorInfo.fill,
          strokeWeight: 1.2,
          strokeOpacity: 0.55,
          strokeStyle: "dashed",
          zIndex: 48,
          extData: { dp, colorInfo, _sevLevel: isRepaired ? "REPAIRED" : sev, _woStatus: woStatus, _ringType: "outer" },
        })
        map?.add(outerRing)
        roadPolylines.push(outerRing)

        const innerFill = new window.AMap.Circle({
          center: [dp.lng, dp.lat],
          radius: baseRadius,
          fillColor: colorInfo.fill,
          fillOpacity: 0.16,
          strokeColor: colorInfo.fill,
          strokeWeight: 1,
          strokeOpacity: 0.45,
          zIndex: 50,
          extData: { dp, colorInfo, _sevLevel: isRepaired ? "REPAIRED" : sev, _woStatus: woStatus, _ringType: "inner" },
        })
        map?.add(innerFill)
        roadPolylines.push(innerFill)

        // 2. 位置符号：经典水滴标记（固定 30×40 像素 SVG，含阴影）
        const icon = new window.AMap.Icon({
          image: createPinIcon(colorInfo.fill, colorInfo.stroke),
          size: new window.AMap.Size(30, 40),
          imageSize: new window.AMap.Size(30, 40),
          imageOffset: new window.AMap.Pixel(0, 0),
        })
        const marker = new window.AMap.Marker({
          position: [dp.lng, dp.lat],
          icon,
          // anchor="center" 时图标几何中心严格落在 (lng,lat)，
          // 与 AMap.Circle 圆心重合，缩放时不会错位
          offset: new window.AMap.Pixel(0, 0),
          anchor: "center",
          zIndex: 100,
          extData: { dp, colorInfo, _sevLevel: isRepaired ? "REPAIRED" : sev, _woStatus: woStatus },
        })

        // 文字标签：用 AMap.Marker + DOM content 实现药丸标签，
        // 避免 AMap.Text 默认外层白底/边框/阴影造成"白框套白框"的冗余
        const labelEl = document.createElement("div")
        labelEl.innerHTML = createRoadLabel("加载中...", colorInfo.fill, colorInfo.stroke)
        const label = new window.AMap.Marker({
          position: [dp.lng, dp.lat],
          content: labelEl,
          // 标签高度约 22px，向上偏移 11px 让它垂直居中于水滴几何中心
          offset: new window.AMap.Pixel(20, -11),
          anchor: "middle-left",
          zIndex: 110,
          extData: { _sevLevel: isRepaired ? "REPAIRED" : sev, _woStatus: woStatus },
        })

        // 异步获取真实道路名
        reverseGeocodeRoad(dp.lng, dp.lat).then((roadName) => {
          // 同步缓存到 geocodeCache（供 click 事件同步读取）
          geocodeCache.set(`${dp.lng.toFixed(6)},${dp.lat.toFixed(6)}`, roadName)
          labelEl.innerHTML = createRoadLabel(roadName, colorInfo.fill, colorInfo.stroke)
        })

        marker.on('click', () => {
          const roadName = geocodeCache.get(`${dp.lng.toFixed(6)},${dp.lat.toFixed(6)}`) || "未知道路"
          // 优先使用 AI 识别结果图（imageBase64），其次使用原始上传图（fileUrl）
          let imageSrc = resolveImgSrc(dp.imageBase64, dp.fileUrl)
          const isRepaired = isRepairedStatus(dp.workOrderNo)
          const woStatus = normalizeWoStatus(dp.workOrderNo)
          selectedDisease.value = {
            type: dp.damageType || '道路病害',
            severity: isRepaired ? '已维修' : (sev === 'HIGH' ? '严重' : sev === 'MEDIUM' ? '中等' : '轻微'),
            taskCode: dp.taskCode || '',
            workOrderCode: dp.workOrderCode || '',
            workOrderStatus: woStatus,
            time: dp.detectionTime || '--',
            location: roadName,
            confidence: dp.confidence ? (dp.confidence * 100).toFixed(1) + '%' : '--',
            size: dp.bbox || '--',
            orderId: dp.workOrderNo || '未生成',
            _roadName: roadName,
            _isRoad: false,
            _loading: false,
            _imageSrc: imageSrc,
          }
        })

        innerFill.on('click', () => {
          const roadName = geocodeCache.get(`${dp.lng.toFixed(6)},${dp.lat.toFixed(6)}`) || "未知道路"
          let imageSrc = resolveImgSrc(dp.imageBase64, dp.fileUrl)
          const isRepaired = isRepairedStatus(dp.workOrderNo)
          const woStatus = normalizeWoStatus(dp.workOrderNo)
          selectedDisease.value = {
            type: dp.damageType || '道路病害',
            severity: isRepaired ? '已维修' : (sev === 'HIGH' ? '严重' : sev === 'MEDIUM' ? '中等' : '轻微'),
            taskCode: dp.taskCode || '',
            workOrderCode: dp.workOrderCode || '',
            workOrderStatus: woStatus,
            time: dp.detectionTime || '--',
            location: roadName,
            confidence: dp.confidence ? (dp.confidence * 100).toFixed(1) + '%' : '--',
            size: dp.bbox || '--',
            orderId: dp.workOrderNo || '未生成',
            _roadName: roadName,
            _isRoad: false,
            _loading: false,
            _imageSrc: imageSrc,
          }
        })

        map?.add(marker)
        map?.add(label)
        roadPolylines.push(marker)
        roadPolylines.push(label)
        allMarkers.push(marker)
      }

      // 自适应视野到所有标记（只在首次加载时执行，后续保持用户缩放状态）
      if (allMarkers.length > 0 && !savedMapState) {
        try { map?.setFitView(allMarkers, false, [80, 80, 80, 80]) } catch(e) {}
      }

      console.log("Drew", roadPolylines.length, "disease markers and circles")
    })
  } catch(e) {
    console.warn("[DiseaseLayer] 加载道路病害数据失败:", e)
  }
}

function normalizeWoStatus(woNo?: string): "pending" | "assigned" | "completed" {
  const no = (woNo || "").trim().toUpperCase()
  if (!no || no === "PENDING_ASSIGNMENT") return "pending"
  if (["ASSIGNED", "IN_PROGRESS", "PENDING_DEPT_REVIEW", "PENDING_ADMIN_REVIEW", "REJECTED"].includes(no)) return "assigned"
  return "completed"
}

function isRepairedStatus(woNo?: string): boolean {
  return normalizeWoStatus(woNo) === "completed"
}

function toggleLayerVisibility(layerId: string) {
  const layer = layers.value.find(l => l.id === layerId)
  if (!layer) return
  layer.visible = !layer.visible

  // layerId 格式: sev_high / sev_medium / sev_low
  const sevLevel = layerId.replace("sev_", "").toUpperCase()
  roadPolylines.forEach((pl: any) => {
    const extData = pl.getExtData?.() || {}
    if (extData._sevLevel === sevLevel) {
      if (layer.visible) { try { pl.show() } catch(e) {} }
      else { try { pl.hide() } catch(e) {} }
    }
  })
}

function toggleWorkOrderVis(status: string) {
  const vis = workOrderVis.value as any
  vis[status] = !vis[status]
  roadPolylines.forEach((pl: any) => {
    const extData = pl.getExtData?.() || {}
    if (extData._woStatus === status) {
      if (vis[status]) { try { pl.show() } catch(e) {} }
      else { try { pl.hide() } catch(e) {} }
    }
  })
}

// --- 筛选条件变化时重新渲染地图标记 ---
// 不重新请求后端数据，只对已有数据重新过滤并重绘标记
watch([filterSeverity, filterDate, filterStatus], () => {
  if (!map) return
  // 清除旧标记
  roadPolylines.forEach((pl) => { try { map?.remove(pl) } catch(e) {} })
  roadPolylines.length = 0

  const roads = roadDiseaseData.value || []
  if (roads.length === 0) return

  waitForMap(async () => {
    // 重新执行去重 + 筛选（与 loadRoadDiseaseData 内部逻辑一致）
    const sevRank: Record<string, number> = { HIGH: 3, MEDIUM: 2, LOW: 1, UNKNOWN: 0 }
    const coordMap = new Map<string, any>()
    for (const road of roads) {
      for (const dp of (road.diseasePoints || [])) {
        if (!dp.lng || !dp.lat) continue
        const key = `${dp.lng.toFixed(6)},${dp.lat.toFixed(6)}`
        const existing = coordMap.get(key)
        if (!existing) coordMap.set(key, dp)
        else if ((sevRank[dp.severity || "MEDIUM"] ?? 0) > (sevRank[existing.severity || "MEDIUM"] ?? 0))
          coordMap.set(key, dp)
      }
    }
    const dedupedDps = Array.from(coordMap.values())

    // 应用筛选
    const filteredDps = dedupedDps.filter((dp: any) => {
        const woStatus = normalizeWoStatus(dp.workOrderNo)
        // 默认过滤掉已修复的点
        if (woStatus === "completed" && !filterStatus.value) return false
        if (filterSeverity.value && (dp.severity || "MEDIUM") !== filterSeverity.value) return false
        if (filterDate.value && (dp.detectionTime || "").substring(0, 10) !== filterDate.value) return false
        if (filterStatus.value) {
          if (filterStatus.value === "PENDING" && woStatus !== "pending") return false
          if (filterStatus.value === "ASSIGNED" && woStatus !== "assigned") return false
          if (filterStatus.value === "COMPLETED" && woStatus !== "completed") return false
        }
        return true
      })

    // 更新图层数字（只统计未修复的点）
    const sevCounts: Record<string, number> = { HIGH: 0, MEDIUM: 0, LOW: 0 }
    for (const dp of filteredDps) {
      const sev = dp.severity || "MEDIUM"
      if (sevCounts[sev] !== undefined) sevCounts[sev]++
    }
    const hl = layers.value.find(l => l.id === "sev_high"); if (hl) hl.count = sevCounts.HIGH
    const ml = layers.value.find(l => l.id === "sev_medium"); if (ml) ml.count = sevCounts.MEDIUM
    const ll = layers.value.find(l => l.id === "sev_low"); if (ll) ll.count = sevCounts.LOW

    // 更新工单状态统计（基于所有点，不只是筛选后的）
    const woCounts = { pending: 0, assigned: 0, completed: 0 }
    for (const dp of dedupedDps) {
      const status = normalizeWoStatus(dp.workOrderNo)
      woCounts[status]++
    }
    workOrderStatusCounts.value = woCounts

    const allMarkers: any[] = []
    for (const dp of filteredDps) {
      const sev = dp.severity || "MEDIUM"
      const woStatus = normalizeWoStatus(dp.workOrderNo)
      const isRepaired = woStatus === "completed"
      const colorInfo = isRepaired ? MARKER_COLORS.REPAIRED : (MARKER_COLORS[sev] || MARKER_COLORS.MEDIUM)

      const baseRadius = (dp as any).radius ? (dp as any).radius : colorInfo.radius
      const outerRing = new window.AMap.Circle({
        center: [dp.lng, dp.lat],
        radius: baseRadius * 1.15,
        fillColor: "transparent",
        fillOpacity: 0,
        strokeColor: colorInfo.fill,
        strokeWeight: 1.2,
        strokeOpacity: 0.55,
        strokeStyle: "dashed",
        zIndex: 48,
        extData: { dp, colorInfo, _sevLevel: sev, _woStatus: woStatus, _ringType: "outer" },
      })
      map?.add(outerRing)
      roadPolylines.push(outerRing)

      const innerFill = new window.AMap.Circle({
        center: [dp.lng, dp.lat],
        radius: baseRadius,
        fillColor: colorInfo.fill,
        fillOpacity: 0.16,
        strokeColor: colorInfo.fill,
        strokeWeight: 1,
        strokeOpacity: 0.45,
        zIndex: 50,
        extData: { dp, colorInfo, _sevLevel: sev, _woStatus: woStatus, _ringType: "inner" },
      })
      map?.add(innerFill)
      roadPolylines.push(innerFill)

      const icon = new window.AMap.Icon({
        image: createPinIcon(colorInfo.fill, colorInfo.stroke),
        size: new window.AMap.Size(30, 40),
        imageSize: new window.AMap.Size(30, 40),
        imageOffset: new window.AMap.Pixel(0, 0),
      })
      const marker = new window.AMap.Marker({
        position: [dp.lng, dp.lat],
        icon,
        // anchor="center" 时图标几何中心严格落在 (lng,lat)，
        // 与 AMap.Circle 圆心重合，缩放时不会错位
        offset: new window.AMap.Pixel(0, 0),
        anchor: "center",
        zIndex: 100,
        extData: { dp, colorInfo, _sevLevel: sev, _woStatus: woStatus },
      })

      const labelEl = document.createElement("div")
      labelEl.innerHTML = createRoadLabel("加载中...", colorInfo.fill, colorInfo.stroke)
      const label = new window.AMap.Marker({
        position: [dp.lng, dp.lat],
        content: labelEl,
        offset: new window.AMap.Pixel(20, -11),
        anchor: "middle-left",
        zIndex: 110,
        extData: { _sevLevel: sev, _woStatus: woStatus },
      })

      // 逆地理编码获取真实路名（利用缓存，不会重复请求）
      reverseGeocodeRoad(dp.lng, dp.lat).then((roadName) => {
        // 同步缓存到 geocodeCache（供 click 事件同步读取）
        geocodeCache.set(`${dp.lng.toFixed(6)},${dp.lat.toFixed(6)}`, roadName)
        labelEl.innerHTML = createRoadLabel(roadName, colorInfo.fill, colorInfo.stroke)
      })

        marker.on('click', () => {
          const roadName = geocodeCache.get(`${dp.lng.toFixed(6)},${dp.lat.toFixed(6)}`) || "未知道路"
          let imageSrc = resolveImgSrc(dp.imageBase64, dp.fileUrl)
          const woStatus = normalizeWoStatus(dp.workOrderNo)
          const isRepaired = woStatus === "completed"
          selectedDisease.value = {
            type: dp.damageType || '道路病害',
            severity: isRepaired ? '已维修' : (sev === 'HIGH' ? '严重' : sev === 'MEDIUM' ? '中等' : '轻微'),
            taskCode: dp.taskCode || '',
            workOrderCode: dp.workOrderCode || '',
            workOrderStatus: woStatus,
            time: dp.detectionTime || '--',
            location: roadName,
            confidence: dp.confidence ? (dp.confidence * 100).toFixed(1) + '%' : '--',
            size: dp.bbox || '--',
            orderId: dp.workOrderNo || '未生成',
            _roadName: roadName,
            _isRoad: false,
            _loading: false,
            _imageSrc: imageSrc,
          }
        })

      innerFill.on('click', () => {
        const roadName = geocodeCache.get(`${dp.lng.toFixed(6)},${dp.lat.toFixed(6)}`) || "未知道路"
        let imageSrc = resolveImgSrc(dp.imageBase64, dp.fileUrl)
        const woStatus = normalizeWoStatus(dp.workOrderNo)
        const isRepaired = woStatus === "completed"
        selectedDisease.value = {
          type: dp.damageType || '道路病害',
          severity: isRepaired ? '已维修' : (sev === 'HIGH' ? '严重' : sev === 'MEDIUM' ? '中等' : '轻微'),
          taskCode: dp.taskCode || '',
          workOrderCode: dp.workOrderCode || '',
          workOrderStatus: woStatus,
          time: dp.detectionTime || '--',
          location: roadName,
          confidence: dp.confidence ? (dp.confidence * 100).toFixed(1) + '%' : '--',
          size: dp.bbox || '--',
          orderId: dp.workOrderNo || '未生成',
          _roadName: roadName,
          _isRoad: false,
          _loading: false,
          _imageSrc: imageSrc,
        }
      })

      map?.add(marker)
      map?.add(label)
      roadPolylines.push(marker)
      roadPolylines.push(label)
      allMarkers.push(marker)

      // 应用图层可见性（如果某等级被关闭了，新绘制的标记也要隐藏）
      const layerId = `sev_${sev.toLowerCase()}`
      const layer = layers.value.find(l => l.id === layerId)
      if (layer && !layer.visible) {
        try { outerRing.hide() } catch(e) {}
        try { innerFill.hide() } catch(e) {}
        try { marker.hide() } catch(e) {}
        try { label.hide() } catch(e) {}
      }
    }

    if (allMarkers.length > 0 && !savedMapState) {
      try { map?.setFitView(allMarkers, false, [80, 80, 80, 80]) } catch(e) {}
    }
  })
})

// --- Analysis Tab Helpers ---
function damageTypeLabel(t: string) {
  return ({ CRACK: "裂缝", TRANSVERSE_CRACK: "横向裂缝", LONGITUDINAL_CRACK: "纵向裂缝", NET_CRACK: "网状裂缝", POTHOLE: "坑槽", MARKING_DAMAGE: "标线损坏", ROAD_SPILL: "路面抛洒", UNKNOWN: "未知" } as any)[t] || t || "--"
}
function healthColor(s: string) {
  return ({ HIGH: "#ef4444", MEDIUM: "#f59e0b", LOW: "#22c55e" } as any)[s] || "#94a3b8"
}
function healthBg(s: string) {
  return ({ HIGH: "#fef2f2", MEDIUM: "#fffbeb", LOW: "#f0fdf4" } as any)[s] || "#f8f9fc"
}
function healthLabel(s: string) {
  return ({ HIGH: "严重", MEDIUM: "中等", LOW: "良好" } as any)[s] || "未知"
}
function sevDotColor(s: string) {
  return ({ HIGH: "#ef4444", MEDIUM: "#f59e0b", LOW: "#22c55e" } as any)[s] || "#94a3b8"
}
function sevBgColor(s: string) {
  return ({ HIGH: "#fef2f2", MEDIUM: "#fffbeb", LOW: "#f0fdf4" } as any)[s] || "#f8f9fc"
}
function sevTextColor(s: string) {
  return ({ HIGH: "#ef4444", MEDIUM: "#d97706", LOW: "#16a34a" } as any)[s] || "#64748b"
}

async function loadMapAnalysisData() {
  try {
    const [statsR, typesR, markersR] = await Promise.allSettled([
      mapApi.statistics(),
      mapApi.damageTypes(),
      mapApi.markers({ onlyWithCoordinates: true }),
    ])
    if (statsR.status === "fulfilled") {
      mapStatsData.value = statsR.value.data.data
      // 同步更新左上角浮动统计区的"已修复"数字
      const stats = statsR.value.data.data
      if (stats) {
        repairedCount.value = stats.repairedCount || 0
      }
    }
    if (typesR.status === "fulfilled") {
      damageTypeRatios.value = typesR.value.data.data || []
    }
    if (markersR.status === "fulfilled") {
      const markers = markersR.value.data.data || []
      // 只展示严重和中等的标记作为告警
      const alerts = markers.filter((m: any) => m.severity === "HIGH" || m.severity === "MEDIUM")
      // 为每个告警标记异步获取真实道路名（利用逆地理编码缓存）
      alertMarkers.value = alerts.map((m: any) => ({
        ...m,
        roadName: m.roadName || "",  // 先置空，逆地理编码返回后更新
        _roadNameLoading: true,
      }))
      // 异步逆地理编码获取路名
      for (const am of alertMarkers.value) {
        if (am.longitude && am.latitude) {
          reverseGeocodeRoad(am.longitude, am.latitude).then((roadName) => {
            am.roadName = roadName
            am._roadNameLoading = false
          })
        }
      }
    }
  } catch {}
}

async function sendChat() {
  const msg = chatInput.value.trim()
  if (!msg || chatTyping.value) return
  chatMessages.value.push({ role: "user", text: msg })
  chatInput.value = ""
  chatTyping.value = true
  setTimeout(() => { if (chatMsgRef.value) chatMsgRef.value.scrollTop = chatMsgRef.value.scrollHeight }, 50)

  try {
    // 调用后端 AI 接口（和左侧 AgentChat.vue 一样）
    const res = await agentApi.chat({ message: msg, includeContext: true })
    const data = res.data.data
    let reply = data?.answer || ""

    // 如果后端没有返回有效回复，基于当前地图数据生成本地回复
    if (!reply || reply.trim() === "") {
      const rd = roadDiseaseData.value || []
      const total = rd.reduce((s: number, r: any) => s + (r.totalCount || 0), 0)
      const high = rd.reduce((s: number, r: any) => s + (r.highCount || 0), 0)
      const medium = rd.reduce((s: number, r: any) => s + (r.mediumCount || 0), 0)
      const low = rd.reduce((s: number, r: any) => s + (r.lowCount || 0), 0)
      const m = msg.toLowerCase()

      if (m.includes("最多") || m.includes("top") || m.includes("排行")) {
        const sorted = [...rd].sort((a: any, b: any) => (b.totalCount || 0) - (a.totalCount || 0))
        reply = "病害最多的道路排行：<br>" + sorted.slice(0, 5).map((r: any, i: number) => (i + 1) + ". " + roadNameCn(r.roadName) + "：" + r.totalCount + "处（严重" + r.highCount + "处）").join("<br>")
      } else if (m.includes("严重") || m.includes("high")) {
        const hasHigh = rd.filter((r: any) => (r.highCount || 0) > 0)
        reply = hasHigh.length > 0 ? "存在严重病害的道路：<br>" + hasHigh.map((r: any) => roadNameCn(r.roadName) + "：" + r.highCount + "处").join("<br>") : "当前没有严重病害道路"
      } else if (m.includes("统计") || m.includes("总数") || m.includes("多少")) {
        reply = "当前病害统计数据：<br>- 病害总数: " + total + " 处<br>- 严重: " + high + " 处<br>- 中等: " + medium + " 处<br>- 轻微: " + low + " 处"
      } else {
        reply = "我是道路病害 AI 助手，您可以问我：<br>- 病害最多的道路<br>- 严重病害分布<br>- 病害统计数据<br>- 道路病害排行"
      }
    }

    chatMessages.value.push({ role: "ai", text: reply })
  } catch(e) {
    // 后端不可用时，基于地图数据生成本地回复
    const rd = roadDiseaseData.value || []
    const total = rd.reduce((s: number, r: any) => s + (r.totalCount || 0), 0)
    const high = rd.reduce((s: number, r: any) => s + (r.highCount || 0), 0)
    const medium = rd.reduce((s: number, r: any) => s + (r.mediumCount || 0), 0)
    const low = rd.reduce((s: number, r: any) => s + (r.lowCount || 0), 0)
    const m = msg.toLowerCase()

    let reply = ""
    if (m.includes("最多") || m.includes("top") || m.includes("排行")) {
      const sorted = [...rd].sort((a: any, b: any) => (b.totalCount || 0) - (a.totalCount || 0))
      reply = sorted.length > 0
        ? "病害最多的道路排行：<br>" + sorted.slice(0, 5).map((r: any, i: number) => (i + 1) + ". " + roadNameCn(r.roadName) + "：" + r.totalCount + "处（严重" + r.highCount + "处）").join("<br>")
        : "暂无道路病害数据"
    } else if (m.includes("严重") || m.includes("high")) {
      const hasHigh = rd.filter((r: any) => (r.highCount || 0) > 0)
      reply = hasHigh.length > 0 ? "存在严重病害的道路：<br>" + hasHigh.map((r: any) => roadNameCn(r.roadName) + "：" + r.highCount + "处").join("<br>") : "当前没有严重病害道路"
    } else if (m.includes("统计") || m.includes("总数") || m.includes("多少")) {
      reply = "当前病害统计数据：<br>- 病害总数: " + total + " 处<br>- 严重: " + high + " 处<br>- 中等: " + medium + " 处<br>- 轻微: " + low + " 处"
    } else {
      reply = "AI 服务暂时不可用，以下是基于当前地图数据的回复：<br>- 病害总数: " + total + " 处<br>- 严重: " + high + " 处<br>- 中等: " + medium + " 处<br>- 轻微: " + low + " 处<br><br>您可以问我：病害最多的道路、严重病害分布、病害统计数据等"
    }
    chatMessages.value.push({ role: "ai", text: reply })
  }
  chatTyping.value = false
  setTimeout(() => { if (chatMsgRef.value) chatMsgRef.value.scrollTop = chatMsgRef.value.scrollHeight }, 50)
}

onMounted(() => {
  nextTick(() => { initMap(); initECharts(); })
  startTimeUpdate()
  loadStats()
  loadDiseaseData()
  loadRoadDiseaseData()
  loadMapAnalysisData()
  window.addEventListener("data-updated", onDataUpdated)
  refreshTimer = setInterval(() => {
    loadStats()
    loadRoadDiseaseData()
    loadMapAnalysisData()
  }, 30000)

  // 挂载调试对象到 window，方便在 DevTools Console 中诊断
  ;(window as any).__batchGeocoder = batchGeocoder
  ;(window as any).__roadDiseaseData = roadDiseaseData
  ;(window as any).__roadNameResolved = roadNameResolved
})

function onDataUpdated() {
  loadStats()
  loadDiseaseData()
  loadRoadDiseaseData()
  loadMapAnalysisData()
}

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
  if (refreshTimer) clearInterval(refreshTimer)
  if (trendChart) trendChart.dispose()
  if (pieChart) pieChart.dispose()
  if (severityChart) severityChart.dispose()
  map = null
  // 清理批量逆地理编码器
  disposeBatchGeocoder()
  window.removeEventListener("data-updated", onDataUpdated)
})
</script>
<style scoped>
.ds-container { display:flex; height:100vh; margin:-24px; width:calc(100% + 48px); background:#f5f7fa; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; overflow:hidden; position:relative; }
.ds-map-area { flex:1; position:relative; }
.ds-float-stats { position:absolute; top:14px; left:14px; z-index:10; display:flex; background:#fff; border:1px solid #f0f2f5; border-radius:10px; overflow:hidden; }
.ds-float-stat { display:flex; flex-direction:column; align-items:center; padding:8px 14px; border-right:1px solid #eef0f4; }
.ds-float-stat:last-child { border-right:none; }
.ds-fs-val { font-size:17px; font-weight:800; color:#0f172a; font-variant-numeric:tabular-nums; }
.ds-fs-lbl { font-size:10px; color:#94a3b8; margin-top:1px; white-space:nowrap; }
.ds-float-time { display:flex; align-items:center; padding:0 14px; font-size:11px; color:#94a3b8; font-variant-numeric:tabular-nums; }
.ds-map { width:100%; height:100%; background:#f1f5f9; }
.ds-toggle-btn { position:fixed; top:50%; transform:translateY(-50%); z-index:100; width:28px; height:48px; display:flex; align-items:center; justify-content:center; background:#fff; border:1px solid #d0d5dd; border-radius:6px; cursor:pointer; color:#94a3b8; box-shadow:0 2px 6px rgba(0,0,0,0.1); padding:0; font-family:inherit; transition:right .25s ease,color .15s,border-color .15s; }
.ds-toggle-btn:hover { color:#2563eb; border-color:#2563eb; box-shadow:0 2px 8px rgba(37,99,235,0.15); }
.ds-view-controls { position:absolute; right:14px; bottom:30px; z-index:15; display:flex; flex-direction:column; gap:2px; background:#fff; border:1px solid #f0f2f5; border-radius:8px; padding:4px; }
.ds-vc-btn { width:32px; height:32px; display:flex; align-items:center; justify-content:center; border:none; background:transparent; border-radius:6px; cursor:pointer; color:#64748b; font-size:13px; transition:all .15s; }
.ds-vc-btn:hover { background:#f1f5f9; color:#2563eb; }
.ds-vc-level { text-align:center; font-size:10px; font-weight:700; color:#94a3b8; padding:2px 0; }
.ds-vc-locate { margin-top:2px; border-top:1px solid #eef0f4; padding-top:6px; }
.ds-link { color: #4361ee; cursor: pointer; text-decoration: underline; }
.ds-link:hover { color: #1d4ed8; }
.ds-wo-status-pending { color: #ef4444; }
.ds-wo-status-assigned { color: #f59e0b; }
.ds-wo-status-completed { color: #22c55e; }
.ds-sidebar { width:340px; flex-shrink:0; background:#fff; border-left:1px solid #f0f2f5; display:flex; flex-direction:column; transition:width .25s ease,margin .25s ease; overflow:hidden; }
.ds-sidebar.collapsed { width:0; margin-right:-340px; border-left:none; }
.ds-sidebar-inner { width:340px; height:100%; display:flex; flex-direction:column; overflow-y:auto; }
.ds-sidebar-inner::-webkit-scrollbar { width:3px; }
.ds-sidebar-inner::-webkit-scrollbar-thumb { background:#e2e8f0; border-radius:2px; }
.ds-sb-header { display:flex; align-items:center; gap:8px; padding:16px; border-bottom:1px solid #f0f2f5; font-size:14px; font-weight:700; color:#0f172a; }
.ds-sb-header svg { color:#4361ee; }
.ds-sb-tabs { display:flex; margin:0 16px; padding:0; border-bottom:1px solid #f0f2f5; background:none; border-radius:0; gap:0; }
.ds-sb-tab { flex:1; display:flex; flex-direction:column; align-items:center; gap:4px; padding:12px 8px 10px; text-align:center; font-size:13px; font-weight:500; color:#94a3b8; cursor:pointer; transition:all .2s; background:transparent; font-family:inherit; border:none; border-bottom:3px solid transparent; margin-bottom:-1px; line-height:1.3; }
.ds-sb-tab.active { background:transparent; color:#2563eb; font-weight:600; border-bottom-color:#2563eb; box-shadow:none; }
.ds-sb-tab:hover:not(.active) { background:transparent; color:#64748b; }
.ds-tab-panel { overflow-y:auto; flex:1; display:flex; flex-direction:column; padding:16px; gap:0; }
.ds-tab-panel::-webkit-scrollbar { width:4px; }
.ds-tab-panel::-webkit-scrollbar-thumb { background:#d1d5db; border-radius:2px; }
.ds-sb-search-wrap { position:relative; margin-bottom:16px; }
.ds-sb-search { display:flex; align-items:center; gap:6px; padding:8px 12px; background:#f8f9fc; border:1px solid #eef0f4; border-radius:8px; }
.ds-sb-search svg { color:#94a3b8; flex-shrink:0; }
.ds-sb-search input { flex:1; border:none; background:transparent; outline:none; font-size:13px; color:#1e293b; font-family:inherit; }
.ds-sb-search input::placeholder { color:#94a3b8; }
.ds-search-results { position:absolute; top:calc(100% + 4px); left:0; right:0; max-height:280px; overflow-y:auto; background:#fff; border:1px solid #eef0f4; border-radius:8px; box-shadow:0 8px 24px rgba(0,0,0,0.12); z-index:200; }
.ds-search-item { display:flex; align-items:center; gap:8px; padding:8px 12px; cursor:pointer; transition:background 0.12s; border-bottom:1px solid #f1f5f9; }
.ds-search-item:last-child { border-bottom:none; }
.ds-search-item:hover { background:#f8f9fc; }
.ds-search-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.ds-search-info { flex:1; min-width:0; }
.ds-search-name { font-size:13px; font-weight:500; color:#0f172a; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.ds-search-meta { font-size:11px; color:#94a3b8; margin-top:1px; }
.ds-search-arrow { color:#cbd5e1; flex-shrink:0; }
.ds-search-empty { padding:12px; text-align:center; font-size:12px; color:#94a3b8; background:#fff; border:1px solid #eef0f4; border-radius:8px; margin-top:4px; }
.ds-rank-item { margin-bottom:8px; padding:6px 8px; border-radius:6px; cursor:pointer; transition:background 0.15s, box-shadow 0.15s; border:1px solid transparent; }
.ds-rank-item:hover { background:#f0f5ff; border-color:#c7d7fe; box-shadow:0 2px 8px rgba(67,97,238,0.10); }
.ds-rank-item:active { background:#e0e7ff; }
.ds-sb-section { padding:0; margin-bottom:16px; }
.ds-sb-section-title { padding:0 0 0; margin-bottom:10px; font-size:11px; font-weight:600; color:#94a3b8; letter-spacing:0.04em; }
.ds-layer-item { margin-bottom:2px; }
.ds-layer-label { display:flex; align-items:center; gap:8px; font-size:13px; font-weight:500; color:#475569; cursor:pointer; padding:7px 10px; border-radius:6px; }
.ds-layer-label input[type=checkbox] { accent-color:#4361ee; }
.ds-layer-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.ds-layer-count { min-width: 22px; margin-left: auto; font-size: 11px; color: #94a3b8; text-align: right; font-variant-numeric: tabular-nums; }
.ds-filter-row { margin-bottom:10px; }
.ds-filter-row:last-child { margin-bottom:0; }
.ds-filter-row label { font-size:11px; font-weight:500; color:#94a3b8; display:block; margin-bottom:6px; }
.ds-filter-row select, .ds-filter-row input[type=date] { width:100%; padding:7px 10px; border:1px solid #e2e8f0; border-radius:6px; font-size:12px; color:#334155; background:#fafbfc; font-family:inherit; outline:none; transition:border-color .15s; }
.ds-filter-row select:focus, .ds-filter-row input[type=date]:focus { border-color:#2563eb; background:#fff; }
.ds-sb-actions { padding:12px 0 0; margin-top:auto; display:flex; flex-direction:column; gap:6px; border-top:1px solid #f0f2f5; }
.ds-sb-btn { display:flex; align-items:center; justify-content:center; gap:6px; padding:8px 12px; border:1px solid #e2e8f0; background:#fff; border-radius:8px; font-size:12px; font-weight:500; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; }
.ds-sb-btn:hover { border-color:#4361ee; color:#4361ee; }
.ds-sb-btn.primary { background:#2563eb; color:#fff; border-color:#2563eb; font-weight:500; }
.ds-sb-btn.primary:hover { background:#3651d4; }
.ds-popup { position:absolute; right:310px; top:14px; z-index:100; width:300px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; }
.ds-popup-head { display:flex; align-items:center; justify-content:space-between; padding:12px 14px; border-bottom:1px solid #f1f5f9; font-size:13px; font-weight:700; color:#0f172a; }
.ds-popup-head button { width:24px; height:24px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; font-size:12px; cursor:pointer; }
.ds-popup-head button:hover { background:#e2e8f0; color:#475569; }
.ds-popup-body { padding:8px 0; }
.ds-popup-img { padding:8px 14px; }
.ds-popup-info { padding:0 14px; }
.ds-popup-row { display:flex; align-items:center; justify-content:space-between; padding:6px 0; border-bottom:1px solid #f8f9fc; font-size:12px; }
.ds-popup-row:last-child { border-bottom:none; }
.ds-popup-label { color:#64748b; flex-shrink:0; }
.ds-popup-row strong { color:#0f172a; text-align:right; display:flex; align-items:center; gap:4px; }
.ds-conf { color:#10b981 !important; }
.ds-order { color:#4361ee !important; }
.ds-sev-dot { width:6px; height:6px; border-radius:50%; display:inline-block; flex-shrink:0; }
.ds-popup-foot { display:flex; gap:8px; padding:10px 14px; border-top:1px solid #f1f5f9; }
.ds-btn { flex:1; padding:8px; border:1px solid #e2e8f0; border-radius:8px; font-size:12px; font-weight:600; cursor:pointer; font-family:inherit; text-align:center; }
.ds-btn-primary { background:#2563eb; color:#fff; border-color:#2563eb; }
.ds-btn-primary:hover { background:#1d4ed8; }
.ds-btn-ghost { background:transparent; color:#64748b; }
.ds-btn-ghost:hover { background:#f1f5f9; }
.ds-an-section { padding:0; margin-bottom:16px; }
.ds-an-section+.ds-an-section { border-top:1px solid #f0f2f5; padding-top:16px; }
.ds-an-card { display:flex; align-items:center; gap:12px; padding:12px 14px; background:#f8f9fc; border-radius:10px; }
.ds-an-card+.ds-an-card { margin-top:8px; }
.ds-an-card-icon { width:36px; height:36px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.ds-an-card-info { display:flex; flex-direction:column; }
.ds-an-card-val { font-size:22px; font-weight:800; color:#0f172a; line-height:1.1; font-variant-numeric:tabular-nums; }
.ds-an-card-lbl { font-size:11px; color:#94a3b8; margin-top:2px; }
.ds-chat-panel { overflow:hidden; padding:0; }
.ds-chat-clear { margin-left:auto; width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; flex-shrink:0; }
.ds-chat-clear:hover { background:#fef2f2; color:#ef4444; }
.ds-chat-msgs { flex:1; overflow-y:auto; padding:12px 16px; display:flex; flex-direction:column; gap:8px; }
.ds-chat-msgs::-webkit-scrollbar { width:3px; }
.ds-chat-msgs::-webkit-scrollbar-thumb { background:#e2e8f0; border-radius:2px; }
.ds-chat-msg { display:flex; gap:8px; max-width:100%; }
.ds-chat-msg.user { justify-content:flex-end; }
.ds-chat-avatar { width:28px; height:28px; display:flex; align-items:center; justify-content:center; background:#eef2ff; border-radius:8px; flex-shrink:0; color:#4361ee; }
.ds-chat-bubble { padding:9px 12px; border-radius:8px; font-size:13px; line-height:1.5; color:#334155; background:#f1f5f9; max-width:200px; word-break:break-word; }
.ds-chat-msg.user .ds-chat-bubble { background:#2563eb; color:#fff; border-bottom-right-radius:2px; }
.ds-chat-msg.ai .ds-chat-bubble { border-bottom-left-radius:2px; }
.ds-chat-bubble strong { font-weight:700; }
.ds-chat-msg.user .ds-chat-bubble strong { color:#fff; }
.ds-chat-typing { display:flex; gap:3px; align-items:center; padding:10px 14px; }
.ds-chat-typing span { width:5px; height:5px; border-radius:50%; background:#94a3b8; animation:ds-typing 1s infinite; }
.ds-chat-typing span:nth-child(2) { animation-delay:.2s; }
.ds-chat-typing span:nth-child(3) { animation-delay:.4s; }
@keyframes ds-typing { 0%,60%,to { opacity:.3; transform:scale(1) } 30% { opacity:1; transform:scale(1.2) } }
.ds-chat-input-bar { display:flex; gap:6px; padding:10px 16px 16px; border-top:1px solid #f0f2f5; background:#fff; }
.ds-chat-input-bar input { flex:1; padding:8px 12px; border:1px solid #e2e8f0; border-radius:8px; font-size:12px; outline:none; font-family:inherit; color:#334155; }
.ds-chat-input-bar input:focus { border-color:#4361ee; }
.ds-chat-input-bar input::placeholder { color:#94a3b8; }
.ds-chat-send { width:34px; height:34px; display:flex; align-items:center; justify-content:center; background:#4361ee; border:none; border-radius:8px; color:#fff; cursor:pointer; flex-shrink:0; }
.ds-chat-send:hover { background:#3651d4; }
.ds-chat-send:disabled { background:#e2e8f0; color:#94a3b8; cursor:not-allowed; }
.ds-chat-avatar .avatar-video-wrap { width:28px; height:28px; border-radius:8px; overflow:hidden; border:1px solid rgba(183,36,255,.25); box-shadow:0 0 4px #b724ff1f; flex-shrink:0; display:flex; }
.ds-vc-sep { height:1px; background:#eef0f4; margin:3px 6px; }
.ds-vc-btn.active { background:#eef2ff; color:#2563eb; }
.ds-vc-pitch-row { display:flex; align-items:center; gap:4px; padding:4px 8px; }
.ds-vc-slider { width:60px; height:3px; -webkit-appearance:none; appearance:none; background:#e2e8f0; border-radius:2px; outline:none; cursor:pointer; }
.ds-vc-slider::-webkit-slider-thumb { -webkit-appearance:none; width:12px; height:12px; background:#4361ee; border-radius:50%; cursor:pointer; border:2px solid #fff; box-shadow:0 1px 3px rgba(0,0,0,0.15); }
.ds-vc-slider-label { font-size:10px; font-weight:600; color:#94a3b8; min-width:22px; text-align:right; }

/* 图层开关样式 */
.ds-layer-toggle {
  display:flex; align-items:center; justify-content:space-between;
  padding:7px 10px; margin-bottom:4px;
}
.ds-tt-label { display:flex; align-items:center; gap:6px; }
.ds-switch {
  position:relative; display:inline-block; width:36px; height:20px; flex-shrink:0;
}
.ds-switch input { opacity:0; width:0; height:0; }
.ds-switch-slider {
  position:absolute; cursor:pointer; top:0; left:0; right:0; bottom:0;
  background:#cbd5e1; border-radius:20px; transition:0.2s;
}
.ds-switch-slider::before {
  content:""; position:absolute; height:14px; width:14px; left:3px; bottom:3px;
  background:#fff; border-radius:50%; transition:0.2s;
}
.ds-switch input:checked + .ds-switch-slider { background:#4361ee; }
.ds-switch input:checked + .ds-switch-slider::before { transform:translateX(16px); }

/* Analysis Tab - Map Stats Mini Cards */
.ds-an-card-mini { display:flex; flex-direction:column; align-items:center; padding:10px 8px; background:#f8f9fc; border-radius:8px; }
.ds-an-mini-val { font-size:18px; font-weight:800; line-height:1; font-variant-numeric:tabular-nums; }
.ds-an-mini-lbl { font-size:10px; color:#94a3b8; margin-top:3px; }

.ds-link { color: #4361ee; cursor: pointer; text-decoration: underline; }
.ds-link:hover { color: #1d4ed8; }
.ds-link-disabled { color: #94a3b8 !important; cursor: not-allowed; text-decoration: none !important; }
.ds-wo-status-pending { color: #ef4444; }
.ds-wo-status-assigned { color: #f59e0b; }
.ds-wo-status-completed { color: #22c55e; }
.ds-health-dot { width:6px; height:6px; border-radius:50%; flex-shrink:0; }
.ds-health-badge { display:inline-block; padding:1px 6px; border-radius:4px; font-size:9px; font-weight:600; }

/* Alert Dot */
.ds-alert-dot { width:5px; height:5px; border-radius:50%; flex-shrink:0; }
.ds-alert-type { display:inline-block; padding:1px 6px; border-radius:3px; font-size:9px; font-weight:600; flex-shrink:0; }

/* Disease Legend */
.ds-disease-legend {
  position:absolute; left:14px; bottom:30px; z-index:15;
  background:rgba(255,255,255,0.95); border:1px solid #e2e8f0;
  border-radius:10px; padding:10px 14px;
  box-shadow:0 2px 8px rgba(0,0,0,0.08);
  backdrop-filter:blur(8px);
}
.ds-legend-title {
  font-size:11px; font-weight:700; color:#0f172a; margin-bottom:8px;
  letter-spacing:0.02em;
}
.ds-legend-items { display:flex; gap:12px; }
.ds-legend-item { display:flex; align-items:center; gap:4px; font-size:10px; color:#64748b; }
.ds-legend-bar {
  display:inline-block; width:20px; height:4px; border-radius:2px;
}
</style>
