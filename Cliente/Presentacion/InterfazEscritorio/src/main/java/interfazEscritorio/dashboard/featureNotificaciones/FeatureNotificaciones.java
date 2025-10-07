package interfazEscritorio.dashboard.featureNotificaciones;

import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Representa el panel central que muestra una lista de notificaciones.
 */
public class FeatureNotificaciones extends VBox {

    public FeatureNotificaciones() {
        super(15);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #ecf0f1;");

        // --- Cabecera de Notificaciones ---
        BorderPane header = new BorderPane();
        Label titulo = new Label("NOTIFICATIONS (5)");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        Hyperlink verTodoLink = new Hyperlink("See All");
        header.setLeft(titulo);
        header.setRight(verTodoLink);

        // --- Contenedor de Tarjetas ---
        VBox tarjetasContainer = new VBox(10);
        tarjetasContainer.getChildren().add(crearTarjetaNotificacion("NEW (3)", "alice123 mentioned you in Team Alpha", "2 minutes ago"));
        tarjetasContainer.getChildren().add(crearTarjetaNotificacion("bob_smith sent you a message", "Do you have time to chat?", "16 minutes ago"));
        tarjetasContainer.getChildren().add(crearTarjetaNotificacion("emma_j sends a friend request", "Review 'Appel it on Project Beta' again!", "5 hours ago"));

        // --- Scroll Pane para las tarjetas ---
        ScrollPane scrollPane = new ScrollPane(tarjetasContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        this.getChildren().addAll(header, scrollPane);
    }

    private VBox crearTarjetaNotificacion(String titulo, String contenido, String tiempo) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d0d0d0; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label tituloLabel = new Label(titulo);
        tituloLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Text contenidoText = new Text(contenido);
        contenidoText.setWrappingWidth(350); // Para que el texto se ajuste

        Label tiempoLabel = new Label(tiempo);
        tiempoLabel.setStyle("-fx-text-fill: gray;");

        card.getChildren().addAll(tituloLabel, contenidoText, tiempoLabel);
        return card;
    }
}
