    "usuarioOrigenId": "user-uuid-123",
    "accion": "ACEPTAR"
  }
}
```

**Respuesta del servidor:**
```json
{
  "status": "success",
  "message": "Solicitud de amistad aceptada"
}
```

---

## 3. Notificaciones en Tiempo Real

El sistema también maneja notificaciones que llegan en tiempo real desde el servidor sin que el usuario las solicite.

### Inicialización de Manejadores

```java
public void inicializarManejadores() {
    System.out.println("🔧 Inicializando manejadores");
    
    // Registrar manejador para nuevas notificaciones
    gestorRespuesta.registrarManejador(
        "nuevaNotificacion", 
        this::manejarNuevaNotificacion
    );
    
    // Registrar manejador para solicitudes aceptadas
    gestorRespuesta.registrarManejador(
        "solicitudAceptada", 
        this::manejarSolicitudAceptada
    );
    
    System.out.println("✅ Manejadores inicializados");
}
```

### Flujo de Notificación en Tiempo Real

```
┌──────────┐                ┌──────────────────┐           ┌────────────┐
│ Servidor │                │ GestorNotifica-  │           │   Vista    │
└────┬─────┘                │     ciones       │           └─────┬──────┘
     │                      └────────┬─────────┘                 │
     │ 1. Push: Nueva notificación  │                            │
     │──────────────────────────────>│                            │
     │                               │                            │
     │                               │ 2. Parsear JSON            │
     │                               │                            │
     │                               │ 3. Crear DTONotificacion   │
     │                               │                            │
     │                               │ 4. Guardar en caché        │
     │                               │    repositorio.guardar()   │
     │                               │                            │
     │                               │ 5. Notificar observadores  │
     │                               │    "NUEVA_NOTIFICACION"    │
     │                               │────────────────────────────>│
     │                               │                            │
     │                               │                            │ 6. Actualizar UI
     │                               │                            │    (Mostrar badge)
```

### Código del Manejador

```java
private void manejarNuevaNotificacion(DTOResponse respuesta) {
    System.out.println("🔔 Nueva notificación en tiempo real");
    
    try {
        // Parsear datos de la respuesta
        JsonElement element = gson.toJsonTree(respuesta.getData());
        JsonObject data = element.getAsJsonObject();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        // Extraer campos
        String id = data.get("id").getAsString();
        String tipo = data.get("tipo").getAsString();
        String titulo = data.get("titulo").getAsString();
        String contenido = data.get("contenido").getAsString();
        LocalDateTime fecha = LocalDateTime.parse(
            data.get("fecha").getAsString(), 
            formatter
        );
        boolean leida = false;
        String origenId = data.get("origenId").getAsString();
        
        // Crear DTO
        DTONotificacion notificacion = new DTONotificacion(
            id, tipo, titulo, contenido, fecha, leida, origenId
        );
        
        // Guardar en repositorio (caché local)
        repositorioNotificacion.guardar(notificacion);
        
        // Notificar a observadores (Vistas)
        notificarObservadores("NUEVA_NOTIFICACION", notificacion);
        
    } catch (Exception e) {
        System.err.println("❌ Error al procesar: " + e.getMessage());
    }
}
```

**Mensaje JSON del servidor (Push):**
```json
{
  "action": "nuevaNotificacion",
  "data": {
    "id": "notif-003",
    "tipo": "MENCION",
    "titulo": "Te mencionaron",
    "contenido": "@tuUsuario en canal 'General'",
    "fecha": "2025-10-16T14:20:00",
    "origenId": "canal-general-uuid"
  }
}
```

---

## Repositorio y Persistencia

### Implementación del Repositorio

El repositorio **NO** tiene comunicación con el servidor. Solo maneja almacenamiento en memoria (caché).

```java
public class RepositorioNotificacionImpl implements IRepositorioNotificacion {
    
    // Caché en memoria
    private final List<DTONotificacion> notificacionesCache;
    
    public RepositorioNotificacionImpl() {
        this.notificacionesCache = new ArrayList<>();
        System.out.println("✅ [Repositorio]: Inicializado (solo caché)");
    }
    
    @Override
    public void guardar(DTONotificacion notificacion) {
        // Agregar al inicio (más recientes primero)
        notificacionesCache.add(0, notificacion);
        System.out.println("💾 Notificación guardada: " + notificacion.getId());
    }
    
