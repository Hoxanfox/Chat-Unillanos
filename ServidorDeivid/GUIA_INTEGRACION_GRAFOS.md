```

**3. Verificar que la red está iniciada:**
```java
if (controlador.isRedIniciada()) {
    // OK, debería estar sincronizando
}
```

**4. Ver logs para debugging:**
```java
LoggerCentral.setLevel(Level.DEBUG); // Ver todos los mensajes
```

---

## 📈 Personalización del Panel

### Cambiar colores:
```java
private static final Color COLOR_PEER_ONLINE = new Color(46, 204, 113);
private static final Color COLOR_CLIENTE = new Color(52, 152, 219);
```

### Cambiar intervalo de actualización:
En `ServicioTopologiaRed.java`:
```java
private static final long INTERVALO_SINCRONIZACION_MS = 3000; // 3 segundos
```

### Agregar información adicional:
Modifica el método `dibujarPeer()` o `dibujarClientesDePeer()` para mostrar más datos.

---

## ✅ Resumen

**Todo está conectado y funcionando:**

| Capa | Componente | Función |
|------|-----------|---------|
| **Vista** | `PanelTopologiaRed` | Dibuja grafos, implementa IObservador |
| **Controlador** | `ControladorP2P` | Expone métodos para la vista |
| **Servicio** | `ServicioP2P` | Orquesta los gestores |
| **Gestor** | `ServicioTopologiaRed` | Sincroniza vía P2P |
| **Datos** | `DTOTopologiaRed` | Transporta información |

**Actualización automática:**
- ⏱️ Cada 5 segundos (configurable)
- 🔔 Notificaciones push vía Observer
- 🎨 Redibujado automático en Swing

**¡El sistema está listo para usarse!** 🚀
# 🎨 Guía de Integración: Vista de Topología con Grafos

## ✅ Sistema Completamente Integrado

La arquitectura completa está funcionando siguiendo el patrón **Vista → Controlador → Servicio → Gestor → Repositorio**

---

## 📊 Flujo de Datos Completo

```
┌─────────────────────────────────────────────────────────────────┐
│                         VISTA (UI)                              │
│  PanelTopologiaRed.java - Muestra grafos en tiempo real        │
└──────────────────────────┬──────────────────────────────────────┘
                           │ (implementa IObservador)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      CONTROLADOR                                │
│  ControladorP2P.java - Coordina la lógica de presentación      │
│  • obtenerTopologiaCompleta()                                   │
│  • suscribirseATopologia(observador)                           │
│  • forzarActualizacionTopologia()                              │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                       SERVICIO                                  │
│  ServicioP2P.java - Lógica de negocio                         │
│  • obtenerTopologiaCompleta()                                   │
│  • registrarObservadorTopologia()                              │
│  • forzarActualizacionTopologia()                              │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                        GESTOR                                   │
│  ServicioTopologiaRed.java - Sincronización P2P                │
│  • Envía topología cada 5 segundos (broadcast)                 │
│  • Recibe topologías de peers remotos                          │
│  • Notifica cambios a observadores                             │
│  • Usa DTOs para transportar datos                             │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                     REPOSITORIO                                 │
│  • ServicioCliente.getSesionesActivas()                        │
│  • GestorConexiones.obtenerDetallesPeers()                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Paso 1: Integrar el Panel en tu Aplicación

### Opción A: Ventana Independiente

```java
import interfazGrafica.paneles.PanelTopologiaRed;
import controlador.p2p.ControladorP2P;

public class VentanaTopologia extends JFrame {
    
    public VentanaTopologia(ControladorP2P controlador) {
        setTitle("Topología de Red P2P - Tiempo Real");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Agregar el panel de topología
        PanelTopologiaRed panelTopologia = new PanelTopologiaRed(controlador);
        add(panelTopologia);
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
```

### Opción B: Integrar en una Ventana Existente con Tabs

```java
import javax.swing.*;
import interfazGrafica.paneles.PanelTopologiaRed;

public class VentanaPrincipal extends JFrame {
    
    private ControladorP2P controladorP2P;
    private PanelTopologiaRed panelTopologia;
    
    public VentanaPrincipal() {
        controladorP2P = new ControladorP2P();
        
        setTitle("Sistema P2P");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Crear panel con pestañas
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Pestaña 1: Topología en tiempo real
        panelTopologia = new PanelTopologiaRed(controladorP2P);
        tabbedPane.addTab("🌐 Topología", panelTopologia);
        
        // Pestaña 2: Lista de peers (tu vista existente)
        tabbedPane.addTab("📋 Peers", crearPanelPeers());
        
        // Pestaña 3: Chat (tu vista existente)
        tabbedPane.addTab("💬 Chat", crearPanelChat());
        
        add(tabbedPane);
        setLocationRelativeTo(null);
    }
    
    private JPanel crearPanelPeers() {
        // Tu panel existente de peers
        return new JPanel();
    }
    
    private JPanel crearPanelChat() {
        // Tu panel existente de chat
        return new JPanel();
    }
}
```

---

## 🔧 Paso 2: Inicializar y Conectar los Servicios

En tu `Main.java` o clase de inicialización:

