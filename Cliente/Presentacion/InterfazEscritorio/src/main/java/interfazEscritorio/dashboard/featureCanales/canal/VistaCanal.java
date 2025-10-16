package interfazEscritorio.dashboard.featureCanales.canal;
import controlador.canales.IControladorCanales;
import dto.canales.DTOCanalCreado;
import dto.canales.DTOMensajeCanal;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import observador.IObservador;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
public class VistaCanal extends BorderPane implements IObservador {
    private final IControladorCanales controlador;
    private final DTOCanalCreado canal;
    private final VBox mensajesBox;
    private final TextField campoMensaje;
    private final Button btnEnviar;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public VistaCanal(DTOCanalCreado canal, Runnable onVolver, Consumer<DTOCanalCreado> onVerMiembros, IControladorCanales controlador) {
        this.controlador = controlador;
        this.canal = canal;
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #ecf0f1;");
        controlador.registrarObservadorMensajes(this);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        Label tituloChat = new Label("Channel: " + canal.getNombre());
        tituloChat.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnMiembros = new Button("Ver Miembros");
        btnMiembros.setOnAction(e -> onVerMiembros.accept(canal));
        Button btnVolver = new Button("← Volver");
        btnVolver.setOnAction(e -> onVolver.run());
        header.getChildren().addAll(tituloChat, spacer, btnMiembros, btnVolver);
        this.setTop(header);
        mensajesBox = new VBox(15);
        mensajesBox.setPadding(new Insets(10));
        mensajesBox.setStyle("-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-radius: 5;");
        Label cargando = new Label("Cargando mensajes...");
        cargando.setTextFill(Color.GRAY);
        mensajesBox.getChildren().add(cargando);
        ScrollPane scrollPane = new ScrollPane(mensajesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.vvalueProperty().bind(mensajesBox.heightProperty());
        VBox inputArea = new VBox(5);
        HBox entradaBox = new HBox(10);
        entradaBox.setPadding(new Insets(10, 0, 5, 0));
        campoMensaje = new TextField();
        campoMensaje.setPromptText("Type your message ...");
        HBox.setHgrow(campoMensaje, Priority.ALWAYS);
        btnEnviar = new Button("Send");
        btnEnviar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnEnviar.setOnAction(e -> enviarMensaje());
        campoMensaje.setOnAction(e -> enviarMensaje());
        entradaBox.getChildren().addAll(campoMensaje, btnEnviar);
        Label footerLabel = new Label("Canal ID: " + canal.getId());
        footerLabel.setTextFill(Color.GRAY);
        inputArea.getChildren().addAll(entradaBox, footerLabel);
        this.setCenter(scrollPane);
        this.setBottom(inputArea);
        controlador.solicitarHistorialCanal(canal.getId(), 50);
    }
    private void enviarMensaje() {
        String contenido = campoMensaje.getText().trim();
        if (contenido.isEmpty()) return;
        btnEnviar.setDisable(true);
        campoMensaje.setDisable(true);
        CompletableFuture<Void> futuroEnvio = controlador.enviarMensajeTexto(canal.getId(), contenido);
        futuroEnvio.thenAccept(v -> Platform.runLater(() -> {
            campoMensaje.clear();
            btnEnviar.setDisable(false);
            campoMensaje.setDisable(false);
            campoMensaje.requestFocus();
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                btnEnviar.setDisable(false);
                campoMensaje.setDisable(false);
            });
            return null;
        });
    }
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "HISTORIAL_CANAL_RECIBIDO":
                    if (datos instanceof List) {
                        cargarHistorial((List<DTOMensajeCanal>) datos);
                    }
                    break;
                case "MENSAJE_CANAL_RECIBIDO":
                    if (datos instanceof DTOMensajeCanal) {
                        DTOMensajeCanal mensaje = (DTOMensajeCanal) datos;
                        if (mensaje.getCanalId().equals(canal.getId())) {
                            agregarMensaje(mensaje);
                        }
                    }
                    break;
                case "ERROR_OPERACION":
                    mostrarError(datos.toString());
                    break;
            }
        });
    }
    private void cargarHistorial(List<DTOMensajeCanal> mensajes) {
        mensajesBox.getChildren().clear();
        if (mensajes.isEmpty()) {
            Label sinMensajes = new Label("No hay mensajes en este canal.");
            sinMensajes.setTextFill(Color.GRAY);
            mensajesBox.getChildren().add(sinMensajes);
        } else {
            for (DTOMensajeCanal mensaje : mensajes) {
                agregarMensaje(mensaje);
            }
        }
    }
    private void agregarMensaje(DTOMensajeCanal mensaje) {
        VBox bubble = new VBox(3);
        bubble.setPadding(new Insets(5));
        if (mensaje.isEsPropio()) {
            bubble.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: #d4edda; -fx-background-radius: 10; -fx-padding: 8;");
        } else {
            bubble.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 8;");
        }
        String nombreAutor = mensaje.isEsPropio() ? "Tú" : mensaje.getNombreRemitente();
        String hora = mensaje.getFechaEnvio() != null ? mensaje.getFechaEnvio().format(FORMATTER) : "";
        Label autorLabel = new Label(nombreAutor + " - " + hora);
        autorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        autorLabel.setTextFill(mensaje.isEsPropio() ? Color.DARKGREEN : Color.DARKBLUE);
        Label contenidoLabel = new Label(mensaje.getContenido());
        contenidoLabel.setWrapText(true);
        contenidoLabel.setMaxWidth(400);
        bubble.getChildren().addAll(autorLabel, contenidoLabel);
        mensajesBox.getChildren().add(bubble);
    }
    private void mostrarError(String mensaje) {
        Label errorLabel = new Label("Error: " + mensaje);
        errorLabel.setTextFill(Color.RED);
        mensajesBox.getChildren().add(errorLabel);
    }
}
