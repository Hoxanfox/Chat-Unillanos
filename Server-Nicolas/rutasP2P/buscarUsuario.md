# 🔍 Ruta P2P: buscarUsuario

## Descripción
Esta ruta permite buscar en qué peer (servidor) está conectado un usuario específico dentro de la red P2P. Es fundamental para el enrutamiento de mensajes entre usuarios que están conectados a diferentes servidores.

## Propósito
Cuando un usuario en el Servidor A quiere enviar un mensaje a un usuario en el Servidor B, primero necesita saber en qué servidor está conectado el destinatario. Esta ruta proporciona esa información.

---

## 📥 Request

### Action
```
buscarUsuario
```

### Estructura
```json
{
  "action": "buscarUsuario",
  "data": {
    "usuarioId": "uuid-del-usuario"
  }
}
```

### Parámetros

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `usuarioId` | String (UUID) | ✅ Sí | ID del usuario que se desea localizar |

### Ejemplo de Request
```json
{
  "action": "buscarUsuario",
  "data": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

## 📤 Response

### Estructura de Éxito
```json
{
  "action": "buscarUsuario",
  "status": "success",
  "message": "Usuario encontrado exitosamente",
  "data": {
    "usuarioId": "uuid",
    "username": "string",
    "conectado": true | false,
    "peerId": "uuid | null",
    "peerIp": "string | null",
    "peerPuerto": number | null
  }
}
```

### Campos de Respuesta

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `usuarioId` | String (UUID) | ID del usuario encontrado |
| `username` | String | Nombre de usuario |
| `conectado` | Boolean | Indica si el usuario está actualmente conectado |
| `peerId` | String (UUID) o null | ID del peer donde está conectado (null si no está asociado a ningún peer) |
| `peerIp` | String o null | Dirección IP del peer |
| `peerPuerto` | Number o null | Puerto del peer |

### Ejemplo de Response (Usuario Conectado)
```json
{
  "action": "buscarUsuario",
  "status": "success",
  "message": "Usuario encontrado exitosamente",
  "data": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "juan",
    "conectado": true,
    "peerId": "660e8400-e29b-41d4-a716-446655440000",
    "peerIp": "192.168.1.10",
    "peerPuerto": 9000
  }
}
```

### Ejemplo de Response (Usuario Sin Peer Asociado)
```json
{
  "action": "buscarUsuario",
  "status": "success",
  "message": "Usuario encontrado exitosamente",
  "data": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "maria",
    "conectado": false,
    "peerId": null,
    "peerIp": null,
    "peerPuerto": null
  }
}
```

---

## ❌ Errores

### 1. Usuario No Encontrado
```json
{
  "action": "buscarUsuario",
  "status": "error",
  "message": "Usuario no encontrado",
  "data": {
    "usuarioId": "DESCONOCIDO"
  }
}
```

### 2. Campo Requerido Faltante
```json
{
  "action": "buscarUsuario",
  "status": "error",
  "message": "El ID del usuario es requerido",
  "data": {
    "campo": "usuarioId",
    "motivo": "Campo requerido"
  }
}
```

### 3. Formato UUID Inválido
```json
{
  "action": "buscarUsuario",
  "status": "error",
  "message": "Formato de UUID inválido",
  "data": {
    "campo": "usuarioId",
    "motivo": "Formato UUID inválido"
  }
}
```

### 4. Error General del Servidor
```json
{
  "action": "buscarUsuario",
  "status": "error",
  "message": "Error al buscar usuario",
  "data": null
}
```

---

## 🔄 Flujo de Uso

### Caso de Uso: Enviar Mensaje Entre Peers

```
1. Cliente A (Peer 1) quiere enviar mensaje a Usuario B

2. Cliente A → Peer 1: buscarUsuario
   {
     "usuarioId": "usuario-B-uuid"
   }

3. Peer 1 → Cliente A: Response
   {
     "peerId": "peer-2-uuid",
     "peerIp": "192.168.1.11",
     "peerPuerto": 9000,
     "conectado": true
   }

4. Cliente A ahora sabe que Usuario B está en Peer 2

5. Cliente A → Peer 1: enviarMensajeDirecto
   {
     "peerDestinoId": "peer-2-uuid",
     "destinatarioId": "usuario-B-uuid",
     "contenido": "Hola!"
   }

