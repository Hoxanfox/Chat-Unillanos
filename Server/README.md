# Servidor Chat-Unillanos

Servidor sencillo de pruebas para el cliente de Chat-Unillanos, construido con **Netty** para manejar conexiones TCP y comunicación JSON.

## 🚀 Características

- ✅ Servidor TCP con Netty
- ✅ Comunicación basada en JSON sobre TCP
- ✅ Framing automático con delimitador de línea (`\n`)
- ✅ Autenticación simple de usuarios
- ✅ Lista de contactos en tiempo real
- ✅ Notificaciones automáticas cuando usuarios se conectan/desconectan
- ✅ Gestión de múltiples sesiones concurrentes

## 📋 Requisitos

- Java 17 o superior
- Maven 3.6 o superior

## 🔧 Compilación

Desde el directorio `Server/`:

```bash
mvn clean package
```

## ▶️ Ejecución

### Opción 1: Usando Maven

```bash
mvn exec:java
```

### Opción 2: Usando Java directamente

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="servidor.Main"
```

### Opción 3: Con puerto personalizado

```bash
mvn exec:java -Dexec.args="9999"
```

Por defecto, el servidor escucha en el puerto **8888**.

## 🔌 Protocolo de Comunicación

### Formato de Mensajes

Todos los mensajes son JSON seguidos de un salto de línea (`\n`).

### Petición del Cliente (DTORequest)

```json
{
  "action": "authenticateUser",
  "payload": {
    "emailUsuario": "usuario@ejemplo.com",
    "passwordUsuario": "password123"
  }
}
```

### Respuesta del Servidor (DTOResponse)

```json
{
  "action": "authenticateUser",
  "status": "success",
  "message": "Autenticación exitosa",
  "data": "usuario"
}
```

## 📡 Acciones Soportadas

### 1. Autenticación de Usuario

**Acción:** `authenticateUser`

**Payload:**
```json
{
  "emailUsuario": "usuario@ejemplo.com",
  "passwordUsuario": "password123"
}
```

**Respuesta exitosa:**
```json
{
  "action": "authenticateUser",
  "status": "success",
  "message": "Autenticación exitosa",
  "data": "usuario"
}
```

**Nota:** Para pruebas, el servidor acepta cualquier email/password no vacío.

### 2. Solicitar Lista de Contactos

**Acción:** `solicitarListaContactos`

**Payload:** `null`

**Respuesta:**
```json
{
  "action": "actualizarListaContactos",
  "status": "success",
  "message": "Lista de contactos actualizada",
  "data": [
    {"nombre": "usuario1", "estado": "Online"},
    {"nombre": "usuario2", "estado": "Online"}
  ]
}
```

### 3. Actualización Automática de Contactos

Cuando un usuario se conecta o desconecta, el servidor envía automáticamente a todos los clientes autenticados:

```json
{
  "action": "actualizarListaContactos",
  "status": "success",
  "message": "Lista de contactos actualizada",
  "data": [...]
}
```

## 🏗️ Arquitectura

```
Server/
├── pom.xml
└── src/main/java/servidor/
    ├── Main.java                   # Punto de entrada
    ├── ServidorNetty.java          # Configuración del servidor Netty
    ├── ManejadorCliente.java       # Procesa mensajes de clientes
    ├── GestorSesiones.java         # Gestiona usuarios conectados
    └── dto/
        ├── DTORequest.java         # Peticiones del cliente
        ├── DTOResponse.java        # Respuestas del servidor
        ├── DTOAutenticacion.java   # Datos de autenticación
        └── DTOContacto.java        # Datos de contacto
```

## 🔍 Logs del Servidor

El servidor muestra logs informativos:

```
╔════════════════════════════════════════════════╗
║   SERVIDOR CHAT-UNILLANOS INICIADO            ║
╚════════════════════════════════════════════════╝
🚀 Escuchando en puerto: 8888
⏳ Esperando conexiones de clientes...

✓ Nuevo cliente conectado. Total de conexiones: 1
📩 Mensaje recibido: {"action":"authenticateUser",...}
✓ Usuario autenticado: usuario1
📤 Respuesta enviada: {"action":"authenticateUser",...}
📢 Actualización de contactos difundida a todos los usuarios
```

## 🧪 Pruebas

1. **Inicia el servidor:**
   ```bash
   mvn exec:java
   ```

2. **Inicia uno o más clientes** (desde el directorio `Cliente/`):
   ```bash
   mvn -f Presentacion/Main/pom.xml javafx:run
   ```

3. **En el cliente:**
   - Conecta al servidor: `127.0.0.1` (puerto 8888)
   - Inicia sesión con cualquier email
   - Observa la lista de usuarios en línea

## ⚠️ Notas Importantes

- Este es un **servidor de pruebas simplificado**
- No hay persistencia de datos (todo en memoria)
- No hay validación real de credenciales
- No hay cifrado de comunicaciones
- Pensado solo para validar la funcionalidad del cliente

## 🔮 Próximas Mejoras

- [ ] Persistencia de usuarios en base de datos
- [ ] Validación real de credenciales
- [ ] Mensajes privados entre usuarios
- [ ] Canales/grupos de chat
- [ ] Historial de mensajes
- [ ] Cifrado TLS/SSL
- [ ] Autenticación con tokens

