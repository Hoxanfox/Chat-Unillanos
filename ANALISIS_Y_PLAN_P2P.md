# 🌐 Análisis y Plan de Implementación P2P

**Fecha:** 2025-11-06  
**Objetivo:** Convertir el sistema actual en una arquitectura P2P distribuida

---

## 📊 ESTADO ACTUAL DEL PROYECTO

### ✅ Lo que YA existe:

1. **Entidad Peer** (`Server-Nicolas/datos/server-dominio/src/main/java/com/arquitectura/domain/Peer.java`)
   - ✅ Tiene `peerId` (UUID)
   - ✅ Tiene `ip` (String)
   - ❌ Falta `puerto` (int)
   - ❌ Falta `conectado` (String/Enum)
   - ❌ Falta `ultimoLatido` (LocalDateTime)

2. **PeerRepository** (`Server-Nicolas/datos/server-persistencia/src/main/java/com/arquitectura/persistence/repository/PeerRepository.java`)
   - ✅ Métodos básicos de JPA
   - ✅ `findByIp(String ip)`
   - ✅ `findByPeerId(UUID peerId)`

3. **Infraestructura de red**
   - ✅ Servidor TCP funcional
   - ✅ Sistema de mensajería JSON
   - ✅ Gestión de conexiones cliente-servidor

### ❌ Lo que FALTA implementar:

1. **Modelo de dominio completo**
   - Puerto del peer
   - Estado de conexión (ONLINE/OFFLINE)
   - Timestamp del último latido
   - Metadatos adicionales

2. **Capa de negocio P2P**
   - Servicio de gestión de peers
   - Lógica de descubrimiento de peers
   - Sistema de heartbeat (latidos)
   - Retransmisión de peticiones

3. **Controlador P2P**
   - Endpoints para gestión de peers
   - Manejo de retransmisiones
   - Actualización de estado de peers

4. **Cliente P2P** (comunicación servidor-servidor)
   - Cliente HTTP/TCP para comunicación entre servidores
   - Manejo de timeouts y reconexiones
   - Pool de conexiones

---

## 🏗️ ARQUITECTURA P2P PROPUESTA

```
┌─────────────────────────────────────────────────────────────┐
│                    ARQUITECTURA P2P                          │
└─────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│  Servidor A  │◄───────►│  Servidor B  │◄───────►│  Servidor C  │
│  (Peer A)    │         │  (Peer B)    │         │  (Peer C)    │
└──────┬───────┘         └──────┬───────┘         └──────┬───────┘
       │                        │                        │
       │ Clientes               │ Clientes               │ Clientes
       ▼                        ▼                        ▼
   ┌───────┐               ┌───────┐               ┌───────┐
   │Client1│               │Client2│               │Client3│
   └───────┘               └───────┘               └───────┘

Cada servidor:
1. Mantiene lista de peers conocidos
2. Envía heartbeats periódicos
3. Retransmite peticiones a otros peers
4. Sincroniza datos cuando es necesario
```

---

## 📋 PLAN DE IMPLEMENTACIÓN COMPLETO

### **FASE 1: Actualizar el Modelo de Dominio** ⏱️ 30 min

#### 1.1. Actualizar entidad `Peer`
**Archivo:** `Server-Nicolas/datos/server-dominio/src/main/java/com/arquitectura/domain/Peer.java`

**Agregar campos:**
```java
@Column(name = "puerto", nullable = false)
private int puerto;

@Column(name = "conectado", nullable = false, length = 20)
private String conectado; // "ONLINE" o "OFFLINE"

@Column(name = "ultimo_latido")
private LocalDateTime ultimoLatido;

@Column(name = "nombre_servidor", length = 100)
private String nombreServidor; // Opcional: nombre descriptivo
```

#### 1.2. Crear Enum para estado de conexión
**Archivo nuevo:** `Server-Nicolas/datos/server-dominio/src/main/java/com/arquitectura/domain/enums/EstadoPeer.java`

```java
public enum EstadoPeer {
    ONLINE,
    OFFLINE,
    DESCONOCIDO
}
```

#### 1.3. Actualizar PeerRepository
**Archivo:** `Server-Nicolas/datos/server-persistencia/src/main/java/com/arquitectura/persistence/repository/PeerRepository.java`

**Agregar métodos:**
```java
List<Peer> findByConectado(String conectado);
List<Peer> findAllByOrderByUltimoLatidoDesc();
Optional<Peer> findByIpAndPuerto(String ip, int puerto);

@Modifying
@Query("UPDATE Peer p SET p.conectado = :estado WHERE p.peerId = :peerId")
void actualizarEstado(@Param("peerId") UUID peerId, @Param("estado") String estado);

@Modifying
@Query("UPDATE Peer p SET p.ultimoLatido = :timestamp WHERE p.peerId = :peerId")
void actualizarLatido(@Param("peerId") UUID peerId, @Param("timestamp") LocalDateTime timestamp);
```

---

### **FASE 2: Crear DTOs para P2P** ⏱️ 20 min

#### 2.1. DTOs de Request
**Directorio:** `Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/p2p/`

