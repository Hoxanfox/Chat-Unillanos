@echo off
echo ========================================
echo Iniciando Servidor 2 (Puerto 22101)
echo ========================================
cd /d "%~dp0"
java -jar comunes\server-app\target\server-app-1.0-SNAPSHOT-jar-with-dependencies.jar --server.port=22101 --p2p.puerto=22101 --p2p.nombre.servidor=Servidor-2 --spring.datasource.url=jdbc:h2:file:./data/chatdb2 --logging.file.name=logs/server2.log --p2p.peers.bootstrap=172.29.128.1:22100 --p2p.heartbeat.timeout=300000
pause
