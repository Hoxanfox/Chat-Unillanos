            }
        });
    }
});

// 4. Realizar operaciones
controlador.crearCanal("Mi Canal", "Descripción")
    .thenAccept(dto -> {
        System.out.println("Canal creado: " + dto.getNombre());
    })
    .exceptionally(error -> {
        mostrarError(error.getMessage());
        return null;
    });
```

### Para Desarrolladores Backend (Servicios/Fachadas)

```java
// 1. Crear fachada con sus gestores
IRepositorioCanal repo = new RepositorioCanalImpl();
ICreadorCanal creador = new CreadorCanal(repo);
IListadorCanales listador = new ListadorCanales(repo);

// 2. Implementar operación de negocio
public CompletableFuture<Canal> crearCanal(String nombre, String desc) {
    // Validar nombre
    if (nombre == null || nombre.trim().isEmpty()) {
        throw new IllegalArgumentException("Nombre requerido");
    }
    
    // Delegar a repositorio
    return repo.crearCanal(nombre, desc)
        .thenApply(canal -> {
            // Lógica adicional post-creación
            notificarObservadores("CANAL_CREADO", canal);
            return canal;
        });
}
```

### Para Desarrolladores de Persistencia (Repositorios)

```java
// 1. Construir mensaje JSON
JsonObject mensaje = new JsonObject();
mensaje.addProperty("accion", "CREAR_CANAL");
JsonObject datos = new JsonObject();
datos.addProperty("nombre", nombre);
datos.addProperty("descripcion", descripcion);
mensaje.add("datos", datos);

// 2. Enviar al servidor
CompletableFuture<Canal> future = new CompletableFuture<>();
cliente.enviar(mensaje.toString(), respuesta -> {
    // 3. Procesar respuesta
    Canal canal = gson.fromJson(respuesta, Canal.class);
    
    // 4. Guardar en caché y DB
    actualizarCache(canal);
    guardarEnDB(canal);
    
    // 5. Completar future
    future.complete(canal);
});

return future;
```

---

## Tipos de Notificaciones (Observer)

### Canales
- `CANAL_CREADO`: Se creó un nuevo canal
- `CANALES_RECIBIDOS`: Lista de canales actualizada
- `ERROR_CREAR_CANAL`: Error al crear canal

### Mensajes
- `MENSAJE_RECIBIDO`: Nuevo mensaje en canal
- `HISTORIAL_RECIBIDO`: Historial de mensajes cargado
- `MENSAJE_ENVIADO`: Confirmación de envío

### Miembros
- `MIEMBROS_CANAL_RECIBIDOS`: Lista de miembros actualizada
- `MIEMBRO_AGREGADO`: Nuevo miembro añadido
- `MIEMBRO_REMOVIDO`: Miembro eliminado

---

## Ejemplo Completo: Flujo de Creación de Canal

### 1. Vista (JavaFX)

```java
public class VistaCrearCanal extends VBox {
    private IControladorCanales controlador;
    
    private void crearCanal() {
        String nombre = txtNombre.getText();
        String desc = txtDescripcion.getText();
        
        controlador.crearCanal(nombre, desc)
            .thenAccept(dto -> {
                Platform.runLater(() -> {
                    mostrarExito("Canal creado: " + dto.getNombre());
                    cerrarVentana();
                });
            })
            .exceptionally(error -> {
                Platform.runLater(() -> {
                    mostrarError(error.getMessage());
                });
                return null;
            });
    }
}
```

### 2. Controlador

```java
public class ControladorCanalesImpl implements IControladorCanales {
    private final IServicioCanales servicio;
    
    @Override
    public CompletableFuture<DTOCanalCreado> crearCanal(String nombre, String desc) {
        return servicio.crearCanal(nombre, desc)
            .thenApply(canal -> new DTOCanalCreado(
                canal.getIdCanal().toString(),
                canal.getNombre()
            ));
    }
}
```

### 3. Servicio

```java
public class ServicioCanalesImpl implements IServicioCanales {
    private final IFachadaCanales fachada;
    
    @Override
    public CompletableFuture<Canal> crearCanal(String nombre, String desc) {
        System.out.println("➡️ [Servicio]: Creando canal: " + nombre);
        return fachada.crearCanal(nombre, desc);
    }
}
```

### 4. Fachada

```java
public class FachadaCanalesImpl implements IFachadaCanales {
    private final ICreadorCanal creador;
    
