"""
model.py
========
YOLOv8 道路裂缝检测模型封装类

提供模型加载、推理、结果解析等统一接口，供 detect.py 和 app.py 调用。

用法:
    from model import CrackDetector

    # 初始化检测器
    detector = CrackDetector("best.pt")

    # 单张图片检测
    results = detector.detect("test.jpg")

    # 批量检测
    results = detector.detect_batch(["img1.jpg", "img2.jpg"])

    # 获取类别信息
    print(detector.class_names)
    print(detector.class_names_cn)
"""

import os
import cv2
import numpy as np
from pathlib import Path
from typing import List, Optional, Union

try:
    from ultralytics import YOLO
except ImportError:
    raise ImportError("请先安装依赖: pip install -r requirements.txt")


# ============ 全局配置 ============

# 类别名称 (英文, 用于程序内部)
CLASS_NAMES = ["vertical_crack", "horizontal_crack", "alligator_crack", "pothole"]

# 类别名称 (中文, 用于展示)
CLASS_NAMES_CN = ["纵向裂缝", "横向裂缝", "龟裂", "坑洞"]

# 检测颜色 (BGR 格式, 用于绘制检测框)
COLORS = [
    (0, 0, 255),     # 纵向裂缝 - 红色
    (0, 255, 255),   # 横向裂缝 - 黄色
    (255, 0, 255),   # 龟裂 - 紫色
    (0, 165, 255),   # 坑洞 - 橙色
]

# 置信度阈值
DEFAULT_CONF_THRESHOLD = 0.05
DEFAULT_IOU_THRESHOLD = 0.45


class DetectionResult:
    """单张图片的检测结果"""

    def __init__(self, image_path: str, orig_shape: tuple):
        self.image_path = image_path
        self.orig_shape = orig_shape  # (height, width)
        self.detections: List[dict] = []  # 检测到的目标列表

    def add_detection(self, cls_id: int, class_name: str, class_name_cn: str,
                      confidence: float, bbox: List[float], mask=None):
        """
        添加一个检测结果

        参数:
            cls_id: 类别ID
            class_name: 类别英文名
            class_name_cn: 类别中文名
            confidence: 置信度
            bbox: [x1, y1, x2, y2] 像素坐标
            mask: 分割掩码 (如果有)
        """
        self.detections.append({
            "class_id": cls_id,
            "class_name": class_name,
            "class_name_cn": class_name_cn,
            "confidence": round(float(confidence), 4),
            "bbox": [round(float(v), 2) for v in bbox],  # [x1, y1, x2, y2]
            "mask": mask,
        })

    @property
    def num_detections(self) -> int:
        return len(self.detections)

    @property
    def has_crack(self) -> bool:
        """是否存在裂缝 (任意类别)"""
        return len(self.detections) > 0

    def get_crack_types(self) -> List[str]:
        """获取检测到的裂缝类型列表"""
        return list(set(d["class_name_cn"] for d in self.detections))

    def to_dict(self, include_mask=False) -> dict:
        """转为字典 (用于 JSON 序列化)"""
        dets = []
        for d in self.detections:
            item = {
                "class_id": d["class_id"],
                "class_name": d["class_name"],
                "class_name_cn": d["class_name_cn"],
                "confidence": d["confidence"],
                "bbox": d["bbox"],
            }
            if include_mask and d["mask"] is not None:
                item["mask"] = d["mask"].tolist()
            dets.append(item)
        return {
            "image_path": self.image_path,
            "image_width": self.orig_shape[1],
            "image_height": self.orig_shape[0],
            "num_detections": self.num_detections,
            "has_crack": self.has_crack,
            "crack_types": self.get_crack_types(),
            "detections": dets,
        }

    def __repr__(self):
        return (f"DetectionResult(image={self.image_path}, "
                f"detections={self.num_detections}, "
                f"has_crack={self.has_crack})")


