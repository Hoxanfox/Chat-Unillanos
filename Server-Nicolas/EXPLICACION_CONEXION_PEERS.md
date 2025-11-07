# 📡 Explicación Completa: Conexión entre Servidores (Peers)

## 🎯 ¿Cómo se Conecta Este Servidor a los Peers?

### **Arquitectura General**

Este servidor tiene un sistema P2P (Peer-to-Peer) que permite conectarse a otros servidores. Hay **3 formas** de establecer conexiones:

---

## 1️⃣ **Conexiones Salientes (Outgoing)**
**El servidor busca y se conecta activamente a otros servidores**

### Flujo de Inicio:

```
ServerLauncher.main()
    │
    ├─→ 1. Inicia PeerConnectionManager
    │
    ├─→ 2. Espera 2 segundos
    │
    └─→ 3. Llama a connectToAllKnownPeers()
            │
            ├─→ A) Busca peers en la BASE DE DATOS
            │      └─→ Si encuentra peers → Conecta a cada uno
            │
            └─→ B) Si NO hay peers en BD → Llama a connectToBootstrapPeers()
                   └─→ Lee `peer.bootstrap.nodes` de server.properties
                       └─→ Intenta conectar a esas IPs
```

### Código Relevante:

**Archivo:** `transporte/server-Transporte/src/main/java/com/arquitectura/transporte/PeerConnectionManager.java`

```java
public void connectToAllKnownPeers() {
    List<Peer> peers = peerRepository.findAll();
    
    if (peers.isEmpty() || peers.size() == 1) {
        // ⚠️ NO HAY PEERS EN LA BD
        log.warn("⚠️ No hay peers registrados en la base de datos.");
        connectToBootstrapPeers(); // Intenta con bootstrap
        return;
    }
    
    // Conectar a cada peer de la BD
    for (Peer peer : peers) {
        if (!peer.getPeerId().equals(localPeerId)) {
            connectToPeer(peer.getPeerId(), peer.getIp(), peer.getPuerto());
        }
    }
}
```

---

## 2️⃣ **Conexiones Entrantes (Incoming)**
**Otros servidores se conectan a este servidor**

### Flujo:

```
ServerLauncher.main()
    │
    └─→ Inicia peerConnectionManager.startPeerServer()
            │
            └─→ Abre ServerSocket en puerto 22200
                │
                └─→ ESPERA conexiones entrantes (serverSocket.accept())
                    │
                    ├─→ Otro servidor conecta
                    │
                    ├─→ Se crea PeerHandler para manejarlo
                    │
                    ├─→ Espera handshake del peer remoto
                    │
                    └─→ Si handshake OK → Se registra en activePeerConnections
```

### Código Relevante:

```java
public void startPeerServer() {
    new Thread(() -> {
        try (ServerSocket serverSocket = new ServerSocket(peerPort)) {
            log.info("Servidor P2P iniciado en puerto {}", peerPort);
            
            while (running) {
                Socket peerSocket = serverSocket.accept(); // ESPERA AQUÍ
                
                // Crear handler para el peer que se conectó
                PeerHandler handler = new PeerHandler(peerSocket, gson, this, ...);
                peerPool.submit(handler);
            }
        }
    }).start();
}
```

**Este modo SIEMPRE está activo** - El servidor siempre puede recibir conexiones.

---

## 3️⃣ **Bootstrap Peers (Peers de Arranque)**
**Configuración manual para la primera vez**

### ¿Qué son?

Son IPs de servidores conocidos que se configuran en `config/server.properties`:

```properties
# Lista de servidores conocidos (separados por coma)
peer.bootstrap.nodes=192.168.1.10:22200,10.0.0.5:22200
```

### ¿Cuándo se usan?

- Cuando la base de datos está **VACÍA** (primer arranque)
- Cuando NO hay peers disponibles en la BD
- Para "sembrar" la red inicial

### Ventajas:

✅ No requiere base de datos previa  
✅ Ideal para red nueva  
✅ Permite descubrimiento inicial  

---

## 🚨 **¿Qué Pasa si NO HAY PEERS en la Red?**

### **Escenario 1: Base de datos vacía + Sin bootstrap**

