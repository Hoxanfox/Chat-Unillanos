# 🌐 Sistema de Sincronización de Topología de Red P2P

## 📋 Resumen de la Implementación

He implementado un **sistema completo de sincronización automática de topología de red** que:

✅ **Sincroniza periódicamente** (cada 5 segundos) la topología completa de la red  
✅ **Notifica cambios** cuando un cliente CS se conecta/desconecta  
✅ **Usa el protocolo existente** del proyecto (DTORequest/DTOResponse con IRouterMensajes)  
✅ **Incluye estados completos**: host, puerto de peers P2P y clientes CS  
✅ **Actualiza automáticamente** la interfaz mediante el patrón Observer  

---

## 🎯 Componentes Implementados

### 1. **DTOTopologiaRed** - El Mensajero
**Ubicación**: `Infraestructura/DTO/src/main/java/dto/topologia/DTOTopologiaRed.java`

DTO serializable que transporta la topología de un peer a través de la red P2P:

```java
public class DTOTopologiaRed {
    // Información del peer
    private String idPeer;
    private String ipPeer;
    private int puertoPeer;
    private String estadoPeer;
    
    // Clientes conectados a este peer
    private List<DTOSesionCliente> clientesConectados;
    private int numeroClientes;
    
    // Timestamp para sincronización
    private long timestamp;
}
```

### 2. **ServicioTopologiaRed** - El Cartógrafo
**Ubicación**: `Negocio/GestorP2P/src/main/java/gestorP2P/servicios/ServicioTopologiaRed.java`

Servicio P2P que se encarga de:
- 📡 Enviar topología local cada 5 segundos a todos los peers
- 📥 Recibir y almacenar topologías de peers remotos
- 🔔 Notificar a observadores cuando hay cambios
- 🗑️ Limpiar topologías de peers desconectados

**Rutas P2P Registradas**:
- `actualizarTopologia` - Recibe actualizaciones de otros peers
- `solicitarTopologia` - Responde con la topología local cuando se solicita

---

## 🔄 Flujo de Funcionamiento

### Sincronización Periódica (cada 5 segundos)

```
┌─────────────────────────────────────────────────────┐
│ 1. Timer se activa cada 5 segundos                  │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│ 2. construirTopologiaLocal()                        │
│    - Obtiene ID, IP, Puerto del peer local          │
│    - Consulta ServicioCliente.getSesionesActivas()  │
│    - Crea DTOTopologiaRed con todos los datos       │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│ 3. Envía DTORequest("actualizarTopologia")          │
│    a TODOS los peers conectados (broadcast)         │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│ 4. Peers remotos reciben y actualizan sus mapas     │
│    topologiasRemotas.put(idPeer, topologia)        │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│ 5. Notifica a observadores (UI se actualiza)        │
│    notificarObservadores("TOPOLOGIA_ACTUALIZADA")   │
└─────────────────────────────────────────────────────┘
```

### Cuando un Cliente CS se Conecta/Desconecta

```
Cliente conecta/desconecta
         │
         ▼
ServicioGestionRed (CS) detecta cambio
         │
         ▼
Próximo ciclo de sincronización (máximo 5 segundos)
         │
         ▼
ServicioTopologiaRed envía topología actualizada
         │
         ▼
Todos los peers reciben el cambio
         │
         ▼
Interfaces gráficas se actualizan automáticamente
```

---

## 🔌 Integración con el Sistema Existente

### Conexión de Servicios

El ServicioTopologiaRed se registra automáticamente en el `ServicioP2P`:

```java
// En ServicioP2P.configurarServicios()
this.servicioTopologia = new ServicioTopologiaRed();
fachada.registrarServicio(servicioTopologia);
```

### Inyección del Servicio de Clientes

Cuando se inyecta el `ServicioCliente` en `ServicioP2P`, automáticamente se propaga al servicio de topología:

