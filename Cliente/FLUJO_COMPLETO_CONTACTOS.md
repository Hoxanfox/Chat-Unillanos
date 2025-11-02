0# Flujo Completo: Gestión de Contactos con Sesión

## 📊 Arquitectura de Llamadas

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUJO DE INICIALIZACIÓN                      │
└─────────────────────────────────────────────────────────────────┘

1. Usuario inicia sesión
   └─> GestorSesionUsuario.establecerSesion(userId, token)
       └─> Sesión almacenada en memoria

2. Se crea FachadaContactosImpl
   └─> Constructor:
       ├─> new GestionContactosImpl()
       ├─> gestorSesion = GestorSesionUsuario.getInstancia()
       ├─> if (gestorSesion.haySesionActiva())
       │   └─> String usuarioId = gestorSesion.getUserId()
       │   └─> gestionContactos.setUsuarioId(usuarioId) ✅
       └─> Observadores registrados

┌─────────────────────────────────────────────────────────────────┐
│              FLUJO REQUEST: Solicitar Contactos                 │
└─────────────────────────────────────────────────────────────────┘

Servicio/UI
   │
   │ solicitarActualizacionContactos()
   ├──────────────────────────────────────────────────────────────>
   │                                                 FachadaContactosImpl
   │                                                        │
   │                                                        │ 1. gestorSesion.haySesionActiva()
   │                                                        │ 2. usuarioId = gestorSesion.getUserId()
   │                                                        │ 3. gestionContactos.setUsuarioId(usuarioId)
   │                                                        │
   │                                                        │ solicitarActualizacionContactos()
   │                                                        ├─────────────────────────────────>
   │                                                        │                  GestionContactosImpl
   │                                                        │                         │
   │                                                        │                         │ 4. Crear DTORequest:
   │                                                        │                         │    {
   │                                                        │                         │      "action": "listarContactos",
   │                                                        │                         │      "data": {
   │                                                        │                         │        "usuarioId": "usuario-123"
   │                                                        │                         │      }
   │                                                        │                         │    }
   │                                                        │                         │
   │                                                        │                         │ enviadorPeticiones.enviar()
   │                                                        │                         └──────────────────────────>
   │                                                        │                                        SERVIDOR
   │                                                        │                                           │
   │                                                        │                         manejarRespuestaListarContactos()
   │                                                        │                         <─────────────────────────
   │                                                        │                         │
   │                                                        │                         │ 5. Respuesta:
   │                                                        │                         │    {
   │                                                        │                         │      "action": "listarContactos",
   │                                                        │                         │      "status": "success",
   │                                                        │                         │      "data": [contactos...]
   │                                                        │                         │    }
   │                                                        │                         │
   │                                                        │                         │ 6. procesarListaContactos()
   │                                                        │                         │    ├─> Actualizar caché
   │                                                        │                         │    └─> notificarObservadores("ACTUALIZAR_CONTACTOS")
   │                                                        │                         │
   │                                                        │    actualizar()         │
   │                                                        │<────────────────────────┘
   │                                                        │
   │                                                        │ 7. notificarObservadores()
   │    actualizar("ACTUALIZAR_CONTACTOS", contactos)      │
   │<───────────────────────────────────────────────────────┘
   │
   │ 8. Actualizar UI con la lista de contactos
   └─>

┌─────────────────────────────────────────────────────────────────┐
│           FLUJO PUSH: Actualización Automática                  │
└─────────────────────────────────────────────────────────────────┘

SERVIDOR
   │
   │ Evento: Contacto cambia estado (ONLINE/OFFLINE)
   │
   │ Push: "solicitarListaContactos"
   └──────────────────────────────────────────────────────────────>
                                                    GestionContactosImpl
                                                           │
                                                           │ manejarPushActualizacionContactos()
                                                           │
                                                           │ 1. Recibe:
                                                           │    {
                                                           │      "action": "solicitarListaContactos",
                                                           │      "status": "success",
                                                           │      "data": [contactos actualizados...]
                                                           │    }
                                                           │
                                                           │ 2. procesarListaContactos()
                                                           │    ├─> Actualizar caché automáticamente
                                                           │    └─> notificarObservadores("ACTUALIZAR_CONTACTOS")
                                                           │
                                                           │ actualizar()
                                                           ├──────────────────────────>
                                                           │            FachadaContactosImpl
                                                           │                   │
                                                           │                   │ notificarObservadores()
                                                           │                   └────────────────────>
                                                           │                              Servicio/UI
                                                           │                                  │
                                                           │                                  │ 3. Actualizar UI automáticamente
                                                           │                                  └─>
