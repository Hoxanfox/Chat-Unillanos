# 💬 Chat Unillanos Server

Sistema de mensajería en tiempo real con arquitectura distribuida P2P (Peer-to-Peer) construido con Spring Framework.

## 🚀 Características

- ✅ **Mensajería en Tiempo Real**: WebSocket para comunicación bidireccional instantánea
- ✅ **Arquitectura P2P**: Escalabilidad horizontal mediante red distribuida de servidores
- ✅ **Mensajes Multimedia**: Soporte para texto y audio
- ✅ **Canales Grupales**: Crea y gestiona canales con múltiples miembros
- ✅ **Chats Directos**: Mensajería privada entre usuarios
- ✅ **Sistema de Invitaciones**: Invita usuarios a canales con aceptación/rechazo
- ✅ **Transferencia de Archivos**: Sistema de chunks para archivos grandes
- ✅ **Notificaciones Push**: Eventos en tiempo real para todos los clientes
- ✅ **Heartbeat Monitoring**: Monitoreo automático de estado de peers
- ✅ **Persistencia**: Base de datos MySQL con Hibernate/JPA

## 📋 Requisitos

- **Java**: 21 o superior
- **Maven**: 3.6 o superior
- **MySQL**: 8.0 o superior
- **Docker** (opcional): Para ejecutar MySQL en contenedor

## 🛠️ Instalación

### 1. Clonar el Repositorio
```bash
git clone <repository-url>
cd ChatProject-Server
```

### 2. Configurar Base de Datos

**Opción A: Usar Docker**
```bash
docker-compose up -d
```

**Opción B: MySQL Local**
```sql
CREATE DATABASE chat_db;
```

Editar `config/database.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/chat_db
db.username=root
db.password=tu_password
```

### 3. Compilar el Proyecto
```bash
mvn clean install
```

### 4. Ejecutar el Servidor
```bash
cd vista/server-vista
mvn exec:java
```

El servidor estará disponible en:
- **WebSocket**: `ws://localhost:22100`
- **P2P**: `localhost:22200`

## 📁 Estructura del Proyecto

```
ChatProject-Server/
├── comunes/                    # Módulos compartidos
│   ├── server-app/            # Aplicación principal
│   ├── Server-DTO/            # Data Transfer Objects
│   ├── server-Utils/          # Utilidades
│   └── server-events/         # Eventos del sistema
├── datos/                      # Capa de persistencia
│   ├── server-dominio/        # Entidades JPA
│   └── server-persistencia/   # Repositorios
├── negocio/                    # Lógica de negocio
│   ├── server-logicaFachada/  # Fachada principal
│   ├── server-logicaUsuarios/ # Gestión de usuarios
│   ├── server-LogicaCanales/  # Gestión de canales
│   ├── server-LogicaMensajes/ # Gestión de mensajes
│   └── server-LogicaPeers/    # Gestión P2P
├── transporte/                 # Capa de comunicación
│   ├── server-Transporte/     # WebSocket handlers
│   └── server-controladorTransporte/ # Controladores
├── vista/                      # Interfaz de usuario
│   ├── server-vista/          # UI del servidor
│   └── server-controladorVista/ # Controladores UI
├── config/                     # Archivos de configuración
│   ├── database.properties
│   ├── server.properties
│   ├── p2p.properties
│   └── mail.properties
├── storage/                    # Almacenamiento de archivos
│   ├── audio_files/
│   └── user_photos/
└── scripts/                    # Scripts SQL
```

## 🔧 Configuración

### Servidor Principal (`config/server.properties`)
```properties
server.port=22100
server.max.connections=100
```

### Red P2P (`config/p2p.properties`)
```properties
p2p.enabled=true
p2p.puerto=22100
p2p.nombre.servidor=Servidor-Principal
p2p.heartbeat.interval=30000
p2p.heartbeat.timeout=90000
```

### Base de Datos (`config/database.properties`)
```properties
db.url=jdbc:mysql://localhost:3306/chat_db
db.username=root
db.password=root1234
hibernate.hbm2ddl.auto=update
```

## 📡 API del Servidor

### Formato de Comunicación

**Request (Cliente → Servidor):**
```json
{
  "action": "nombreDeLaAccion",
  "payload": {
    "campo1": "valor1",
    "campo2": "valor2"
  }
}
```

**Response (Servidor → Cliente):**
```json
{
  "action": "nombreDeLaAccion",
  "status": "success",
  "message": "Mensaje descriptivo",
  "data": {
    "resultado": "..."
  }
}
```

### Acciones Principales

#### Autenticación
```javascript
// Login
{
  "action": "authenticateUser",
  "payload": {
    "nombreUsuario": "juan",
    "password": "123456"
  }
}

// Registro
{
  "action": "registerUser",
  "payload": {
    "username": "maria",
    "email": "maria@example.com",
    "password": "password123"
  }
}
```

