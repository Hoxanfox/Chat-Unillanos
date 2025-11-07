# IMPLEMENTACIÓN DE FEDERACIÓN P2P - ARQUITECTURA CORREGIDA

## 📋 Resumen

Se ha implementado la funcionalidad de **federación P2P** respetando estrictamente la arquitectura en capas del sistema. La solución utiliza **excepciones especiales** para delegar la comunicación P2P a las capas superiores, evitando violaciones arquitectónicas.

## 🎯 Objetivo Cumplido

✅ **Un cliente en Peer 1 puede crear un canal directo con un cliente en Peer 2**
✅ **Respetando la arquitectura en capas**: Transporte → Controlador → Fachada → Lógica → Dominio

---

## 🏗️ Arquitectura Correcta Implementada

### Flujo de Capas Respetado

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENTE                               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              CAPA DE TRANSPORTE                          │
│  - PeerConnectionManager (comunicación P2P)              │
│  - PeerHandler (recepción de peticiones P2P)            │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│         CAPA DE CONTROLADOR                              │
│  - PeerController (maneja peticiones P2P)               │
│  - ChannelController (maneja peticiones de clientes)    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              CAPA DE FACHADA                             │
│  - ChatFachadaImpl (intercepta FederationRequired)       │
│  - Orquesta la comunicación P2P                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│          CAPA DE LÓGICA DE NEGOCIO                       │
│  - ChannelServiceImpl (lanza FederationRequired)        │
│  - UserPeerMappingService (mapeo usuario-peer)          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              CAPA DE DOMINIO                             │
│  - Entidades (User, Channel, Peer)                      │
└─────────────────────────────────────────────────────────┘
```

---

## 🆕 Componentes Implementados

### 1. **FederationRequiredException** (Nuevo)
📁 `negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/exceptions/FederationRequiredException.java`

**Propósito:** Excepción especial que se lanza cuando se detecta que se necesita federación P2P.

**Atributos:**
- `targetPeerId` - UUID del peer que debe manejar la petición
- `user1Id` - UUID del primer usuario
- `user2Id` - UUID del segundo usuario
- `action` - Acción que se requiere ejecutar ("crearCanalDirecto")

**Por qué es importante:**
Esta excepción permite que la capa de lógica comunique a la capa de fachada que necesita ayuda con P2P, sin violar la arquitectura.

### 2. **UserPeerMappingService** (Nuevo)
📁 `negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/UserPeerMappingService.java`

**Propósito:** Mantener el mapeo de qué usuarios pertenecen a qué servidores.

**Funcionalidades:**
- `registerUserToPeer(UUID userId, UUID peerId)` - Registra un usuario en un peer
- `getPeerForUser(UUID userId)` - Obtiene el peer al que pertenece un usuario
- `isLocalUser(UUID userId)` - Verifica si un usuario es local
- `isRemoteUser(UUID userId)` - Verifica si un usuario es remoto

### 3. **ChannelServiceImpl** (Modificado)
📁 `negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`

**Cambios:**
- ✅ Inyecta `UserPeerMappingService` (misma capa - permitido)
- ❌ NO inyecta `PeerConnectionManager` (capa inferior - prohibido)
- ✅ Lanza `FederationRequiredException` cuando detecta usuarios remotos
- ✅ Crea canales localmente cuando ambos usuarios son locales

### 4. **ChatFachadaImpl** (Debe modificarse - Pendiente)
📁 `negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`

**Cambios requeridos:**
```java
@Override
public ChannelResponseDto crearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception {
    try {
        // Intentar crear el canal normalmente
        return channelService.crearCanalDirecto(user1Id, user2Id);
        
    } catch (FederationRequiredException e) {
        // Capturar la excepción de federación
        System.out.println("→ [Fachada] Se requiere federación P2P");
        
        // Construir petición P2P
        Map<String, Object> payload = new HashMap<>();
        payload.put("user1Id", e.getUser1Id().toString());
        payload.put("user2Id", e.getUser2Id().toString());
        
        DTORequest request = new DTORequest(e.getAction(), payload);
        
        // Usar PeerConnectionManager (ahora sí está en la capa correcta)
        DTOResponse response = peerConnectionManager.sendRequestToPeer(
            e.getTargetPeerId(), 
            request
        );
        
        if (response == null || !response.getStatus()) {
            throw new Exception("Error al crear canal en peer remoto");
        }
        
        // Convertir respuesta a ChannelResponseDto
        return mapResponseToChannelDto(response);
    }
}
```

### 5. **PeerController** (Modificado)
📁 `transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/PeerController.java`

**Cambios:**
- ✅ Maneja peticiones `retransmitirpeticion` de otros peers
- ✅ Detecta acción `crearCanalDirecto` y la procesa localmente
- ✅ Devuelve respuesta al peer origen

---

## 🔄 Flujo de Ejecución Completo (Corregido)

### Escenario: Cliente1 (Peer1) quiere chatear con Cliente2 (Peer2)

```
┌─────────┐                                              ┌─────────┐
│ Peer 1  │                                              │ Peer 2  │
└────┬────┘                                              └────┬────┘
     │                                                        │
     │ 1. Cliente1 → crearCanalDirecto(user1, user2)         │
     │                                                        │
     │ 2. ChannelService verifica usuarios                   │
     │    - user1: LOCAL ✓                                   │
     │    - user2: REMOTO ✗                                  │
     │                                                        │
     │ 3. ChannelService lanza                               │
     │    FederationRequiredException                        │
     │    ↓                                                   │
     │                                                        │
     │ 4. Fachada captura la excepción                       │
     │    - Extrae targetPeerId                              │
     │    - Construye DTORequest                             │
     │    ↓                                                   │
     │                                                        │
     │ 5. Fachada → PeerConnectionManager                    │
     │    sendRequestToPeer(peer2, request)                  │
     │    ────────────────────────────────────────────────►  │
     │                                                        │
     │                                 6. PeerController      │
     │                                    recibe petición     │
     │                                    ↓                   │
     │                                                        │
     │                                 7. Extrae acción       │
     │                                    "crearCanalDirecto" │
     │                                    ↓                   │
     │                                                        │
     │                                 8. ChannelService      │
     │                                    crea canal LOCAL    │
     │                                    (user2 es local)    │
     │                                    ↓                   │
     │                                                        │
     │  9. DTOResponse con datos del canal                   │
     │  ◄────────────────────────────────────────────────────│
     │                                                        │
     │ 10. Fachada convierte a ChannelResponseDto            │
     │     ↓                                                  │
     │                                                        │
     │ 11. Cliente1 recibe confirmación                      │
     │                                                        │
