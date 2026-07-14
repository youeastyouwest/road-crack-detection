<template>
  <div class="wo-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ isAdmin ? t('wo.title') : t('wo.deptTitle') }}</h2>
        <p class="page-desc">{{ isAdmin ? t('wo.desc') : t('wo.deptDesc') }}</p>
      </div>
      <div v-if="isAdmin" style="display:flex;gap:8px">
        <button class="btn-ghost" @click="handleExport"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>{{ t('wo.export') }}</button>
        <button class="btn-primary" @click="showCreate=true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>{{ t('wo.create') }}</button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card" title="基于当前页数据"><div class="stat-icon" style="background:#eef2ff;color:#2563eb"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div><div><span class="stat-val">{{ stats.pending }}</span><span class="stat-lbl">{{ t('wo.statPendingAssignment') }}</span></div></div>
      <div class="stat-card" title="基于当前页数据"><div class="stat-icon" style="background:#fef3c7;color:#d97706"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg></div><div><span class="stat-val">{{ stats.inProgress }}</span><span class="stat-lbl">{{ t('wo.statInProgress') }}</span></div></div>
      <div class="stat-card" title="基于当前页数据"><div class="stat-icon" style="background:#f0f7ff;color:#0ea5e9"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg></div><div><span class="stat-val">{{ stats.pendingReview }}</span><span class="stat-lbl">{{ t('wo.statPendingReview') }}</span></div></div>
      <div class="stat-card" title="基于当前页数据"><div class="stat-icon" style="background:#dcfce7;color:#16a34a"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 6 9 17 4 12"/></svg></div><div><span class="stat-val">{{ stats.completed }}</span><span class="stat-lbl">{{ t('wo.statClosed') }}</span></div></div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="filter-group">
            <select v-model="filter.status" class="filter-select">
              <option value="">{{ t('wo.allStatus') }}</option>
              <option value="PENDING_ASSIGNMENT">{{ t('wo.statusPendingAssignment') }}</option>
              <option value="ASSIGNED">{{ t('wo.statusAssigned') }}</option>
              <option value="IN_PROGRESS">{{ t('wo.statusInProgress') }}</option>
              <option value="COMPLETED">{{ t('wo.statusCompleted') }}</option>
              <option value="PENDING_DEPT_REVIEW">{{ t('wo.statusPendingDeptReview') }}</option>
              <option value="PENDING_ADMIN_REVIEW">{{ t('wo.statusPendingAdminReview') }}</option>
              <option value="REJECTED">{{ t('wo.statusRejected') }}</option>
              <option value="CLOSED">{{ t('wo.statusClosed') }}</option>
              <option value="CANCELLED">{{ t('wo.statusCancelled') }}</option>
            </select>
            <select v-if="isAdmin" v-model="filter.departmentCode" class="filter-select"><option value="">{{ t('wo.allDept') }}</option><option value="ROAD_ADMIN">{{ t('wo.deptRoadAdmin') }}</option><option value="SANITATION">{{ t('wo.deptSanitation') }}</option><option value="TRAFFIC_POLICE">{{ t('wo.deptTrafficPolice') }}</option></select>
            <select v-model="filter.severityLevel" class="filter-select"><option value="">{{ t('wo.allLevels') }}</option><option value="LOW">{{ t('severity.low') }}</option><option value="MEDIUM">{{ t('severity.medium') }}</option><option value="HIGH">{{ t('severity.high') }}</option></select>
            <div class="search-wrap"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input v-model="filter.keyword" :placeholder="t('wo.searchPlaceholder')" @keyup.enter="loadData" /></div>
            <button class="btn-ghost" @click="loadData">{{ t('common.search') }}</button>
          </div>
        </div>
      </div>

      <div class="table-wrap">
        <table class="ds-table">
          <thead><tr><th>{{ t('wo.id') }}</th><th>{{ t('wo.titleLabel') }}</th><th>{{ t('wo.location') }}</th><th>{{ t('wo.level') }}</th><th>{{ t('common.status') }}</th><th>{{ t('wo.dept') }}</th><th>{{ t('wo.assignee') }}</th><th style="width:240px">{{ t('common.actions') }}</th></tr></thead>
          <tbody>
            <tr v-for="row in orders" :key="row.id" :class="{ 'row-highlighted': row.workOrderCode === highlightedWorkOrderCode }">
              <td><span class="code-tag">{{ row.workOrderCode }}</span></td>
              <td class="td-title">{{ row.title }}</td>
              <td class="td-muted">{{ row.location || '--' }}</td>
              <td><span :class="['sev-badge', row.severityLevel === 'HIGH' ? 'sev-high' : row.severityLevel === 'MEDIUM' ? 'sev-med' : 'sev-low']">{{ severityLabel(row.severityLevel as string) }}</span></td>
              <td><span :class="['status-tag', statusCls(row.status)]">{{ statusLabel(row.status) }}</span></td>
              <td class="td-dept">{{ deptLabel(row.departmentCode || '') }}</td>
              <td class="td-muted">{{ row.assignee || '--' }}</td>
              <td><div class="action-group">
                <button class="action-btn" @click="viewDetail(row)">{{ t('common.detail') }}</button>
                <button class="action-btn" v-if="isAdmin && row.status==='PENDING_ASSIGNMENT'" @click="openAssignDept(row)">{{ t('wo.assignDept') }}</button>
                <button class="action-btn" v-if="isDeptAdmin && row.status==='ASSIGNED'" @click="openAssignWorker(row)">{{ t('wo.assignWorker') }}</button>
                <button class="action-btn" v-if="isDeptAdmin && row.status==='REJECTED'" @click="openAssignWorker(row)">{{ t('wo.reassign') }}</button>
                <button class="action-btn action-review" v-if="isDeptAdmin && row.status==='PENDING_DEPT_REVIEW'" @click="openReview(row, 'dept')">{{ t('wo.reviewReport') }}</button>
                <button class="action-btn action-review" v-if="isAdmin && row.status==='PENDING_ADMIN_REVIEW'" @click="openReview(row, 'admin')">{{ t('wo.finalReview') }}</button>
                <button class="action-btn action-danger" v-if="isAdmin && row.status!=='CLOSED'&&row.status!=='CANCELLED'" @click="handleCancel(row)">{{ t('wo.cancel') }}</button>
              </div></td>
            </tr>
            <tr v-if="!loading && orders.length === 0"><td colspan="8" class="empty-row">{{ t('wo.noData') }}</td></tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span class="page-info">{{ t('common.total', { count: total }) }}</span>
        <div class="page-btns">
          <button class="page-btn" :disabled="page<=1" @click="page--;loadData()">{{ t('common.prevPage') }}</button>
          <span class="page-cur">{{ page }}</span>
          <button class="page-btn" :disabled="page*10>=total" @click="page++;loadData()">{{ t('common.nextPage') }}</button>
        </div>
      </div>
    </div>

    <!-- Create Modal (admin only) -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate=false">
      <div class="modal-card">
        <div class="modal-head"><span>{{ t('wo.create') }}</span><button class="modal-close" @click="showCreate=false">x</button></div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group"><label>{{ t('wo.titleLabel') }}</label><input v-model="createForm.title" :placeholder="t('wo.formTitlePlaceholder')" /></div>
            <div class="form-group"><label>{{ t('wo.damageType') }}</label><select v-model="createForm.damageType"><option value="CRACK">{{ t('damage.crack') }}</option><option value="MARKING_DAMAGE">{{ t('damage.markingDamage') }}</option><option value="ROAD_SPILL">{{ t('damage.roadSpill') }}</option><option value="POTHOLE">{{ t('damage.pothole') }}</option></select></div>
            <div class="form-group"><label>{{ t('wo.formSeverity') }}</label><select v-model="createForm.severityLevel"><option value="LOW">{{ t('severity.low') }}</option><option value="MEDIUM">{{ t('severity.medium') }}</option><option value="HIGH">{{ t('severity.high') }}</option></select></div>
            <div class="form-group"><label>{{ t('wo.location') }}</label><input v-model="createForm.location" :placeholder="t('wo.formLocationPlaceholder')" /></div>
            <div class="form-group"><label>{{ t('wo.formDept') }}</label><select v-model="createForm.departmentCode"><option value="ROAD_ADMIN">{{ t('wo.deptRoadAdmin') }}</option><option value="SANITATION">{{ t('wo.deptSanitation') }}</option><option value="TRAFFIC_POLICE">{{ t('wo.deptTrafficPolice') }}</option></select></div>
            <div class="form-group"><label>{{ t('wo.formDetectionTaskId') }}</label><input v-model.number="createForm.detectionTaskId" type="number" min="1" /></div>
          </div>
          <div class="form-group" style="margin-top:12px"><label>{{ t('wo.description') }}</label><textarea v-model="createForm.description" rows="3" :placeholder="t('wo.formDescriptionPlaceholder')"></textarea></div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showCreate=false">{{ t('common.cancel') }}</button>
          <button class="btn-primary" :disabled="creating" @click="handleCreate">{{ creating ? t('wo.submittingText') : t('wo.submit') }}</button>
        </div>
      </div>
    </div>

    <!-- Assign Department Modal (super admin) -->
    <div v-if="showAssignDept" class="modal-overlay" @click.self="showAssignDept=false">
      <div class="modal-card" style="width:480px">
        <div class="modal-head"><span>{{ t('wo.assignToDeptTitle') }}{{ assignTarget?.workOrderCode || assignTarget?.id }}</span><button class="modal-close" @click="showAssignDept=false">x</button></div>
        <div class="modal-body">
          <div class="assign-preview" v-if="assignTarget">
            <div class="assign-preview-head">
              <span class="assign-preview-title">{{ assignTarget.title }}</span>
              <span :class="['sev-badge', assignTarget.severityLevel === 'HIGH' ? 'sev-high' : assignTarget.severityLevel === 'MEDIUM' ? 'sev-med' : 'sev-low']">{{ severityLabel(assignTarget.severityLevel as keyof object) }}</span>
            </div>
            <div class="assign-preview-grid">
              <div class="assign-preview-item"><span class="ap-label">{{ t('wo.location') }}</span><span class="ap-val">{{ assignTarget.location || '--' }}</span></div>
              <div class="assign-preview-item"><span class="ap-label">{{ t('wo.damageType') }}</span><span class="ap-val">{{ damageTypeLabel(assignTarget.damageType || '') }}</span></div>
              <div class="assign-preview-item"><span class="ap-label">{{ t('wo.createdAt') }}</span><span class="ap-val">{{ formatDate(assignTarget.createdAt) || '--' }}</span></div>
              <div class="assign-preview-item"><span class="ap-label">{{ t('wo.description') }}</span><span class="ap-val">{{ assignTarget.description || '--' }}</span></div>
            </div>
          </div>
          <div class="form-group" style="margin-top:16px">
            <label>{{ t('wo.selectDept') }} <span class="required">*</span></label>
            <select v-model="assignDeptForm.departmentCode">
              <option v-for="d in deptOptions" :key="d.value" :value="d.value">{{ d.label }}</option>
            </select>
          </div>
          <div class="dept-hint">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            <span>{{ t('wo.assignDeptHint') }}</span>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showAssignDept=false">{{ t('common.cancel') }}</button>
          <button class="btn-primary" :disabled="assigning" @click="confirmAssignDept">{{ assigning ? t('wo.assigningText') : t('wo.confirmAssign') }}</button>
        </div>
      </div>
    </div>

    <!-- Assign Worker Modal (dept admin) -->
    <div v-if="showAssignWorker" class="modal-overlay" @click.self="showAssignWorker=false">
      <div class="modal-card" style="width:600px">
        <div class="modal-head"><span>{{ t('wo.assignWorkerTitle') }}{{ assignTarget?.workOrderCode || assignTarget?.id }}</span><button class="modal-close" @click="showAssignWorker=false">x</button></div>
        <div class="modal-body">
          <div class="assign-preview" v-if="assignTarget">
            <div class="assign-preview-head">
              <span class="assign-preview-title">{{ assignTarget.title }}</span>
              <span :class="['sev-badge', assignTarget.severityLevel === 'HIGH' ? 'sev-high' : assignTarget.severityLevel === 'MEDIUM' ? 'sev-med' : 'sev-low']">{{ severityLabel(assignTarget.severityLevel as keyof object) }}</span>
            </div>
            <div class="assign-preview-grid">
              <div class="assign-preview-item"><span class="ap-label">{{ t('wo.location') }}</span><span class="ap-val">{{ assignTarget.location || '--' }}</span></div>
              <div class="assign-preview-item"><span class="ap-label">{{ t('wo.damageType') }}</span><span class="ap-val">{{ damageTypeLabel(assignTarget.damageType || '') }}</span></div>
              <div class="assign-preview-item"><span class="ap-label">{{ t('wo.dept') }}</span><span class="ap-val">{{ deptLabel(assignTarget.departmentCode || '') }}</span></div>
              <div class="assign-preview-item"><span class="ap-label">{{ t('wo.createdAt') }}</span><span class="ap-val">{{ formatDate(assignTarget.createdAt) || '--' }}</span></div>
            </div>
          </div>
          <div class="form-group" style="margin-top:16px">
            <label>{{ t('wo.selectWorker') }} <span class="required">*</span></label>
            <select v-model="assignWorkerForm.assignee">
              <option value="">{{ t('wo.pleaseSelectWorker') }}</option>
              <option v-for="w in availableWorkers" :key="w.name" :value="w.name">{{ w.name }}{{ w.phone ? ' (' + w.phone + ')' : '' }}</option>
            </select>
          </div>
          <div class="assign-empty-hint" v-if="availableWorkers.length === 0">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#d97706" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>{{ t('wo.noWorkerAvailable') }}</span>
          </div>
          <div class="assign-worker-info" v-if="assignWorkerForm.assignee && selectedWorkerInfo">
            <div class="awi-avatar">{{ assignWorkerForm.assignee.charAt(0) }}</div>
            <div class="awi-detail">
              <div class="awi-name">{{ selectedWorkerInfo.name }}</div>
              <div class="awi-meta">
                <span v-if="selectedWorkerInfo.phone">{{ selectedWorkerInfo.phone }}</span>
                <span v-if="selectedWorkerInfo.email">{{ selectedWorkerInfo.email }}</span>
                <span>{{ deptLabel(selectedWorkerInfo.dept) }}</span>
              </div>
            </div>
            <div class="awi-workload">
              <span class="awi-wl-num">{{ getWorkerWorkload(selectedWorkerInfo.name) }}</span>
              <span class="awi-wl-lbl">{{ t('wo.inProcessingOrders') }}</span>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showAssignWorker=false">{{ t('common.cancel') }}</button>
          <button class="btn-primary" :disabled="assigning || !assignWorkerForm.assignee" @click="confirmAssignWorker">{{ assigning ? t('wo.assigningWorker') : t('wo.confirmAssignWorker') }}</button>
        </div>
      </div>
    </div>

    <!-- Review Modal -->
    <div v-if="showReview" class="modal-overlay" @click.self="showReview=false">
      <div class="modal-card" style="width:600px">
        <div class="modal-head"><span>{{ reviewMode === 'dept' ? t('wo.deptReviewReport') : t('wo.finalReviewReport') }} #{{ reviewTarget?.workOrderCode || reviewTarget?.id }}</span><button class="modal-close" @click="showReview=false">x</button></div>
        <div class="modal-body">
          <div v-if="reviewLoading" style="text-align:center;padding:30px;color:#94a3b8">{{ t('wo.loadingReport') }}</div>
          <template v-else>
            <!-- 工单信息 -->
            <div class="review-order-info" v-if="reviewTarget">
              <div class="review-order-title">{{ reviewTarget.title }}</div>
              <div class="review-order-meta">
                <span>{{ t('wo.location') }}: {{ reviewTarget.location || '--' }}</span>
                <span>{{ t('wo.dept') }}: {{ deptLabel(reviewTarget.departmentCode || '') }}</span>
                <span>{{ t('wo.assignee') }}: {{ reviewTarget.assignee || '--' }}</span>
              </div>
            </div>
            <!-- 报告信息 -->
            <div class="review-report" v-if="reviewReport">
              <div class="review-report-head">
                <span class="review-report-code">{{ reviewReport.reportCode || (t('wo.reportCodePrefix') + reviewReport.id) }}</span>
                <span :class="['status-tag', reportStatusCls(reviewReport.status)]">{{ reportStatusLabel(reviewReport.status) }}</span>
              </div>
              <div class="review-report-grid">
                <div><span class="rr-label">{{ t('mr.executor') }}</span><span class="rr-val">{{ reviewReport.executor || '--' }}</span></div>
                <div><span class="rr-label">{{ t('mr.finishedAt') }}</span><span class="rr-val">{{ formatDate(reviewReport.finishedAt) || '--' }}</span></div>
                <div><span class="rr-label">{{ t('mr.materials') }}</span><span class="rr-val">{{ reviewReport.materials || '--' }}</span></div>
                <div><span class="rr-label">{{ t('mr.submittedAt') }}</span><span class="rr-val">{{ formatDate(reviewReport.createdAt) || '--' }}</span></div>
              </div>
              <div v-if="reviewReport.description" class="review-report-desc">
                <span class="rr-label">{{ t('mr.description') }}</span>
                <p>{{ reviewReport.description }}</p>
              </div>
              <!-- 上次审核意见（驳回时显示） -->
              <div v-if="reviewReport.reviewRemark" class="review-prev-remark">
                <span class="rr-label">{{ t('review.opinion') }}</span>
                <p>{{ reviewReport.reviewRemark }} - {{ reviewReport.reviewer || '--' }}</p>
              </div>
            </div>
            <!-- 审核表单 -->
            <div class="review-form">
              <div class="review-form-label">{{ t('wo.reviewResult') }} <span class="required">*</span></div>
              <div class="review-actions">
                <button :class="['review-btn', reviewForm.approved === true ? 'review-btn-pass' : '']" @click="reviewForm.approved = true">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                  {{ reviewMode === 'dept' ? t('wo.reviewPassDept') : t('wo.reviewPassAdmin') }}
                </button>
                <button :class="['review-btn', reviewForm.approved === false ? 'review-btn-reject' : '']" @click="reviewForm.approved = false">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  {{ t('wo.reviewRejectText') }}
                </button>
              </div>
              <div class="form-group" style="margin-top:14px">
                <label>{{ t('wo.reviewOpinion') }}</label>
                <textarea v-model="reviewForm.remark" rows="3" :placeholder="reviewForm.approved === false ? t('wo.rejectReasonPlaceholder') : t('wo.reviewRemarkPlaceholder')"></textarea>
              </div>
            </div>
          </template>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showReview=false">{{ t('common.cancel') }}</button>
          <button class="btn-primary" :disabled="reviewing || reviewForm.approved === null" @click="confirmReview">{{ reviewing ? t('wo.submittingReview') : t('wo.submitReviewBtn') }}</button>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail=false">
      <div class="modal-card" style="width:600px">
        <div class="modal-head"><span>{{ t('wo.detail') }} #{{ detailTarget?.workOrderCode || detailTarget?.id }}</span><button class="modal-close" @click="showDetail=false">x</button></div>
        <div class="modal-body">
          <div v-if="detailLoading" style="text-align:center;padding:30px;color:#94a3b8">{{ t('wo.detailLoading') }}</div>
          <template v-else>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
              <div><label class="detail-label">{{ t('wo.titleLabel') }}</label><div class="detail-val">{{ detailTarget?.title }}</div></div>
              <div><label class="detail-label">{{ t('wo.formDept') }}</label><div class="detail-val">{{ deptLabel(detailTarget?.departmentCode || '') }}</div></div>
              <div><label class="detail-label">{{ t('common.status') }}</label><div class="detail-val"><span :class="['status-tag', statusCls(detailTarget?.status || '')]">{{ statusLabel(detailTarget?.status || '') }}</span></div></div>
              <div><label class="detail-label">{{ t('wo.level') }}</label><div class="detail-val"><span :class="['sev-badge', detailTarget?.severityLevel === 'HIGH' ? 'sev-high' : detailTarget?.severityLevel === 'MEDIUM' ? 'sev-med' : 'sev-low']">{{ severityLabel(detailTarget?.severityLevel || '') }}</span></div></div>
              <div><label class="detail-label">{{ t('wo.location') }}</label><div class="detail-val">{{ detailTarget?.location || '--' }}</div></div>
              <div><label class="detail-label">{{ t('wo.assignee') }}</label><div class="detail-val">{{ detailTarget?.assignee || '--' }}</div></div>
              <div><label class="detail-label">{{ t('wo.damageType') }}</label><div class="detail-val">{{ damageTypeLabel(detailTarget?.damageType || '') }}</div></div>
              <div><label class="detail-label">{{ t('wo.createdAt') }}</label><div class="detail-val">{{ detailTarget?.createdAt || '--' }}</div></div>
              <div v-if="detailTarget?.description" style="grid-column:1/-1"><label class="detail-label">{{ t('wo.description') }}</label><div class="detail-val">{{ detailTarget.description }}</div></div>
            </div>
            <div v-if="detailTarget?.statusLogs?.length" style="margin-top:20px;border-top:1px solid #f0f2f5;padding-top:16px">
              <label class="detail-label">{{ t('wo.statusFlow') }}</label>
              <div class="status-timeline" style="margin-top:10px">
                <div v-for="(log, i) in detailTarget.statusLogs" :key="i" class="timeline-item">
                  <div class="timeline-dot"></div>
                  <div class="timeline-content">
                    <span class="timeline-status">{{ statusLabel(log.toStatus || '') }}</span>
                    <span class="timeline-time">{{ log.operatedAt || '--' }}</span>
                    <span v-if="log.note" class="timeline-note">{{ log.note }}</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showDetail=false">{{ t('common.close') }}</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue"