```java
public class Main {
    public static void main(String[] args) {
        // 1. Crear servicios
        ServicioP2P servicioP2P = new ServicioP2P();
        ServicioCliente servicioCS = new ServicioCliente();
        
        // 2. ✅ IMPORTANTE: Conectar los servicios
        servicioP2P.setServicioCliente(servicioCS);
        servicioCS.setServicioSincronizacionP2P(servicioP2P.getServicioSincronizacion());
        
        // 3. Crear controladores
        ControladorP2P controladorP2P = new ControladorP2P(servicioP2P);
        ControladorClienteServidor controladorCS = new ControladorClienteServidor(servicioCS);
        
        // 4. Iniciar servicios
        controladorP2P.iniciarRed();
        controladorCS.iniciarServidor(8000);
        
        // 5. Crear y mostrar interfaz
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal(controladorP2P);
            ventana.setVisible(true);
        });
    }
}
```

---

## 📡 Paso 3: Actualización Automática con Observadores

El panel se actualiza **automáticamente** mediante el patrón Observer:

### Eventos que notifica el sistema:

1. **TOPOLOGIA_ACTUALIZADA** (cada 5 segundos)
   - Datos: `Map<String, DTOTopologiaRed>`
   - Cuándo: El timer del ServicioTopologiaRed envía actualizaciones periódicas

2. **TOPOLOGIA_REMOTA_RECIBIDA** (cuando llegan datos de peers)
   - Datos: `Map<String, DTOTopologiaRed>`
   - Cuándo: Se recibe topología de un peer remoto vía P2P

3. **PEER_DESCONECTADO** (cuando se desconecta un peer)
   - Datos: `String` (ID del peer)
   - Cuándo: Se detecta desconexión de un peer

### Implementación en el Panel:

```java
@Override
public void actualizar(String tipoDeDato, Object datos) {
    switch (tipoDeDato) {
        case "TOPOLOGIA_ACTUALIZADA":
        case "TOPOLOGIA_REMOTA_RECIBIDA":
            // Actualizar datos y repintar
            Map<String, DTOTopologiaRed> nuevaTopologia = 
                (Map<String, DTOTopologiaRed>) datos;
            topologiaActual = nuevaTopologia;
            SwingUtilities.invokeLater(this::repaint);
            break;
            
        case "PEER_DESCONECTADO":
            // Refrescar vista
            SwingUtilities.invokeLater(() -> {
                actualizarTopologia();
                repaint();
            });
            break;
    }
}
```

---

## 🎨 Características del Panel de Topología

### Visualización:

✅ **Peers P2P** (círculos grandes)
- Color verde: Peer online
- Color rojo: Peer offline
- Muestra: ID, IP:Puerto, número de clientes

✅ **Clientes CS** (círculos pequeños)
- Color azul: Cliente conectado
- Punto verde: Cliente autenticado
- Punto amarillo: Cliente solo conectado

✅ **Conexiones** (líneas)
- Líneas grises conectan peers con sus clientes

✅ **Actualización automática**
- Se redibuja cada 5 segundos automáticamente
- Botón manual de actualización disponible

✅ **Leyenda interactiva**
- Explica los colores y símbolos

---

## 🔄 Flujo de Sincronización Automática

```
Cada 5 segundos:

ServicioTopologiaRed (Peer A)
    ↓ construye topología local
    ↓ (peer info + clientes conectados)
    ↓
    ↓ broadcast P2P
    ↓──────────────────────→ ServicioTopologiaRed (Peer B)
                                  ↓ guarda en topologiasRemotas
                                  ↓ notifica observadores
                                  ↓
                            PanelTopologiaRed (Vista B)
                                  ↓ actualizar()
                                  ↓ repaint()
                                  ↓
                            🎨 GRAFO ACTUALIZADO
```

---

## 📊 Datos Disponibles en Cada Nodo

### Para cada Peer:
```java
DTOTopologiaRed {
    String idPeer;           // UUID del peer
    String ipPeer;           // IP desde configuracion.txt
    int puertoPeer;          // Puerto desde configuracion.txt
    String estadoPeer;       // "ONLINE" / "OFFLINE"
    long timestamp;          // Momento de última actualización
    int numeroClientes;      // Contador rápido
    List<DTOSesionCliente> clientesConectados;
}
```

### Para cada Cliente:
```java
DTOSesionCliente {
    String idSesion;         // ID de la conexión
    String idUsuario;        // UUID del usuario (si autenticado)
    String ip;               // IP del cliente
    int puerto;              // Puerto del cliente
    String estado;           // "CONECTADO" / "AUTENTICADO"
    String fechaConexion;    // Timestamp de conexión
}
```

---

## 🎯 Métodos Disponibles en el Controlador

```java
// Obtener topología completa
Map<String, DTOTopologiaRed> topologia = 
    controlador.obtenerTopologiaCompleta();

// Suscribirse a cambios (para observadores personalizados)
controlador.suscribirseATopologia(miObservador);

// Forzar actualización inmediata
controlador.forzarActualizacionTopologia();

// Obtener peers con clientes (método alternativo)
List<DTOPeerConClientes> peersConClientes = 
    controlador.obtenerPeersConClientes();
```

---

## 🚨 Troubleshooting

### Si no se actualizan los grafos:

**1. Verificar que los servicios están conectados:**
```java
servicioP2P.setServicioCliente(servicioCS); // ← IMPORTANTE
```

**2. Verificar que el observador está registrado:**
```java
controlador.suscribirseATopologia(panelTopologia);

