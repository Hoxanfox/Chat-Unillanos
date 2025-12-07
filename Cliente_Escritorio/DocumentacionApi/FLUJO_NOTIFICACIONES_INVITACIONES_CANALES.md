# Flujo de Notificaciones de Invitaciones a Canales

## 📋 Resumen

Este documento explica el flujo completo desde que el servidor envía una invitación a canal hasta que se actualiza en la interfaz de usuario.

---

## 🔄 Flujo Completo

### 1️⃣ **Servidor envía notificación PUSH**

Cuando un usuario invita a otro a un canal, el servidor envía un mensaje PUSH:

```json
{
  "action": "notificacionInvitacionCanal",
  "status": "success",
  "message": "Has sido invitado a un grupo",
  "data": {
    "invitacionId": "uuid-invitacion",
    "grupoId": "id-del-canal",
    "peerGrupoId": "id-del-canal",
    "nombreCanal": "Nombre del Canal",
    "invitadoPor": {
      "id": "id-del-invitador",
      "peerId": "peer-id"
    },
    "fechaInvitacion": "2025-10-16T10:35:00Z"
  }
}
```

---

### 2️⃣ **GestorRespuesta recibe el mensaje**

El `GestorRespuesta` (singleton) recibe el mensaje del servidor y lo dirige al manejador registrado.

**Ubicación:** `Persistencia/Comunicacion`

---

### 3️⃣ **GestorNotificaciones procesa la invitación**

El `GestorNotificaciones` tiene registrado el manejador `notificacionInvitacionCanal`:

```java
gestorRespuesta.registrarManejador("notificacionInvitacionCanal", this::manejarInvitacionCanal);
```

**Método `manejarInvitacionCanal`:**
1. ✅ Extrae los datos de la invitación
2. ✅ Crea un `DTONotificacion` con tipo `"INVITACION_CANAL"`
3. ✅ Guarda la notificación en el **repositorio local (caché)**
4. ✅ Notifica a sus observadores con el evento `"NUEVA_NOTIFICACION"`

**Ubicación:** `Negocio/GestionNotificaciones/GestorNotificaciones.java`

---

### 4️⃣ **ServicioNotificaciones recibe la notificación**

El `ServicioNotificaciones` está registrado como observador del `GestorNotificaciones`.

**Método `actualizar`:**
- Recibe `"NUEVA_NOTIFICACION"` con el `DTONotificacion`
- Llama a `solicitarActualizacionNotificaciones()` para refrescar la lista completa
- Notifica a sus observadores (la UI)

**Ubicación:** `Negocio/Servicio/ServicioNotificacionesImpl.java`

---

### 5️⃣ **FeatureNotificaciones actualiza la UI**

El componente `FeatureNotificaciones` está registrado como observador del `ServicioNotificaciones`.

**Método `actualizar`:**
- Recibe `"ACTUALIZAR_NOTIFICACIONES"` con la lista de notificaciones
- Usa `Platform.runLater()` para actualizar la UI en el hilo correcto
- Llama a `cargarNotificaciones()` que:
  - Limpia el contenedor
  - Filtra solo `"INVITACION_CANAL"`
  - Crea tarjetas con botones Aceptar/Rechazar

**Ubicación:** `Presentacion/InterfazEscritorio/featureNotificaciones/FeatureNotificaciones.java`

---

## 🔥 **NUEVO: Integración con GestorSincronizacionGlobal**

### 6️⃣ **SIGNAL_UPDATE del servidor**

Cuando hay cambios en el servidor (por ejemplo, alguien más respondió a una invitación), el servidor envía:

```json
{
  "action": "SIGNAL_UPDATE",
  "status": "success",
  "resource": "canales"  // o "notificaciones"
}
```

---

### 7️⃣ **GestorSincronizacionGlobal dispara actualizaciones**

El `GestorSincronizacionGlobal` captura `SIGNAL_UPDATE` y dispara actualizaciones globales:

```java
// Notifica a TODOS los observadores registrados
notificarObservadores("ACTUALIZAR_NOTIFICACIONES", null);
notificarObservadores("ACTUALIZAR_CANALES", null);
notificarObservadores("ACTUALIZAR_CONTACTOS", null);
// etc.
```

**Ubicación:** `Negocio/GestionNotificaciones/GestorSincronizacionGlobal.java`

---

### 8️⃣ **GestorNotificaciones responde a SIGNAL_UPDATE**

El `GestorNotificaciones` **AHORA implementa IObservador** y está registrado en el `GestorSincronizacionGlobal`.