    @Override
    public CompletableFuture<Canal> crearCanal(String nombre, String desc) {
        return creador.crearCanal(nombre, desc)
            .thenApply(canal -> {
                // Notificar a observadores
                notificarObservadores("CANAL_CREADO", canal);
                return canal;
            });
    }
}
```

### 5. Gestor de Dominio

```java
public class CreadorCanal implements ICreadorCanal {
    private final IRepositorioCanal repositorio;
    
    @Override
    public CompletableFuture<Canal> crearCanal(String nombre, String desc) {
        // Validaciones de negocio
        if (nombre.length() < 3) {
            throw new IllegalArgumentException("Nombre muy corto");
        }
        
        // Delegar a repositorio
        return repositorio.crearCanal(nombre, desc);
    }
}
```

### 6. Repositorio

```java
public class RepositorioCanalImpl implements IRepositorioCanal {
    @Override
    public CompletableFuture<Canal> crearCanal(String nombre, String desc) {
        // Construir mensaje JSON
        JsonObject mensaje = construirMensajeCreacion(nombre, desc);
        
        // Enviar al servidor
        CompletableFuture<Canal> future = new CompletableFuture<>();
        cliente.enviar(mensaje.toString(), respuesta -> {
            Canal canal = parsearRespuesta(respuesta);
            actualizarCache(canal);
            guardarEnDB(canal);
            future.complete(canal);
        });
        
        return future;
    }
}
```

---

## Ventajas de Esta Arquitectura

✅ **Mantenibilidad**: Fácil de entender y modificar  
✅ **Testabilidad**: Cada capa se puede probar independientemente  
✅ **Escalabilidad**: Fácil añadir nuevas funcionalidades  
✅ **Reutilización**: Componentes bien definidos y reutilizables  
✅ **Flexibilidad**: Fácil cambiar implementaciones  
✅ **Separación de Responsabilidades**: Cada componente tiene un propósito claro  

---

## Documentación Adicional

- [Documentación de Controladores](DOCUMENTACION_CONTROLADORES.md)
- [Documentación de Servicios](DOCUMENTACION_SERVICIOS.md)
- [Documentación de Fachadas](DOCUMENTACION_FACHADAS.md)
- [Documentación de Repositorios](DOCUMENTACION_REPOSITORIOS.md)
- [Protocolo JSON de Canales](PROTOCOLO_JSON_CANALES_DETALLADO.md)
- [Sistema de Gestión de Usuarios](SISTEMA_GESTION_USUARIOS.md)
- [Sistema de Gestión de Archivos](SISTEMA_COMPLETO_ARCHIVOS.md)

---

## Conclusión

Esta arquitectura proporciona una base sólida y mantenible para el sistema Chat Unillanos. Cada capa tiene responsabilidades claras y se comunica con las demás mediante interfaces bien definidas, permitiendo un desarrollo organizado y escalable.
# Documentación Completa de la Arquitectura del Sistema

## Índice

1. [Visión General](#visión-general)
2. [Arquitectura en Capas](#arquitectura-en-capas)
3. [Flujo de Datos](#flujo-de-datos)
4. [Patrones de Diseño](#patrones-de-diseño)
5. [Componentes Principales](#componentes-principales)
6. [Guía de Uso](#guía-de-uso)

---

## Visión General

El sistema **Chat Unillanos** está construido con una arquitectura en capas limpia que separa claramente las responsabilidades y mantiene el desacoplamiento entre componentes.

### Principios Arquitectónicos

✅ **Separación de Responsabilidades**: Cada capa tiene una única responsabilidad  
✅ **Inversión de Dependencias**: Las capas dependen de abstracciones, no de implementaciones  
✅ **Bajo Acoplamiento**: Las capas se comunican mediante interfaces bien definidas  
✅ **Alta Cohesión**: Los componentes relacionados están agrupados  
✅ **Principio Abierto/Cerrado**: Abierto a extensión, cerrado a modificación  

---

## Arquitectura en Capas

```
┌─────────────────────────────────────────────────────────┐
│                 CAPA DE PRESENTACIÓN                    │
├─────────────────────────────────────────────────────────┤
│  Vistas (JavaFX)                                        │
│  └── VistaMiembrosCanal, VistaCanal, VistaCrearCanal  │
│                          ↓                              │
│  Controladores                                          │
│  └── ControladorCanalesImpl                            │
│                          ↓                              │
│  DTOs (Data Transfer Objects)                          │
│  └── DTOCanalCreado, DTOMiembroCanal                   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   CAPA DE NEGOCIO                       │
├─────────────────────────────────────────────────────────┤
│  Servicios                                              │
│  └── ServicioCanalesImpl, ServicioUsuarioImpl          │
│                          ↓                              │
│  Fachadas                                               │
│  └── FachadaCanalesImpl, FachadaUsuariosImpl           │
│                          ↓                              │
│  Gestores de Dominio                                    │
│  └── CreadorCanal, ListadorCanales, GestorMensajes     │
│                          ↓                              │
│  Entidades de Dominio                                   │
│  └── Canal, Mensaje, Usuario                           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                 CAPA DE PERSISTENCIA                    │
├─────────────────────────────────────────────────────────┤
│  Repositorios                                           │
│  └── RepositorioCanalImpl, RepositorioMensajeImpl      │
│                          ↓                              │
│  Comunicación                                           │
│  └── ClienteComunicacionImpl (Sockets + JSON)          │
│                          ↓                              │
│  Base de Datos Local (H2)                              │
│  └── Caché y persistencia offline                      │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                      SERVIDOR                           │
│  (TCP Sockets + Base de Datos PostgreSQL)              │
└─────────────────────────────────────────────────────────┘
```

---

## Flujo de Datos

### Ejemplo: Crear un Canal

```
1. Usuario hace clic en "Crear Canal" (Vista)
                ↓