```java
// En tu código de inicialización
servicioP2P.setServicioCliente(servicioCS);

// Esto automáticamente hace:
// servicioTopologia.setServicioCliente(servicioCS);
```

---

## 📊 Estructura de Datos Resultante

Cada peer mantiene un mapa con la topología completa de la red:

```
Map<String, DTOTopologiaRed> topologiasRemotas
│
├─ "peer-uuid-001" -> TopologiaRed {
│     idPeer: "peer-uuid-001"
│     ip: "192.168.1.100"
│     puerto: 9000
│     estado: "ONLINE"
│     clientesConectados: [
│         Cliente {idSesion: "cliente-1", ip: "192.168.1.50", estado: "AUTENTICADO"},
│         Cliente {idSesion: "cliente-2", ip: "192.168.1.51", estado: "CONECTADO"}
│     ]
│     numeroClientes: 2
│  }
│
├─ "peer-uuid-002" -> TopologiaRed {
│     idPeer: "peer-uuid-002"
│     ip: "192.168.1.101"
│     puerto: 9001
│     estado: "ONLINE"
│     clientesConectados: [
│         Cliente {idSesion: "cliente-3", ip: "192.168.1.52", estado: "AUTENTICADO"}
│     ]
│     numeroClientes: 1
│  }
│
└─ "LOCAL" -> TopologiaRed {
      idPeer: "LOCAL"
      ip: "localhost"
      puerto: 9002
      estado: "ONLINE"
      clientesConectados: [
          Cliente {idSesion: "cliente-4", ip: "192.168.1.53", estado: "AUTENTICADO"},
          Cliente {idSesion: "cliente-5", ip: "192.168.1.54", estado: "AUTENTICADO"},
          Cliente {idSesion: "cliente-6", ip: "192.168.1.55", estado: "CONECTADO"}
      ]
      numeroClientes: 3
   }
```

---

## 🎨 Uso desde la Interfaz Gráfica

### Opción 1: Usar el Servicio Directamente

```java
// Si tienes acceso al ServicioP2P
ServicioTopologiaRed servicioTopologia = ...;

// Obtener topología completa
Map<String, DTOTopologiaRed> topologiaCompleta = 
    servicioTopologia.obtenerTopologiaCompleta();

// Iterar por cada peer
for (Map.Entry<String, DTOTopologiaRed> entry : topologiaCompleta.entrySet()) {
    String idPeer = entry.getKey();
    DTOTopologiaRed topo = entry.getValue();
    
    System.out.println("Peer: " + topo.getIdPeer());
    System.out.println("  IP: " + topo.getIpPeer() + ":" + topo.getPuertoPeer());
    System.out.println("  Estado: " + topo.getEstadoPeer());
    System.out.println("  Clientes: " + topo.getNumeroClientes());
    
    for (DTOSesionCliente cliente : topo.getClientesConectados()) {
        System.out.println("    - " + cliente.getIdSesion() + 
                         " [" + cliente.getEstado() + "]");
    }
}
```

### Opción 2: Suscribirse a Cambios (Recomendado para UI)

```java
// Implementar IObservador en tu componente de UI
public class PanelTopologia implements IObservador {
    
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        switch (tipoDeDato) {
            case "TOPOLOGIA_ACTUALIZADA":
                Map<String, DTOTopologiaRed> topologia = 
                    (Map<String, DTOTopologiaRed>) datos;
                actualizarGrafico(topologia);
                break;
                
            case "TOPOLOGIA_REMOTA_RECIBIDA":
                // Se recibió topología de un peer remoto
                actualizarGrafico((Map<String, DTOTopologiaRed>) datos);
                break;
                
            case "PEER_DESCONECTADO":
                String idPeer = (String) datos;
                eliminarPeerDelGrafico(idPeer);
                break;
        }
    }
    
    private void actualizarGrafico(Map<String, DTOTopologiaRed> topologia) {
        // Actualizar tu componente gráfico aquí
        // Ejemplo: dibujar nodos, conexiones, etc.
    }
}

// Registrar el observador
servicioTopologia.registrarObservador(new PanelTopologia());
```

