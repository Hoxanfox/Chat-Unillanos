package interfazEscritorio.dashboard.featureCanales.canal;

import interfazEscritorio.dashboard.featureCanales.canal.miembrosCanal.VistaMiembrosCanal;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.function.Consumer;

/**
 * Representa la vista de chat para un canal específico, rediseñada.
 */
public class VistaCanal extends BorderPane {

    /**
     * @param nombreCanal   El nombre del canal.
     * @param onVolver      Acción para regresar a la vista de notificaciones.
     * @param onVerMiembros Acción para mostrar la vista de miembros del canal.
     */
    public VistaCanal(String nombreCanal, Runnable onVolver, Consumer<String> onVerMiembros) {
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #ecf0f1;");

        // --- Header ---
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        Label tituloChat = new Label("Channel: " + nombreCanal);
        tituloChat.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Botón de Miembros (reemplaza el icono)
        Button btnMiembros = new Button("Ver Miembros");
        btnMiembros.setOnAction(e -> onVerMiembros.accept(nombreCanal));

        Button btnVolver = new Button("← Volver");
        btnVolver.setOnAction(e -> onVolver.run());

        header.getChildren().addAll(tituloChat, spacer, btnMiembros, btnVolver);
        this.setTop(header);

        // --- Área de Mensajes (Centro) ---
        VBox mensajesBox = new VBox(15);
        mensajesBox.setPadding(new Insets(10));
        mensajesBox.setStyle("-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-radius: 5;");
        mensajesBox.getChildren().add(crearMensaje("SYSTEM", "Welcome to " + nombreCanal + " channel", Color.GRAY));
        mensajesBox.getChildren().add(crearMensaje("john_doe", "Hey team, let's discuss the upcoming release", Color.BLACK));
        mensajesBox.getChildren().add(crearMensaje("alice123", "I've prepared the test cases", Color.BLACK, Pos.CENTER_RIGHT));

        ScrollPane scrollPane = new ScrollPane(mensajesBox);
        scrollPane.setFitToWidth(true);

        // --- Área de Entrada de Texto (Abajo) ---
        VBox inputArea = new VBox(5);
        HBox entradaBox = new HBox(10);
        entradaBox.setPadding(new Insets(10, 0, 5, 0));
        TextField campoMensaje = new TextField();
        campoMensaje.setPromptText("Type your message ...");
        HBox.setHgrow(campoMensaje, javafx.scene.layout.Priority.ALWAYS);
        Button btnEnviar = new Button("Send");
        entradaBox.getChildren().addAll(campoMensaje, btnEnviar);

        Label footerLabel = new Label("Members: john_doe, alice123 (2)");
        footerLabel.setTextFill(Color.GRAY);

        inputArea.getChildren().addAll(entradaBox, footerLabel);

        this.setCenter(scrollPane);
        this.setBottom(inputArea);
    }

    private VBox crearMensaje(String autor, String contenido, Color color, Pos... alignment) {
        VBox bubble = new VBox(3);
        Label autorLabel = new Label(autor + " - 20:15");
        autorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        autorLabel.setTextFill(color);

        Label contenidoLabel = new Label(contenido);
        contenidoLabel.setWrapText(true);

        bubble.getChildren().addAll(autorLabel, contenidoLabel);
        if (alignment.length > 0) {
            bubble.setAlignment(alignment[0]);
        }
        return bubble;
    }
}

