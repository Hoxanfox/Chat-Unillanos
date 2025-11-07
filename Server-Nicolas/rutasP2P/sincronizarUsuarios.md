# Ruta: sincronizarUsuarios

## Descripción
Sincroniza la información de usuarios conectados entre peers de la red P2P. Permite que un peer obtenga la lista actualizada de todos los usuarios conectados en el sistema, incluyendo en qué peer está conectado cada usuario.

## Endpoint
**Acción:** `sincronizarUsuarios`

## Request

### Estructura del Request
```json
{
  "action": "sincronizarUsuarios",
  "data": {
    "peerId": "uuid-del-peer-solicitante"
  }
}
```

### Campos del Request
- `peerId` (String, UUID, opcional): ID del peer que solicita la sincronización

## Response

### Caso de Éxito
```json
{
  "action": "sincronizarUsuarios",
  "status": "success",
  "message": "Usuarios sincronizados exitosamente",
  "data": {
    "usuarios": [
      {
        "usuarioId": "uuid-usuario-1",
        "username": "usuario1",
        "conectado": true,
        "peerId": "uuid-peer-1",
        "peerIp": "192.168.1.5",
        "peerPuerto": 9000
      },
      {
        "usuarioId": "uuid-usuario-2",
        "username": "usuario2",
        "conectado": true,
        "peerId": "uuid-peer-2",
        "peerIp": "192.168.1.6",
        "peerPuerto": 9001
      },
      {
        "usuarioId": "uuid-usuario-3",
        "username": "usuario3",
        "conectado": false,
        "peerId": null,
        "peerIp": null,
        "peerPuerto": null
      }
    ],
    "totalUsuarios": 3,
    "usuariosConectados": 2,
    "fechaSincronizacion": "2024-11-07T10:30:00"
  }
}
```

### Caso de Éxito - Sin Usuarios Conectados
```json
{
  "action": "sincronizarUsuarios",
  "status": "success",
  "message": "Usuarios sincronizados exitosamente",
  "data": {
    "usuarios": [],
    "totalUsuarios": 0,
    "usuariosConectados": 0,
    "fechaSincronizacion": "2024-11-07T10:30:00"
  }
}
```

### Caso de Error - Peer No Reconocido
```json
{
  "action": "sincronizarUsuarios",
  "status": "error",
  "message": "Peer no reconocido",
  "data": {
    "peerId": "uuid-invalido",
    "motivo": "El peer no está registrado en la red"
  }
}
```

## Flujo de Operación

1. **Validación de Peer**: Si se proporciona peerId, verificar que esté registrado (opcional)
2. **Obtener Usuarios Conectados**: Consultar todos los usuarios que están actualmente conectados
3. **Obtener Ubicación de Usuarios**: Para cada usuario conectado, determinar en qué peer está
4. **Preparar Respuesta**: Construir lista con información completa de usuarios y sus ubicaciones
5. **Incluir Estadísticas**: Agregar contadores de usuarios totales y conectados
6. **Timestamp**: Incluir fecha y hora de la sincronización

## Notas de Implementación
- La lista debe incluir tanto usuarios conectados como desconectados para dar una vista completa
- Para usuarios desconectados, los campos de peer deben ser null
- Esta ruta puede ser llamada periódicamente por los peers para mantener su caché actualizado
- La información de ubicación (peerId, peerIp, peerPuerto) solo debe estar presente para usuarios conectados
- El timestamp ayuda a los peers a determinar qué tan reciente es la información
- Si un peer no está registrado pero solicita sincronización, se puede permitir (no es crítico validar el peerId)