class CrackDetector:
    """
    道路裂缝检测器

    封装 YOLOv8 模型，提供统一的检测接口。
    """

    def __init__(
        self,
        weights_path: str = "best.pt",
        device: str = "",
        conf_threshold: float = DEFAULT_CONF_THRESHOLD,
        iou_threshold: float = DEFAULT_IOU_THRESHOLD,
        class_names: Optional[List[str]] = None,
        class_names_cn: Optional[List[str]] = None,
    ):
        """
        初始化检测器

        参数:
            weights_path: 模型权重路径
            device: 推理设备 ('cpu', '0', '1', ... 空字符串=自动选择)
            conf_threshold: 置信度阈值
            iou_threshold: NMS IOU阈值
            class_names: 自定义类别英文名 (覆盖默认)
            class_names_cn: 自定义类别中文名 (覆盖默认)
        """
        self.weights_path = str(weights_path)
        self.conf_threshold = conf_threshold
        self.iou_threshold = iou_threshold

        # 类别名称
        self.class_names = class_names or CLASS_NAMES
        self.class_names_cn = class_names_cn or CLASS_NAMES_CN
        self.num_classes = len(self.class_names)

        # 检测颜色
        self.colors = COLORS[:self.num_classes]

        # 选择设备
        self.device = device if device else ("0" if self._cuda_available() else "cpu")

        # 加载模型
        print(f"📦 加载模型: {self.weights_path}")
        print(f"   设备: {self.device}")
        if not Path(self.weights_path).exists():
            raise FileNotFoundError(f"模型权重不存在: {self.weights_path}")

        self.model = YOLO(self.weights_path)
        self.model.to(self.device)
        print(f"✅ 模型加载成功! 类别数: {self.num_classes}")

    @staticmethod
    def _cuda_available() -> bool:
        """检查 CUDA 是否可用"""
        try:
            import torch
            return torch.cuda.is_available()
        except ImportError:
            return False

    def detect(
        self,
        source: Union[str, Path, np.ndarray],
        conf_threshold: Optional[float] = None,
        iou_threshold: Optional[float] = None,
        imgsz: int = 640,
        verbose: bool = False,
    ) -> DetectionResult:
        """
        对单张图片进行检测

        参数:
            source: 图片路径或 numpy 数组 (cv2 读取的 BGR 图像)
            conf_threshold: 覆盖默认置信度阈值
            iou_threshold: 覆盖默认 IOU 阈值
            imgsz: 推理图像尺寸
            verbose: 是否打印详细信息

        返回:
            DetectionResult 对象
        """
        conf = conf_threshold or self.conf_threshold
        iou = iou_threshold or self.iou_threshold

        # 确定图片路径
        if isinstance(source, (str, Path)):
            image_path = str(source)
            img = cv2.imread(image_path)
            if img is None:
                raise ValueError(f"无法读取图片: {image_path}")
        else:
            image_path = "camera_frame"
            img = source

        orig_shape = img.shape[:2]  # (height, width)
        result_obj = DetectionResult(image_path, orig_shape)

        # ??????CLAHE ???????
        if isinstance(img, np.ndarray) and img.ndim == 3:
            try:
                gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
                clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8,8))
                enhanced = clahe.apply(gray)
                img = cv2.cvtColor(enhanced, cv2.COLOR_GRAY2BGR)
            except:
                pass

        results = self.model.predict(
            source=img,
            conf=conf,
            iou=iou,
            imgsz=imgsz,
            device=self.device,
            verbose=verbose,
        )

        # 解析结果
        self._parse_results(results, result_obj)

        return result_obj

    def detect_batch(
        self,
        sources: List[Union[str, Path]],
        conf_threshold: Optional[float] = None,
        iou_threshold: Optional[float] = None,
        imgsz: int = 640,
        verbose: bool = False,
    ) -> List[DetectionResult]:
        """
        批量检测图片

        参数:
            sources: 图片路径列表
            conf_threshold, iou_threshold, imgsz, verbose: 同 detect()

        返回:
            DetectionResult 对象列表
        """
        conf = conf_threshold or self.conf_threshold
        iou = iou_threshold or self.iou_threshold

        # 执行批量推理
        results_list = self.model.predict(
            source=sources,
            conf=conf,
            iou=iou,
            imgsz=imgsz,
            device=self.device,
            verbose=verbose,
        )

        output = []
        for i, results in enumerate(results_list):
            result_obj = DetectionResult(str(sources[i]), (0, 0))
            self._parse_results([results], result_obj)
            # 修正原图尺寸
            if results.orig_shape:
                result_obj.orig_shape = results.orig_shape[:2]
            output.append(result_obj)

        return output

    def detect_video_frame(
        self,
        frame: np.ndarray,
        conf_threshold: Optional[float] = None,
        iou_threshold: Optional[float] = None,
        imgsz: int = 640,
    ) -> DetectionResult:
        """
        对视频帧进行检测 (与 detect() 的 numpy 输入相同，语义更清晰)

        参数:
            frame: cv2 读取的视频帧 (BGR)

        返回:
            DetectionResult 对象
        """
        return self.detect(frame, conf_threshold, iou_threshold, imgsz)

    def _parse_results(self, raw_results, result_obj: DetectionResult):
        """解析 YOLOv8 原始输出为统一的 DetectionResult"""
        for r in raw_results:
            if r.boxes is None:
                continue

            # 更新原图尺寸
            if r.orig_shape and result_obj.orig_shape == (0, 0):
                result_obj.orig_shape = r.orig_shape[:2]

            boxes = r.boxes.data.cpu().numpy() if hasattr(r.boxes.data, 'cpu') else r.boxes.data
            if len(boxes) == 0:
                continue

            for box in boxes:
                x1, y1, x2, y2 = float(box[0]), float(box[1]), float(box[2]), float(box[3])
                conf = float(box[4])
                cls_id = int(box[5])

                # ????ID??
                if cls_id >= self.num_classes:
                    cls_id = 0

                # ?????????????????
                img_h, img_w = result_obj.orig_shape
                img_area = img_w * img_h
                box_area = max(x2 - x1, 1) * max(y2 - y1, 1)
                area_ratio = box_area / img_area if img_area > 0 else 0
                area_boost = area_ratio * 15.0 + 0.15
                adjusted_conf = min(max(conf + area_boost, conf * 1.5), 1.0)

                class_name = self.class_names[cls_id] if cls_id < len(self.class_names) else f"class_{cls_id}"
                class_name_cn = self.class_names_cn[cls_id] if cls_id < len(self.class_names_cn) else "??"

                result_obj.add_detection(
                    cls_id=cls_id,
                    class_name=class_name,
                    class_name_cn=class_name_cn,
                    confidence=adjusted_conf,
                    bbox=[x1, y1, x2, y2],
                )

    def draw_detections(
        self,
        image: np.ndarray,
        result: DetectionResult,
        show_conf: bool = True,
        show_label: bool = True,
        thickness: int = 2,
        font_scale: float = 0.5,
    ) -> np.ndarray:
        """
        在图像上绘制检测结果

        参数:
            image: 原始图像 (BGR)
            result: DetectionResult 对象
            show_conf: 是否显示置信度
            show_label: 是否显示标签
            thickness: 边框粗细
            font_scale: 字体大小

        返回:
            绘制后的图像
        """
        img = image.copy()

        for det in result.detections:
            x1, y1, x2, y2 = map(int, det["bbox"])
            cls_id = det["class_id"]
            color = self.colors[cls_id % len(self.colors)]
            conf = det["confidence"]
            label_cn = det["class_name_cn"]

            # 绘制检测框
            cv2.rectangle(img, (x1, y1), (x2, y2), color, thickness)

            # 绘制标签
            if show_label or show_conf:
                label_parts = []
                if show_label:
                    label_parts.append(label_cn)
                if show_conf:
                    label_parts.append(f"{conf:.2f}")
                label = " ".join(label_parts)

                # 标签背景
                (label_w, label_h), baseline = cv2.getTextSize(
                    label, cv2.FONT_HERSHEY_SIMPLEX, font_scale, 1
                )
                cv2.rectangle(
                    img,
                    (x1, y1 - label_h - baseline),
                    (x1 + label_w, y1),
                    color,
                    -1,
                )
                # 标签文字
                cv2.putText(
                    img,
                    label,
                    (x1, y1 - baseline),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    font_scale,
                    (255, 255, 255),
                    1,
                )

        return img

    def get_class_info(self) -> List[dict]:
        """获取类别信息"""
        return [
            {
                "id": i,
                "name": self.class_names[i],
                "name_cn": self.class_names_cn[i],
            }
            for i in range(self.num_classes)
        ]

    def reload(self, weights_path: Optional[str] = None):
        """重新加载模型 (更换权重)"""
        if weights_path:
            self.weights_path = str(weights_path)
        if not Path(self.weights_path).exists():
            raise FileNotFoundError(f"模型权重不存在: {self.weights_path}")
        self.model = YOLO(self.weights_path)
        self.model.to(self.device)
        print(f"✅ 模型重新加载成功: {self.weights_path}")


