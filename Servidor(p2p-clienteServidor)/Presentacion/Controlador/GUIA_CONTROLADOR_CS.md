# 📘 Guía de Uso - Controlador Cliente-Servidor

## 🎯 Descripción
El `ControladorClienteServidor` es la interfaz simplificada para que la vista/interfaz gráfica pueda controlar el servidor de clientes.

**IMPORTANTE:** El controlador SOLO se comunica con la capa de **Servicio** (`IServicioClienteControl`), respetando las capas arquitectónicas y evitando acoplamiento con DTOs, Logger o clases de negocio.

---

## 🚀 Uso Básico

### 1️⃣ Crear el Controlador

```java
// Crear instancia del controlador
ControladorClienteServidor controlador = new ControladorClienteServidor();
```

### 2️⃣ Configurar Callbacks (Opcional)

```java
// Para recibir mensajes informativos
controlador.setOnMensajeInfo(mensaje -> {
    System.out.println("✅ INFO: " + mensaje);
    // Actualizar UI aquí
});

// Para recibir mensajes de error
controlador.setOnMensajeError(error -> {
    System.err.println("❌ ERROR: " + error);
    // Mostrar alerta en UI
});
```

### 3️⃣ Iniciar el Servidor

```java
// Opción A: Puerto automático (lee de configuracion.txt)
controlador.iniciarServidorAutomatico();

// Opción B: Puerto específico
controlador.iniciarServidor(8000);
```

### 4️⃣ Consultar Estado

```java
// ¿Está el servidor activo?
boolean activo = controlador.isServidorActivo();

// Obtener resumen del estado
String estado = controlador.getEstadoServidor();
System.out.println(estado); // "Estado: ACTIVO | Clientes: N/A"
```

### 5️⃣ Detener el Servidor

```java
controlador.detenerServidor();
```

---

## 📋 Ejemplo para Interfaz Gráfica (Swing)

```java
public class VentanaServidor extends JFrame {
    private ControladorClienteServidor controlador;
    private JLabel lblEstado;
    private JButton btnIniciar;
    private JButton btnDetener;
    private JTextArea txtLog;
    
    public VentanaServidor() {
        // 1. Crear controlador
        controlador = new ControladorClienteServidor();
        
        // 2. Configurar callbacks
        configurarEventos();
        
        // 3. Configurar botones
        btnIniciar.addActionListener(e -> iniciarServidor());
        btnDetener.addActionListener(e -> detenerServidor());
        
        // Inicializar UI
        actualizarEstado();
    }
    
    private void configurarEventos() {
        // Mensajes informativos al log
        controlador.setOnMensajeInfo(mensaje -> {
            SwingUtilities.invokeLater(() -> {
                txtLog.append("[INFO] " + mensaje + "\n");
                actualizarEstado();
            });
        });
        
        // Mensajes de error
        controlador.setOnMensajeError(error -> {
            SwingUtilities.invokeLater(() -> {
                txtLog.append("[ERROR] " + error + "\n");
                JOptionPane.showMessageDialog(this, 
                    error, 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
        });
    }
    
    private void iniciarServidor() {
        try {
            controlador.iniciarServidorAutomatico();
            lblEstado.setText("🟢 ACTIVO");
            lblEstado.setForeground(Color.GREEN);
            btnIniciar.setEnabled(false);
            btnDetener.setEnabled(true);
        } catch (Exception e) {
            // El error ya fue manejado por el callback
        }
    }
    
    private void detenerServidor() {
        controlador.detenerServidor();
        lblEstado.setText("🔴 DETENIDO");
        lblEstado.setForeground(Color.RED);
        btnIniciar.setEnabled(true);
        btnDetener.setEnabled(false);
    }
    
    private void actualizarEstado() {
        String estado = controlador.getEstadoServidor();
        lblEstado.setText(estado);
    }
}
```

---

## 🖥️ Ejemplo para Consola Simple

```java
public class ConsolaServidor {
    public static void main(String[] args) {
        ControladorClienteServidor controlador = new ControladorClienteServidor();
        Scanner scanner = new Scanner(System.in);
        
        // Configurar callbacks para logging
        controlador.setOnMensajeInfo(msg -> System.out.println("[INFO] " + msg));
        controlador.setOnMensajeError(err -> System.err.println("[ERROR] " + err));
        
        while (true) {
            System.out.println("\n=== SERVIDOR CLIENTE-SERVIDOR ===");
            System.out.println("1. Iniciar servidor");
            System.out.println("2. Detener servidor");
            System.out.println("3. Ver estado");
            System.out.println("4. Salir");
            System.out.print("Opción: ");
            
            int opcion = scanner.nextInt();
            
            switch (opcion) {
                case 1:
                    try {
                        controlador.iniciarServidorAutomatico();
                    } catch (Exception e) {
                        System.err.println("Error al iniciar: " + e.getMessage());
                    }
                    break;
                    
                case 2:
                    controlador.detenerServidor();
                    break;
                    
                case 3:
                    System.out.println("\n📊 ESTADO:");
                    System.out.println("  " + controlador.getEstadoServidor());
                    System.out.println("  Activo: " + 
                        (controlador.isServidorActivo() ? "SÍ" : "NO"));
                    break;
                    
                case 4:
                    if (controlador.isServidorActivo()) {
                        controlador.detenerServidor();
                    }
                    System.out.println("👋 Saliendo...");
                    System.exit(0);
                    break;
                    
                default:
                    System.out.println("❌ Opción inválida");
            }
        }
    }
}
```

---

## 🔧 Configuración (configuracion.txt)

El servidor lee automáticamente estas propiedades:

```properties
# Host y puerto para el servidor de clientes
cliente.host=192.168.1.9
cliente.puerto=8000

# Configuración del pool de clientes
pool.clientes.min=1
pool.clientes.max=10
```

---

## ✅ Arquitectura en Capas Respetada

```
┌─────────────────────────────────────┐
│  VISTA / INTERFAZ GRÁFICA           │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  CONTROLADOR                        │
│  ControladorClienteServidor         │
└─────────────────┬───────────────────┘
                  │ (solo interface)
┌─────────────────▼───────────────────┐
│  SERVICIO                           │
│  IServicioClienteControl            │
│  ServicioCliente                    │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  NEGOCIO                            │
│  FachadaClientes                    │
│  ServicioGestionRed                 │
│  Otros servicios...                 │
└─────────────────────────────────────┘
```

**Regla de Oro:** El Controlador NUNCA importa clases de la capa de Negocio (DTOs, servicios específicos, etc.). Solo se comunica con interfaces de la capa de Servicio.

---

## 📞 API Resumida

| Método | Descripción |
|--------|-------------|
| `iniciarServidorAutomatico()` | Inicia con configuración de archivo |
| `iniciarServidor(puerto)` | Inicia en puerto específico |
| `detenerServidor()` | Detiene el servidor |
| `isServidorActivo()` | Retorna estado del servidor |
| `getEstadoServidor()` | String con resumen del estado |
| `setOnMensajeInfo(callback)` | Callback para mensajes informativos |
| `setOnMensajeError(callback)` | Callback para errores |

---

## 🎯 Ventajas de Esta Arquitectura

1. **Desacoplamiento Total**: La vista no conoce la implementación interna
2. **Fácil Testing**: Se puede inyectar un servicio mock
3. **Mantenibilidad**: Cambios en negocio no afectan al controlador
4. **Simplicidad**: Solo 7 métodos públicos para controlar todo

---

**¡El controlador está listo y respeta las capas arquitectónicas!** 🎉
