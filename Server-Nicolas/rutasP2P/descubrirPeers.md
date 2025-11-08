# Ruta: descubrirPeers

## Descripción
Permite a un peer descubrir otros peers activos en la red P2P. Esta ruta es útil cuando un nuevo peer se une a la red y necesita conocer qué otros peers están disponibles para establecer conexiones.

## Endpoint
**Acción:** `descubrirPeers`

## Request

### Estructura del Request
```json
{
  "action": "descubrirPeers",
  "data": {
    "peerId": "uuid-del-peer-solicitante",
    "ip": "192.168.1.10",
    "puerto": 9000
  }
}
```

### Campos del Request
- `peerId` (String, UUID, opcional): ID del peer que solicita el descubrimiento. Si no se proporciona, se asume que es un peer nuevo
- `ip` (String, requerido): Dirección IP del peer solicitante
- `puerto` (Number, requerido): Puerto del peer solicitante

## Response

### Caso de Éxito
```json
{
  "action": "descubrirPeers",
  "status": "success",
  "message": "Peers descubiertos exitosamente",
  "data": {
    "peersDisponibles": [
      {
        "peerId": "uuid-peer-1",
        "ip": "192.168.1.5",
        "puerto": 9000,
        "conectado": "ONLINE"
      },
      {
        "peerId": "uuid-peer-2",
        "ip": "192.168.1.6",
        "puerto": 9001,
        "conectado": "ONLINE"
      }
    ],
    "totalPeers": 2,
    "peerSolicitante": {
      "peerId": "uuid-del-peer-solicitante",
      "registrado": true
    }
  }
}
```

### Caso de Éxito - Peer Nuevo Registrado
```json
{
  "action": "descubrirPeers",
  "status": "success",
  "message": "Peer registrado y peers descubiertos",
  "data": {
    "peersDisponibles": [
      {
        "peerId": "uuid-peer-1",
        "ip": "192.168.1.5",
        "puerto": 9000,
        "conectado": "ONLINE"
      }
    ],
    "totalPeers": 1,
    "peerSolicitante": {
      "peerId": "uuid-nuevo-peer",
      "registrado": true,
      "esNuevo": true
    }
  }
}
```

### Caso de Error - Datos Inválidos
```json
{
  "action": "descubrirPeers",
  "status": "error",
  "message": "Datos del peer inválidos",
  "data": {
    "campo": "ip/puerto",
    "motivo": "Campos requeridos"
  }
}
```

### Caso de Error - Puerto Inválido
```json
{
  "action": "descubrirPeers",
  "status": "error",
  "message": "Puerto inválido",
  "data": {
    "campo": "puerto",
    "motivo": "El puerto debe estar entre 1 y 65535"
  }
}
```

## Flujo de Operación

1. **Validación de Campos**: Verificar que IP y puerto estén presentes y sean válidos
2. **Verificar Peer Existente**: Si se proporciona peerId, verificar si ya está registrado
3. **Registrar Peer Nuevo**: Si el peer no existe, registrarlo automáticamente en la red
4. **Obtener Peers Activos**: Listar todos los peers que están en estado ONLINE
5. **Excluir Peer Solicitante**: No incluir al peer solicitante en la lista de peers disponibles
6. **Retornar Lista**: Devolver la lista de peers disponibles con su información de conexión

## Notas de Implementación
- Si el peer no proporciona un peerId, se debe crear uno nuevo y registrarlo automáticamente
- Solo se deben retornar peers en estado ONLINE para evitar intentos de conexión fallidos
- El peer solicitante no debe aparecer en su propia lista de peers disponibles
- Esta ruta puede ser llamada periódicamente por los peers para actualizar su lista de peers conocidos
- Si el peer ya existe pero con diferente IP/puerto, se debe actualizar su información