```

---

## 🔍 Detalles de Implementación

### 1. **FachadaContactosImpl.java**

#### Constructor
```java
public FachadaContactosImpl() {
    this.gestionContactos = new GestionContactosImpl();
    this.gestorSesion = GestorSesionUsuario.getInstancia();
    
    // ✅ Establecer automáticamente el usuario de la sesión activa
    if (gestorSesion.haySesionActiva()) {
        String usuarioId = gestorSesion.getUserId();
        ((GestionContactosImpl) gestionContactos).setUsuarioId(usuarioId);
        System.out.println("✅ Usuario de sesión establecido: " + usuarioId);
    }
}
```

#### Método solicitarActualizacionContactos()
```java
@Override
public void solicitarActualizacionContactos() {
    // ✅ Verificar y actualizar el usuario antes de solicitar (por si cambió la sesión)
    if (gestorSesion.haySesionActiva()) {
        String usuarioId = gestorSesion.getUserId();
        ((GestionContactosImpl) gestionContactos).setUsuarioId(usuarioId);
        System.out.println("🔑 Usuario actualizado desde sesión: " + usuarioId);
    } else {
        System.err.println("⚠️ ADVERTENCIA - No hay sesión activa al solicitar contactos");
    }
    
    gestionContactos.solicitarActualizacionContactos();
}
```

**✅ Ventajas:**
- Obtiene automáticamente el `usuarioId` de la sesión
- Actualiza el ID antes de cada petición (por si cambió)
- No requiere parámetros adicionales
- Manejo de errores si no hay sesión activa

---

### 2. **GestionContactosImpl.java**

#### Variable de instancia
```java
private String usuarioIdActual;
```

#### Método setUsuarioId()
```java
public void setUsuarioId(String usuarioId) {
    this.usuarioIdActual = usuarioId;
    System.out.println("✅ [GestionContactos]: Usuario ID establecido: " + usuarioId);
}
```

#### Método solicitarActualizacionContactos()
```java
@Override
public void solicitarActualizacionContactos() {
    System.out.println("📤 [GestionContactos]: Solicitando lista de contactos...");
    
    Map<String, Object> data = new HashMap<>();
    if (usuarioIdActual != null && !usuarioIdActual.isEmpty()) {
        data.put("usuarioId", usuarioIdActual);  // ✅ Incluye el usuarioId
        System.out.println("   UsuarioId: " + usuarioIdActual);
    }
    
    DTORequest peticion = new DTORequest("listarContactos", data.isEmpty() ? null : data);
    enviadorPeticiones.enviar(peticion);
}
```

#### Manejadores registrados
```java
// REQUEST: Respuesta a petición del cliente
this.gestorRespuesta.registrarManejador("listarContactos", this::manejarRespuestaListarContactos);

// PUSH: Notificación del servidor
this.gestorRespuesta.registrarManejador("solicitarListaContactos", this::manejarPushActualizacionContactos);
```

---

## 📝 Ejemplo de Logs en Consola

### Escenario 1: Inicialización con Sesión Activa
```
🔧 [FachadaContactos]: Inicializando fachada de contactos...
✅ [GestionContactos]: Manejadores registrados
   📤 REQUEST: listarContactos
   📥 PUSH: solicitarListaContactos, actualizarListaContactos
✅ [GestionContactos]: Usuario ID establecido: 550e8400-e29b-41d4-a716-446655440000
✅ [FachadaContactos]: Usuario de sesión establecido automáticamente: 550e8400-e29b-41d4-a716-446655440000
👁️ [GestionContactos]: Observador registrado
✅ [FachadaContactos]: Fachada inicializada con gestores de contactos y mensajes
```

### Escenario 2: Solicitud Manual de Contactos
```
➡️ [FachadaContactos]: Solicitando actualización de contactos al gestor
🔑 [FachadaContactos]: Usuario actualizado desde sesión: 550e8400-e29b-41d4-a716-446655440000
✅ [GestionContactos]: Usuario ID establecido: 550e8400-e29b-41d4-a716-446655440000
📤 [GestionContactos]: Solicitando lista de contactos al servidor...
   UsuarioId: 550e8400-e29b-41d4-a716-446655440000
```

### Escenario 3: Respuesta del Servidor (REQUEST)
```
📤 [GestionContactos][REQUEST]: Respuesta a listarContactos recibida
   Status: success, Message: Lista de contactos obtenida
✅ [GestionContactos][REQUEST]: 3 contactos procesados
📋 [GestionContactos][REQUEST]: Contactos actualizados:
   - Juan Pérez (juan@example.com) [ONLINE] ID: uuid-1
   - María García (maria@example.com) [OFFLINE] ID: uuid-2
   - Carlos López (carlos@example.com) [ONLINE] ID: uuid-3
📢 [GestionContactos]: Notificando a 1 observadores - Tipo: ACTUALIZAR_CONTACTOS
📢 [FachadaContactos]: Recibida notificación - Tipo: ACTUALIZAR_CONTACTOS
📣 [FachadaContactos]: Notificando a 1 observadores - Tipo: ACTUALIZAR_CONTACTOS
```

### Escenario 4: Notificación PUSH del Servidor
```
📥 [GestionContactos][PUSH]: Notificación de actualización recibida
   Action: solicitarListaContactos, Status: success
