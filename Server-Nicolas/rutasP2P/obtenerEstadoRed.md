# Ruta: obtenerEstadoRed (Topología)

## Descripción
Obtiene el estado completo de la red P2P, incluyendo información sobre todos los peers, usuarios conectados, y la topología de la red. Esta ruta proporciona una vista general del sistema distribuido.

## Endpoint
**Acción:** `obtenerEstadoRed`

## Request

### Estructura del Request
```json
{
  "action": "obtenerEstadoRed",
  "data": {
    "peerId": "uuid-del-peer-solicitante",
    "incluirDetalles": true
  }
}
```

### Campos del Request
- `peerId` (String, UUID, opcional): ID del peer que solicita el estado
- `incluirDetalles` (Boolean, opcional): Si se deben incluir detalles adicionales (por defecto: false)

## Response

### Caso de Éxito - Vista Completa
```json
{
  "action": "obtenerEstadoRed",
  "status": "success",
  "message": "Estado de la red obtenido exitosamente",
  "data": {
    "topologia": {
      "totalPeers": 3,
      "peersOnline": 2,
      "peersOffline": 1,
      "peers": [
        {
          "peerId": "uuid-peer-1",
          "ip": "192.168.1.5",
          "puerto": 9000,
          "estado": "ONLINE",
          "usuariosConectados": 5,
          "ultimoLatido": "2024-11-07T10:30:00"
        },
        {
          "peerId": "uuid-peer-2",
          "ip": "192.168.1.6",
          "puerto": 9001,
          "estado": "ONLINE",
          "usuariosConectados": 3,
          "ultimoLatido": "2024-11-07T10:29:55"
        },
        {
          "peerId": "uuid-peer-3",
          "ip": "192.168.1.7",
          "puerto": 9002,
          "estado": "OFFLINE",
          "usuariosConectados": 0,
          "ultimoLatido": "2024-11-07T10:25:00"
        }
      ]
    },
    "usuarios": {
      "totalUsuarios": 10,
      "usuariosConectados": 8,
      "usuariosOffline": 2,
      "distribucion": [
        {
          "peerId": "uuid-peer-1",
          "cantidad": 5
        },
        {
          "peerId": "uuid-peer-2",
          "cantidad": 3
        }
      ]
    },
    "estadisticas": {
      "tiempoActividad": "2h 30m",
      "mensajesEnrutados": 1250,
      "latenciaPromedio": 150
    },
    "fechaConsulta": "2024-11-07T10:30:00"
  }
}
```

### Caso de Éxito - Vista Básica
```json
{
  "action": "obtenerEstadoRed",
  "status": "success",
  "message": "Estado de la red obtenido exitosamente",
  "data": {
    "topologia": {
      "totalPeers": 3,
      "peersOnline": 2,
      "peersOffline": 1
    },
    "usuarios": {
      "totalUsuarios": 10,
      "usuariosConectados": 8,
      "usuariosOffline": 2
    },
    "fechaConsulta": "2024-11-07T10:30:00"
  }
}
```

### Caso de Error
```json
{
  "action": "obtenerEstadoRed",
  "status": "error",
  "message": "Error al obtener estado de la red",
  "data": {
    "motivo": "Error interno del servidor"
  }
}
```

## Flujo de Operación

1. **Validar Request**: Verificar que el request es válido
2. **Obtener Peers**: Consultar todos los peers registrados en la red
3. **Clasificar Peers**: Separar peers en ONLINE y OFFLINE
4. **Obtener Usuarios**: Consultar información de usuarios conectados
5. **Calcular Distribución**: Determinar cuántos usuarios hay en cada peer
6. **Agregar Estadísticas**: Si se solicitan detalles, incluir estadísticas adicionales
7. **Construir Topología**: Ensamblar la vista completa de la red
8. **Retornar Respuesta**: Enviar información estructurada

## Notas de Implementación
- Esta ruta proporciona una vista centralizada de la red distribuida
- Útil para:
  - Monitoreo y administración de la red
  - Balanceo de carga entre peers
  - Detección de problemas de conectividad
  - Visualización de la topología de red
  - Análisis de distribución de usuarios
- Si `incluirDetalles` es true, se deben incluir:
  - Información detallada de cada peer
  - Estadísticas de rendimiento
  - Historial de latidos
  - Métricas de uso
- La información debe ser actual (no cacheada por mucho tiempo)
- Puede ser una operación costosa si hay muchos peers
- Considerar implementar paginación si la red crece mucho
- El timestamp de consulta ayuda a determinar qué tan reciente es la información
