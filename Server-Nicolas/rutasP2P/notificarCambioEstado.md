# Ruta: notificarCambioEstado

## Descripción
Notifica a otros peers de la red cuando un usuario cambia su estado de conexión (ONLINE/OFFLINE). Esto permite mantener sincronizada la información de usuarios conectados en toda la red P2P.

## Endpoint
**Acción:** `notificarCambioEstado`

## Request

### Estructura del Request
```json
{
  "action": "notificarCambioEstado",
  "data": {
    "usuarioId": "uuid-del-usuario",
    "nuevoEstado": "ONLINE",
    "peerId": "uuid-del-peer",
    "peerIp": "192.168.1.5",
    "peerPuerto": 9000
  }
}
```

### Campos del Request
- `usuarioId` (String, UUID, requerido): ID del usuario que cambió de estado
- `nuevoEstado` (String, requerido): Nuevo estado del usuario ("ONLINE" o "OFFLINE")
- `peerId` (String, UUID, opcional): ID del peer donde se conectó el usuario (solo para ONLINE)
- `peerIp` (String, opcional): IP del peer donde se conectó el usuario (solo para ONLINE)
- `peerPuerto` (Number, opcional): Puerto del peer donde se conectó el usuario (solo para ONLINE)

## Response

### Caso de Éxito - Usuario Conectado
```json
{
  "action": "notificarCambioEstado",
  "status": "success",
  "message": "Cambio de estado notificado exitosamente",
  "data": {
    "usuarioId": "uuid-del-usuario",
    "username": "usuario1",
    "estadoAnterior": "OFFLINE",
    "estadoNuevo": "ONLINE",
    "peerId": "uuid-del-peer",
    "peerIp": "192.168.1.5",
    "peerPuerto": 9000,
    "fechaCambio": "2024-11-07T10:30:00"
  }
}
```

### Caso de Éxito - Usuario Desconectado
```json
{
  "action": "notificarCambioEstado",
  "status": "success",
  "message": "Cambio de estado notificado exitosamente",
  "data": {
    "usuarioId": "uuid-del-usuario",
    "username": "usuario1",
    "estadoAnterior": "ONLINE",
    "estadoNuevo": "OFFLINE",
    "peerId": null,
    "peerIp": null,
    "peerPuerto": null,
    "fechaCambio": "2024-11-07T10:30:00"
  }
}
```

### Caso de Error - Usuario No Encontrado
```json
{
  "action": "notificarCambioEstado",
  "status": "error",
  "message": "Usuario no encontrado",
  "data": {
    "usuarioId": "uuid-invalido",
    "motivo": "El usuario no existe en el sistema"
  }
}
```

### Caso de Error - Estado Inválido
```json
{
  "action": "notificarCambioEstado",
  "status": "error",
  "message": "Estado inválido",
  "data": {
    "campo": "nuevoEstado",
    "motivo": "El estado debe ser ONLINE u OFFLINE"
  }
}
```

### Caso de Error - Campos Faltantes
```json
{
  "action": "notificarCambioEstado",
  "status": "error",
  "message": "Faltan campos requeridos",
  "data": {
    "campo": "usuarioId/nuevoEstado",
    "motivo": "Campos requeridos"
  }
}
```

## Flujo de Operación

1. **Validación de Campos**: Verificar que usuarioId y nuevoEstado estén presentes
2. **Validar Estado**: Confirmar que el estado sea "ONLINE" u "OFFLINE"
3. **Buscar Usuario**: Verificar que el usuario existe en el sistema
4. **Obtener Estado Anterior**: Consultar el estado actual del usuario antes del cambio
5. **Actualizar Estado**: Cambiar el estado del usuario en el sistema
6. **Registrar Timestamp**: Guardar la fecha y hora del cambio
7. **Retornar Confirmación**: Devolver información del cambio realizado

## Notas de Implementación
- Si el nuevo estado es "ONLINE", se deben proporcionar peerId, peerIp y peerPuerto
- Si el nuevo estado es "OFFLINE", los campos de peer deben ser null o no proporcionarse
- Esta ruta puede ser llamada por peers cuando detectan que un usuario se conecta o desconecta
- El timestamp ayuda a resolver conflictos si múltiples peers notifican cambios simultáneos
- Se debe validar que el estado realmente cambió (no notificar si ya está en ese estado)
- Esta notificación puede desencadenar eventos en el sistema para actualizar cachés o notificar a otros usuarios
