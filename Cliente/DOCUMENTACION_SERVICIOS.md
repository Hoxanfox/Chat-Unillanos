# Documentación de Servicios

## Visión General

Los servicios forman parte de la **capa de Negocio** y actúan como coordinadores de alto nivel entre los controladores y las fachadas. Su responsabilidad principal es delegar operaciones a las fachadas especializadas.

### Responsabilidades

1. **Recibir peticiones** desde los controladores
2. **Delegar operaciones** a las fachadas de negocio
3. **Coordinar** operaciones transversales entre múltiples fachadas
4. **Proporcionar una API limpia** hacia la capa de presentación
5. **Gestionar observadores** de forma centralizada

---

## ServicioCanalesImpl

**Ubicación**: `Negocio/Servicio/src/main/java/servicio/canales/ServicioCanalesImpl.java`

**Interfaz**: `IServicioCanales`

### Descripción

Servicio que coordina todas las operaciones relacionadas con canales de comunicación. Actúa como punto de entrada único para la gestión de canales desde la capa de presentación.

### Dependencias

- `IFachadaCanales`: Fachada que orquesta los gestores de canales
- `FachadaGeneralImpl`: Singleton que proporciona acceso a todas las fachadas

### Inicialización

```java
public ServicioCanalesImpl() {
    this.fachadaCanales = FachadaGeneralImpl.getInstancia().getFachadaCanales();
    System.out.println("✅ [ServicioCanales]: Inicializado con FachadaCanales");
}
```

### Métodos Principales

#### Gestión de Canales

**`crearCanal(String nombre, String descripcion)`**
- **Propósito**: Crea un nuevo canal de comunicación grupal
- **Parámetros**:
  - `nombre`: Nombre del canal (visible para usuarios)
  - `descripcion`: Descripción opcional del canal
- **Retorno**: `CompletableFuture<Canal>` con el canal creado
- **Delegación**: `fachadaCanales.crearCanal(nombre, descripcion)`

**`solicitarCanalesUsuario()`**
- **Propósito**: Solicita al servidor la lista de canales del usuario actual
- **Comunicación**: Asíncrona mediante patrón Observer
- **Delegación**: `fachadaCanales.solicitarCanalesUsuario()`

**`obtenerCanalesCache()`**
- **Propósito**: Obtiene canales desde la caché local sin consultar el servidor
- **Retorno**: `List<Canal>` con los canales en memoria
- **Uso**: Para carga rápida de la interfaz

#### Gestión de Mensajes

**`enviarMensajeTexto(String canalId, String contenido)`**
- **Propósito**: Envía un mensaje de texto a un canal
- **Parámetros**:
  - `canalId`: Identificador único del canal
  - `contenido`: Texto del mensaje
- **Retorno**: `CompletableFuture<Void>` confirmando el envío

**`enviarMensajeAudio(String canalId, String audioFileId)`**
- **Propósito**: Envía un mensaje de audio a un canal
- **Parámetros**:
  - `canalId`: ID del canal destino
  - `audioFileId`: ID del archivo de audio previamente subido

**`enviarArchivo(String canalId, String fileId)`**
- **Propósito**: Envía un archivo adjunto a un canal
- **Parámetros**:
  - `canalId`: ID del canal destino
  - `fileId`: ID del archivo en el servidor

**`solicitarHistorialCanal(String canalId, int limite)`**
- **Propósito**: Solicita el historial de mensajes de un canal
- **Parámetros**:
  - `canalId`: ID del canal
  - `limite`: Número máximo de mensajes a recuperar
- **Comunicación**: Los mensajes llegan mediante observadores

#### Gestión de Miembros

**`invitarMiembro(String canalId, String contactoId)`**
- **Propósito**: Invita a un usuario a unirse a un canal
- **Parámetros**:
  - `canalId`: ID del canal
  - `contactoId`: ID del usuario a invitar
- **Retorno**: `CompletableFuture<Void>` confirmando la invitación
- **Permisos**: Solo el administrador del canal puede invitar

