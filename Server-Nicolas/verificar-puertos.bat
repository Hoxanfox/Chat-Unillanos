@echo off
echo ╔════════════════════════════════════════════════════╗
echo ║   VERIFICACIÓN DE PUERTOS                          ║
echo ╚════════════════════════════════════════════════════╝
echo.
echo Verificando si los puertos 22100 y 22101 están en uso...
echo.

netstat -ano | findstr ":22100"
if %errorlevel% equ 0 (
    echo [✓] Puerto 22100 está EN USO
) else (
    echo [✗] Puerto 22100 está LIBRE
)

netstat -ano | findstr ":22101"
if %errorlevel% equ 0 (
    echo [✓] Puerto 22101 está EN USO
) else (
    echo [✗] Puerto 22101 está LIBRE
)

echo.
echo ════════════════════════════════════════════════════
echo Si ambos puertos están EN USO, los servidores están corriendo.
echo Si alguno está LIBRE, ese servidor no está iniciado.
echo ════════════════════════════════════════════════════
echo.
pause
