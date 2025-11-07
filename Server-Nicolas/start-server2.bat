@echo off
echo ============================================
echo   SERVIDOR 2 - PEER A
echo   Puerto: 22300
echo   Base de Datos: server-22300 (MySQL 3307)
echo ============================================
echo.
echo Copiando archivos de configuracion...
copy /Y config\database-server2.properties config\database.properties >nul
copy /Y config\application-server2.properties config\server.properties >nul
copy /Y config\application-server2.properties config\p2p.properties >nul
echo.
echo Iniciando servidor...
java -jar comunes\server-app\target\server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