import { useRoute } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import { useAuthStore } from "@/stores/auth"
import { t } from "@/i18n"
import { workOrderApi, detectionApi, userApi, reportApi } from "@/api"
import type { WorkOrderResponse, MaintenanceReportResponse } from "@/types"

const authStore = useAuthStore()
const route = useRoute()
const isAdmin = computed(() => authStore.isAdmin)
const isDeptAdmin = computed(() => authStore.isDeptAdmin)

const orders = ref<WorkOrderResponse[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const showCreate = ref(false)
const showDetail = ref(false)
const detailLoading = ref(false)
const detailTarget = ref<WorkOrderResponse | null>(null)
const creating = ref(false)
const assigning = ref(false)
const detectionInfo = ref<any>(null)

// 高亮状态
const highlightedWorkOrderCode = ref<string>("")

// Assign dept modal (admin)
const showAssignDept = ref(false)
const assignDeptForm = reactive({ departmentCode: "ROAD_ADMIN" })

// Assign worker modal (dept admin)
const showAssignWorker = ref(false)
const assignWorkerForm = reactive({ assignee: "" })

// Review modal
const showReview = ref(false)
const reviewMode = ref<"dept" | "admin">("dept")
const reviewTarget = ref<WorkOrderResponse | null>(null)
const reviewReport = ref<MaintenanceReportResponse | null>(null)
const reviewLoading = ref(false)
const reviewing = ref(false)
const reviewForm = reactive({ approved: null as boolean | null, remark: "" })

const assignTarget = ref<WorkOrderResponse | null>(null)
const filter = reactive({ status: "", severityLevel: "", keyword: "", departmentCode: "" })
const createForm = reactive({
  title: "", damageType: "CRACK", severityLevel: "MEDIUM",
  location: "", departmentCode: "ROAD_ADMIN", description: "", detectionTaskId: undefined as number | undefined
})
const stats = reactive({ pending: 0, inProgress: 0, pendingReview: 0, completed: 0 })

// deptId -> departmentCode mapping
const deptIdToCode: Record<number, string> = { 2: "ROAD_ADMIN", 3: "SANITATION", 4: "TRAFFIC_POLICE" }

interface WorkerInfo {
  name: string
  dept: string
  phone?: string
  email?: string
  username?: string
}

// Workers in current dept admin's department
const deptWorkers = ref<WorkerInfo[]>([])
const availableWorkers = computed(() => deptWorkers.value)
const deptOptions = computed(() => [
  { value: "ROAD_ADMIN", label: t('wo.deptRoadAdmin') },
  { value: "SANITATION", label: t('wo.deptSanitation') },
  { value: "TRAFFIC_POLICE", label: t('wo.deptTrafficPolice') },
])

const selectedWorkerInfo = computed(() => {
  if (!assignWorkerForm.assignee) return null
  return deptWorkers.value.find(w => w.name === assignWorkerForm.assignee) || null
})

async function loadDeptWorkers() {
  if (!isDeptAdmin.value) return
  try {
    const res = await userApi.page({ page: 1, size: 200, deptId: authStore.userDetail?.deptId })
    const users = res.data.data.records || []
    deptWorkers.value = users
      .filter(u => u.roleCode === "ROLE_MAINTAINER")
      .map(u => ({
        name: u.realName || u.username || "未知",
        dept: authStore.deptCode,
        phone: u.phone,
        email: u.email,
        username: u.username,
      }))
    // Fallback: if no ROLE_MAINTAINER filter works, try all users in dept
    if (deptWorkers.value.length === 0) {
      deptWorkers.value = users.map(u => ({
        name: u.realName || u.username || "未知",
        dept: authStore.deptCode,
        phone: u.phone,
        email: u.email,
        username: u.username,
      }))
    }
  } catch {
    // Load all users and filter by dept code
    try {
      const res = await userApi.page({ page: 1, size: 200 })
      const users = res.data.data.records || []
      const myDeptId = authStore.userDetail?.deptId
      deptWorkers.value = users
        .filter(u => {
          const code = deptIdToCode[u.deptId ?? 0]
          return code === authStore.deptCode
        })
        .map(u => ({
          name: u.realName || u.username || "未知",
          dept: authStore.deptCode,
          phone: u.phone,
          email: u.email,
          username: u.username,
        }))
    } catch { /* ignore */ }
  }
}

function formatDate(dt?: string): string {
  if (!dt) return ""
  try {
    const d = new Date(dt)
    return d.toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
  } catch { return dt }
}

function getWorkerWorkload(assigneeName: string): number {
  return orders.value.filter(o =>
    o.assignee === assigneeName &&
    (o.status === "ASSIGNED" || o.status === "IN_PROGRESS")
  ).length
}

function statusCls(s: string) {
  return ({
    PENDING_ASSIGNMENT: "stat-info", ASSIGNED: "stat-primary", IN_PROGRESS: "stat-warn",
    COMPLETED: "stat-success", PENDING_DEPT_REVIEW: "stat-review-dept",
    PENDING_ADMIN_REVIEW: "stat-review-admin", REJECTED: "stat-rejected",
    CLOSED: "stat-closed", CANCELLED: "stat-cancel",
  } as any)[s] || "stat-default"
}
function severityLabel(s: string) {
  return ({ LOW: t('severity.low'), MEDIUM: t('severity.medium'), HIGH: t('severity.high') } as any)[s] || '--'
}
function statusLabel(s: string) {
  return ({
    PENDING_ASSIGNMENT: t('wo.statusPendingAssignment'), ASSIGNED: t('wo.statusAssigned'), IN_PROGRESS: t('wo.statusInProgress'),
    COMPLETED: t('wo.statusCompleted'), PENDING_DEPT_REVIEW: t('wo.statusPendingDeptReview'),
    PENDING_ADMIN_REVIEW: t('wo.statusPendingAdminReview'), REJECTED: t('wo.statusRejected'),
    CLOSED: t('wo.statusClosed'), CANCELLED: t('wo.statusCancelled'),
  } as any)[s] || s
}
function reportStatusCls(s?: string) {
  return ({ PENDING: "stat-warn", DEPT_APPROVED: "stat-review-admin", APPROVED: "stat-closed", REJECTED: "stat-rejected" } as any)[s || ""] || "stat-default"
}
function reportStatusLabel(s?: string) {
  return ({ PENDING: t('mr.statusPending'), DEPT_APPROVED: t('mr.statusDeptApproved'), APPROVED: t('mr.statusApproved'), REJECTED: t('mr.statusRejected') } as any)[s || ""] || s || "--"
}
function deptLabel(code: string) {
  return ({ ROAD_ADMIN: t('wo.deptRoadAdmin'), SANITATION: t('wo.deptSanitation'), TRAFFIC_POLICE: t('wo.deptTrafficPolice') } as any)[code] || code || "--"
}
function damageTypeLabel(typeCode: string) {
  return ({ CRACK: t('damage.crack'), TRANSVERSE_CRACK: t('damage.transverseCrack'), LONGITUDINAL_CRACK: t('damage.longitudinalCrack'), NET_CRACK: t('damage.netCrack'), POTHOLE: t('damage.pothole'), MARKING_DAMAGE: t('damage.markingDamage'), ROAD_SPILL: t('damage.roadSpill'), UNKNOWN: t('damage.unknown') } as any)[typeCode] || typeCode || "--"
}

function calcStats(records: WorkOrderResponse[]) {
  stats.pending = records.filter(r => r.status === "PENDING_ASSIGNMENT").length
  stats.inProgress = records.filter(r => r.status === "ASSIGNED" || r.status === "IN_PROGRESS").length
  stats.pendingReview = records.filter(r => r.status === "PENDING_DEPT_REVIEW" || r.status === "PENDING_ADMIN_REVIEW").length
  stats.completed = records.filter(r => r.status === "CLOSED").length
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: 10 }
    if (filter.status) params.status = filter.status
    if (filter.severityLevel) params.severityLevel = filter.severityLevel
    if (isAdmin.value && filter.departmentCode) params.departmentCode = filter.departmentCode
    if (isDeptAdmin.value && authStore.deptCode) params.departmentCode = authStore.deptCode
    if (filter.keyword) params.keyword = filter.keyword
    const r = await workOrderApi.list(params)
    orders.value = r.data.data.records
    total.value = r.data.data.total
    calcStats(r.data.data.records)
  } catch { ElMessage.error(t('wo.loadFailed')) }
  loading.value = false
}

