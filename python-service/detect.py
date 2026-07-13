#! /usr/bin/env python3
"""
detect.py
=========
YOLOv8 道路裂缝检测脚本

支持对图片、视频、摄像头实时流进行检测，并保存标注结果。

用法:
  # 检测单张图片
  python detect.py --source test.jpg

  # 检测整个目录
  python detect.py --source ./test_images/

  # 检测视频
  python detect.py --source video.mp4 --output results/

  # 使用摄像头实时检测
  python detect.py --source 0

  # 批量处理 + JSON 输出
  python detect.py --source ./test_images/ --format json

  # 修改阈值
  python detect.py --source test.jpg --conf 0.35 --iou 0.5
"""

import os
import sys
import json
import time
import argparse
from pathlib import Path

import cv2
import numpy as np
from tqdm import tqdm

# 添加项目根目录到 sys.path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from model import CrackDetector, DetectionResult


def parse_args():
    parser = argparse.ArgumentParser(description="YOLOv8 道路裂缝检测")
    parser.add_argument("--weights", type=str, default="best.pt",
                        help="模型权重文件路径 (默认: best.pt)")
    parser.add_argument("--source", type=str, required=True,
                        help="输入来源: 图片路径、目录路径、视频路径、或摄像头ID(0)")
    parser.add_argument("--output", type=str, default="runs/detect",
                        help="输出目录 (默认: runs/detect)")
    parser.add_argument("--conf", type=float, default=0.25,
                        help="置信度阈值 (默认: 0.25)")
    parser.add_argument("--iou", type=float, default=0.45,
                        help="IOU 阈值 (默认: 0.45)")
    parser.add_argument("--imgsz", type=int, default=640,
                        help="推理图像尺寸 (默认: 640)")
    parser.add_argument("--device", type=str, default="",
                        help="推理设备: 'cpu', '0', '1' (默认: 自动选择)")
    parser.add_argument("--format", type=str, default="image",
                        choices=["image", "json", "both"],
                        help="输出格式: image(标注图片), json(结果JSON), both(两者) (默认: image)")
    parser.add_argument("--save-txt", action="store_true",
                        help="保存 YOLO 格式的标注文件")
    parser.add_argument("--show", action="store_true",
                        help="实时显示检测结果 (按 ESC 或 q 退出)")
    parser.add_argument("--no-label", action="store_true",
                        help="不显示标签")
    parser.add_argument("--no-conf", action="store_true",
                        help="不显示置信度")
    parser.add_argument("--line-thickness", type=int, default=2,
                        help="边框粗细 (默认: 2)")
    parser.add_argument("--exist-ok", action="store_true",
                        help="允许覆盖已有输出")
    return parser.parse_args()


def detect_image(
    detector: CrackDetector,
    image_path: str,
    args,
) -> DetectionResult:
    """检测单张图片"""
    return detector.detect(
        source=image_path,
        conf_threshold=args.conf,
        iou_threshold=args.iou,
        imgsz=args.imgsz,
    )


def save_result(
    detector: CrackDetector,
    result: DetectionResult,
    output_dir: Path,
    args,
):
    """保存检测结果"""
    os.makedirs(output_dir, exist_ok=True)

    stem = Path(result.image_path).stem

    # 保存标注图片
    if args.format in ("image", "both"):
        img = cv2.imread(result.image_path)
        if img is not None:
            drawn = detector.draw_detections(
                img, result,
                show_conf=not args.no_conf,
                show_label=not args.no_label,
                thickness=args.line_thickness,
            )
            out_img = output_dir / f"{stem}_result.jpg"
            cv2.imwrite(str(out_img), drawn)

    # 保存 JSON 结果
    if args.format in ("json", "both"):
        out_json = output_dir / f"{stem}_result.json"
        with open(out_json, "w", encoding="utf-8") as f:
            json.dump(result.to_dict(), f, indent=2, ensure_ascii=False)

    # 保存 YOLO 格式标注
    if args.save_txt:
        out_txt = output_dir / f"{stem}.txt"
        with open(out_txt, "w") as f:
            for det in result.detections:
                x1, y1, x2, y2 = det["bbox"]
                img_w = result.orig_shape[1]
                img_h = result.orig_shape[0]
                # 转换为 YOLO 格式 (归一化)
                x_center = (x1 + x2) / 2 / img_w
                y_center = (y1 + y2) / 2 / img_h
                width = (x2 - x1) / img_w
                height = (y2 - y1) / img_h
                f.write(f"{det['class_id']} {x_center:.6f} {y_center:.6f} "
                        f"{width:.6f} {height:.6f}\n")


