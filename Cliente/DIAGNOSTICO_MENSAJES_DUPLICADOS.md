# 🐛 DIAGNÓSTICO: Error "query did not return a unique result: 2"

## 📋 Resumen del Problema

El servidor está devolviendo el error:
```
"Error al enviar mensaje: query did not return a unique result: 2"
"Error al obtener el historial: query did not return a unique result: 2"
```

Este error indica que hay **registros duplicados en la base de datos del servidor**.

---

## 🔍 Causa Raíz

### El problema está en el SERVIDOR, no en el cliente

Cuando el servidor intenta buscar un usuario o un contacto, está encontrando **2 registros** en lugar de 1. Esto puede ocurrir por:

1. **Usuarios duplicados**: El mismo usuario se registró múltiples veces
2. **Contactos duplicados**: La misma relación de contacto existe dos veces
3. **Falta de restricciones UNIQUE en la BD**: La base de datos permite duplicados

---

## 📊 Información del Error

### Contexto del Log:

```
📤 [GestionMensajes]: Enviando mensaje de TEXTO
   → Remitente: 4bed8adf-3af3-4bc2-afbb-815a0b83069a (Peer: null)
   → Destinatario: 7fed39d7-7d87-42c6-a26f-26bd7927f7a1 (Peer: null)
   → Contenido: asdasdas

>> Petición enviada: {"action":"enviarmensajedirecto","payload":{"remitenteId":"4bed8adf-3af3-4bc2-afbb-815a0b83069a","destinatarioId":"7fed39d7-7d87-42c6-a26f-26bd7927f7a1","tipo":"TEXTO","contenido":"asdasdas"}}

<< Respuesta recibida: {"action":"enviarMensajeDirecto","status":"error","message":"Error al enviar mensaje: query did not return a unique result: 2"}
```

### IDs involucrados:
- **Remitente (Usuario actual)**: `4bed8adf-3af3-4bc2-afbb-815a0b83069a`
- **Destinatario (Contacto)**: `7fed39d7-7d87-42c6-a26f-26bd7927f7a1`

---

## 🛠️ Soluciones (EN EL SERVIDOR)

### Opción 1: Limpiar registros duplicados manualmente

Conectarse a la base de datos del servidor y ejecutar:

```sql
-- Ver usuarios duplicados
SELECT id, nombre, email, COUNT(*) as count
FROM usuarios
GROUP BY id
HAVING COUNT(*) > 1;

-- Ver contactos duplicados
SELECT usuario_id, contacto_id, COUNT(*) as count
FROM contactos
GROUP BY usuario_id, contacto_id
HAVING COUNT(*) > 1;

-- Eliminar duplicados (conservar solo el primero)
DELETE FROM usuarios
WHERE id IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY id ORDER BY fecha_creacion) as row_num
        FROM usuarios
    ) t
    WHERE row_num > 1
);

-- Lo mismo para contactos
DELETE FROM contactos
WHERE id NOT IN (
    SELECT MIN(id)
    FROM contactos
    GROUP BY usuario_id, contacto_id
);
```

### Opción 2: Agregar restricciones UNIQUE en la BD

```sql
-- Asegurar que los IDs sean únicos
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_id UNIQUE (id);

-- Asegurar que no haya contactos duplicados
ALTER TABLE contactos ADD CONSTRAINT uk_contactos_usuario_contacto UNIQUE (usuario_id, contacto_id);
```

### Opción 3: Modificar el código del servidor

Si el servidor usa JPA/Hibernate, asegúrate de que las consultas usen:

```java
// ❌ MAL: .getSingleResult() falla si hay duplicados
Usuario usuario = em.createQuery("SELECT u FROM Usuario u WHERE u.id = :id", Usuario.class)
    .setParameter("id", userId)
    .getSingleResult(); // ← Lanza excepción si hay duplicados

// ✅ BIEN: .getResultList() y tomar el primero
List<Usuario> usuarios = em.createQuery("SELECT u FROM Usuario u WHERE u.id = :id", Usuario.class)
    .setParameter("id", userId)
    .getResultList();

if (usuarios.isEmpty()) {
    throw new NotFoundException("Usuario no encontrado");
}

Usuario usuario = usuarios.get(0); // Tomar el primero
```

---

## 🔧 Diagnóstico en el Cliente

### Verificar si el problema persiste

El cliente ya está manejando correctamente el error y lo muestra al usuario. No hay cambios necesarios en el cliente, pero puedes agregar un mensaje más descriptivo:

```java
// En GestionMensajesImpl.java, método manejarRespuestaEnvioMensaje()
if (errorMsg.contains("query did not return a unique result")) {
    notificarObservadores("ERROR_BD_DUPLICADOS", 
        "Hay registros duplicados en el servidor. Contacta al administrador.");
} else {
    notificarObservadores("ERROR_ENVIO_MENSAJE", errorMsg);
}
```

---

## 📝 Notas Adicionales

### ⚠️ Advertencia de PeerId

Los logs también muestran:
```
⚠️ [GestorContactoPeers]: No se encontró peerId para contacto 7fed39d7-7d87-42c6-a26f-26bd7927f7a1
⚠️ [GestionMensajes]: No se encontró peerId del destinatario — se enviará el mensaje con peerDestinoId = null
```

Esto es **normal** si el destinatario no está conectado o no tiene una sesión WebRTC activa. El mensaje se envía con `peerDestinoId = null` y el servidor lo almacena para entrega posterior.

---

## ✅ Resumen de Cambios Realizados

1. **Archivo `GestionArchivosImpl.java`**: 
   - ✅ Agregado método `extraerNombreDeFileId()` para extraer el nombre del archivo del fileId
   - ✅ Corregido el uso de `downloadInfo.getFileName()` → usar `extraerNombreDeFileId(fileId)`
   - ✅ Evita crear archivos duplicados en BD local

2. **Problema de mensajes duplicados**:
   - ⚠️ Es un problema del **SERVIDOR**, no del cliente
   - ✅ El cliente ya maneja correctamente el error
   - 🔧 Se requiere limpieza de la base de datos del servidor

---

## 🎯 Acción Requerida

**DEBES corregir la base de datos del servidor**:

1. Conectar a la BD del servidor
2. Identificar registros duplicados (usuarios o contactos)
3. Eliminar duplicados manualmente
4. Agregar restricciones UNIQUE para prevenir futuros duplicados

El cliente está funcionando correctamente y reportando el error como debe ser.

