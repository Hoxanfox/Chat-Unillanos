- **Eventos**: `CANALES_RECIBIDOS`, `CANAL_ACTUALIZADO`

**`registrarObservadorMensajes(IObservador observador)`**
- **Propósito**: Registra observador para mensajes de canales
- **Delegación**: `gestorMensajes.registrarObservador(observador)`
- **Eventos**: `MENSAJE_RECIBIDO`, `HISTORIAL_RECIBIDO`, `MENSAJE_AUDIO`, `ARCHIVO_COMPARTIDO`

**`registrarObservadorMiembros(IObservador observador)`**
- **Propósito**: Registra observador para cambios en miembros
- **Delegación**: `listadorMiembros.registrarObservador(observador)`
- **Eventos**: `MIEMBROS_CANAL_RECIBIDOS`, `MIEMBRO_AGREGADO`, `MIEMBRO_REMOVIDO`

**`inicializarManejadoresMensajes()`**
- **Propósito**: Configura los handlers de mensajes entrantes desde el servidor
- **Delegación**: `gestorMensajes.inicializarManejadoresMensajes()`
- **Momento**: Debe llamarse una vez al iniciar la aplicación
- **Efecto**: Registra listeners para protocolo JSON

### Patrón Facade en Acción

La fachada simplifica la interacción con múltiples componentes:

**Sin Fachada (complejo)**:
```java
IRepositorioCanal repoCanal = new RepositorioCanalImpl();
IRepositorioMensajeCanal repoMensajes = new RepositorioMensajeCanalImpl();
CreadorCanal creador = new CreadorCanal(repoCanal);
ListadorCanales listador = new ListadorCanales(repoCanal);
GestorMensajesCanal gestor = new GestorMensajesCanalImpl(repoMensajes);

// Crear canal
creador.crearCanal("General", "Canal general");
// Enviar mensaje
gestor.enviarMensajeTexto(canalId, "Hola");
```

**Con Fachada (simplificado)**:
```java
IFachadaCanales fachada = new FachadaCanalesImpl();

// Crear canal
fachada.crearCanal("General", "Canal general");
// Enviar mensaje
fachada.enviarMensajeTexto(canalId, "Hola");
```

---

## Otras Fachadas del Sistema

### FachadaUsuariosImpl

**Ubicación**: `Negocio/Fachada/src/main/java/fachada/gestionUsuarios/insercionDB/FachadaUsuariosImpl.java`

**Responsabilidades**:
- Gestión de registro de usuarios
- Actualización de perfiles
- Validación de datos de usuario
- Persistencia en base de datos local

**Gestores Coordinados**:
- `RegistradorUsuario`: Crea nuevos usuarios
- `ActualizadorPerfil`: Modifica información de perfil
- `ValidadorDatos`: Valida correos, contraseñas, etc.

### FachadaAutenticacionUsuario

**Ubicación**: `Negocio/Fachada/src/main/java/fachada/gestionUsuarios/autenticacion/FachadaAutenticacionUsuario.java`

**Responsabilidades**:
- Autenticación de usuarios (login)
- Gestión de tokens de sesión
- Verificación de credenciales
- Manejo de cierre de sesión

**Operaciones**:
- `iniciarSesion(String correo, String contrasena)`
- `verificarToken(String token)`
- `cerrarSesion()`
- `renovarToken()`

### FachadaChatImpl

**Ubicación**: `Negocio/Fachada/src/main/java/fachada/gestionContactos/chat/FachadaChatImpl.java`

**Responsabilidades**:
- Gestión de chats uno-a-uno
- Envío de mensajes privados
- Historial de conversaciones privadas

**Gestores Coordinados**:
- `EnviadorMensajesPrivados`: Envía mensajes 1-1
- `ReceptorMensajesPrivados`: Recibe mensajes entrantes
- `GestorHistorialPrivado`: Maneja historial de conversaciones

### FachadaContactosImpl