#### Mensajería
```javascript
// Enviar mensaje de texto
{
  "action": "enviarMensajeCanal",
  "payload": {
    "canalId": "uuid-del-canal",
    "contenido": "Hola a todos!"
  }
}

// Enviar mensaje directo
{
  "action": "enviarMensajeDirecto",
  "payload": {
    "remitenteId": "uuid-remitente",
    "destinatarioId": "uuid-destinatario",
    "tipo": "texto",
    "contenido": "Hola!"
  }
}
```

#### Canales
```javascript
// Listar canales
{
  "action": "listarCanales",
  "payload": {
    "usuarioId": "uuid-del-usuario"
  }
}

// Crear canal directo
{
  "action": "crearCanalDirecto",
  "payload": {
    "user1Id": "uuid-usuario-1",
    "user2Id": "uuid-usuario-2"
  }
}
```

### Notificaciones Push

El servidor envía notificaciones push automáticamente:

```javascript
// Nuevo mensaje
{
  "action": "push_newMessage",
  "status": true,
  "message": "Nuevo mensaje recibido",
  "data": {
    "messageId": "uuid",
    "channelId": "uuid",
    "author": { "userId": "uuid", "username": "juan" },
    "content": "Hola!",
    "messageType": "TEXT",
    "timestamp": "2025-11-07T10:30:00Z"
  }
}

// Invitación a canal
{
  "action": "push_userInvited",
  "status": true,
  "message": "Has sido invitado a un canal",
  "data": {
    "channelId": "uuid",
    "channelName": "Canal General",
    "owner": { "userId": "uuid", "username": "admin" }
  }
}
```

## 🌐 Sistema P2P

### Arquitectura Distribuida

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Servidor A │◄───►│  Servidor B │◄───►│  Servidor C │
│  (Puerto    │     │  (Puerto    │     │  (Puerto    │
│   22100)    │     │   22100)    │     │   22100)    │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
   ┌───┴───┐           ┌───┴───┐           ┌───┴───┐
   │Clientes│          │Clientes│          │Clientes│
   │ 1-50  │          │ 51-100│          │101-150│
   └───────┘           └───────┘           └───────┘
```

### Agregar Peer
```javascript
{
  "action": "añadirPeer",
  "payload": {
    "ip": "192.168.1.10",
    "puerto": 22100,
    "nombreServidor": "Servidor-B"
  }
}
```

### Heartbeat
Los peers envían heartbeats cada 30 segundos para mantener la conexión activa.

## 📚 Documentación

- **[DOCUMENTACION_API.md](DOCUMENTACION_API.md)**: Documentación completa de todas las rutas API
- **[ARQUITECTURA_SERVIDOR.md](ARQUITECTURA_SERVIDOR.md)**: Detalles técnicos de la arquitectura
- **[GUIA_INTEGRACION_CLIENTE.md](GUIA_INTEGRACION_CLIENTE.md)**: Guía para integrar clientes

## 🧪 Testing

### Ejecutar Tests
```bash
mvn test
```

### Test de Conexión
```bash
# Usando wscat (npm install -g wscat)
wscat -c ws://localhost:22100

# Enviar mensaje de prueba
{"action":"authenticateUser","payload":{"nombreUsuario":"test","password":"test123"}}
```

## 🔒 Seguridad

- **Autenticación requerida**: Todas las operaciones (excepto login/registro) requieren autenticación
- **Validación de permisos**: Verificación de membresía en canales
- **Validación de datos**: Validación de entrada en todos los endpoints
- **Sesiones**: Gestión de sesiones por conexión WebSocket

## 📊 Monitoreo

### Logs
Los logs se imprimen en consola con formato:
```
→ [Controller] Procesando acción...
✓ [Controller] Operación exitosa
✗ [Controller] Error: mensaje de error
```

### Métricas
- Clientes conectados
- Peers activos
- Mensajes por segundo
- Latencia de heartbeat

## 🚀 Despliegue

### Producción

1. **Configurar variables de entorno**
```bash
export DB_PASSWORD=secure_password
export MAIL_PASSWORD=mail_password
```

2. **Compilar para producción**
```bash
mvn clean package -DskipTests
```

3. **Ejecutar**
```bash
java -jar vista/server-vista/target/server-vista-1.0-SNAPSHOT.jar
```

### Docker (Futuro)
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/*.jar app.jar
EXPOSE 22100 22200
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📝 Licencia

Este proyecto es parte del sistema Chat Unillanos.

## 👥 Equipo

Desarrollado por el equipo de Chat Unillanos.

## 📞 Soporte

Para reportar problemas o sugerencias:
- Crear un issue en el repositorio
- Contactar al equipo de desarrollo

---

## 🎯 Roadmap

- [ ] Implementar encriptación end-to-end
- [ ] Agregar soporte para videollamadas
- [ ] Implementar sistema de roles y permisos
- [ ] Agregar soporte para reacciones a mensajes
- [ ] Implementar búsqueda de mensajes
- [ ] Agregar soporte para mensajes programados
- [ ] Implementar sistema de moderación
- [ ] Agregar analytics y métricas avanzadas

---

**¡Gracias por usar Chat Unillanos Server!** 🎉