def process_images(
    detector: CrackDetector,
    src_path: Path,
    output_dir: Path,
    args,
):
    """处理图片目录"""
    extensions = (".jpg", ".jpeg", ".png", ".bmp", ".tif", ".tiff")
    image_files = [
        p for p in src_path.iterdir()
        if p.suffix.lower() in extensions
    ]

    if not image_files:
        print(f"⚠️  目录中没有图片: {src_path}")
        return

    print(f"🔍 共发现 {len(image_files)} 张图片，开始检测...")

    results_summary = []
    for img_file in tqdm(image_files, desc="检测进度"):
        try:
            result = detect_image(detector, str(img_file), args)
            save_result(detector, result, output_dir, args)
            results_summary.append(result.to_dict())
        except Exception as e:
            print(f"\n❌ 检测失败: {img_file.name} - {e}")

    # 保存汇总 JSON
    summary_path = output_dir / "summary.json"
    with open(summary_path, "w", encoding="utf-8") as f:
        json.dump({
            "total_images": len(image_files),
            "detected_count": sum(1 for r in results_summary if r["has_crack"]),
            "total_detections": sum(r["num_detections"] for r in results_summary),
            "results": results_summary,
        }, f, indent=2, ensure_ascii=False)

    # 打印汇总
    print(f"\n{'='*50}")
    print(f"  检测完成!")
    print(f"  总图片: {len(image_files)}")
    print(f"  有裂缝: {sum(1 for r in results_summary if r['has_crack'])}")
    print(f"  无裂缝: {sum(1 for r in results_summary if not r['has_crack'])}")
    print(f"  总目标: {sum(r['num_detections'] for r in results_summary)}")
    print(f"  结果已保存至: {output_dir}")
    print(f"{'='*50}")


