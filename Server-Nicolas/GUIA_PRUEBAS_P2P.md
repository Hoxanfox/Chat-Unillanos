# Guía de Pruebas P2P - Sistema de Chat Distribuido

## 📋 Requisitos Previos

- Java 21 instalado
- Maven instalado
- Base de datos PostgreSQL configurada
- Dos terminales abiertas

## 🚀 Paso 1: Preparar Configuraciones para Dos Servidores

### Configuración Servidor 1 (Principal)

Archivo: `config/server.properties`
```properties
server.port=8080
peer.server.port=9090
peer.nombre.servidor=Servidor-Principal
peer.max.connections=10
peer.heartbeat.interval.ms=30000
peer.heartbeat.timeout.seconds=60
peer.reconnect.attempts=3
peer.reconnect.delay.ms=5000
```

### Configuración Servidor 2 (Secundario)

Necesitas crear una carpeta separada para el segundo servidor o usar variables de entorno.

**Opción 1: Copiar proyecto**
```bash
# Desde la raíz del proyecto
xcopy /E /I Server-Nicolas Server-Nicolas-2
cd Server-Nicolas-2\config
```

Edita `server.properties`:
```properties
server.port=8081
peer.server.port=9091
peer.nombre.servidor=Servidor-Secundario
peer.max.connections=10
peer.heartbeat.interval.ms=30000
peer.heartbeat.timeout.seconds=60
peer.reconnect.attempts=3
peer.reconnect.delay.ms=5000
# Peer de arranque (conectar al Servidor 1)
peer.bootstrap.nodes=localhost:9090
```

## 🔧 Paso 2: Configurar Base de Datos

Ambos servidores pueden usar la misma base de datos (ya que tienen columna `servidor_padre` para distinguir).

Si quieres bases de datos separadas:

### Base de Datos 1 (Servidor Principal)
```sql
CREATE DATABASE chat_server1;
```

### Base de Datos 2 (Servidor Secundario)
```sql
CREATE DATABASE chat_server2;
```

Edita `config/database.properties` en cada servidor:

**Servidor 1:**
```properties
db.url=jdbc:postgresql://localhost:5432/chat_server1
db.username=postgres
db.password=tu_password
```

**Servidor 2:**
```properties
db.url=jdbc:postgresql://localhost:5432/chat_server2
db.username=postgres
db.password=tu_password
```

## ▶️ Paso 3: Iniciar los Servidores

### Terminal 1 - Servidor Principal (Puerto 8080)
```bash
cd "d:\universidad\decimo semestre\arquitectura de software\Peer to peer\Chat-Unillanos\Server-Nicolas"
mvn spring-boot:run
```

**Espera a ver:**
```
✓ [PeerService] Servicio de peers inicializado
✓ [PeerNotificationService] Servicio de notificaciones P2P inicializado
Servidor P2P iniciado en puerto 9090 (Cliente en puerto 8080)
```

### Terminal 2 - Servidor Secundario (Puerto 8081)
```bash
cd "d:\universidad\decimo semestre\arquitectura de software\Peer to peer\Chat-Unillanos\Server-Nicolas-2"
mvn spring-boot:run
```

**Espera a ver:**
```
✓ [PeerService] Servicio de peers inicializado
✓ Intentando conectar a 1 peers válidos
✓ Conexión saliente establecida con peer ... (localhost:9090)
```

## 🧪 Paso 4: Pruebas Funcionales

### Prueba 1: Verificar Conexión entre Peers

#### En GUI del Servidor 1:
1. Abre el GUI (debería abrirse automáticamente)
2. Click en botón **"Ver Peers"**
3. Deberías ver el Servidor 2 en la lista:
   - Nombre: Servidor-Secundario
   - IP: localhost
   - Puerto: 9091
   - Estado: ONLINE ✅

#### En GUI del Servidor 2:
1. Abre el GUI
2. Click en botón **"Ver Peers"**
3. Deberías ver el Servidor 1 en la lista:
   - Nombre: Servidor-Principal
   - IP: localhost
   - Puerto: 9090
   - Estado: ONLINE ✅