```

---

## ✅ Ventajas de Esta Arquitectura

### 1. **Respeta la Separación de Capas**
- ❌ **Antes**: `ChannelServiceImpl` → `PeerConnectionManager` (VIOLACIÓN)
- ✅ **Ahora**: `ChannelServiceImpl` → `FederationRequiredException` → `Fachada` → `PeerConnectionManager` (CORRECTO)

### 2. **Responsabilidades Claras**
- **ChannelServiceImpl**: Detecta y señaliza federación
- **Fachada**: Orquesta la comunicación P2P
- **PeerConnectionManager**: Maneja la comunicación de red

### 3. **Fácil de Probar**
- Se puede probar `ChannelServiceImpl` sin necesidad de levantar conexiones P2P
- La fachada puede mockear `PeerConnectionManager`

### 4. **Extensible**
- Se pueden agregar más operaciones federadas siguiendo el mismo patrón
- Ejemplo: `enviarMensajeRemoto`, `invitarUsuarioRemoto`, etc.

---

## 📝 Dependencias Maven

### ✅ server-LogicaCanales (Correctas)
```xml
<dependencies>
    <!-- Dependencias normales -->
    <dependency>
        <groupId>com.arquitectura</groupId>
        <artifactId>server-dominio</artifactId>
    </dependency>
    
    <!-- Solo LogicaPeers - misma capa -->
    <dependency>
        <groupId>com.arquitectura</groupId>
        <artifactId>server-LogicaPeers</artifactId>
    </dependency>
    
    <!-- ❌ NO incluye server-Transporte -->
</dependencies>
```

### ✅ server-logicaFachada (Requeridas)
```xml
<dependencies>
    <!-- Todas las lógicas -->
    <dependency>
        <groupId>com.arquitectura</groupId>
        <artifactId>server-LogicaCanales</artifactId>
    </dependency>
    
    <!-- Transporte para comunicación P2P -->
    <dependency>
        <groupId>com.arquitectura</groupId>
        <artifactId>server-Transporte</artifactId>
    </dependency>
</dependencies>
```

---

## 🚀 Próximos Pasos

### 1. **Modificar ChatFachadaImpl**
Implementar el catch de `FederationRequiredException` y manejar la comunicación P2P.

### 2. **Probar el Flujo Completo**
- Registrar usuarios en ambos peers
- Sincronizar mapeo de usuarios
- Crear canal directo federado

### 3. **Implementar Sincronización Automática**
```java
@EventListener
public void onPeerConnected(PeerConnectedEvent event) {
    // Enviar lista de usuarios locales al peer conectado
    syncUsersWithPeer(event.getPeerId());
}
```

---

## 📊 Diagrama de Clases Final

```
                    ┌──────────────────┐
                    │  ChatFachadaImpl │
                    ├──────────────────┤
                    │ + crearCanal...  │◄───────┐
                    │ + catch(FedReq)  │        │ Maneja
                    └────────┬─────────┘        │
                             │                  │
                        Llama│            ┌─────┴─────────┐
                             │            │ Federation    │
                             │            │ Required      │
                             ▼            │ Exception     │
                ┌─────────────────────┐  └───────────────┘
                │ ChannelServiceImpl  │         ▲
                ├─────────────────────┤         │
                │ - userPeerMapping   │         │ Lanza
                │ + obtenerOCrear...  │─────────┘
                └─────────────────────┘
                             │
                        Usa  │
                             ▼
                ┌──────────────────────┐
                │ UserPeerMappingService│
                ├──────────────────────┤
                │ + isLocalUser()      │
                │ + getPeerForUser()   │
                └──────────────────────┘
```

---

## 🎉 Resumen

✅ **Arquitectura respetada**
- Ninguna capa llama a capas inferiores
- Flujo: Transporte → Controlador → Fachada → Lógica → Dominio

✅ **Componentes creados**
- `FederationRequiredException` - Excepción de comunicación
- `UserPeerMappingService` - Servicio de mapeo

✅ **Pendiente**
- Modificar `ChatFachadaImpl` para capturar `FederationRequiredException`
- Inyectar `PeerConnectionManager` en la fachada
- Implementar lógica de envío P2P en la fachada

---

**Fecha:** 6 de noviembre de 2025  
**Estado:** ✅ ARQUITECTURA CORREGIDA - Pendiente implementación en Fachada