def process_video(
    detector: CrackDetector,
    video_path: str,
    output_dir: Path,
    args,
):
    """处理视频文件"""
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"❌ 无法打开视频: {video_path}")
        return

    # 获取视频信息
    fps = cap.get(cv2.CAP_PROP_FPS)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

    print(f"🎬 视频信息: {width}x{height}, {fps:.1f} FPS, {total_frames} 帧")

    # 准备视频写入
    os.makedirs(output_dir, exist_ok=True)
    output_video_path = output_dir / f"result_{Path(video_path).stem}.mp4"
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out = cv2.VideoWriter(str(output_video_path), fourcc, fps, (width, height))

    # 帧处理循环
    frame_count = 0
    crack_frames = 0
    start_time = time.time()

    pbar = tqdm(total=total_frames, desc="处理视频")
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # 检测
        result = detector.detect_video_frame(
            frame,
            conf_threshold=args.conf,
            iou_threshold=args.iou,
            imgsz=args.imgsz,
        )

        # 绘制结果
        drawn = detector.draw_detections(
            frame, result,
            show_conf=not args.no_conf,
            show_label=not args.no_label,
            thickness=args.line_thickness,
        )

        # 在画面上添加信息
        fps_text = f"FPS: {frame_count / (time.time() - start_time + 0.001):.1f}"
        cv2.putText(drawn, fps_text, (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
        if result.has_crack:
            crack_frames += 1
            cv2.putText(drawn, f"⚠️  {result.num_detections} 处裂缝",
                        (10, 70), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

        # 写入输出视频
        out.write(drawn)

        # 显示
        if args.show:
            # 调整显示大小
            display = cv2.resize(drawn, (width // 2, height // 2))
            cv2.imshow("Crack Detection (ESC/q:退出)", display)
            key = cv2.waitKey(1) & 0xFF
            if key in (27, ord("q"), ord("Q")):
                print("\n🛑 用户中断")
                break

        frame_count += 1
        pbar.update(1)
        pbar.set_postfix({"裂缝帧": crack_frames})

    pbar.close()
    cap.release()
    out.release()
    cv2.destroyAllWindows()

    elapsed = time.time() - start_time
    print(f"\n{'='*50}")
    print(f"  视频处理完成!")
    print(f"  总帧数: {frame_count}")
    print(f"  有裂缝帧: {crack_frames} ({crack_frames/max(frame_count,1)*100:.1f}%)")
    print(f"  处理耗时: {elapsed:.1f} 秒")
    print(f"  平均速度: {frame_count/elapsed:.1f} FPS" if elapsed > 0 else "")
    print(f"  结果视频: {output_video_path}")
    print(f"{'='*50}")


def process_camera(
    detector: CrackDetector,
    camera_id: int,
    output_dir: Path,
    args,
):
    """实时摄像头检测"""
    print(f"📷 打开摄像头 #{camera_id}...")
    cap = cv2.VideoCapture(camera_id)
    if not cap.isOpened():
        print(f"❌ 无法打开摄像头 #{camera_id}")
        return

    # 获取摄像头信息
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv2.CAP_PROP_FPS) or 30

    print(f"   分辨率: {width}x{height}")
    print(f"   按 ESC 或 q 退出")
    print(f"   按 s 截图保存\n")

    os.makedirs(output_dir, exist_ok=True)

    frame_count = 0
    crack_frames = 0
    start_time = time.time()
    screenshot_idx = 0

    while True:
        ret, frame = cap.read()
        if not ret:
            print("⚠️  无法读取摄像头画面")
            break

        frame_count += 1

        # 检测
        result = detector.detect_video_frame(
            frame,
            conf_threshold=args.conf,
            iou_threshold=args.iou,
            imgsz=args.imgsz,
        )

        if result.has_crack:
            crack_frames += 1

        # 绘制
        drawn = detector.draw_detections(
            frame, result,
            show_conf=not args.no_conf,
            show_label=not args.no_label,
            thickness=args.line_thickness,
        )

        # 叠加信息
        current_fps = frame_count / (time.time() - start_time + 0.001)
        info_lines = [
            f"FPS: {current_fps:.1f}",
            f"检测到: {result.num_detections} 处裂缝" if result.has_crack else "✅ 路面正常",
            f"帧数: {frame_count} | 含裂缝: {crack_frames}",
            "[s]截图 [q/ESC]退出",
        ]
        for i, line in enumerate(info_lines):
            color = (0, 0, 255) if result.has_crack else (0, 255, 0)
            cv2.putText(drawn, line, (10, 30 + i * 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

        # 显示
        display = cv2.resize(drawn, (width // 2, height // 2))
        cv2.imshow("Crack Detection (实时)", display)
        key = cv2.waitKey(1) & 0xFF

        if key in (27, ord("q"), ord("Q")):
            print("🛑 退出")
            break
        elif key == ord("s"):
            screenshot_path = output_dir / f"screenshot_{screenshot_idx:04d}.jpg"
            cv2.imwrite(str(screenshot_path), drawn)
            print(f"   📸 截图已保存: {screenshot_path}")
            screenshot_idx += 1

    cap.release()
    cv2.destroyAllWindows()

    elapsed = time.time() - start_time
    print(f"\n{'='*50}")
    print(f"  实时检测结束")
    print(f"  总帧数: {frame_count}")
    print(f"  含裂缝帧: {crack_frames}")
    print(f"  运行时间: {elapsed:.1f} 秒")
    print(f"{'='*50}")


def main():
    args = parse_args()

    # 初始化检测器
    print(f"🚀 初始化裂缝检测器...")
    detector = CrackDetector(
        weights_path=args.weights,
        device=args.device,
        conf_threshold=args.conf,
        iou_threshold=args.iou,
    )

    source = args.source

    # 创建输出目录
    timestamp = time.strftime("%Y%m%d_%H%M%S")
    output_dir = Path(args.output) / timestamp
    if not args.exist_ok:
        # 使用时间戳目录避免覆盖
        pass

    # 判断输入来源类型
    if source.isdigit():
        # 摄像头模式
        process_camera(detector, int(source), output_dir, args)

    elif Path(source).is_dir():
        # 目录模式
        process_images(detector, Path(source), output_dir, args)

    elif Path(source).suffix.lower() in (".mp4", ".avi", ".mov", ".mkv", ".flv", ".wmv"):
        # 视频模式
        process_video(detector, source, output_dir, args)

    else:
        # 单张图片模式
        if not Path(source).exists():
            print(f"❌ 文件不存在: {source}")
            sys.exit(1)

        print(f"🔍 检测图片: {source}")
        result = detect_image(detector, source, args)
        save_result(detector, result, output_dir, args)

        print(f"\n检测结果:")
        print(f"  图片: {result.image_path}")
        print(f"  尺寸: {result.orig_shape[1]}x{result.orig_shape[0]}")
        print(f"  目标数: {result.num_detections}")
        print(f"  含裂缝: {'是' if result.has_crack else '否'}")

        if result.has_crack:
            print(f"\n  裂缝详情:")
            for det in result.detections:
                print(f"    [{det['class_name_cn']}] "
                      f"置信度: {det['confidence']:.2%}, "
                      f"位置: ({det['bbox'][0]:.0f}, {det['bbox'][1]:.0f}) - "
                      f"({det['bbox'][2]:.0f}, {det['bbox'][3]:.0f})")

        print(f"\n  结果已保存至: {output_dir}")


if __name__ == "__main__":
    main()
