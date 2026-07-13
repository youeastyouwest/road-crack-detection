"""
app.py
======
YOLOv8 道路裂缝检测系统 - FastAPI 后端服务

为前端提供 RESTful API，支持:
  - 图片上传检测
  - Base64 图片检测
  - 视频上传检测（隔帧算法）
  - 批量检测
  - 摄像头实时检测 (WebSocket)
  - 获取检测统计信息
  - 查看历史检测记录

启动:
  python app.py                      # 开发模式 (http://localhost:8000)
  uvicorn app:app --host 0.0.0.0 --port 8000 --reload   # 热重载
  python app.py --host 0.0.0.0 --port 8000 --weights best.pt

API 文档 (启动后):
  Swagger UI: http://localhost:8000/docs
  ReDoc:      http://localhost:8000/redoc
"""

import os
import sys
import json
import time
import uuid
import base64
import argparse
from io import BytesIO
from pathlib import Path
from datetime import datetime
from typing import List, Optional

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

try:
    import uvicorn
    from fastapi import FastAPI, UploadFile, File, Form, WebSocket, WebSocketDisconnect
    from fastapi.responses import JSONResponse, HTMLResponse, FileResponse
    from fastapi.staticfiles import StaticFiles
    from fastapi.middleware.cors import CORSMiddleware
    from pydantic import BaseModel, Field
except ImportError:
    raise ImportError("请先安装依赖: pip install -r requirements.txt")

# 添加项目根目录到 sys.path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from model import CrackDetector, DetectionResult, CLASS_NAMES_CN


# ============ 配置 ============

class Settings:
    """应用配置"""
    # 服务配置
    HOST: str = os.getenv("CRACK_HOST", "0.0.0.0")
    PORT: int = int(os.getenv("CRACK_PORT", "8000"))

    # 模型配置
    WEIGHTS_PATH: str = os.getenv("CRACK_WEIGHTS_PATH", "best.pt")
    CONF_THRESHOLD: float = float(os.getenv("CRACK_CONF_THRESHOLD", "0.05"))
    IOU_THRESHOLD: float = 0.45
    DEVICE: str = os.getenv("CRACK_DEVICE", "")  # 空=自动选择

    # 文件存储
    UPLOAD_DIR: Path = Path("runs/app/uploads")
    RESULT_DIR: Path = Path("runs/app/results")
    HISTORY_FILE: Path = Path("runs/app/history.json")

    # 最大上传大小 (50MB)
    MAX_FILE_SIZE: int = 500 * 1024 * 1024

    # 允许的文件类型
    ALLOWED_EXTENSIONS: set = {".jpg", ".jpeg", ".png", ".bmp", ".tif", ".tiff", ".mp4", ".webm", ".avi", ".mov"}


settings = Settings()


# ============ FastAPI 应用 ============

app = FastAPI(
    title="道路裂缝检测系统 API",
    description="基于 YOLOv8 的道路裂缝检测后端服务\n\n"
                "支持对道路图片中的纵向裂缝、横向裂缝、龟裂、坑洞进行检测。",
    version="1.0.0",
    contact={
        "name": "道路裂缝检测系统",
    },
)

# CORS 配置 - 允许前端跨域访问
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============ 全局变量 ============

# 检测器 (延迟初始化)
detector: Optional[CrackDetector] = None

# 检测历史
detection_history: list = []


# ============ Pydantic 模型 ============

class DetectRequest(BaseModel):
    """检测请求"""
    image: str = Field(..., description="Base64 编码的图片数据")
    conf_threshold: Optional[float] = Field(None, description="置信度阈值")
    iou_threshold: Optional[float] = Field(None, description="IOU 阈值")

    class Config:
        json_schema_extra = {
            "example": {
                "image": "/9j/4AAQ... (base64 encoded image)",
                "conf_threshold": 0.25,
            }
        }


class BatchDetectRequest(BaseModel):
    """批量检测请求"""
    images: List[str] = Field(..., description="Base64 编码的图片列表")
    conf_threshold: Optional[float] = Field(None, description="置信度阈值")

    class Config:
        json_schema_extra = {
            "example": {
                "images": ["/9j/4AAQ...", "/9j/4AAQ..."],
                "conf_threshold": 0.25,
            }
        }


class DetectResponse(BaseModel):
    """检测响应"""
    success: bool
    message: str
    data: Optional[dict] = None
    error: Optional[str] = None