    @Override
    public void guardarTodas(List<DTONotificacion> notificaciones) {
        notificacionesCache.clear();
        notificacionesCache.addAll(notificaciones);
        System.out.println("💾 " + notificaciones.size() + " notificaciones guardadas");
    }
    
    @Override
    public List<DTONotificacion> obtenerTodas() {
        // Retornar copia para evitar modificaciones externas
        return new ArrayList<>(notificacionesCache);
    }
    
    @Override
    public void remover(String notificacionId) {
        boolean removido = notificacionesCache.removeIf(
            n -> n.getId().equals(notificacionId)
        );
        if (removido) {
            System.out.println("🗑️ Notificación removida: " + notificacionId);
        }
    }
    
    @Override
    public void limpiarCache() {
        notificacionesCache.clear();
        System.out.println("🧹 Caché limpiada");
    }
    
    @Override
    public DTONotificacion buscarPorId(String notificacionId) {
        return notificacionesCache.stream()
            .filter(n -> n.getId().equals(notificacionId))
            .findFirst()
            .orElse(null);
    }
}
```

### Ventajas de esta Arquitectura

✅ **Separación de responsabilidades**
- Repositorio: Solo caché local
- Gestor: Comunicación y lógica de negocio

✅ **Sin dependencias circulares**
- Repositorio solo depende de DTO y Dominio
- No depende de módulos de comunicación

✅ **Fácil de testear**
- Repositorio se puede testear sin servidor
- Gestor se puede testear con mocks

✅ **Caché rápida**
- Acceso instantáneo a notificaciones
- No necesita consultar servidor cada vez

---

## Protocolo JSON

### Tipos de Notificaciones

#### 1. Solicitud de Amistad
```json
{
  "id": "notif-uuid",
  "tipo": "SOLICITUD_AMISTAD",
  "titulo": "Nueva solicitud de amistad",
  "contenido": "Juan Pérez quiere ser tu amigo",
  "fecha": "2025-10-16T10:30:00",
  "leida": false,
  "origenId": "user-uuid-123"
}
```

#### 2. Invitación a Canal
```json
{
  "id": "notif-uuid",
  "tipo": "INVITACION_CANAL",
  "titulo": "Invitación a canal",
  "contenido": "Te invitaron al canal 'Proyecto X'",
  "fecha": "2025-10-16T11:15:00",
  "leida": false,
  "origenId": "canal-uuid-456"
}
```

#### 3. Mención en Mensaje
```json
{
  "id": "notif-uuid",
  "tipo": "MENCION",
  "titulo": "Te mencionaron",
  "contenido": "@tuUsuario en canal 'General': Revisa esto...",
  "fecha": "2025-10-16T14:20:00",
  "leida": false,
  "origenId": "mensaje-uuid-789"
}
```

### Acciones del Cliente al Servidor

#### 1. Obtener Notificaciones
**Petición:**
```json
{
  "action": "obtenerNotificaciones",
  "data": {
    "usuarioId": "mi-user-uuid"
  }
}
```

**Respuesta:**
```json
{
  "status": "success",
  "data": [ /* array de notificaciones */ ]
}
```

#### 2. Responder Solicitud de Amistad
**Petición (Aceptar):**
```json
{
  "action": "responderSolicitudAmistad",
  "data": {
    "solicitudId": "notif-001",
    "usuarioId": "mi-user-uuid",
    "usuarioOrigenId": "user-uuid-123",
    "accion": "ACEPTAR"
  }
}
```

**Petición (Rechazar):**
```json
{
  "action": "responderSolicitudAmistad",
  "data": {
    "solicitudId": "notif-001",
    "usuarioId": "mi-user-uuid",
    "accion": "RECHAZAR"
  }
}
```

#### 3. Responder Invitación a Canal
**Petición (Aceptar):**
```json
{
  "action": "responderInvitacionCanal",
  "data": {
    "invitacionId": "notif-002",
    "usuarioId": "mi-user-uuid",
    "canalId": "canal-uuid-456",
    "accion": "ACEPTAR"
  }
}
```

#### 4. Marcar como Leída
**Petición:**
```json
{
  "action": "marcarNotificacionLeida",
  "data": {
    "notificacionId": "notif-001"
  }
}
```

#### 5. Marcar Todas como Leídas
**Petición:**
```json
{
  "action": "marcarTodasNotificacionesLeidas",
  "data": {
    "usuarioId": "mi-user-uuid"
  }
}
```

---

## Patrón Observer

### Implementación del Patrón

El gestor implementa `ISujeto` para notificar cambios a las vistas:

```java
public class GestorNotificaciones implements ISujeto {
    
