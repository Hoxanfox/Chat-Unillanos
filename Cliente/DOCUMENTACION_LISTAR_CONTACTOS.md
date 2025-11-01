# Documentación: Sistema de Gestión de Contactos

## 📋 Resumen

El sistema maneja dos tipos de comunicación para gestionar contactos:

1. **REQUEST (Cliente → Servidor)**: `listarContactos`
2. **PUSH (Servidor → Cliente)**: `solicitarListaContactos`

---

## 🔄 Flujo de Comunicación

### 1️⃣ Petición REQUEST: `listarContactos`

**Cliente solicita la lista de contactos al servidor**

#### Request (Cliente → Servidor)
```json
{
  "action": "listarContactos",
  "data": {
    "usuarioId": "el-id-del-usuario-actual"
  }
}
```

#### Response Success (Servidor → Cliente)
```json
{
  "action": "listarContactos",
  "status": "success",
  "message": "Lista de contactos obtenida",
  "data": [
    {
      "id": "contacto-uuid-1",
      "idPeer": "servidor-uuid-1",
      "nombre": "Juan Pérez",
      "email": "juan.perez@example.com",
      "imagenBase64": "sadkjashdkjahskdjhasjkdhaskjdhqwql",
      "conectado": "OFFLINE"
    },
    {
      "id": "contacto-uuid-2",
      "idServidor": "servidor-uuid-2",
      "nombre": "Maria García",
      "email": "maria.garcia@example.com",
      "conectado": "ONLINE",
      "imagenBase64": "sadkjashdkjahskdjhasjkdhaskjdhqwql"
    }
  ]
}
```

#### Response Error (Servidor → Cliente)
```json
{
  "action": "listarContactos",
  "status": "error",
  "message": "Error al obtener contactos: [descripción del error]",
  "data": null
}
```

---

### 2️⃣ Notificación PUSH: `solicitarListaContactos`

**Servidor envía actualización automática al cliente**

#### Push Notification (Servidor → Cliente)
```json
{
  "action": "solicitarListaContactos",
  "status": "success",
  "message": "Lista de contactos obtenida exitosamente",
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "nombre": "María García",
      "email": "maria@example.com",
      "photoId": "foto456.jpg",
      "estado": "ONLINE",
      "fechaRegistro": "2024-01-20T15:45:00"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "nombre": "Carlos López",
      "email": "carlos@example.com",
      "photoId": "foto789.jpg",
      "estado": "OFFLINE",
      "fechaRegistro": "2024-01-18T10:30:00"
    }
  ]
}
```

#### Push Error (Servidor → Cliente)
```json
{
  "action": "solicitarListaContactos",
  "status": "error",
  "message": "Error al obtener los contactos",
  "data": null
}
```

---

## 🏗️ Implementación en Cliente

### Archivo: `GestionContactosImpl.java`

#### Manejadores Registrados

```java
// REQUEST: Respuesta a petición del cliente
this.gestorRespuesta.registrarManejador("listarContactos", this::manejarRespuestaListarContactos);

// PUSH: Notificación del servidor (actualización automática)
this.gestorRespuesta.registrarManejador("solicitarListaContactos", this::manejarPushActualizacionContactos);

// PUSH: Actualización de lista de contactos (legacy)
this.gestorRespuesta.registrarManejador("actualizarListaContactos", this::manejarPushActualizacionContactos);
```

#### Métodos Principales

##### 1. `solicitarActualizacionContactos()`
Envía petición REQUEST al servidor

```java
public void solicitarActualizacionContactos() {
    Map<String, Object> data = new HashMap<>();
    if (usuarioIdActual != null && !usuarioIdActual.isEmpty()) {
        data.put("usuarioId", usuarioIdActual);
    }
    
    DTORequest peticion = new DTORequest("listarContactos", data.isEmpty() ? null : data);
    enviadorPeticiones.enviar(peticion);
}
```

##### 2. `setUsuarioId(String usuarioId)`
Establece el ID del usuario actual

```java
public void setUsuarioId(String usuarioId) {
    this.usuarioIdActual = usuarioId;
}
```

##### 3. `manejarRespuestaListarContactos(DTOResponse respuesta)`
Maneja la respuesta del servidor a la petición REQUEST

##### 4. `manejarPushActualizacionContactos(DTOResponse respuesta)`
Maneja las notificaciones PUSH del servidor

##### 5. `procesarListaContactos(DTOResponse respuesta, String tipo)`
Procesa la lista de contactos (común para REQUEST y PUSH)

---

## 📊 Flujo de Datos

