# 🚀 INICIO RÁPIDO - Pruebas P2P

## ✅ Resumen de lo Implementado

Se ha implementado un sistema completo de comunicación P2P transparente entre servidores. Los clientes pueden comunicarse sin saber que están en servidores diferentes.

## 📦 Archivos Creados

### Código Principal
1. ✅ `IPeerNotificationService.java` - Interfaz de notificaciones P2P
2. ✅ `PeerNotificationServiceImpl.java` - Implementación de comunicación TCP entre servidores
3. ✅ `P2PNotificationController.java` - Maneja notificaciones entrantes de otros peers
4. ✅ `ApplicationContextProvider.java` - Helper para Spring Context
5. ✅ `PeersReportPanel.java` - Panel GUI para ver peers conectados

### Modificaciones
- ✅ `MessageServiceImpl.java` - Retransmite mensajes a peers automáticamente
- ✅ `ServerViewController.java` - Métodos para obtener lista de peers
- ✅ `ServerMainWindow.java` - Botón "Ver Peers" en GUI
- ✅ `pom.xml` (LogicaMensajes) - Dependencia a LogicaPeers

### Utilidades de Prueba
- 📄 `GUIA_PRUEBAS_P2P.md` - Guía completa paso a paso
- 📄 `IMPLEMENTACION_COMUNICACION_P2P.md` - Documentación técnica
- 🔧 `start-server1.bat` - Script para iniciar Servidor 1
- 🔧 `start-server2.bat` - Script para iniciar Servidor 2  
- 🔍 `verificar-p2p.bat` - Script de verificación del sistema

## ⚡ Cómo Probar en 5 Minutos

### Paso 1: Compilar (Terminal 1)
```cmd
cd "d:\universidad\decimo semestre\arquitectura de software\Peer to peer\Chat-Unillanos\Server-Nicolas"
mvn clean install -DskipTests
```

### Paso 2: Iniciar Servidor 1 (Terminal 1)
```cmd
start-server1.bat
```
Espera a ver: `Servidor P2P iniciado en puerto 9090`

### Paso 3: Copiar para Servidor 2
```cmd
cd ..
xcopy /E /I Server-Nicolas Server-Nicolas-2
```

### Paso 4: Configurar Servidor 2
Edita `Server-Nicolas-2\config\server.properties`:
```properties
server.port=8081
peer.server.port=9091
peer.nombre.servidor=Servidor-Secundario
peer.bootstrap.nodes=localhost:9090
```

### Paso 5: Iniciar Servidor 2 (Terminal 2)
```cmd
cd "d:\universidad\decimo semestre\arquitectura de software\Peer to peer\Chat-Unillanos\Server-Nicolas-2"
mvn spring-boot:run
```

### Paso 6: Verificar Conexión P2P
En el GUI de cualquier servidor:
1. Click en **"Ver Peers"**
2. Deberías ver el otro servidor con estado **ONLINE** ✅

## 🧪 Prueba Principal - Mensaje Cross-Server

### Escenario Simple:
1. **Cliente 1** → Conecta al puerto 8080 → Registra usuario "Alice"
2. **Cliente 2** → Conecta al puerto 8081 → Registra usuario "Bob"  
3. Alice crea canal "Test"
4. Alice invita a Bob
5. Bob acepta invitación
6. **Alice envía: "Hola desde Servidor 1"**
7. ✅ **Bob recibe el mensaje en tiempo real**
8. **Bob responde: "Hola desde Servidor 2"**
9. ✅ **Alice recibe la respuesta**

### Logs Esperados en Servidor 1:
```
→ [MessageService] Notificando mensaje a peer xxx con 1 miembros
✓ [PeerNotificationService] Respuesta recibida con status: success
✓ [MessageService] Mensaje notificado exitosamente a peer xxx
```

### Logs Esperados en Servidor 2:
```
→ [P2PNotificationController] Procesando acción P2P: notificarMensaje
→ [P2PNotificationController] Mensaje recibido de peer
✓ [P2PNotificationController] Mensaje retransmitido a usuarios locales
```

## 🎯 Qué Demuestra Esto

- ✅ Los servidores se descubren y conectan automáticamente
- ✅ Los mensajes se retransmiten de servidor a servidor
- ✅ El cliente NO sabe que su contacto está en otro servidor
- ✅ La comunicación es bidireccional y en tiempo real
- ✅ El sistema es completamente transparente

## 📊 Checklist Rápido

Antes de empezar las pruebas:
- [ ] Compilación exitosa (`mvn clean install`)
- [ ] Servidor 1 iniciado (puerto 8080/9090)
- [ ] Servidor 2 configurado y copiado
- [ ] Servidor 2 iniciado (puerto 8081/9091)
- [ ] Ambos servidores muestran "ONLINE" en "Ver Peers"
- [ ] Aplicación cliente disponible

Durante las pruebas:
- [ ] Usuario registrado en Servidor 1
- [ ] Usuario registrado en Servidor 2
- [ ] Canal creado en Servidor 1
- [ ] Usuario de Servidor 2 invitado
- [ ] ⭐ Mensaje enviado de Servidor 1 → Servidor 2
- [ ] ⭐ Mensaje enviado de Servidor 2 → Servidor 1

## 🆘 Solución de Problemas Rápida

### "BUILD FAILURE"
```cmd
# Limpiar completamente
mvn clean
# Reinstalar dependencias
mvn install -DskipTests
```

### "Peer destino no encontrado"
- Verifica que ambos servidores estén ejecutándose
- Revisa tabla `peers` en la base de datos
- Espera 30 segundos para que se complete el heartbeat

### "Puerto ya en uso"
- Cambia los puertos en `server.properties`
- O cierra el proceso que está usando el puerto

### No se retransmiten mensajes
1. Verifica en GUI que el peer esté ONLINE
2. Revisa logs de ambos servidores
3. Verifica que los usuarios tengan `servidor_padre` en BD:
```sql
SELECT username, servidor_padre FROM users;
```

## 📚 Documentación Completa

Para información detallada, consulta:
- **GUIA_PRUEBAS_P2P.md** - Guía paso a paso con todas las pruebas
- **IMPLEMENTACION_COMUNICACION_P2P.md** - Documentación técnica completa

## 🎉 Resultado Esperado

Si todo funciona correctamente:
- Dos servidores independientes comunicándose
- Usuarios en diferentes servidores chateando en tiempo real
- Sistema completamente transparente para los clientes
- Base para un chat distribuido escalable

---

**¡Empieza por el Paso 1 y en 5 minutos tendrás el sistema funcionando!** 🚀

