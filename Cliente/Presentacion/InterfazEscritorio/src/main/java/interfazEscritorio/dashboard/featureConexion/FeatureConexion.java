package interfazEscritorio.dashboard.featureConexion;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * Representa la barra de estado inferior con información de la conexión.
 */
public class FeatureConexion extends BorderPane {

    public FeatureConexion() {
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");

        // --- Información Izquierda ---
        HBox infoIzquierda = new HBox(10);
        Label statusLabel = new Label("Status: Connected");
        Label serverLabel = new Label("Server: chat.example.com");
        infoIzquierda.getChildren().addAll(statusLabel, serverLabel);

        // --- Información Derecha ---
        Label pingLabel = new Label("Ping: 45ms");

        this.setLeft(infoIzquierda);
        this.setRight(pingLabel);
    }
}
