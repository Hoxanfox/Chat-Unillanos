            switch (tipoDeDato) {
                case "CANALES_ACTUALIZADOS":
                    manejarCanalesActualizados((List<Canal>) datos);
                    break;

                case "MENSAJE_CANAL_RECIBIDO":
                    manejarNuevoMensaje((DTOMensajeCanal) datos);
                    break;

                case "HISTORIAL_CANAL_RECIBIDO":
                    manejarHistorialRecibido((List<DTOMensajeCanal>) datos);
                    break;

                case "NUEVA_INVITACION_CANAL":
                    manejarNuevaInvitacion((Map<String, String>) datos);
                    break;

                case "NUEVO_MIEMBRO_EN_CANAL":
                    manejarNuevoMiembro((Map<String, String>) datos);
                    break;

                case "ERROR_OPERACION":
                    manejarError((String) datos);
                    break;

                case "MENSAJE_ENVIADO_EXITOSO":
                    manejarMensajeEnviadoExitoso();
                    break;

                default:
                    System.out.println("Notificación no manejada: " + tipoDeDato);
            }
        });
    }

    // === MANEJADORES DE NOTIFICACIONES ===

    private void manejarCanalesActualizados(List<Canal> canales) {
        System.out.println("✓ UI: Actualizando lista de canales (" + canales.size() + " canales)");
        listViewCanales.getItems().clear();
        listViewCanales.getItems().addAll(canales);
    }

    private void manejarNuevoMensaje(DTOMensajeCanal mensaje) {
        // Solo agregar si el mensaje es del canal actualmente seleccionado
        if (canalSeleccionado != null && 
            mensaje.getCanalId().equals(canalSeleccionado.getIdCanal().toString())) {
            
            System.out.println("✓ UI: Nuevo mensaje recibido en canal actual");
            listViewMensajes.getItems().add(mensaje);
            
            // Hacer scroll al último mensaje
            listViewMensajes.scrollTo(mensaje);
            
            // Reproducir sonido de notificación (opcional)
            reproducirSonidoNotificacion();
        } else {
            // Mostrar badge de mensaje no leído
            System.out.println("✓ UI: Nuevo mensaje en otro canal");
            mostrarBadgeCanalConMensajesNuevos(mensaje.getCanalId());
        }
    }

    private void manejarHistorialRecibido(List<DTOMensajeCanal> historial) {
        System.out.println("✓ UI: Historial recibido (" + historial.size() + " mensajes)");
        listViewMensajes.getItems().clear();
        listViewMensajes.getItems().addAll(historial);
        
        // Hacer scroll al último mensaje
        if (!historial.isEmpty()) {
            listViewMensajes.scrollTo(historial.size() - 1);
        }
    }

    private void manejarNuevaInvitacion(Map<String, String> datos) {
        String nombreCanal = datos.get("nombreCanal");
        String invitadoPor = datos.get("invitadoPor");
        
        System.out.println("✓ UI: Nueva invitación a canal: " + nombreCanal);
        
        // Mostrar notificación visual
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nueva Invitación");
        alert.setHeaderText("Invitación a Canal");
        alert.setContentText(invitadoPor + " te ha invitado a unirte al canal: " + nombreCanal);
        alert.show();
        
        // Actualizar lista de canales
        listadorCanales.solicitarCanalesUsuario();
    }

    private void manejarNuevoMiembro(Map<String, String> datos) {
        String nombreUsuario = datos.get("nombreUsuario");
        String canalId = datos.get("canalId");
        
        System.out.println("✓ UI: Nuevo miembro en canal: " + nombreUsuario);
        
        // Si es el canal actual, actualizar lista de miembros
        if (canalSeleccionado != null && 
            canalId.equals(canalSeleccionado.getIdCanal().toString())) {
            // Aquí puedes actualizar una lista de miembros si la tienes
            mostrarNotificacionEnChat(nombreUsuario + " se ha unido al canal");
        }
    }

    private void manejarError(String mensajeError) {
        System.err.println("✗ UI: Error recibido: " + mensajeError);
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Ha ocurrido un error");
        alert.setContentText(mensajeError);
        alert.show();
    }

    private void manejarMensajeEnviadoExitoso() {
        System.out.println("✓ UI: Mensaje enviado exitosamente");
        txtMensaje.clear();
    }

    // === ACCIONES DE USUARIO ===

    private void cambiarCanalActivo(Canal canal) {
        this.canalSeleccionado = canal;
        lblCanalActual.setText("# " + canal.getNombre());
        
        System.out.println("→ Cambiando a canal: " + canal.getNombre());
        
        // Solicitar historial del canal
        gestorMensajes.solicitarHistorialCanal(canal.getIdCanal().toString(), 50);
    }

    private void enviarMensaje() {
        if (canalSeleccionado == null) {
            mostrarAlerta("Selecciona un canal primero");
            return;
        }

        String contenido = txtMensaje.getText().trim();
        if (contenido.isEmpty()) {
            return;
        }

        System.out.println("→ Enviando mensaje al canal: " + canalSeleccionado.getNombre());

        // Enviar mensaje de texto
        gestorMensajes.enviarMensajeTexto(canalSeleccionado.getIdCanal().toString(), contenido)
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    mostrarAlerta("Error al enviar mensaje: " + ex.getMessage());
                });
                return null;
            });
    }

    // === MÉTODOS AUXILIARES ===

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atención");
        alert.setContentText(mensaje);
        alert.show();
    }

    private void mostrarNotificacionEnChat(String mensaje) {
        // Aquí puedes mostrar un mensaje del sistema en el chat
        System.out.println("💬 " + mensaje);
    }

    private void reproducirSonidoNotificacion() {
        // Implementar reproducción de sonido
    }

    private void mostrarBadgeCanalConMensajesNuevos(String canalId) {
        // Implementar badge visual en el ListView
    }

    /**
     * Método llamado al cerrar la ventana.
     * IMPORTANTE: Desregistrarse como observador para evitar memory leaks.
     */
    public void cerrar() {
        listadorCanales.removerObservador(this);
        gestorMensajes.removerObservador(this);
        gestorNotificaciones.removerObservador(this);
        
        System.out.println("✓ Observadores removidos correctamente");
    }
}
```

---

## 💡 Ejemplos Completos por Funcionalidad

### 1. Crear un Nuevo Canal

```java
@FXML
private void btnCrearCanal_Click() {
    // Mostrar diálogo para ingresar nombre
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Crear Canal");
    dialog.setHeaderText("Nuevo Canal");
    dialog.setContentText("Nombre del canal:");

    dialog.showAndWait().ifPresent(nombre -> {
        if (!nombre.trim().isEmpty()) {
            ICreadorCanal creador = InicializadorGestionCanales.getInstancia().getCreadorCanal();
            
            creador.crearCanal(nombre, "Canal creado desde la UI")
                .thenAccept(canal -> {
                    Platform.runLater(() -> {
                        System.out.println("✓ Canal creado: " + canal.getNombre());
                        // Actualizar lista de canales
                        listadorCanales.solicitarCanalesUsuario();
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        mostrarAlerta("Error al crear canal: " + ex.getMessage());
                    });
                    return null;
                });
        }
    });
}
```

### 2. Invitar Miembro a Canal

```java
@FXML
private void btnInvitarMiembro_Click() {
    if (canalSeleccionado == null) {
        mostrarAlerta("Selecciona un canal primero");
        return;
    }

    // Mostrar diálogo para seleccionar contacto (esto depende de tu UI de contactos)
    String contactoId = mostrarDialogoSeleccionarContacto();
    
    if (contactoId != null) {
        IInvitadorMiembro invitador = InicializadorGestionCanales.getInstancia().getInvitadorMiembro();
        
        invitador.invitarMiembro(canalSeleccionado.getIdCanal().toString(), contactoId)
            .thenAccept(exitoso -> {
                Platform.runLater(() -> {
                    if (exitoso) {
                        System.out.println("✓ Invitación enviada");
                        mostrarAlerta("Invitación enviada exitosamente");
                    } else {
                        mostrarAlerta("No se pudo enviar la invitación");
                    }
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    mostrarAlerta("Error: " + ex.getMessage());
                });
                return null;
            });
    }
}
```

### 3. Enviar Mensaje de Audio

```java
@FXML
private void btnEnviarAudio_Click() {
    if (canalSeleccionado == null) {
        mostrarAlerta("Selecciona un canal primero");
        return;
    }

    // 1. Grabar audio (esto depende de tu implementación de grabación)
    File archivoAudio = grabarAudio();
    
    if (archivoAudio != null) {
        // 2. Subir el archivo al servidor
        IGestionArchivos gestionArchivos = obtenerGestorArchivos();
        
        gestionArchivos.subirArchivo(archivoAudio)
            .thenAccept(fileId -> {
                // 3. Enviar mensaje con el ID del archivo
                gestorMensajes.enviarMensajeAudio(
                    canalSeleccionado.getIdCanal().toString(), 
                    fileId
                ).thenRun(() -> {
                    System.out.println("✓ Mensaje de audio enviado");
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    mostrarAlerta("Error al enviar audio: " + ex.getMessage());
                });
                return null;
            });
    }
}
```

### 4. Mostrar Lista de Miembros

```java
@FXML
private void btnVerMiembros_Click() {
    if (canalSeleccionado == null) {
        mostrarAlerta("Selecciona un canal primero");
        return;
    }

    IListadorMiembros listador = InicializadorGestionCanales.getInstancia().getListadorMiembros();
    
    // Registrarse temporalmente como observador
    IObservador observadorTemp = (tipo, datos) -> {
        if ("MIEMBROS_ACTUALIZADOS".equals(tipo)) {
            Platform.runLater(() -> {
                List<DTOMiembroCanal> miembros = (List<DTOMiembroCanal>) datos;
                mostrarDialogoMiembros(miembros);
            });
        }
    };
    
    listador.registrarObservador(observadorTemp);
    listador.solicitarMiembros(canalSeleccionado.getIdCanal().toString());
}
```

---

## ⚠️ Manejo de Errores

### Patrón Recomendado

```java
gestorMensajes.enviarMensajeTexto(canalId, contenido)
    .thenAccept(resultado -> {
        // Éxito
        Platform.runLater(() -> {
            System.out.println("✓ Operación exitosa");
        });
    })
    .exceptionally(ex -> {
        // Error
        Platform.runLater(() -> {
            if (ex instanceof IllegalStateException) {
                mostrarAlerta("Sesión expirada. Por favor, inicia sesión nuevamente.");
                redirigirALogin();
            } else {
                mostrarAlerta("Error: " + ex.getMessage());
            }
        });
        return null;
    });
