package interfazEscritorio.dashboard.featureContactos.chatContacto;

import controlador.chat.IControladorChat;
import dto.featureContactos.DTOContacto;
import dto.vistaContactoChat.DTOMensaje;
import observador.IObservador;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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

import java.util.List;

/**
 * Vista de chat privado que AHORA gestiona el estado de grabación de audio.
 */
public class VistaContactoChat extends BorderPane implements IObservador {

    private final IControladorChat controlador;
    private final DTOContacto contacto;
    private final Runnable onVolver;
    private final VBox mensajesBox;
    private boolean isRecording = false; // Estado para saber si se está grabando

    public VistaContactoChat(DTOContacto contacto, IControladorChat controlador, Runnable onVolver) {
        this.contacto = contacto;
        this.controlador = controlador;
        this.onVolver = onVolver;

        // 1. Suscribirse para recibir nuevos mensajes
        this.controlador.registrarObservador(this);

        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #ecf0f1;");

        // --- Header ---
        this.setTop(crearHeader());

        // --- Área de Mensajes (Centro) ---
        mensajesBox = new VBox(10);
        mensajesBox.setPadding(new Insets(10));
        mensajesBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        ScrollPane scrollPane = new ScrollPane(mensajesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.vvalueProperty().bind(mensajesBox.heightProperty()); // Auto-scroll
        this.setCenter(scrollPane);

        // --- Área de Entrada (Abajo) ---
        this.setBottom(crearPanelInferior());

        // 2. Solicitar el historial de mensajes al abrir la vista
        this.controlador.solicitarHistorial(contacto.getId());
    }

    private Node crearHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);
        header.setPadding(new Insets(0, 0, 10, 0));
        Label tituloChat = new Label("Private Chat: " + contacto.getNombre());
        tituloChat.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        Button btnVolver = new Button("← Volver");
        btnVolver.setOnAction(e -> onVolver.run());
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(tituloChat, spacer, btnVolver);
        return header;
    }

    private Node crearPanelInferior() {
        HBox entradaBox = new HBox(10);
        entradaBox.setAlignment(Pos.CENTER);
        TextField campoMensaje = new TextField();
        campoMensaje.setPromptText("Type your message...");
        HBox.setHgrow(campoMensaje, javafx.scene.layout.Priority.ALWAYS);

        Button btnAudio = new Button("🎤"); // Micrófono
        Button btnEnviar = new Button("Send");

        // Lógica del botón de Audio
        btnAudio.setOnAction(e -> {
            if (isRecording) {
                // Si se está grabando, el botón de audio actúa como "Cancelar"
                controlador.cancelarGrabacion();
                isRecording = false;
                btnAudio.setText("🎤");
                campoMensaje.setDisable(false);
                System.out.println("🎤 [VistaChat]: Grabación cancelada.");
            } else {
                // Si no se está grabando, inicia la grabación
                controlador.iniciarGrabacionAudio();
                isRecording = true;
                btnAudio.setText("❌"); // Cambia a un ícono de "cancelar"
                campoMensaje.setDisable(true); // Deshabilita el texto mientras se graba
                System.out.println("🔴 [VistaChat]: Iniciando grabación...");
            }
        });

        // Lógica del botón de Enviar
        btnEnviar.setOnAction(e -> {
            if (isRecording) {
                // Si se está grabando, "Send" detiene y envía el audio
                System.out.println("➡️ [VistaChat]: Deteniendo y enviando grabación de audio...");
                controlador.detenerYEnviarGrabacion(contacto.getId());
                isRecording = false;
                btnAudio.setText("🎤");
                campoMensaje.setDisable(false);
            } else {
                // Si no se está grabando, envía el mensaje de texto
                String texto = campoMensaje.getText();
                if (texto != null && !texto.trim().isEmpty()) {
                    System.out.println("➡️ [VistaChat]: Enviando mensaje de texto...");
                    controlador.enviarMensajeTexto(contacto.getId(), texto);
                    campoMensaje.clear();
                }
            }
        });

        entradaBox.getChildren().addAll(campoMensaje, btnAudio, btnEnviar);
        return new VBox(5, entradaBox, new Label("Status: " + contacto.getNombre() + " is online."));
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        if ("NUEVO_MENSAJE_PRIVADO".equals(tipoDeDato) && datos instanceof DTOMensaje) {
            Platform.runLater(() -> agregarMensaje((DTOMensaje) datos));
        } else if ("HISTORIAL_MENSAJES".equals(tipoDeDato) && datos instanceof List) {
            Platform.runLater(() -> {
                mensajesBox.getChildren().clear();
                ((List<DTOMensaje>) datos).forEach(this::agregarMensaje);
            });
        }
    }

    private void agregarMensaje(DTOMensaje mensaje) {
        Pos alineacion = mensaje.esMio() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;
        mensajesBox.getChildren().add(crearBurbujaMensaje(mensaje.getAutorConFecha(), mensaje.getContenido(), alineacion));
    }

    private VBox crearBurbujaMensaje(String autor, String contenido, Pos alineacion) {
        VBox burbuja = new VBox(3);
        burbuja.setPadding(new Insets(8));
        burbuja.setMaxWidth(300);
        Label autorLabel = new Label(autor);
        autorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        Text contenidoText = new Text(contenido);
        contenidoText.setWrappingWidth(280);
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

