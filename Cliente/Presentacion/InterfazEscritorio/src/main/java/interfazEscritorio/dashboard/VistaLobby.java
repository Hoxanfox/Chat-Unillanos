package interfazEscritorio.dashboard;

import controlador.canales.ControladorCanalesImpl;
import controlador.canales.IControladorCanales;
import controlador.chat.ControladorChat;
import controlador.chat.IControladorChat;
import controlador.conexion.ControladorConexion;
import controlador.conexion.IControladorConexion;
import controlador.contactos.ControladorContactos;
import controlador.contactos.IControladorContactos;
import controlador.notificaciones.ControladorNotificaciones;
import controlador.notificaciones.IControladorNotificaciones;
import controlador.usuario.ControladorUsuario;
import controlador.usuario.IControladorUsuario;
import dto.canales.DTOCanalCreado;
import dto.featureContactos.DTOContacto;
import dto.vistaLobby.DTOUsuario;
import interfazEscritorio.dashboard.featureCanales.FeatureCanales;
import interfazEscritorio.dashboard.featureCanales.canal.VistaCanal;
import interfazEscritorio.dashboard.featureCanales.canal.miembrosCanal.VistaInvitarMiembro;
import interfazEscritorio.dashboard.featureCanales.canal.miembrosCanal.VistaMiembrosCanal;
import interfazEscritorio.dashboard.featureCanales.crearCanal.VistaCrearCanal;
import interfazEscritorio.dashboard.featureConexion.FeatureConexion;
import interfazEscritorio.dashboard.featureContactos.FeatureContactos;
import interfazEscritorio.dashboard.featureContactos.chatContacto.VistaContactoChat;
import interfazEscritorio.dashboard.featureHeader.FeatureHeader;
import interfazEscritorio.dashboard.featureNotificaciones.FeatureNotificaciones;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class VistaLobby extends BorderPane {

    private final FeatureNotificaciones panelNotificaciones;
    private final IControladorContactos controladorContactos;
    private final IControladorChat controladorChat;
    private final IControladorUsuario controladorUsuario;
    private final IControladorCanales controladorCanales;
    private final IControladorNotificaciones controladorNotificaciones;
    private final IControladorConexion controladorConexion;
    private DTOUsuario usuarioLogueado;

    public VistaLobby() {
        // Instanciar controladores
        this.controladorContactos = new ControladorContactos();
        this.controladorChat = new ControladorChat();
        this.controladorUsuario = new ControladorUsuario();
        this.controladorCanales = new ControladorCanalesImpl();
        this.controladorNotificaciones = new ControladorNotificaciones();
        this.controladorConexion = new ControladorConexion();

        // Cargar información del usuario logueado
        cargarInformacionUsuario();

        // Crear componentes con sus controladores
        FeatureHeader header = new FeatureHeader(this::cerrarSesion, this.controladorNotificaciones);
        FeatureContactos contactos = new FeatureContactos(this::mostrarChatPrivado, this.controladorContactos);
        FeatureCanales canales = new FeatureCanales(this::mostrarVistaCanal, this::mostrarVistaCrearCanal, this.controladorCanales);
        this.panelNotificaciones = new FeatureNotificaciones(this.controladorNotificaciones);
        FeatureConexion barraEstado = new FeatureConexion(this.controladorConexion);

        VBox panelIzquierdo = new VBox(20);
        panelIzquierdo.setStyle("-fx-background-color: #2c3e50;");
        panelIzquierdo.setMinWidth(200);
        panelIzquierdo.getChildren().addAll(contactos, canales);

        this.setTop(header);
        this.setLeft(panelIzquierdo);
        this.setCenter(panelNotificaciones);
        this.setBottom(barraEstado);

        // Inicializar manejadores de mensajes de canal
        controladorCanales.inicializarManejadoresMensajes();
    }

    private void cargarInformacionUsuario() {
        System.out.println("🔄 [VistaLobby]: Cargando información del usuario logueado...");

        controladorUsuario.cargarInformacionUsuarioLogueado()
                .thenAccept(usuario -> {
                    Platform.runLater(() -> {
                        this.usuarioLogueado = usuario;
                        System.out.println("✅ [VistaLobby]: Usuario cargado: " + usuario.getNombre());
                    });
                })
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        System.err.println("❌ [VistaLobby]: Error al cargar usuario: " + error.getMessage());
                    });
                    return null;
                });
    }

    private void cerrarSesion() {
        System.out.println("🚪 [VistaLobby]: Cerrando sesión...");
        controladorUsuario.cerrarSesion();
        // Aquí puedes navegar de vuelta al login
        System.out.println("✅ [VistaLobby]: Sesión cerrada. Redirigir al login.");
    }

    public DTOUsuario getUsuarioLogueado() {
        return usuarioLogueado;
    }

    private void mostrarChatPrivado(DTOContacto contacto) {
        VistaContactoChat chatView = new VistaContactoChat(contacto, this.controladorChat, this::mostrarNotificaciones);
        this.setCenter(chatView);
    }

    private void mostrarVistaCanal(DTOCanalCreado canal) {
        VistaCanal vistaCanal = new VistaCanal(canal, this::mostrarNotificaciones, this::mostrarVistaMiembros, this.controladorCanales);
        this.setCenter(vistaCanal);
    }

    private void mostrarVistaMiembros(DTOCanalCreado canal) {
        VistaMiembrosCanal vistaMiembros = new VistaMiembrosCanal(
            canal.getId(),
            canal.getNombre(),
            () -> this.mostrarVistaCanal(canal),
            (canalId) -> this.mostrarVistaInvitar(canal),
            this.controladorCanales
        );
        this.setCenter(vistaMiembros);
    }

    private void mostrarVistaInvitar(DTOCanalCreado canal) {
        VistaInvitarMiembro vistaInvitar = new VistaInvitarMiembro(
            canal.getId(),
            canal.getNombre(),
            () -> this.mostrarVistaMiembros(canal),
            this.controladorContactos,
            this.controladorCanales
        );
        this.setCenter(vistaInvitar);
    }

    private void mostrarVistaCrearCanal() {
        VistaCrearCanal vistaCrearCanal = new VistaCrearCanal(this::mostrarNotificaciones, this.controladorCanales);
        this.setCenter(vistaCrearCanal);
    }

    private void mostrarNotificaciones() {
        this.setCenter(panelNotificaciones);
    }
}