    private final List<IObservador> observadores;
    
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("🔔 Observador registrado. Total: " 
                + observadores.size());
        }
    }
    
    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🔕 Observador removido");
    }
    
    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📢 Notificando a " + observadores.size() 
            + " observadores - Tipo: " + tipoDeDato);
        
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
```

### Tipos de Notificaciones a Observadores

| Tipo de Notificación | Datos Enviados | Cuándo se Envía |
|----------------------|----------------|-----------------|
| `NOTIFICACIONES_RECIBIDAS` | `List<DTONotificacion>` | Al obtener todas las notificaciones del servidor |
| `NUEVA_NOTIFICACION` | `DTONotificacion` | Al recibir una notificación en tiempo real |
| `SOLICITUD_AMISTAD_ACEPTADA` | `String` (solicitudId) | Al aceptar una solicitud de amistad |
| `SOLICITUD_AMISTAD_RECHAZADA` | `String` (solicitudId) | Al rechazar una solicitud de amistad |
| `INVITACION_CANAL_ACEPTADA` | `JsonObject` (invitacionId, canalId) | Al aceptar una invitación a canal |
| `INVITACION_CANAL_RECHAZADA` | `String` (invitacionId) | Al rechazar una invitación a canal |
| `CONTACTO_AGREGADO` | `String` (usuarioId) | Al aceptar una solicitud de amistad |
| `CANAL_UNIDO` | `String` (canalId) | Al aceptar una invitación a canal |
| `TODAS_NOTIFICACIONES_LEIDAS` | `null` | Al marcar todas como leídas |
| `NOTIFICACION_REMOVIDA` | `String` (notificacionId) | Al remover una notificación de la caché |
| `TU_SOLICITUD_ACEPTADA` | `JsonObject` (datos del usuario) | Cuando otro usuario acepta tu solicitud |

---

## Ejemplos de Uso

### Ejemplo 1: Vista que Muestra Notificaciones

```java
public class VistaNotificaciones extends VBox implements IObservador {
    
    private final GestorNotificaciones gestorNotificaciones;
    private final VBox listaNotificaciones;
    private final Label badgeContador;
    
    public VistaNotificaciones() {
        this.gestorNotificaciones = new GestorNotificaciones();
        this.listaNotificaciones = new VBox(10);
        this.badgeContador = new Label("0");
        
        // Registrarse como observador
        gestorNotificaciones.registrarObservador(this);
        
        // Inicializar manejadores de tiempo real
        gestorNotificaciones.inicializarManejadores();
        
        inicializarUI();
        
        // Cargar notificaciones del servidor
        cargarNotificaciones();
    }
    
    private void cargarNotificaciones() {
        gestorNotificaciones.obtenerNotificaciones()
            .thenAccept(notificaciones -> {
                Platform.runLater(() -> {
                    System.out.println("✅ " + notificaciones.size() 
                        + " notificaciones cargadas");
                });
            })
            .exceptionally(error -> {
                Platform.runLater(() -> {
                    mostrarError("Error al cargar notificaciones: " 
                        + error.getMessage());
                });
                return null;
            });
    }
    
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "NOTIFICACIONES_RECIBIDAS":
                    List<DTONotificacion> notificaciones = 
                        (List<DTONotificacion>) datos;
                    mostrarNotificaciones(notificaciones);
                    actualizarBadge(notificaciones.size());
                    break;
                    
                case "NUEVA_NOTIFICACION":
                    DTONotificacion notif = (DTONotificacion) datos;
                    agregarNotificacion(notif);
                    incrementarBadge();
                    mostrarAlerta(notif);
                    break;
                    
                case "SOLICITUD_AMISTAD_ACEPTADA":
                    String solicitudId = (String) datos;
                    removerNotificacionDeUI(solicitudId);
                    decrementarBadge();
                    break;
                    
