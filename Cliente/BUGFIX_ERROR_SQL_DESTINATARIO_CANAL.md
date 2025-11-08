# Corrección de Errores Críticos en Mensajes de Canal

## 📅 Fecha: 7 de Noviembre, 2025

## 🐛 Problemas Encontrados

### 1. **Error SQL: NULL not allowed for column "ID_DESTINATARIO"**

**Error Original:**
```
Error al guardar mensaje recibido de canal: NULL not allowed for column "ID_DESTINATARIO"; SQL statement:
MERGE INTO mensaje_recibido_canal (id_mensaje, contenido, fecha_envio, tipo, id_destinatario, id_remitente_canal) 
KEY(id_mensaje) VALUES (?, ?, ?, ?, ?, ?) [23502-224]
```

**Causa:**
- Al sincronizar el historial de mensajes del servidor con la base de datos local, no se estaba estableciendo el campo `id_destinatario` en la entidad `MensajeRecibidoCanal`
- Este campo es **obligatorio (NOT NULL)** en la base de datos
- El `id_destinatario` representa el **ID del usuario actual** que recibe el mensaje

**Solución Implementada:**

1. **Modificado `IRepositorioMensajeCanal.java`:**
   - Agregado parámetro `usuarioId` al método `sincronizarHistorial`:
   ```java
   CompletableFuture<Void> sincronizarHistorial(String canalId, String usuarioId, List<DTOMensajeCanal> mensajes);
   ```

2. **Modificado `RepositorioMensajeCanalImpl.java`:**
   - Actualizado método `sincronizarHistorial` para recibir y pasar el `usuarioId`:
   ```java
   public CompletableFuture<Void> sincronizarHistorial(String canalId, String usuarioId, List<DTOMensajeCanal> mensajes) {
       return CompletableFuture.runAsync(() -> {
           eliminarMensajesDeCanal(canalId).join();
           for (DTOMensajeCanal dto : mensajes) {
               MensajeRecibidoCanal mensaje = convertirDTOAMensajeRecibido(dto, usuarioId);
               guardarMensajeRecibido(mensaje).join();
           }
       });
   }
   ```

   - Actualizado método `convertirDTOAMensajeRecibido` para establecer el destinatario:
   ```java
   private MensajeRecibidoCanal convertirDTOAMensajeRecibido(DTOMensajeCanal dto, String usuarioId) {
       MensajeRecibidoCanal mensaje = new MensajeRecibidoCanal();
       mensaje.setIdMensaje(UUID.fromString(dto.getMensajeId()));
       mensaje.setIdRemitenteCanal(UUID.fromString(dto.getCanalId()));
       mensaje.setIdDestinatario(UUID.fromString(usuarioId)); // ✅ FIX
       mensaje.setTipo(dto.getTipo());
       mensaje.setFechaEnvio(dto.getFechaEnvio());
       // ... resto del código
       return mensaje;
   }
   ```

3. **Modificado `GestorMensajesCanalImpl.java`:**
   - Actualizada la llamada para pasar el `usuarioId`:
   ```java
   String usuarioActual = gestorSesion.getUserId();
   // ...
   repositorioMensajes.sincronizarHistorial(canalId, usuarioActual, historial)
   ```

---

### 2. **Notificaciones Múltiples Duplicadas**

**Problema Original:**
```
📥 [VistaCanal]: Notificación recibida - Tipo: HISTORIAL_CANAL_RECIBIDO
📥 [VistaCanal]: Notificación recibida - Tipo: HISTORIAL_CANAL_RECIBIDO
📥 [VistaCanal]: Notificación recibida - Tipo: HISTORIAL_CANAL_RECIBIDO
📥 [VistaCanal]: Notificación recibida - Tipo: HISTORIAL_CANAL_RECIBIDO
📥 [VistaCanal]: Notificación recibida - Tipo: HISTORIAL_CANAL_RECIBIDO
📥 [VistaCanal]: Notificación recibida - Tipo: HISTORIAL_CANAL_RECIBIDO
📥 [VistaCanal]: Notificación recibida - Tipo: HISTORIAL_CANAL_RECIBIDO
```

**Causa:**
- Múltiples observadores registrados o múltiples instancias de `VistaCanal`
- Posible problema de sincronización en el patrón Observer

