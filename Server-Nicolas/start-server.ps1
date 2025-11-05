# Script para iniciar el servidor en segundo plano
$jarPath = "vista\server-vista\target\server-vista-1.0-SNAPSHOT.jar"

Write-Host "Iniciando servidor..." -ForegroundColor Green
Start-Process -FilePath "java" -ArgumentList "-jar", $jarPath -NoNewWindow -PassThru

Write-Host "Servidor iniciado. Presiona Ctrl+C para detener." -ForegroundColor Yellow
