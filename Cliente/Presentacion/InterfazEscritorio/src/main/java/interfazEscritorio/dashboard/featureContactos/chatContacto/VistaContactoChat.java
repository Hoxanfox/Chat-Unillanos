package interfazEscritorio.dashboard.featureContactos.chatContacto;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Representa la vista de chat privado con un contacto específico.
 */
public class VistaContactoChat extends BorderPane {

    /**
     * @param nombreContacto El nombre del contacto con el que se chatea.
     * @param onVolver       Acción a ejecutar para volver a la vista anterior (notificaciones).
     */
    public VistaContactoChat(String nombreContacto, Runnable onVolver) {
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #ecf0f1;"); // Coincide con el fondo del lobby

        // --- Header con Título y Botón de Volver (Arriba) ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label tituloChat = new Label("Private Chat: " + nombreContacto);
        tituloChat.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Button btnVolver = new Button("← Volver");
        btnVolver.setOnAction(e -> onVolver.run());

        // Un espaciador para empujar el botón de volver a la derecha (opcional, pero mejora el layout)
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(tituloChat, spacer, btnVolver);
        this.setTop(header);


        // --- Área de Mensajes (Centro) ---
        VBox mensajesBox = new VBox(10);
        mensajesBox.setPadding(new Insets(10));
        mensajesBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        mensajesBox.getChildren().add(crearBurbujaMensaje("john_doe - 20:15", "Hey Alice, how's the project going?", Pos.CENTER_LEFT));
        mensajesBox.getChildren().add(crearBurbujaMensaje("alice123 - 20:16", "Hi John! It's going well. I've finished the first module.", Pos.CENTER_RIGHT));
        mensajesBox.getChildren().add(crearBurbujaMensaje("john_doe - 20:17", "That's great! Can you send me the documentation?", Pos.CENTER_LEFT));
        mensajesBox.getChildren().add(crearBurbujaMensaje("alice123 - 20:18", "Sure, here it is. [documentation.pdf]", Pos.CENTER_RIGHT));

        ScrollPane scrollPane = new ScrollPane(mensajesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.vvalueProperty().bind(mensajesBox.heightProperty());

        // --- Área de Entrada de Texto (Abajo) ---
        HBox entradaBox = new HBox(10);
        entradaBox.setPadding(new Insets(10, 0, 0, 0));
        entradaBox.setAlignment(Pos.CENTER);

        TextField campoMensaje = new TextField();
        campoMensaje.setPromptText("Type your message...");
        HBox.setHgrow(campoMensaje, javafx.scene.layout.Priority.ALWAYS); // Hacer que el campo crezca

        Button btnAdjuntar = new Button("📎");
        Button btnEnviar = new Button("Send");

        entradaBox.getChildren().addAll(campoMensaje, btnAdjuntar, btnEnviar);

        Label statusLabel = new Label("Status: " + nombreContacto + " is typing....");
        statusLabel.setPadding(new Insets(5, 0, 0, 0));

        VBox bottomContainer = new VBox(5, entradaBox, statusLabel);

        this.setCenter(scrollPane);
        this.setBottom(bottomContainer);
    }

    private VBox crearBurbujaMensaje(String autor, String contenido, Pos alineacion) {
        VBox burbuja = new VBox(3);
        burbuja.setPadding(new Insets(8));
        burbuja.setMaxWidth(300); // Un poco más de ancho para los mensajes

        Label autorLabel = new Label(autor);
        autorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        Text contenidoText = new Text(contenido);
        contenidoText.setWrappingWidth(280); // Permite que el texto se ajuste

        if (alineacion == Pos.CENTER_RIGHT) {
            burbuja.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 10;");
        } else {
            burbuja.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");
        }

        burbuja.getChildren().addAll(autorLabel, contenidoText);

        HBox wrapper = new HBox(burbuja);
        wrapper.setAlignment(alineacion);

        return new VBox(wrapper);
    }
}