**✅ Si ves ambos servidores ONLINE, la conexión P2P funciona correctamente**

---

### Prueba 2: Registrar Usuarios en Cada Servidor

#### Cliente 1 (Conectado a Servidor 1 - Puerto 8080):
1. Abre la aplicación cliente
2. Configura servidor: `localhost:8080`
3. Registra usuario: **"AliceServer1"**
   - Email: alice@server1.com
   - Password: 123456

#### Cliente 2 (Conectado a Servidor 2 - Puerto 8081):
1. Abre otra instancia del cliente
2. Configura servidor: `localhost:8081`
3. Registra usuario: **"BobServer2"**
   - Email: bob@server2.com
   - Password: 123456

**✅ Cada usuario debe estar registrado en su servidor respectivo**

---

### Prueba 3: Verificar que los Usuarios están en Servidores Diferentes

#### En Base de Datos:
```sql
-- Verifica el servidor padre de cada usuario
SELECT username, email, servidor_padre FROM users;
```

Deberías ver:
- AliceServer1 → servidor_padre = ID del Servidor 1
- BobServer2 → servidor_padre = ID del Servidor 2

---

### Prueba 4: Crear Canal en Servidor 1

#### Cliente 1 (AliceServer1):
1. Inicia sesión
2. Crea un canal llamado **"Canal-CrossServer"**

**✅ El canal se crea en Servidor 1**

---

### Prueba 5: Invitar Usuario del Servidor 2 al Canal

#### Cliente 1 (AliceServer1):
1. En el canal "Canal-CrossServer"
2. Click en "Invitar Usuario"
3. Busca: **"BobServer2"**
4. Envía invitación

**🔍 Monitorea los logs del Servidor 1:**
```
→ [ChannelService] Invitando usuario BobServer2 a canal...
→ [PeerNotificationService] Notificando invitación a canal al peer {id-server-2}
✓ [PeerNotificationService] Notificación de invitación: exitosa
```

**🔍 Monitorea los logs del Servidor 2:**
```
→ [P2PNotificationController] Procesando acción P2P: notificarInvitacionCanal
→ [P2PNotificationController] Invitación a canal recibida
✓ [P2PNotificationController] Invitación procesada
```

---

### Prueba 6: Aceptar Invitación desde Servidor 2

#### Cliente 2 (BobServer2):
1. Verifica que recibió la invitación al canal "Canal-CrossServer"
2. Acepta la invitación

**✅ BobServer2 ahora es miembro del canal creado en Servidor 1**

---

### Prueba 7: ⭐ **PRUEBA PRINCIPAL** - Mensajes entre Servidores

#### Cliente 1 (AliceServer1):
1. En el canal "Canal-CrossServer"
2. Envía mensaje: **"Hola Bob desde Servidor 1! 👋"**

**🔍 Monitorea los logs del Servidor 1:**
```
→ [MessageService] Enviando mensaje de texto...
✓ [MessageService] Mensaje guardado en BD
→ [MessageService] Notificando mensaje a peer {id-server-2} con 1 miembros
→ [PeerNotificationService] Notificando nuevo mensaje al peer {id-server-2}
→ [PeerNotificationService] Enviando petición 'notificarMensaje' al peer {id-server-2}
✓ [PeerNotificationService] Respuesta recibida con status: success
✓ [MessageService] Mensaje notificado exitosamente a peer {id-server-2}
```

**🔍 Monitorea los logs del Servidor 2:**
```
→ [P2PNotificationController] Procesando acción P2P: notificarMensaje
→ [P2PNotificationController] Mensaje recibido de peer:
  Canal: {canal-id}
  Autor: AliceServer1
  Tipo: TEXT
✓ [P2PNotificationController] Mensaje retransmitido a usuarios locales
```

**✅ RESULTADO ESPERADO:** 
- Cliente 2 (BobServer2) recibe el mensaje en tiempo real
- El mensaje aparece como si viniera directamente del servidor
- **BobServer2 NO SABE que AliceServer1 está en otro servidor**

