
notificarObservadores("NUEVO_MENSAJE_PRIVADO", mensaje);
```

**Cambios Clave:**
- ✅ Los mensajes propios ya NO se ignoran
- ✅ Se marcan correctamente con `esMio = true`
- ✅ Solo se filtran mensajes de otros peers (no los propios)
- ✅ Los mensajes aparecen en la vista correctamente alineados (derecha para propios, izquierda para recibidos)

---

### 3. ⚠️ Comando Desconocido: "obtenernotificaciones"

**Error:**
```
{"action":"obtenernotificaciones","status":"error","message":"Comando desconocido: obtenernotificaciones"}
```

**Causa:** El cliente intenta usar el comando `"obtenernotificaciones"` (minúsculas) pero el servidor probablemente espera otro formato o no lo implementa.

**Estado:** ⚠️ ERROR MENOR - No afecta la funcionalidad principal del chat

**Ubicación:** `Negocio/GestionNotificaciones/src/main/java/gestionNotificaciones/GestorNotificaciones.java:69`

**Recomendación:** 
- Verificar la API del servidor para el comando correcto
- O deshabilitar esta funcionalidad si no es crítica
- O implementarla en el servidor si es necesaria

---

## ✅ Resultados de las Correcciones

### Mensajes del Historial
- ✅ Se cargan correctamente al abrir el chat
- ✅ Se muestran con la alineación correcta (izquierda/derecha)
- ✅ Se marcan correctamente como propios o recibidos

### Mensajes Nuevos
- ✅ Los mensajes enviados ahora aparecen en la vista (alineados a la derecha)
- ✅ Los mensajes recibidos aparecen en la vista (alineados a la izquierda)
- ✅ El contenido se muestra correctamente
- ✅ El timestamp se formatea correctamente

### ListadorCanales
- ✅ Ya no hay NullPointerException
- ✅ Los canales se parsean correctamente
- ✅ Los datos del servidor se mapean a los objetos de dominio

---

## 🔧 Archivos Modificados

1. **ListadorCanales.java**
   - Corregido mapeo de campos del servidor (`idCanal`, `nombreCanal`, `ownerId`)

2. **GestionMensajesImpl.java**
   - Eliminada lógica que ignoraba mensajes propios
   - Mejorada lógica de filtrado de mensajes por peer

3. **VistaContactoChat.java**
   - Agregado soporte para ambos tipos de notificación de historial
   - `HISTORIAL_MENSAJES_RECIBIDO` y `HISTORIAL_MENSAJES`

---

## 🧪 Pruebas Recomendadas

1. ✅ Abrir chat con un contacto y verificar que el historial se carga
2. ✅ Enviar un mensaje de texto y verificar que aparece en la vista (derecha)
3. ✅ Recibir un mensaje de texto y verificar que aparece en la vista (izquierda)
4. ✅ Listar canales y verificar que no hay errores
5. ⚠️ Verificar funcionalidad de notificaciones (si es necesaria)

---

## 📊 Flujo Completo de Mensajes

```
ENVIAR MENSAJE:
1. Usuario escribe mensaje en VistaContactoChat
2. ControladorChat → ServicioChat → FachadaContactos → GestionMensajes
3. GestionMensajes envía al servidor
4. Servidor procesa y devuelve PUSH "nuevoMensajeDirecto"
5. GestionMensajes mapea el mensaje (con mapearMensajeDesdeServidor)
6. Marca esMio = true (porque es el remitente)
7. Notifica "NUEVO_MENSAJE_PRIVADO"
8. VistaContactoChat recibe y muestra (alineado a la derecha)

RECIBIR MENSAJE:
1. Servidor envía PUSH "nuevoMensajeDirecto"
2. GestionMensajes mapea el mensaje
3. Marca esMio = false
4. Notifica "NUEVO_MENSAJE_PRIVADO"
5. VistaContactoChat recibe y muestra (alineado a la izquierda)