2. Vista llama a controlador.crearCanal(nombre, desc)
                ↓
3. Controlador delega a servicio.crearCanal(nombre, desc)
                ↓
4. Servicio delega a fachada.crearCanal(nombre, desc)
                ↓
5. Fachada llama a creadorCanal.crearCanal(nombre, desc)
                ↓
6. Gestor llama a repositorio.crearCanal(nombre, desc)
                ↓
7. Repositorio envía mensaje JSON al servidor
                ↓
8. Servidor crea canal y responde con datos
                ↓
9. Repositorio recibe respuesta y crea objeto Canal
                ↓
10. Repositorio guarda en caché y DB local
                ↓
11. Canal se propaga hacia arriba (CompletableFuture)
                ↓
12. Controlador convierte Canal a DTOCanalCreado
                ↓
13. Vista recibe DTO y actualiza interfaz
```

### Ejemplo: Recibir un Mensaje (Patrón Observer)

```
1. Servidor envía mensaje JSON al cliente
                ↓
2. ClienteComunicación detecta mensaje entrante
                ↓
3. Repositorio procesa mensaje y crea objeto Mensaje
                ↓
4. Repositorio notifica a observadores registrados
                ↓
5. Gestor recibe notificación y valida mensaje
                ↓
6. Fachada propaga notificación
                ↓
7. Servicio propaga notificación
                ↓
8. Controlador propaga notificación
                ↓
9. Vista recibe notificación (actualizar método)
                ↓
