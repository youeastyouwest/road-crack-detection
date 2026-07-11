# Start YOLOv8 Python Service
param(
    [string]$Port = "5000",
    [string]$ModelPath = "models/best.pt"
)

$Python = "python"
$Script = "D:\road-crack-detection\python-service\app.py"

Write-Host "Starting YOLOv8 detection service on port $Port..."
Write-Host "Model: $ModelPath"
$env:PORT = $Port
$env:YOLO_MODEL_PATH = $ModelPath
& $Python $Script