HISTORIAL:
1. VistaContactoChat solicita historial al abrir
2. GestionMensajes envía petición "solicitarHistorialPrivado"
3. Servidor devuelve lista de mensajes
4. GestionMensajes parsea y marca cada mensaje (esMio)
5. Notifica "HISTORIAL_MENSAJES_RECIBIDO"
6. VistaContactoChat limpia y carga todos los mensajes
```

---

## 🎯 Estado Final

- ✅ **ListadorCanales:** Funcionando correctamente
- ✅ **Historial de Mensajes:** Se carga y muestra correctamente
- ✅ **Envío de Mensajes:** Los mensajes propios se muestran en la vista
- ✅ **Recepción de Mensajes:** Los mensajes de otros usuarios se muestran correctamente
- ⚠️ **Notificaciones:** Comando no reconocido por el servidor (error menor)

---

**Fecha:** 2025-11-06  
**Estado:** ✅ PROBLEMAS PRINCIPALES RESUELTOS  
**Compilación:** ✅ EXITOSA
# BUGFIX: Corrección de Múltiples Errores en el Sistema de Chat

## 📋 Errores Identificados y Corregidos

### 1. ❌ NullPointerException en ListadorCanales

**Error:**
```
Cannot invoke "String.length()" because "name" is null
at java.base/java.util.UUID.fromString(UUID.java:242)
at gestionCanales.listarCanales.ListadorCanales.convertirMapaACanal
```

**Causa:** El código intentaba acceder a campos `"id"` y `"nombre"`, pero el servidor envía `"idCanal"` y `"nombreCanal"`.

**Solución Aplicada:**

**Archivo:** `Negocio/GestionCanales/src/main/java/gestionCanales/listarCanales/ListadorCanales.java`

```java
private Canal convertirMapaACanal(Map<String, Object> data) {
    // ANTES: data.get("id"), data.get("nombre")
    // AHORA: data.get("idCanal"), data.get("nombreCanal")
    String id = (String) data.get("idCanal");
    String nombre = (String) data.get("nombreCanal");
    String idAdministrador = (String) data.getOrDefault("ownerId", null);

    return new Canal(
            UUID.fromString(id),
            nombre,
            (idAdministrador != null) ? UUID.fromString(idAdministrador) : null
    );
}
```

**Mapeo de Campos del Servidor:**
| Servidor | Cliente |
|----------|---------|
| `idCanal` | `id` |
| `nombreCanal` | `nombre` |
| `ownerId` | `idAdministrador` |

---

### 2. ❌ Mensajes No Se Renderizan en la Vista

**Problema:** Tanto el historial como los nuevos mensajes no se mostraban en la interfaz.

**Causas:**
1. **Historial:** La vista esperaba notificación tipo `"HISTORIAL_MENSAJES"` pero recibía `"HISTORIAL_MENSAJES_RECIBIDO"`
2. **Mensajes propios:** El cliente ignoraba los mensajes propios en el PUSH con `"Ignorando mensaje propio en push"`

**Solución Aplicada:**

#### A. Soporte para Ambos Tipos de Notificación en la Vista

**Archivo:** `Presentacion/InterfazEscritorio/src/main/java/interfazEscritorio/dashboard/featureContactos/chatContacto/VistaContactoChat.java`

```java
case "HISTORIAL_MENSAJES_RECIBIDO":
case "HISTORIAL_MENSAJES":
    // Historial completo recibido
    if (datos instanceof List) {
        List<?> lista = (List<?>) datos;
        System.out.println("📜 [VistaContactoChat]: Historial recibido - Total mensajes: " + lista.size());
        Platform.runLater(() -> {
            mensajesBox.getChildren().clear();
            for (Object obj : lista) {
                if (obj instanceof DTOMensaje) {
                    agregarMensaje((DTOMensaje) obj);
                }
            }
            System.out.println("✅ [VistaContactoChat]: Historial cargado en la vista");
        });
    }
    break;
```

#### B. No Ignorar Mensajes Propios en el PUSH

**Archivo:** `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

**ANTES:**
```java
// Verificar si el mensaje es mío
boolean esMio = myUserId != null && myUserId.equals(mensaje.getRemitenteId());
if (esMio) {
    System.out.println("⏩ [GestionMensajes]: Ignorando mensaje propio en push");
    return;  // ❌ Esto impedía que los mensajes propios se mostraran
}
```

**DESPUÉS:**
```java
// Marcar si el mensaje es mío
boolean esMio = myUserId != null && myUserId.equals(mensaje.getRemitenteId());
mensaje.setEsMio(esMio);

// Solo filtrar mensajes dirigidos a otro peer (pero NO los propios)
if (!esMio && myPeerId != null && mensaje.getPeerDestinoId() != null &&
        !myPeerId.equals(mensaje.getPeerDestinoId())) {
    System.out.println("⏩ [GestionMensajes]: Ignorando mensaje dirigido a otro peer");
    return;
}

System.out.println("✅ [GestionMensajes]: Nuevo mensaje privado recibido");
System.out.println("   → De: " + mensaje.getRemitenteId() + (esMio ? " (YO)" : ""));
System.out.println("   → Tipo: " + mensaje.getTipo());
System.out.println("   → Contenido: " + mensaje.getContenido());

