package interfazEscritorio.dashboard;

import controlador.contactos.ControladorContactos;
import controlador.contactos.IControladorContactos;
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

    public VistaLobby() {
        FeatureHeader header = new FeatureHeader();
        // 1. La Vista ahora crea el Controlador, que es su punto de contacto correcto.
        IControladorContactos controladorContactos = new ControladorContactos();

        // 2. Pasar la instancia del Controlador al constructor de FeatureContactos.
        FeatureContactos contactos = new FeatureContactos(this::mostrarChatPrivado, controladorContactos);

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

    private void mostrarChatPrivado(String nombreUsuario) {
        VistaContactoChat chatView = new VistaContactoChat(nombreUsuario, this::mostrarNotificaciones);
        this.setCenter(chatView);
    }

    private void mostrarVistaCanal(String nombreCanal) {
        VistaCanal vistaCanal = new VistaCanal(nombreCanal, this::mostrarNotificaciones, this::mostrarVistaMiembros);
        this.setCenter(vistaCanal);
    }

    private void mostrarVistaMiembros(String nombreCanal) {
        // Ahora se le pasa la acción para invitar miembros.
        VistaMiembrosCanal vistaMiembros = new VistaMiembrosCanal(nombreCanal, () -> this.mostrarVistaCanal(nombreCanal), this::mostrarVistaInvitar);
        this.setCenter(vistaMiembros);
    }

    /**
     * Muestra la vista para invitar miembros a un canal.
     * @param nombreCanal El canal al que se invitará.
     */
    private void mostrarVistaInvitar(String nombreCanal) {
        // La acción 'onVolver' regresa a la vista de miembros.
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