                case "CONTACTO_AGREGADO":
                    String usuarioId = (String) datos;
                    mostrarMensaje("Nuevo contacto agregado");
                    // Actualizar lista de contactos
                    break;
                    
                case "TODAS_NOTIFICACIONES_LEIDAS":
                    limpiarNotificaciones();
                    actualizarBadge(0);
                    break;
            }
        });
    }
    
    private void mostrarNotificaciones(List<DTONotificacion> notificaciones) {
        listaNotificaciones.getChildren().clear();
        
        for (DTONotificacion notif : notificaciones) {
            VBox tarjeta = crearTarjetaNotificacion(notif);
            listaNotificaciones.getChildren().add(tarjeta);
        }
    }
    
    private VBox crearTarjetaNotificacion(DTONotificacion notif) {
        VBox tarjeta = new VBox(5);
        tarjeta.setPadding(new Insets(10));
        tarjeta.setStyle("-fx-background-color: white; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-radius: 5;");
        
        // Título
        Label titulo = new Label(notif.getTitulo());
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        // Contenido
        Label contenido = new Label(notif.getContenido());
        contenido.setWrapText(true);
        
        // Tiempo relativo
        Label tiempo = new Label(notif.getTiempoRelativo());
        tiempo.setTextFill(Color.GRAY);
        tiempo.setFont(Font.font(10));
        
        // Botones de acción según el tipo
        HBox acciones = crearBotonesAccion(notif);
        
        tarjeta.getChildren().addAll(titulo, contenido, tiempo, acciones);
        
        return tarjeta;
    }
    
    private HBox crearBotonesAccion(DTONotificacion notif) {
        HBox acciones = new HBox(10);
        
        switch (notif.getTipo()) {
            case "SOLICITUD_AMISTAD":
                Button btnAceptar = new Button("Aceptar");
                btnAceptar.setStyle("-fx-background-color: #4CAF50; " +
                                   "-fx-text-fill: white;");
                btnAceptar.setOnAction(e -> 
                    aceptarSolicitudAmistad(notif));
                
                Button btnRechazar = new Button("Rechazar");
                btnRechazar.setStyle("-fx-background-color: #f44336; " +
                                    "-fx-text-fill: white;");
                btnRechazar.setOnAction(e -> 
                    rechazarSolicitudAmistad(notif));
                
                acciones.getChildren().addAll(btnAceptar, btnRechazar);
                break;
                
            case "INVITACION_CANAL":
                Button btnUnirse = new Button("Unirse");
                btnUnirse.setStyle("-fx-background-color: #2196F3; " +
                                  "-fx-text-fill: white;");
                btnUnirse.setOnAction(e -> 
                    aceptarInvitacionCanal(notif));
                
                Button btnIgnorar = new Button("Ignorar");
                btnIgnorar.setOnAction(e -> 
                    rechazarInvitacionCanal(notif));
                
                acciones.getChildren().addAll(btnUnirse, btnIgnorar);
                break;
                
            case "MENCION":
                Button btnIrMensaje = new Button("Ver mensaje");
                btnIrMensaje.setOnAction(e -> 
                    navegarAMensaje(notif.getOrigenId()));
                
                acciones.getChildren().add(btnIrMensaje);
                break;
        }
        
        // Botón para marcar como leída
        Button btnMarcarLeida = new Button("Marcar como leída");
        btnMarcarLeida.setStyle("-fx-background-color: transparent; " +
                               "-fx-text-fill: gray;");
        btnMarcarLeida.setOnAction(e -> marcarComoLeida(notif));
        
        acciones.getChildren().add(btnMarcarLeida);
        
        return acciones;
    }
    
    private void aceptarSolicitudAmistad(DTONotificacion notif) {
        gestorNotificaciones.aceptarSolicitudAmistad(
            notif.getId(), 
            notif.getOrigenId()
        ).thenRun(() -> {
            Platform.runLater(() -> {
                mostrarMensaje("Solicitud de amistad aceptada");
            });
        }).exceptionally(error -> {
            Platform.runLater(() -> {
                mostrarError("Error: " + error.getMessage());
            });
            return null;
        });
    }
    
    private void marcarComoLeida(DTONotificacion notif) {
        gestorNotificaciones.marcarComoLeida(notif.getId())
            .thenRun(() -> {
                Platform.runLater(() -> {
                    removerNotificacionDeUI(notif.getId());
                    decrementarBadge();
                });
            });
    }
    
    private void actualizarBadge(int contador) {
        badgeContador.setText(String.valueOf(contador));
        badgeContador.setVisible(contador > 0);
    }
    
    private void incrementarBadge() {
        int actual = Integer.parseInt(badgeContador.getText());
        actualizarBadge(actual + 1);
    }
    
    private void decrementarBadge() {
        int actual = Integer.parseInt(badgeContador.getText());
        if (actual > 0) {
            actualizarBadge(actual - 1);
        }
    }
}
```

### Ejemplo 2: Inicialización en el Main

```java
public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // ... código de inicialización ...
        
        // Inicializar gestor de notificaciones
        GestorNotificaciones gestorNotificaciones = new GestorNotificaciones();
        
        // Inicializar manejadores de tiempo real
        gestorNotificaciones.inicializarManejadores();
        
        // Crear vista de notificaciones
        VistaNotificaciones vistaNotificaciones = new VistaNotificaciones();
        
        // ... resto del código ...
    }
}
```

---

## Resumen de Ventajas de Esta Arquitectura

### ✅ Separación Clara de Responsabilidades
- **Repositorio**: Solo caché local, sin comunicación
- **Gestor**: Comunicación y lógica de negocio
- **Vista**: Presentación y UI

### ✅ Sin Dependencias Circulares
- Repositorio no depende de GestionUsuario
- Repositorio no depende de módulos de comunicación
- Solo depende de DTO y Dominio

### ✅ Patrón Observer Bien Implementado
- Comunicación asíncrona entre capas
- Desacoplamiento temporal
- Múltiples vistas pueden observar el mismo gestor

### ✅ Fácil de Mantener y Extender
- Agregar nuevos tipos de notificaciones es simple
- Modificar la caché no afecta la comunicación
- Testeable en cada capa independientemente

### ✅ Rendimiento Optimizado
- Caché local para acceso rápido
- No consulta el servidor innecesariamente
- Actualizaciones en tiempo real eficientes

---

## Diagrama Final de Flujo Completo

```
┌──────────────────────────────────────────────────────────────────┐
│                         USUARIO                                   │
│                  (Interactúa con la UI)                          │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN                            │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Vista Notificaciones (JavaFX)                              │  │
│  │ - Implementa IObservador                                   │  │
│  │ - Muestra notificaciones en UI                            │  │
│  │ - Maneja clicks de botones Aceptar/Rechazar              │  │
│  └────────────────────────┬───────────────────────────────────┘  │
└───────────────────────────┼──────────────────────────────────────┘
                            │ actualizar(tipo, datos)
                            │
