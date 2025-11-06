@echo off
echo ╔════════════════════════════════════════╗
echo ║    SERVIDOR 2 - Puerto 22101           ║
echo ╚════════════════════════════════════════╝
cd /d "%~dp0"

set SERVER_PORT=22101
set P2P_PORT=22101
set SERVER_NAME=Servidor-2
set DB_PATH=./data/chatdb2
set LOG_FILE=logs/server2.log
set BOOTSTRAP_PEERS=172.29.128.1:22100
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