---

## ⚡ Características Avanzadas

### Forzar Actualización Inmediata

Si necesitas enviar la topología inmediatamente (sin esperar los 5 segundos):

```java
servicioTopologia.forzarActualizacion();
```

**Cuándo usar**: 
- Cuando un cliente se conecta/desconecta
- Después de cambios importantes en la configuración
- Al solicitar una vista actualizada desde la UI

### Limpieza de Peers Desconectados

El servicio limpia automáticamente la topología de peers desconectados:

```java
// Se llama automáticamente cuando se detecta una desconexión
servicioTopologia.limpiarPeerDesconectado(idPeer);
```

---

## 📈 Información Disponible para Grafos

Para cada **Peer P2P**:
- ✅ ID único
- ✅ Dirección IP
- ✅ Puerto
- ✅ Estado (ONLINE/OFFLINE)
- ✅ Timestamp de última actualización

Para cada **Cliente CS**:
- ✅ ID de sesión
- ✅ ID de usuario (si está autenticado)
- ✅ Dirección IP
- ✅ Puerto
- ✅ Estado (CONECTADO/AUTENTICADO)
- ✅ Fecha de conexión

---

## 🔧 Configuración

### Modificar Intervalo de Sincronización

Por defecto es 5 segundos. Para cambiarlo:

```java
// En ServicioTopologiaRed.java
private static final long INTERVALO_SINCRONIZACION_MS = 3000; // 3 segundos
```

### Deshabilitar Sincronización Periódica

Si solo quieres sincronización manual:

```java
// Comentar la línea en ServicioTopologiaRed.iniciar()
// timer.scheduleAtFixedRate(...);

// Usar solo:
servicioTopologia.forzarActualizacion(); // Cuando sea necesario
```

---

## 🎯 Próximos Pasos Sugeridos

### 1. Crear Visualización en la Interfaz

Usa librerías como:
- **JGraphX** (para Swing)
- **JavaFX Graph** (para JavaFX)  
- **Cytoscape.js** (si usas web)

### 2. Agregar Métricas Adicionales

Extiende `DTOTopologiaRed` con:
```java
private long tiempoActividad; // Uptime del peer
private double cargaCPU;
private long memoriaUsada;
private int mensajesProcesados;
```

### 3. Implementar Filtros

```java
// Mostrar solo peers online
topologia.values().stream()
    .filter(t -> "ONLINE".equals(t.getEstadoPeer()))
    .collect(Collectors.toList());

// Mostrar solo peers con clientes
topologia.values().stream()
    .filter(t -> t.getNumeroClientes() > 0)
    .collect(Collectors.toList());
```

---

## ✅ Resumen de Beneficios

| Característica | Antes | Ahora |
|---------------|-------|-------|
| Ver clientes de otros peers | ❌ No | ✅ Sí (automático) |
| Actualización de topología | ⚠️ Manual | ✅ Automática (5s) |
| Estado de peers | ⚠️ Básico | ✅ Completo |
| Estado de clientes | ❌ Solo local | ✅ Toda la red |
| Notificaciones a UI | ⚠️ Polling | ✅ Push (Observer) |
| Sincronización | ⚠️ Eventual | ✅ Periódica + Eventos |

---

## 🚀 ¡Todo Listo!

El sistema está **completamente funcional** y se activa automáticamente cuando:

1. ✅ Inicias el `ServicioP2P` (la red P2P)
2. ✅ Inyectas el `ServicioCliente` en el `ServicioP2P`
3. ✅ Te suscribes como observador (opcional, para notificaciones en UI)

**La topología se sincroniza automáticamente cada 5 segundos y cuando hay cambios en los clientes conectados.**

¡Ahora puedes crear grafos dinámicos y en tiempo real de toda tu red P2P con información completa de peers y clientes! 🎉