┌───────────────────────────▼──────────────────────────────────────┐
│                   CAPA DE NEGOCIO                                 │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ GestorNotificaciones                                       │  │
│  │ - Implementa ISujeto                                       │  │
│  │ - Maneja comunicación con servidor                        │  │
│  │ - EnviadorPeticiones / GestorRespuesta                   │  │
│  │ - Parsea JSON (Gson)                                      │  │
│  │ - Gestiona observadores                                   │  │
│  │ - Lógica de negocio (validaciones)                       │  │
│  └────────────────────────┬───────────────────────────────────┘  │
└───────────────────────────┼──────────────────────────────────────┘
                            │ guardar() / obtenerTodas()
                            │
┌───────────────────────────▼──────────────────────────────────────┐
│               CAPA DE PERSISTENCIA                                │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ RepositorioNotificacionImpl                                │  │
│  │ - SOLO maneja caché local                                 │  │
│  │ - ArrayList<DTONotificacion>                              │  │
│  │ - NO tiene comunicación con servidor                      │  │
│  │ - Operaciones CRUD simples                                │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                            
                            ▲
                            │ JSON via Sockets TCP
                            │
┌───────────────────────────┴──────────────────────────────────────┐
│                        SERVIDOR                                   │
│  - Procesa peticiones                                            │
│  - Envía respuestas                                              │
│  - Push de notificaciones en tiempo real                         │
│  - Base de datos PostgreSQL                                      │
└──────────────────────────────────────────────────────────────────┘
```

---

## Conclusión

Este sistema de notificaciones implementa una arquitectura limpia y bien estructurada que:

1. **Respeta la separación de responsabilidades** entre capas
2. **Evita dependencias circulares** manteniendo el repositorio simple
3. **Usa el patrón Observer** para comunicación asíncrona eficiente
4. **Maneja comunicación JSON** con el servidor de forma robusta
5. **Proporciona caché local** para rendimiento óptimo
6. **Soporta notificaciones en tiempo real** para mejor experiencia de usuario

La documentación completa del flujo desde peticiones hasta persistencia muestra cómo cada componente interactúa de manera clara y mantenible.
# Documentación Sistema de Notificaciones

## Índice
1. [Visión General](#visión-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Flujo de Peticiones y Respuestas](#flujo-de-peticiones-y-respuestas)
4. [Gestor de Notificaciones](#gestor-de-notificaciones)
5. [Repositorio y Persistencia](#repositorio-y-persistencia)
6. [Protocolo JSON](#protocolo-json)
7. [Patrón Observer](#patrón-observer)
8. [Ejemplos de Uso](#ejemplos-de-uso)

---

## Visión General

El sistema de notificaciones permite a los usuarios:
- ✅ Recibir solicitudes de amistad
- ✅ Recibir invitaciones a canales
- ✅ Recibir menciones en mensajes
- ✅ Aceptar o rechazar solicitudes e invitaciones
- ✅ Marcar notificaciones como leídas
- ✅ Recibir notificaciones en tiempo real

### Componentes Principales

```
┌─────────────────────────────────────────────────────┐
│              CAPA DE PRESENTACIÓN                   │
│  (Vistas JavaFX - Implementan IObservador)         │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│              CAPA DE NEGOCIO                        │
│  GestorNotificaciones (Implementa ISujeto)         │
│  - Comunicación con servidor                        │
│  - Lógica de negocio                               │
│  - Gestión de observadores                          │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│           CAPA DE PERSISTENCIA                      │
│  RepositorioNotificacionImpl                        │
│  - Caché local (ArrayList)                         │
│  - NO tiene comunicación con servidor               │
└─────────────────────────────────────────────────────┘
```

---

## Arquitectura del Sistema

### Separación de Responsabilidades

#### 1. **Repositorio (Persistencia)**
- ✅ **SOLO** maneja almacenamiento en caché local
- ✅ NO tiene comunicación con el servidor
- ✅ NO depende de módulos de comunicación
- ✅ Operaciones simples CRUD en memoria

```java
public interface IRepositorioNotificacion {
    void guardar(DTONotificacion notificacion);
    void guardarTodas(List<DTONotificacion> notificaciones);
    List<DTONotificacion> obtenerTodas();
    void remover(String notificacionId);
    void limpiarCache();
    DTONotificacion buscarPorId(String notificacionId);
}
```

#### 2. **Gestor (Negocio)**
- ✅ Maneja la comunicación con el servidor
- ✅ Implementa lógica de negocio
- ✅ Usa el repositorio para caché
- ✅ Implementa el patrón Observer (ISujeto)
- ✅ Parsea respuestas JSON del servidor

---

## Flujo de Peticiones y Respuestas

### 1. Obtener Notificaciones del Servidor

```
┌────────────┐                ┌──────────────────┐               ┌──────────┐
│   Vista    │                │ GestorNotifica-  │               │ Servidor │
│  (JavaFX)  │                │     ciones       │               │          │
└─────┬──────┘                └────────┬─────────┘               └────┬─────┘
      │                                │                              │
      │ 1. Solicitar notificaciones    │                              │
      │──────────────────────────────>│                              │
      │                                │                              │
      │                                │ 2. Construir petición JSON   │
      │                                │    (EnviadorPeticiones)      │
      │                                │──────────────────────────────>│
      │                                │                              │
      │                                │                              │
      │                                │ 3. Respuesta JSON            │
      │                                │<──────────────────────────────│
      │                                │                              │
      │                                │ 4. Parsear respuesta         │
      │                                │    (Gson)                    │
      │                                │                              │
      │                                │ 5. Guardar en repositorio    │
      │                                │    (caché local)             │
      │                                │                              │
      │ 6. Notificar observadores      │                              │
      │    (Patrón Observer)           │                              │
      │<──────────────────────────────│                              │
      │                                │                              │
      │ 7. Actualizar UI               │                              │
      │                                │                              │
