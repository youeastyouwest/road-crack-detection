@echo off
chcp 65001 >nul
title Road Crack Detection - Start All
echo ============================================
echo   Road Crack Detection - Starting...
echo ============================================
echo.

:: ===== 1. Start YOLO Python Service (port 8000) =====
echo [1/3] Starting YOLO Python Service (python-service)...
cd /d "D:\road-crack-detection\python-service"
start "YOLO" cmd /c "python app.py"
timeout /t 5 /nobreak >nul

:: ===== 2. Start Java Backend (port 8080) =====
echo [2/3] Starting Java Backend (crack-bootstrap, DB mode)...
cd /d "D:\road-crack-detection"
start "Backend" cmd /c "java -Dserver.port=8080 -jar server\crack-bootstrap\target\crack-bootstrap-0.1.0-SNAPSHOT.jar --crack.persistence.mode=db --crack.algorithm.mock-enabled=false --crack.algorithm.base-url=http://localhost:8000"
timeout /t 10 /nobreak >nul

:: ===== 3. Start Vite Frontend (port 5173) =====
echo [3/3] Starting Vite Frontend (client)...
cd /d "D:\road-crack-detection\client"
start "Frontend" cmd /c "node_modules\.bin\vite.cmd --host"
timeout /t 5 /nobreak >nul

echo.
echo ============================================
echo   All services started!
echo.
echo   Frontend:  http://localhost:5173
echo   Backend:   http://localhost:8080
echo   YOLO API:  http://localhost:8000
echo.
echo   Press Ctrl+C to stop each service window.
echo   Close each cmd window to stop services.
echo ============================================
pause
