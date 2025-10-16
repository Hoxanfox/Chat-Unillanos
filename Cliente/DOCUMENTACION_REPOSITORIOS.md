  "accion": "OBTENER_CANALES_USUARIO",
  "datos": {}
}
```
- **Proceso**:
  1. Envía solicitud al servidor
  2. Espera respuesta con array de canales
  3. Actualiza caché local
  4. Sincroniza con base de datos local
  5. Retorna lista actualizada

**`obtenerCanalPorId(String canalId)`**
- **Propósito**: Obtiene un canal específico por su ID
- **Retorno**: `CompletableFuture<Canal>`
- **Estrategia**:
  1. Busca primero en caché local (rápido)
  2. Si no existe, consulta base de datos local
  3. Si tampoco existe, consulta al servidor
  4. Actualiza caché con el resultado

**`guardarCanalLocal(Canal canal)`**
- **Propósito**: Persiste un canal en la base de datos H2 local
- **SQL**:
```sql
INSERT INTO canales (id_canal, nombre, descripcion, id_administrador, fecha_creacion)
VALUES (?, ?, ?, ?, ?)
ON CONFLICT (id_canal) DO UPDATE SET nombre = ?, descripcion = ?
```
- **Uso**: Para funcionamiento offline y carga rápida

**`sincronizarCanales(List<Canal> canales)`**
- **Propósito**: Sincroniza una lista de canales con la base de datos local
- **Proceso**:
  1. Inicia transacción
  2. Para cada canal: INSERT or UPDATE
  3. Elimina canales que ya no existen
  4. Commit de transacción
  5. Actualiza caché en memoria

#### Gestión de Miembros

**`invitarMiembro(String canalId, String contactoId)`**
- **Propósito**: Envía invitación a un usuario para unirse al canal
- **Retorno**: `CompletableFuture<Void>`
- **Protocolo**:
```json
{
  "accion": "INVITAR_MIEMBRO_CANAL",
  "datos": {
    "canalId": "uuid-del-canal",
    "contactoId": "uuid-del-contacto"
  }
}
```
- **Validaciones en servidor**:
  - Usuario que invita es administrador
  - Usuario invitado no es ya miembro
  - Usuario invitado está en contactos

**`solicitarMiembros(String canalId)`**
- **Propósito**: Obtiene la lista de miembros de un canal
- **Retorno**: `CompletableFuture<List<DTOMiembroCanal>>`
- **Protocolo**:
```json
{
  "accion": "OBTENER_MIEMBROS_CANAL",
  "datos": {
    "canalId": "uuid-del-canal"
  }
}
```
- **Respuesta**:
```json
{
  "estado": "EXITO",
  "datos": [
    {
      "usuarioId": "uuid-usuario-1",
      "nombreUsuario": "Juan Pérez",
      "rol": "ADMINISTRADOR",
      "fechaUnion": "2024-01-15T10:30:00"
    },
    {
      "usuarioId": "uuid-usuario-2",
      "nombreUsuario": "María García",
      "rol": "MIEMBRO",
      "fechaUnion": "2024-01-16T14:20:00"
    }
  ]
}
```

**`sincronizarMiembros(String canalId, List<DTOMiembroCanal> miembros)`**
- **Propósito**: Actualiza la lista de miembros en la base de datos local
- **SQL**:
```sql
DELETE FROM miembros_canal WHERE canal_id = ?;
INSERT INTO miembros_canal (canal_id, usuario_id, nombre_usuario, rol, fecha_union)
VALUES (?, ?, ?, ?, ?);
```

#### Caché Local

**`getCanalesCache()`**
- **Propósito**: Obtiene canales desde la caché en memoria
- **Retorno**: `List<Canal>` inmediato (sin espera)
- **Uso**: Para renderizado rápido de UI sin latencia

**`actualizarCache(Canal canal)`**
- **Propósito**: Añade o actualiza un canal en la caché
- **Proceso**:
  1. Busca si el canal ya existe en caché
  2. Si existe, actualiza sus datos
  3. Si no existe, lo añade a la lista
  4. Ordena canales por fecha de último mensaje

**`limpiarCache()`**
- **Propósito**: Elimina todos los canales de la caché en memoria
- **Uso**: Al cerrar sesión o cambiar de usuario

---

## RepositorioMensajeCanalImpl

**Ubicación**: `Persistencia/Repositorio/src/main/java/repositorio/mensaje/RepositorioMensajeCanalImpl.java`

**Interfaz**: `IRepositorioMensajeCanal`

### Descripción

Repositorio especializado en la gestión de mensajes dentro de canales. Maneja el envío, recepción y almacenamiento de mensajes de texto, audio y archivos.

### Métodos Principales

#### Envío de Mensajes

**`enviarMensajeTexto(String canalId, String contenido)`**
- **Propósito**: Envía un mensaje de texto a un canal
- **Protocolo**:
```json
{
  "accion": "ENVIAR_MENSAJE_CANAL",
  "datos": {
    "canalId": "uuid-del-canal",
    "tipo": "TEXTO",
    "contenido": "Hola a todos!"
  }
}
```
- **Proceso**:
  1. Crea objeto Mensaje con timestamp
  2. Serializa a JSON
  3. Envía al servidor
  4. Espera confirmación
  5. Guarda en base de datos local

**`enviarMensajeAudio(String canalId, String audioFileId)`**
- **Propósito**: Envía un mensaje de audio
- **Protocolo**:
```json
{
  "accion": "ENVIAR_MENSAJE_CANAL",
  "datos": {
    "canalId": "uuid-del-canal",
    "tipo": "AUDIO",
    "archivoId": "uuid-del-audio"
  }
}
```
- **Pre-requisito**: El archivo de audio debe haberse subido previamente al servidor

**`enviarArchivo(String canalId, String fileId)`**
- **Propósito**: Comparte un archivo en el canal
- **Protocolo**: Similar a mensaje de audio pero con tipo "ARCHIVO"

#### Recepción de Mensajes

**`solicitarHistorial(String canalId, int limite)`**
- **Propósito**: Solicita mensajes anteriores de un canal
- **Parámetros**:
  - `canalId`: ID del canal
  - `limite`: Número de mensajes (típicamente 50-100)
- **Protocolo**:
```json
{
  "accion": "OBTENER_HISTORIAL_CANAL",
  "datos": {
    "canalId": "uuid-del-canal",
    "limite": 50
  }
}
```
- **Respuesta**: Array de mensajes ordenados por fecha (más recientes primero)

**`guardarMensajeLocal(Mensaje mensaje)`**
- **Propósito**: Persiste un mensaje en la base de datos local
- **SQL**:
```sql
INSERT INTO mensajes_canal (id_mensaje, canal_id, usuario_id, contenido, tipo, fecha_envio)
VALUES (?, ?, ?, ?, ?, ?)
```
- **Uso**: Para historial offline y búsqueda local

**`obtenerHistorialLocal(String canalId, int limite)`**
- **Propósito**: Obtiene mensajes desde la base de datos local
- **Retorno**: `List<Mensaje>` ordenados por fecha
- **Uso**: Para mostrar mensajes mientras se carga el historial del servidor

---

## Otros Repositorios del Sistema

### RepositorioUsuarioImpl

**Ubicación**: `Persistencia/Repositorio/src/main/java/repositorio/usuario/RepositorioUsuarioImpl.java`

**Responsabilidades**:
- Autenticación con el servidor
- Registro de nuevos usuarios
- Actualización de perfil
- Gestión de sesiones
- Almacenamiento de tokens

**Operaciones Principales**:
- `autenticar(String correo, String contrasena)`
- `registrar(Usuario usuario)`
- `actualizarPerfil(Usuario usuario)`
- `obtenerUsuarioActual()`
- `guardarToken(String token)`

### RepositorioContactosImpl

**Ubicación**: `Persistencia/Repositorio/src/main/java/repositorio/contactos/RepositorioContactosImpl.java`

**Responsabilidades**:
- Gestión de lista de contactos
- Envío de solicitudes de amistad
- Sincronización con servidor
- Caché local de contactos

**Operaciones**:
- `solicitarContactos()`
- `enviarSolicitud(String usuarioId)`
- `aceptarSolicitud(String solicitudId)`
- `obtenerContactosLocal()`

### RepositorioMensajePrivadoImpl

**Ubicación**: `Persistencia/Repositorio/src/main/java/repositorio/mensajePrivado/RepositorioMensajePrivadoImpl.java`

**Responsabilidades**:
- Gestión de mensajes privados (1-1)
- Historial de conversaciones privadas
- Sincronización de chats

**Operaciones**:
- `enviarMensajePrivado(String destinatarioId, String contenido)`
- `solicitarHistorialPrivado(String contactoId, int limite)`
- `marcarMensajesComoLeidos(String conversacionId)`

---

## Protocolo de Comunicación JSON

### Estructura General de Mensajes

Todos los mensajes siguen esta estructura:

```json
{
  "accion": "NOMBRE_ACCION",
  "datos": { ... },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Respuestas del Servidor

```json
{
  "estado": "EXITO" | "ERROR",
  "datos": { ... },
  "mensaje": "Descripción opcional"
}
```

### Acciones Disponibles

#### Canales
- `CREAR_CANAL`
- `OBTENER_CANALES_USUARIO`
- `INVITAR_MIEMBRO_CANAL`
- `OBTENER_MIEMBROS_CANAL`
- `ENVIAR_MENSAJE_CANAL`
- `OBTENER_HISTORIAL_CANAL`

#### Usuarios
- `AUTENTICAR_USUARIO`
- `REGISTRAR_USUARIO`
- `ACTUALIZAR_PERFIL`
- `CERRAR_SESION`

#### Contactos
- `OBTENER_CONTACTOS`
- `ENVIAR_SOLICITUD_CONTACTO`
- `ACEPTAR_SOLICITUD`
- `RECHAZAR_SOLICITUD`

#### Mensajes Privados
- `ENVIAR_MENSAJE_PRIVADO`
- `OBTENER_HISTORIAL_PRIVADO`

---

## Base de Datos H2 Local

### Esquema de Tablas

#### Tabla: canales
```sql
CREATE TABLE canales (
    id_canal VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    id_administrador VARCHAR(36) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Tabla: miembros_canal
```sql
CREATE TABLE miembros_canal (
    canal_id VARCHAR(36) NOT NULL,
    usuario_id VARCHAR(36) NOT NULL,
    nombre_usuario VARCHAR(100),
    rol VARCHAR(20),
    fecha_union TIMESTAMP,
    PRIMARY KEY (canal_id, usuario_id),
    FOREIGN KEY (canal_id) REFERENCES canales(id_canal)
);
```

#### Tabla: mensajes_canal
```sql
CREATE TABLE mensajes_canal (
    id_mensaje VARCHAR(36) PRIMARY KEY,
    canal_id VARCHAR(36) NOT NULL,
    usuario_id VARCHAR(36) NOT NULL,
    contenido TEXT,
    tipo VARCHAR(20),
    archivo_id VARCHAR(36),
    fecha_envio TIMESTAMP NOT NULL,
    FOREIGN KEY (canal_id) REFERENCES canales(id_canal)
);

CREATE INDEX idx_mensajes_canal ON mensajes_canal(canal_id, fecha_envio DESC);
```

#### Tabla: contactos
```sql
CREATE TABLE contactos (
    usuario_id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100),
    estado VARCHAR(20),
    fecha_agregado TIMESTAMP
);
```

#### Tabla: mensajes_privados
```sql
CREATE TABLE mensajes_privados (
    id_mensaje VARCHAR(36) PRIMARY KEY,
    remitente_id VARCHAR(36) NOT NULL,
    destinatario_id VARCHAR(36) NOT NULL,
    contenido TEXT,
    tipo VARCHAR(20),
    fecha_envio TIMESTAMP NOT NULL,
    leido BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_conversacion ON mensajes_privados(remitente_id, destinatario_id, fecha_envio DESC);
```

---

## Manejo de Conexiones

### TransporteCanal (Singleton)

**Ubicación**: `Persistencia/Transporte/src/main/java/transporte/TransporteCanal.java`

```java
public class TransporteCanal {
    private static TransporteCanal instancia;
    private IClienteComunicacion cliente;

    private TransporteCanal() {
        this.cliente = new ClienteComunicacionImpl();
    }

    public static synchronized TransporteCanal getInstancia() {
        if (instancia == null) {
            instancia = new TransporteCanal();
        }
        return instancia;
    }

    public IClienteComunicacion getCliente() {
        return cliente;
    }
}
```

### ClienteComunicacionImpl

**Responsabilidades**:
- Gestión de socket TCP con el servidor
- Envío y recepción de mensajes JSON
- Reconexión automática
- Manejo de timeout

**Métodos**:
- `conectar(String host, int puerto)`
- `desconectar()`
- `enviar(String mensaje)`
- `registrarManejador(String accion, ManejadorMensaje manejador)`

---

## Patrón Repository

### Ventajas

✅ **Abstracción**: La lógica de negocio no conoce detalles de persistencia  
✅ **Testabilidad**: Fácil de mockear para pruebas unitarias  
✅ **Centralización**: Toda la lógica de datos en un lugar  
✅ **Cambio de fuente de datos**: Fácil migrar de H2 a PostgreSQL  
✅ **Caché transparente**: La capa de negocio no maneja caché  

### Desventajas y Soluciones

❌ **Complejidad adicional** → Solo crear repositorios para agregados importantes  
❌ **Sincronización compleja** → Usar estrategia eventual consistency  
❌ **Overhead de red** → Implementar caché inteligente con TTL  

---

## Estrategias de Caché

### Cache-Aside (Lazy Loading)

```java
public CompletableFuture<Canal> obtenerCanalPorId(String canalId) {
    // 1. Buscar en caché
    Canal canalCache = buscarEnCache(canalId);
    if (canalCache != null) {
        return CompletableFuture.completedFuture(canalCache);
    }

    // 2. Si no está en caché, consultar servidor
    return consultarServidor(canalId)
        .thenApply(canal -> {
            // 3. Guardar en caché
            actualizarCache(canal);
            return canal;
        });
}
```

### Write-Through Cache

```java
public CompletableFuture<Void> guardarCanal(Canal canal) {
    return enviarAlServidor(canal)
        .thenAccept(respuesta -> {
            // Actualizar caché después de guardar
            actualizarCache(canal);
            guardarEnDB(canal);
        });
}
```

---

## Manejo de Errores

### Errores de Red

```java
public CompletableFuture<Canal> crearCanal(String nombre, String descripcion) {
    return enviarSolicitud(...)
        .exceptionally(error -> {
            if (error instanceof SocketTimeoutException) {
                logger.error("Timeout al crear canal");
                // Reintentar o usar datos locales
            } else if (error instanceof IOException) {
                logger.error("Error de red: " + error.getMessage());
                // Intentar reconexión
            }
            throw new RuntimeException("Error al crear canal", error);
        });
}
```

### Errores de Base de Datos

```java
private void guardarEnDB(Canal canal) {
    try (Connection conn = getConexion()) {
        PreparedStatement stmt = conn.prepareStatement(...);
        stmt.executeUpdate();
    } catch (SQLException e) {
        logger.error("Error al guardar en DB: " + e.getMessage());
        // Log pero no lanzar excepción (no crítico)
    }
}
```

---

## Mejoras Futuras

- [ ] Implementar pool de conexiones a base de datos
- [ ] Añadir retry automático con backoff exponencial
- [ ] Implementar circuit breaker para el servidor
- [ ] Añadir métricas de rendimiento de consultas
- [ ] Implementar paginación en consultas de historial
- [ ] Añadir compresión de mensajes JSON grandes
- [ ] Implementar encriptación end-to-end de mensajes
- [ ] Añadir soporte para sincronización offline
- [ ] Implementar índices full-text para búsqueda de mensajes
- [ ] Añadir limpieza automática de caché antigua (LRU)
# Documentación de Repositorios

## Visión General

Los repositorios forman parte de la **capa de Persistencia** y actúan como intermediarios entre la lógica de negocio y las fuentes de datos (servidor remoto y base de datos local). Implementan el patrón **Repository** para abstraer la lógica de acceso a datos.

### Responsabilidades

1. **Comunicación con el servidor** mediante sockets y protocolo JSON
2. **Persistencia local** en base de datos H2
3. **Sincronización** entre datos locales y remotos
4. **Caché de datos** para rendimiento
5. **Gestión de conexiones** y manejo de errores de red
6. **Serialización/Deserialización** de objetos de dominio

---

## RepositorioCanalImpl

**Ubicación**: `Persistencia/Repositorio/src/main/java/repositorio/canal/RepositorioCanalImpl.java`

**Interfaz**: `IRepositorioCanal`

### Descripción

Repositorio que gestiona toda la persistencia y comunicación relacionada con canales. Mantiene una caché local de canales y sincroniza con el servidor.

### Dependencias

- `IClienteComunicacion`: Cliente de comunicación con el servidor
- `Base de datos H2`: Almacenamiento local persistente
- `Gson`: Serialización/deserialización JSON
- `TransporteCanal`: Singleton que gestiona conexiones

### Estructura de Datos

```java
private List<Canal> canalesCache = new ArrayList<>();
private Connection conexionDB;
```

### Métodos Principales

#### Gestión de Canales

**`crearCanal(String nombre, String descripcion)`**
- **Propósito**: Envía solicitud al servidor para crear un canal
- **Retorno**: `CompletableFuture<Canal>` con el canal creado
- **Protocolo JSON**:
```json
{
  "accion": "CREAR_CANAL",
  "datos": {
    "nombre": "General",
    "descripcion": "Canal general de discusión"
  }
}
```
- **Proceso**:
  1. Construye mensaje JSON
  2. Envía al servidor mediante socket
  3. Espera respuesta asíncrona
  4. Deserializa respuesta a objeto Canal
  5. Guarda en caché local
  6. Persiste en base de datos H2
  7. Retorna CompletableFuture resuelto

**`solicitarCanales()`**
- **Propósito**: Solicita la lista completa de canales del usuario
- **Retorno**: `CompletableFuture<List<Canal>>`
- **Protocolo**:
```json
{

