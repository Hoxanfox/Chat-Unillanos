# Revisión e Implementación de la Relación Peer (Servidor) en el Dominio

## Fecha: 2 de noviembre de 2025

## Resumen Ejecutivo

Se ha revisado y mejorado la implementación del concepto "Peer" (servidor) en el proyecto. Se identificó que **faltaba la relación entre `Channel` y `Peer`**, lo cual es crítico para una arquitectura servidor-servidor.

---

## Problemas Identificados

### ✅ Lo que estaba bien implementado:
1. **Entidad `Peer`**: Correctamente definida en el dominio con ID y dirección IP
2. **Relación `User` - `Peer`**: Los usuarios tienen correctamente asociado su servidor padre mediante la columna `servidor_padre`
3. **Repositorio `PeerRepository`**: Implementado con métodos para buscar por IP y por ID
4. **Lógica en `UserServiceImpl`**: Al registrar y autenticar usuarios, se asigna automáticamente el servidor padre

### ❌ Lo que faltaba:
1. **Relación `Channel` - `Peer`**: Los canales NO tenían relación con el servidor donde fueron creados
2. **Asignación automática del Peer en canales**: Al crear canales (grupos o directos), no se registraba qué servidor lo creó

---

## Cambios Implementados

### 1. Dominio: `Channel.java`
**Ubicación**: `datos/server-dominio/src/main/java/com/arquitectura/domain/Channel.java`

#### Cambios realizados:
```java
// Se agregó la relación ManyToOne con Peer
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "servidor_padre", referencedColumnName = "id")
private Peer peerId;

// Se agregaron los getters y setters
public Peer getPeerId() { return peerId; }
public void setPeerId(Peer peerId) { this.peerId = peerId; }
```

**Impacto**:
- Ahora cada canal tiene registrado el servidor donde fue creado
- La columna `servidor_padre` en la tabla `channels` almacenará el ID del servidor padre
- Misma nomenclatura que en la tabla `users` para mantener consistencia

---

### 2. Lógica de Negocio: `ChannelServiceImpl.java`
**Ubicación**: `negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`

#### Cambios realizados:

1. **Se inyectaron nuevas dependencias**:
```java
private final PeerRepository peerRepository;
private final NetworkUtils networkUtils;
```

2. **Se modificó el método `crearCanal()`**:
```java
// Obtener el Peer (servidor) actual
String serverPeerAddress = networkUtils.getServerIPAddress();
Peer currentPeer = peerRepository.findByIp(serverPeerAddress)
        .orElseGet(() -> peerRepository.save(new Peer(serverPeerAddress)));

Channel newChannel = new Channel(requestDto.getChannelName(), owner, tipo);
newChannel.setPeerId(currentPeer); // Asignamos el servidor padre
```

3. **Se modificó el método `crearCanalDirecto()`**:
```java
// Obtener el Peer (servidor) actual
String serverPeerAddress = networkUtils.getServerIPAddress();
Peer currentPeer = peerRepository.findByIp(serverPeerAddress)
        .orElseGet(() -> peerRepository.save(new Peer(serverPeerAddress)));

Channel directChannel = new Channel(channelName, user1, TipoCanal.DIRECTO);
directChannel.setPeerId(currentPeer); // Asignamos el servidor padre
```

**Impacto**:
- Ahora al crear cualquier canal (grupo o directo), se registra automáticamente el servidor padre
- Si el servidor no existe en la base de datos, se crea automáticamente
- Mismo comportamiento que en el registro de usuarios

---

## Estructura de la Base de Datos

### Tabla `peers`
```
┌─────────────┬──────────┬─────────────┐
│ Columna     │ Tipo     │ Descripción │
├─────────────┼──────────┼─────────────┤
│ id          │ UUID     │ PK          │
│ ip          │ VARCHAR  │ IP servidor │
└─────────────┴──────────┴─────────────┘
```

### Tabla `users`
```
┌──────────────────┬──────────┬────────────────────────┐
│ Columna          │ Tipo     │ Descripción            │
├──────────────────┼──────────┼────────────────────────┤
│ user_id          │ UUID     │ PK                     │
│ servidor_padre   │ UUID     │ FK -> peers(id) ✅     │
│ username         │ VARCHAR  │                        │
│ ...              │          │                        │
└──────────────────┴──────────┴────────────────────────┘
```

