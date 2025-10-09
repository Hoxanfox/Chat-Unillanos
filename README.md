# Chat-Unillanos 💬

Aplicación de chat en tiempo real desarrollada con **Java 17**, **JavaFX** y **Netty**.

## 📖 Descripción

Chat-Unillanos es un sistema de mensajería instantánea que permite a múltiples usuarios conectarse, autenticarse y ver quién está en línea en tiempo real. El proyecto está dividido en dos componentes principales:

- **Cliente**: Aplicación de escritorio JavaFX con arquitectura en capas
- **Servidor**: Servidor TCP con Netty para manejar múltiples conexiones concurrentes

## 🏗️ Arquitectura

### Cliente (JavaFX)
```
Cliente/
├── Presentacion/          # UI y Controladores
│   ├── Main/             # Punto de entrada
│   ├── InterfazEscritorio/  # Vistas JavaFX
│   └── Controlador/      # Controladores MVC
├── Negocio/              # Lógica de negocio
│   ├── Servicio/         # Servicios de aplicación
│   ├── Fachada/          # Patrón Fachada
│   ├── Comunicacion/     # Gestión de mensajes cliente-servidor
│   ├── GestionUsuario/   # Lógica de autenticación
│   └── GestionContactos/ # Lógica de contactos (Observer)
├── Persistencia/         # Capa de datos
│   ├── Conexion/         # Gestor de sesiones (Singleton)
│   ├── Transporte/       # TransporteTCP con sockets
│   └── Dominio/          # Entidades de dominio
└── Infraestructura/      # Componentes transversales
    ├── DTO/              # Data Transfer Objects
    ├── Observador/       # Patrón Observer
    └── Logger/           # Sistema de logging
```

### Servidor (Netty)
```
Server/
└── src/main/java/servidor/
    ├── Main.java                # Punto de entrada
    ├── ServidorNetty.java       # Configuración Netty
    ├── ManejadorCliente.java    # Lógica de negocio
    ├── GestorSesiones.java      # Gestión de usuarios (Singleton)
    └── dto/                     # Data Transfer Objects
```

## 🚀 Inicio Rápido

### Requisitos
- Java JDK 17+
- Maven 3.6+

### 1. Iniciar el Servidor

**Windows:**
```cmd
cd Server
iniciar-servidor.bat
```

**Linux/Mac:**
```bash
cd Server
chmod +x iniciar-servidor.sh
./iniciar-servidor.sh
```

**O manualmente:**
```bash
cd Server
mvn clean compile exec:java
```

### 2. Iniciar el Cliente

**Windows:**
```cmd
cd Cliente
iniciar-cliente.bat
```

**Linux/Mac:**
```bash
cd Cliente
chmod +x iniciar-cliente.sh
./iniciar-cliente.sh
```

**O manualmente:**
```bash
cd Cliente
mvn clean compile
cd Presentacion/Main
mvn javafx:run
```

### 3. Conectarse y Probar

1. En el cliente, ingresa: `127.0.0.1`
2. Click en "Conectar"
3. Ingresa cualquier email (ej: `usuario@test.com`)
4. Ingresa cualquier contraseña
5. ¡Listo! Verás la lista de usuarios en línea

**Para probar con múltiples usuarios:** Abre varias instancias del cliente con diferentes emails.

## 📋 Características Implementadas

- ✅ Servidor TCP con Netty
- ✅ Framing de mensajes (delimitador de línea)
- ✅ Comunicación JSON sobre TCP
- ✅ Autenticación de usuarios
- ✅ Lista de contactos en tiempo real
- ✅ Actualización automática cuando usuarios se conectan/desconectan
- ✅ Gestión de múltiples sesiones concurrentes
- ✅ Arquitectura en capas limpia (Cliente)
- ✅ Patrones: MVC, DTO, Singleton, Observer, Fachada
- ✅ Programación asíncrona (CompletableFuture)
- ✅ Multi-threading

## 🎯 Funcionalidades Pendientes

- ⏳ Mensajes privados entre usuarios
- ⏳ Canales/grupos de chat
- ⏳ Historial de mensajes
- ⏳ Persistencia en base de datos
- ⏳ Validación real de credenciales
- ⏳ Cifrado de comunicaciones (TLS/SSL)

## 🛠️ Tecnologías

### Cliente
- Java 17
- JavaFX 17.0.2
- Maven (multi-módulo)
- Gson 2.10.1
- Sockets Java

### Servidor
- Java 17
- Netty 4.1.100.Final
- Maven
- Gson 2.10.1

## 📚 Documentación

- [Instrucciones de Pruebas Detalladas](INSTRUCCIONES_PRUEBAS.md)
- [README del Servidor](Server/README.md)

## 🧪 Pruebas

Ver [INSTRUCCIONES_PRUEBAS.md](INSTRUCCIONES_PRUEBAS.md) para guía completa de pruebas.

### Checklist Rápido
- [ ] El servidor inicia en puerto 8888
- [ ] El cliente se conecta al servidor
- [ ] El usuario puede autenticarse
- [ ] Se muestra la lista de contactos
- [ ] Múltiples usuarios pueden conectarse
- [ ] La lista se actualiza automáticamente

## 🔌 Protocolo de Comunicación

### Formato de Mensajes
Todos los mensajes son JSON terminados en `\n` (Line-Based Framing).

### Petición del Cliente
```json
{
  "action": "authenticateUser",
  "payload": {
    "emailUsuario": "usuario@ejemplo.com",
    "passwordUsuario": "password123"
  }
}
```

### Respuesta del Servidor
```json
{
  "action": "authenticateUser",
  "status": "success",
  "message": "Autenticación exitosa",
  "data": "usuario"
}
```

## 📊 Estado del Proyecto

Este es un **proyecto académico** para la asignatura de Arquitectura de Software. El servidor actual es una **versión de pruebas simplificada** para validar la funcionalidad del cliente.

### Versión Actual: 1.0-SNAPSHOT
- ✅ Cliente completo con UI funcional
- ✅ Servidor básico para pruebas
- ⚠️ Sin persistencia (datos en memoria)
- ⚠️ Sin validación de credenciales

## 👥 Contribuir

Este es un proyecto académico. Las contribuciones están limitadas a los miembros del equipo.

## 📝 Licencia

Proyecto académico - Universidad de los Llanos

---

**Desarrollado para:** Arquitectura de Software - Corte 1  
**Universidad:** Universidad de los Llanos  
**Periodo:** 2025-2