**`solicitarMiembrosCanal(String canalId)`**
- **Propósito**: Solicita la lista de miembros de un canal
- **Parámetro**: `canalId` - ID del canal
- **Respuesta**: Lista de miembros mediante observador

#### Gestión de Observadores

**`registrarObservadorCreacion(IObservador observador)`**
- **Propósito**: Registra un observador para eventos de creación de canales
- **Notificaciones**: `CANAL_CREADO`, `ERROR_CREAR_CANAL`

**`registrarObservadorListado(IObservador observador)`**
- **Propósito**: Registra un observador para cambios en la lista de canales
- **Notificaciones**: `CANALES_RECIBIDOS`, `CANAL_ACTUALIZADO`

**`registrarObservadorMensajes(IObservador observador)`**
- **Propósito**: Registra un observador para nuevos mensajes en canales
- **Notificaciones**: `MENSAJE_RECIBIDO`, `HISTORIAL_RECIBIDO`

**`registrarObservadorMiembros(IObservador observador)`**
- **Propósito**: Registra un observador para cambios en miembros de canales
- **Notificaciones**: `MIEMBROS_CANAL_RECIBIDOS`, `MIEMBRO_AGREGADO`

**`inicializarManejadoresMensajes()`**
- **Propósito**: Configura los listeners para mensajes entrantes desde el servidor
- **Momento**: Debe llamarse una vez al iniciar la aplicación
- **Efecto**: Establece handlers para protocolo JSON de mensajes

### Logging y Trazabilidad

El servicio incluye logging detallado para facilitar el debugging:

```java
System.out.println("➡️ [ServicioCanales]: Creando canal: " + nombre);
System.out.println("🔔 [ServicioCanales]: Registrando observador de creación");
System.out.println("➡️ [ServicioCanales]: Solicitando lista de canales");
```

### Ejemplo de Uso Completo

```java
// Inicializar servicio
IServicioCanales servicio = new ServicioCanalesImpl();

// Inicializar manejadores (una sola vez al inicio)
servicio.inicializarManejadoresMensajes();

// Registrar observador para nuevos mensajes
servicio.registrarObservadorMensajes(new IObservador() {
    @Override
    public void actualizar(String tipo, Object datos) {
        switch (tipo) {
            case "MENSAJE_RECIBIDO":
                Mensaje mensaje = (Mensaje) datos;
                mostrarMensajeEnUI(mensaje);
                break;
            case "HISTORIAL_RECIBIDO":
                List<Mensaje> historial = (List<Mensaje>) datos;
                cargarHistorialEnUI(historial);
                break;
        }
    }
});

// Crear un canal
servicio.crearCanal("Proyecto X", "Discusiones sobre el proyecto X")
    .thenAccept(canal -> {
        System.out.println("Canal creado: " + canal.getNombre());
        // Solicitar historial
        servicio.solicitarHistorialCanal(canal.getIdCanal().toString(), 50);
    })
    .exceptionally(error -> {
        System.err.println("Error al crear canal: " + error.getMessage());
        return null;
    });

// Enviar mensaje
servicio.enviarMensajeTexto(canalId, "Hola equipo!")
    .thenRun(() -> System.out.println("Mensaje enviado"));

// Invitar miembro
servicio.invitarMiembro(canalId, contactoId)
    .thenRun(() -> System.out.println("Invitación enviada"));
```

---

## Otros Servicios

### ServicioUsuarioImpl

**Ubicación**: `Negocio/Servicio/src/main/java/servicio/usuario/ServicioUsuarioImpl.java`

**Responsabilidades**:
- Gestión de autenticación (login/logout)
- Registro de nuevos usuarios
- Actualización de perfil de usuario
- Gestión de sesiones

**Operaciones Principales**:
- `iniciarSesion(String correo, String contrasena)`
- `registrarUsuario(String nombre, String correo, String contrasena)`
- `cerrarSesion()`
- `obtenerUsuarioActual()`