**Ubicación**: `Negocio/Fachada/src/main/java/fachada/gestionContactos/contactos/FachadaContactosImpl.java`

**Responsabilidades**:
- Gestión de lista de contactos
- Solicitudes de amistad
- Búsqueda de usuarios

**Gestores Coordinados**:
- `SolicitadorContacto`: Envía solicitudes de contacto
- `AceptadorSolicitud`: Acepta/rechaza solicitudes
- `ListadorContactos`: Obtiene lista de contactos

### FachadaConexionImpl

**Ubicación**: `Negocio/Fachada/src/main/java/fachada/gestionConexion/FachadaConexionImpl.java`

**Responsabilidades**:
- Gestión de conexión con el servidor
- Reconexión automática
- Manejo de desconexiones
- Estado de conectividad

**Operaciones**:
- `conectar(String host, int puerto)`
- `desconectar()`
- `estaConectado()`
- `configurarReconexionAutomatica()`

### FachadaNotificacionesImpl

**Ubicación**: `Negocio/Fachada/src/main/java/fachada/gestionNotificaciones/FachadaNotificacionesImpl.java`

**Responsabilidades**:
- Gestión de notificaciones del sistema
- Alertas visuales y sonoras
- Notificaciones push
- Gestión de prioridades

---

## FachadaGeneralImpl (Fachada de Fachadas)

**Ubicación**: `Negocio/Fachada/src/main/java/fachada/FachadaGeneralImpl.java`

### Descripción

Fachada principal que actúa como punto de acceso único a todas las demás fachadas del sistema. Implementa el patrón **Singleton** para garantizar una única instancia en toda la aplicación.

### Patrón Singleton

```java
public class FachadaGeneralImpl implements IFachadaGeneral {
    private static FachadaGeneralImpl instancia;

    private FachadaGeneralImpl() {
        // Constructor privado
        inicializarFachadas();
    }

    public static synchronized FachadaGeneralImpl getInstancia() {
        if (instancia == null) {
            instancia = new FachadaGeneralImpl();
        }
        return instancia;
    }
}
```

### Métodos de Acceso

```java
// Obtener fachadas específicas
IFachadaCanales getFachadaCanales()
IFachadaUsuarios getFachadaUsuarios()
IFachadaChat getFachadaChat()
IFachadaContactos getFachadaContactos()
IFachadaConexion getFachadaConexion()
IFachadaNotificaciones getFachadaNotificaciones()
```

### Ventajas del Singleton

✅ **Control centralizado**: Un único punto de acceso  
✅ **Gestión de recursos**: Evita múltiples instancias  
✅ **Estado compartido**: Todas las capas usan las mismas fachadas  
✅ **Lazy initialization**: Se crea solo cuando se necesita  

---

## Principios de Diseño Aplicados

### Patrón Facade

**Propósito**: Proporcionar una interfaz unificada a un conjunto de interfaces en un subsistema.

**Beneficios**:
- Simplifica el uso de subsistemas complejos
- Reduce el acoplamiento entre clientes y subsistemas
- Permite cambios internos sin afectar clientes

### Separación de Responsabilidades

Cada gestor tiene una única responsabilidad:
- **CreadorCanal**: Solo crea canales
- **ListadorCanales**: Solo lista y cachea canales
- **GestorMensajes**: Solo gestiona mensajes
- **InvitadorMiembro**: Solo gestiona invitaciones
- **ListadorMiembros**: Solo lista miembros

### Inyección de Dependencias

Las fachadas reciben sus dependencias (repositorios) en el constructor:

```java
public FachadaCanalesImpl() {
    IRepositorioCanal repositorioCanal = new RepositorioCanalImpl();
    this.creadorCanal = new CreadorCanal(repositorioCanal);
    this.listadorCanales = new ListadorCanales(repositorioCanal);
}
```

---

## Diagrama de Arquitectura