```

---

## ✅ Best Practices

### 1. Siempre usar Platform.runLater para actualizar UI

```java
@Override
public void actualizar(String tipoDeDato, Object datos) {
    // ✓ CORRECTO
    Platform.runLater(() -> {
        actualizarInterfaz(datos);
    });
    
    // ✗ INCORRECTO (puede causar excepciones)
    actualizarInterfaz(datos);
}
```

### 2. Desregistrar observadores al cerrar ventanas

```java
@Override
public void close() {
    // Remover todos los observadores
    listadorCanales.removerObservador(this);
    gestorMensajes.removerObservador(this);
    gestorNotificaciones.removerObservador(this);
}
```

### 3. Validar datos antes de enviar

```java
private void enviarMensaje() {
    // Validar canal seleccionado
    if (canalSeleccionado == null) {
        mostrarAlerta("Selecciona un canal");
        return;
    }
    
    // Validar contenido
    String contenido = txtMensaje.getText().trim();
    if (contenido.isEmpty()) {
        return;
    }
    
    // Validar longitud
    if (contenido.length() > 1000) {
        mostrarAlerta("El mensaje es demasiado largo");
        return;
    }
    
    // Enviar
    gestorMensajes.enviarMensajeTexto(canalSeleccionado.getId(), contenido);
}
```

### 4. Usar try-catch en manejadores críticos

```java
@Override
public void actualizar(String tipoDeDato, Object datos) {
    Platform.runLater(() -> {
        try {
            switch (tipoDeDato) {
                case "MENSAJE_CANAL_RECIBIDO":
                    manejarNuevoMensaje((DTOMensajeCanal) datos);
                    break;
                // ... más casos
            }
        } catch (Exception e) {
            System.err.println("Error en manejador de notificación: " + e.getMessage());
            e.printStackTrace();
        }
    });
}
```

---

## 🎯 Resumen de Flujo Completo

```
1. Usuario abre ventana de canales
   → VentanaCanales.initialize()
   → Registra observadores
   → Solicita lista de canales

