@echo off
echo ========================================
echo   INICIANDO CLIENTE CHAT-UNILLANOS
echo ========================================
echo.

cd /d "%~dp0"

echo Compilando e instalando el cliente...
call mvn clean install -DskipTests

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

