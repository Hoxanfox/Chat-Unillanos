@echo off
echo ╔════════════════════════════════════════════════════╗
echo ║   DETENER SERVIDORES                               ║
echo ╚════════════════════════════════════════════════════╝
echo.
echo Buscando procesos Java en los puertos 22100 y 22101...
echo.

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":22100"') do (
    echo Deteniendo proceso en puerto 22100 (PID: %%a)
    taskkill /F /PID %%a
)

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":22101"') do (
    echo Deteniendo proceso en puerto 22101 (PID: %%a)
    taskkill /F /PID %%a
)

echo.
echo ════════════════════════════════════════════════════
echo Servidores detenidos.
echo ════════════════════════════════════════════════════
echo.
pause