2. Servidor responde con canales
   → GestorRespuesta recibe respuesta
   → ListadorCanales.manejarRespuestaListado()
   → Persiste en BD local
   → notificarObservadores("CANALES_ACTUALIZADOS", canales)

3. VentanaCanales recibe notificación
   → actualizar("CANALES_ACTUALIZADOS", canales)
   → Platform.runLater(() -> actualizar UI)

4. Usuario selecciona canal
   → cambiarCanalActivo(canal)
   → Solicita historial

5. Servidor envía historial
   → Mismo flujo de observador
   → UI se actualiza automáticamente

6. Usuario envía mensaje
   → enviarMensaje()
   → Persiste localmente
   → Envía al servidor
   → Notificación de confirmación

7. Otros usuarios reciben mensaje (push)
   → GestorMensajesCanal.manejarNuevoMensaje()
   → Persiste localmente
   → Notifica a observadores
   → UI se actualiza en tiempo real
```

---

📅 **Última actualización**: 16 de Octubre, 2025
👨‍💻 **Sistema de Gestión de Canales - Chat Unillanos**
# 🎨 GUÍA DE IMPLEMENTACIÓN EN LA UI - SISTEMA DE CANALES

## 📋 Índice
1. [Configuración Inicial](#configuración-inicial)
2. [Implementar IObservador en la Vista](#implementar-iobservador-en-la-vista)
3. [Ejemplos Completos por Funcionalidad](#ejemplos-completos-por-funcionalidad)
4. [Manejo de Errores](#manejo-de-errores)
5. [Best Practices](#best-practices)

---

## 🚀 Configuración Inicial

### Paso 1: El sistema se inicializa automáticamente al conectar

El sistema de gestión de canales se inicializa automáticamente después de una conexión exitosa gracias a la modificación en `GestionConexionImpl`:

```java
// En GestionConexionImpl.java
if (sesion != null && sesion.estaActiva()) {
    gestorConexion.setSesion(sesion);
    GestorRespuesta.getInstancia().iniciarEscucha();
    
    // ✓ Se inicializa automáticamente
    InicializadorGestionCanales.getInstancia().inicializar();
    
    return true;
}
```

### Paso 2: Acceder a los componentes desde la UI

```java
// En cualquier controlador o vista
InicializadorGestionCanales inicializador = InicializadorGestionCanales.getInstancia();