async function handleCreate() {
  if (!createForm.title) return ElMessage.warning(t('wo.needTitle'))
  creating.value = true
  try {
    await workOrderApi.create(createForm as any)
    ElMessage.success(t('wo.createdAndWait'))
    showCreate.value = false
    Object.assign(createForm, { title: "", damageType: "CRACK", severityLevel: "MEDIUM", location: "", departmentCode: "ROAD_ADMIN", description: "", detectionTaskId: undefined })
    await loadData()
  } catch { ElMessage.error(t('wo.createFailed')) }
  finally { creating.value = false }
}

// ─── Admin: Assign to Department ───
function openAssignDept(row: WorkOrderResponse) {
  assignTarget.value = row
  const validDepts = ["ROAD_ADMIN", "SANITATION", "TRAFFIC_POLICE"]
  assignDeptForm.departmentCode = validDepts.includes(row.departmentCode || "") ? (row.departmentCode as string) : "ROAD_ADMIN"
  showAssignDept.value = true
}

async function confirmAssignDept() {
  assigning.value = true
  try {
    await workOrderApi.assign(assignTarget.value!.id, {
      departmentCode: assignDeptForm.departmentCode as any,
    } as any)
    ElMessage.success(t('wo.assignedTo', { dept: deptLabel(assignDeptForm.departmentCode) }))
    showAssignDept.value = false
    await loadData()
  } catch { ElMessage.error(t('wo.assignFailed')) }
  finally { assigning.value = false }
}

