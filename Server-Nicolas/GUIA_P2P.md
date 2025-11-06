# 🌐 Guía de Funcionalidades P2P del Chat

## 📋 Índice
1. [¿Qué es P2P?](#qué-es-p2p)
2. [Funcionalidades Implementadas](#funcionalidades-implementadas)
3. [Cómo Iniciar los Servidores](#cómo-iniciar-los-servidores)
4. [Cómo Probar P2P](#cómo-probar-p2p)
5. [Logs y Monitoreo](#logs-y-monitoreo)
6. [Solución de Problemas](#solución-de-problemas)

---

## 🤔 ¿Qué es P2P?

**P2P (Peer-to-Peer)** significa que múltiples servidores pueden comunicarse directamente entre sí, sin necesidad de un servidor central. En este chat:

- Cada servidor es un **peer** (compañero)
- Los peers se descubren y conectan automáticamente
- Los mensajes se sincronizan entre todos los servidores
- Si un servidor cae, los demás siguen funcionando

---

## ✨ Funcionalidades Implementadas

### 1. **Descubrimiento Automático de Peers**
- Los servidores se encuentran automáticamente
- Puedes configurar peers conocidos en `p2p.peers.bootstrap`
- Cada 5 minutos busca nuevos peers

### 2. **Heartbeat (Latido del Corazón)**
- Cada 30 segundos los servidores se envían un "ping"
- Si un servidor no responde en 5 minutos, se marca como OFFLINE
- Detecta automáticamente cuando un servidor vuelve a estar online

### 3. **Sincronización de Mensajes**
- Cuando envías un mensaje en un servidor, se replica a todos los demás
- Los usuarios conectados a diferentes servidores pueden chatear entre sí
- Los mensajes se guardan en la base de datos de cada servidor

### 4. **Sincronización de Usuarios**
- Los usuarios registrados en un servidor son visibles en todos
- El estado de conexión se sincroniza (online/offline)
- Los perfiles se replican automáticamente

### 5. **Sincronización de Canales**
- Los canales creados en un servidor aparecen en todos
- Las membresías se sincronizan
- Los mensajes del canal se replican

### 6. **Tolerancia a Fallos**
- Si un servidor cae, los demás siguen funcionando
- Cuando vuelve, se reconecta automáticamente
- Los mensajes perdidos se sincronizan

---

## 🚀 Cómo Iniciar los Servidores

### Opción 1: Usando los Scripts (Recomendado)

1. **Recompilar el proyecto** (solo si hiciste cambios):
   ```cmd
   cd Server-Nicolas
   mvn clean package -DskipTests
   ```

2. **Iniciar Servidor 1** (en una terminal):
   ```cmd
   start-server1.bat
   ```

3. **Iniciar Servidor 2** (en otra terminal):
   ```cmd
   start-server2.bat
   ```

### Opción 2: Manualmente

**Terminal 1 - Servidor 1:**
```cmd
cd Server-Nicolas
java -jar comunes\server-app\target\server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Terminal 2 - Servidor 2:**
```cmd
cd Server-Nicolas
java -Dspring.config.name=application-server2 -jar comunes\server-app\target\server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## 🧪 Cómo Probar P2P

### Prueba 1: Verificar Conexión entre Servidores

1. Inicia ambos servidores
2. Busca en los logs:
   ```
   ✓ [HeartbeatService] Heartbeats enviados: 1 exitosos, 0 fallidos
   ```
3. Verifica las estadísticas:
   ```
   ✓ [HeartbeatService] Estadísticas de red P2P:
   - Total de peers: 2
   - Peers activos: 2
   - Peers offline: 0
   ```

### Prueba 2: Sincronización de Usuarios

1. **En Cliente 1** (conectado al Servidor 1 - puerto 22100):
   - Registra un usuario: `usuario1`

2. **En Cliente 2** (conectado al Servidor 2 - puerto 22101):
   - Deberías ver a `usuario1` en la lista de usuarios
   - Registra otro usuario: `usuario2`

3. **En Cliente 1**:
   - Deberías ver a `usuario2` aparecer automáticamente

### Prueba 3: Chat entre Servidores

1. **Cliente 1** (Servidor 1): Crea un canal "General"
2. **Cliente 2** (Servidor 2): Únete al canal "General"
3. **Cliente 1**: Envía un mensaje
4. **Cliente 2**: Deberías recibir el mensaje instantáneamente

### Prueba 4: Tolerancia a Fallos

1. Inicia ambos servidores
2. Cierra el Servidor 2 (Ctrl+C)
3. Observa los logs del Servidor 1:
   ```
   ⚠ Peer marcado como OFFLINE: 172.29.128.1:22101
   ```
4. Reinicia el Servidor 2
5. Observa cómo se reconecta automáticamente

---

## 📊 Logs y Monitoreo

### Símbolos en los Logs

- `✓` = Operación exitosa
- `⚠` = Advertencia
- `✗` = Error
- `?` = Información

### Logs Importantes

**Conexión exitosa:**
```
✓ [HeartbeatService] Heartbeats enviados: 1 exitosos, 0 fallidos
```

**Peer desconectado:**
```
⚠ Peer marcado como OFFLINE: 172.29.128.1:22101
```

**Sincronización de mensaje:**
```
✓ [MessageSyncService] Mensaje sincronizado a 2 peers
```

**Estadísticas de red:**
```
✓ [HeartbeatService] Estadísticas de red P2P:
- Total de peers: 2
- Peers activos: 2
- Peers offline: 0
```

### Archivos de Log

- **Servidor 1**: `Server-Nicolas/logs/server.log`
- **Servidor 2**: `Server-Nicolas/logs/server2.log`

---

## 🔧 Solución de Problemas

### Problema 1: "Address already in use"

**Causa**: Intentas iniciar dos servidores en el mismo puerto.

**Solución**:
- Usa `start-server1.bat` y `start-server2.bat`
- O especifica `-Dspring.config.name=application-server2` para el segundo servidor

### Problema 2: "Heartbeats enviados: 0 exitosos"

**Causa**: Los servidores no se pueden comunicar.

**Solución**:
1. Verifica que ambos servidores estén corriendo
2. Revisa la configuración de `p2p.peers.bootstrap`
3. Asegúrate de que los puertos no estén bloqueados por firewall
4. Verifica la IP en los logs: `[NetworkUtils] Dirección IP del servidor detectada`

### Problema 3: "Peers marcados como OFFLINE rápidamente"

**Causa**: El timeout era muy corto (90 segundos).

**Solución**: Ya está corregido a 5 minutos (300000 ms)

### Problema 4: Los mensajes no se sincronizan

**Causa**: Los peers no están conectados o hay error en la sincronización.

**Solución**:
1. Verifica que ambos servidores muestren "Peers activos: 2"
2. Revisa los logs de sincronización
3. Asegúrate de que los usuarios estén en el mismo canal

### Problema 5: No veo cambios en la GUI

**Causa**: La GUI actual es básica y no muestra visualmente el estado P2P.

**Solución**: 
- Los cambios P2P se ven en los **logs de consola**
- Para ver el efecto, necesitas:
  - 2 servidores corriendo
  - 2 clientes conectados (uno a cada servidor)
  - Enviar mensajes entre ellos

---

## 📝 Configuración Avanzada

### Cambiar Puertos

Edita `application.properties` o `application-server2.properties`:
```properties
server.port=22100
p2p.puerto=22100
```

### Agregar Más Servidores

Para un tercer servidor, crea `application-server3.properties`:
```properties
server.port=22102
p2p.puerto=22102
p2p.nombre.servidor=Servidor-3
p2p.peers.bootstrap=172.29.128.1:22100,172.29.128.1:22101
spring.datasource.url=jdbc:h2:file:./data/chatdb3
logging.file.name=logs/server3.log
```

### Ajustar Timeouts

```properties
# Más frecuente (cada 10 segundos)
p2p.heartbeat.interval=10000

# Timeout más largo (10 minutos)
p2p.heartbeat.timeout=600000
```

---

## 🎯 Resumen de Comandos Rápidos

```cmd
# Recompilar
cd Server-Nicolas
mvn clean package -DskipTests

# Iniciar Servidor 1
start-server1.bat

# Iniciar Servidor 2 (en otra terminal)
start-server2.bat

# Ver logs en tiempo real
tail -f logs/server.log
tail -f logs/server2.log
```

---

## 📚 Arquitectura P2P

```
┌─────────────────┐         ┌─────────────────┐
│   Servidor 1    │◄───────►│   Servidor 2    │
│  Puerto: 22100  │ Heartbeat│  Puerto: 22101  │
│  DB: chatdb     │  Sync    │  DB: chatdb2    │
└────────┬────────┘         └────────┬────────┘
         │                           │
         │                           │
    ┌────▼────┐                 ┌────▼────┐
    │Cliente 1│                 │Cliente 2│
    └─────────┘                 └─────────┘
```

---

## ✅ Checklist de Verificación

- [ ] Ambos servidores inician sin errores
- [ ] Los logs muestran "Peers activos: 2"
- [ ] Los heartbeats son exitosos
- [ ] Los usuarios se sincronizan entre servidores
- [ ] Los mensajes se replican correctamente
- [ ] Los canales aparecen en ambos servidores
- [ ] Al cerrar un servidor, el otro detecta el OFFLINE
- [ ] Al reiniciar, se reconecta automáticamente

---

**¡Listo!** Ahora tienes un sistema de chat P2P completamente funcional. 🎉
