@echo off
echo ============================================
echo   SERVIDOR 3 - PEER B
echo   Puerto: 22400
echo   Base de Datos: server-22400 (MySQL 3308)
echo ============================================
echo.
echo Copiando archivos de configuracion...
copy /Y config\database-server3.properties config\database.properties >nul
copy /Y config\application-server3.properties config\server.properties >nul
copy /Y config\application-server3.properties config\p2p.properties >nul
echo.
echo Iniciando servidor...
java -jar comunes\server-app\target\server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
