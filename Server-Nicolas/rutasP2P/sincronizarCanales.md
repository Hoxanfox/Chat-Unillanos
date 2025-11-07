# Ruta: sincronizarCanales (Canales Globales)

## Descripción
Sincroniza la información de canales globales entre peers de la red P2P. Permite que todos los peers tengan una vista consistente de los canales disponibles, sus miembros y su estado.

## Endpoint
**Acción:** `sincronizarCanales`

## Request

### Estructura del Request
```json
{
  "action": "sincronizarCanales",
  "data": {
    "peerId": "uuid-del-peer-solicitante",
    "incluirMiembros": true
  }
}
```

### Campos del Request
- `peerId` (String, UUID, opcional): ID del peer que solicita la sincronización
- `incluirMiembros` (Boolean, opcional): Si se debe incluir la lista de miembros de cada canal (por defecto: false)

## Response

### Caso de Éxito - Con Miembros
```json
{
  "action": "sincronizarCanales",
  "status": "success",
  "message": "Canales sincronizados exitosamente",
  "data": {
    "canales": [
      {
        "canalId": "uuid-canal-1",
        "nombre": "General",
        "tipo": "PUBLICO",
        "propietarioId": "uuid-usuario-1",
        "propietarioUsername": "admin",
        "fechaCreacion": "2024-11-01T10:00:00",
        "miembros": [
          {
            "usuarioId": "uuid-usuario-1",
            "username": "admin",
            "rol": "ADMIN"
          },
          {
            "usuarioId": "uuid-usuario-2",
            "username": "usuario1",
            "rol": "MIEMBRO"
          }
        ],
        "totalMiembros": 2
      },
      {
        "canalId": "uuid-canal-2",
        "nombre": "Desarrollo",
        "tipo": "PRIVADO",
        "propietarioId": "uuid-usuario-1",
        "propietarioUsername": "admin",
        "fechaCreacion": "2024-11-02T14:30:00",
        "miembros": [
          {
            "usuarioId": "uuid-usuario-1",
            "username": "admin",
            "rol": "ADMIN"
          }
        ],
        "totalMiembros": 1
      }
    ],
    "totalCanales": 2,
    "canalesPublicos": 1,
    "canalesPrivados": 1,
    "fechaSincronizacion": "2024-11-07T10:30:00"
  }
}
```

### Caso de Éxito - Sin Miembros
```json
{
  "action": "sincronizarCanales",
  "status": "success",
  "message": "Canales sincronizados exitosamente",
  "data": {
    "canales": [
      {
        "canalId": "uuid-canal-1",
        "nombre": "General",
        "tipo": "PUBLICO",
        "propietarioId": "uuid-usuario-1",
        "propietarioUsername": "admin",
        "fechaCreacion": "2024-11-01T10:00:00",
        "totalMiembros": 2
      },
      {
        "canalId": "uuid-canal-2",
        "nombre": "Desarrollo",
        "tipo": "PRIVADO",
        "propietarioId": "uuid-usuario-1",
        "propietarioUsername": "admin",
        "fechaCreacion": "2024-11-02T14:30:00",
        "totalMiembros": 1
      }
    ],
    "totalCanales": 2,
    "canalesPublicos": 1,
    "canalesPrivados": 1,
    "fechaSincronizacion": "2024-11-07T10:30:00"
  }
}
```

### Caso de Éxito - Sin Canales
```json
{
  "action": "sincronizarCanales",
  "status": "success",
  "message": "Canales sincronizados exitosamente",
  "data": {
    "canales": [],
    "totalCanales": 0,
    "canalesPublicos": 0,
    "canalesPrivados": 0,
    "fechaSincronizacion": "2024-11-07T10:30:00"
  }
}
```

### Caso de Error
```json
{
  "action": "sincronizarCanales",
  "status": "error",
  "message": "Error al sincronizar canales",
  "data": {
    "motivo": "Error interno del servidor"
  }
}
```

## Flujo de Operación

1. **Validar Request**: Verificar que el request es válido
2. **Obtener Canales**: Consultar todos los canales del sistema
3. **Clasificar Canales**: Separar canales en PUBLICO y PRIVADO
4. **Obtener Miembros**: Si se solicita, obtener lista de miembros de cada canal
5. **Preparar Información**: Construir estructura con información de canales
6. **Agregar Estadísticas**: Incluir contadores y métricas
7. **Timestamp**: Agregar fecha y hora de sincronización
8. **Retornar Respuesta**: Enviar información estructurada

## Notas de Implementación
- Esta ruta permite mantener sincronizada la información de canales en toda la red
- Útil para:
  - Mantener caché de canales en cada peer
  - Permitir que usuarios vean canales disponibles sin consultar al servidor central
  - Facilitar la búsqueda de canales
  - Mostrar estadísticas de canales
- Si `incluirMiembros` es true:
  - Se debe incluir la lista completa de miembros de cada canal
  - Incluir el rol de cada miembro (ADMIN/MIEMBRO)
  - Puede ser una operación costosa con muchos canales
- Si `incluirMiembros` es false:
  - Solo incluir el conteo de miembros
  - Operación más rápida y ligera
- Los canales PRIVADOS solo deben incluir información básica
- El timestamp ayuda a determinar qué tan reciente es la información
- Considerar implementar sincronización incremental (solo cambios desde última sincronización)
- Esta información puede ser cacheada por los peers con un TTL apropiado
