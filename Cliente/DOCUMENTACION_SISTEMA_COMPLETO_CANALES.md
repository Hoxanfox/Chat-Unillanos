# 📚 DOCUMENTACIÓN COMPLETA - SISTEMA DE GESTIÓN DE CANALES

## 📋 Índice
1. [Arquitectura General](#arquitectura-general)
2. [Patrón Observer](#patrón-observer)
3. [Sistema de Persistencia](#sistema-de-persistencia)
4. [Protocolo de Comunicación](#protocolo-de-comunicación)
5. [Flujos Completos](#flujos-completos)

---

## 🏗️ Arquitectura General

### Capas del Sistema
```
┌─────────────────────────────────────────────────┐
│         PRESENTACIÓN (UI + Controladores)       │
│  - Implementa IObservador                       │
│  - Recibe notificaciones del negocio            │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│         NEGOCIO (Lógica de Negocio)             │
│  - Implementa ISujeto                           │
│  - Gestiona peticiones/respuestas               │
│  - Orquesta persistencia                        │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│         PERSISTENCIA (Repositorios)             │
│  - Base de datos H2 local                       │
│  - Operaciones asíncronas                       │
└─────────────────────────────────────────────────┘
```

---

## 🔔 Patrón Observer

### Flujo de Notificaciones

```
SERVIDOR → GestorRespuesta → Gestor de Negocio → Observadores (UI)
                                    ↓
                            Persistencia Local
```

### Tipos de Notificaciones

| Tipo de Notificación | Descripción | Datos Incluidos |
|---------------------|-------------|-----------------|
| `CANALES_ACTUALIZADOS` | Lista de canales actualizada | `List<Canal>` |
| `CANAL_CREADO` | Nuevo canal creado exitosamente | `Canal` |
| `NUEVA_INVITACION_CANAL` | Invitación a canal recibida | `Map<String, String>` |
| `NUEVO_MIEMBRO_EN_CANAL` | Miembro agregado al canal | `Map<String, String>` |
| `MIEMBROS_ACTUALIZADOS` | Lista de miembros actualizada | `List<DTOMiembroCanal>` |
| `MENSAJE_CANAL_RECIBIDO` | Nuevo mensaje en canal | `DTOMensajeCanal` |
| `HISTORIAL_CANAL_RECIBIDO` | Historial de mensajes | `List<DTOMensajeCanal>` |
| `ERROR_OPERACION` | Error en operación | `String` (mensaje error) |

### Implementación en UI

```java
public class VentanaChat implements IObservador {
    
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "CANALES_ACTUALIZADOS":
                    List<Canal> canales = (List<Canal>) datos;
                    actualizarListaCanales(canales);
                    break;
                    
                case "MENSAJE_CANAL_RECIBIDO":
                    DTOMensajeCanal mensaje = (DTOMensajeCanal) datos;
                    mostrarNuevoMensaje(mensaje);
                    break;
                    
                case "NUEVA_INVITACION_CANAL":
                    Map<String, String> invitacion = (Map<String, String>) datos;
                    mostrarNotificacionInvitacion(invitacion);
                    break;
                    
                case "ERROR_OPERACION":
                    String error = (String) datos;
                    mostrarError(error);
                    break;
            }
        });
    }
}
```

---

## 💾 Sistema de Persistencia

### Estructura de Base de Datos

#### Tabla: `canales`
```sql
CREATE TABLE canales (
    id_canal UUID PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL UNIQUE,
    id_administrador UUID,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Tabla: `canal_usuario` (Relación muchos-a-muchos)
```sql
CREATE TABLE canal_usuario (
    id_canal_usuario UUID PRIMARY KEY,
    id_canal UUID NOT NULL,
    id_usuario UUID NOT NULL,
    rol VARCHAR(50) DEFAULT 'miembro',
    fecha_union TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_canal, id_usuario)
);
```

#### Tabla: `mensaje_enviado_canal`
```sql
CREATE TABLE mensaje_enviado_canal (
    id_mensaje_enviado_canal UUID PRIMARY KEY,
    contenido BLOB,
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo VARCHAR(50),
    id_remitente UUID NOT NULL,
    id_destinatario_canal UUID NOT NULL
);
```

#### Tabla: `mensaje_recibido_canal`
```sql
CREATE TABLE mensaje_recibido_canal (
    id_mensaje UUID PRIMARY KEY,
    contenido BLOB,
    fecha_envio TIMESTAMP,
    tipo VARCHAR(50),
    id_destinatario UUID NOT NULL,
    id_remitente_canal UUID NOT NULL
);
```

### Operaciones de Repositorio

```java
// Guardar canal localmente
CompletableFuture<Boolean> guardar(Canal canal);

// Sincronizar canales del servidor
CompletableFuture<Void> sincronizarCanales(List<Canal> canalesDelServidor);

// Guardar mensaje en canal
CompletableFuture<Boolean> guardarMensaje(MensajeCanal mensaje);

// Obtener historial de canal
CompletableFuture<List<MensajeCanal>> obtenerHistorialCanal(String canalId, int limite);
```

---

## 📡 Protocolo de Comunicación

### 1. CREAR CANAL

#### Petición al Servidor
```json
{
  "action": "crearCanal",
  "data": {
    "creadorId": "123e4567-e89b-12d3-a456-426614174000",
    "nombre": "Canal General",
    "descripcion": "Canal para discusiones generales"
  }
}
```

#### Respuesta del Servidor (Éxito)
```json
{
  "status": "success",
  "message": "Canal creado exitosamente",
  "data": {
    "id": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "nombre": "Canal General",
    "creadorId": "123e4567-e89b-12d3-a456-426614174000",
    "fechaCreacion": "2025-10-16T10:30:00Z"
  }
}
```

#### Respuesta del Servidor (Error)
```json
{
  "status": "error",
  "message": "El nombre del canal ya existe",
  "data": null
}
```

---

### 2. LISTAR CANALES

#### Petición al Servidor
```json
{
  "action": "listarCanales",
  "data": {
    "usuarioId": "123e4567-e89b-12d3-a456-426614174000",
    "limite": 100,
    "offset": 0
  }
}
```

#### Respuesta del Servidor
```json
{
  "status": "success",
  "message": "Lista de canales obtenida",
  "data": [
    {
      "id": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
      "nombre": "Canal General",
      "creadorId": "123e4567-e89b-12d3-a456-426614174000",
      "cantidadMiembros": 25
    },
    {
      "id": "111fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
      "nombre": "Canal de Proyectos",
      "creadorId": "456e7890-e89b-12d3-a456-426614174111",
      "cantidadMiembros": 12
    }
  ]
}
```

---

### 3. INVITAR MIEMBRO A CANAL

#### Petición al Servidor
```json
{
  "action": "invitarMiembroCanal",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioIdInvitador": "123e4567-e89b-12d3-a456-426614174000",
    "usuarioIdInvitado": "789e4567-e89b-12d3-a456-426614174222"
  }
}
```

#### Respuesta del Servidor
```json
{
  "status": "success",
  "message": "Invitación enviada exitosamente",
  "data": {
    "invitacionId": "aaa1234b-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioInvitado": "789e4567-e89b-12d3-a456-426614174222",
    "estado": "pendiente"
  }
}
```

#### Notificación Push al Usuario Invitado
```json
{
  "action": "notificacionInvitacionCanal",
  "status": "success",
  "message": "Has sido invitado a un canal",
  "data": {
    "invitacionId": "aaa1234b-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "nombreCanal": "Canal General",
    "invitadoPor": "Juan Pérez",
    "fechaInvitacion": "2025-10-16T10:35:00Z"
  }
}
```

---

### 4. ACEPTAR INVITACIÓN

#### Petición al Servidor
```json
{
  "action": "aceptarInvitacionCanal",
  "data": {
    "invitacionId": "aaa1234b-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioId": "789e4567-e89b-12d3-a456-426614174222"
  }
}
```

#### Respuesta del Servidor
```json
{
  "status": "success",
  "message": "Te has unido al canal exitosamente",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "nombreCanal": "Canal General",
    "rol": "miembro"
  }
}
```

#### Notificación Push a Miembros del Canal
```json
{
  "action": "nuevoMiembro",
  "status": "success",
  "message": "Nuevo miembro se ha unido al canal",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioId": "789e4567-e89b-12d3-a456-426614174222",
    "nombreUsuario": "María García",
    "rol": "miembro",
    "fechaUnion": "2025-10-16T10:40:00Z"
  }
}
```

---

### 5. LISTAR MIEMBROS DEL CANAL

#### Petición al Servidor
```json
{
  "action": "listarMiembrosCanal",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioId": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

#### Respuesta del Servidor
```json
{
  "status": "success",
  "message": "Lista de miembros obtenida",
  "data": [
    {
      "usuarioId": "123e4567-e89b-12d3-a456-426614174000",
      "nombre": "Juan Pérez",
      "rol": "administrador",
      "estado": "activo",
      "fechaUnion": "2025-10-15T09:00:00Z"
    },
    {
      "usuarioId": "789e4567-e89b-12d3-a456-426614174222",
      "nombre": "María García",
      "rol": "miembro",
      "estado": "activo",
      "fechaUnion": "2025-10-16T10:40:00Z"
    }
  ]
}
```

---

### 6. ENVIAR MENSAJE A CANAL

#### Petición al Servidor (Mensaje de Texto)
```json
{
  "action": "enviarMensajeCanal",
  "data": {
    "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "tipo": "texto",
    "contenido": "Hola a todos, ¿cómo están?"
  }
}
```

#### Petición al Servidor (Mensaje de Audio)
```json
{
  "action": "enviarMensajeCanal",
  "data": {
    "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "tipo": "audio",
    "fileId": "audio_abc123xyz789"
  }
}
```

#### Respuesta del Servidor
```json
{
  "status": "success",
  "message": "Mensaje enviado",
  "data": {
    "mensajeId": "msg-123abc-456def-789ghi",
    "fechaEnvio": "2025-10-16T10:45:00Z"
  }
}
```

#### Notificación Push a Miembros del Canal
```json
{
  "action": "nuevoMensajeCanal",
  "status": "success",
  "message": "Nuevo mensaje en canal",
  "data": {
    "mensajeId": "msg-123abc-456def-789ghi",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
    "nombreRemitente": "Juan Pérez",
    "tipo": "texto",
    "contenido": "Hola a todos, ¿cómo están?",
    "fechaEnvio": "2025-10-16T10:45:00Z"
  }
}
```

---

### 7. SOLICITAR HISTORIAL DE CANAL

#### Petición al Servidor
```json
{
  "action": "solicitarHistorialCanal",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioId": "123e4567-e89b-12d3-a456-426614174000",
    "limite": 50,
    "offset": 0
  }
}
```

#### Respuesta del Servidor
```json
{
  "status": "success",
  "message": "Historial de mensajes obtenido",
  "data": [
    {
      "mensajeId": "msg-001",
      "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
      "nombreRemitente": "Juan Pérez",
      "tipo": "texto",
      "contenido": "Bienvenidos al canal",
      "fechaEnvio": "2025-10-16T09:00:00Z"
    },
    {
      "mensajeId": "msg-002",
      "remitenteId": "789e4567-e89b-12d3-a456-426614174222",
      "nombreRemitente": "María García",
      "tipo": "texto",
      "contenido": "Gracias por la invitación",
      "fechaEnvio": "2025-10-16T09:05:00Z"
    },
    {
      "mensajeId": "msg-003",
      "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
      "nombreRemitente": "Juan Pérez",
      "tipo": "audio",
      "fileId": "audio_abc123xyz789",
      "fechaEnvio": "2025-10-16T09:10:00Z"
    }
  ]
}
```

---

## 🔄 Flujos Completos

### FLUJO 1: Crear Canal y Persistir Localmente

```
1. Usuario hace clic en "Crear Canal"
   ↓
2. UI llama: gestorCanales.crearCanal("Canal General", "Descripción")
   ↓
3. CreadorCanal envía petición al servidor
   ↓
4. Servidor responde con datos del canal creado
   ↓
5. GestorRespuesta invoca el callback registrado
   ↓
6. CreadorCanal persiste el canal localmente
   RepositorioCanal.guardar(canal)
   ↓
7. CreadorCanal notifica a observadores (UI)
   notificarObservadores("CANAL_CREADO", canal)
   ↓
8. UI recibe actualización y muestra el nuevo canal
```

### FLUJO 2: Recibir Mensaje de Canal

```
1. Servidor envía notificación push de nuevo mensaje
   ↓
2. GestorRespuesta recibe la notificación
   ↓
3. GestorMensajesCanal procesa el mensaje
   ↓
4. Persiste el mensaje localmente
   RepositorioMensajeCanal.guardar(mensaje)
   ↓
5. Notifica a observadores
   notificarObservadores("MENSAJE_CANAL_RECIBIDO", mensaje)
   ↓
6. UI actualiza la vista del chat en tiempo real
```

### FLUJO 3: Sincronizar Canales al Conectar

```
1. Usuario inicia sesión exitosamente
   ↓
2. Sistema solicita lista de canales
   ↓
3. Servidor responde con todos los canales del usuario
   ↓
4. ListadorCanales sincroniza con DB local
   - Compara canales del servidor con los locales
   - Inserta nuevos canales
   - Actualiza canales existentes
   - Elimina canales que ya no existen
   ↓
5. Notifica a UI con lista actualizada
   notificarObservadores("CANALES_ACTUALIZADOS", canales)
   ↓
6. UI muestra la lista sincronizada
```

---

## 🛠️ Implementación Técnica

### Gestor de Mensajes de Canal con Observer y Persistencia

```java
public class GestorMensajesCanal implements IGestorMensajesCanal {
    
    private final List<IObservador> observadores = new CopyOnWriteArrayList<>();
    private final IRepositorioMensajeCanal repositorioMensajes;
    private final IGestorRespuesta gestorRespuesta;
    
    public GestorMensajesCanal(IRepositorioMensajeCanal repositorio) {
        this.repositorioMensajes = repositorio;
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        
        // Registrar manejadores de respuestas del servidor
        inicializarManejadores();
    }
    
    private void inicializarManejadores() {
        // Manejador para nuevos mensajes (notificación push)
        gestorRespuesta.registrarManejador("nuevoMensajeCanal", 
            this::manejarNuevoMensaje);
        
        // Manejador para historial de mensajes
        gestorRespuesta.registrarManejador("respuestaHistorialCanal", 
            this::manejarHistorial);
        
        // Manejador para confirmación de envío
        gestorRespuesta.registrarManejador("enviarMensajeCanal", 
            this::manejarConfirmacionEnvio);
    }
    
    private void manejarNuevoMensaje(DTOResponse respuesta) {
        if (!respuesta.fueExitoso()) return;
        
        try {
            DTOMensajeCanal mensaje = gson.fromJson(
                gson.toJson(respuesta.getData()), 
                DTOMensajeCanal.class
            );
            
            // Persistir mensaje recibido localmente
            repositorioMensajes.guardarMensajeRecibido(mensaje)
                .thenAccept(guardado -> {
                    if (guardado) {
                        // Notificar a la UI
                        notificarObservadores("MENSAJE_CANAL_RECIBIDO", mensaje);
                    }
                })
                .exceptionally(ex -> {
                    notificarObservadores("ERROR_OPERACION", 
                        "Error al guardar mensaje: " + ex.getMessage());
                    return null;
                });
                
        } catch (Exception e) {
            System.err.println("Error procesando nuevo mensaje: " + e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<Void> enviarMensaje(String canalId, String contenido) {
        String remitenteId = GestorSesionUsuario.getInstancia().getUserId();
        
        DTOEnviarMensajeCanal payload = new DTOEnviarMensajeCanal(
            remitenteId, canalId, "texto", contenido
        );
        
        DTORequest peticion = new DTORequest("enviarMensajeCanal", payload);
        
        // Persistir mensaje enviado localmente (estado: pendiente)
        return repositorioMensajes.guardarMensajeEnviado(payload)
            .thenCompose(guardado -> {
                // Enviar al servidor
                enviadorPeticiones.enviar(peticion);
                return CompletableFuture.completedFuture(null);
            });
    }
    
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }
    
    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador obs : observadores) {
            obs.actualizar(tipoDeDato, datos);
        }
    }
}
```

---

## 📊 Diagramas de Secuencia

### Enviar Mensaje a Canal

```
UI → Controlador → GestorMensajesCanal → RepositorioMensajes → H2
                          ↓
                   EnviadorPeticiones → Servidor
                          ↓
                   GestorRespuesta (callback)
                          ↓
                   notificarObservadores() → UI actualiza
```

### Recibir Mensaje de Canal (Push)

```
Servidor → GestorRespuesta → GestorMensajesCanal → RepositorioMensajes
                                      ↓
                              notificarObservadores()
                                      ↓
                                     UI
```

---

## ✅ Checklist de Implementación

- [x] Interfaces IObservador e ISujeto
- [x] Implementación de repositorios asíncronos
- [x] Gestores de negocio con Observer
- [x] Manejadores de respuestas del servidor
- [x] Persistencia de mensajes de canal
- [x] Sincronización de datos servidor-local
- [x] Notificaciones push procesadas
- [ ] Implementar repositorio de mensajes de canal
- [ ] Crear DTOs para mensajes de canal
- [ ] UI con implementación de IObservador

---

## 🔐 Manejo de Errores

### Errores del Servidor
```json
{
  "status": "error",
  "message": "No tienes permisos para enviar mensajes en este canal",
  "data": {
    "codigo": "PERMISSION_DENIED",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d"
  }
}
```

### Errores Locales
- **Sin conexión**: Los mensajes se guardan localmente y se sincronizan al reconectar
- **Error de persistencia**: Se notifica al usuario y se reintenta
- **Sesión expirada**: Se redirige al login

---

## 📝 Notas Finales

1. **Todas las operaciones son asíncronas** para no bloquear la UI
2. **Los observadores se notifican en el hilo de UI** (JavaFX Platform.runLater)
3. **La persistencia local funciona offline** y se sincroniza al reconectar
4. **Los mensajes se guardan antes de enviar** para garantizar no pérdida de datos
5. **Las notificaciones push se procesan automáticamente** sin polling

---

📅 **Última actualización**: 16 de Octubre, 2025
👨‍💻 **Autor**: Sistema de Gestión de Canales - Chat Unillanos

