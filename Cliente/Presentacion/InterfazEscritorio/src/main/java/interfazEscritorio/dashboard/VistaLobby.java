package interfazEscritorio.dashboard;

import controlador.chat.ControladorChat;
import controlador.chat.IControladorChat;
import controlador.contactos.ControladorContactos;
import controlador.contactos.IControladorContactos;
import controlador.usuario.ControladorUsuario;
import controlador.usuario.IControladorUsuario;
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
    private DTOUsuario usuarioLogueado;

    public VistaLobby() {
        // Instanciar controladores
        this.controladorContactos = new ControladorContactos();
        this.controladorChat = new ControladorChat();
        this.controladorUsuario = new ControladorUsuario();

        // Cargar información del usuario logueado
        cargarInformacionUsuario();

        // Crear componentes (FeatureHeader ahora recibe el callback de cerrar sesión)
        FeatureHeader header = new FeatureHeader(this::cerrarSesion);
        FeatureContactos contactos = new FeatureContactos(this::mostrarChatPrivado, this.controladorContactos);
        FeatureCanales canales = new FeatureCanales(this::mostrarVistaCanal, this::mostrarVistaCrearCanal);
        this.panelNotificaciones = new FeatureNotificaciones();
        FeatureConexion barraEstado = new FeatureConexion();

        VBox panelIzquierdo = new VBox(20);
        panelIzquierdo.setStyle("-fx-background-color: #2c3e50;");
        panelIzquierdo.setMinWidth(200);
        panelIzquierdo.getChildren().addAll(contactos, canales);

        this.setTop(header);
        this.setLeft(panelIzquierdo);
        this.setCenter(panelNotificaciones);
        this.setBottom(barraEstado);
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

    private void mostrarVistaCanal(String nombreCanal) {
        VistaCanal vistaCanal = new VistaCanal(nombreCanal, this::mostrarNotificaciones, this::mostrarVistaMiembros);
        this.setCenter(vistaCanal);
    }

    private void mostrarVistaMiembros(String nombreCanal) {
        VistaMiembrosCanal vistaMiembros = new VistaMiembrosCanal(nombreCanal, () -> this.mostrarVistaCanal(nombreCanal), this::mostrarVistaInvitar);
        this.setCenter(vistaMiembros);
    }

    private void mostrarVistaInvitar(String nombreCanal) {
        VistaInvitarMiembro vistaInvitar = new VistaInvitarMiembro(nombreCanal, () -> this.mostrarVistaMiembros(nombreCanal));
        this.setCenter(vistaInvitar);
    }

    private void mostrarVistaCrearCanal() {
        VistaCrearCanal vistaCrearCanal = new VistaCrearCanal(this::mostrarNotificaciones);
        this.setCenter(vistaCrearCanal);
    }

    private void mostrarNotificaciones() {
        this.setCenter(panelNotificaciones);
    }
}