#### Cliente 2 (BobServer2):
1. Responde: **"Hola Alice desde Servidor 2! 🚀"**

**✅ RESULTADO ESPERADO:**
- Cliente 1 (AliceServer1) recibe la respuesta en tiempo real
- La comunicación es bidireccional y transparente

---

### Prueba 8: Chat Privado Cross-Server

#### Cliente 1 (AliceServer1):
1. Busca contacto: **"BobServer2"**
2. Inicia chat privado
3. Envía: **"Este es un mensaje privado cross-server"**

**🔍 Logs esperados:**
```
→ [MessageService] Mensaje privado enviado
→ [MessageService] Notificando mensaje a peer {id-server-2}
✓ [MessageService] Mensaje notificado exitosamente
```

**✅ RESULTADO ESPERADO:**
- BobServer2 recibe el mensaje privado
- El chat 1-a-1 funciona entre servidores

---

### Prueba 9: Verificar Persistencia

#### Prueba de Reconexión:
1. Desconecta Cliente 2 (BobServer2)
2. Cliente 1 envía mensaje: **"Mensaje mientras Bob está offline"**
3. Reconecta Cliente 2 (BobServer2)
4. Solicita historial del canal

**✅ RESULTADO ESPERADO:**
- BobServer2 ve todos los mensajes al reconectarse
- Los mensajes están guardados en el Servidor 1
- El Servidor 2 los recupera vía P2P

---

## 🎯 Checklist de Validación

Marca cada prueba completada:

- [ ] ✅ Servidor 1 inicia correctamente (puerto 8080/9090)
- [ ] ✅ Servidor 2 inicia correctamente (puerto 8081/9091)
- [ ] ✅ Ambos servidores se muestran como ONLINE en "Ver Peers"
- [ ] ✅ Usuario registrado en Servidor 1
- [ ] ✅ Usuario registrado en Servidor 2
- [ ] ✅ Canal creado en Servidor 1
- [ ] ✅ Usuario de Servidor 2 invitado al canal
- [ ] ✅ Usuario de Servidor 2 acepta invitación
- [ ] ⭐ **Mensaje de Servidor 1 → Servidor 2 funciona**
- [ ] ⭐ **Mensaje de Servidor 2 → Servidor 1 funciona**
- [ ] ✅ Chat privado cross-server funciona
- [ ] ✅ Mensajes persisten y se recuperan

---

## 🐛 Troubleshooting

### Error: "Peer destino no encontrado"
**Solución:** Verifica que ambos servidores estén en la BD de peers:
```sql
SELECT * FROM peers;
```

### Error: "No se recibió respuesta del peer"
**Solución:** 
- Verifica que los puertos P2P no estén bloqueados
- Verifica firewall de Windows
- Verifica que `peer.server.port` sea diferente en cada servidor

### Error: "Usuario no encontrado en este servidor"
**Solución:** Esto es normal - significa que el usuario está en el otro servidor. El sistema P2P debería manejarlo automáticamente.

### No se retransmiten mensajes
**Solución:**
1. Verifica logs de ambos servidores
2. Verifica que `PeerNotificationService` esté inicializado
3. Verifica que los usuarios tengan `servidor_padre` correcto en BD

---

## 📊 Métricas de Éxito

Si todo funciona correctamente:

1. **Latencia P2P:** < 100ms entre servidores locales
2. **Tasa de éxito:** 100% de mensajes entregados
3. **Transparencia:** Cliente no detecta que contacto está en otro servidor
4. **Escalabilidad:** Puedes agregar Servidor 3, 4, 5... sin cambios en clientes

---

## 🎉 Prueba Exitosa

Si completaste todas las pruebas:

**¡Felicidades! 🎊**

Tienes un sistema de chat P2P completamente funcional donde:
- Los servidores se comunican automáticamente
- Los clientes NO saben en qué servidor están los demás
- Los mensajes se retransmiten de forma transparente
- El sistema es escalable horizontalmente

**Siguiente paso:** Implementar las mejoras pendientes (sincronización de invitaciones, caché, encriptación, etc.)