```
┌─────────────────────────────────────────────────────────────┐
│                    PETICIÓN REQUEST                         │
└─────────────────────────────────────────────────────────────┘

Cliente                          Servidor
   │                                │
   │  1. solicitarActualizacion()   │
   │  ────────────────────────────> │
   │     action: "listarContactos"  │
   │     data: { usuarioId: "..." } │
   │                                │
   │  2. Response                   │
   │  <──────────────────────────── │
   │     Lista de contactos         │
   │                                │
   │  3. procesarListaContactos()   │
   │     ├─ Actualizar caché        │
   │     └─ Notificar observadores  │

┌─────────────────────────────────────────────────────────────┐
│                   NOTIFICACIÓN PUSH                         │
└─────────────────────────────────────────────────────────────┘

Servidor                         Cliente
   │                                │
   │  1. Push Notification          │
   │  ────────────────────────────> │
   │  action: "solicitarListaContactos"
   │  data: [ contactos actualizados ]
   │                                │
   │                                │ 2. procesarListaContactos()
   │                                │    ├─ Actualizar caché
   │                                │    └─ Notificar observadores
```

---

## 🔔 Notificaciones a Observadores

Cuando se recibe una lista de contactos (REQUEST o PUSH), se notifica:

### Evento: `ACTUALIZAR_CONTACTOS`
```java
notificarObservadores("ACTUALIZAR_CONTACTOS", this.contactosCache);
```

### Evento: `ERROR_CONTACTOS`
```java
notificarObservadores("ERROR_CONTACTOS", mensajeError);
```

---

## 🎯 Casos de Uso

### Caso 1: Cliente solicita lista al iniciar sesión
```java
// 1. Establecer usuario ID
gestionContactos.setUsuarioId("usuario-123");

// 2. Solicitar lista
gestionContactos.solicitarActualizacionContactos();

// 3. El servidor responde con "listarContactos"
// 4. Se actualiza la caché y se notifica a los observadores
```

### Caso 2: Servidor envía actualización automática
```java
// 1. Un contacto cambia su estado (ONLINE/OFFLINE)
// 2. Servidor envía PUSH "solicitarListaContactos"
// 3. Cliente recibe y procesa automáticamente
// 4. Se actualiza la caché y se notifica a los observadores
```

---

## 📝 Logs del Sistema

### Logs de REQUEST
```
📤 [GestionContactos]: Solicitando lista de contactos al servidor...
   UsuarioId: usuario-123
📤 [GestionContactos][REQUEST]: Respuesta a listarContactos recibida
   Status: success, Message: Lista de contactos obtenida
✅ [GestionContactos][REQUEST]: 5 contactos procesados
📋 [GestionContactos][REQUEST]: Contactos actualizados:
   - Juan Pérez (juan@example.com) [ONLINE] ID: uuid-1
   - María García (maria@example.com) [OFFLINE] ID: uuid-2
📢 [GestionContactos]: Notificando a 2 observadores - Tipo: ACTUALIZAR_CONTACTOS
```

### Logs de PUSH
```
📥 [GestionContactos][PUSH]: Notificación de actualización recibida
   Action: solicitarListaContactos, Status: success
✅ [GestionContactos][PUSH]: 5 contactos procesados
📋 [GestionContactos][PUSH]: Contactos actualizados:
   - Juan Pérez (juan@example.com) [OFFLINE] ID: uuid-1
   - María García (maria@example.com) [ONLINE] ID: uuid-2
📢 [GestionContactos]: Notificando a 2 observadores - Tipo: ACTUALIZAR_CONTACTOS
```

---

## ✅ Checklist de Implementación

- [x] Manejador para `listarContactos` (REQUEST)
- [x] Manejador para `solicitarListaContactos` (PUSH)
- [x] Método `setUsuarioId()` para establecer usuario actual
- [x] Envío de `usuarioId` en la petición REQUEST
- [x] Procesamiento de respuestas exitosas
- [x] Manejo de errores
- [x] Actualización de caché de contactos
- [x] Notificación a observadores
- [x] Logs detallados para debugging

---

## 🔧 Configuración Necesaria

1. **En el inicio de sesión:**
   ```java
   gestionContactos.setUsuarioId(usuarioLogueado.getId());
   ```

2. **Registrar observador en la UI:**
   ```java
   gestionContactos.registrarObservador(controladorContactos);
   ```

3. **Solicitar lista inicial:**
   ```java
   gestionContactos.solicitarActualizacionContactos();
   ```

---

## 📚 Referencias

- `GestionContactosImpl.java`: Implementación del gestor
- `IGestionContactos.java`: Interfaz del contrato
- `DTOContacto.java`: Objeto de transferencia de datos
- `GestorRespuesta.java`: Gestor de respuestas del servidor