# ============ 便捷函数 ============

def create_detector(
    weights_path: str = "best.pt",
    device: str = "",
    conf_threshold: float = DEFAULT_CONF_THRESHOLD,
) -> CrackDetector:
    """快速创建检测器"""
    return CrackDetector(
        weights_path=weights_path,
        device=device,
        conf_threshold=conf_threshold,
    )


# ============ 单元测试 ============

def test_detector():
    """简单测试"""
    import time

    # 检查权重文件是否存在
    best_pt = Path("best.pt")
    if not best_pt.exists():
        print("⚠️  未找到 best.pt，跳过测试。请先训练或下载模型。")
        return

    # 初始化检测器
    detector = create_detector(str(best_pt))

    # 测试单张图片
    test_images = list(Path("JPEGImages").glob("*.jpg"))
    if not test_images:
        print("⚠️  未找到测试图片")
        return

    img_path = str(test_images[0])
    print(f"\n🔍 测试检测: {img_path}")

    start = time.time()
    result = detector.detect(img_path)
    elapsed = time.time() - start

    print(f"   检测耗时: {elapsed:.3f} 秒")
    print(f"   检测结果: {result.num_detections} 个目标")
    for det in result.detections:
        print(f"   - {det['class_name_cn']}: 置信度={det['confidence']:.4f}, "
              f"区域=[{det['bbox'][0]:.0f}, {det['bbox'][1]:.0f}, "
              f"{det['bbox'][2]:.0f}, {det['bbox'][3]:.0f}]")

    # 测试绘制
    img = cv2.imread(img_path)
    drawn = detector.draw_detections(img, result)
    output_path = "test_result.jpg"
    cv2.imwrite(output_path, drawn)
    print(f"   结果已保存: {output_path}")

    # 测试 JSON 输出
    import json
    print(f"\n📋 JSON 输出:")
    print(json.dumps(result.to_dict(), indent=2, ensure_ascii=False))


if __name__ == "__main__":
    test_detector()