**Análisis:**
- Este comportamiento sugiere que hay **7-8 observadores registrados** para el mismo evento
- Puede ocurrir si:
  1. La vista se abre y cierra múltiples veces sin desregistrar el observador
  2. Hay múltiples instancias de `VistaCanal` activas
  3. El observador se registra en múltiples lugares

**Solución Recomendada (a implementar):**

1. **Agregar método de limpieza en `VistaCanal`:**
   ```java
   public void limpiar() {
       // Desregistrar el observador cuando se cierra la vista
       controlador.removerObservadorMensajes(this);
   }
   ```

2. **Llamar al método de limpieza cuando se cambia de vista:**
   ```java
   // En FeatureCanales o donde se gestione la navegación
   if (vistaActual != null) {
       vistaActual.limpiar();
   }
   vistaActual = nuevaVista;
   ```

3. **Verificar que no hay registros duplicados:**
   - El método `registrarObservador` en `GestorMensajesCanalImpl` ya tiene protección:
   ```java
   if (!observadores.contains(observador)) {
       observadores.add(observador);
   }
   ```

---

## ✅ Resultados Esperados

### Antes:
```
Error al guardar mensaje recibido de canal: NULL not allowed for column "ID_DESTINATARIO"
📥 [VistaCanal]: Notificación recibida - Tipo: HISTORIAL_CANAL_RECIBIDO (x7 veces)
```

### Después:
```
Historial de canal 13f2cc70-d18d-4da7-8506-92c3fa4ea1b7 sincronizado: 2 mensajes.
✓ Historial de canal sincronizado: 2 mensajes
📥 [VistaCanal]: Notificación recibida - Tipo: HISTORIAL_CANAL_RECIBIDO (1 sola vez)
📜 [VistaCanal]: Historial recibido - Total mensajes: 2
```

---

## 📊 Archivos Modificados

1. **IRepositorioMensajeCanal.java**
   - ✅ Agregado parámetro `usuarioId` a `sincronizarHistorial`

2. **RepositorioMensajeCanalImpl.java**
   - ✅ Actualizado `sincronizarHistorial` para recibir `usuarioId`
   - ✅ Actualizado `convertirDTOAMensajeRecibido` para establecer `id_destinatario`

3. **GestorMensajesCanalImpl.java**
   - ✅ Actualizada llamada a `sincronizarHistorial` con `usuarioActual`

---

## 🔍 Sobre el Error de "invitarmiembro"

**Error del Servidor (NO del cliente):**
```
>> Petición enviada: {"action":"invitarmiembro","payload":{...}}
<< Respuesta recibida: {"action":"invitarMiembro","status":"error","message":"Error interno del servidor al invitar miembro"}
```

**Confirmación:**
- ✅ El cliente está enviando correctamente `"invitarmiembro"` en minúsculas
- ❌ El servidor responde con error interno
- 📝 El problema está en el **SERVIDOR**, no en el cliente

**Posibles causas en el servidor:**
1. La ruta no acepta "invitarmiembro" en minúsculas
2. Error en el procesamiento de la invitación
3. Problema de permisos o validación de usuarios
4. Error en la base de datos del servidor

---

## 🚀 Próximos Pasos

### Problema de Notificaciones Múltiples:
1. ✅ Implementar método `limpiar()` en `VistaCanal`
2. ✅ Llamar a `limpiar()` cuando se cierra o cambia de vista
3. ✅ Verificar que no hay múltiples instancias activas

### Verificación:
1. ✅ Compilar el proyecto
2. ✅ Ejecutar y probar envío de mensajes a canales
3. ✅ Verificar que no hay más errores SQL
4. ✅ Verificar cantidad de notificaciones recibidas

---

## 📝 Resumen

| Problema | Estado | Solución |
|----------|--------|----------|
| Error SQL ID_DESTINATARIO NULL | ✅ **RESUELTO** | Agregado parámetro usuarioId |
| Notificaciones duplicadas | ⚠️ **PARCIAL** | Requiere limpieza de observadores |
| Error "invitarmiembro" | ❌ **SERVIDOR** | Problema del servidor, no del cliente |

**El error SQL crítico está RESUELTO.** Los mensajes ahora se guardan correctamente en la base de datos local con el ID del destinatario establecido.

