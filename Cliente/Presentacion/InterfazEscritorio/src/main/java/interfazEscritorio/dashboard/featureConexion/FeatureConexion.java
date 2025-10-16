package interfazEscritorio.dashboard.featureConexion;

import controlador.conexion.IControladorConexion;
import dto.gestionConexion.DTOEstadoConexion;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import observador.IObservador;

/**
 * Representa la barra de estado inferior con información de la conexión.
 * AHORA implementa IObservador para recibir actualizaciones en tiempo real.
 */
public class FeatureConexion extends BorderPane implements IObservador {

    private final IControladorConexion controlador;
    private final Label statusLabel;
    private final Label serverLabel;
    private final Label pingLabel;

    public FeatureConexion(IControladorConexion controlador) {
        this.controlador = controlador;
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");

        // Registrarse como observador
        controlador.registrarObservador(this);
        System.out.println("✅ [FeatureConexion]: Registrada como observador");

        // --- Información Izquierda ---
        HBox infoIzquierda = new HBox(10);
        statusLabel = new Label("Status: Disconnected");
        statusLabel.setTextFill(Color.RED);
        serverLabel = new Label("Server: Unknown");
        infoIzquierda.getChildren().addAll(statusLabel, serverLabel);

        // --- Información Derecha ---
        pingLabel = new Label("Ping: --ms");

        this.setLeft(infoIzquierda);
        this.setRight(pingLabel);

        // Solicitar estado inicial
        controlador.solicitarActualizacionEstado();
        System.out.println("📡 [FeatureConexion]: Solicitando estado de conexión...");
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [FeatureConexion]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            if ("ACTUALIZAR_ESTADO_CONEXION".equals(tipoDeDato) && datos instanceof DTOEstadoConexion) {
                DTOEstadoConexion estado = (DTOEstadoConexion) datos;
                System.out.println("✅ [FeatureConexion]: Actualizando estado de conexión");
                actualizarEstado(estado);
            }
        });
    }

    private void actualizarEstado(DTOEstadoConexion estado) {
        statusLabel.setText("Status: " + estado.getEstadoTexto());
        statusLabel.setTextFill(estado.isConectado() ? Color.GREEN : Color.RED);

        serverLabel.setText("Server: " + estado.getServidor());
        pingLabel.setText(estado.getPingTexto());

        System.out.println("🔄 [FeatureConexion]: Estado actualizado - " + estado.getMensaje());
    }
}
