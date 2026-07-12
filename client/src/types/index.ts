// === Enums ===
export enum DamageType {
  CRACK = "CRACK",
  MARKING_DAMAGE = "MARKING_DAMAGE",
  ROAD_SPILL = "ROAD_SPILL",
  POTHOLE = "POTHOLE",
  UNKNOWN = "UNKNOWN",
}
export enum SeverityLevel { LOW = "LOW", MEDIUM = "MEDIUM", HIGH = "HIGH" }
export enum DataSourceType { DRONE_VIDEO = "DRONE_VIDEO", MANUAL_IMAGE = "MANUAL_IMAGE", MANUAL_VIDEO = "MANUAL_VIDEO", CROWD_SOURCE = "CROWD_SOURCE" }
export enum DetectionTaskStatus { PENDING = "PENDING", PROCESSING = "PROCESSING", COMPLETED = "COMPLETED", FAILED = "FAILED" }
export enum WorkOrderStatus {
  PENDING_ASSIGNMENT = "PENDING_ASSIGNMENT",
  ASSIGNED = "ASSIGNED",
  IN_PROGRESS = "IN_PROGRESS",
  COMPLETED = "COMPLETED",
  PENDING_DEPT_REVIEW = "PENDING_DEPT_REVIEW",
  PENDING_ADMIN_REVIEW = "PENDING_ADMIN_REVIEW",
  REJECTED = "REJECTED",
  CLOSED = "CLOSED",
  CANCELLED = "CANCELLED",
}
export enum DepartmentCode { ROAD_ADMIN = "ROAD_ADMIN", SANITATION = "SANITATION", TRAFFIC_POLICE = "TRAFFIC_POLICE" }

// === Backend Role Codes (6 roles) ===
export const RoleCode = {
  ADMIN: "ROLE_ADMIN",
  ROAD_ADMIN: "ROLE_ROAD_ADMIN",
  SANIT_ADMIN: "ROLE_SANIT_ADMIN",
  TRAFFIC_ADMIN: "ROLE_TRAFFIC_ADMIN",
  MAINTAINER: "ROLE_MAINTAINER",
  CROWDSOURCE: "ROLE_CROWDSOURCE",
  VIEWER: "ROLE_VIEWER",
} as const

export type RoleCodeType = typeof RoleCode[keyof typeof RoleCode]

export const MAINTENANCE_ROLES = [RoleCode.ROAD_ADMIN, RoleCode.SANIT_ADMIN, RoleCode.TRAFFIC_ADMIN, RoleCode.MAINTAINER]
export const DEPT_ADMIN_ROLES = [RoleCode.ROAD_ADMIN, RoleCode.SANIT_ADMIN, RoleCode.TRAFFIC_ADMIN]