```

### Código Detallado del Flujo

#### Paso 1: Vista solicita notificaciones
```java
// En la Vista (JavaFX)
gestorNotificaciones.obtenerNotificaciones()
    .thenAccept(notificaciones -> {
        Platform.runLater(() -> {
            mostrarNotificacionesEnUI(notificaciones);
        });
    });
```

#### Paso 2: Gestor construye petición JSON
```java
// En GestorNotificaciones
public CompletableFuture<List<DTONotificacion>> obtenerNotificaciones() {
    CompletableFuture<List<DTONotificacion>> future = new CompletableFuture<>();
    
    // Obtener ID del usuario actual
    String usuarioId = gestorSesion.getUserId();
    
    // Construir payload JSON
    JsonObject payload = new JsonObject();
    payload.addProperty("usuarioId", usuarioId);
    
    // Crear petición con acción específica
    DTORequest request = new DTORequest("obtenerNotificaciones", payload);
    
    // Registrar manejador de respuesta (callback)
    gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
        // Este callback se ejecutará cuando llegue la respuesta
        procesarRespuesta(respuesta, future);
    });
    
    // Enviar petición al servidor
    enviadorPeticiones.enviar(request);
    
    return future;
}
```

**Estructura de la petición JSON enviada:**
```json
{
  "action": "obtenerNotificaciones",
  "data": {
    "usuarioId": "uuid-del-usuario-actual"
  }
}
```

#### Paso 3 y 4: Servidor responde y Gestor parsea

**Respuesta del servidor:**
```json
{
  "status": "success",
  "data": [
    {
      "id": "notif-001",
      "tipo": "SOLICITUD_AMISTAD",
      "titulo": "Nueva solicitud de amistad",
      "contenido": "Juan Pérez quiere ser tu amigo",
      "fecha": "2025-10-16T10:30:00",
      "leida": false,
      "origenId": "user-uuid-123"
    },
    {
      "id": "notif-002",
      "tipo": "INVITACION_CANAL",
      "titulo": "Invitación a canal",
      "contenido": "Te invitaron al canal 'Proyecto X'",
      "fecha": "2025-10-16T11:15:00",
      "leida": false,
      "origenId": "canal-uuid-456"
    }
  ]
}
```

**Parseo de la respuesta:**
```java
private void procesarRespuesta(DTOResponse respuesta, CompletableFuture future) {
    if ("success".equals(respuesta.getStatus())) {
        try {
            // Parsear el array de notificaciones
            List<DTONotificacion> notificaciones = parsearNotificaciones(respuesta);
            
            // PASO 5: Guardar en repositorio (caché local)
            repositorioNotificacion.guardarTodas(notificaciones);
            
            System.out.println("✅ " + notificaciones.size() + " notificaciones recibidas");
            
            // PASO 6: Notificar a observadores
            notificarObservadores("NOTIFICACIONES_RECIBIDAS", notificaciones);
            
            // Completar el CompletableFuture
            future.complete(notificaciones);
            
        } catch (Exception e) {
            System.err.println("❌ Error al parsear: " + e.getMessage());
            future.completeExceptionally(e);
        }
    } else {
        String error = "Error: " + respuesta.getMessage();
        future.completeExceptionally(new RuntimeException(error));
    }
}
```

**Método de parseo detallado:**
```java
private List<DTONotificacion> parsearNotificaciones(DTOResponse respuesta) {
    List<DTONotificacion> notificaciones = new ArrayList<>();
    
    if (respuesta.getData() != null) {
        // Convertir Object a JsonElement usando Gson
        JsonElement element = gson.toJsonTree(respuesta.getData());
        JsonArray array = element.getAsJsonArray();
        
        // Formato de fechas ISO
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        // Iterar sobre cada notificación
        for (JsonElement item : array) {
            JsonObject obj = item.getAsJsonObject();
            
            // Extraer campos
            String id = obj.get("id").getAsString();
            String tipo = obj.get("tipo").getAsString();
            String titulo = obj.get("titulo").getAsString();
            String contenido = obj.get("contenido").getAsString();
            LocalDateTime fecha = LocalDateTime.parse(
                obj.get("fecha").getAsString(), 
                formatter
            );
            boolean leida = obj.get("leida").getAsBoolean();
            String origenId = obj.get("origenId").getAsString();
            
            // Crear DTO
            DTONotificacion notif = new DTONotificacion(
                id, tipo, titulo, contenido, fecha, leida, origenId
            );
            
            notificaciones.add(notif);
        }
    }
    
    return notificaciones;
}
```

#### Paso 5: Guardar en Repositorio (Caché Local)

```java
// En RepositorioNotificacionImpl
@Override
public void guardarTodas(List<DTONotificacion> notificaciones) {
    // Limpiar caché anterior
    notificacionesCache.clear();
    
    // Guardar nuevas notificaciones
    notificacionesCache.addAll(notificaciones);
    
    System.out.println("💾 " + notificaciones.size() + " notificaciones guardadas en caché");
}
```

**Estructura de la caché:**
```java
private final List<DTONotificacion> notificacionesCache = new ArrayList<>();
```

---

## 2. Aceptar Solicitud de Amistad

### Flujo Completo

```
┌────────────┐           ┌──────────────────┐           ┌──────────┐
│   Vista    │           │ GestorNotifica-  │           │ Servidor │
└─────┬──────┘           │     ciones       │           └────┬─────┘
      │                  └────────┬─────────┘                │
      │ 1. Clic "Aceptar"         │                          │
      │──────────────────────────>│                          │
      │                           │                          │
      │                           │ 2. Construir petición    │
      │                           │    JSON con acción       │
      │                           │    "responderSolicitud-  │
      │                           │     Amistad"             │
      │                           │──────────────────────────>│
      │                           │                          │
      │                           │                          │
      │                           │ 3. Servidor procesa:     │
      │                           │    - Agrega contacto     │
      │                           │    - Elimina notif       │
      │                           │<──────────────────────────│
      │                           │                          │
      │                           │ 4. Remover de caché      │
      │                           │    local                 │
      │                           │                          │
      │ 5. Notificar observadores │                          │
      │    "CONTACTO_AGREGADO"    │                          │
      │<──────────────────────────│                          │
      │                           │                          │
