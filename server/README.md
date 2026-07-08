# Server

这是一版按你们文档拆好的 Spring Boot 多模块后端骨架，当前优先实现了“后端 2”可独立推进的部分：

- 检测任务接口
- 工单接口
- 维修报告接口
- WebSocket 推送
- 算法服务调用占位

## 模块说明

- `crack-common`: 通用返回体、错误码、业务异常
- `crack-api`: DTO、VO、枚举
- `crack-dao`: 预留给数据库层
- `crack-service`: 业务服务、内存实现、算法占位
- `crack-websocket`: WebSocket/STOMP 配置与推送实现
- `crack-report`: 预留给报告生成模块
- `crack-bootstrap`: 启动入口、控制器、全局异常处理、配置

## 当前接口

- `POST /api/detection-tasks`: 创建检测任务
- `POST /api/detection-tasks/{taskId}/execute`: 异步执行检测任务
- `GET /api/detection-tasks`: 分页查询检测任务
- `GET /api/detection-tasks/{taskId}`: 查询检测任务详情
- `GET /api/detection-tasks/{taskId}/result`: 查询检测结果
- `DELETE /api/detection-tasks/{taskId}`: 删除检测任务
- `POST /api/work-orders`: 手动创建工单
- `PUT /api/work-orders/{workOrderId}/assign`: 分配工单
- `PUT /api/work-orders/{workOrderId}/status`: 更新工单状态
- `PUT /api/work-orders/{workOrderId}/cancel`: 取消工单
- 
- `GET /api/work-orders`: 分页查询工单列表
- `GET /api/work-orders/{workOrderId}`: 查询工单详情
- `POST /api/maintenance-reports`: 创建维修报告
- `GET /api/maintenance-reports`: 分页查询维修报告列表
- `GET /api/maintenance-reports/{reportId}`: 查询维修报告详情

## 当前 WebSocket 约定

- 握手端点: `/ws`
- 应用前缀: `/app`
- 订阅前缀: `/topic`
- 订阅检测进度: `/topic/detection/{taskId}/progress`
- 订阅检测结果: `/topic/detection/{taskId}/result`
- 订阅工单更新: `/topic/work-orders`
- 订阅告警消息: `/topic/alerts`

## 运行

如果你本机的 Maven 不在 PATH 里，可以直接用绝对路径：

```powershell
& 'D:\DevTools\Maven\apache-maven-3.9.16\bin\mvn.cmd' -f server\pom.xml clean package
& 'D:\DevTools\Maven\apache-maven-3.9.16\bin\mvn.cmd' -f server\crack-bootstrap\pom.xml spring-boot:run
```

## 数据库版检测任务

当前检测任务支持两种模式：

- 默认 `memory`：继续使用内存版，适合快速看接口返回
- 设置 `CRACK_PERSISTENCE_MODE=db`：启用数据库版检测任务落库

数据库模式建议额外设置：

```powershell
$env:CRACK_PERSISTENCE_MODE='db'
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/road_crack_detection?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME='你的用户名'
$env:DB_PASSWORD='你的密码'
$env:DB_INIT_MODE='always'
```

临时测试脚本放在忽略目录 `temp/` 下：

- `temp/step1_mysql_setup.sql`
- `temp/step1_detection_smoke.ps1`