export interface ApiResponse<T> { code: number; message: string; data: T; timestamp: number }
export interface PageResponse<T> { records: T[]; total: number; size: number; current: number; pages: number }
export interface LoginRequest { username: string; password: string }
export interface LoginResponse { accessToken: string; refreshToken: string; tokenType: string; expiresIn: number; userId: number; username: string; realName: string; roles: string[] }
export interface RegisterRequest { username: string; password: string; email: string; realName?: string; phone?: string; code: string; deptId?: number; roleId?: number }
export interface ChangePasswordRequest { oldPassword: string; newPassword: string }
export interface ResetPasswordRequest { email: string; newPassword: string; code: string }
export interface UserEntity { id?: number; username: string; password?: string; realName: string; email?: string; phone?: string; deptId?: number; deptName?: string; avatar?: string; roleCode?: string; roleName?: string; roleId?: number; status: number; lastLoginAt?: string; lastLoginIp?: string; createdAt?: string; updatedAt?: string }
export interface UserDetailResponse { user: UserEntity; roles: string[]; roleIds?: number[] }
export interface UserPageQuery { page?: number; size?: number; username?: string; realName?: string; status?: number; deptId?: number }
export interface DetectionTaskResponse { id: number; taskCode?: string; dataSourceType: DataSourceType; fileName?: string; fileUrl?: string; location?: string; remark?: string; submittedBy?: string; status: DetectionTaskStatus; failureReason?: string; createdAt: string; updatedAt?: string; result?: DetectionResultResponse }
export interface DetectionResultResponse { taskId: number; summary?: string; items: DetectionItemResponse[]; generatedWorkOrderId?: number; completedAt: string; imageBase64?: string }
export interface DetectionItemResponse { damageType: string; severityLevel: string; confidence: number; boundingBox?: BoundingBoxResponse; suggestion?: string }
export interface BoundingBoxResponse { x: number; y: number; width: number; height: number }
export interface DetectionProgressMessage { type: string; taskId: number; status: DetectionTaskStatus; progress: number; message: string; timestamp: string }
export interface CreateWorkOrderRequest { detectionTaskId: number; title: string; damageType: DamageType; severityLevel: SeverityLevel; location: string; departmentCode: DepartmentCode; evidenceUrl?: string; description?: string }
export interface WorkOrderResponse { id: number; workOrderCode?: string; detectionTaskId?: number; title: string; damageType?: string; severityLevel?: string; location?: string; departmentCode?: string; assignee?: string; status: WorkOrderStatus; evidenceUrl?: string; description?: string; dueAt?: string; createdAt: string; updatedAt?: string; statusLogs?: WorkOrderStatusLogResponse[] }
export interface WorkOrderStatusLogResponse { fromStatus?: string; toStatus: string; note?: string; operatedAt: string }
export interface AssignWorkOrderRequest { departmentCode: DepartmentCode; assignee?: string }
export interface UpdateWorkOrderStatusRequest { status: string; note?: string }
export interface CancelWorkOrderRequest { reason: string }
export interface DepartmentEntity { id?: number; name: string; code: string; parentId?: number; description?: string; sortOrder?: number; status: number; children?: DepartmentEntity[]; createdAt?: string; updatedAt?: string }
export interface RoleEntity { id?: number; name: string; code: string; status: number; description?: string; createdAt?: string; updatedAt?: string }
export interface CreateMaintenanceReportRequest { workOrderId: number; executor: string; beforeImageUrl?: string; afterImageUrl?: string; materials?: string; description?: string; finishedAt: string }
export interface MaintenanceReportResponse {
  id: number
  reportCode?: string
  workOrderId: number
  executor: string
  beforeImageUrl?: string
  afterImageUrl?: string
  materials?: string
  description?: string
  finishedAt: string
  status?: string
  reviewRemark?: string
  reviewer?: string
  reviewedAt?: string
  createdAt: string
}
export interface ReviewRequest {
  approved: boolean
  remark?: string
}
export interface AlertMessageResponse { type: string; level: string; taskId?: number; title?: string; message: string; timestamp: string }
export interface DashboardStatsResponse { totalRoads?: number; monitoredRoads?: number; detectionToday?: number; pendingAlerts?: number; totalCracksDetected?: number; totalWorkOrders?: number; [key: string]: any }
export interface TrendStatsItem { date: string; count: number }
export interface CrackTypeStatItem { type: string; count: number; percentage: number }
export interface SeverityStatItem { level: string; count: number; percentage: number }
export interface DeptWorkloadItem { deptName: string; total: number; completed: number; pending: number }
export interface AlertResponse { id: number; alertCode?: string; alertType: string; alertLevel: string; title: string; content?: string; damageType?: string; location?: string; workOrderId?: number; detectionTaskId?: number; status: string; handledBy?: string; handledAt?: string; handleRemark?: string; createdAt: string }
export interface CrowdReportResponse { id: number; reportCode?: string; reporterName: string; reporterPhone: string; location: string; lng?: number; lat?: number; damageType: string; severityLevel: string; description?: string; imageUrl?: string; status: string; remark?: string; reviewedBy?: string; reviewedAt?: string; detectionTaskId?: number; workOrderId?: number; createdAt: string; updatedAt?: string }
export interface MapMarkerResponse { id: number; longitude: number; latitude: number; damageType: string; severity: string; roadName?: string; status: string; taskId?: number; address?: string }
export interface MapMarkerDetailResponse { id: number; longitude: number; latitude: number; damageType: string; severity: string; roadName?: string; description?: string; status: string; roadSegment?: string; departmentName?: string; distance?: string; confidence?: number; detectedAt?: string; imageUrls?: string[]; workOrderStatus?: string }
export interface MapStatisticsResponse { totalMarkers: number; newMarkers: number; repairedCount: number; pendingRepair: number; highSeverityCount: number; mediumSeverityCount: number; lowSeverityCount: number }
export interface MapTrendPointResponse { date: string; count: number; repairedCount: number }
export interface MapDamageTypeRatioResponse { damageType: string; count: number; ratio: number }
export interface RoadResponse { id: number; roadCode?: string; roadName: string; roadGrade?: string; district?: string; startPoint?: string; endPoint?: string; lengthKm?: number; laneCount?: number; surfaceType?: string; builtYear?: number; lastMaintained?: string; healthScore?: number; damageLevel?: string; latestDetectionAt?: string; totalDetectionCount?: number; currentDamageCount?: number; departmentCode?: string; status: string; remark?: string; createdAt: string }
export interface RoadHealthArchiveResponse { id: number; roadId: number; road?: RoadResponse; archiveDate: string; healthScore?: number; damageLevel?: string; totalDetectionCount?: number; totalDamageCount?: number; crackCount?: number; potholeCount?: number; markingDamageCount?: number; roadSpillCount?: number; unknownCount?: number; severityLowCount?: number; severityMediumCount?: number; severityHighCount?: number; evaluation?: string; suggestion?: string; createdAt: string; updatedAt?: string }
export interface RoadHealthArchiveDashboardResponse { totalRoads: number; archivedRoads: number; averageHealthScore: number; healthyRoads: number; subHealthyRoads: number; unhealthyRoads: number }
export interface FileUploadResponse { url: string; originalName: string; size: number; contentType: string }
export interface AgentChatResponse { sessionId: string; question?: string; answer: string; dataSource?: string; timestamp: number }
export interface AgentDetectImageResponse { taskId?: string; hasCrack: boolean; numDetections: number; crackTypes?: string[]; detectionData?: any; resultImageBase64?: string; advice?: string; dataSource?: string; generatedWorkOrderId?: number; workOrderStatus?: string; dispatchedDepartment?: string; dispatchedAssignee?: string; timestamp: number }
export interface AgentReportResponse { reportId: string; reportType?: string; title?: string; summary?: string; content?: string; keyStats?: Record<string, any>; suggestions?: string[]; timestamp: number }
export interface AuditLogResponse { id?: number; operator?: string; username?: string; action?: string; module?: string; description?: string; ip?: string; duration?: number; costTime?: number; status?: string; errorMsg?: string; createdAt?: string; [key: string]: any }
export interface SystemConfigResponse { siteName?: string; siteLogo?: string; language?: string; allowRegister?: boolean; detectionInterval?: number; alertThreshold?: number; dataRetentionDays?: number; maxUploadSize?: number; emailNotification?: boolean; smsNotification?: boolean; maintenanceAlert?: boolean; minPasswordLength?: number; maxLoginAttempts?: number; sessionTimeout?: number; captchaEnabled?: boolean; darkMode?: boolean; [key: string]: any }
export interface SystemConfigRequest { siteName: string; language: string; allowRegister: boolean; detectionInterval: number; alertThreshold: number; dataRetentionDays: number; maxUploadSize: number; emailNotification: boolean; smsNotification: boolean; maintenanceAlert: boolean; minPasswordLength: number; maxLoginAttempts: number; sessionTimeout: number; captchaEnabled: boolean; darkMode: boolean }
export interface CreateCrowdReportRequest { reporterName?: string; reporterPhone?: string; location: string; lng?: number; lat?: number; damageType?: string; severityLevel?: string; description?: string; imageUrl: string }
export interface ReviewCrowdReportRequest { action: string; remark?: string }
export interface CreateDetectionTaskSubmitRequest { dataSourceType: DataSourceType; location: string; fileName?: string; fileUrl?: string; remark?: string }
export interface GenerateArchiveRequest { roadId: number; archiveDate: string }
export interface GenerateReportRequest { reportType: string; roadSection?: string; startTime?: string; endTime?: string }
export interface RoadDiseaseSummaryResponse {
  roadId: number
  roadName: string
  centerLng: number
  centerLat: number
  pathPoints: number[][]
  totalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  overallSeverity: string
  diseasePoints: DiseasePoint[]
}

export interface DiseasePoint {
  taskId: number
  lng: number
  lat: number
  damageType: string
  severity: string
  confidence: number
  detectionTime: string
  address: string
  workOrderNo: string
  imageBase64?: string
  fileUrl?: string
  bbox?: string
}