```

### Código Detallado

```java
public CompletableFuture<Void> aceptarSolicitudAmistad(
    String solicitudId, 
    String usuarioOrigenId
) {
    System.out.println("✅ Aceptando solicitud de amistad");
    
    // Validación de negocio
    if (solicitudId == null || solicitudId.trim().isEmpty()) {
        return CompletableFuture.failedFuture(
            new IllegalArgumentException("ID de solicitud inválido")
        );
    }
    
    CompletableFuture<Void> future = new CompletableFuture<>();
    String usuarioId = gestorSesion.getUserId();
    
    // Construir payload con toda la información necesaria
    JsonObject payload = new JsonObject();
    payload.addProperty("solicitudId", solicitudId);
    payload.addProperty("usuarioId", usuarioId);
    payload.addProperty("usuarioOrigenId", usuarioOrigenId);
    payload.addProperty("accion", "ACEPTAR");
    
    // Crear petición
    DTORequest request = new DTORequest("responderSolicitudAmistad", payload);
    
    // Registrar manejador de respuesta
    gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
        if ("success".equals(respuesta.getStatus())) {
            System.out.println("✅ Solicitud aceptada exitosamente");
            
            // Remover notificación de caché local
            repositorioNotificacion.remover(solicitudId);
            
            // Notificar a observadores
            notificarObservadores("SOLICITUD_AMISTAD_ACEPTADA", solicitudId);
            notificarObservadores("CONTACTO_AGREGADO", usuarioOrigenId);
            
            future.complete(null);
        } else {
            String error = "Error al aceptar: " + respuesta.getMessage();
            System.err.println("❌ " + error);
            future.completeExceptionally(new RuntimeException(error));
        }
    });
    
    // Enviar petición al servidor
    enviadorPeticiones.enviar(request);
    
    return future;
}
```

**Petición JSON enviada:**
```json
{
  "action": "responderSolicitudAmistad",
  "data": {
    "solicitudId": "notif-001",
    "usuarioId": "mi-user-uuid",