// ─── Dept Admin: Assign Worker ───
function openAssignWorker(row: WorkOrderResponse) {
  assignTarget.value = row
  assignWorkerForm.assignee = ""
  showAssignWorker.value = true
}

async function confirmAssignWorker() {
  if (!assignWorkerForm.assignee) return ElMessage.warning(t('wo.pleaseSelectWorker'))
  assigning.value = true
  try {
    await workOrderApi.assignWorker(assignTarget.value!.id, assignWorkerForm.assignee)
    ElMessage.success(t('wo.assignedToWorker', { name: assignWorkerForm.assignee }))
    showAssignWorker.value = false
    await loadData()
  } catch { ElMessage.error(t('wo.assignWorkerFailed')) }
  finally { assigning.value = false }
}

// ─── Review ───
async function openReview(row: WorkOrderResponse, mode: "dept" | "admin") {
  reviewMode.value = mode
  reviewTarget.value = row
  reviewReport.value = null
  reviewForm.approved = null
  reviewForm.remark = ""
  showReview.value = true
  reviewLoading.value = true
  try {
    // Fetch the report for this work order
    const res = await reportApi.list({ workOrderId: row.id })
    const reports = res.data.data.records || res.data.data || []
    if (reports.length > 0) {
      reviewReport.value = reports[0]
    }
  } catch { /* ignore */ }
  reviewLoading.value = false
}

