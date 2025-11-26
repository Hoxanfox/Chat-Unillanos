# Guía: Obtener Peers con sus Clientes Conectados

## 📋 Resumen

**SÍ es posible** obtener información de los peers y sus clientes conectados desde los servicios. He implementado la infraestructura necesaria para que puedas consultar esta información desde la interfaz.

## 🎯 ¿Qué se ha implementado?

### 1. Nuevo DTO: `DTOPeerConClientes`
- **Ubicación**: `Infraestructura/DTO/src/main/java/dto/p2p/DTOPeerConClientes.java`
- **Propósito**: Combina información de un peer con la lista de sus clientes conectados

```java
public class DTOPeerConClientes {
    private DTOPeerDetails peer;              // Información del peer
    private List<DTOSesionCliente> clientesConectados;  // Clientes conectados a ese peer
    private int numeroClientes;               // Contador rápido
}
```

### 2. Nuevo método en `IServicioP2PControl`
```java
List<DTOPeerConClientes> obtenerPeersConClientes();
```

### 3. Implementación en `ServicioP2P`
- Método `obtenerPeersConClientes()` que combina datos de peers y clientes
- Método `setServicioCliente()` para inyectar el servicio de clientes

## 🔧 ¿Cómo funciona actualmente?

### Limitación Actual: Solo Clientes Locales
Por ahora, el sistema **solo puede obtener los clientes del servidor local**, no de peers remotos. Esto es porque:

1. **Servidor Local**: Conoce sus propios clientes conectados (a través de `ServicioCliente.getSesionesActivas()`)
2. **Peers Remotos**: NO comparten automáticamente su lista de clientes

### Para obtener clientes de peers remotos necesitarías:
- Implementar un **protocolo P2P adicional** que permita consultar clientes remotos
- Agregar un mensaje tipo `REQUEST_CLIENT_LIST` / `RESPONSE_CLIENT_LIST`
- Manejar privacidad y seguridad (¿todos los peers deben ver todos los clientes?)

## 📝 Ejemplo de Uso

### Opción 1: Desde el Controlador P2P

```java
// 1. Inyectar el servicio de clientes al servicio P2P
ServicioP2P servicioP2P = new ServicioP2P();
ServicioCliente servicioCliente = new ServicioCliente();

// Conectar los servicios
servicioP2P.setServicioCliente(servicioCliente);

// 2. Obtener peers con clientes
List<DTOPeerConClientes> peersConClientes = servicioP2P.obtenerPeersConClientes();

// 3. Usar la información
for (DTOPeerConClientes peerInfo : peersConClientes) {
    DTOPeerDetails peer = peerInfo.getPeer();
    List<DTOSesionCliente> clientes = peerInfo.getClientesConectados();
    
    System.out.println("Peer: " + peer.getId() + " - " + peer.getIp());
    System.out.println("Clientes conectados: " + peerInfo.getNumeroClientes());
    
    for (DTOSesionCliente cliente : clientes) {
        System.out.println("  - Cliente: " + cliente.getIdSesion() + 
                         " (" + cliente.getEstado() + ")");
    }
}
```

### Opción 2: Desde la Interfaz/Consola

Puedes agregar un nuevo comando en `ControladorConsola`:

```java
case "TOPOLOGY":
case "TOPOLOGIA":
    List<DTOPeerConClientes> topologia = controladorPuro.obtenerPeersConClientes();
    
    System.out.println("\n=== TOPOLOGÍA DE LA RED ===");
    for (DTOPeerConClientes peerInfo : topologia) {
        DTOPeerDetails peer = peerInfo.getPeer();
        boolean esLocal = peerInfo.esLocal();
        
        System.out.printf("%s [%s] - %s:%d (%d clientes)\n",
            esLocal ? "🏠 LOCAL" : "🌐 REMOTO",
            peer.getId(),
            peer.getIp(),
            peer.getPuerto(),
            peerInfo.getNumeroClientes()
        );
        
        // Mostrar clientes solo si es el servidor local
        if (esLocal) {
            for (DTOSesionCliente cliente : peerInfo.getClientesConectados()) {
                System.out.printf("    └─ Cliente: %s [%s]\n",
                    cliente.getIdSesion(),
                    cliente.getEstado()
                );
            }
        } else {
            System.out.println("    └─ (Clientes remotos no disponibles)");
        }
    }
    break;
```