**Archivos a crear:**
1. `AddPeerRequestDto.java`
2. `ReportHeartbeatRequestDto.java`
3. `RetransmitRequestDto.java`

#### 2.2. DTOs de Response
1. `PeerResponseDto.java`
2. `PeerListResponseDto.java`
3. `HeartbeatResponseDto.java`

---

### **FASE 3: Crear Servicio de Gestión de Peers** ⏱️ 1 hora

#### 3.1. Interfaz del servicio
**Archivo nuevo:** `Server-Nicolas/negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/IPeerService.java`

**Métodos principales:**
```java
public interface IPeerService {
    // Gestión de peers
    PeerResponseDto agregarPeer(String ip, int puerto);
    List<PeerResponseDto> listarPeersDisponibles();
    void actualizarEstadoPeer(UUID peerId, String estado);
    
    // Heartbeat
    void reportarLatido(UUID peerId);
    void verificarPeersInactivos();
    
    // Retransmisión
    DTOResponse retransmitirPeticion(UUID peerDestinoId, DTORequest peticionOriginal);
    
    // Descubrimiento
    void descubrirPeers();
    Peer obtenerPeerActual();
}
```

#### 3.2. Implementación del servicio
**Archivo nuevo:** `Server-Nicolas/negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/PeerServiceImpl.java`

**Funcionalidades clave:**
- Gestión de lista de peers
- Sistema de heartbeat con timeout configurable
- Cliente HTTP para comunicación peer-to-peer
- Manejo de errores y reconexiones

---

### **FASE 4: Crear Cliente P2P (Servidor-Servidor)** ⏱️ 1 hora

#### 4.1. Cliente HTTP para comunicación entre servidores
**Archivo nuevo:** `Server-Nicolas/comunes/server-Utils/src/main/java/com/arquitectura/utils/p2p/PeerClient.java`

**Funcionalidades:**
```java
public class PeerClient {
    public DTOResponse enviarPeticion(String ip, int puerto, DTORequest request);
    public boolean verificarConexion(String ip, int puerto);
    public void cerrarConexion();
}
```

#### 4.2. Pool de conexiones
**Archivo nuevo:** `Server-Nicolas/comunes/server-Utils/src/main/java/com/arquitectura/utils/p2p/PeerConnectionPool.java`

---

### **FASE 5: Crear Controlador P2P** ⏱️ 45 min

#### 5.1. Controlador de Peers
**Archivo nuevo:** `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/PeerController.java`

**Endpoints a implementar:**
1. `añadirPeer` - Agregar un nuevo peer a la red
2. `listarPeersDisponibles` - Obtener lista de peers
3. `reportarLatido` - Recibir heartbeat de un peer
4. `retransmitirPeticion` - Retransmitir petición a otro peer
5. `actualizarListaPeers` - Sincronizar lista de peers

---

### **FASE 6: Sistema de Heartbeat Automático** ⏱️ 30 min

#### 6.1. Servicio de Heartbeat
**Archivo nuevo:** `Server-Nicolas/negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/HeartbeatService.java`

**Funcionalidades:**
```java
@Service
public class HeartbeatService {
    @Scheduled(fixedRate = 30000) // Cada 30 segundos
    public void enviarHeartbeats();
    
    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    public void verificarPeersInactivos();
}
```

---

### **FASE 7: Integración con Fachada** ⏱️ 20 min

#### 7.1. Actualizar IChatFachada
**Archivo:** `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/IChatFachada.java`

**Agregar métodos P2P:**
```java
// Métodos P2P
PeerResponseDto agregarPeer(String ip, int puerto);
List<PeerResponseDto> listarPeersDisponibles();
void reportarLatido(UUID peerId);
DTOResponse retransmitirPeticion(UUID peerDestinoId, DTORequest peticion);
```

---

### **FASE 8: Configuración y Propiedades** ⏱️ 15 min

#### 8.1. Archivo de configuración
**Archivo:** `Server-Nicolas/comunes/server-app/src/main/resources/application.properties`

**Agregar propiedades P2P:**
```properties
# Configuración P2P
p2p.enabled=true
p2p.puerto=22100
p2p.heartbeat.interval=30000
p2p.heartbeat.timeout=90000
p2p.discovery.enabled=true
p2p.discovery.interval=300000
```

---

### **FASE 9: Testing y Validación** ⏱️ 1 hora

#### 9.1. Tests unitarios
- Test de PeerService
- Test de PeerController
- Test de HeartbeatService

#### 9.2. Tests de integración
- Test de comunicación peer-to-peer
- Test de retransmisión de peticiones
- Test de sincronización de datos

---

## 🎯 ENDPOINTS P2P A IMPLEMENTAR

### 1. **añadirPeer**
```json
Request:
{
  "action": "añadirPeer",
  "payload": {
    "ip": "192.168.1.10",
    "puerto": 22100
  }
}

Response:
{
  "action": "añadirPeer",
  "status": "success",
  "message": "Peer añadido exitosamente",
  "data": {
    "peerId": "uuid-generado",
    "ip": "192.168.1.10",
    "puerto": 22100,
    "conectado": "ONLINE"
  }
}
```

