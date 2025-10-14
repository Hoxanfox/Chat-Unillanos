@echo off
REM Chat-Unillanos: Script para generar JAR ejecutable
echo === Chat-Unillanos: Build JAR Ejecutable ===

REM Verificar que Maven está instalado
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven no está instalado o no está en PATH.
    pause
    exit /b 1
)

REM Ir a la raíz del proyecto
cd /d %~dp0

echo.
echo [1/2] Compilando proyecto y generando JAR...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Compilación fallida.
    pause
    exit /b 1
)

echo.
echo [2/2] JAR ejecutable generado exitosamente!
echo.
echo Ubicación: Presentacion\Main\target\main-1.0.0-SNAPSHOT.jar
echo.
echo Para ejecutar:
echo   1. Asegúrate de que MySQL esté corriendo: docker-compose up -d
echo   2. Ejecuta: java -jar Presentacion\Main\target\main-1.0.0-SNAPSHOT.jar
echo.

pause