class HealthResponse(BaseModel):
    """健康检查响应"""
    status: str
    model_loaded: bool
    device: str
    num_classes: int
    class_names: list
    uptime: str
    total_detections: int


# ============ 工具函数 ============

def get_detector() -> CrackDetector:
    """获取或初始化检测器"""
    global detector
    if detector is None:
        if not Path(settings.WEIGHTS_PATH).exists():
            raise FileNotFoundError(
                f"模型权重不存在: {settings.WEIGHTS_PATH}\n"
                f"请先运行 python train.py 训练模型, "
                f"或下载预训练模型放到项目根目录。"
            )
        detector = CrackDetector(
            weights_path=settings.WEIGHTS_PATH,
            device=settings.DEVICE,
            conf_threshold=settings.CONF_THRESHOLD,
            iou_threshold=settings.IOU_THRESHOLD,
        )
    return detector


def allowed_file(filename: str) -> bool:
    """检查文件类型是否允许"""
    ext = Path(filename).suffix.lower()
    return ext in settings.ALLOWED_EXTENSIONS


def decode_base64_image(data: str) -> np.ndarray:
    """将 Base64 编码的图片解码为 OpenCV 图像"""
    # 移除 data:image/...;base64, 前缀
    if "," in data:
        data = data.split(",", 1)[1]

    img_data = base64.b64decode(data)
    np_arr = np.frombuffer(img_data, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("Base64 图片解码失败")
    return img


def encode_image_base64(img: np.ndarray, format: str = ".jpg") -> str:
    """将 OpenCV 图像编码为 Base64"""
    _, buffer = cv2.imencode(format, img)
    return base64.b64encode(buffer).decode("utf-8")


def save_history(record: dict):
    """保存检测历史"""
    global detection_history
    detection_history.append(record)

    # 保持最近 1000 条
    if len(detection_history) > 1000:
        detection_history = detection_history[-1000:]

    # 持久化到文件
    try:
        settings.HISTORY_FILE.parent.mkdir(parents=True, exist_ok=True)
        with open(settings.HISTORY_FILE, "w", encoding="utf-8") as f:
            json.dump(detection_history[-500:], f, indent=2, ensure_ascii=False)
    except Exception:
        pass


def load_history():
    """加载检测历史"""
    global detection_history
    try:
        if settings.HISTORY_FILE.exists():
            with open(settings.HISTORY_FILE, "r", encoding="utf-8") as f:
                detection_history = json.load(f)
    except Exception:
        detection_history = []


def result_to_response(
    result: DetectionResult,
    include_image: bool = False,
    image_format: str = ".jpg",
) -> dict:
    """将 DetectionResult 转为 API 响应格式"""
    resp = result.to_dict()
    if include_image and Path(result.image_path).exists():
        img = cv2.imread(result.image_path)
        drawn = get_detector().draw_detections(img, result)
        resp["image_base64"] = encode_image_base64(drawn, image_format)
    return resp


# ============ 生命周期事件 ============

@app.on_event("startup")
async def startup():
    """应用启动时初始化"""
    global start_time
    start_time = time.time()

    # 创建目录
    settings.UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    settings.RESULT_DIR.mkdir(parents=True, exist_ok=True)

    # 加载历史记录
    load_history()

    # 尝试初始化模型
    try:
        get_detector()
        print(f"✅ 模型加载成功")
    except Exception as e:
        print(f"⚠️  模型加载失败: {e}")
        print(f"   请在训练完成后: python train.py")


@app.on_event("shutdown")
async def shutdown():
    """应用关闭时清理"""
    print("🛑 服务关闭")


# ============ API 端点 ============

@app.get("/", response_class=HTMLResponse)
async def root():
    """根路径 - 返回 API 信息"""
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <title>道路裂缝检测系统 API</title>
        <style>
            body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }
            h1 { color: #333; }
            .endpoint { background: #f5f5f5; padding: 10px; margin: 5px 0; border-radius: 5px; }
            code { background: #eee; padding: 2px 5px; border-radius: 3px; }
        </style>
    </head>
    <body>
        <h1>🚧 道路裂缝检测系统</h1>
        <p>基于 YOLOv8 的道路裂缝检测后端服务</p>
        <hr>
        <h2>API 端点</h2>
        <div class="endpoint">📋 <code>GET /health</code> - 健康检查</div>
        <div class="endpoint">📷 <code>POST /detect/file</code> - 上传图片检测</div>
        <div class="endpoint">📷 <code>POST /detect/base64</code> - Base64 图片检测</div>
        <div class="endpoint">🎬 <code>POST /detect/video</code> - 上传视频检测（隔帧算法）</div>
        <div class="endpoint">📷 <code>POST /detect/batch</code> - 批量检测</div>
        <div class="endpoint">📊 <code>GET /stats</code> - 统计信息</div>
        <div class="endpoint">📜 <code>GET /history</code> - 检测历史</div>
        <div class="endpoint">🔌 <code>WS /ws/detect</code> - WebSocket 实时检测</div>
        <hr>
        <p>📚 <a href="/docs">Swagger UI 文档</a> | <a href="/redoc">ReDoc 文档</a></p>
    </body>
    </html>
    """


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """健康检查接口"""
    try:
        det = get_detector()
        model_loaded = True
        device = det.device
        num_classes = det.num_classes
        class_names = det.class_names_cn
    except Exception as e:
        model_loaded = False
        device = "N/A"
        num_classes = 0
        class_names = []

    uptime_seconds = int(time.time() - start_time)
    uptime_str = f"{uptime_seconds // 3600}h {(uptime_seconds % 3600) // 60}m {uptime_seconds % 60}s"

    return HealthResponse(
        status="ok" if model_loaded else "degraded",
        model_loaded=model_loaded,
        device=device,
        num_classes=num_classes,
        class_names=class_names,
        uptime=uptime_str,
        total_detections=sum(r.get("num_detections", 0) for r in detection_history),
    )


@app.post("/detect/file", response_model=DetectResponse)
async def detect_file(
    file: UploadFile = File(..., description="上传图片文件"),
    conf_threshold: Optional[float] = Form(None, description="置信度阈值"),
    iou_threshold: Optional[float] = Form(None, description="IOU 阈值"),
    draw_result: bool = Form(True, description="是否返回标注后的图片"),
):
    """
    上传图片进行裂缝检测

    支持格式: jpg, jpeg, png, bmp, tif, tiff, mp4, webm, avi, mov
    最大文件大小: 500MB
    """
    # 验证文件
    if not file.filename:
        return DetectResponse(success=False, message="未选择文件", error="No file provided")

    if not allowed_file(file.filename):
        return DetectResponse(
            success=False,
            message="不支持的文件格式",
            error=f"支持的格式: {', '.join(settings.ALLOWED_EXTENSIONS)}",
        )

    # 读取文件内容
    contents = await file.read()
    if len(contents) > settings.MAX_FILE_SIZE:
        return DetectResponse(
            success=False,
            message="文件过大",
            error=f"最大支持 {settings.MAX_FILE_SIZE // 1024 // 1024}MB",
        )

    try:
        # 转换为 OpenCV 图像
        np_arr = np.frombuffer(contents, np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
        if img is None:
            return DetectResponse(success=False, message="图片解码失败",
                                  error="无法解码图片文件")

        # 保存上传文件
        file_id = str(uuid.uuid4())[:8]
        file_ext = Path(file.filename).suffix
        upload_path = settings.UPLOAD_DIR / f"{file_id}_{file.filename}"
        with open(upload_path, "wb") as f:
            f.write(contents)

        # 执行检测
        det = get_detector()
        result = det.detect(
            source=img,
            conf_threshold=conf_threshold or settings.CONF_THRESHOLD,
            iou_threshold=iou_threshold or det.iou_threshold,
        )
        result.image_path = str(upload_path)

        # 保存结果图
        result_path = settings.RESULT_DIR / f"{file_id}_{Path(file.filename).stem}_result.jpg"
        drawn = det.draw_detections(img, result)
        cv2.imwrite(str(result_path), drawn)

        # 构建响应
        resp_data = result.to_dict()
        if draw_result:
            resp_data["image_base64"] = encode_image_base64(drawn)

        resp_data["result_image_path"] = str(result_path)

        # 保存历史
        save_history({
            "id": file_id,
            "timestamp": datetime.now().isoformat(),
            "filename": file.filename,
            "num_detections": result.num_detections,
            "has_crack": result.has_crack,
            "crack_types": result.get_crack_types(),
            "detections": result.to_dict()["detections"],
        })

        return DetectResponse(
            success=True,
            message=f"检测完成，发现 {result.num_detections} 处{'裂缝' if result.has_crack else '异常'}",
            data=resp_data,
        )

    except Exception as e:
        return DetectResponse(
            success=False,
            message="检测失败",
            error=str(e),
        )


@app.post("/detect/base64", response_model=DetectResponse)
async def detect_base64(request: DetectRequest):
    """
    通过 Base64 编码的图片进行裂缝检测

    适用于前端直接上传图片的场景。
    支持带 data:image/...;base64, 前缀的字符串。
    """
    try:
        # 解码 Base64 图片
        img = decode_base64_image(request.image)
        if img is None:
            return DetectResponse(success=False, message="Base64 图片解码失败",
                                  error="请检查图片数据是否完整")

        # 执行检测
        det = get_detector()
        result = det.detect(
            source=img,
            conf_threshold=request.conf_threshold or settings.CONF_THRESHOLD,
            iou_threshold=request.iou_threshold or det.iou_threshold,
        )

        # 绘制结果
        drawn = det.draw_detections(img, result)

        # 构建响应
        resp_data = result.to_dict()
        resp_data["image_base64"] = encode_image_base64(drawn)

        # 保存历史
        save_history({
            "id": str(uuid.uuid4())[:8],
            "timestamp": datetime.now().isoformat(),
            "filename": "base64_input",
            "num_detections": result.num_detections,
            "has_crack": result.has_crack,
            "crack_types": result.get_crack_types(),
            "detections": result.to_dict()["detections"],
        })

        return DetectResponse(
            success=True,
            message=f"检测完成，发现 {result.num_detections} 处目标",
            data=resp_data,
        )

    except ValueError as e:
        return DetectResponse(success=False, message="图片解码失败", error=str(e))
    except Exception as e:
        return DetectResponse(success=False, message="检测失败", error=str(e))


@app.post("/detect/batch", response_model=DetectResponse)
async def detect_batch(request: BatchDetectRequest):
    """
    批量检测 Base64 图片

    同时上传多张图片进行检测，提高处理效率。
    """
    if len(request.images) > 50:
        return DetectResponse(
            success=False,
            message="批量检测数量超限",
            error="单次最多支持 50 张图片",
        )

    try:
        det = get_detector()
        results = []

        for i, img_b64 in enumerate(request.images):
            img = decode_base64_image(img_b64)
            result = det.detect(
                source=img,
                conf_threshold=request.conf_threshold or settings.CONF_THRESHOLD,
            )
            drawn = det.draw_detections(img, result)

            result_dict = result.to_dict()
            result_dict["image_base64"] = encode_image_base64(drawn)
            result_dict["index"] = i
            results.append(result_dict)

        total_detections = sum(r["num_detections"] for r in results)
        crack_count = sum(1 for r in results if r["has_crack"])

        return DetectResponse(
            success=True,
            message=f"批量检测完成: {len(results)} 张, {crack_count} 张含裂缝, 共 {total_detections} 处目标",
            data={
                "total": len(results),
                "crack_count": crack_count,
                "total_detections": total_detections,
                "results": results,
            },
        )

    except Exception as e:
        return DetectResponse(success=False, message="批量检测失败", error=str(e))


@app.get("/stats")
async def get_statistics():
    """获取检测统计信息"""
    try:
        det = get_detector()
        class_info = det.get_class_info()
    except Exception:
        class_info = []

    total = len(detection_history)
    crack_count = sum(1 for r in detection_history if r.get("has_crack"))
    total_detections = sum(r.get("num_detections", 0) for r in detection_history)

    # 类别统计
    class_stats = {}
    for record in detection_history:
        for det in record.get("detections", []):
            cls_id = det.get("class_id", -1)
            cls_name = det.get("class_name_cn", "unknown")
            if cls_name not in class_stats:
                class_stats[cls_name] = 0
            class_stats[cls_name] += 1

    return {
        "success": True,
        "data": {
            "total_requests": total,
            "crack_detected_count": crack_count,
            "total_detections": total_detections,
            "class_statistics": class_stats,
            "class_info": class_info,
            "model_loaded": detector is not None,
        },
    }


@app.get("/history")
async def get_history(
    limit: int = 50,
    offset: int = 0,
    has_crack: Optional[bool] = None,
):
    """获取检测历史记录"""
    records = detection_history

    # 过滤
    if has_crack is not None:
        records = [r for r in records if r.get("has_crack") == has_crack]

    total = len(records)
    records = records[offset:offset + limit]

    return {
        "success": True,
        "data": {
            "total": total,
            "offset": offset,
            "limit": limit,
            "records": records,
        },
    }


@app.delete("/history")
async def clear_history():
    """清空检测历史"""
    global detection_history
    detection_history = []
    try:
        if settings.HISTORY_FILE.exists():
            settings.HISTORY_FILE.unlink()
    except Exception:
        pass
    return {"success": True, "message": "检测历史已清空"}


@app.post("/model/reload")
async def reload_model(weights_path: Optional[str] = None):
    """重新加载模型 (更换权重文件)"""
    global detector
    try:
        if weights_path:
            settings.WEIGHTS_PATH = weights_path
        if detector is not None:
            detector.reload(settings.WEIGHTS_PATH)
        else:
            get_detector()
        return {"success": True, "message": f"模型已加载: {settings.WEIGHTS_PATH}"}
    except Exception as e:
        return {"success": False, "message": f"模型加载失败: {e}", "error": str(e)}


@app.post("/detect/video", response_model=DetectResponse)
async def detect_video(
    file: UploadFile = File(..., description="上传视频文件"),
    conf_threshold: Optional[float] = Form(None, description="置信度阈值"),
    vid_stride: int = Form(2, description="隔帧检测步长"),
):
    """
    上传视频进行裂缝检测

    使用隔帧检测算法，对视频每隔 vid_stride 帧进行 YOLOv8 推理。
    返回每帧检测结果汇总及标注后的视频路径。

    支持格式: mp4, avi, mov, webm
    最大文件大小: 500MB
    """
    if not file.filename:
        return DetectResponse(success=False, message="未选择文件", error="No file provided")

    # 检查文件类型
    video_exts = {".mp4", ".avi", ".mov", ".webm", ".mkv", ".flv", ".wmv"}
    ext = Path(file.filename).suffix.lower()
    if ext not in video_exts:
        return DetectResponse(
            success=False,
            message="不支持的视频格式",
            error=f"支持的格式: {', '.join(video_exts)}",
        )

    # 读取文件
    contents = await file.read()
    if len(contents) > settings.MAX_FILE_SIZE:
        return DetectResponse(
            success=False,
            message="文件过大",
            error=f"最大支持 {settings.MAX_FILE_SIZE // 1024 // 1024}MB",
        )

    try:
        # 保存上传视频
        file_id = str(uuid.uuid4())[:8]
        video_filename = f"{file_id}_{file.filename}"
        upload_path = settings.UPLOAD_DIR / video_filename
        with open(upload_path, "wb") as f:
            f.write(contents)

        # 执行视频检测
        det = get_detector()
        video_result = det.detect_video(
            video_path=str(upload_path),
            output_dir=str(settings.RESULT_DIR),
            output_name=f"video_{file_id}",
            conf_threshold=conf_threshold or settings.CONF_THRESHOLD,
            vid_stride=vid_stride,
        )

        # 转换 output_video_path 为 URL 可访问路径
        video_url = None
        if video_result["output_video_path"]:
            try:
                output_path = Path(video_result["output_video_path"]).resolve()
                rel_path = output_path.relative_to(settings.RESULT_DIR.resolve())
                video_url = f"http://localhost:8000/results/{rel_path.as_posix()}"
            except ValueError:
                video_url = None

        # 提取关键帧标注图片（从检测到裂缝的帧中选取）
        keyframe_urls = []
        detected_frames = [fr for fr in video_result["frame_results"] if fr.get("num_detections", 0) > 0]
        if detected_frames:
            # 按检测数量降序，最多取 5 帧
            detected_frames.sort(key=lambda x: x.get("num_detections", 0), reverse=True)
            selected_frames = detected_frames[:5]

            cap = cv2.VideoCapture(str(upload_path))
            if cap.isOpened():
                for fr in selected_frames:
                    frame_idx = fr.get("frame_idx", 0)
                    cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
                    ret, frame = cap.read()
                    if not ret:
                        continue

                    # 构建 DetectionResult 用于绘制
                    from model import DetectionResult
                    result = DetectionResult(str(upload_path), frame.shape[:2])
                    for d in fr.get("detections", []):
                        result.add_detection(
                            cls_id=d.get("class_id", 0),
                            class_name=d.get("class_name", ""),
                            class_name_cn=d.get("class_name_cn", ""),
                            confidence=d.get("confidence", 0),
                            bbox=d.get("bbox", [0, 0, 0, 0]),
                        )

                    drawn = det.draw_detections(frame, result)
                    kf_name = f"keyframe_{file_id}_f{frame_idx}.jpg"
                    kf_path = (settings.RESULT_DIR / kf_name).resolve()
                    cv2.imwrite(str(kf_path), drawn)

                    try:
                        rel_path = kf_path.relative_to(settings.RESULT_DIR.resolve())
                        kf_url = f"http://localhost:8000/results/{rel_path.as_posix()}"
                        keyframe_urls.append(kf_url)
                    except ValueError:
                        pass
                cap.release()

        # 构建响应数据
        resp_data = {
            "file_id": file_id,
            "filename": file.filename,
            "total_frames_processed": video_result["total_frames_processed"],
            "frames_with_detections": video_result["frames_with_detections"],
            "total_detections": video_result["total_detections"],
            "crack_types": video_result["crack_types"],
            "output_video_path": video_url,
            "keyframe_urls": keyframe_urls,
            "summary": video_result["summary"],
            "frame_results": video_result["frame_results"],
        }

        # 保存历史
        save_history({
            "id": file_id,
            "timestamp": datetime.now().isoformat(),
            "filename": file.filename,
            "media_type": "video",
            "num_detections": video_result["total_detections"],
            "frames_with_detections": video_result["frames_with_detections"],
            "total_frames": video_result["total_frames_processed"],
            "crack_types": video_result["crack_types"],
        })

        return DetectResponse(
            success=True,
            message=video_result["summary"],
            data=resp_data,
        )

    except Exception as e:
        return DetectResponse(
            success=False,
            message="视频检测失败",
            error=str(e),
        )


# ============ WebSocket 实时检测 ============

@app.websocket("/ws/detect")
async def websocket_detect(websocket: WebSocket):
    """
    WebSocket 实时检测端点

    用于摄像头实时上传帧进行检测。
    发送: Base64 编码的 JPEG 图片帧
    接收: JSON 格式的检测结果
    """
    await websocket.accept()
    print(f"🔌 WebSocket 客户端已连接")

    frame_count = 0
    start_time_ws = time.time()

    try:
        while True:
            # 接收消息
            data = await websocket.receive_text()

            # 处理 ping 心跳
            if data == "ping":
                await websocket.send_json({"type": "pong"})
                continue

            try:
                # 解码图片
                img = decode_base64_image(data)
                frame_count += 1

                # 执行检测
                det = get_detector()
                result = det.detect_video_frame(img)

                # 绘制结果
                drawn = det.draw_detections(img, result)
                result_b64 = encode_image_base64(drawn)

                # 计算 FPS
                elapsed = time.time() - start_time_ws
                fps = frame_count / elapsed if elapsed > 0 else 0

                # 发送结果
                await websocket.send_json({
                    "type": "detection_result",
                    "frame_id": frame_count,
                    "fps": round(fps, 1),
                    "num_detections": result.num_detections,
                    "has_crack": result.has_crack,
                    "crack_types": result.get_crack_types(),
                    "detections": result.to_dict()["detections"],
                    "image_base64": f"data:image/jpeg;base64,{result_b64}",
                })

            except ValueError as e:
                await websocket.send_json({
                    "type": "error",
                    "message": f"图片解码失败: {str(e)}",
                })
            except Exception as e:
                await websocket.send_json({
                    "type": "error",
                    "message": f"检测失败: {str(e)}",
                })

    except WebSocketDisconnect:
        print(f"🔌 WebSocket 客户端已断开 (共处理 {frame_count} 帧)")
    except Exception as e:
        print(f"🔌 WebSocket 错误: {e}")
    finally:
        try:
            await websocket.close()
        except Exception:
            pass


# ============ 静态文件服务 ============

# 提供结果图片访问
try:
    app.mount("/results", StaticFiles(directory="runs/app/results"), name="results")
except Exception:
    os.makedirs("runs/app/results", exist_ok=True)
    app.mount("/results", StaticFiles(directory="runs/app/results"), name="results")


# ============ Web 管理界面 (简易) ============

@app.get("/webui", response_class=HTMLResponse)
async def webui():
    """简易 Web 管理界面"""
    html_path = Path(__file__).parent / "webui.html"
    if html_path.exists():
        return html_path.read_text(encoding="utf-8")
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <title>道路裂缝检测系统</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                   background: #f0f2f5; color: #333; }
            .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                      color: white; padding: 20px 40px; }
            .container { max-width: 1200px; margin: 20px auto; padding: 0 20px; }
            .card { background: white; border-radius: 10px; padding: 20px;
                    margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
            .upload-zone { border: 2px dashed #ccc; border-radius: 10px;
                          padding: 40px; text-align: center; cursor: pointer;
                          transition: all 0.3s; }
            .upload-zone:hover { border-color: #667eea; background: #f8f9ff; }
            .upload-zone.dragover { border-color: #667eea; background: #eef0ff; }
            .result-img { max-width: 100%; border-radius: 8px; margin-top: 20px; }
            .badge { display: inline-block; padding: 4px 12px; border-radius: 20px;
                    font-size: 12px; margin: 2px; }
            .badge-red { background: #fee; color: #c00; }
            .badge-yellow { background: #fff3cd; color: #856404; }
            .badge-purple { background: #f3e8ff; color: #6b21a8; }
            .badge-orange { background: #fff3e0; color: #e65100; }
            .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                         gap: 15px; margin: 20px 0; }
            .stat-card { text-align: center; padding: 20px; background: #f8f9fa;
                        border-radius: 8px; }
            .stat-number { font-size: 28px; font-weight: bold; color: #667eea; }
            .stat-label { font-size: 14px; color: #666; margin-top: 5px; }
            .btn { background: #667eea; color: white; border: none; padding: 10px 24px;
                  border-radius: 6px; cursor: pointer; font-size: 14px; }
            .btn:hover { background: #5a67d8; }
            .btn:disabled { background: #ccc; cursor: not-allowed; }
            .loading { display: none; text-align: center; padding: 20px; }
            .loading.show { display: block; }
            .spinner { border: 3px solid #f3f3f3; border-top: 3px solid #667eea;
                      border-radius: 50%; width: 40px; height: 40px;
                      animation: spin 1s linear infinite; margin: 0 auto; }
            @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
            .info-table { width: 100%%; border-collapse: collapse; }
            .info-table th, .info-table td { padding: 8px 12px; text-align: left;
                                            border-bottom: 1px solid #eee; }
        </style>
    </head>
    <body>
        <div class="header">
            <h1>🚧 道路裂缝检测系统</h1>
            <p>基于 YOLOv8 的智能道路裂缝检测</p>
        </div>
        <div class="container">
            <div class="card">
                <h2>📤 上传检测</h2>
                <div class="upload-zone" id="uploadZone"
                     onclick="document.getElementById('fileInput').click()">
                    <p style="font-size: 48px;">📷</p>
                    <p style="font-size: 16px; color: #666;">点击或拖拽图片到此处</p>
                    <p style="font-size: 12px; color: #999;">支持 JPG、PNG、BMP 格式</p>
                    <input type="file" id="fileInput" accept="image/*" hidden
                           onchange="handleFile(this.files[0])">
                </div>
                <div class="loading" id="loading">
                    <div class="spinner"></div>
                    <p style="margin-top: 10px;">正在检测中...</p>
                </div>
                <div id="resultArea"></div>
            </div>

            <div class="card">
                <h2>📊 统计概览</h2>
                <div class="stats-grid" id="statsGrid">
                    <div class="stat-card">
                        <div class="stat-number" id="statTotal">-</div>
                        <div class="stat-label">总检测次数</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-number" id="statCrack">-</div>
                        <div class="stat-label">发现裂缝</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-number" id="statTargets">-</div>
                        <div class="stat-label">总目标数</div>
                    </div>
                </div>
            </div>
        </div>

        <script>
            // 拖拽上传
            const zone = document.getElementById('uploadZone');
            zone.addEventListener('dragover', e => {
                e.preventDefault();
                zone.classList.add('dragover');
            });
            zone.addEventListener('dragleave', () => {
                zone.classList.remove('dragover');
            });
            zone.addEventListener('drop', e => {
                e.preventDefault();
                zone.classList.remove('dragover');
                if (e.dataTransfer.files.length > 0) {
                    handleFile(e.dataTransfer.files[0]);
                }
            });

            async function handleFile(file) {
                const loading = document.getElementById('loading');
                const resultArea = document.getElementById('resultArea');
                loading.classList.add('show');
                resultArea.innerHTML = '';

                try {
                    const formData = new FormData();
                    formData.append('file', file);

                    const res = await fetch('/detect/file', {
                        method: 'POST',
                        body: formData,
                    });
                    const data = await res.json();

                    if (data.success) {
                        let html = '<h3>检测结果</h3>';
                        html += '<table class="info-table">';
                        html += `<tr><th>裂缝数量</th><td>${data.data.num_detections}</td></tr>`;
                        html += `<tr><th>裂缝类型</th><td>${data.data.crack_types.join(', ') || '无'}</td></tr>`;

                        if (data.data.detections.length > 0) {
                            html += '<tr><th>详情</th><td>';
                            data.data.detections.forEach(d => {
                                const badgeClass = ['badge-red', 'badge-yellow', 'badge-purple', 'badge-orange'][d.class_id] || 'badge-red';
                                html += `<span class="badge ${badgeClass}">${d.class_name_cn} (${(d.confidence*100).toFixed(0)}%)</span> `;
                            });
                            html += '</td></tr>';
                        }
                        html += '</table>';

                        if (data.data.image_base64) {
                            html += `<img class="result-img" src="data:image/jpeg;base64,${data.data.image_base64}" alt="检测结果">`;
                        }
                        resultArea.innerHTML = html;
                        loadStats();
                    } else {
                        resultArea.innerHTML = `<p style="color:red">❌ ${data.message}</p>`;
                    }
                } catch (err) {
                    resultArea.innerHTML = `<p style="color:red">❌ 请求失败: ${err.message}</p>`;
                }
                loading.classList.remove('show');
            }

            async function loadStats() {
                try {
                    const res = await fetch('/stats');
                    const data = await res.json();
                    if (data.success) {
                        document.getElementById('statTotal').textContent = data.data.total_requests;
                        document.getElementById('statCrack').textContent = data.data.crack_detected_count;
                        document.getElementById('statTargets').textContent = data.data.total_detections;
                    }
                } catch (e) {
                    console.error('Failed to load stats:', e);
                }
            }
            loadStats();
        </script>
    </body>
    </html>
    """


# ============ 主函数 ============

def main():
    parser = argparse.ArgumentParser(description="YOLOv8 道路裂缝检测 API 服务")
    parser.add_argument("--host", type=str, default=settings.HOST,
                        help=f"监听地址 (默认: {settings.HOST})")
    parser.add_argument("--port", type=int, default=settings.PORT,
                        help=f"监听端口 (默认: {settings.PORT})")
    parser.add_argument("--weights", type=str, default=settings.WEIGHTS_PATH,
                        help=f"模型权重路径 (默认: {settings.WEIGHTS_PATH})")
    parser.add_argument("--conf", type=float, default=settings.CONF_THRESHOLD,
                        help=f"置信度阈值 (默认: {settings.CONF_THRESHOLD})")
    parser.add_argument("--device", type=str, default=settings.DEVICE,
                        help="推理设备: cpu / 0 / 1 (默认: 自动)")
    parser.add_argument("--reload", action="store_true",
                        help="热重载模式 (开发用)")
    args = parser.parse_args()
    weights_path = str(Path(args.weights).expanduser().resolve())

    # 更新配置
    settings.HOST = args.host
    settings.PORT = args.port
    settings.WEIGHTS_PATH = weights_path
    settings.CONF_THRESHOLD = args.conf
    settings.DEVICE = args.device

    # 让 uvicorn 重新导入 app 模块时也能读到启动参数
    os.environ["CRACK_HOST"] = args.host
    os.environ["CRACK_PORT"] = str(args.port)
    os.environ["CRACK_WEIGHTS_PATH"] = weights_path
    os.environ["CRACK_CONF_THRESHOLD"] = str(args.conf)
    os.environ["CRACK_DEVICE"] = args.device

    print(f"""
╔════════════════════════════════════════╗
║     🚧 道路裂缝检测系统 API             ║
╠════════════════════════════════════════╣
║  地址: http://{args.host}:{args.port}      ║
║  文档: http://{args.host}:{args.port}/docs ║
║  WebUI: http://{args.host}:{args.port}/webui ║
║  模型: {weights_path}                     ║
║  设备: {args.device or '自动'}                   ║
╚════════════════════════════════════════╝
    """)

    uvicorn.run(
        "app:app",
        host=args.host,
        port=args.port,
        reload=args.reload,
        log_level="info",
    )


start_time = time.time()


if __name__ == "__main__":
    main()
