@echo off
chcp 65001 >nul
title ???????? - ????
echo ============================================
echo   ???????? - ?????...
echo ============================================
echo.

:: ===== 1. Start YOLO Python Service (port 8000) =====
echo [1/3] ?? YOLO ?????? (python-service)...
cd /d "D:\road-crack-detection\python-service"
start "YOLO" cmd /c "python app.py"
timeout /t 5 /nobreak >nul

:: ===== 2. Start Java Backend (port 8080) =====
echo [2/3] ?? Java ???? (crack-bootstrap)...
cd /d "D:\road-crack-detection"
start "Backend" cmd /c "java -jar server\crack-bootstrap\target\crack-bootstrap-0.1.0-SNAPSHOT.jar --crack.persistence.mode=memory --crack.algorithm.mock-enabled=false --crack.algorithm.base-url=http://localhost:8000"
timeout /t 10 /nobreak >nul

:: ===== 3. Start Vite Frontend (port 5173) =====
echo [3/3] ?? Vite ???? (client)...
cd /d "D:\road-crack-detection\client"
start "Frontend" cmd /c "node_modules\.bin\vite.cmd --host"
timeout /t 5 /nobreak >nul

echo.
echo ============================================
echo   ????????
echo.
echo   ?????  http://localhost:5173
echo   ?????  http://localhost:8080
echo   YOLO???  http://localhost:8000
echo.
echo   ? Ctrl+C ?????????????
echo   ????????????? cmd ??
echo ============================================
pause