# Start Backend
$jar = "D:\road-crack-detection\server\crack-bootstrap\target\crack-bootstrap-0.1.0-SNAPSHOT.jar"
$log = "D:\road-crack-detection\server\backend.log"
$p = Start-Process -WindowStyle Hidden -FilePath "java" -ArgumentList "-jar $jar --crack.persistence.mode=memory --crack.algorithm.mock-enabled=false --crack.algorithm.base-url=http://localhost:8000" -PassThru
Write-Host "Backend started PID: $($p.Id)"

# Start Frontend
Set-Location "D:\road-crack-detection\client"
$f = Start-Process -WindowStyle Hidden -FilePath "cmd.exe" -ArgumentList "/c node_modules\.bin\vite.cmd --host" -PassThru
Write-Host "Frontend started PID: $($f.Id)"

# Wait to keep alive
Start-Sleep -Seconds 300
