# Guía de Integración - Invitaciones Cross-Server

## Paso 1: Modificar ChannelServiceImpl.java

Ubicación: `negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`

### 1.1 Agregar el nuevo servicio como dependencia

Busca la sección de campos (después de la línea 45), y agrega:

```java
private final ChannelInvitationP2PService channelInvitationP2PService;
```

### 1.2 Actualizar el constructor

Busca el constructor (línea 49) y modifícalo para incluir el nuevo servicio:

```java
@Autowired
public ChannelServiceImpl(ChannelRepository channelRepository, UserRepository userRepository, 
                          MembresiaCanalRepository membresiaCanalRepository, PeerRepository peerRepository,
                          NetworkUtils networkUtils, ApplicationEventPublisher eventPublisher,
                          UserPeerMappingService userPeerMappingService,
                          ChannelInvitationP2PService channelInvitationP2PService) {
    this.channelRepository = channelRepository;
    this.userRepository = userRepository;
    this.membresiaCanalRepository = membresiaCanalRepository;
    this.peerRepository = peerRepository;
    this.networkUtils = networkUtils;
    this.eventPublisher = eventPublisher;
    this.userPeerMappingService = userPeerMappingService;
    this.channelInvitationP2PService = channelInvitationP2PService;
}
```

### 1.3 Reemplazar el método invitarMiembro

Busca el método `invitarMiembro` (alrededor de la línea 238) y reemplázalo con:

```java
@Override
@Transactional
public void invitarMiembro(InviteMemberRequestDto inviteMemberRequestDto, UUID ownerId) throws Exception {
    Channel channel = channelRepository.findById(inviteMemberRequestDto.getChannelId())
            .orElseThrow(() -> new Exception("Canal no encontrado."));

    if (!channel.getOwner().getUserId().equals(ownerId)) {
        throw new Exception("Solo el propietario del canal puede enviar invitaciones.");
    }

    if (channel.getTipo() != TipoCanal.GRUPO) {
        throw new Exception("Solo se pueden enviar invitaciones a canales de tipo GRUPO.");
    }

    User userToInvite = userRepository.findById(inviteMemberRequestDto.getUserIdToInvite())
            .orElseThrow(() -> new Exception("Usuario a invitar no encontrado."));

    MembresiaCanalId membresiaId = new MembresiaCanalId(channel.getChannelId(), userToInvite.getUserId());

    // Verificar si ya existe una membresía
    if(membresiaCanalRepository.existsById(membresiaId)){
        throw new Exception("El usuario ya es miembro o tiene una invitación pendiente.");
    }

    // *** CAMBIO PRINCIPAL: Usar el servicio P2P para procesar la invitación ***
    channelInvitationP2PService.procesarInvitacion(channel, userToInvite, ownerId);
    
    System.out.println("✓ [ChannelService] Invitación procesada: " + userToInvite.getUsername() + 
                       " al canal " + channel.getName());
}
```

### 1.4 Actualizar el método responderInvitacion

Busca el método `responderInvitacion` (alrededor de la línea 258) y actualízalo:

```java
@Override
@Transactional
public void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId) throws Exception {
    MembresiaCanalId membresiaId = new MembresiaCanalId(requestDto.getChannelId(), userId);

    MembresiaCanal invitacion = membresiaCanalRepository.findById(membresiaId)
            .orElseThrow(() -> new Exception("No se encontró una invitación para este usuario en este canal."));

    if (invitacion.getEstado() != EstadoMembresia.PENDIENTE) {
        throw new Exception("No hay una invitación pendiente que responder.");
    }

    Channel channel = invitacion.getCanal();

    if (requestDto.isAccepted()) {
        invitacion.setEstado(EstadoMembresia.ACTIVO);
        membresiaCanalRepository.save(invitacion);
        
        // *** NUEVO: Notificar aceptación al peer del canal si es remoto ***
        channelInvitationP2PService.procesarAceptacionInvitacion(channel, userId);
        
        System.out.println("✓ [ChannelService] Invitación aceptada por " + userId);
    } else {
        membresiaCanalRepository.delete(invitacion);
        System.out.println("✓ [ChannelService] Invitación rechazada por " + userId);
    }
}
```

