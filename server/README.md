# Chat-Unillanos Server

Servidor de mensajería en tiempo real construido con arquitectura modular de 4 capas, utilizando Java 21, Spring Boot, Netty y MySQL.

## 📋 Tabla de Contenidos

- [Características](#características)
- [Arquitectura](#arquitectura)
- [Requisitos Previos](#requisitos-previos)
- [Instalación y Ejecución](#instalación-y-ejecución)
- [Protocolo de Comunicación](#protocolo-de-comunicación)
- [API del Servidor](#api-del-servidor)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Tecnologías Utilizadas](#tecnologías-utilizadas)
- [Comandos Útiles](#comandos-útiles)

## ✨ Características

- 🔐 **Autenticación segura** con BCrypt
- 💬 **Mensajería en tiempo real** (directa y por canales)
- 👥 **Gestión de canales** con roles (admin/member)
- 📁 **Compartir archivos** con deduplicación por hash SHA-256
- 🖥️ **Interfaz de administración** JavaFX
- 📊 **Sistema de logging** y auditoría
- ⚡ **Alta concurrencia** con hilos virtuales de Java 21
- 🔄 **Comunicación asíncrona** TCP/IP con Netty

## 🏗️ Arquitectura

El proyecto sigue una **arquitectura de 4 capas** con separación estricta de responsabilidades:

```
┌─────────────────────────────────────────┐
│      Capa de Presentación               │
│  ┌──────────┐      ┌──────────┐        │
│  │   Main   │──────│   GUI    │        │
│  └──────────┘      └──────────┘        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      Capa de Lógica de Negocio          │
│  ┌──────────┐      ┌──────────┐        │
│  │Servicios │──────│Validadores│        │
│  └──────────┘      └──────────┘        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         Capa de Datos                   │
│       ┌──────────────┐                  │
│       │ Repositorios │                  │
│       └──────────────┘                  │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      Capa de Infraestructura            │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐  │
│  │ Netty│ │ DTOs │ │Config│ │ Logs │  │
│  └──────┘ └──────┘ └──────┘ └──────┘  │
└────────────────┬────────────────────────┘
                 │
            ┌────▼────┐
            │  MySQL  │
            └─────────┘
```

## 📦 Requisitos Previos

- **Java 21** (LTS) - [Descargar](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.6+** - [Descargar](https://maven.apache.org/download.cgi)
- **Docker** y **Docker Compose** - [Descargar](https://www.docker.com/products/docker-desktop)

### Verificar Instalación

```bash
java -version   # Debe mostrar openjdk version "21.x.x"
mvn -version    # Debe mostrar Java version: 21.x.x
docker --version
```

## 🚀 Instalación y Ejecución

### Opción 1: Script Automatizado (Windows)

**Modo Desarrollo (compila y ejecuta):**
```cmd
start-server.bat
```

Este script:
1. Levanta MySQL con Docker
2. Espera a que esté healthy
3. Compila el proyecto
4. Ejecuta el servidor y la GUI

**Modo Producción (JAR ejecutable):**
```cmd
# 1. Generar JAR
build-jar.bat

# 2. Ejecutar JAR
run-jar.bat
```

### Opción 2: Manual

**1. Levantar la Base de Datos**

```bash
docker-compose up -d
```

**2. Compilar el Proyecto**

```bash
mvn clean install
```

**3. Ejecutar el Servidor**

```bash
mvn -pl Presentacion/Main spring-boot:run
```

El servidor se iniciará en:
- **Puerto TCP**: 8080 (configurable)
- **GUI de Administración**: Se abre automáticamente

## 📡 Protocolo de Comunicación

### Conexión al Servidor

El servidor utiliza **TCP/IP** con mensajes **JSON** delimitados por salto de línea (`\n`).

**Ejemplo de conexión (Java):**

```java
Socket socket = new Socket("localhost", 8080);
BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
```

**Ejemplo de conexión (Python):**

```python
import socket
import json

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('localhost', 8080))
```

### Formato de Mensajes

#### Petición (Cliente → Servidor)

```json
{
  "action": "nombre_accion",
  "payload": {
    // Datos específicos de la acción
  }
}
```

**Importante:** Cada mensaje debe terminar con `\n`

#### Respuesta (Servidor → Cliente)

```json
{
  "action": "nombre_accion",
  "status": "success",
  "message": "Operación exitosa",
  "data": {
    // Datos de respuesta
  }
}
```

**Campos:**
- `action`: Acción que se ejecutó
- `status`: `"success"` o `"error"`
- `message`: Mensaje descriptivo del resultado
- `data`: Datos de respuesta (puede ser `null`)

## 🔌 API del Servidor

### Formato General de Peticiones

**Todas las peticiones deben seguir este formato:**
```json
{
  "action": "nombre_de_la_accion",
  "payload": {
    // Datos específicos según la acción
  }
}
```

**Importante:** Cada mensaje debe terminar con un salto de línea (`\n`)

### 1. Autenticación

#### Registro de Usuario

**Petición:**
```json
{
  "action": "registro",
  "payload": {
    "nombre": "Juan Pérez",
    "email": "juan@example.com",
    "password": "MiPassword123!"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "registro",
  "status": "success",
  "message": "Usuario registrado exitosamente",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Pérez",
    "email": "juan@example.com",
    "estado": "OFFLINE",
    "fechaRegistro": "2025-10-14T02:30:00"
  }
}
```

**Respuesta de error (email duplicado):**
```json
{
  "action": "registro",
  "status": "error",
  "message": "El email ya está registrado",
  "data": null
}
```

#### Login

**Petición:**
```json
{
  "action": "login",
  "payload": {
    "email": "juan@example.com",
    "password": "MiPassword123!"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "login",
  "status": "success",
  "message": "Autenticación exitosa",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Pérez",
    "email": "juan@example.com",
    "estado": "ONLINE",
    "photoId": null
  }
}
```

**Respuesta de error (credenciales inválidas):**
```json
{
  "action": "login",
  "status": "error",
  "message": "Credenciales inválidas",
  "data": null
}
```

#### Logout

**Petición:**
```json
{
  "action": "logout",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "logout",
  "status": "success",
  "message": "Sesión cerrada exitosamente",
  "data": null
}
```

#### Actualizar Perfil

**Petición:**
```json
{
  "action": "actualizar_perfil",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Carlos Pérez",
    "photoId": "nueva-foto-id"
  }
}
```

### 2. Gestión de Canales

#### Crear Canal

**Petición:**
```json
{
  "action": "crear_canal",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Canal General",
    "descripcion": "Canal para conversaciones generales"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "crear_canal",
  "status": "success",
  "message": "Canal creado exitosamente",
  "data": {
    "id": "660e8400-e29b-41d4-a716-446655440000",
    "nombre": "Canal General",
    "descripcion": "Canal para conversaciones generales",
    "creadorId": "550e8400-e29b-41d4-a716-446655440000",
    "fechaCreacion": "2025-10-14T02:35:00",
    "activo": true,
    "cantidadMiembros": 1
  }
}
```

**Respuesta de error (nombre duplicado):**
```json
{
  "action": "crear_canal",
  "status": "error",
  "message": "Ya existe un canal con ese nombre",
  "data": null
}
```

#### Unirse a Canal

**Petición:**
```json
{
  "action": "unirse_canal",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "canalId": "660e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "unirse_canal",
  "status": "success",
  "message": "Te has unido al canal exitosamente",
  "data": {
    "canalId": "660e8400-e29b-41d4-a716-446655440000",
    "nombreCanal": "Canal General",
    "rol": "MEMBER",
    "fechaUnion": "2025-10-14T02:40:00"
  }
}
```

#### Salir de Canal

**Petición:**
```json
{
  "action": "salir_canal",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "canalId": "660e8400-e29b-41d4-a716-446655440000"
  }
}
```

#### Listar Canales del Usuario

**Petición:**
```json
{
  "action": "listar_canales",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "listar_canales",
  "status": "success",
  "message": "Canales obtenidos exitosamente",
  "data": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440000",
      "nombre": "Canal General",
      "descripcion": "Canal para conversaciones generales",
      "activo": true,
      "cantidadMiembros": 15,
      "rol": "ADMIN"
    },
    {
      "id": "770e8400-e29b-41d4-a716-446655440000",
      "nombre": "Canal de Proyectos",
      "descripcion": "Discusión de proyectos",
      "activo": true,
      "cantidadMiembros": 8,
      "rol": "MEMBER"
    }
  ]
}
```

#### Gestionar Miembro (Solo Admin)

**Petición:**
```json
{
  "action": "gestionar_miembro",
  "payload": {
    "adminId": "550e8400-e29b-41d4-a716-446655440000",
    "canalId": "660e8400-e29b-41d4-a716-446655440000",
    "usuarioId": "880e8400-e29b-41d4-a716-446655440000",
    "accion": "REMOVER"
  }
}
```

**Acciones disponibles:** `"AGREGAR"`, `"REMOVER"`, `"PROMOVER_ADMIN"`, `"QUITAR_ADMIN"`

### 3. Mensajería

#### Enviar Mensaje Directo

**Petición:**
```json
{
  "action": "enviar_mensaje",
  "payload": {
    "remitenteId": "550e8400-e29b-41d4-a716-446655440000",
    "destinatarioId": "770e8400-e29b-41d4-a716-446655440000",
    "contenido": "Hola, ¿cómo estás?",
    "tipo": "DIRECT"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "enviar_mensaje",
  "status": "success",
  "message": "Mensaje enviado exitosamente",
  "data": {
    "id": 1,
    "remitenteId": "550e8400-e29b-41d4-a716-446655440000",
    "destinatarioId": "770e8400-e29b-41d4-a716-446655440000",
    "tipo": "DIRECT",
    "contenido": "Hola, ¿cómo estás?",
    "fechaEnvio": "2025-10-14T02:40:00"
  }
}
```

#### Enviar Mensaje a Canal

**Petición:**
```json
{
  "action": "enviar_mensaje",
  "payload": {
    "remitenteId": "550e8400-e29b-41d4-a716-446655440000",
    "canalId": "660e8400-e29b-41d4-a716-446655440000",
    "contenido": "Hola a todos!",
    "tipo": "CHANNEL"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "enviar_mensaje",
  "status": "success",
  "message": "Mensaje enviado exitosamente",
  "data": {
    "id": 2,
    "remitenteId": "550e8400-e29b-41d4-a716-446655440000",
    "canalId": "660e8400-e29b-41d4-a716-446655440000",
    "tipo": "CHANNEL",
    "contenido": "Hola a todos!",
    "fechaEnvio": "2025-10-14T02:40:00"
  }
}
```

#### Obtener Historial de Mensajes

**Para mensajes directos:**
```json
{
  "action": "obtener_historial",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "destinatarioId": "770e8400-e29b-41d4-a716-446655440000",
    "tipo": "DIRECT",
    "limite": 50
  }
}
```

**Para mensajes de canal:**
```json
{
  "action": "obtener_historial",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "canalId": "660e8400-e29b-41d4-a716-446655440000",
    "tipo": "CHANNEL",
    "limite": 50
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "obtener_historial",
  "status": "success",
  "message": "Historial obtenido exitosamente",
  "data": [
    {
      "id": 1,
      "remitenteId": "550e8400-e29b-41d4-a716-446655440000",
      "remitenteNombre": "Juan Pérez",
      "contenido": "Hola a todos!",
      "tipo": "CHANNEL",
      "fechaEnvio": "2025-10-14T02:40:00",
      "fileId": null
    },
    {
      "id": 2,
      "remitenteId": "770e8400-e29b-41d4-a716-446655440000",
      "remitenteNombre": "María García",
      "contenido": "¡Hola Juan!",
      "tipo": "CHANNEL",
      "fechaEnvio": "2025-10-14T02:41:00",
      "fileId": null
    }
  ]
}
```

#### Marcar Mensaje como Leído

**Petición:**
```json
{
  "action": "marcar_mensaje_leido",
  "payload": {
    "mensajeId": "1",
    "usuarioId": "770e8400-e29b-41d4-a716-446655440000"
  }
}
```

### 4. Gestión de Archivos

#### Subir Archivo

**Petición:**
```json
{
  "action": "subir_archivo",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "nombreOriginal": "documento.pdf",
    "contenidoBase64": "JVBERi0xLjQKJeLjz9MKMSAwIG9iago8PC...",
    "tipoArchivo": "DOCUMENT"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "subir_archivo",
  "status": "success",
  "message": "Archivo subido exitosamente",
  "data": {
    "id": "880e8400-e29b-41d4-a716-446655440000",
    "nombreOriginal": "documento.pdf",
    "hashSha256": "a1b2c3d4e5f6...",
    "tamanoBytes": 102400,
    "tipo": "DOCUMENT",
    "fechaSubida": "2025-10-14T02:45:00"
  }
}
```

**Tipos de archivo soportados:**
- `"IMAGE"` - Imágenes (JPG, PNG, GIF, WebP)
- `"VIDEO"` - Videos (MP4, AVI, MOV)
- `"AUDIO"` - Audios (MP3, WAV, OGG)
- `"DOCUMENT"` - Documentos (PDF, DOC, DOCX, TXT)

#### Descargar Archivo

**Petición:**
```json
{
  "action": "descargar_archivo",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "archivoId": "880e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "descargar_archivo",
  "status": "success",
  "message": "Archivo obtenido exitosamente",
  "data": {
    "id": "880e8400-e29b-41d4-a716-446655440000",
    "nombreOriginal": "documento.pdf",
    "contenidoBase64": "JVBERi0xLjQKJeLjz9MKMSAwIG9iago8PC...",
    "tipoArchivo": "DOCUMENT",
    "tamanoBytes": 102400
  }
}
```

#### Listar Archivos del Usuario

**Petición:**
```json
{
  "action": "listar_archivos",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "tipoArchivo": "DOCUMENT"
  }
}
```

**Respuesta exitosa:**
```json
{
  "action": "listar_archivos",
  "status": "success",
  "message": "Archivos obtenidos exitosamente",
  "data": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440000",
      "nombreOriginal": "documento.pdf",
      "tipoArchivo": "DOCUMENT",
      "tamanoBytes": 102400,
      "fechaSubida": "2025-10-14T02:45:00"
    },
    {
      "id": "990e8400-e29b-41d4-a716-446655440000",
      "nombreOriginal": "imagen.jpg",
      "tipoArchivo": "IMAGE",
      "tamanoBytes": 204800,
      "fechaSubida": "2025-10-14T02:50:00"
    }
  ]
}
```

### 5. Notificaciones en Tiempo Real

El servidor envía notificaciones automáticas a los clientes conectados:

#### Notificación de Nuevo Mensaje

```json
{
  "action": "notificacion_mensaje",
  "status": "success",
  "message": "Nuevo mensaje recibido",
  "data": {
    "id": 1,
    "remitenteId": "770e8400-e29b-41d4-a716-446655440000",
    "remitenteNombre": "María García",
    "contenido": "Hola!",
    "tipo": "DIRECT",
    "fechaEnvio": "2025-10-14T02:50:00"
  }
}
```

#### Notificación de Usuario Conectado

```json
{
  "action": "notificacion_usuario_online",
  "status": "success",
  "message": "Usuario conectado",
  "data": {
    "usuarioId": "770e8400-e29b-41d4-a716-446655440000",
    "nombre": "María García",
    "estado": "ONLINE"
  }
}
```

## 💻 Ejemplos de Cliente

### Java

```java
import java.io.*;
import java.net.*;
import com.google.gson.Gson;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson = new Gson();
    
    public void conectar(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }
    
    public String enviarPeticion(String action, Object payload) throws IOException {
        // Crear DTORequest
        DTORequest request = new DTORequest();
        request.setAction(action);
        request.setPayload(payload);
        
        // Serializar y enviar
        String json = gson.toJson(request);
        out.println(json);
        
        // Leer respuesta
        return in.readLine();
    }
    
    public void login(String email, String password) throws IOException {
        DTOLogin loginData = new DTOLogin();
        loginData.setEmail(email);
        loginData.setPassword(password);
        
        String response = enviarPeticion("login", loginData);
        System.out.println("Respuesta login: " + response);
    }
    
    public void enviarMensaje(String remitenteId, String destinatarioId, String contenido) throws IOException {
        DTOEnviarMensaje mensaje = new DTOEnviarMensaje();
        mensaje.setRemitenteId(remitenteId);
        mensaje.setDestinatarioId(destinatarioId);
        mensaje.setContenido(contenido);
        mensaje.setTipo("DIRECT");
        
        String response = enviarPeticion("enviar_mensaje", mensaje);
        System.out.println("Respuesta mensaje: " + response);
    }
    
    public void cerrar() throws IOException {
        if (socket != null) socket.close();
    }
}

// Uso
public class Main {
    public static void main(String[] args) {
        try {
            ChatClient client = new ChatClient();
            client.conectar("localhost", 8080);
            
            // Login
            client.login("juan@example.com", "MiPassword123!");
            
            // Enviar mensaje
            client.enviarMensaje(
                "550e8400-e29b-41d4-a716-446655440000",
                "770e8400-e29b-41d4-a716-446655440000",
                "Hola desde Java!"
            );
            
            client.cerrar();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### Python

```python
import socket
import json
import threading

class ChatClient:
    def __init__(self, host='localhost', port=8080):
        self.host = host
        self.port = port
        self.socket = None
        self.connected = False
    
    def conectar(self):
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.host, self.port))
            self.connected = True
            print(f"Conectado al servidor {self.host}:{self.port}")
            
            # Iniciar hilo para recibir mensajes
            threading.Thread(target=self.recibir_mensajes, daemon=True).start()
            
        except Exception as e:
            print(f"Error al conectar: {e}")
    
    def enviar_peticion(self, action, payload):
        if not self.connected:
            print("No conectado al servidor")
            return None
        
        try:
            request = {
                "action": action,
                "payload": payload
            }
            
            json_data = json.dumps(request) + '\n'
            self.socket.send(json_data.encode('utf-8'))
            print(f"Enviado: {action}")
            
        except Exception as e:
            print(f"Error al enviar petición: {e}")
    
    def login(self, email, password):
        payload = {
            "email": email,
            "password": password
        }
        self.enviar_peticion("login", payload)
    
    def registrar_usuario(self, nombre, email, password):
        payload = {
            "nombre": nombre,
            "email": email,
            "password": password
        }
        self.enviar_peticion("registro", payload)
    
    def enviar_mensaje(self, remitente_id, destinatario_id, contenido):
        payload = {
            "remitenteId": remitente_id,
            "destinatarioId": destinatario_id,
            "contenido": contenido,
            "tipo": "DIRECT"
        }
        self.enviar_peticion("enviar_mensaje", payload)
    
    def crear_canal(self, usuario_id, nombre, descripcion):
        payload = {
            "usuarioId": usuario_id,
            "nombre": nombre,
            "descripcion": descripcion
        }
        self.enviar_peticion("crear_canal", payload)
    
    def recibir_mensajes(self):
        try:
            while self.connected:
                data = self.socket.recv(1024).decode('utf-8')
                if data:
                    # Procesar mensajes delimitados por \n
                    for line in data.strip().split('\n'):
                        if line:
                            response = json.loads(line)
                            self.procesar_respuesta(response)
        except Exception as e:
            print(f"Error al recibir mensajes: {e}")
            self.connected = False
    
    def procesar_respuesta(self, response):
        action = response.get('action', 'unknown')
        status = response.get('status', 'unknown')
        message = response.get('message', '')
        data = response.get('data')
        
        print(f"\n--- Respuesta ---")
        print(f"Acción: {action}")
        print(f"Estado: {status}")
        print(f"Mensaje: {message}")
        if data:
            print(f"Datos: {json.dumps(data, indent=2, ensure_ascii=False)}")
        print("----------------\n")
    
    def cerrar(self):
        self.connected = False
        if self.socket:
            self.socket.close()
        print("Conexión cerrada")

# Uso
if __name__ == "__main__":
    client = ChatClient()
    client.conectar()
    
    # Registrar usuario
    client.registrar_usuario("Juan Pérez", "juan@example.com", "MiPassword123!")
    
    # Login
    client.login("juan@example.com", "MiPassword123!")
    
    # Crear canal
    client.crear_canal(
        "550e8400-e29b-41d4-a716-446655440000",
        "Canal Python",
        "Canal creado desde Python"
    )
    
    # Mantener conexión activa
    try:
        while True:
            pass
    except KeyboardInterrupt:
        client.cerrar()
```

### JavaScript/Node.js

```javascript
const net = require('net');

class ChatClient {
    constructor(host = 'localhost', port = 8080) {
        this.host = host;
        this.port = port;
        this.socket = null;
        this.connected = false;
        this.buffer = '';
    }
    
    conectar() {
        return new Promise((resolve, reject) => {
            this.socket = new net.Socket();
            
            this.socket.connect(this.port, this.host, () => {
                this.connected = true;
                console.log(`Conectado al servidor ${this.host}:${this.port}`);
                resolve();
            });
            
            this.socket.on('data', (data) => {
                this.buffer += data.toString();
                this.procesarMensajes();
            });
            
            this.socket.on('close', () => {
                this.connected = false;
                console.log('Conexión cerrada');
            });
            
            this.socket.on('error', (err) => {
                reject(err);
            });
        });
    }
    
    procesarMensajes() {
        const lines = this.buffer.split('\n');
        this.buffer = lines.pop(); // Mantener línea incompleta
        
        lines.forEach(line => {
            if (line.trim()) {
                try {
                    const response = JSON.parse(line);
                    this.procesarRespuesta(response);
                } catch (e) {
                    console.error('Error al parsear JSON:', e);
                }
            }
        });
    }
    
    enviarPeticion(action, payload) {
        if (!this.connected) {
            console.log('No conectado al servidor');
            return;
        }
        
        const request = {
            action: action,
            payload: payload
        };
        
        const jsonData = JSON.stringify(request) + '\n';
        this.socket.write(jsonData);
        console.log(`Enviado: ${action}`);
    }
    
    login(email, password) {
        this.enviarPeticion('login', {
            email: email,
            password: password
        });
    }
    
    registrarUsuario(nombre, email, password) {
        this.enviarPeticion('registro', {
            nombre: nombre,
            email: email,
            password: password
        });
    }
    
    enviarMensaje(remitenteId, destinatarioId, contenido) {
        this.enviarPeticion('enviar_mensaje', {
            remitenteId: remitenteId,
            destinatarioId: destinatarioId,
            contenido: contenido,
            tipo: 'DIRECT'
        });
    }
    
    subirArchivo(usuarioId, nombreOriginal, contenidoBase64, tipoArchivo) {
        this.enviarPeticion('subir_archivo', {
            usuarioId: usuarioId,
            nombreOriginal: nombreOriginal,
            contenidoBase64: contenidoBase64,
            tipoArchivo: tipoArchivo
        });
    }
    
    procesarRespuesta(response) {
        const { action, status, message, data } = response;
        
        console.log('\n--- Respuesta ---');
        console.log(`Acción: ${action}`);
        console.log(`Estado: ${status}`);
        console.log(`Mensaje: ${message}`);
        if (data) {
            console.log(`Datos: ${JSON.stringify(data, null, 2)}`);
        }
        console.log('----------------\n');
    }
    
    cerrar() {
        this.connected = false;
        if (this.socket) {
            this.socket.end();
        }
    }
}

// Uso
async function main() {
    const client = new ChatClient();
    
    try {
        await client.conectar();
        
        // Registrar usuario
        client.registrarUsuario('Juan Pérez', 'juan@example.com', 'MiPassword123!');
        
        // Esperar un poco y hacer login
        setTimeout(() => {
            client.login('juan@example.com', 'MiPassword123!');
        }, 1000);
        
        // Enviar mensaje después del login
        setTimeout(() => {
            client.enviarMensaje(
                '550e8400-e29b-41d4-a716-446655440000',
                '770e8400-e29b-41d4-a716-446655440000',
                'Hola desde Node.js!'
            );
        }, 2000);
        
        // Mantener conexión
        process.on('SIGINT', () => {
            client.cerrar();
            process.exit();
        });
        
    } catch (error) {
        console.error('Error:', error);
    }
}

main();
```

### C#

```csharp
using System;
using System.IO;
using System.Net.Sockets;
using System.Text;
using Newtonsoft.Json;

public class ChatClient
{
    private TcpClient client;
    private NetworkStream stream;
    private StreamReader reader;
    private StreamWriter writer;
    private bool connected = false;
    
    public async Task ConectarAsync(string host = "localhost", int port = 8080)
    {
        try
        {
            client = new TcpClient();
            await client.ConnectAsync(host, port);
            stream = client.GetStream();
            reader = new StreamReader(stream, Encoding.UTF8);
            writer = new StreamWriter(stream, Encoding.UTF8) { AutoFlush = true };
            connected = true;
            
            Console.WriteLine($"Conectado al servidor {host}:{port}");
            
            // Iniciar tarea para recibir mensajes
            _ = Task.Run(RecibirMensajesAsync);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error al conectar: {ex.Message}");
        }
    }
    
    public async Task EnviarPeticionAsync(string action, object payload)
    {
        if (!connected)
        {
            Console.WriteLine("No conectado al servidor");
            return;
        }
        
        try
        {
            var request = new
            {
                action = action,
                payload = payload
            };
            
            string json = JsonConvert.SerializeObject(request);
            await writer.WriteLineAsync(json);
            Console.WriteLine($"Enviado: {action}");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error al enviar petición: {ex.Message}");
        }
    }
    
    public async Task LoginAsync(string email, string password)
    {
        var payload = new
        {
            email = email,
            password = password
        };
        await EnviarPeticionAsync("login", payload);
    }
    
    public async Task RegistrarUsuarioAsync(string nombre, string email, string password)
    {
        var payload = new
        {
            nombre = nombre,
            email = email,
            password = password
        };
        await EnviarPeticionAsync("registro", payload);
    }
    
    public async Task EnviarMensajeAsync(string remitenteId, string destinatarioId, string contenido)
    {
        var payload = new
        {
            remitenteId = remitenteId,
            destinatarioId = destinatarioId,
            contenido = contenido,
            tipo = "DIRECT"
        };
        await EnviarPeticionAsync("enviar_mensaje", payload);
    }
    
    private async Task RecibirMensajesAsync()
    {
        try
        {
            while (connected)
            {
                string line = await reader.ReadLineAsync();
                if (line != null)
                {
                    ProcessarRespuesta(line);
                }
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error al recibir mensajes: {ex.Message}");
            connected = false;
        }
    }
    
    private void ProcessarRespuesta(string json)
    {
        try
        {
            dynamic response = JsonConvert.DeserializeObject(json);
            string action = response.action;
            string status = response.status;
            string message = response.message;
            object data = response.data;
            
            Console.WriteLine("\n--- Respuesta ---");
            Console.WriteLine($"Acción: {action}");
            Console.WriteLine($"Estado: {status}");
            Console.WriteLine($"Mensaje: {message}");
            if (data != null)
            {
                Console.WriteLine($"Datos: {JsonConvert.SerializeObject(data, Formatting.Indented)}");
            }
            Console.WriteLine("----------------\n");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error al procesar respuesta: {ex.Message}");
        }
    }
    
    public void Cerrar()
    {
        connected = false;
        reader?.Close();
        writer?.Close();
        stream?.Close();
        client?.Close();
        Console.WriteLine("Conexión cerrada");
    }
}

// Uso
class Program
{
    static async Task Main(string[] args)
    {
        var client = new ChatClient();
        await client.ConectarAsync();
        
        // Registrar usuario
        await client.RegistrarUsuarioAsync("Juan Pérez", "juan@example.com", "MiPassword123!");
        
        // Esperar y hacer login
        await Task.Delay(1000);
        await client.LoginAsync("juan@example.com", "MiPassword123!");
        
        // Enviar mensaje
        await Task.Delay(2000);
        await client.EnviarMensajeAsync(
            "550e8400-e29b-41d4-a716-446655440000",
            "770e8400-e29b-41d4-a716-446655440000",
            "Hola desde C#!"
        );
        
        // Mantener conexión
        Console.WriteLine("Presiona Enter para cerrar...");
        Console.ReadLine();
        client.Cerrar();
    }
}
```

## 🗂️ Estructura del Proyecto

```
ChatServer/
├── pom.xml                    # Proyecto padre Maven
├── docker-compose.yml         # Configuración MySQL
├── init-db.sql               # Esquema de base de datos
├── start-server.bat          # Script de inicio (Windows)
│
├── Presentacion/
│   ├── Main/                 # Punto de entrada (ServerApplication)
│   └── GUI/                  # Interfaz JavaFX de administración
│
├── LogicaNegocio/
│   ├── Servicios/            # Lógica de negocio y ActionDispatcher
│   └── Validadores/          # Validadores de datos
│
├── Datos/
│   └── Repositorios/         # Acceso a datos con JDBC
│
└── Infraestructura/
    ├── DTOs/                 # DTORequest, DTOResponse
    ├── Netty/                # Servidor TCP/IP asíncrono
    ├── Configuracion/        # application.properties
    └── Logs/                 # Sistema de logging
```

## 🛠️ Tecnologías Utilizadas

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 LTS | Lenguaje de programación |
| Spring Boot | 3.3.5 | Framework de inyección de dependencias |
| Netty | 4.1.110 | Servidor TCP/IP asíncrono |
| MySQL | 8.x | Base de datos relacional |
| HikariCP | - | Pool de conexiones |
| Gson | 2.10.1 | Serialización JSON |
| BCrypt | - | Hashing de contraseñas |
| JavaFX | 21 | Interfaz gráfica |
| Maven | 4.0.0 | Gestión de dependencias |

### ⚡ Características de Java 21

Este proyecto aprovecha las siguientes características de Java 21:

- **Hilos Virtuales (Virtual Threads)** - Para manejar miles de conexiones concurrentes con mínimo overhead
- **Pattern Matching** - Para código más limpio y seguro
- **Record Classes** - Para DTOs inmutables
- **Sequenced Collections** - Para manejo mejorado de colecciones ordenadas

## 📦 Generar JAR Ejecutable

Spring Boot genera automáticamente un **JAR ejecutable** con todas las dependencias incluidas (fat JAR).

### Compilar y Generar JAR

```bash
mvn clean package -DskipTests
```

Esto generará el archivo:
```
Presentacion/Main/target/main-1.0.0-SNAPSHOT.jar
```

### Ejecutar el JAR

```bash
java -jar Presentacion/Main/target/main-1.0.0-SNAPSHOT.jar
```

**Requisitos para ejecutar:**
- Java 21 instalado
- MySQL ejecutándose (Docker: `docker-compose up -d`)

### Ventajas del JAR Ejecutable

✅ **Un solo archivo** con todas las dependencias (~29 MB)  
✅ **Portable** - Copia y ejecuta en cualquier máquina con Java 21  
✅ **No requiere Maven** para ejecutar  
✅ **Incluye todas las librerías** (Spring, Netty, JavaFX, MySQL Driver, etc.)  
✅ **Fácil distribución** - Comparte el JAR y listo

### Contenido del JAR

El JAR ejecutable incluye:
- Todos los módulos del proyecto (10 módulos)
- Spring Boot 3.3.5 + dependencias
- Netty 4.1.110
- JavaFX 21
- MySQL Connector
- Gson, BCrypt, HikariCP
- Configuración (`application.properties`)

## 📝 Comandos Útiles

### Maven

```bash
# Compilar todo el proyecto
mvn clean install

# Compilar sin tests
mvn clean install -DskipTests

# Generar JAR ejecutable
mvn clean package -DskipTests

# Ejecutar tests
mvn test

# Ejecutar el servidor (modo desarrollo)
mvn -pl Presentacion/Main spring-boot:run

# Ver árbol de dependencias
mvn dependency:tree
```

### Docker

```bash
# Levantar MySQL
docker-compose up -d

# Ver logs de MySQL
docker-compose logs -f mysql

# Detener MySQL
docker-compose down

# Detener y eliminar volúmenes (resetear BD)
docker-compose down -v
```

## 📊 Esquema de Base de Datos

El servidor utiliza MySQL 8.x con las siguientes tablas:

- **usuarios** - Información de usuarios registrados
- **canales** - Canales de comunicación grupal
- **canal_miembros** - Relación N:M entre usuarios y canales
- **mensajes** - Mensajes directos y de canal (tabla unificada)
- **archivos** - Archivos compartidos con deduplicación por hash
- **logs_sistema** - Registro de eventos del sistema

### Usuario de Prueba

El sistema incluye un usuario de prueba:

- **Email**: `admin@unillanos.edu.co`
- **Contraseña**: `Admin123!`

## 🔧 Configuración

La configuración principal se encuentra en:
```
Infraestructura/Configuracion/src/main/resources/application.properties
```

### Parámetros Principales

```properties
# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/chat_unillanos
spring.datasource.username=chatuser
spring.datasource.password=chatpassword

# Servidor Netty
server.netty.port=8080
server.netty.boss-threads=1
server.netty.worker-threads=4

# HikariCP Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# Archivos
archivos.directorio.base=./uploads
```

## 📈 Estado del Proyecto

✅ **Completado:**
- ✅ Infraestructura base (Netty, ConnectionManager, Logger)
- ✅ Gestión de usuarios (registro, login, perfiles)
- ✅ Gestión de canales (crear, unirse, administrar)
- ✅ Mensajería en tiempo real (directa y por canal)
- ✅ Gestión de archivos (subir, descargar, deduplicación)
- ✅ GUI JavaFX (Dashboard, Usuarios, Canales, Logs)

## ⚠️ Códigos de Error Comunes

### Errores de Validación

| Error | Descripción | Solución |
|-------|-------------|----------|
| `"El email ya está registrado"` | Email duplicado en registro | Usar un email diferente |
| `"Credenciales inválidas"` | Login fallido | Verificar email y contraseña |
| `"Ya existe un canal con ese nombre"` | Nombre de canal duplicado | Elegir otro nombre |
| `"Usuario no encontrado"` | ID de usuario inválido | Verificar que el usuario existe |
| `"No autorizado para esta acción"` | Sin permisos | Verificar roles y permisos |

### Errores de Conexión

| Error | Descripción | Solución |
|-------|-------------|----------|
| `Connection refused` | Servidor no disponible | Verificar que el servidor esté ejecutándose |
| `Connection timeout` | Timeout de conexión | Verificar red y firewall |
| `Connection reset` | Conexión perdida | Reconectar al servidor |

## 🔧 Mejores Prácticas

### 1. Manejo de Conexiones

```java
// ✅ CORRECTO: Siempre cerrar conexiones
try {
    client.conectar();
    // ... operaciones ...
} finally {
    client.cerrar();
}

// ❌ INCORRECTO: No cerrar conexiones
client.conectar();
// ... operaciones ... (sin cerrar)
```

### 2. Manejo de Errores

```java
// ✅ CORRECTO: Manejar errores de red
try {
    String response = client.enviarPeticion("login", loginData);
    DTOResponse dtoResponse = gson.fromJson(response, DTOResponse.class);
    
    if ("error".equals(dtoResponse.getStatus())) {
        System.err.println("Error: " + dtoResponse.getMessage());
        return;
    }
    // Procesar respuesta exitosa...
} catch (IOException e) {
    System.err.println("Error de conexión: " + e.getMessage());
}
```

### 3. Reconexión Automática

```python
# ✅ CORRECTO: Implementar reconexión
class ChatClient:
    def __init__(self):
        self.max_reintentos = 3
        self.delay_reintento = 1000  # ms
    
    def conectar_con_reintentos(self):
        for intento in range(self.max_reintentos):
            try:
                self.conectar()
                return True
            except Exception as e:
                if intento == self.max_reintentos - 1:
                    raise e
                time.sleep(self.delay_reintento / 1000)
        return False
```

### 4. Validación de Datos

```javascript
// ✅ CORRECTO: Validar datos antes de enviar
enviarMensaje(remitenteId, destinatarioId, contenido) {
    // Validaciones
    if (!remitenteId || !destinatarioId || !contenido) {
        console.error('Faltan datos requeridos');
        return;
    }
    
    if (contenido.length > 5000) {
        console.error('Mensaje muy largo');
        return;
    }
    
    // Enviar petición
    this.enviarPeticion('enviar_mensaje', {
        remitenteId,
        destinatarioId,
        contenido,
        tipo: 'DIRECT'
    });
}
```

### 5. Manejo de Archivos

```java
// ✅ CORRECTO: Validar tamaño antes de subir
public void subirArchivo(File archivo) {
    long maxSize = 10 * 1024 * 1024; // 10 MB
    
    if (archivo.length() > maxSize) {
        System.err.println("Archivo muy grande");
        return;
    }
    
    String base64 = Base64.getEncoder().encodeToString(
        Files.readAllBytes(archivo.toPath())
    );
    
    // Subir archivo...
}
```

## 📊 Límites del Sistema

### Tamaños Máximos

| Recurso | Límite | Descripción |
|---------|--------|-------------|
| Mensaje de texto | 5,000 caracteres | Contenido de mensaje |
| Archivo imagen | 10 MB | JPG, PNG, GIF, WebP |
| Archivo video | 50 MB | MP4, AVI, MOV |
| Archivo audio | 20 MB | MP3, WAV, OGG |
| Archivo documento | 10 MB | PDF, DOC, DOCX, TXT |
| Nombre de canal | 100 caracteres | Título del canal |
| Descripción de canal | 500 caracteres | Descripción del canal |

### Límites de Concurrencia

| Recurso | Límite | Descripción |
|---------|--------|-------------|
| Conexiones simultáneas | 1,000 | Clientes TCP conectados |
| Mensajes por minuto | 1,000 | Por usuario |
| Canales por usuario | 50 | Canales donde es miembro |
| Miembros por canal | 500 | Usuarios en un canal |

## 🚀 Optimización de Rendimiento

### 1. Agrupación de Mensajes

```python
# ✅ CORRECTO: Agrupar mensajes para reducir latencia
class ChatClient:
    def __init__(self):
        self.mensaje_buffer = []
        self.buffer_size = 10
        self.buffer_timeout = 100  # ms
    
    def enviar_mensaje_buffered(self, mensaje):
        self.mensaje_buffer.append(mensaje)
        
        if len(self.mensaje_buffer) >= self.buffer_size:
            self.flush_buffer()
    
    def flush_buffer(self):
        if self.mensaje_buffer:
            # Enviar todos los mensajes de una vez
            self.enviar_peticion('enviar_mensajes_batch', {
                'mensajes': self.mensaje_buffer
            })
            self.mensaje_buffer.clear()
```

### 2. Compresión de Datos

```javascript
// ✅ CORRECTO: Comprimir archivos grandes
const zlib = require('zlib');

async function subirArchivoComprimido(archivo) {
    const buffer = await fs.promises.readFile(archivo);
    const comprimido = zlib.gzipSync(buffer);
    const base64 = comprimido.toString('base64');
    
    client.enviarPeticion('subir_archivo', {
        usuarioId: userId,
        nombreOriginal: archivo,
        contenidoBase64: base64,
        tipoArchivo: 'DOCUMENT',
        comprimido: true
    });
}
```

## 📄 Licencia

Este proyecto es parte del curso de Arquitectura de Software - Universidad de los Llanos.

## 👥 Contacto

Para más información, consultar la documentación en el directorio del proyecto.

---

**Desarrollado con ❤️ usando Java 21 y Spring Boot**