```log
INFO - Conectando a 0 peers conocidos en BD...
WARN - ⚠️ No hay peers registrados en la base de datos.
INFO - Intentando conectar a peers de arranque (bootstrap)...
WARN - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
WARN - ⚠️  RED AISLADA - Este servidor NO tiene peers configurados
WARN - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
WARN - El servidor está en modo LISTENING en puerto 22200
WARN - Para conectar manualmente a otro peer:
WARN -   1. Asegúrate que el otro servidor esté corriendo
WARN -   2. Usa la API REST: POST /api/peers/register
WARN -   3. O configura 'peer.bootstrap.nodes' en server.properties
WARN - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**El servidor:**
- ✅ Inicia correctamente
- ✅ Puede recibir conexiones (puerto 22200)
- ❌ NO se conecta a nadie
- ❌ Está AISLADO esperando que alguien se conecte a él

---

## 🛠️ **Soluciones para Red Vacía**

### **Opción A: Configurar Bootstrap Peers**

Edita `config/server.properties`:

```properties
peer.bootstrap.nodes=192.168.1.10:22200,192.168.1.11:22200
```

Reinicia el servidor → Intentará conectarse automáticamente.

---

### **Opción B: Conexión Manual desde otro Servidor**

Si tienes **2 servidores** (A y B):

1. **Servidor A**: Levanta primero (queda aislado)
2. **Servidor B**: Configura en su `server.properties`:
   ```properties
   peer.bootstrap.nodes=<IP_DE_A>:22200
   ```
3. **Servidor B** se conectará automáticamente a **Servidor A**
4. Ambos se registran mutuamente en sus bases de datos
5. A partir de ahí, se reconectan automáticamente

---

### **Opción C: API REST (Registro Manual)**

Puedes agregar un endpoint REST para registrar peers manualmente:

```bash
curl -X POST http://localhost:22100/api/peers/register \
  -H "Content-Type: application/json" \
  -d '{
    "ip": "192.168.1.10",
    "puerto": 22200
  }'
```

Esto agregaría el peer a la BD y intentaría conectarse.

---

## 📊 **Estados de Conexión de un Peer**

| Estado | Descripción |
|--------|-------------|
| **CONNECTING** | Intentando conectar (temporal) |
| **ONLINE** | Conectado y funcionando |
| **OFFLINE** | Desconectado (sin heartbeat > 60 seg) |

---

## 🔄 **Reconexión Automática**

El sistema tiene **tareas de mantenimiento** que se ejecutan periódicamente:

### 1. **Verificar Heartbeats** (cada 30 segundos)
- Revisa si los peers responden
- Si un peer no responde por > 60 segundos → Lo desconecta

### 2. **Intentar Reconexiones** (cada 10 segundos)
- Busca peers en BD con estado OFFLINE
- Intenta reconectarse a ellos
- Máximo 3 intentos con delay de 5 segundos

### 3. **Sincronizar con Base de Datos** (cada 60 segundos)
- Actualiza el estado ONLINE/OFFLINE de cada peer
- Registra el último heartbeat

---

## 📁 **Archivos Clave**

### Configuración
- `config/server.properties` - Configuración de puertos y bootstrap peers

### Transporte (Conexiones)
- `transporte/server-Transporte/src/.../PeerConnectionManager.java` - Gestor principal
- `transporte/server-Transporte/src/.../PeerHandler.java` - Maneja conexiones entrantes
- `transporte/server-Transporte/src/.../PeerOutgoingConnection.java` - Maneja conexiones salientes

### Negocio (Lógica)
- `negocio/server-logicaUsuarios/src/.../PeerServiceImpl.java` - Lógica de negocio de peers

### Datos (Persistencia)
- `datos/server-dominio/src/.../Peer.java` - Entidad del dominio
- `datos/server-persistencia/src/.../PeerRepository.java` - Acceso a BD

---

## 🎬 **Ejemplo Práctico: Levantar 2 Servidores**

### Servidor A (Puerto P2P: 22200)

```properties
# config/server.properties
server.port=22100
peer.server.port=22200
peer.bootstrap.nodes=
```

Se levanta → Queda en modo LISTENING (esperando conexiones)

---

### Servidor B (Puerto P2P: 22201)

```properties
# config/server.properties
server.port=22101
peer.server.port=22201
peer.bootstrap.nodes=localhost:22200
```

Se levanta → Se conecta automáticamente al Servidor A

---

### Resultado:

```
Servidor A recibe conexión de B
    │
    ├─→ A registra a B en su BD
    └─→ B registra a A en su BD
        │
        └─→ Ambos quedan conectados permanentemente
            │
            └─→ Si uno cae, el otro intenta reconectar automáticamente
```

---

## ✅ **Resumen**

| Situación | Qué Hace el Servidor |
|-----------|---------------------|
| **BD con peers** | ✓ Conecta automáticamente a todos |
| **BD vacía + bootstrap configurado** | ✓ Conecta a bootstrap peers |
| **BD vacía + sin bootstrap** | ⚠️ Modo LISTENING (aislado) |
| **Peer se cae** | ⚠️ Lo detecta y reintenta conectar |
| **Otro peer conecta** | ✓ Lo acepta y registra automáticamente |

---

## 🔧 **Para Debugging**

Busca estos logs al iniciar:

```log
INFO - PeerConnectionManager inicializado. Local Peer ID: xxxxxxxx
INFO - Puerto P2P: 22200, Max conexiones: 50
INFO - Servidor P2P iniciado en puerto 22200
INFO - Conectando a X peers conocidos en BD...
```

Si ves:
- `Conectando a 0 peers` → BD vacía
- `RED AISLADA` → Sin bootstrap configurado
- `Intentando conectar a X bootstrap peers` → Usando bootstrap

---

**¿Tienes más dudas sobre alguna parte específica?**

