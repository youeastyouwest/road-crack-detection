# 途安智巡 — 前后端接口文档

> 版本: v1.0  
> 更新日期: 2026-07-11  
> 基础路径: `/api`  
> 数据格式: `application/json`（文件上传使用 `multipart/form-data`）  
> 认证方式: `Bearer {accessToken}` 放在 `Authorization` 请求头

---

## 目录

1. [通用响应格式](#通用响应格式)
2. [枚举定义](#枚举定义)
3. [认证模块 Auth](#1-认证模块-auth)
4. [用户管理 User](#2-用户管理-user)
5. [角色管理 Role](#3-角色管理-role)
6. [部门管理 Department](#4-部门管理-department)
7. [检测任务 DetectionTask](#5-检测任务-detectiontask)
8. [AI Agent 智能服务](#6-ai-agent-智能服务)
9. [预警管理 Alert](#7-预警管理-alert)
10. [众包上报 CrowdReport](#8-众包上报-crowdreport)
11. [工单管理 WorkOrder](#9-工单管理-workorder)
12. [维修报告 MaintenanceReport](#10-维修报告-maintenancereport)
13. [地图点位 Map](#11-地图点位-map)
14. [道路管理 Road](#12-道路管理-road)
15. [健康档案 RoadHealthArchive](#13-健康档案-roadhealtharchive)
16. [统计分析 Statistics](#14-统计分析-statistics)
17. [审计日志 AuditLog](#15-审计日志-auditlog)
18. [文件上传 File](#16-文件上传-file)
19. [健康检查](#17-健康检查)

---

## 通用响应格式

### ApiResponse<T>

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1744000000000
}
```

### PageResponse<T>

```json
{
  "records": [ ... ],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

---

## 枚举定义

### DamageType
| 值 | 说明 |
|----|------|
| `CRACK` | 裂缝 |
| `MARKING_DAMAGE` | 标线损坏 |
| `ROAD_SPILL` | 路面抛洒 |
| `POTHOLE` | 坑槽 |
| `UNKNOWN` | 未知 |

### SeverityLevel
| 值 | 说明 |
|----|------|
| `LOW` | 低 |
| `MEDIUM` | 中 |
| `HIGH` | 高 |

### WorkOrderStatus
| 值 | 说明 |
|----|------|
| `PENDING_ASSIGNMENT` | 待指派 |
| `ASSIGNED` | 已指派 |
| `IN_PROGRESS` | 处理中 |
| `COMPLETED` | 已完成 |
| `CLOSED` | 已关闭 |
| `CANCELLED` | 已取消 |

### DetectionTaskStatus
| 值 | 说明 |
|----|------|
| `PENDING` | 待处理 |
| `PROCESSING` | 处理中 |
| `COMPLETED` | 已完成 |
| `FAILED` | 失败 |

### DataSourceType
| 值 | 说明 |
|----|------|
| `DRONE_VIDEO` | 无人机视频 |
| `MANUAL_IMAGE` | 人工拍摄图片 |
| `MANUAL_VIDEO` | 人工拍摄视频 |
| `CROWD_SOURCE` | 众包来源 |

### CrowdReportStatus
| 值 | 说明 |
|----|------|
| `PENDING` | 待审核 |
| `APPROVED` | 已通过 |
| `REJECTED` | 已驳回 |

### DepartmentCode
| 值 | 说明 |
|----|------|
| `ROAD_ADMIN` | 道路管理局 |
| `SANITATION` | 环卫部门 |
| `TRAFFIC_POLICE` | 交警部门 |

---

## 1. 认证模块 Auth

`/api/auth`

### POST /login — 登录

**Request:**
```json
{ "username": "admin", "password": "123456" }
```

**Response (LoginResponse):**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "username": "admin",
  "realName": "管理员",
  "roles": ["ROLE_ADMIN"]
}
```

### POST /refresh — 刷新令牌

**参数:** `refreshToken` (query)

### POST /register — 注册

**Request:**
```json
{
  "username": "user1",
  "password": "pass123",
  "email": "user@example.com",
  "realName": "用户",
  "phone": "13800138000",
  "code": "123456",
  "deptId": 1
}
```

### POST /send-code — 发送验证码

**参数:** `email` (query), `type` (query, 1=注册 2=重置)

### PUT /change-password — 修改密码

**Request:**
```json
{ "oldPassword": "old123", "newPassword": "new456" }
```

### POST /reset-password — 重置密码

**Request:**
```json
{ "email": "user@example.com", "newPassword": "new456", "code": "123456" }
```

---

## 2. 用户管理 User

> ⚠ 仅在 `crack.persistence.mode=db` 时生效

`/api/user`

### GET /page — 分页查询

**参数:** `page`, `size`, `username`, `realName`, `status`, `deptId`

### GET /{id} — 用户详情

**Response:** `UserEntity` + `roles: string[]` + `roleIds: number[]`

### GET /current — 当前登录用户

### POST — 创建用户

**Request:** `UserEntity` + `roleIds` (query)

### PUT /{id} — 更新用户

### DELETE /{id} — 删除用户

### PUT /{id}/status — 启用/禁用

**参数:** `status` (0=禁用, 1=启用)

### PUT /{id}/reset-password — 重置密码

---

## 3. 角色管理 Role

> ⚠ 仅在 `crack.persistence.mode=db` 时生效

`/api/role`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 角色列表 |
| GET | `/{id}` | 角色详情 |
| POST | `/` | 创建角色 |
| PUT | `/{id}` | 更新角色 |
| DELETE | `/{id}` | 删除角色 |

---

## 4. 部门管理 Department

> ⚠ 仅在 `crack.persistence.mode=db` 时生效

`/api/department`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/tree` | 部门树 |
| GET | `/list` | 部门列表 |
| GET | `/{id}` | 部门详情 |
| POST | `/` | 创建部门 |
| PUT | `/{id}` | 更新部门 |
| DELETE | `/{id}` | 删除部门 |

---

## 5. 检测任务 DetectionTask

`/api/detection-tasks`

### POST / — 创建任务 (JSON)

**Request (CreateDetectionTaskRequest):**
```json
{
  "dataSourceType": "MANUAL_IMAGE",
  "fileName": "photo.jpg",
  "fileUrl": "/uploads/xxx.jpg",
  "location": "北京路100号",
  "remark": ""
}
```

**Response (DetectionTaskResponse):**
```json
{
  "id": 1,
  "taskCode": "DT20260711001",
  "dataSourceType": "MANUAL_IMAGE",
  "fileName": "photo.jpg",
  "fileUrl": "/uploads/xxx.jpg",
  "location": "北京路100号",
  "remark": "",
  "submittedBy": "admin",
  "status": "PENDING",
  "failureReason": null,
  "createdAt": "2026-07-11T10:00:00",
  "updatedAt": null,
  "result": null
}
```

### POST /upload — 上传文件创建任务 (multipart)

**参数:** `dataSourceType`, `location`, `remark`, `file`

### POST /{taskId}/execute — 执行检测

### GET / — 分页列表

**参数:** `page`, `size`, `status`, `dataSourceType`, `location`, `submittedBy`

### GET /{taskId} — 获取任务详情

### GET /{taskId}/result — 获取检测结果

**Response (DetectionResultResponse):**
```json
{
  "taskId": 1,
  "summary": "发现3处裂缝",
  "items": [
    {
      "damageType": "CRACK",
      "severityLevel": "HIGH",
      "confidence": 0.92,
      "boundingBox": { "x": 100, "y": 200, "width": 50, "height": 30 },
      "suggestion": "建议立即维修"
    }
  ],
  "generatedWorkOrderId": null,
  "completedAt": "2026-07-11T10:01:00",
  "imageBase64": null
}
```

### DELETE /{taskId} — 删除任务

---

## 6. AI Agent 智能服务

`/api/agent`

### POST /chat — 智能问答

**Request:**
```json
{
  "message": "当前道路状况如何？",
  "sessionId": "abc123",
  "includeContext": true
}
```

**Response (ChatResponse):**
```json
{
  "sessionId": "abc123",
  "question": "当前道路状况如何？",
  "answer": "根据最新检测数据，北京路有3处裂缝需要关注...",
  "dataSource": "系统数据",
  "timestamp": 1744000000
}
```

### POST /detect-image — 图片智能检测 (multipart)

**参数 (multipart):**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `image` | File | ✅ | 图片文件 |
| `question` | String | ❌ | 提问 |
| `generateAdvice` | Boolean | ❌ | 生成建议 (默认true) |
| `autoGenerateWorkOrder` | Boolean | ❌ | 自动创建工单 (默认true) |
| `autoDispatch` | Boolean | ❌ | 自动派发 (默认true) |
| `location` | String | ❌ | 位置 |
| `confidenceThreshold` | Double | ❌ | 置信度阈值 |

**Response (DetectImageResponse):**
```json
{
  "taskId": "agent-task-xxx",
  "hasCrack": true,
  "numDetections": 3,
  "crackTypes": ["CRACK"],
  "detectionData": {},
  "resultImageBase64": "/9j/...",
  "advice": "建议修补裂缝",
  "dataSource": "MANUAL_IMAGE",
  "generatedWorkOrderId": 100,
  "workOrderStatus": "PENDING_ASSIGNMENT",
  "dispatchedDepartment": "ROAD_ADMIN",
  "dispatchedAssignee": "张三",
  "timestamp": 1744000000
}
```

### POST /report — 生成报告

**Request:**
```json
{
  "reportType": "monthly",
  "roadSection": "北京路-上海路段",
  "startTime": "2026-06-01",
  "endTime": "2026-06-30"
}
```

**Response (AgentReportResponse):**
```json
{
  "reportId": "rpt-001",
  "reportType": "monthly",
  "title": "月度道路健康报告",
  "summary": "本月共检测...",
  "content": "详细内容...",
  "keyStats": { "totalDetections": 150, "totalCracks": 45 },
  "suggestions": ["建议优先处理高严重度裂缝"],
  "timestamp": 1744000000
}
```

---

## 7. 预警管理 Alert

`/api/alerts`

### GET / — 分页列表

**参数:** `page`, `size`, `alertLevel`, `alertType`, `status`

**Response (PageResponse<AlertResponse>):**
```json
{
  "records": [{
    "id": 1,
    "alertCode": "ALT20260711001",
    "alertType": "CRACK_DETECTED",
    "alertLevel": "HIGH",
    "title": "检测到高严重度裂缝",
    "content": "在北京路发现...",
    "damageType": "CRACK",
    "location": "北京路100号",
    "workOrderId": null,
    "detectionTaskId": 1,
    "status": "PENDING",
    "handledBy": null,
    "handledAt": null,
    "handleRemark": null,
    "createdAt": "2026-07-11T10:00:00"
  }],
  "total": 1, "size": 10, "current": 1, "pages": 1
}
```

### GET /recent — 最近预警

**参数:** `count` (默认5)

---

## 8. 众包上报 CrowdReport

`/api/crowd-reports`

### POST / — 提交上报

**Request:**
```json
{
  "reporterName": "张三",
  "reporterPhone": "13800000000",
  "location": "北京路100号",
  "lng": 116.4,
  "lat": 39.9,
  "damageType": "POTHOLE",
  "severityLevel": "MEDIUM",
  "description": "路边有个坑",
  "imageUrl": "/uploads/xxx.jpg"
}
```

### GET / — 列表

**参数:** `page`, `size`, `status`, `reporterPhone`, `location`

### GET /{reportId} — 详情

### PUT /{reportId}/review — 审核

**参数:** `reviewer` (query, 默认admin)

**Request:**
```json
{ "action": "APPROVE", "remark": "已确认" }
```

---

## 9. 工单管理 WorkOrder

`/api/work-orders`

### POST / — 创建工单

**Request (CreateWorkOrderRequest):**
```json
{
  "detectionTaskId": 1,
  "title": "北京路裂缝修复",
  "damageType": "CRACK",
  "severityLevel": "HIGH",
  "location": "北京路100号",
  "departmentCode": "ROAD_ADMIN",
  "evidenceUrl": "/uploads/xxx.jpg",
  "description": "需要紧急修复"
}
```

**Response (WorkOrderResponse):**
```json
{
  "id": 100,
  "workOrderCode": "WO20260711001",
  "detectionTaskId": 1,
  "title": "北京路裂缝修复",
  "damageType": "CRACK",
  "severityLevel": "HIGH",
  "location": "北京路100号",
  "departmentCode": "ROAD_ADMIN",
  "assignee": null,
  "status": "PENDING_ASSIGNMENT",
  "evidenceUrl": "/uploads/xxx.jpg",
  "description": "需要紧急修复",
  "dueAt": null,
  "createdAt": "2026-07-11T10:00:00",
  "updatedAt": null,
  "statusLogs": []
}
```

### GET / — 分页列表

**参数:** `page`, `size`, `status`, `departmentCode`, `severityLevel`, `assignee`, `keyword`

### GET /{workOrderId} — 工单详情

### PUT /{workOrderId}/assign — 指派

**Request:**
```json
{ "departmentCode": "ROAD_ADMIN", "assignee": "张三" }
```

### PUT /{workOrderId}/status — 更新状态

**Request:**
```json
{ "status": "IN_PROGRESS", "note": "开始维修" }
```

### PUT /{workOrderId}/cancel — 取消

**Request:**
```json
{ "reason": "无需处理" }
```

---

## 10. 维修报告 MaintenanceReport

`/api/maintenance-reports`

### POST / — 创建报告

**Request:**
```json
{
  "workOrderId": 100,
  "executor": "张三",
  "beforeImageUrl": "/uploads/before.jpg",
  "afterImageUrl": "/uploads/after.jpg",
  "materials": "沥青3吨",
  "description": "已完成修补",
  "finishedAt": "2026-07-11T15:00:00"
}
```

### GET / — 列表

**参数:** `page`, `size`, `workOrderId`, `executor`

### GET /{reportId} — 详情

---

## 11. 地图点位 Map

`/api/map`

### GET /markers — 点位列表

**参数:** `damageType`, `severityLevel`, `status`, `hasWorkOrder`, `onlyWithCoordinates`, `keyword`

**Response (List<MapMarkerResponse>):**
```json
[{
  "id": 1,
  "longitude": 116.397,
  "latitude": 39.909,
  "damageType": "CRACK",
  "severity": "HIGH",
  "roadName": "北京路",
  "status": "PENDING_ASSIGNMENT",
  "taskId": 1,
  "address": "北京路100号"
}]
```

### GET /markers/{markerId} — 点位详情

**Response (MapMarkerDetailResponse):**
```json
{
  "id": 1,
  "longitude": 116.397,
  "latitude": 39.909,
  "damageType": "CRACK",
  "severity": "HIGH",
  "roadName": "北京路",
  "description": "路面裂缝",
  "status": "PENDING_ASSIGNMENT",
  "roadSegment": "K1+200",
  "departmentName": "道路管理局",
  "distance": "50m",
  "confidence": 0.92,
  "detectedAt": "2026-07-11T10:00:00",
  "imageUrls": ["/uploads/xxx.jpg"],
  "workOrderStatus": "PENDING_ASSIGNMENT"
}
```

### GET /statistics — 统计概览

**Response (MapStatisticsResponse):**
```json
{
  "totalMarkers": 150,
  "newMarkers": 10,
  "repairedCount": 80,
  "pendingRepair": 60,
  "highSeverityCount": 15,
  "mediumSeverityCount": 40,
  "lowSeverityCount": 95
}
```

### GET /trend — 趋势

**参数:** `days` (默认7)

**Response (List<MapTrendPointResponse>):**
```json
[{
  "date": "2026-07-04",
  "count": 5,
  "repairedCount": 3
}]
```

### GET /damage-types — 损坏类型分布

**Response (List<MapDamageTypeRatioResponse>):**
```json
[{
  "damageType": "CRACK",
  "count": 80,
  "ratio": 0.53
}]
```

---

## 12. 道路管理 Road

`/api/roads`

### GET / — 分页列表

**参数:** `page`, `size`, `roadName`, `district`, `roadGrade`, `status`

**Response (PageResponse<RoadResponse>):**
```json
{
  "records": [{
    "id": 1,
    "roadCode": "RD001",
    "roadName": "北京路",
    "roadGrade": "主干路",
    "district": "朝阳区",
    "startPoint": "A点",
    "endPoint": "B点",
    "lengthKm": 5.2,
    "laneCount": 4,
    "surfaceType": "沥青",
    "builtYear": 2010,
    "lastMaintained": "2025-01-01T00:00:00",
    "healthScore": 85.5,
    "damageLevel": "良好",
    "latestDetectionAt": "2026-07-11T10:00:00",
    "totalDetectionCount": 20,
    "currentDamageCount": 3,
    "departmentCode": "ROAD_ADMIN",
    "status": "IN_USE",
    "remark": null,
    "createdAt": "2026-01-01T00:00:00"
  }],
  "total": 1, "size": 10, "current": 1, "pages": 1
}
```

---

## 13. 健康档案 RoadHealthArchive

`/api/road-health-archives`

### POST /generate — 生成档案

**Request:**
```json
{ "roadId": 1, "archiveDate": "2026-07-11" }
```

### GET / — 分页列表

**参数:** `page`, `size`, `roadId`, `damageLevel`, `startDate`, `endDate`

### GET /{archiveId} — 详情

### GET /dashboard — 看板

**Response (RoadHealthArchiveDashboardResponse):**
```json
{
  "totalRoads": 50,
  "archivedRoads": 30,
  "averageHealthScore": 78.5,
  "healthyRoads": 15,
  "subHealthyRoads": 10,
  "unhealthyRoads": 5
}
```

---

## 14. 统计分析 Statistics

`/api/statistics`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dashboard` | 仪表盘统计 |
| GET | `/trend?days=30` | 趋势数据 |
| GET | `/crack-type` | 裂缝类型分布 |
| GET | `/severity` | 严重程度分布 |
| GET | `/department-workload` | 部门工作量 |

---

## 15. 审计日志 AuditLog

`/api/audit-log`

### GET /page — 分页查询

**参数:** `page`, `size`, `module`, `status`

**Response (PageResponse<AuditLogResponse>):**
```json
{
  "records": [{
    "id": 1,
    "operator": "admin",
    "action": "LOGIN",
    "target": "系统",
    "detail": "登录系统",
    "ip": "127.0.0.1",
    "duration": 150,
    "status": "SUCCESS",
    "createdAt": "2026-07-11T10:00:00"
  }]
}
```

---

## 16. 文件上传 File

`/api/file`

### POST /upload — 上传文件 (multipart)

**参数:** `file` (File)

**Response (FileUploadResponse):**
```json
{
  "url": "/api/file/download/xxx_photo.jpg",
  "originalName": "photo.jpg",
  "size": 102400,
  "contentType": "image/jpeg"
}
```

---

## 17. 健康检查

### GET /api/ping

**Response:**
```json
{ "code": 200, "message": "success", "data": { "status": "ok" }, "timestamp": 1744000000 }
```
