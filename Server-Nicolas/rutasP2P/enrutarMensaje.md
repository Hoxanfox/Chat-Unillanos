# Ruta: enrutarMensaje

## Descripción
Enruta un mensaje P2P desde un usuario remitente hacia un usuario destinatario. El servidor identifica en qué peer está conectado el destinatario y retransmite el mensaje a ese peer para su entrega.

## Endpoint
**Acción:** `enrutarMensaje`

## Request

### Estructura del Request
```json
{
  "action": "enrutarMensaje",
  "data": {
    "remitenteId": "uuid-del-remitente",
    "destinatarioId": "uuid-del-destinatario",
    "contenido": "Texto del mensaje",
    "tipo": "texto"
  }
}
```

### Campos del Request
- `remitenteId` (String, UUID, requerido): ID del usuario que envía el mensaje
- `destinatarioId` (String, UUID, requerido): ID del usuario que recibirá el mensaje
- `contenido` (String, requerido): Contenido del mensaje a enviar
- `tipo` (String, opcional): Tipo de mensaje (por defecto "texto")

## Response

### Caso de Éxito
```json
{
  "action": "enrutarMensaje",
  "status": "success",
  "message": "Mensaje enrutado exitosamente",
  "data": {
    "destinatarioId": "uuid-del-destinatario",
    "destinatarioUsername": "nombreUsuario",
    "entregado": true,
    "fechaEntrega": "2024-11-07T10:30:00",
    "motivo": "Mensaje entregado exitosamente"
  }
}
```

### Caso de Error - Usuario Destinatario No Encontrado
```json
{
  "action": "enrutarMensaje",
  "status": "error",
  "message": "Usuario destinatario no encontrado",
  "data": {
    "destinatarioId": "uuid-del-destinatario",
    "destinatarioUsername": null,
    "entregado": false,
    "fechaEntrega": null,
    "motivo": "Usuario no encontrado en el sistema"
  }
}
```

### Caso de Error - Usuario Destinatario No Conectado
```json
{
  "action": "enrutarMensaje",
  "status": "error",
  "message": "Usuario destinatario no está conectado",
  "data": {
    "destinatarioId": "uuid-del-destinatario",
    "destinatarioUsername": "nombreUsuario",
    "entregado": false,
    "fechaEntrega": null,
    "motivo": "Usuario no está conectado a ningún peer"
  }
}
```

### Caso de Error - Peer Destinatario No Disponible
```json
{
  "action": "enrutarMensaje",
  "status": "error",
  "message": "No se pudo entregar el mensaje",
  "data": {
    "destinatarioId": "uuid-del-destinatario",
    "destinatarioUsername": "nombreUsuario",
    "entregado": false,
    "fechaEntrega": null,
    "motivo": "Peer destinatario no disponible"
  }
}
```

### Caso de Error - Campos Faltantes
```json
{
  "action": "enrutarMensaje",
  "status": "error",
  "message": "Faltan campos requeridos",
  "data": {
    "campo": "remitenteId/destinatarioId/contenido",
    "motivo": "Campos requeridos"
  }
}
```

## Flujo de Operación

1. **Validación de Campos**: Verificar que todos los campos requeridos estén presentes
2. **Buscar Destinatario**: Localizar al usuario destinatario y verificar su estado de conexión
3. **Verificar Conexión**: Confirmar que el destinatario está conectado a un peer
4. **Retransmitir Mensaje**: Enviar el mensaje al peer donde está conectado el destinatario
5. **Confirmar Entrega**: Retornar confirmación de entrega o error

## Notas de Implementación
- El mensaje debe incluir información del remitente para que el destinatario sepa quién lo envió
- Si el peer destinatario no responde, se debe retornar un error apropiado
- La fecha de entrega debe registrarse en formato ISO 8601
- El tipo de mensaje por defecto es "texto" pero puede extenderse a otros tipos (imagen, archivo, etc.)