```
ServicioCanalesImpl
        ↓ (usa)
FachadaCanalesImpl
        ↓ (orquesta)
    ┌───────┴───────┬──────────┬────────────┬──────────────┐
    ↓               ↓          ↓            ↓              ↓
CreadorCanal  ListadorCanales  GestorMensajes  InvitadorMiembro  ListadorMiembros
    ↓               ↓          ↓            ↓              ↓
    └───────┬───────┴──────────┴────────────┴──────────────┘
            ↓ (usa)
    RepositorioCanalImpl / RepositorioMensajeCanalImpl
            ↓ (comunica con)
          Servidor
```

---

## Manejo de Errores

Las fachadas propagan errores mediante `CompletableFuture.exceptionally()`:

```java
public CompletableFuture<Canal> crearCanal(String nombre, String descripcion) {
    return creadorCanal.crearCanal(nombre, descripcion)
        .exceptionally(error -> {
            System.err.println("❌ Error al crear canal: " + error.getMessage());
            // Notificar a observadores del error
            notificarObservadores("ERROR_CREAR_CANAL", error.getMessage());
            return null;
        });
}
```

---

## Mejoras Futuras

- [ ] Implementar transacciones distribuidas (Saga pattern)
- [ ] Añadir circuit breaker para resiliencia
- [ ] Implementar caché distribuida entre fachadas
- [ ] Añadir métricas de rendimiento por operación
- [ ] Implementar audit logging de todas las operaciones
- [ ] Añadir validación de entrada en cada método público
- [ ] Implementar rate limiting por usuario
- [ ] Añadir soporte para operaciones batch
# Documentación de Fachadas

## Visión General

Las fachadas forman parte de la **capa de Negocio** y actúan como orquestadores de los gestores especializados de dominio. Implementan el patrón **Facade** para proporcionar una interfaz unificada y simplificada a subsistemas complejos.

### Responsabilidades

1. **Orquestar** múltiples gestores de negocio
2. **Coordinar** operaciones complejas entre repositorios
3. **Gestionar** observadores del patrón Observer
4. **Proporcionar** una API cohesiva hacia los servicios
5. **Encapsular** la complejidad de la lógica de negocio

---

## FachadaCanalesImpl

**Ubicación**: `Negocio/Fachada/src/main/java/fachada/gestionCanales/FachadaCanalesImpl.java`

**Interfaz**: `IFachadaCanales`

### Descripción

Fachada principal que orquesta todas las operaciones relacionadas con canales de comunicación grupal. Coordina cinco gestores especializados para proporcionar funcionalidad completa de canales.

### Arquitectura Interna

La fachada integra los siguientes gestores de negocio:

```
FachadaCanalesImpl
├── CreadorCanal          (Creación de canales)
├── ListadorCanales       (Listado y caché de canales)
├── GestorMensajesCanal   (Mensajería en canales)
├── InvitadorMiembro      (Gestión de invitaciones)
└── ListadorMiembros      (Listado de miembros)
```

### Inicialización

```java
public FachadaCanalesImpl() {
    // Crear repositorios compartidos
    IRepositorioCanal repositorioCanal = new RepositorioCanalImpl();
    IRepositorioMensajeCanal repositorioMensajes = new RepositorioMensajeCanalImpl();

    // Inicializar gestores con sus repositorios
    this.creadorCanal = new CreadorCanal(repositorioCanal);
    this.listadorCanales = new ListadorCanales(repositorioCanal);
    this.gestorMensajes = new GestorMensajesCanalImpl(repositorioMensajes);
    this.invitadorMiembro = new InvitadorMiembro(repositorioCanal);
    this.listadorMiembros = new ListadorMiembros(repositorioCanal);

    System.out.println("✅ [FachadaCanales]: Inicializada con todos los gestores");
}
```

### Métodos Principales

#### Gestión de Canales

**`crearCanal(String nombre, String descripcion)`**
- **Propósito**: Crea un nuevo canal de comunicación
- **Delegación**: `creadorCanal.crearCanal(nombre, descripcion)`
- **Retorno**: `CompletableFuture<Canal>` con el canal creado
- **Proceso**:
  1. Valida el nombre del canal
  2. Crea el canal en el servidor
  3. Añade al usuario actual como administrador
  4. Notifica a observadores registrados