async function confirmReview() {
  if (reviewForm.approved === null) return ElMessage.warning(t('wo.needReviewResult'))
  if (reviewForm.approved === false && !reviewForm.remark) return ElMessage.warning(t('wo.needRejectReason'))
  if (!reviewReport.value) return ElMessage.error(t('wo.noRelatedReport'))
  reviewing.value = true
  try {
    const apiCall = reviewMode.value === "dept" ? reportApi.deptReview : reportApi.adminReview
    await apiCall(reviewReport.value.id, {
      approved: reviewForm.approved,
      remark: reviewForm.remark || undefined,
    })
    ElMessage.success(reviewForm.approved ? t('wo.reviewApprovedMsg') : t('wo.reviewRejectedMsg'))
    showReview.value = false
    await loadData()
  } catch { ElMessage.error(t('wo.reviewFailed')) }
  finally { reviewing.value = false }
}

// ─── Detail ───
async function viewDetail(row: WorkOrderResponse) {
  showDetail.value = true
  detailLoading.value = true
  detectionInfo.value = null
  try {
    const r = await workOrderApi.get(row.id)
    detailTarget.value = r.data.data
    if (detailTarget.value?.detectionTaskId) {
      try {
        const dr = await detectionApi.getResult(detailTarget.value.detectionTaskId)
        detectionInfo.value = dr.data.data
      } catch { detectionInfo.value = null }
    }
  } catch {
    detailTarget.value = row
  }
  detailLoading.value = false
}