## Paso 2: Actualizar P2PNotificationController para manejar invitaciones

Ya creé el controlador, pero necesita mejorarse para persistir las invitaciones recibidas.

Ubicación: `transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/P2PNotificationController.java`

El método `handleNotificarInvitacionCanal` necesita guardar la invitación en la BD local.

## Resumen de Cambios

### Archivos Creados:
1. ✅ `ChannelInvitationP2PService.java` - Servicio para manejar invitaciones P2P

### Archivos a Modificar Manualmente:
1. ⚠️ `ChannelServiceImpl.java` - Integrar el nuevo servicio (seguir pasos arriba)
2. ⚠️ `P2PNotificationController.java` - Mejorar manejo de invitaciones entrantes

## Flujo Completo de Invitación Cross-Server

### Escenario: Alice (Servidor 1) invita a Bob (Servidor 2) a un canal

```
1. Cliente Alice → Servidor 1: Invitar a Bob
   ↓
2. Servidor 1: ChannelService.invitarMiembro()
   ↓
3. Servidor 1: ChannelInvitationP2PService.procesarInvitacion()
   ├─ Guarda invitación PENDIENTE en BD
   ├─ Detecta que Bob está en Servidor 2
   └─ Llama peerNotificationService.notificarInvitacionCanal()
   ↓
4. Servidor 1 → Servidor 2: Notificación TCP P2P
   {
     action: "notificarInvitacionCanal",
     payload: {
       canalId: "xxx",
       usuarioInvitadoId: "bob-id",
       usuarioInvitadorId: "alice-id"
     }
   }
   ↓
5. Servidor 2: P2PNotificationController.handleNotificarInvitacionCanal()
   ├─ Guarda invitación en BD del Servidor 2
   └─ Publica evento para notificar a Bob si está conectado
   ↓
6. Servidor 2 → Cliente Bob: Notificación de invitación
   ↓
7. Cliente Bob → Servidor 2: Aceptar invitación
   ↓
8. Servidor 2: ChannelService.responderInvitacion()
   ├─ Marca invitación como ACTIVA en BD
   └─ ChannelInvitationP2PService.procesarAceptacionInvitacion()
   ↓
9. Servidor 2 → Servidor 1: Notificación de aceptación
   {
     action: "notificarAceptacionInvitacion",
     payload: {
       canalId: "xxx",
       usuarioId: "bob-id"
     }
   }
   ↓
10. Servidor 1: Actualiza estado de Bob como miembro activo
   ↓
11. ✅ Bob ahora puede ver mensajes del canal creado en Servidor 1
```

## Próximos Pasos

Después de hacer los cambios manuales en `ChannelServiceImpl.java`:

1. Compilar: `mvn clean compile`
2. Probar invitación cross-server
3. Verificar logs en ambos servidores
4. Confirmar que las invitaciones se sincronizan correctamente

## Logs Esperados

### Servidor 1 (cuando Alice invita a Bob):
```
→ [ChannelInvitation] Invitación guardada en BD: Bob al canal TestChannel
→ [ChannelInvitation] Notificando invitación a peer {servidor-2-id}
  Canal: TestChannel (canal-id)
  Usuario invitado: Bob
✓ [ChannelInvitation] Invitación notificada exitosamente a peer {servidor-2-id}
```

### Servidor 2 (cuando recibe la notificación):
```
→ [P2PNotificationController] Procesando acción P2P: notificarInvitacionCanal
→ [P2PNotificationController] Invitación a canal recibida:
  Canal: canal-id
  Usuario invitado: bob-id
  Usuario invitador: alice-id
✓ [P2PNotificationController] Invitación procesada
```

### Servidor 2 (cuando Bob acepta):
```
✓ [ChannelService] Invitación aceptada por bob-id
→ [ChannelInvitation] Procesando aceptación de invitación
→ [ChannelInvitation] Notificando aceptación al peer del canal: {servidor-1-id}
✓ [ChannelInvitation] Aceptación notificada exitosamente
```

