package interfazEscritorio.dashboard;

import controlador.chat.ControladorChat;
import controlador.chat.IControladorChat;
import controlador.contactos.ControladorContactos;
import controlador.contactos.IControladorContactos;
import dto.featureContactos.DTOContacto;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Vista principal que ensambla los features y gestiona la navegación interna.
 */
public class VistaLobby extends BorderPane {

    private final FeatureNotificaciones panelNotificaciones;
    // CORRECCIÓN 1: Los controladores ahora son campos de la clase.
    private final IControladorContactos controladorContactos;
    private final IControladorChat controladorChat;

    public VistaLobby() {
        // Se instancian los controladores en el constructor.
        this.controladorContactos = new ControladorContactos();
        this.controladorChat = new ControladorChat();

        FeatureHeader header = new FeatureHeader();
        // Se pasa la instancia del controlador correcta al constructor de FeatureContactos.
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

    /**
     * CORRECCIÓN 2: El método ahora recibe el objeto DTOContacto completo
     * y llama al constructor correcto de VistaContactoChat.
     * @param contacto El DTO del contacto seleccionado.
     */
    private void mostrarChatPrivado(DTOContacto contacto) {
        VistaContactoChat chatView = new VistaContactoChat(contacto, this.controladorChat, this::mostrarNotificaciones);
        this.setCenter(chatView);
    }

    // ... el resto de los métodos de navegación no necesitan cambios ...
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