### ServicioChatImpl

**Ubicación**: `Negocio/Servicio/src/main/java/servicio/chat/ServicioChatImpl.java`

**Responsabilidades**:
- Gestión de chats uno-a-uno (conversaciones privadas)
- Envío y recepción de mensajes privados
- Historial de conversaciones

**Operaciones Principales**:
- `enviarMensajePrivado(String destinatarioId, String contenido)`
- `solicitarHistorialChat(String contactoId, int limite)`
- `registrarObservadorMensajes(IObservador observador)`

### ServicioContactosImpl

**Ubicación**: `Negocio/Servicio/src/main/java/servicio/contactos/ServicioContactosImpl.java`

**Responsabilidades**:
- Gestión de lista de contactos
- Envío de solicitudes de amistad
- Aceptación/rechazo de solicitudes

**Operaciones Principales**:
- `solicitarContactos()`
- `enviarSolicitudContacto(String usuarioId)`
- `aceptarSolicitud(String solicitudId)`
- `rechazarSolicitud(String solicitudId)`

### ServicioArchivosImpl

**Ubicación**: `Negocio/Servicio/src/main/java/servicio/archivos/ServicioArchivosImpl.java`

**Responsabilidades**:
- Subida de archivos al servidor
- Descarga de archivos compartidos
- Gestión de metadatos de archivos

**Operaciones Principales**:
- `subirArchivo(File archivo, Consumer<Double> progreso)`
- `descargarArchivo(String fileId, String rutaDestino)`
- `obtenerMetadatosArchivo(String fileId)`

### ServicioNotificacionesImpl

**Ubicación**: `Negocio/Servicio/src/main/java/servicio/notificaciones/ServicioNotificacionesImpl.java`

**Responsabilidades**:
- Gestión de notificaciones del sistema
- Alertas de nuevos mensajes
- Notificaciones de eventos importantes

**Operaciones Principales**:
- `registrarObservadorNotificaciones(IObservador observador)`
- `marcarNotificacionLeida(String notificacionId)`
- `obtenerNotificacionesPendientes()`

---

## Patrón de Diseño: Service Layer

### Ventajas

✅ **Centralización**: Punto único de acceso a la lógica de negocio  
✅ **Coordinación**: Puede orquestar múltiples fachadas  
✅ **Desacoplamiento**: Los controladores no conocen las fachadas directamente  
✅ **Testabilidad**: Fácil de mockear para pruebas unitarias  
✅ **Reutilización**: Múltiples controladores pueden usar el mismo servicio  

### Flujo de Datos

```
Controlador
    ↓ (llama)
Servicio
    ↓ (delega a)
Fachada
    ↓ (orquesta)
Gestores de Negocio
    ↓ (usa)
Repositorios
    ↓ (comunica con)
Servidor
```

---

## Principios SOLID Aplicados

### Single Responsibility Principle (SRP)
Cada servicio tiene una única responsabilidad bien definida (canales, usuarios, chat, etc.)

### Open/Closed Principle (OCP)
Los servicios son abiertos a extensión mediante nuevos métodos pero cerrados a modificación

### Liskov Substitution Principle (LSP)
Las implementaciones pueden sustituirse por sus interfaces sin afectar el comportamiento

### Interface Segregation Principle (ISP)
Cada servicio tiene su propia interfaz específica (IServicioCanales, IServicioChat, etc.)

### Dependency Inversion Principle (DIP)
Los servicios dependen de abstracciones (IFachada) no de implementaciones concretas

---

## Mejoras Futuras

- [ ] Implementar caché distribuida (Redis)
- [ ] Añadir circuit breaker para resiliencia
- [ ] Implementar retry policies para operaciones fallidas
- [ ] Añadir métricas y observabilidad (Micrometer/Prometheus)
- [ ] Implementar rate limiting para prevenir abuso
- [ ] Añadir validación de entrada en todos los métodos
- [ ] Implementar transacciones distribuidas (Saga pattern)

