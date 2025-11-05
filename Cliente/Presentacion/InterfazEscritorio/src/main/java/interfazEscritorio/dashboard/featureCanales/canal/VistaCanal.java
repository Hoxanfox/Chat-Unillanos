package interfazEscritorio.dashboard.featureCanales.canal;

import controlador.canales.IControladorCanales;
import dto.canales.DTOCanalCreado;
import dto.canales.DTOMensajeCanal;
import gestionArchivos.IGestionArchivos;
import gestionArchivos.GestionArchivosImpl;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import observador.IObservador;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class VistaCanal extends BorderPane implements IObservador {
    private final IControladorCanales controlador;
    private final IGestionArchivos gestionArchivos;
    private final DTOCanalCreado canal;
    private final VBox mensajesBox;
    private final TextField campoMensaje;
    private final Button btnEnviar;
    private final Button btnGrabarAudio;
    private final Button btnDetenerGrabacion;
    private final Button btnCancelarGrabacion;
    private final Button btnArchivo;
    private final Label lblEstadoGrabacion;

    private GrabadorAudio grabadorAudio;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public VistaCanal(DTOCanalCreado canal, Runnable onVolver, Consumer<DTOCanalCreado> onVerMiembros, IControladorCanales controlador) {
        this.controlador = controlador;
        this.gestionArchivos = new GestionArchivosImpl();
        this.grabadorAudio = null;
        this.canal = canal;
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #ecf0f1;");

        controlador.registrarObservadorMensajes(this);

        // === HEADER ===
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

        // === MENSAJES ===
        mensajesBox = new VBox(15);
        mensajesBox.setPadding(new Insets(10));
        mensajesBox.setStyle("-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-radius: 5;");

        Label cargando = new Label("Cargando mensajes...");
        cargando.setTextFill(Color.GRAY);
        mensajesBox.getChildren().add(cargando);

        ScrollPane scrollPane = new ScrollPane(mensajesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.vvalueProperty().bind(mensajesBox.heightProperty());

        // === INPUT AREA ===
        VBox inputArea = new VBox(5);
        HBox entradaBox = new HBox(10);
        entradaBox.setPadding(new Insets(10, 0, 5, 0));

        // Botón para grabar audio
        btnGrabarAudio = new Button("🎤 Grabar");
        btnGrabarAudio.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10;");
        btnGrabarAudio.setTooltip(new Tooltip("Grabar mensaje de audio"));
        btnGrabarAudio.setOnAction(e -> iniciarGrabacionAudio());

        // Botón para detener grabación
        btnDetenerGrabacion = new Button("⏹ Detener");
        btnDetenerGrabacion.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10;");
        btnDetenerGrabacion.setTooltip(new Tooltip("Detener y enviar grabación"));
        btnDetenerGrabacion.setOnAction(e -> detenerGrabacionAudio());
        btnDetenerGrabacion.setDisable(true);

        // Botón para cancelar grabación
        btnCancelarGrabacion = new Button("❌");
        btnCancelarGrabacion.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10;");
        btnCancelarGrabacion.setTooltip(new Tooltip("Cancelar grabación"));
        btnCancelarGrabacion.setOnAction(e -> cancelarGrabacionAudio());
        btnCancelarGrabacion.setDisable(true);

        // Botón para enviar archivo
        btnArchivo = new Button("📎");
        btnArchivo.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 5 10;");
        btnArchivo.setTooltip(new Tooltip("Enviar archivo"));
        btnArchivo.setOnAction(e -> seleccionarYEnviarArchivo());

        // Campo de texto
        campoMensaje = new TextField();
        campoMensaje.setPromptText("Type your message ...");
        HBox.setHgrow(campoMensaje, Priority.ALWAYS);

        // Botón enviar
        btnEnviar = new Button("Send");
        btnEnviar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 15;");
        btnEnviar.setOnAction(e -> enviarMensaje());
        campoMensaje.setOnAction(e -> enviarMensaje());

        entradaBox.getChildren().addAll(btnGrabarAudio, btnDetenerGrabacion, btnCancelarGrabacion, btnArchivo, campoMensaje, btnEnviar);

        // Label de estado
        lblEstadoGrabacion = new Label("");
        lblEstadoGrabacion.setTextFill(Color.DARKBLUE);
        lblEstadoGrabacion.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Label footerLabel = new Label("Canal ID: " + canal.getId());
        footerLabel.setTextFill(Color.GRAY);
        footerLabel.setFont(Font.font("Arial", 10));

        inputArea.getChildren().addAll(entradaBox, lblEstadoGrabacion, footerLabel);

        this.setCenter(scrollPane);
        this.setBottom(inputArea);

        // Solicitar historial inicial
        controlador.solicitarHistorialCanal(canal.getId(), 50);
    }

    private void enviarMensaje() {
        String contenido = campoMensaje.getText().trim();
        if (contenido.isEmpty()) return;

        btnEnviar.setDisable(true);
        campoMensaje.setDisable(true);

        controlador.enviarMensajeTexto(canal.getId(), contenido)
            .thenAccept(v -> Platform.runLater(() -> {
                campoMensaje.clear();
                btnEnviar.setDisable(false);
                campoMensaje.setDisable(false);
                campoMensaje.requestFocus();
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    btnEnviar.setDisable(false);
                    campoMensaje.setDisable(false);
                    mostrarError("Error al enviar mensaje: " + ex.getMessage());
                });
                return null;
            });
    }

    private void iniciarGrabacionAudio() {
        try {
            grabadorAudio = new GrabadorAudio();
            grabadorAudio.iniciarGrabacion();

            Platform.runLater(() -> {
                btnGrabarAudio.setDisable(true);
                btnDetenerGrabacion.setDisable(false);
                btnCancelarGrabacion.setDisable(false);
                lblEstadoGrabacion.setText("🔴 Grabando audio... Presione 'Detener' para finalizar.");
                lblEstadoGrabacion.setTextFill(Color.RED);
            });

            System.out.println("🎤 Grabación de audio iniciada");
        } catch (LineUnavailableException e) {
            Platform.runLater(() -> {
                mostrarError("Error al iniciar la grabación de audio: " + e.getMessage());
                lblEstadoGrabacion.setText("❌ Error al acceder al micrófono");
            });
            System.err.println("❌ Error al iniciar grabación: " + e.getMessage());
        }
    }

    private void detenerGrabacionAudio() {
        if (grabadorAudio != null) {
            File audioFile = grabadorAudio.detenerGrabacion();

            Platform.runLater(() -> {
                btnGrabarAudio.setDisable(true);
                btnDetenerGrabacion.setDisable(true);
                btnCancelarGrabacion.setDisable(true);
                lblEstadoGrabacion.setText("⏳ Subiendo audio al servidor...");
                lblEstadoGrabacion.setTextFill(Color.DARKBLUE);
            });

            // Primero subir el archivo al servidor
            gestionArchivos.subirArchivo(audioFile)
                .thenCompose(fileId -> {
                    System.out.println("✅ Audio subido con ID: " + fileId);
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("📤 Enviando mensaje de audio...");
                    });

                    // Luego enviar el mensaje con el ID del archivo
                    return controlador.enviarMensajeAudio(canal.getId(), fileId);
                })
                .thenRun(() -> {
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("✅ Mensaje de audio enviado");
                        lblEstadoGrabacion.setTextFill(Color.GREEN);
                        btnGrabarAudio.setDisable(false);

                        // Limpiar el mensaje después de 3 segundos
                        new Thread(() -> {
                            try {
                                Thread.sleep(3000);
                                Platform.runLater(() -> lblEstadoGrabacion.setText(""));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    });

                    // Eliminar archivo temporal
                    if (audioFile != null && audioFile.exists()) {
                        audioFile.delete();
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("❌ Error al enviar audio: " + ex.getMessage());
                        lblEstadoGrabacion.setTextFill(Color.RED);
                        btnGrabarAudio.setDisable(false);
                        btnDetenerGrabacion.setDisable(true);
                        btnCancelarGrabacion.setDisable(true);
                    });
                    System.err.println("❌ Error al enviar audio: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
        }
    }

    private void cancelarGrabacionAudio() {
        if (grabadorAudio != null) {
            grabadorAudio.cancelarGrabacion();

            Platform.runLater(() -> {
                btnGrabarAudio.setDisable(false);
                btnDetenerGrabacion.setDisable(true);
                btnCancelarGrabacion.setDisable(true);
                lblEstadoGrabacion.setText("❌ Grabación cancelada");
                lblEstadoGrabacion.setTextFill(Color.ORANGE);

                // Limpiar el mensaje después de 2 segundos
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(() -> lblEstadoGrabacion.setText(""));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            });

            System.out.println("❌ Grabación cancelada por el usuario");
        }
    }

    private void seleccionarYEnviarArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo para enviar");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Todos los Archivos", "*.*"),
            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.doc", "*.docx", "*.txt"),
            new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.ogg")
        );

        File archivo = fileChooser.showOpenDialog(this.getScene().getWindow());

        if (archivo != null) {
            btnArchivo.setDisable(true);
            lblEstadoGrabacion.setText("⏳ Subiendo archivo: " + archivo.getName() + "...");
            lblEstadoGrabacion.setTextFill(Color.DARKBLUE);

            // Primero subir el archivo al servidor
            gestionArchivos.subirArchivo(archivo)
                .thenCompose(fileId -> {
                    System.out.println("✅ Archivo subido con ID: " + fileId);
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("📤 Enviando archivo al canal...");
                    });

                    // Luego enviar el mensaje con el ID del archivo
                    return controlador.enviarArchivo(canal.getId(), fileId);
                })
                .thenRun(() -> {
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("✅ Archivo enviado: " + archivo.getName());
                        lblEstadoGrabacion.setTextFill(Color.GREEN);
                        btnArchivo.setDisable(false);

                        // Limpiar el mensaje después de 3 segundos
                        new Thread(() -> {
                            try {
                                Thread.sleep(3000);
                                Platform.runLater(() -> lblEstadoGrabacion.setText(""));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("❌ Error al enviar archivo: " + ex.getMessage());
                        lblEstadoGrabacion.setTextFill(Color.RED);
                        btnArchivo.setDisable(false);
                    });
                    System.err.println("❌ Error al enviar archivo: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
        }
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
        bubble.setPadding(new Insets(8));
        bubble.setMaxWidth(450);

        if (mensaje.isEsPropio()) {
            bubble.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: #d4edda; -fx-background-radius: 10;");
        } else {
            bubble.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;");
        }

        String nombreAutor = mensaje.isEsPropio() ? "Tú" : mensaje.getNombreRemitente();
        String hora = mensaje.getFechaEnvio() != null ? mensaje.getFechaEnvio().format(FORMATTER) : "";

        Label autorLabel = new Label(nombreAutor + " - " + hora);
        autorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        autorLabel.setTextFill(mensaje.isEsPropio() ? Color.DARKGREEN : Color.DARKBLUE);

        // Mostrar contenido según el tipo de mensaje
        if ("audio".equals(mensaje.getTipo())) {
            Label contenidoLabel = new Label("🎤 Mensaje de audio");
            contenidoLabel.setStyle("-fx-font-style: italic;");
            bubble.getChildren().addAll(autorLabel, contenidoLabel);
        } else if (mensaje.getContenido() != null) {
            Label contenidoLabel = new Label(mensaje.getContenido());
            contenidoLabel.setWrapText(true);
            contenidoLabel.setMaxWidth(400);
            bubble.getChildren().addAll(autorLabel, contenidoLabel);
        }

        mensajesBox.getChildren().add(bubble);
    }

    private void mostrarError(String mensaje) {
        Label errorLabel = new Label("❌ Error: " + mensaje);
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        mensajesBox.getChildren().add(errorLabel);
    }
}
