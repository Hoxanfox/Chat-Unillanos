# 🚀 Cómo Iniciar el Sistema de Chat P2P

## 📋 Scripts Disponibles

| Script | Descripción |
|--------|-------------|
| `iniciar-ambos-servidores.bat` | ⭐ **RECOMENDADO** - Inicia ambos servidores automáticamente |
| `server1.bat` | Inicia solo el Servidor 1 (Puerto 22100) |
| `server2.bat` | Inicia solo el Servidor 2 (Puerto 22101) |
| `verificar-puertos.bat` | Verifica si los servidores están corriendo |
| `detener-servidores.bat` | Detiene todos los servidores Java en los puertos 22100 y 22101 |

## ⚡ Inicio Rápido (3 pasos)

### 1️⃣ Abre una terminal en la carpeta del proyecto

```cmd
cd Server-Nicolas
```

### 2️⃣ Ejecuta el script de inicio automático

```cmd
iniciar-ambos-servidores.bat
```

Esto abrirá 2 ventanas nuevas, una para cada servidor.

### 3️⃣ Verifica que funciona

En cualquiera de las ventanas de los servidores, busca:

```
✓ [HeartbeatService] Heartbeats enviados: 1 exitosos, 0 fallidos
✓ Peers activos: 2
```

**¡Listo!** Tus servidores P2P están funcionando.

---

## 🔍 Verificar que los Servidores Están Corriendo

Ejecuta:
```cmd
verificar-puertos.bat
```

Deberías ver:
```
[✓] Puerto 22100 está EN USO
[✓] Puerto 22101 está EN USO
```

---

## 🎯 Configuración de Cada Servidor

### Servidor 1
- **Puerto**: 22100
- **Base de datos**: `./data/chatdb`
- **Logs**: `logs/server.log`
- **Se conecta a**: Servidor 2 (172.29.128.1:22101)

### Servidor 2
- **Puerto**: 22101
- **Base de datos**: `./data/chatdb2`
- **Logs**: `logs/server2.log`
- **Se conecta a**: Servidor 1 (172.29.128.1:22100)

---

## 🧪 Probar la Conexión P2P

### Prueba Básica

1. Inicia ambos servidores
2. Espera 30 segundos (tiempo del primer heartbeat)
3. Busca en los logs: `Peers activos: 2`

### Prueba de Sincronización

1. Conecta un cliente al Servidor 1 (puerto 22100)
2. Conecta otro cliente al Servidor 2 (puerto 22101)
3. Registra usuarios en cada servidor
4. Los usuarios deberían aparecer en ambos servidores

### Prueba de Tolerancia a Fallos

1. Cierra el Servidor 2 (Ctrl+C en su ventana)
2. Espera 5 minutos
3. El Servidor 1 mostrará: `Peer marcado como OFFLINE`
4. Reinicia el Servidor 2
5. Se reconectará automáticamente

---

## 🐛 Solución de Problemas

### Problema: "Address already in use"

**Causa**: Ya hay un servidor corriendo en ese puerto.

**Solución**:
```cmd
# Verifica qué está usando el puerto
netstat -ano | findstr ":22100"
netstat -ano | findstr ":22101"

# Cierra los procesos anteriores o reinicia la computadora
```

### Problema: "Heartbeats enviados: 0 exitosos"

**Causa**: Los servidores no se pueden comunicar.

**Soluciones**:
1. Verifica que ambos servidores estén corriendo
2. Espera 30 segundos (intervalo de heartbeat)
3. Verifica que no haya firewall bloqueando los puertos
4. Revisa que las IPs en `p2p.peers.bootstrap` sean correctas

### Problema: Los servidores se inician en el mismo puerto

**Causa**: No estás usando los scripts correctos.

**Solución**: Usa `server1.bat` y `server2.bat` (o `iniciar-ambos-servidores.bat`)

---

## 📊 Logs Importantes

### Inicio Exitoso
```
✓ [PeerService] Servicio de peers inicializado
✓ Servidor de Chat iniciado en el puerto 22100
✓ [HeartbeatService] Servicio de heartbeat inicializado
```

### Conexión P2P Exitosa
```
✓ [HeartbeatService] Heartbeats enviados: 1 exitosos, 0 fallidos
✓ [PeerService] Se encontraron 2 peers activos
✓ [HeartbeatService] Estadísticas de red P2P:
- Total de peers: 2
- Peers activos: 2
- Peers offline: 0
```

### Peer Desconectado
```
⚠ Peer marcado como OFFLINE: 172.29.128.1:22101
✓ [HeartbeatService] Estadísticas de red P2P:
- Total de peers: 2
- Peers activos: 1
- Peers offline: 1
```

---

## 📚 Documentación Adicional

- **Guía completa P2P**: `GUIA_P2P.md`
- **Instrucciones rápidas**: `INSTRUCCIONES_RAPIDAS.md`
- **Resumen de despliegue**: `DEPLOYMENT_SUMMARY.md`

---

## ✅ Checklist de Verificación

- [ ] Ambos servidores inician sin errores
- [ ] Puerto 22100 está en uso (Servidor 1)
- [ ] Puerto 22101 está en uso (Servidor 2)
- [ ] Los logs muestran "Peers activos: 2"
- [ ] Los heartbeats son exitosos (no 0 exitosos)
- [ ] No hay errores de "Address already in use"

---

## 🎉 ¡Todo Listo!

Si ves "Peers activos: 2" en ambos servidores, tu sistema P2P está funcionando correctamente.

Para probar la sincronización de mensajes, necesitas conectar clientes a cada servidor y enviar mensajes entre ellos.