10. Vista actualiza UI en JavaFX Application Thread
```

---

## Patrones de Diseño

### 1. Patrón MVC (Model-View-Controller)

**Ubicación**: Capa de Presentación

**Componentes**:
- **Model**: DTOs (DTOCanalCreado, DTOMiembroCanal)
- **View**: Vistas JavaFX (VistaMiembrosCanal, VistaCanal)
- **Controller**: Controladores (ControladorCanalesImpl)

**Beneficio**: Separación entre lógica de presentación y lógica de negocio

### 2. Patrón Facade

**Ubicación**: Capa de Negocio

**Implementaciones**:
- FachadaCanalesImpl
- FachadaUsuariosImpl
- FachadaChatImpl

**Beneficio**: Simplifica el acceso a subsistemas complejos

### 3. Patrón Repository

**Ubicación**: Capa de Persistencia

**Implementaciones**:
- RepositorioCanalImpl
- RepositorioMensajeCanalImpl
- RepositorioUsuarioImpl

**Beneficio**: Abstrae la lógica de acceso a datos

### 4. Patrón Observer

**Ubicación**: Transversal (todas las capas)

**Uso**: Comunicación asíncrona de eventos

**Ejemplo**:
```java
// Registrar observador
controlador.registrarObservadorMensajes(new IObservador() {
    @Override
    public void actualizar(String tipo, Object datos) {
        if ("MENSAJE_RECIBIDO".equals(tipo)) {
            Mensaje mensaje = (Mensaje) datos;
            mostrarEnUI(mensaje);
        }
    }
});
```

**Beneficio**: Desacoplamiento temporal entre emisor y receptor

### 5. Patrón Singleton

**Ubicación**: Gestores de recursos compartidos

**Implementaciones**:
- FachadaGeneralImpl
- TransporteCanal

**Beneficio**: Una única instancia compartida en toda la aplicación

### 6. Patrón DTO (Data Transfer Object)

**Ubicación**: Capa de Presentación

**Implementaciones**:
- DTOCanalCreado
- DTOMiembroCanal
- DTOMensaje

**Beneficio**: Desacopla la presentación del dominio

### 7. Patrón Service Layer

**Ubicación**: Capa de Negocio

**Implementaciones**:
- ServicioCanalesImpl
- ServicioUsuarioImpl
- ServicioChatImpl

**Beneficio**: Encapsula lógica de negocio y coordina operaciones

---

## Componentes Principales

### Capa de Presentación

#### Controladores
- **Responsabilidad**: Intermediario entre vistas y servicios
- **Entrada**: Peticiones desde vistas
- **Salida**: DTOs hacia vistas
- **Ver**: [DOCUMENTACION_CONTROLADORES.md](DOCUMENTACION_CONTROLADORES.md)

#### Vistas (JavaFX)
- **Responsabilidad**: Presentación visual al usuario
- **Tecnología**: JavaFX con FXML
- **Patrón**: Observer para actualizaciones reactivas

#### DTOs
- **Responsabilidad**: Transferir datos entre capas
- **Características**: Inmutables, sin lógica de negocio
- **Ejemplo**: DTOCanalCreado, DTOMiembroCanal

### Capa de Negocio

#### Servicios
- **Responsabilidad**: Coordinar operaciones de negocio
- **Entrada**: Peticiones desde controladores
- **Salida**: Objetos de dominio o DTOs
- **Ver**: [DOCUMENTACION_SERVICIOS.md](DOCUMENTACION_SERVICIOS.md)

#### Fachadas
- **Responsabilidad**: Orquestar gestores especializados
- **Patrón**: Facade para simplificar subsistemas
- **Ver**: [DOCUMENTACION_FACHADAS.md](DOCUMENTACION_FACHADAS.md)

#### Gestores de Dominio
- **Responsabilidad**: Implementar reglas de negocio específicas
- **Ejemplos**: CreadorCanal, ListadorCanales, GestorMensajes
- **Características**: Alta cohesión, responsabilidad única

#### Entidades de Dominio
- **Responsabilidad**: Representar conceptos del dominio
- **Ejemplos**: Canal, Mensaje, Usuario
- **Características**: Rich domain models con comportamiento

### Capa de Persistencia

#### Repositorios
- **Responsabilidad**: Gestionar persistencia y comunicación
- **Tecnologías**: Sockets TCP, JSON, H2 Database
- **Ver**: [DOCUMENTACION_REPOSITORIOS.md](DOCUMENTACION_REPOSITORIOS.md)

#### Comunicación
- **Componente**: ClienteComunicacionImpl
- **Protocolo**: JSON sobre TCP Sockets
- **Características**: Asíncrono, con reconexión automática

#### Base de Datos Local
- **Motor**: H2 (embedded)
- **Uso**: Caché persistente, funcionamiento offline
- **Tablas**: canales, mensajes_canal, miembros_canal, contactos

---

## Guía de Uso

### Para Desarrolladores Frontend (Vistas)

```java
// 1. Crear controlador
IControladorCanales controlador = new ControladorCanalesImpl();

// 2. Inicializar manejadores (una vez al inicio)
controlador.inicializarManejadoresMensajes();

// 3. Registrar observador
controlador.registrarObservadorMensajes(new IObservador() {
    @Override
    public void actualizar(String tipo, Object datos) {
        Platform.runLater(() -> {
            // Actualizar UI en el hilo de JavaFX
            if ("MENSAJE_RECIBIDO".equals(tipo)) {
                Mensaje msg = (Mensaje) datos;
                agregarMensajeAVista(msg);

