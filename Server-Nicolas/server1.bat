@echo off
echo ╔════════════════════════════════════════╗
echo ║    SERVIDOR 1 - Puerto 22100           ║
echo ╚════════════════════════════════════════╝
cd /d "%~dp0"

set SERVER_PORT=22100
set P2P_PORT=22100
set SERVER_NAME=Servidor-1
set DB_PATH=./data/chatdb
set LOG_FILE=logs/server.log
set BOOTSTRAP_PEERS=172.29.128.1:22101
set TIMEOUT=300000

echo.
echo Configuración:
echo - Puerto: %SERVER_PORT%
echo - Base de datos: %DB_PATH%
echo - Logs: %LOG_FILE%
echo - Conectando a: %BOOTSTRAP_PEERS%
echo.

java -jar comunes\server-app\target\server-app-1.0-SNAPSHOT-jar-with-dependencies.jar ^
  --server.port=%SERVER_PORT% ^
  --p2p.puerto=%P2P_PORT% ^
  --p2p.nombre.servidor=%SERVER_NAME% ^
  --spring.datasource.url=jdbc:h2:file:%DB_PATH% ^
  --logging.file.name=%LOG_FILE% ^
  --p2p.peers.bootstrap=%BOOTSTRAP_PEERS% ^
  --p2p.heartbeat.timeout=%TIMEOUT%

pause
