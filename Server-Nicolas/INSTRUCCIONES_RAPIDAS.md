# 🚀 Instrucciones Rápidas - Chat P2P

## ⚡ Inicio Rápido

### Opción 1: Automático (Recomendado)

```cmd
cd Server-Nicolas
iniciar-ambos-servidores.bat
```

Esto iniciará ambos servidores en ventanas separadas automáticamente.

### Opción 2: Manual

**Terminal 1:**
```cmd
cd Server-Nicolas
server1.bat
```

**Terminal 2:**
```cmd
cd Server-Nicolas
server2.bat
```

### 2. Verificar que Funciona

Busca en los logs:
```
✓ [HeartbeatService] Heartbeats enviados: 1 exitosos, 0 fallidos
✓ [HeartbeatService] Estadísticas de red P2P:
- Total de peers: 2
- Peers activos: 2
- Peers offline: 0
```

## 🎯 Qué Buscar en los Logs

### ✅ Señales de Éxito

```
✓ [PeerService] Servicio de peers inicializado
✓ Servidor de Chat iniciado en el puerto 22100
✓ [HeartbeatService] Heartbeats enviados: 1 exitosos
✓ [PeerService] Se encontraron 2 peers activos
```

### ⚠️ Señales de Problema

```
✗ Address already in use: bind
   → Solución: Ya hay un servidor en ese puerto, usa start-server2.bat

✗ Heartbeats enviados: 0 exitosos, 1 fallidos
   → Solución: El otro servidor no está corriendo o hay problema de red

⚠ Peer marcado como OFFLINE
   → Normal si cerraste un servidor
```

## 🧪 Pruebas Simples

### Prueba 1: Conexión Básica
1. Inicia ambos servidores
2. Espera 30 segundos
3. Busca "Peers activos: 2" en los logs

### Prueba 2: Tolerancia a Fallos
1. Cierra el Servidor 2 (Ctrl+C)
2. Espera 5 minutos
3. Verás "Peer marcado como OFFLINE" en Servidor 1
4. Reinicia Servidor 2
5. Se reconectará automáticamente

## 📊 Configuración de Servidores

| Servidor | Puerto | Base de Datos | Logs |
|----------|--------|---------------|------|
| Servidor 1 | 22100 | ./data/chatdb | logs/server.log |
| Servidor 2 | 22101 | ./data/chatdb2 | logs/server2.log |

## 🔧 Cambios Realizados

1. **Timeout aumentado**: De 90 segundos a 5 minutos (300000 ms)
2. **Puertos diferentes**: Servidor 1 usa 22100, Servidor 2 usa 22101
3. **Peers bootstrap configurados**: Cada servidor conoce al otro
4. **Bases de datos separadas**: Cada servidor tiene su propia BD

## 📝 Funcionalidades P2P

- ✅ Descubrimiento automático de peers
- ✅ Heartbeat cada 30 segundos
- ✅ Detección de peers offline (timeout 5 minutos)
- ✅ Sincronización de mensajes
- ✅ Sincronización de usuarios
- ✅ Sincronización de canales
- ✅ Tolerancia a fallos

## 🐛 Solución Rápida de Problemas

**Problema**: No veo "Peers activos: 2"
```cmd
# 1. Verifica que ambos servidores estén corriendo
# 2. Espera 30 segundos (intervalo de heartbeat)
# 3. Revisa los logs de ambos servidores
```

**Problema**: "Address already in use"
```cmd
# Usa los scripts correctos:
start-server1.bat  # Para el primer servidor
start-server2.bat  # Para el segundo servidor
```

**Problema**: Peers se marcan como OFFLINE muy rápido
```
# Ya está corregido en la nueva configuración
# Timeout ahora es de 5 minutos
```

## 📖 Documentación Completa

Para más detalles, lee: `GUIA_P2P.md`

---

**¿Todo funcionando?** Deberías ver:
- 2 servidores corriendo
- Heartbeats exitosos cada 30 segundos
- "Peers activos: 2" en las estadísticas
