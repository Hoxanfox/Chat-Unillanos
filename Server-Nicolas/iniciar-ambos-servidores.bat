@echo off
echo ╔════════════════════════════════════════════════════╗
echo ║   INICIANDO SISTEMA P2P - 2 SERVIDORES            ║
echo ╚════════════════════════════════════════════════════╝
echo.
echo Este script iniciará 2 servidores en ventanas separadas:
echo   - Servidor 1: Puerto 22100
echo   - Servidor 2: Puerto 22101
echo.
echo Presiona cualquier tecla para continuar...
pause > nul

cd /d "%~dp0"

echo.
echo [1/2] Iniciando Servidor 1 en puerto 22100...
start "Servidor 1 - Puerto 22100" cmd /k server1.bat

echo [2/2] Esperando 5 segundos antes de iniciar Servidor 2...
timeout /t 5 /nobreak > nul

echo [2/2] Iniciando Servidor 2 en puerto 22101...
start "Servidor 2 - Puerto 22101" cmd /k server2.bat

echo.
echo ╔════════════════════════════════════════════════════╗
echo ║   SERVIDORES INICIADOS                             ║
echo ╠════════════════════════════════════════════════════╣
echo ║ Servidor 1: Puerto 22100                           ║
echo ║ Servidor 2: Puerto 22101                           ║
echo ║                                                    ║
echo ║ Verifica los logs para confirmar la conexión P2P  ║
echo ║ Busca: "Peers activos: 2"                         ║
echo ╚════════════════════════════════════════════════════╝
echo.
pause
