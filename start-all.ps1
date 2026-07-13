# Start All Services
Write-Host "=== Road Crack Detection - Full Stack ==="
Write-Host ""

$ProjectRoot = "C:\Users\WWW\CodeBuddy\20260706100144\road-crack-detection"

$pyJob = Start-Job -ScriptBlock {
    param($root)
    $env:PORT = "5000"
    $env:YOLO_MODEL_PATH = "models/best.pt"
    Set-Location "$root\python-service"
    python app.py
} -ArgumentList $ProjectRoot
Write-Host "[1/3] YOLOv8 service starting on port 5000..."

$jar = "$ProjectRoot\server\crack-bootstrap\target\crack-bootstrap-0.1.0-SNAPSHOT.jar"

$beJob = Start-Job -ScriptBlock {
    param($j)
    java -jar $j --crack.persistence.mode=memory
} -ArgumentList $jar
Write-Host "[2/3] Backend starting on port 8080..."

Write-Host "[3/3] To start frontend, run in another terminal:"
Write-Host "  cd $ProjectRoot\client && npx vite --host"
Write-Host ""
Write-Host "Services:"
Write-Host "  Frontend: http://localhost:5173"
Write-Host "  Backend:  http://localhost:8080"
Write-Host "  YOLOv8:   http://localhost:5000"
Write-Host ""
Write-Host "Press Ctrl+C to stop all services."

$pyJob | Wait-Job
$beJob | Wait-Job