async function handleCancel(row: WorkOrderResponse) {
  ElMessageBox.prompt(t('wo.cancelReasonLabel'), t('wo.cancelOrderTitle'), { inputPlaceholder: t('wo.cancelPlaceholder') })
    .then(async ({ value }) => {
      try {
        await workOrderApi.cancel(row.id, { reason: value })
        ElMessage.success(t('wo.cancelSuccess'))
        await loadData()
      } catch { ElMessage.error(t('wo.cancelFailed')) }
    }).catch(() => {})
}

function handleExport() {
  const headers = t('wo.exportHeaders').split(',')
  const rows = orders.value.map(o => [
    o.workOrderCode || "", o.title || "", o.location || "",
    severityLabel(o.severityLevel as string),
    statusLabel(o.status), deptLabel(o.departmentCode || ""),
    o.assignee || "", o.createdAt || "",
  ])
  const csv = [headers, ...rows].map(r => r.map(c => `"${c}"`).join(",")).join("\n")
  const blob = new Blob(["\ufeff" + csv], { type: "text/csv;charset=utf-8;" })
  const link = document.createElement("a")
  link.href = URL.createObjectURL(blob)
  link.download = `${t('wo.exportName')}_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success(t('wo.exportSuccess', { count: rows.length }))
}

onMounted(() => {
  // 检查是否有高亮参数
  const highlight = route.query.highlight as string
  if (highlight) {
    highlightedWorkOrderCode.value = highlight
    // 自动搜索该工单编号
    filter.keyword = highlight
  }
  loadData()
  if (isDeptAdmin.value) loadDeptWorkers()
})
</script>

<style scoped>
.wo-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
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
.search-wrap input { border:none; background:transparent; outline:none; font-size:12px; color:#1e293b; width:160px; font-family:inherit; }
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
.td-dept { font-size:12px; color:#4361ee; font-weight:500; }
.code-tag { font-family:monospace; font-size:12px; color:#2563eb; font-weight:600; }
.sev-badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.sev-high { background:#fef2f2; color:#dc2626; }
.sev-med { background:#fffbeb; color:#d97706; }
.sev-low { background:#f0fdf4; color:#16a34a; }
.status-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
.stat-info { background:#f1f5f9; color:#64748b; }
.stat-primary { background:#eef2ff; color:#4361ee; }
.stat-warn { background:#fffbeb; color:#d97706; }
.stat-success { background:#f0fdf4; color:#16a34a; }
.stat-review-dept { background:#fef3c7; color:#92400e; }
.stat-review-admin { background:#e0f2fe; color:#0369a1; }
.stat-rejected { background:#fef2f2; color:#dc2626; }
.stat-closed { background:#f0fdf4; color:#059669; }
.stat-cancel { background:#fef2f2; color:#dc2626; }
.stat-default { background:#f8f9fc; color:#94a3b8; }
.action-group { display:flex; gap:4px; flex-wrap:wrap; }
.action-btn { padding:4px 10px; border:1px solid #e2e8f0; border-radius:5px; background:#fff; font-size:11px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; white-space:nowrap; }
.action-btn:hover { border-color:#2563eb; color:#2563eb; }
.action-danger:hover { border-color:#ef4444; color:#ef4444; }
.action-review { border-color:#fbbf24; color:#92400e; }
.action-review:hover { border-color:#d97706; color:#d97706; background:#fffbeb; }
.empty-row { text-align:center; color:#94a3b8; padding:40px 0 !important; font-size:13px; }

/* 高亮样式 */
.row-highlighted {
  background: #eef2ff !important;
  border-left: 4px solid #4338ca;
  animation: rowHighlightPulse 2s ease-in-out infinite;
}

@keyframes rowHighlightPulse {
  0%, 100% { background: #eef2ff; }
  50% { background: #e0e7ff; }
}

/* 确保高亮行的悬停效果 */
.row-highlighted:hover {
  background: #e0e7ff !important;
}

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
.form-group textarea { resize:vertical; min-height:60px; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }

.detail-label { font-size:11px; color:#64748b; font-weight:600; display:block; }
.detail-val { font-size:13px; color:#1e293b; margin-top:4px; }

.status-timeline { position:relative; padding-left:8px; }
.timeline-item { position:relative; padding:0 0 16px 20px; border-left:2px solid #e2e8f0; }
.timeline-item:last-child { border-left-color:transparent; padding-bottom:0; }
.timeline-dot { position:absolute; left:-5px; top:2px; width:8px; height:8px; border-radius:50%; background:#2563eb; border:2px solid #fff; box-shadow:0 0 0 1px #2563eb; }
.timeline-content { display:flex; flex-direction:column; gap:2px; }
.timeline-status { font-size:12px; font-weight:600; color:#1e293b; }
.timeline-time { font-size:11px; color:#94a3b8; }
.timeline-note { font-size:11px; color:#64748b; }

/* Assign Modal shared */
.required { color:#dc2626; }
.assign-preview { background:#f8f9fc; border:1px solid #eef0f4; border-radius:8px; padding:14px 16px; }
.assign-preview-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px; }
.assign-preview-title { font-size:14px; font-weight:600; color:#0f172a; }
.assign-preview-grid { display:grid; grid-template-columns:1fr 1fr; gap:8px 16px; }
.assign-preview-item { display:flex; flex-direction:column; gap:2px; }
.ap-label { font-size:10px; color:#94a3b8; font-weight:600; text-transform:uppercase; letter-spacing:0.03em; }
.ap-val { font-size:12px; color:#475569; }
.assign-empty-hint { display:flex; align-items:center; gap:8px; margin-top:12px; padding:10px 14px; background:#fffbeb; border:1px solid #fde68a; border-radius:8px; font-size:12px; color:#92400e; }
.assign-worker-info { display:flex; align-items:center; gap:12px; margin-top:14px; padding:12px 14px; background:#f0f7ff; border:1px solid #bfdbfe; border-radius:8px; }
.awi-avatar { width:36px; height:36px; border-radius:50%; background:#2563eb; color:#fff; display:flex; align-items:center; justify-content:center; font-size:14px; font-weight:600; flex-shrink:0; }
.awi-detail { flex:1; }
.awi-name { font-size:13px; font-weight:600; color:#0f172a; }
.awi-meta { display:flex; gap:10px; flex-wrap:wrap; margin-top:2px; }
.awi-meta span { font-size:11px; color:#64748b; }
.awi-workload { text-align:center; padding-left:12px; border-left:1px solid #bfdbfe; }
.awi-wl-num { font-size:18px; font-weight:700; color:#2563eb; display:block; line-height:1; }
.awi-wl-lbl { font-size:10px; color:#64748b; margin-top:2px; display:block; }

.dept-hint { display:flex; align-items:center; gap:8px; margin-top:12px; padding:10px 14px; background:#f0f7ff; border:1px solid #bfdbfe; border-radius:8px; font-size:12px; color:#1e40af; }

/* Review Modal */
.review-order-info { background:#f8f9fc; border:1px solid #eef0f4; border-radius:8px; padding:14px 16px; margin-bottom:16px; }
.review-order-title { font-size:14px; font-weight:600; color:#0f172a; margin-bottom:8px; }
.review-order-meta { display:flex; gap:16px; flex-wrap:wrap; font-size:12px; color:#64748b; }
.review-report { background:#fff; border:1px solid #e2e8f0; border-radius:8px; padding:14px 16px; margin-bottom:16px; }
.review-report-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; padding-bottom:10px; border-bottom:1px solid #f0f2f5; }
.review-report-code { font-size:13px; font-weight:600; color:#2563eb; }
.review-report-grid { display:grid; grid-template-columns:1fr 1fr; gap:10px 16px; margin-bottom:12px; }
.rr-label { font-size:10px; color:#94a3b8; font-weight:600; text-transform:uppercase; letter-spacing:0.03em; display:block; margin-bottom:2px; }
.rr-val { font-size:12px; color:#475569; }
.review-report-desc { padding-top:10px; border-top:1px dashed #e2e8f0; }
.review-report-desc p { font-size:12px; color:#475569; margin:4px 0 0; line-height:1.5; }
.review-prev-remark { margin-top:10px; padding:10px 12px; background:#fef2f2; border:1px solid #fecaca; border-radius:6px; }
.review-prev-remark p { font-size:12px; color:#dc2626; margin:4px 0 0; }
.review-form { padding-top:16px; border-top:1px solid #f0f2f5; }
.review-form-label { font-size:12px; font-weight:600; color:#64748b; margin-bottom:10px; }
.review-actions { display:flex; gap:10px; }
.review-btn { flex:1; display:flex; align-items:center; justify-content:center; gap:8px; padding:12px 16px; border:2px solid #e2e8f0; border-radius:8px; background:#fff; font-size:13px; color:#475569; cursor:pointer; font-family:inherit; transition:all .15s; }
.review-btn:hover { border-color:#cbd5e1; }
.review-btn-pass { border-color:#16a34a; color:#16a34a; background:#f0fdf4; }
.review-btn-reject { border-color:#dc2626; color:#dc2626; background:#fef2f2; }
</style>