6. Peer 1 → Peer 2: retransmitirPeticion (enruta el mensaje)

7. Peer 2 → Usuario B: push_newMessage (entrega el mensaje)
```

---

## 🛠️ Implementación Técnica

### Capas Involucradas

1. **PeerController** (`transporte/server-controladorTransporte`)
   - Maneja la petición HTTP/WebSocket
   - Valida el formato del UUID
   - Delega a la fachada

2. **ChatFachadaImpl** (`negocio/server-logicaFachada`)
   - Coordina la llamada al servicio de peers

3. **PeerServiceImpl** (`negocio/server-LogicaPeers`)
   - Busca el usuario en la base de datos
   - Obtiene el peer asociado usando `findByIdWithPeer`
   - Construye el DTO de respuesta

4. **UserRepository** (`datos/server-persistencia`)
   - Query JPA con JOIN FETCH para obtener usuario y peer en una sola consulta

### DTO Creado
- **UserLocationResponseDto** (`comunes/Server-DTO/src/main/java/com/arquitectura/DTO/p2p/`)
  - Contiene toda la información de ubicación del usuario

---

## 📊 Casos de Respuesta

| Escenario | `conectado` | `peerId` | `peerIp` | `peerPuerto` |
|-----------|-------------|----------|----------|--------------|
| Usuario online en peer | `true` | UUID válido | IP del peer | Puerto del peer |
| Usuario offline con peer | `false` | UUID válido | IP del peer | Puerto del peer |
| Usuario sin peer asignado | `true/false` | `null` | `null` | `null` |
| Usuario no existe | Error 404 | - | - | - |

---

## 🔐 Seguridad

- ✅ Requiere autenticación previa
- ✅ Validación de formato UUID
- ✅ Manejo de excepciones robusto
- ✅ No expone información sensible

---

## 💡 Notas Importantes

1. **Peer Null**: Si un usuario no tiene `peerId` asociado, significa que nunca se ha conectado a través de un peer específico o está usando el servidor principal directamente.

2. **Estado Conectado**: El campo `conectado` indica si el usuario está actualmente online, independientemente de si tiene un peer asociado.

3. **Uso en Enrutamiento**: Esta ruta es el primer paso para implementar mensajería cross-peer. Antes de enviar un mensaje, siempre se debe consultar la ubicación del destinatario.

4. **Caché Recomendado**: Para optimizar, el cliente puede cachear temporalmente la ubicación de usuarios frecuentes (con TTL de 1-2 minutos).

---

## 🧪 Ejemplo de Prueba

### Con cURL (si el servidor expone HTTP):
```bash
curl -X POST http://localhost:22100/api \
  -H "Content-Type: application/json" \
  -d '{
    "action": "buscarUsuario",
    "data": {
      "usuarioId": "550e8400-e29b-41d4-a716-446655440000"
    }
  }'
```

### Con WebSocket (JavaScript):
```javascript
const socket = new WebSocket('ws://localhost:22100');

socket.onopen = () => {
  const request = {
    action: "buscarUsuario",
    data: {
      usuarioId: "550e8400-e29b-41d4-a716-446655440000"
    }
  };
  
  socket.send(JSON.stringify(request));
};

socket.onmessage = (event) => {
  const response = JSON.parse(event.data);
  
  if (response.action === "buscarUsuario") {
    if (response.status === "success") {
      console.log("Usuario encontrado en peer:", response.data.peerId);
      console.log("IP del peer:", response.data.peerIp);
      console.log("Puerto:", response.data.peerPuerto);
    } else {
      console.error("Error:", response.message);
    }
  }
};
```

---

## ✅ Estado de Implementación

- ✅ DTO creado (`UserLocationResponseDto`)
- ✅ Servicio implementado (`PeerServiceImpl.buscarUsuario`)
- ✅ Fachada actualizada (`IChatFachada.buscarUsuario`)
- ✅ Controlador implementado (`PeerController.handleBuscarUsuario`)
- ✅ Compilación exitosa
- ✅ Listo para usar

---

**Fecha de Implementación:** 7 de Noviembre, 2025  
**Versión:** 1.0  
**Estado:** ✅ Implementado y Funcional
