@echo off
echo ========================================
echo   INICIANDO SERVIDOR CHAT-UNILLANOS
echo ========================================
echo.

cd /d "%~dp0"

echo Compilando el servidor...
call mvn clean compile

if %errorlevel% neq 0 (
    echo.
    echo ERROR: No se pudo compilar el servidor
    pause
    exit /b 1
)

echo.
echo Iniciando el servidor en el puerto 8888...
echo.

call mvn exec:java

pause

