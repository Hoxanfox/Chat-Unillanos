# Ruta: verificarConexion (Ping/Pong)

## Descripción
Verifica que un peer está activo y respondiendo mediante un mecanismo de Ping/Pong. Esta ruta es fundamental para el monitoreo de salud de la red P2P y para detectar peers que han dejado de responder.

## Endpoint
**Acción:** `verificarConexion` o `ping`

## Request

### Estructura del Request
```json
{
  "action": "verificarConexion",
  "data": {
    "peerId": "uuid-del-peer-solicitante",
    "timestamp": "2024-11-07T10:30:00"
  }
}
```

### Campos del Request
- `peerId` (String, UUID, opcional): ID del peer que solicita la verificación
- `timestamp` (String, opcional): Timestamp del momento en que se envió el ping (formato ISO 8601)

## Response

### Caso de Éxito (Pong)
```json
{
  "action": "verificarConexion",
  "status": "success",
  "message": "pong",
  "data": {
    "peerIdRemoto": "uuid-del-peer-que-responde",
    "timestampPing": "2024-11-07T10:30:00",
    "timestampPong": "2024-11-07T10:30:01",
    "latenciaMs": 1000,
    "estadoServidor": "ONLINE",
    "version": "1.0.0"
  }
}
```

### Caso de Éxito - Ping Simple
```json
{
  "action": "ping",
  "status": "success",
  "message": "pong",
  "data": {
    "timestamp": "2024-11-07T10:30:01"
  }
}
```

## Flujo de Operación

1. **Recibir Ping**: El peer recibe una solicitud de verificación de conexión
2. **Validar Request**: Verificar que el request es válido (opcional validar peerId)
3. **Calcular Latencia**: Si se proporciona timestamp, calcular el tiempo de respuesta
4. **Preparar Pong**: Construir respuesta con información del servidor
5. **Enviar Respuesta**: Retornar inmediatamente con estado "pong"

## Notas de Implementación
- Esta ruta debe ser extremadamente rápida y ligera
- No debe realizar operaciones pesadas o consultas a base de datos
- El timestamp permite calcular la latencia de red entre peers
- La respuesta "pong" confirma que el peer está vivo y respondiendo
- Puede usarse para:
  - Monitoreo de salud de peers
  - Medición de latencia de red
  - Detección de peers caídos
  - Validación de conectividad antes de operaciones importantes
- Si un peer no responde después de varios intentos, puede marcarse como OFFLINE
- La versión del servidor puede ayudar a detectar incompatibilidades

## Variantes

### Ping Simple
```json
{
  "action": "ping",
  "data": {}
}
```

Respuesta:
```json
{
  "action": "ping",
  "status": "success",
  "message": "pong",
  "data": {
    "timestamp": "2024-11-07T10:30:01"
  }
}
```

### Ping con Información Detallada
```json
{
  "action": "verificarConexion",
  "data": {
    "peerId": "uuid-del-peer",
    "timestamp": "2024-11-07T10:30:00",
    "solicitarEstadisticas": true
  }
}
```

Respuesta:
```json
{
  "action": "verificarConexion",
  "status": "success",
  "message": "pong",
  "data": {
    "peerIdRemoto": "uuid-del-peer-que-responde",
    "timestampPing": "2024-11-07T10:30:00",
    "timestampPong": "2024-11-07T10:30:01",
    "latenciaMs": 1000,
    "estadoServidor": "ONLINE",
    "version": "1.0.0",
    "usuariosConectados": 5,
    "cargaCPU": 45.2,
    "memoriaUsada": 512
  }
}
```
