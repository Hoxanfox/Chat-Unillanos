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
    "id": 1,
    "remitenteId": "550e8400-e29b-41d4-a716-446655440000",
    "canalId": "660e8400-e29b-41d4-a716-446655440000",
    "tipo": "CHANNEL",
    "contenido": "Hola a todos!",
    "fechaEnvio": "2025-10-14T02:40:00"
  }
}
```

#### Obtener Historial de Mensajes

**Petición:**
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

## 📄 Licencia

Este proyecto es parte del curso de Arquitectura de Software - Universidad de los Llanos.

## 👥 Contacto

Para más información, consultar la documentación en el directorio del proyecto.

---

**Desarrollado con ❤️ usando Java 21 y Spring Boot**
