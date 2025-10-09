@echo off
echo ========================================
echo   INICIANDO CLIENTE CHAT-UNILLANOS
echo ========================================
echo.

cd /d "%~dp0"

echo Compilando el cliente...
call mvn clean compile

if %errorlevel% neq 0 (
    echo.
    echo ERROR: No se pudo compilar el cliente
    pause
    exit /b 1
)

echo.
echo Iniciando la aplicacion JavaFX...
echo.

cd Presentacion\Main
call mvn javafx:run

pause

