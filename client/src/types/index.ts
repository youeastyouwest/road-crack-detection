// === Enums ===
export enum DamageType {
  TRANSVERSE_CRACK = "TRANSVERSE_CRACK",
  LONGITUDINAL_CRACK = "LONGITUDINAL_CRACK",
  NET_CRACK = "NET_CRACK",
  POTHOLE = "POTHOLE",
  MARKING_DAMAGE = "MARKING_DAMAGE",
  ROAD_SPILL = "ROAD_SPILL",
  OTHER = "OTHER",
}
export enum SeverityLevel { LOW = "LOW", MEDIUM = "MEDIUM", HIGH = "HIGH" }
export enum DataSourceType { IMAGE = "IMAGE", VIDEO = "VIDEO", CROWD_SOURCE = "CROWD_SOURCE" }
export enum DetectionTaskStatus { PENDING = "PENDING", PROCESSING = "PROCESSING", COMPLETED = "COMPLETED", FAILED = "FAILED" }
export enum WorkOrderStatus { PENDING = "PENDING", ASSIGNED = "ASSIGNED", IN_PROGRESS = "IN_PROGRESS", COMPLETED = "COMPLETED", CANCELLED = "CANCELLED" }
export enum DepartmentCode { ROAD_ADMIN = "ROAD_ADMIN", SANITATION = "SANITATION", TRAFFIC_POLICE = "TRAFFIC_POLICE" }

// === Backend Role Codes (6 roles) ===
export const RoleCode = {
  ADMIN: "ROLE_ADMIN",
  ROAD_ADMIN: "ROLE_ROAD_ADMIN",
  SANIT_ADMIN: "ROLE_SANIT_ADMIN",
  TRAFFIC_ADMIN: "ROLE_TRAFFIC_ADMIN",
  MAINTAINER: "ROLE_MAINTAINER",
  CROWDSOURCE: "ROLE_CROWDSOURCE",
} as const

export type RoleCodeType = typeof RoleCode[keyof typeof RoleCode]

export const MAINTENANCE_ROLES = [RoleCode.ROAD_ADMIN, RoleCode.SANIT_ADMIN, RoleCode.TRAFFIC_ADMIN, RoleCode.MAINTAINER]
export const DEPT_ADMIN_ROLES = [RoleCode.ROAD_ADMIN, RoleCode.SANIT_ADMIN, RoleCode.TRAFFIC_ADMIN]

export interface ApiResponse<T> { code: number; message: string; data: T; timestamp: number }
export interface PageResponse<T> { records: T[]; total: number; size: number; current: number; pages: number }
export interface LoginRequest { username: string; password: string }
export interface LoginResponse { accessToken: string; refreshToken: string; tokenType: string; expiresIn: number; userId: number; username: string; realName: string; roles: string[] }
export interface RegisterRequest { username: string; password: string; email: string; realName?: string; phone?: string; code: string; deptId?: number }
export interface ChangePasswordRequest { oldPassword: string; newPassword: string }
export interface ResetPasswordRequest { email: string; newPassword: string; code: string }
export interface UserEntity { id?: number; username: string; password?: string; realName: string; email?: string; phone?: string; deptId?: number; deptName?: string; avatar?: string; status: number; lastLoginAt?: string; lastLoginIp?: string; createdAt?: string; updatedAt?: string }
export interface UserDetailResponse { user: UserEntity; roles: string[]; roleIds?: number[] }
export interface UserPageQuery { page?: number; size?: number; username?: string; realName?: string; status?: number; deptId?: number }
export interface CreateDetectionTaskRequest { roadSegmentId?: number; roadName?: string; location?: string; dataSourceType: DataSourceType; files?: string[]; images?: string[]; description?: string; submittedBy?: string }
export interface DetectionTaskResponse { id: number; taskCode?: string; roadName?: string; roadSegmentName?: string; location?: string; dataSourceType: DataSourceType; status: DetectionTaskStatus; crackCount?: number; severityLevel?: string; submittedBy?: string; description?: string; failureReason?: string; createdAt: string; updatedAt?: string; completedAt?: string }
export interface DetectionResultResponse { taskId: number; summary?: string; items: DetectionItemResponse[]; generatedWorkOrderId?: number; completedAt: string }
export interface DetectionItemResponse { damageType: DamageType; severityLevel: SeverityLevel; confidence: number; boundingBox?: BoundingBoxResponse; lng?: number; lat?: number; lengthMm?: number; widthMm?: number; areaMm2?: number; snapshotUrl?: string; suggestion?: string }
export interface BoundingBoxResponse { x: number; y: number; width: number; height: number }
export interface DetectionProgressMessage { type: string; taskId: number; status: DetectionTaskStatus; progress: number; message: string; timestamp: string }
export interface CreateWorkOrderRequest { detectionTaskId: number; roadSegmentId?: number; title: string; damageType: DamageType; severityLevel: SeverityLevel; location?: string; departmentCode: DepartmentCode; assignee?: string; description?: string; deadline?: string }
export interface WorkOrderResponse { id: number; workOrderCode?: string; title: string; roadName?: string; location?: string; damageType?: DamageType; severityLevel?: SeverityLevel; departmentCode?: string; assignee?: string; status: WorkOrderStatus; description?: string; deadline?: string; dueAt?: string; acceptedAt?: string; finishedAt?: string; createdAt: string; updatedAt?: string }
export interface AssignWorkOrderRequest { assignee: string; note?: string }
export interface UpdateWorkOrderStatusRequest { status: WorkOrderStatus; note?: string }
export interface CancelWorkOrderRequest { reason: string }
export interface DepartmentEntity { id?: number; name: string; code: string; parentId?: number; description?: string; status: number; children?: DepartmentEntity[] }
export interface RoleEntity { id?: number; name: string; code: string; status: number; description?: string }
export interface CreateMaintenanceReportRequest { workOrderId: number; executor: string; beforeImageUrl?: string; afterImageUrl?: string; materials?: string; description?: string; finishedAt: string }
export interface MaintenanceReportResponse { id: number; reportCode?: string; workOrderId: number; workOrderTitle?: string; roadName?: string; repairMethod?: string; executor: string; beforeImageUrl?: string; afterImageUrl?: string; materials?: string; description?: string; status?: string; finishedAt: string; createdAt: string }
export interface AlertMessageResponse { type: string; level: string; taskId?: number; title?: string; message: string; timestamp: string }
export interface StatisticsSummary { totalRoads: number; monitoredRoads: number; detectionToday: number; pendingAlerts: number; totalCracksDetected: number; totalWorkOrders: number }
export interface StatisticsTrend { date: string; count: number }
export interface CrackTypeDistribution { type: string; count: number; percentage: number }
export interface SeverityDistribution { level: string; count: number; percentage: number }
export interface DepartmentWorkload { deptName: string; total: number; completed: number; pending: number }
export interface RoadHealthScore { roadName: string; score: number; trend: string }
export interface AlertRecord { id: number; type: string; level: string; roadName?: string; location?: string; title?: string; description?: string; status: string; createdAt: string; resolvedAt?: string }
export interface RoadEntity { id: number; name: string; code: string; length?: number; lanes?: number; surfaceType?: string; status: string; lastInspection?: string; createdAt?: string }
export interface SystemConfigResponse { siteName: string; logo: string; detectionInterval: number; alertThreshold: number; maintenanceNotify: boolean; dataRetentionDays: number; maxUploadSize: number; allowRegister: boolean; emailNotify: boolean; smsNotify: boolean; darkMode: boolean; language: string }
export interface AuditLogResponse { id: number; username: string; action: string; module: string; ip: string; duration: number; status: string; createdAt: string }