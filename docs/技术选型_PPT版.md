# 技术选型（PPT展示用）

---

## 整体技术架构

```
┌─────────────────────────────────────────────────────┐
│                   前端层 (Vue 3)                      │
│  Vue 3 + TypeScript  │  Element Plus  │  ECharts     │
│  Axios + WebSocket   │  Vite          │  Pinia       │
└──────────────────────────┬──────────────────────────┘
                           │ HTTP (RESTful API) + WebSocket
┌──────────────────────────▼──────────────────────────┐
│                  后端层 (Spring Boot)                  │
│  Spring Boot 3.x  │  Spring Security + JWT           │
│  MyBatis-Plus     │  Spring WebSocket                │
└──────────────────────────┬──────────────────────────┘
            ┌──────────────┼──────────────┐
            ▼              ▼              ▼
       ┌────────┐    ┌────────┐    ┌──────────┐
       │ MySQL  │    │ Redis  │    │ Python   │
       │ 数据库  │    │ 缓存   │    │ 推理服务  │
       └────────┘    └────────┘    └────┬─────┘
                                        │
                                  ┌─────▼─────┐
                                  │ YOLOv8 +  │
                                  │   U-Net   │
                                  └───────────┘
```

---

## 技术选型总表

| 层次 | 技术 | 版本 |
|:----:|------|:----:|
| 🖥️ **前端框架** | Vue 3 + TypeScript | 3.4+ / 5+ |
| 🎨 **UI 组件库** | Element Plus | 2.x |
| 📊 **数据可视化** | ECharts | 5.x |
| ⚡ **构建工具** | Vite | 5.x |
| 📡 **HTTP 通信** | Axios | — |
| 🔄 **实时通信** | WebSocket | — |
| 🗃️ **状态管理** | Pinia | — |
| 🌐 **后端框架** | Spring Boot 3.x | 3.x |
| ☕ **JDK** | JDK 17 (LTS) | 17 |
| 🛠️ **构建工具** | Maven | 3.9+ |
| 🗄️ **数据库** | MySQL | 8.0 |
| ⚡ **缓存** | Redis | 7.x |
| 🔐 **权限安全** | Spring Security + JWT | — |
| 📝 **ORM** | MyBatis-Plus | 3.5+ |
| 🔌 **API 文档** | Knife4j | — |
| 🧠 **深度学习框架** | PyTorch | 2.x |
| 🎯 **目标检测** | YOLOv8 | — |
| 🖼️ **语义分割** | U-Net（改进版） | — |
| ⚙️ **模型推理** | ONNX Runtime | 1.15+ |
| 🖼️ **图像处理** | OpenCV | — |
| 🐳 **容器化部署** | Docker + Docker Compose | — |
| 🌐 **反向代理** | Nginx | 1.24+ |

---

## 各角色技术栈

### 客户端工程师（前端）

| 技术 | 用途 |
|------|------|
| Vue 3 + TypeScript | 前端框架 |
| Element Plus | 界面组件库 |
| ECharts | 裂缝数据可视化（仪表盘/趋势图） |
| Axios | 后端 API 调用 |
| WebSocket | 实时检测结果推送 |
| Pinia | 前端状态管理 |
| Vite | 项目构建与热更新 |

### 服务端工程师（后端）

| 技术 | 用途 |
|------|------|
| Spring Boot 3.x | 后端服务框架 |
| MyBatis-Plus | 数据库操作（ORM） |
| MySQL 8.0 | 业务数据存储 |
| Redis | 缓存与消息队列 |
| Spring Security + JWT | 用户认证与权限控制 |
| Spring WebSocket | 实时检测数据推送 |
| Knife4j | 在线 API 文档 |

### 数据处理工程师（算法）

| 技术 | 用途 |
|------|------|
| PyTorch 2.x | 深度学习模型训练 |
| YOLOv8 | 裂缝目标检测（定位裂缝） |
| U-Net（改进版） | 裂缝像素级分割（精确轮廓） |
| ONNX Runtime | 模型部署与推理加速 |
| OpenCV | 图像预处理与后处理 |
| Flask | 轻量推理 API 服务 |

---

## 系统部署架构

| 组件 | 技术 | 部署方式 |
|:----:|------|:--------:|
| 前端 | Nginx 托管静态资源 | Docker |
| 后端 | Spring Boot Jar 包运行 | Docker |
| 数据库 | MySQL 8.0 | Docker |
| 缓存 | Redis 7 | Docker |
| 推理服务 | Flask + ONNX Runtime | Docker |
| 反向代理 | Nginx | Docker |
