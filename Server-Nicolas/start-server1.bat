@echo off
echo ========================================
echo Iniciando Servidor 1 (Puerto 22100)
echo ========================================
cd /d "%~dp0"
java -jar comunes\server-app\target\server-app-1.0-SNAPSHOT-jar-with-dependencies.jar --p2p.heartbeat.timeout=300000
pause