// Obtener los gestores que necesites
IListadorCanales listadorCanales = inicializador.getListadorCanales();
IGestorMensajesCanal gestorMensajes = inicializador.getGestorMensajesCanal();
ICreadorCanal creadorCanal = inicializador.getCreadorCanal();
// ... etc
```

---

## 👁️ Implementar IObservador en la Vista

### Ejemplo Completo: VentanaCanales.java

```java
package presentacion.vistas;

import dto.canales.DTOMensajeCanal;
import dominio.Canal;
import gestionCanales.inicializador.InicializadorGestionCanales;
import gestionCanales.listarCanales.IListadorCanales;
import gestionCanales.mensajes.IGestorMensajesCanal;
import gestionCanales.notificaciones.IGestorNotificacionesCanal;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import observador.IObservador;

import java.util.List;
import java.util.Map;

/**
 * Ventana principal de gestión de canales.
 * Implementa IObservador para recibir notificaciones en tiempo real.
 */
public class VentanaCanales implements IObservador {

    @FXML private ListView<Canal> listViewCanales;
    @FXML private ListView<DTOMensajeCanal> listViewMensajes;
    @FXML private TextField txtMensaje;
    @FXML private Button btnEnviar;
    @FXML private Label lblCanalActual;
    @FXML private VBox panelNotificaciones;

    // Componentes de negocio
    private IListadorCanales listadorCanales;
    private IGestorMensajesCanal gestorMensajes;
    private IGestorNotificacionesCanal gestorNotificaciones;

    private Canal canalSeleccionado;

    /**
     * Método de inicialización de JavaFX.
     * Se llama automáticamente después de cargar el FXML.
     */
    @FXML
    public void initialize() {
        // Obtener instancias de los gestores
        InicializadorGestionCanales inicializador = InicializadorGestionCanales.getInstancia();
        
        listadorCanales = inicializador.getListadorCanales();
        gestorMensajes = inicializador.getGestorMensajesCanal();
        gestorNotificaciones = inicializador.getGestorNotificacionesCanal();

        // ✓ REGISTRARSE COMO OBSERVADOR
        listadorCanales.registrarObservador(this);
        gestorMensajes.registrarObservador(this);
        gestorNotificaciones.registrarObservador(this);

        // Configurar eventos de UI
        configurarEventos();

        // Solicitar lista de canales
        listadorCanales.solicitarCanalesUsuario();
    }

    private void configurarEventos() {
        // Al seleccionar un canal
        listViewCanales.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cambiarCanalActivo(newVal);
            }
        });

        // Al hacer clic en enviar
        btnEnviar.setOnAction(e -> enviarMensaje());
        
        // Al presionar Enter en el campo de texto
        txtMensaje.setOnAction(e -> enviarMensaje());
    }

    /**
     * ★ MÉTODO PRINCIPAL DEL PATRÓN OBSERVER ★
     * Este método es llamado automáticamente por los gestores de negocio
     * cuando ocurre un evento relevante.
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        // ⚠️ IMPORTANTE: Todas las actualizaciones de UI deben hacerse en el hilo de JavaFX
        Platform.runLater(() -> {