✅ [GestionContactos][PUSH]: 3 contactos procesados
📋 [GestionContactos][PUSH]: Contactos actualizados:
   - Juan Pérez (juan@example.com) [OFFLINE] ID: uuid-1  ← Estado cambió
   - María García (maria@example.com) [ONLINE] ID: uuid-2  ← Estado cambió
   - Carlos López (carlos@example.com) [ONLINE] ID: uuid-3
📢 [GestionContactos]: Notificando a 1 observadores - Tipo: ACTUALIZAR_CONTACTOS
📢 [FachadaContactos]: Recibida notificación - Tipo: ACTUALIZAR_CONTACTOS
📣 [FachadaContactos]: Notificando a 1 observadores - Tipo: ACTUALIZAR_CONTACTOS
```

---

## ✅ Checklist de Validación

- [x] **FachadaContactosImpl** obtiene `usuarioId` desde `GestorSesionUsuario`
- [x] **FachadaContactosImpl** establece el ID en el constructor (si hay sesión)
- [x] **FachadaContactosImpl** actualiza el ID antes de cada solicitud
- [x] **GestionContactosImpl** almacena el `usuarioId` en variable de instancia
- [x] **GestionContactosImpl** incluye `usuarioId` en la petición `listarContactos`
- [x] **GestionContactosImpl** maneja respuesta REQUEST `listarContactos`
- [x] **GestionContactosImpl** maneja notificación PUSH `solicitarListaContactos`
- [x] Logs detallados en cada paso del flujo
- [x] Manejo de errores cuando no hay sesión activa
- [x] Patrón Observador funciona correctamente
- [x] No hay errores de compilación

---

## 🎯 Cómo Usar desde el Servicio

### Paso 1: Inicializar después del login
```java
// Después de un login exitoso
GestorSesionUsuario.getInstancia().establecerSesion(usuarioId, token);

// Crear la fachada (obtendrá automáticamente el usuarioId)
IFachadaContactos fachadaContactos = new FachadaContactosImpl();

// Registrar observador
fachadaContactos.registrarObservador(servicioContactos);
```

### Paso 2: Solicitar contactos
```java
// Simple y directo - no necesitas pasar el usuarioId
fachadaContactos.solicitarActualizacionContactos();
```

### Paso 3: Recibir notificaciones
```java
@Override
public void actualizar(String tipoDeDato, Object datos) {
    switch (tipoDeDato) {
        case "ACTUALIZAR_CONTACTOS":
            List<DTOContacto> contactos = (List<DTOContacto>) datos;
            // Actualizar UI con los contactos
            break;
            
        case "ERROR_CONTACTOS":
            String mensajeError = (String) datos;
            // Mostrar error al usuario
            break;
    }
}
```

---

## 🔄 Flujo de Datos Completo

```
Usuario hace Login
  ↓
GestorSesionUsuario almacena (userId, token)
  ↓
Se crea FachadaContactosImpl
  ↓
FachadaContactos obtiene userId de la sesión
  ↓
FachadaContactos.setUsuarioId(userId) → GestionContactos
  ↓
Usuario solicita ver contactos
  ↓
FachadaContactos.solicitarActualizacionContactos()
  ↓
FachadaContactos actualiza userId desde sesión (por si cambió)
  ↓
GestionContactos.solicitarActualizacionContactos()
  ↓
Envía: { "action": "listarContactos", "data": { "usuarioId": "xxx" } }
  ↓
Servidor responde con lista de contactos
  ↓
GestionContactos procesa y actualiza caché
  ↓
Notifica a FachadaContactos
  ↓
FachadaContactos notifica a Servicio/UI
  ↓
UI se actualiza con la lista de contactos
```

---

## 🚨 Manejo de Errores

### Error: No hay sesión activa
```
⚠️ [FachadaContactos]: ADVERTENCIA - No hay sesión activa al solicitar contactos
```

### Error: Respuesta con error del servidor
```
❌ [GestionContactos][REQUEST]: Error del servidor: Usuario no encontrado
📢 [GestionContactos]: Notificando a 1 observadores - Tipo: ERROR_CONTACTOS
```

### Error: Formato de datos incorrecto
```
❌ [GestionContactos][REQUEST]: Error al parsear contactos: ...
📢 [GestionContactos]: Notificando a 1 observadores - Tipo: ERROR_CONTACTOS
```

---

## 📚 Archivos Modificados

1. ✅ `FachadaContactosImpl.java` - Integración con GestorSesionUsuario
2. ✅ `GestionContactosImpl.java` - Manejo de REQUEST y PUSH
3. ✅ `IGestionContactos.java` - Contrato actualizado

---

## 🎉 Resultado Final

El sistema ahora:
- ✅ Obtiene automáticamente el `usuarioId` de la sesión activa
- ✅ Envía el `usuarioId` correcto en cada petición al servidor
- ✅ Maneja respuestas REQUEST (`listarContactos`)
- ✅ Maneja notificaciones PUSH (`solicitarListaContactos`)
- ✅ Actualiza la caché automáticamente
- ✅ Notifica a la UI cuando hay cambios
- ✅ Tiene logs detallados para debugging
- ✅ Maneja errores correctamente

