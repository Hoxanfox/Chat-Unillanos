# 🏗️ Arquitectura del Servidor - Chat Unillanos

## Índice
1. [Visión General](#visión-general)
2. [Estructura de Módulos](#estructura-de-módulos)
3. [Flujo de Datos](#flujo-de-datos)
4. [Configuración](#configuración)
5. [Sistema P2P](#sistema-p2p)
6. [Gestión de Archivos](#gestión-de-archivos)
7. [Sistema de Eventos](#sistema-de-eventos)

---

## Visión General

Chat Unillanos Server es un sistema de mensajería en tiempo real con arquitectura distribuida P2P (Peer-to-Peer). El servidor está construido con Spring Framework y sigue una arquitectura modular en capas.

### Características Principales
- ✅ Comunicación en tiempo real vía WebSocket
- ✅ Arquitectura P2P para escalabilidad horizontal
- ✅ Mensajes de texto y audio
- ✅ Canales grupales y chats directos
- ✅ Sistema de invitaciones a canales
- ✅ Transferencia de archivos por chunks
- ✅ Heartbeat para monitoreo de peers
- ✅ Notificaciones push en tiempo real

### Stack Tecnológico
- **Lenguaje:** Java 21
- **Framework:** Spring Framework 6.2.11
- **ORM:** Hibernate 6.2.7 con JPA
- **Base de Datos:** MySQL 8.0
- **Serialización:** Gson
- **Protocolo:** WebSocket + JSON
- **Build Tool:** Maven

---

## Estructura de Módulos

El proyecto está organizado en módulos Maven independientes:

### 📁 Comunes (`comunes/`)
Módulos compartidos por todas las capas:

#### `server-app`
- Punto de entrada de la aplicación
- Configuración de Spring Context
- Inicialización de componentes

#### `Server-DTO`
- Data Transfer Objects para comunicación entre capas
- DTOs de Request/Response
- DTOs de dominio (Usuario, Canal, Mensaje, Peer)

#### `server-Utils`
- Utilidades compartidas
- Gestión de chunks de archivos
- Helpers y validadores

#### `server-events`
- Eventos del sistema para notificaciones push
- `NewMessageEvent`: Nuevo mensaje en canal
- `UserInvitedEvent`: Invitación a canal
- `ContactListUpdateEvent`: Actualización de contactos
- `ForceLogoutEvent`: Cierre de sesión forzado
- `PeerConnectedEvent`: Nuevo peer conectado
- `PeerDisconnectedEvent`: Peer desconectado
- Y más...

### 📁 Datos (`datos/`)
Capa de persistencia:

#### `server-dominio`
- Entidades JPA (Usuario, Canal, Mensaje, Peer, etc.)
- Modelos de dominio

#### `server-persistencia`
- Repositorios Spring Data JPA
- Acceso a base de datos
- Queries personalizadas

### 📁 Negocio (`negocio/`)
Lógica de negocio:

#### `server-logicaFachada`
- Fachada principal (`IChatFachada`)
- Punto de entrada para controladores
- Coordina servicios de negocio

#### `server-logicaUsuarios`
- Autenticación y registro
- Gestión de usuarios
- Estados de conexión

#### `server-LogicaCanales`
- Creación de canales
- Gestión de membresías
- Sistema de invitaciones

#### `server-LogicaMensajes`
- Envío de mensajes
- Historial de conversaciones
- Transcripciones de audio

#### `server-LogicaPeers`
- Gestión de red P2P
- Heartbeat monitoring
- Sincronización entre peers

### 📁 Transporte (`transporte/`)
Capa de comunicación:

#### `server-Transporte`
- `ServerListener`: Escucha conexiones entrantes
- `ClientHandler`: Maneja conexiones de clientes WebSocket
- `PeerHandler`: Maneja conexiones P2P entre servidores
- `PeerConnectionManager`: Gestiona pool de conexiones P2P

#### `server-controladorTransporte`
- `RequestDispatcher`: Enruta peticiones a controladores
- Controladores especializados:
  - `UserController`: Usuarios y autenticación
  - `ChannelController`: Canales y membresías
  - `MessageController`: Mensajes
  - `FileController`: Archivos
  - `PeerController`: Operaciones P2P

### 📁 Vista (`vista/`)
Interfaz de usuario del servidor:

#### `server-vista`
- `ServerMainWindow`: Ventana principal del servidor
- Interfaz gráfica para administración

#### `server-controladorVista`
- Controladores de la interfaz gráfica
- Lógica de presentación

---

## Flujo de Datos

### 1. Conexión del Cliente
```
Cliente → WebSocket → ServerListener → ClientHandler
```

### 2. Procesamiento de Petición
```
ClientHandler → RequestDispatcher → Controller específico → Fachada → Servicio → Repositorio → BD
```

### 3. Respuesta al Cliente
```
BD → Repositorio → Servicio → Fachada → Controller → ClientHandler → Cliente
```

### 4. Notificación Push
```
Evento → EventPublisher → Todos los ClientHandlers conectados → Clientes
```

### 5. Comunicación P2P
```
Servidor A → PeerConnectionManager → PeerHandler → Servidor B
```

---

## Configuración

### Archivos de Configuración (`config/`)

#### `server.properties`
```properties
# Puerto principal del servidor
server.port=22100
server.max.connections=100

# Configuración P2P
peer.server.port=22200
peer.max.connections=50
peer.heartbeat.interval.ms=1000
peer.heartbeat.timeout.seconds=60
peer.reconnect.attempts=3
peer.reconnect.delay.ms=5000

# Peers de arranque (Bootstrap)
peer.bootstrap.nodes=
```

#### `database.properties`
```properties
# Conexión MySQL
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/chat_db
db.username=root
db.password=root1234

# Hibernate
hibernate.dialect=org.hibernate.dialect.MySQLDialect
hibernate.hbm2ddl.auto=update
hibernate.show_sql=true
```

#### `p2p.properties`
```properties
# Sistema P2P
p2p.enabled=true
p2p.puerto=22100
p2p.nombre.servidor=Servidor-Secundario

# Heartbeat
p2p.heartbeat.enabled=true
p2p.heartbeat.interval=30000
p2p.heartbeat.timeout=90000

# Descubrimiento
p2p.discovery.enabled=true
p2p.discovery.interval=300000
p2p.peers.bootstrap=192.168.1.4:22100

# Cliente P2P
p2p.client.timeout=10000
p2p.client.pool.threads=10
p2p.client.retry.attempts=3
p2p.client.retry.delay=1000
```

#### `mail.properties`
```properties
# Configuración de correo (Gmail)
mail.host=smtp.gmail.com
mail.port=587
mail.username=nicolaslozanoc12@gmail.com
mail.password=${MAIL_PASSWORD}
mail.properties.mail.smtp.auth=true
mail.properties.mail.smtp.starttls.enable=true
```

### Docker Compose
```yaml
version: '3.8'
services:
  db:
    image: mysql:8.0
    container_name: chat-db
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root1234
      MYSQL_DATABASE: chat_db
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
```

---

## Sistema P2P

### Arquitectura P2P
El servidor puede funcionar como nodo en una red distribuida de servidores:

```
Servidor A ←→ Servidor B ←→ Servidor C
     ↓              ↓              ↓
Clientes 1-50  Clientes 51-100  Clientes 101-150
```

### Componentes P2P

#### PeerConnectionManager
- Mantiene pool de conexiones activas con otros peers
- Gestiona reconexiones automáticas
- Balancea carga entre peers

#### PeerHandler
- Maneja comunicación bidireccional con otros servidores
- Procesa peticiones retransmitidas
- Sincroniza datos entre peers

#### Heartbeat System
- Monitorea estado de peers cada 30 segundos (configurable)
- Marca peers como offline si no responden en 90 segundos
- Permite recuperación automática de conexiones

### Operaciones P2P

#### Añadir Peer
```java
chatFachada.agregarPeer(ip, puerto, nombreServidor);
```

#### Retransmitir Petición
```java
DTOResponse respuesta = chatFachada.retransmitirPeticion(peerDestinoId, peticionOriginal);
```

#### Sincronizar Lista de Peers
```java
chatFachada.actualizarListaPeers(listaDePeers);
```

### Casos de Uso P2P

1. **Balanceo de Carga**: Distribuir clientes entre múltiples servidores
2. **Alta Disponibilidad**: Si un servidor falla, otros continúan operando
3. **Mensajería Cross-Server**: Usuarios en diferentes servidores pueden comunicarse
4. **Sincronización de Datos**: Compartir información de usuarios y canales

---

## Gestión de Archivos

### Sistema de Chunks
Los archivos grandes se dividen en chunks para transferencia eficiente:

#### Subida de Archivos
1. **Iniciar Upload**: Cliente solicita `startFileUpload` con metadata
2. **Enviar Chunks**: Cliente envía chunks secuencialmente con `uploadFileChunk`
3. **Confirmar Chunks**: Servidor envía ACK por cada chunk recibido
4. **Finalizar Upload**: Cliente envía `endFileUpload` para ensamblar archivo

#### Descarga de Archivos
1. **Iniciar Download**: Cliente solicita `startFileDownload` con fileId
2. **Solicitar Chunks**: Cliente solicita chunks con `requestFileChunk`
3. **Recibir Chunks**: Servidor envía chunks vía push
4. **Ensamblar**: Cliente ensambla chunks en archivo completo

### Almacenamiento
```
storage/
├── audio_files/        # Mensajes de audio
└── user_photos/        # Fotos de perfil
```

### Chunk Manager
- Tamaño de chunk: Configurable (default: 64KB)
- Formato: Base64 para transmisión JSON
- Validación: Checksum por chunk
- Timeout: 30 segundos por chunk

---

## Sistema de Eventos

### Event Publisher
El servidor usa un sistema de eventos para notificaciones push en tiempo real.

### Tipos de Eventos

#### NewMessageEvent
Notifica nuevo mensaje en un canal:
```java
{
  "action": "push_newMessage",
  "data": {
    "messageId": "uuid",
    "channelId": "uuid",
    "author": {...},
    "content": "..."
  }
}
```

#### UserInvitedEvent
Notifica invitación a canal:
```java
{
  "action": "push_userInvited",
  "data": {
    "channelId": "uuid",
    "channelName": "...",
    "owner": {...}
  }
}
```

#### ContactListUpdateEvent
Notifica cambio en lista de contactos:
```java
{
  "action": "push_contactListUpdate",
  "data": {
    "shouldRefresh": true
  }
}
```

### Broadcast vs Unicast

**Broadcast**: Envía a todos los clientes conectados
```java
contactListBroadcaster.broadcastContactListUpdate(data);
```

**Unicast**: Envía a cliente específico
```java
handler.sendMessage(jsonResponse);
```

**Multicast**: Envía a miembros de un canal
```java
chatFachada.notificarMiembrosDeCanal(channelId, evento);
```

---

## Seguridad

### Autenticación
- Basada en sesión WebSocket
- Usuario debe autenticarse antes de realizar operaciones
- Token de sesión mantenido en `ClientHandler`

### Validaciones
- Verificación de permisos por operación
- Validación de membresía en canales
- Validación de propiedad de recursos

### Acciones Públicas
Solo estas acciones no requieren autenticación:
- `authenticateUser`
- `registerUser`
- `uploadFileForRegistration`
- `uploadFileChunk` (para registro)
- `endFileUpload` (para registro)

---

## Escalabilidad

### Horizontal (P2P)
- Agregar más servidores a la red P2P
- Distribución automática de carga
- Sincronización de datos entre peers

### Vertical
- Aumentar `server.max.connections`
- Aumentar `peer.max.connections`
- Optimizar pool de threads

### Base de Datos
- Índices en campos frecuentemente consultados
- Connection pooling con Hibernate
- Queries optimizadas con JPA

---

## Monitoreo y Logs

### Logs del Sistema
```java
System.out.println("→ [Controller] Procesando acción...");
System.out.println("✓ [Controller] Operación exitosa");
System.err.println("✗ [Controller] Error: ...");
```

### Métricas P2P
- Número de peers activos
- Latencia de heartbeat
- Tasa de reconexiones

### Métricas de Clientes
- Clientes conectados
- Mensajes por segundo
- Tasa de errores

---

## Mantenimiento

### Scripts SQL
```sql
-- Limpiar peers inválidos
-- Ver: scripts/limpiar_peers_invalidos.sql
```

### Backup
- Base de datos MySQL: Backup diario recomendado
- Archivos de storage: Backup incremental

### Actualizaciones
1. Detener servidor
2. Actualizar código
3. Ejecutar migraciones de BD si es necesario
4. Reiniciar servidor
5. Verificar logs

---

## Troubleshooting

### Problema: Peer no se conecta
**Solución:**
- Verificar firewall en puerto P2P
- Verificar configuración en `p2p.properties`
- Revisar logs de heartbeat

### Problema: Cliente no recibe push
**Solución:**
- Verificar conexión WebSocket activa
- Verificar que el usuario esté autenticado
- Revisar logs de EventPublisher

### Problema: Error al subir archivo
**Solución:**
- Verificar permisos en directorio `storage/`
- Verificar tamaño de chunk
- Revisar timeout de conexión

---

## Desarrollo

### Agregar Nueva Acción
1. Crear DTO en `Server-DTO`
2. Agregar método en fachada
3. Implementar lógica en servicio
4. Crear handler en controlador
5. Agregar acción a `SUPPORTED_ACTIONS`
6. Documentar en `DOCUMENTACION_API.md`

### Agregar Nuevo Evento Push
1. Crear clase de evento en `server-events`
2. Publicar evento en servicio
3. Implementar listener en `ClientHandler`
4. Documentar formato en `DOCUMENTACION_API.md`

### Testing
```bash
# Compilar proyecto
mvn clean install

# Ejecutar tests
mvn test

# Ejecutar servidor
mvn exec:java
```

---

## Referencias

- [Documentación de API](DOCUMENTACION_API.md)
- [Spring Framework Documentation](https://spring.io/projects/spring-framework)
- [Hibernate Documentation](https://hibernate.org/orm/documentation/)
- [WebSocket Protocol](https://tools.ietf.org/html/rfc6455)
