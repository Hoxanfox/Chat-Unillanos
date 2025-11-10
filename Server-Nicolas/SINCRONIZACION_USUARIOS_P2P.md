# Sincronización de Usuarios P2P

## Descripción

Se ha implementado una funcionalidad completa de sincronización de usuarios entre peers en la red P2P. Esta funcionalidad permite que cuando un usuario se autentica, el servidor consulte automáticamente a todos los peers activos para obtener sus usuarios y combinarlos con los usuarios locales antes de enviar la lista completa a los clientes.

## Archivos Modificados

### 1. `IPeerService.java`
**Ruta:** `negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/IPeerService.java`

Se agregó el método:
```java
List<java.util.Map<String, Object>> sincronizarUsuariosDeTodosLosPeers();
```

### 2. `PeerServiceImpl.java`
**Ruta:** `negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/PeerServiceImpl.java`

Se implementó el método `sincronizarUsuariosDeTodosLosPeers()` que:
- Obtiene la lista de peers activos en la red
- Filtra el peer local para no consultarse a sí mismo
- Itera sobre cada peer remoto y hace una petición P2P con la acción `sincronizarUsuarios`
- Combina los usuarios de todos los peers evitando duplicados
- Devuelve una lista unificada de usuarios

### 3. `IP2PFachada.java`
**Ruta:** `negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/p2p/IP2PFachada.java`

Se agregó el método a la interfaz de fachada:
```java
List<java.util.Map<String, Object>> sincronizarUsuariosDeTodosLosPeers();
```

### 4. `P2PFachadaImpl.java`
**Ruta:** `negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/p2p/P2PFachadaImpl.java`

Se implementó el método en la fachada que delega al servicio de peers.

### 5. `UserController.java`
**Ruta:** `transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/UserController.java`

Se modificó el método `broadcastContactListToAllClients()` para:
1. **Sincronizar usuarios de peers remotos** - Llama a `sincronizarUsuariosDeTodosLosPeers()`
2. **Obtener usuarios locales** - Consulta la base de datos local
3. **Combinar ambas listas** - Evita duplicados usando un Set de IDs
4. **Enviar broadcast** - Notifica a todos los clientes conectados con la lista completa

## Flujo de Operación

### Cuando un usuario se autentica:

```
1. Cliente envía authenticateUser
   ↓
2. UserController.handleAuthenticate()
   ↓
3. Usuario autenticado exitosamente
   ↓
4. broadcastContactListToAllClients() se ejecuta
   ↓
5. Sincronización P2P:
   ├─ Consulta Peer 1 (sincronizarUsuarios) → Obtiene usuarios del Peer 1
   ├─ Consulta Peer 2 (sincronizarUsuarios) → Obtiene usuarios del Peer 2
   └─ Consulta Peer N (sincronizarUsuarios) → Obtiene usuarios del Peer N
   ↓
6. Combina usuarios locales + usuarios de peers remotos
   ↓
7. Envía notificación PUSH "solicitarListaContactos" a todos los clientes
   ↓
8. Clientes reciben lista completa con usuarios de toda la red P2P
```

## Formato de Datos

### Respuesta de sincronizarUsuarios (desde cada peer):
```json
{
  "action": "sincronizarUsuarios",
  "status": "success",
  "data": {
    "usuarios": [
      {
        "usuarioId": "uuid-usuario-1",
        "username": "usuario1",
        "conectado": true,
        "peerId": "uuid-peer-1",
        "peerIp": "192.168.1.5",
        "peerPuerto": 9000
      }
    ],
    "totalUsuarios": 5,
    "usuariosConectados": 3,
    "fechaSincronizacion": "2025-11-10T10:30:00"
  }
}
```

### Notificación PUSH enviada a los clientes:
```json
{
  "action": "solicitarListaContactos",
  "data": {
    "contacts": [
      {
        "id": "uuid-usuario-1",
        "peerid": "uuid-peer-1",
        "nombre": "usuario1",
        "email": "usuario1@example.com",
        "estado": "online",
        "photoFileId": "path/to/photo.jpg"
      },
      {
        "id": "uuid-usuario-2",
        "peerid": "uuid-peer-2",
        "nombre": "usuario2",
        "email": null,
        "estado": "offline",
        "photoFileId": null
      }
    ],
    "total": 10
  }
}
```

## Características Importantes

### 1. **Evita Duplicados**
- Usa un `Set<String>` con los IDs de usuario para asegurar que no se agreguen usuarios duplicados
- Prioriza los usuarios locales sobre los de peers remotos

### 2. **No Recursivo**
- Cuando un peer recibe la petición `sincronizarUsuarios`, SOLO devuelve sus usuarios locales
- NO consulta a otros peers para evitar bucles infinitos
- El peer que inicia la sincronización es quien consulta múltiples peers

### 3. **Tolerante a Fallos**
- Si un peer no responde, continúa con los demás
- Captura excepciones individualmente para cada peer
- Registra logs detallados de cada operación

### 4. **Información Completa**
- Combina usuarios locales (con email y foto) con usuarios remotos
- Mantiene el `peerId` para saber dónde está cada usuario
- Preserva el estado online/offline

## Logs Generados

Cuando se ejecuta la sincronización, verás logs como:

```
🔄 [PeerService] Iniciando sincronización de usuarios de todos los peers...
→ [PeerService] Consultando usuarios de 2 peers remotos activos
  ├─ Consultando peer: Servidor-2 (192.168.1.5:9000)
  └─ ✓ Agregados 3 usuarios del peer Servidor-2
  ├─ Consultando peer: Servidor-3 (192.168.1.6:9000)
  └─ ✓ Agregados 2 usuarios del peer Servidor-3
✓ [PeerService] Sincronización completada. Total usuarios de peers remotos: 5

📋 [UserController] Obteniendo usuarios locales...
✓ [UserController] Obtenidos 4 usuarios locales de BD
✓ [UserController] Procesados 4 usuarios locales
✓ [UserController] Agregados 5 usuarios de peers remotos
📊 [UserController] Total usuarios combinados: 9
✅ [UserController] Notificación enviada. Total contactos: 9 (Locales: 4, Peers remotos: 5)
```

## Ruta P2P Utilizada

### sincronizarUsuarios
**Ubicación:** Ya existía en `rutasP2P/sincronizarUsuarios.md`

Esta ruta es manejada por el `PeerController` y devuelve únicamente los usuarios locales del peer consultado.

## Mejoras Futuras Posibles

1. **Caché de Sincronización**: Implementar un caché temporal para no consultar peers en cada autenticación
2. **Sincronización Incremental**: Solo sincronizar cambios desde la última consulta
3. **Compresión de Datos**: Comprimir la lista de usuarios si es muy grande
4. **Paginación**: Soportar paginación para redes con muchos usuarios
5. **WebSocket para Sincronización**: Usar WebSockets para sincronización en tiempo real

## Pruebas Recomendadas

1. **Autenticar usuario en Peer 1**
   - Verificar que recibe usuarios de Peer 2 y Peer 3

2. **Verificar no duplicados**
   - Tener el mismo usuario en múltiples peers
   - Verificar que solo aparece una vez en la lista

3. **Peer desconectado**
   - Desconectar un peer
   - Verificar que la sincronización continúa con los demás

4. **Red grande**
   - Probar con 5+ peers
   - Verificar tiempos de respuesta aceptables

## Estado de Compilación

✅ **Compilación Exitosa**
- Todos los módulos compilaron correctamente
- No hay errores de sintaxis
- Las interfaces y implementaciones están alineadas

---

**Fecha de Implementación:** 2025-11-10  
**Autor:** Sistema de Sincronización P2P