### 2. **listarPeersDisponibles**
```json
Request:
{
  "action": "listarPeersDisponibles",
  "payload": {}
}

Response:
{
  "action": "listarPeersDisponibles",
  "status": "success",
  "message": "Lista de peers obtenida",
  "data": [
    {
      "peerId": "uuid-peer-1",
      "ip": "192.168.1.10",
      "puerto": 22100,
      "conectado": "ONLINE",
      "ultimoLatido": "2025-11-06T10:30:00"
    }
  ]
}
```

### 3. **reportarLatido**
```json
Request:
{
  "action": "reportarLatido",
  "payload": {
    "peerId": "uuid-del-peer",
    "ip": "192.168.1.10",
    "puerto": 22100
  }
}

Response:
{
  "action": "reportarLatido",
  "status": "success",
  "message": "Latido recibido",
  "data": {
    "proximoLatidoMs": 30000
  }
}
```

### 4. **retransmitirPeticion**
```json
Request:
{
  "action": "retransmitirPeticion",
  "payload": {
    "peerDestinoId": "uuid-peer-destino",
    "peticionOriginal": {
      "action": "enviarMensaje",
      "payload": { ... }
    }
  }
}

Response:
{
  "action": "retransmitirPeticion",
  "status": "success",
  "message": "Petición retransmitida exitosamente",
  "data": {
    "respuestaPeer": { ... }
  }
}
```

---

## 📦 ESTRUCTURA DE MÓDULOS NUEVOS

```
Server-Nicolas/
├── negocio/
│   └── server-LogicaPeers/          ← NUEVO MÓDULO
│       ├── pom.xml
│       └── src/main/java/com/arquitectura/logicaPeers/
│           ├── IPeerService.java
│           ├── PeerServiceImpl.java
│           └── HeartbeatService.java
│
├── comunes/
│   ├── Server-DTO/
│   │   └── src/main/java/com/arquitectura/DTO/
│   │       └── p2p/                 ← NUEVO PAQUETE
│   │           ├── AddPeerRequestDto.java
│   │           ├── PeerResponseDto.java
│   │           ├── ReportHeartbeatRequestDto.java
│   │           └── RetransmitRequestDto.java
│   │
│   └── server-Utils/
│       └── src/main/java/com/arquitectura/utils/
│           └── p2p/                 ← NUEVO PAQUETE
│               ├── PeerClient.java
│               └── PeerConnectionPool.java
│
└── transporte/
    └── server-controladorTransporte/
        └── src/main/java/com/arquitectura/controlador/controllers/
            └── PeerController.java  ← NUEVO CONTROLADOR
```

---

## ⏱️ ESTIMACIÓN DE TIEMPO TOTAL

| Fase | Descripción | Tiempo Estimado |
|------|-------------|-----------------|
| 1 | Actualizar Modelo de Dominio | 30 min |
| 2 | Crear DTOs P2P | 20 min |
| 3 | Crear Servicio de Peers | 1 hora |
| 4 | Crear Cliente P2P | 1 hora |
| 5 | Crear Controlador P2P | 45 min |
| 6 | Sistema de Heartbeat | 30 min |
| 7 | Integración con Fachada | 20 min |
| 8 | Configuración | 15 min |
| 9 | Testing | 1 hora |
| **TOTAL** | | **~5-6 horas** |

---

## 🚀 ORDEN DE IMPLEMENTACIÓN RECOMENDADO

1. ✅ **Primero:** Actualizar entidad Peer y repositorio (Base de datos)
2. ✅ **Segundo:** Crear DTOs (Contratos de comunicación)
3. ✅ **Tercero:** Crear servicio de gestión de peers (Lógica de negocio)
4. ✅ **Cuarto:** Crear cliente P2P (Comunicación servidor-servidor)
5. ✅ **Quinto:** Crear controlador P2P (Endpoints)
6. ✅ **Sexto:** Implementar heartbeat automático
7. ✅ **Séptimo:** Integrar con fachada
8. ✅ **Octavo:** Configuración y testing

---

## 💡 CONSIDERACIONES IMPORTANTES

### Seguridad
- Implementar autenticación entre peers
- Validar origen de las peticiones
- Encriptar comunicación entre servidores

### Escalabilidad
- Pool de conexiones para múltiples peers
- Caché de peers conocidos
- Balanceo de carga en retransmisiones

### Resiliencia
- Timeout configurable para heartbeats
- Reconexión automática
- Manejo de peers caídos

### Sincronización
- Estrategia de resolución de conflictos
- Versionado de datos
- Logs de sincronización

---

## 📝 NOTAS ADICIONALES

1. **El cliente NO necesita cambios significativos** - Solo se conecta a su servidor local
2. **Cada servidor actúa como peer** - Mantiene su propia lista de peers
3. **Comunicación asíncrona** - Los heartbeats y retransmisiones no bloquean
4. **Base de datos local** - Cada peer mantiene su propia BD con réplica parcial

---

**¿Comenzamos con la Fase 1?** 🚀