**`solicitarCanalesUsuario()`**
- **Propósito**: Solicita la lista de canales del usuario actual
- **Delegación**: `listadorCanales.solicitarCanalesUsuario()`
- **Comunicación**: Asíncrona mediante Observer
- **Proceso**:
  1. Envía solicitud al servidor
  2. Espera respuesta asíncrona
  3. Actualiza caché local
  4. Notifica a observadores con la lista actualizada

**`obtenerCanalesCache()`**
- **Propósito**: Obtiene canales desde la caché local
- **Delegación**: `listadorCanales.getCanales()`
- **Retorno**: `List<Canal>` inmediato sin consultar servidor
- **Uso**: Para carga rápida de interfaces

#### Gestión de Mensajes

**`enviarMensajeTexto(String canalId, String contenido)`**
- **Propósito**: Envía un mensaje de texto a un canal
- **Delegación**: `gestorMensajes.enviarMensajeTexto(canalId, contenido)`
- **Retorno**: `CompletableFuture<Void>` confirmando el envío
- **Proceso**:
  1. Valida que el usuario sea miembro del canal
  2. Crea el objeto Mensaje con timestamp
  3. Envía al servidor mediante protocolo JSON
  4. Confirma recepción

**`enviarMensajeAudio(String canalId, String audioFileId)`**
- **Propósito**: Envía un mensaje de audio a un canal
- **Parámetros**:
  - `canalId`: ID del canal destino
  - `audioFileId`: ID del archivo de audio previamente subido
- **Proceso**: Similar a mensaje de texto pero con referencia al archivo

**`enviarArchivo(String canalId, String fileId)`**
- **Propósito**: Comparte un archivo en un canal
- **Proceso**:
  1. Valida que el archivo exista en el servidor
  2. Crea mensaje con tipo "archivo"
  3. Envía al canal
  4. Los miembros pueden descargar el archivo

**`solicitarHistorialCanal(String canalId, int limite)`**
- **Propósito**: Solicita mensajes anteriores de un canal
- **Delegación**: `gestorMensajes.solicitarHistorialCanal(canalId, limite)`
- **Parámetros**:
  - `canalId`: ID del canal
  - `limite`: Número máximo de mensajes (típicamente 50-100)
- **Respuesta**: Llega mediante observador con lista de mensajes

#### Gestión de Miembros

**`invitarMiembro(String canalId, String contactoId)`**
- **Propósito**: Invita a un usuario a unirse a un canal
- **Delegación**: `invitadorMiembro.invitarMiembro(canalId, contactoId)`
- **Retorno**: `CompletableFuture<Void>`
- **Validaciones**:
  - Usuario que invita debe ser administrador del canal
  - Usuario invitado debe estar en contactos
  - Usuario invitado no debe ser ya miembro
- **Proceso**:
  1. Valida permisos
  2. Envía invitación al servidor
  3. Servidor notifica al usuario invitado
  4. Usuario puede aceptar/rechazar

**`solicitarMiembrosCanal(String canalId)`**
- **Propósito**: Obtiene la lista de miembros de un canal
- **Delegación**: `listadorMiembros.solicitarMiembros(canalId)`
- **Respuesta**: Lista de `DTOMiembroCanal` mediante observador
- **Información incluida**: ID, nombre, rol, fecha de unión

#### Gestión de Observadores

**`registrarObservadorCreacion(IObservador observador)`**
- **Propósito**: Registra observador para eventos de creación de canales
- **Delegación**: `creadorCanal.registrarObservador(observador)`
- **Eventos**: `CANAL_CREADO`, `ERROR_CREAR_CANAL`

**`registrarObservadorListado(IObservador observador)`**
- **Propósito**: Registra observador para cambios en lista de canales
- **Delegación**: `listadorCanales.registrarObservador(observador)`

