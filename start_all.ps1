Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  道路裂缝检测系统 - 服务启动中..." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 1. YOLO
Write-Host "[1/3] 启动 YOLO 裂缝检测服务..." -ForegroundColor Yellow
$yolo = Start-Process -WindowStyle Hidden -FilePath "python" -ArgumentList "app.py" -WorkingDirectory "D:\road-crack-detection\python-service" -PassThru
Write-Host "  YOLO PID: $($yolo.Id)" -ForegroundColor Green

Start-Sleep -Seconds 5

# 2. Backend
Write-Host "[2/3] 启动 Java 后端服务..." -ForegroundColor Yellow
$backend = Start-Process -WindowStyle Hidden -FilePath "java" -ArgumentList "-Dserver.port=8080 -jar server\crack-bootstrap\target\crack-bootstrap-0.1.0-SNAPSHOT.jar --crack.persistence.mode=db --crack.algorithm.mock-enabled=false --crack.algorithm.base-url=http://localhost:8000" -WorkingDirectory "D:\road-crack-detection" -PassThru
Write-Host "  Backend PID: $($backend.Id)" -ForegroundColor Green

Start-Sleep -Seconds 10

# 3. Frontend
Write-Host "[3/3] 启动 Vite 前端服务..." -ForegroundColor Yellow
$frontend = Start-Process -WindowStyle Hidden -FilePath "cmd.exe" -ArgumentList "/c node_modules\.bin\vite.cmd --host" -WorkingDirectory "D:\road-crack-detection\client" -PassThru
Write-Host "  Frontend PID: $($frontend.Id)" -ForegroundColor Green

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  所有服务已启动！" -ForegroundColor Cyan
Write-Host "  前端地址： http://localhost:5173" -ForegroundColor White
Write-Host "  后端地址： http://localhost:8080" -ForegroundColor White
Write-Host "  YOLO服务： http://localhost:8000" -ForegroundColor White
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "按任意键退出此窗口（不影响后台服务）..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")