**Método `actualizar` (nuevo):**
```java
@Override
public void actualizar(String tipoDeDato, Object datos) {
    if ("ACTUALIZAR_NOTIFICACIONES".equals(tipoDeDato)) {
        // Obtener notificaciones del caché local
        List<DTONotificacion> notificacionesCache = repositorioNotificacion.obtenerTodas();
        
        // Notificar a los observadores (ServicioNotificaciones -> UI)
        notificarObservadores("ACTUALIZAR_NOTIFICACIONES", notificacionesCache);
    }
}
```

---

## 📊 Diagrama del Flujo

```
SERVIDOR
   │
   │ (1) notificacionInvitacionCanal (PUSH)
   ▼
GestorRespuesta
   │
   │ (2) Enruta según "action"
   ▼
GestorNotificaciones.manejarInvitacionCanal()
   │
   ├─► (3a) Guarda en RepositorioNotificacion (caché)
   │
   └─► (3b) notificarObservadores("NUEVA_NOTIFICACION", dto)
         │
         ▼
   ServicioNotificaciones.actualizar()
         │
         ├─► (4a) solicitarActualizacionNotificaciones()
         │
         └─► (4b) notificarObservadores("ACTUALIZAR_NOTIFICACIONES", lista)
               │
               ▼
         FeatureNotificaciones.actualizar()
               │
               └─► (5) Platform.runLater() → cargarNotificaciones() → UI actualizada ✅


SERVIDOR (cuando hay cambios)
   │
   │ (6) SIGNAL_UPDATE (PUSH)
   ▼
GestorSincronizacionGlobal
   │
   │ (7) notificarObservadores("ACTUALIZAR_NOTIFICACIONES", null)
   ▼
GestorNotificaciones.actualizar()  ← NUEVO ✨
   │
   │ (8) Obtiene del caché y notifica
   ▼
ServicioNotificaciones.actualizar()
   │
   └─► FeatureNotificaciones.actualizar() → UI actualizada ✅
```

---

## ✅ Cambios Implementados

### **GestorNotificaciones** ahora:
1. ✅ **Implementa `IObservador`** - puede recibir actualizaciones
2. ✅ **Se registra en `GestorSincronizacionGlobal`** - en el constructor
3. ✅ **Responde a `ACTUALIZAR_NOTIFICACIONES`** - método `actualizar()`
4. ✅ **Propaga las notificaciones del caché** - hacia ServicioNotificaciones → UI

---

## 🎯 Ventajas de este diseño

1. **Actualizaciones automáticas**: Cuando el servidor envía `SIGNAL_UPDATE`, todas las notificaciones se refrescan
2. **Caché local**: Las invitaciones recibidas por PUSH se guardan localmente
3. **Sincronización global**: Un solo punto centralizado (`GestorSincronizacionGlobal`) maneja todas las actualizaciones
4. **Desacoplamiento**: Cada capa tiene su responsabilidad clara
5. **Thread-safe UI**: `Platform.runLater()` garantiza que la UI se actualice en el hilo correcto

---

## 🔧 Inicialización requerida

Para que todo funcione, se debe inicializar en este orden:

```java
// 1. Inicializar GestorSincronizacionGlobal
GestorSincronizacionGlobal.getInstancia().inicializar();

// 2. Crear GestorNotificaciones (se auto-registra)
GestorNotificaciones gestor = new GestorNotificaciones();

// 3. Inicializar sus manejadores PUSH
gestor.inicializarManejadores();

// 4. Crear ServicioNotificaciones (se registra en GestorNotificaciones)
ServicioNotificaciones servicio = new ServicioNotificacionesImpl();

// 5. La UI se registra en el Controlador/Servicio
FeatureNotificaciones feature = new FeatureNotificaciones(controlador);
```

---

## 📝 Notas Importantes

- **No duplicar manejadores**: Solo `GestorNotificaciones` debe manejar `notificacionInvitacionCanal`
- **Repositorio local**: Las notificaciones se guardan en memoria/cache, no en BD (por ahora)
- **SIGNAL_UPDATE es global**: Actualiza todo, no solo notificaciones
- **El servidor NO tiene endpoint de listar notificaciones**: Por eso usamos el caché local

---

## 🐛 Troubleshooting

### ❌ "No se actualizan las invitaciones"
- ✅ Verificar que `GestorNotificaciones` esté registrado en `GestorSincronizacionGlobal`
- ✅ Verificar que `inicializarManejadores()` se haya llamado
- ✅ Verificar logs: buscar "🔔 [GestorNotificaciones]: Nueva invitación a canal"

### ❌ "Error en UI Thread"
- ✅ Verificar que se use `Platform.runLater()` en `FeatureNotificaciones.actualizar()`

### ❌ "Notificaciones duplicadas"
- ✅ Verificar que solo `GestorNotificaciones` maneje `notificacionInvitacionCanal`
- ✅ Remover manejadores en `GestorNotificacionesCanal` y `GestorInvitacionesImpl`

---

**Fecha de actualización:** 2025-11-28
**Versión:** 2.0

