@echo off
echo ============================================
echo   SERVIDOR 1 - SERVIDOR PRINCIPAL
echo   Puerto: 22100
echo   Base de Datos: server-22100 (MySQL 3306)
echo ============================================
echo.
echo Copiando archivos de configuracion...
copy /Y config\database-server1.properties config\database.properties >nul
copy /Y config\application-server1.properties config\server.properties >nul
copy /Y config\application-server1.properties config\p2p.properties >nul
echo.
echo Iniciando servidor...
java -jar comunes\server-app\target\server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