### Tabla `channels`
```
┌──────────────────┬──────────┬────────────────────────┐
│ Columna          │ Tipo     │ Descripción            │
├──────────────────┼──────────┼────────────────────────┤
│ channel_id       │ UUID     │ PK                     │
│ servidor_padre   │ UUID     │ FK -> peers(id) ✅ NEW │
│ owner_id         │ UUID     │ FK -> users(user_id)   │
│ channel_name     │ VARCHAR  │                        │
│ tipo             │ VARCHAR  │ GRUPO/DIRECTO          │
└──────────────────┴──────────┴────────────────────────┘
```

---

## Beneficios de los Cambios

1. **Trazabilidad**: Ahora se puede saber qué servidor creó cada canal
2. **Resiliencia**: Posibilita implementar réplicas o recuperación cuando un servidor falle
3. **Arquitectura distribuida**: Mejor soporte para la arquitectura servidor-servidor
4. **Consistencia**: Tanto usuarios como canales tienen la misma relación con Peer
5. **Escalabilidad**: Facilita futuras implementaciones de sincronización entre servidores

---

## Escenarios de Uso

### Escenario 1: Usuario se registra en Servidor A
```
1. Usuario se registra -> Se crea en Servidor A
2. Se asigna Peer(IP_ServidorA) al usuario
3. Usuario queda vinculado al Servidor A
```

### Escenario 2: Usuario crea un grupo en Servidor A
```
1. Usuario autenticado crea un canal
2. Se asigna Peer(IP_ServidorA) al canal
3. Canal queda vinculado al Servidor A
4. Si Servidor A cae, se sabe qué servidor era el dueño original
```

### Escenario 3: Usuario de Servidor B quiere unirse a grupo de Servidor A
```
1. Usuario de Servidor B recibe invitación
2. El canal tiene servidor_padre = Servidor A
3. Se puede validar o redirigir según la lógica de negocio
4. Se mantiene la referencia al servidor original
```

---

## Próximos Pasos Recomendados

### Para mejorar la arquitectura servidor-servidor:

1. **Implementar sincronización de Peers**:
   - Crear un servicio que registre todos los servidores conocidos
   - Implementar heartbeat entre servidores

2. **Validación de pertenencia**:
   - Validar que los usuarios solo puedan acceder a canales de su servidor padre
   - O implementar federación entre servidores

3. **Réplicas y recuperación**:
   - Implementar replicación de canales críticos entre servidores
   - Sistema de failover cuando un servidor cae

4. **API servidor-servidor**:
   - Crear endpoints para comunicación entre servidores
   - Implementar autenticación entre servidores

5. **Dashboard de monitoreo**:
   - Vista de qué usuarios pertenecen a qué servidor
   - Vista de qué canales pertenecen a qué servidor
   - Estado de salud de cada servidor peer

---

## Nota sobre el Retorno de UserResponseDto

Respecto a tu pregunta inicial sobre si devolver `UserResponseDto` en `registrarUsuario()` y `autenticarUsuario()` consume memoria innecesaria:

- **`autenticarUsuario()`**: ✅ **SÍ necesita devolver** el DTO porque se usa para establecer la sesión del usuario
- **`registrarUsuario()`**: ✅ **Ya fue cambiado a void** (según el código revisado) porque nadie usa el retorno

El impacto en memoria es mínimo ya que:
1. Los DTOs son objetos pequeños
2. Se crean solo en el momento de la autenticación
3. Java los recolecta automáticamente cuando ya no se usan (Garbage Collection)
4. El beneficio de tener el DTO disponible supera el costo mínimo de memoria

---

## Conclusión

La implementación del campo `Peer` ahora está **correctamente implementada tanto para usuarios como para canales**. Esto proporciona una base sólida para:
- Identificar el origen de cada entidad (usuario o canal)
- Implementar lógica de federación entre servidores
- Manejar fallos y recuperación de servidores
- Escalar la arquitectura servidor-servidor

**Estado**: ✅ **Implementación completa y funcional**