## 🔗 Conectar los Servicios

**IMPORTANTE**: Para que funcione correctamente, debes inyectar el servicio de clientes en el servicio P2P.

### Dónde hacer la inyección:

En el lugar donde inicializas ambos servicios (probablemente en `ControladorConsola` o un orquestador):

```java
public class ControladorConsola {
    private final ControladorP2P controladorP2P;
    private final ControladorClienteServidor controladorCS;
    
    public ControladorConsola() {
        // Crear servicios
        ServicioP2P servicioP2P = new ServicioP2P();
        ServicioCliente servicioCS = new ServicioCliente();
        
        // ✅ CONECTAR LOS SERVICIOS
        servicioP2P.setServicioCliente(servicioCS);
        
        // También conectar P2P con CS para sincronización
        servicioCS.setServicioSincronizacionP2P(servicioP2P.getServicioSincronizacion());
        
        // Crear controladores
        this.controladorP2P = new ControladorP2P(servicioP2P);
        this.controladorCS = new ControladorClienteServidor(servicioCS);
    }
}
```

## 🎨 Métodos Disponibles en DTOPeerConClientes

```java
// Información del peer
DTOPeerDetails getPeer()
String getIdPeer()
String getIpPeer()
int getPuertoPeer()
String getEstadoPeer()

// Información de clientes
List<DTOSesionCliente> getClientesConectados()
int getNumeroClientes()

// Utilidades
boolean esLocal()  // Indica si es el servidor local
String toString()  // Descripción legible
```

## 📊 Estructura de Datos Retornada

```
List<DTOPeerConClientes>
  ├─ DTOPeerConClientes (Peer 1 - LOCAL)
  │   ├─ peer: DTOPeerDetails
  │   │   ├─ id: "LOCAL"
  │   │   ├─ ip: "192.168.1.100"
  │   │   └─ puerto: 9000
  │   └─ clientesConectados: List<DTOSesionCliente>
  │       ├─ Cliente 1: {idSesion, ip, puerto, estado}
  │       └─ Cliente 2: {idSesion, ip, puerto, estado}
  │
  └─ DTOPeerConClientes (Peer 2 - REMOTO)
      ├─ peer: DTOPeerDetails
      │   ├─ id: "peer-uuid-123"
      │   ├─ ip: "192.168.1.101"
      │   └─ puerto: 9001
      └─ clientesConectados: [] (vacío - no disponible remotamente)
```

## 🚀 Próximos Pasos (Opcional)

Si necesitas obtener clientes de peers remotos, deberías:

1. **Crear nuevos mensajes P2P**:
   - `MensajeSolicitarClientes` (REQUEST_CLIENT_LIST)
   - `MensajeRespuestaClientes` (RESPONSE_CLIENT_LIST)

2. **Agregar handler en el protocolo**:
   ```java
   case REQUEST_CLIENT_LIST:
       List<DTOSesionCliente> clientes = servicioCliente.getSesionesActivas();
       enviarRespuesta(new MensajeRespuestaClientes(clientes));
       break;
   ```

3. **Actualizar DTOPeerConClientes** con datos remotos cuando llegue la respuesta

## ✅ Resumen

- ✅ **Sí es posible** obtener peers con sus clientes
- ✅ Funciona para el **servidor local** (inmediato)
- ⚠️ Para peers **remotos** requiere implementar protocolo adicional
- ✅ Los DTOs y métodos ya están implementados
- ✅ Solo falta conectar los servicios e invocar el método

## 📞 Uso desde la Interfaz

Para usar desde tu interfaz gráfica, simplemente llama:

```java
List<DTOPeerConClientes> topologia = controlador.obtenerPeersConClientes();
```

Y tendrás toda la información para mostrar una vista de topología de red con peers y sus clientes conectados.

