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
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import observador.IObservador;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private boolean isRecording = false;

    // Evitar mensajes duplicados
    private final Set<String> mensajesMostrados = Collections.synchronizedSet(new HashSet<>());

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public VistaCanal(DTOCanalCreado canal, Runnable onVolver, Consumer<DTOCanalCreado> onVerMiembros, IControladorCanales controlador) {
        System.out.println("🔧 [VistaCanal]: Inicializando vista de canal...");
        System.out.println("   → Canal: " + canal.getNombre() + " (ID: " + canal.getId() + ")");

        this.controlador = controlador;
        this.gestionArchivos = new GestionArchivosImpl();
        this.grabadorAudio = null;
        this.canal = canal;
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #ecf0f1;");

        System.out.println("🔔 [VistaCanal]: Registrándose como observador del controlador...");
        controlador.registrarObservadorMensajes(this);

        // === HEADER ===
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label tituloChat = new Label("📢 Canal: " + canal.getNombre());
        tituloChat.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnMiembros = new Button("👥 Ver Miembros");
        btnMiembros.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-padding: 5 10;");
        btnMiembros.setOnAction(e -> onVerMiembros.accept(canal));

        Button btnVolver = new Button("← Volver");
        btnVolver.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 5 10;");
        btnVolver.setOnAction(e -> {
            System.out.println("🔙 [VistaCanal]: Regresando a la lista de canales");
            onVolver.run();
        });

        header.getChildren().addAll(tituloChat, spacer, btnMiembros, btnVolver);
        this.setTop(header);

        // === MENSAJES ===
        mensajesBox = new VBox(10);
        mensajesBox.setPadding(new Insets(10));
        mensajesBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        Label cargando = new Label("Cargando mensajes del canal...");
        cargando.setTextFill(Color.GRAY);
        mensajesBox.getChildren().add(cargando);

        ScrollPane scrollPane = new ScrollPane(mensajesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.vvalueProperty().bind(mensajesBox.heightProperty());

        // === INPUT AREA ===
        VBox inputArea = new VBox(5);
        HBox entradaBox = new HBox(10);
        entradaBox.setPadding(new Insets(10, 0, 5, 0));
        entradaBox.setAlignment(Pos.CENTER);

        // Botón para grabar audio
        btnGrabarAudio = new Button("🎤");
        btnGrabarAudio.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 5 10;");
        btnGrabarAudio.setTooltip(new Tooltip("Grabar mensaje de audio"));
        btnGrabarAudio.setOnAction(e -> manejarBotonAudio());

        // Botón para cancelar grabación
        btnCancelarGrabacion = new Button("❌");
        btnCancelarGrabacion.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10;");
        btnCancelarGrabacion.setTooltip(new Tooltip("Cancelar grabación"));
        btnCancelarGrabacion.setOnAction(e -> cancelarGrabacionAudio());
        btnCancelarGrabacion.setVisible(false);

        // Botón para enviar archivo
        btnArchivo = new Button("📎");
        btnArchivo.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 5 10;");
        btnArchivo.setTooltip(new Tooltip("Enviar archivo"));
        btnArchivo.setOnAction(e -> seleccionarYEnviarArchivo());

        // Campo de texto
        campoMensaje = new TextField();
        campoMensaje.setPromptText("Type your message...");
        HBox.setHgrow(campoMensaje, Priority.ALWAYS);
        campoMensaje.setOnAction(e -> manejarBotonEnviar());

        // Botón enviar
        btnEnviar = new Button("Send");
        btnEnviar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 15;");
        btnEnviar.setOnAction(e -> manejarBotonEnviar());

        // Botón para detener grabación (oculto inicialmente)
        btnDetenerGrabacion = new Button("⏹ Detener");
        btnDetenerGrabacion.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10;");
        btnDetenerGrabacion.setVisible(false);

        entradaBox.getChildren().addAll(btnGrabarAudio, btnCancelarGrabacion, btnArchivo, campoMensaje, btnEnviar);

        // Label de estado
        lblEstadoGrabacion = new Label("");
        lblEstadoGrabacion.setTextFill(Color.DARKBLUE);
        lblEstadoGrabacion.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Label footerLabel = new Label("📢 Todos los miembros del canal pueden ver los mensajes");
        footerLabel.setTextFill(Color.GRAY);
        footerLabel.setFont(Font.font("Arial", 10));

        inputArea.getChildren().addAll(entradaBox, lblEstadoGrabacion, footerLabel);

        this.setCenter(scrollPane);
        this.setBottom(inputArea);

        // Solicitar historial inicial
        System.out.println("📡 [VistaCanal]: Solicitando historial del canal...");
        controlador.solicitarHistorialCanal(canal.getId(), 50);
        System.out.println("✅ [VistaCanal]: Vista inicializada correctamente");
    }

    /**
     * Maneja el botón de audio: inicia grabación o cancela si ya está grabando
     */
    private void manejarBotonAudio() {
        if (isRecording) {
            // Cancelar grabación
            System.out.println("🎤 [VistaCanal]: Cancelando grabación...");
            cancelarGrabacionAudio();
        } else {
            // Iniciar grabación
            System.out.println("🔴 [VistaCanal]: Iniciando grabación...");
            iniciarGrabacionAudio();
        }
    }

    /**
     * Maneja el botón de enviar: envía audio si está grabando, o mensaje de texto si no
     */
    private void manejarBotonEnviar() {
        if (isRecording) {
            // Detener y enviar audio
            System.out.println("➡️ [VistaCanal]: Deteniendo y enviando grabación de audio...");
            detenerGrabacionAudio();
        } else {
            // Enviar mensaje de texto
            enviarMensaje();
        }
    }

    private void enviarMensaje() {
        String contenido = campoMensaje.getText().trim();
        if (contenido.isEmpty()) return;

        System.out.println("➡️ [VistaCanal]: Enviando mensaje de texto...");
        System.out.println("   → Canal: " + canal.getId());
        System.out.println("   → Contenido: " + contenido);

        btnEnviar.setDisable(true);
        campoMensaje.setDisable(true);

        controlador.enviarMensajeTexto(canal.getId(), contenido)
            .thenAccept(v -> Platform.runLater(() -> {
                campoMensaje.clear();
                btnEnviar.setDisable(false);
                campoMensaje.setDisable(false);
                campoMensaje.requestFocus();
                System.out.println("✅ [VistaCanal]: Mensaje enviado exitosamente");
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    btnEnviar.setDisable(false);
                    campoMensaje.setDisable(false);
                    mostrarError("Error al enviar mensaje: " + ex.getMessage());
                });
                System.err.println("❌ [VistaCanal]: Error al enviar mensaje: " + ex.getMessage());
                return null;
            });
    }

    private void iniciarGrabacionAudio() {
        try {
            grabadorAudio = new GrabadorAudio();
            grabadorAudio.iniciarGrabacion();

            isRecording = true;

            Platform.runLater(() -> {
                btnGrabarAudio.setText("❌");
                btnGrabarAudio.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 5 10;");
                btnCancelarGrabacion.setVisible(true);
                campoMensaje.setDisable(true);
                btnArchivo.setDisable(true);
                lblEstadoGrabacion.setText("🔴 Grabando audio... Presione 'Send' para enviar o '❌' para cancelar.");
                lblEstadoGrabacion.setTextFill(Color.RED);
            });

            System.out.println("🎤 [VistaCanal]: Grabación de audio iniciada");
        } catch (LineUnavailableException e) {
            Platform.runLater(() -> {
                mostrarError("Error al iniciar la grabación de audio: " + e.getMessage());
                lblEstadoGrabacion.setText("❌ Error al acceder al micrófono");
            });
            System.err.println("❌ [VistaCanal]: Error al iniciar grabación: " + e.getMessage());
        }
    }

    private void detenerGrabacionAudio() {
        if (grabadorAudio != null) {
            File audioFile = grabadorAudio.detenerGrabacion();

            Platform.runLater(() -> {
                btnGrabarAudio.setDisable(true);
                btnEnviar.setDisable(true);
                btnCancelarGrabacion.setVisible(false);
                lblEstadoGrabacion.setText("⏳ Subiendo audio al servidor...");
                lblEstadoGrabacion.setTextFill(Color.DARKBLUE);
            });

            // Primero subir el archivo al servidor
            gestionArchivos.subirArchivo(audioFile)
                .thenCompose(fileId -> {
                    System.out.println("✅ [VistaCanal]: Audio subido con ID: " + fileId);
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("📤 Enviando mensaje de audio al canal...");
                    });

                    // Luego enviar el mensaje con el ID del archivo
                    return controlador.enviarMensajeAudio(canal.getId(), fileId);
                })
                .thenRun(() -> {
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("✅ Mensaje de audio enviado al canal");
                        lblEstadoGrabacion.setTextFill(Color.GREEN);
                        resetearEstadoGrabacion();

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

                    System.out.println("✅ [VistaCanal]: Audio enviado exitosamente");
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("❌ Error al enviar audio: " + ex.getMessage());
                        lblEstadoGrabacion.setTextFill(Color.RED);
                        resetearEstadoGrabacion();
                    });
                    System.err.println("❌ [VistaCanal]: Error al enviar audio: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
        }
    }

    private void cancelarGrabacionAudio() {
        if (grabadorAudio != null) {
            grabadorAudio.cancelarGrabacion();

            Platform.runLater(() -> {
                lblEstadoGrabacion.setText("❌ Grabación cancelada");
                lblEstadoGrabacion.setTextFill(Color.ORANGE);
                resetearEstadoGrabacion();

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

            System.out.println("❌ [VistaCanal]: Grabación cancelada por el usuario");
        }
    }

    private void resetearEstadoGrabacion() {
        isRecording = false;
        btnGrabarAudio.setText("🎤");
        btnGrabarAudio.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 5 10;");
        btnGrabarAudio.setDisable(false);
        btnEnviar.setDisable(false);
        btnCancelarGrabacion.setVisible(false);
        campoMensaje.setDisable(false);
        btnArchivo.setDisable(false);
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
            System.out.println("📎 [VistaCanal]: Enviando archivo al canal...");
            System.out.println("   → Archivo: " + archivo.getName());

            btnArchivo.setDisable(true);
            lblEstadoGrabacion.setText("⏳ Subiendo archivo: " + archivo.getName() + "...");
            lblEstadoGrabacion.setTextFill(Color.DARKBLUE);

            // Primero subir el archivo al servidor
            gestionArchivos.subirArchivo(archivo)
                .thenCompose(fileId -> {
                    System.out.println("✅ [VistaCanal]: Archivo subido con ID: " + fileId);
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
                    System.out.println("✅ [VistaCanal]: Archivo enviado exitosamente");
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        lblEstadoGrabacion.setText("❌ Error al enviar archivo: " + ex.getMessage());
                        lblEstadoGrabacion.setTextFill(Color.RED);
                        btnArchivo.setDisable(false);
                    });
                    System.err.println("❌ [VistaCanal]: Error al enviar archivo: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
        }
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("📥 [VistaCanal]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "HISTORIAL_CANAL_RECIBIDO":
                    if (datos instanceof List) {
                        List<?> lista = (List<?>) datos;
                        System.out.println("📜 [VistaCanal]: Historial recibido - Total mensajes: " + lista.size());
                        cargarHistorial((List<DTOMensajeCanal>) datos);
                    }
                    break;

                case "MENSAJE_CANAL_RECIBIDO":
                case "NUEVO_MENSAJE_CANAL":
                    if (datos instanceof DTOMensajeCanal) {
                        DTOMensajeCanal mensaje = (DTOMensajeCanal) datos;
                        if (mensaje.getCanalId().equals(canal.getId())) {
                            System.out.println("💬 [VistaCanal]: Nuevo mensaje recibido");
                            System.out.println("   → De: " + mensaje.getNombreRemitente());
                            System.out.println("   → Tipo: " + mensaje.getTipo());
                            agregarMensaje(mensaje);
                        }
                    }
                    break;

                case "ERROR_OPERACION":
                case "ERROR_ENVIO_MENSAJE":
                    String error = datos != null ? datos.toString() : "Error desconocido";
                    System.err.println("❌ [VistaCanal]: Error: " + error);
                    mostrarError(error);
                    break;

                default:
                    System.out.println("⚠️ [VistaCanal]: Tipo de notificación no manejado: " + tipoDeDato);
            }
        });
    }

    private void cargarHistorial(List<DTOMensajeCanal> mensajes) {
        mensajesBox.getChildren().clear();
        mensajesMostrados.clear();

        if (mensajes.isEmpty()) {
            Label sinMensajes = new Label("📭 No hay mensajes en este canal. ¡Sé el primero en escribir!");
            sinMensajes.setTextFill(Color.GRAY);
            sinMensajes.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            mensajesBox.getChildren().add(sinMensajes);
            System.out.println("📭 [VistaCanal]: No hay mensajes en el historial");
        } else {
            for (DTOMensajeCanal mensaje : mensajes) {
                agregarMensaje(mensaje);
            }
            System.out.println("✅ [VistaCanal]: Historial cargado en la vista");
        }
    }

    private void agregarMensaje(DTOMensajeCanal mensaje) {
        // Validación para evitar burbujas vacías o duplicadas
        String id = mensaje.getMensajeId();
        if (id != null && !id.isEmpty() && mensajesMostrados.contains(id)) {
            System.out.println("⚠️ [VistaCanal]: Mensaje ya mostrado, ignorando ID: " + id);
            return;
        }

        boolean hasText = mensaje.getContenido() != null && !mensaje.getContenido().trim().isEmpty();
        boolean hasFile = mensaje.getFileId() != null && !mensaje.getFileId().isEmpty();

        if (!hasText && !hasFile) {
            System.out.println("⚠️ [VistaCanal]: Mensaje vacío, no se mostrará");
            return;
        }

        // Mensajes propios a la IZQUIERDA (verde), otros a la DERECHA (blanco)
        Pos alineacion = mensaje.isEsPropio() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT;

        System.out.println("🔍 [VistaCanal]: Agregando mensaje:");
        System.out.println("   → Tipo: " + mensaje.getTipo());
        System.out.println("   → esPropio: " + mensaje.isEsPropio());
        System.out.println("   → Alineación: " + (mensaje.isEsPropio() ? "IZQUIERDA (propio)" : "DERECHA (otros)"));

        VBox burbuja = crearBurbujaMensaje(mensaje, alineacion);
        mensajesBox.getChildren().add(burbuja);

        if (id != null && !id.isEmpty()) {
            mensajesMostrados.add(id);
        }

        System.out.println("✅ [VistaCanal]: Mensaje agregado a la vista");
    }

    private VBox crearBurbujaMensaje(DTOMensajeCanal mensaje, Pos alineacion) {
        VBox burbuja = new VBox(5);
        burbuja.setPadding(new Insets(8));
        burbuja.setMaxWidth(400);

        String nombreAutor = mensaje.isEsPropio() ? "Tú" : mensaje.getNombreRemitente();
        String hora = mensaje.getFechaEnvio() != null ? mensaje.getFechaEnvio().format(FORMATTER) : "";

        Label autorLabel = new Label(nombreAutor + " - " + hora);
        autorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        autorLabel.setTextFill(mensaje.isEsPropio() ? Color.DARKGREEN : Color.DARKBLUE);

        // Estilo según si es propio o no
        if (mensaje.isEsPropio()) {
            burbuja.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 10;");
        } else {
            burbuja.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");
        }

        // ✅ FIX: Usar comparación case-insensitive para tipo de mensaje (servidor envía "AUDIO"/"TEXT")
        // Mostrar contenido según el tipo de mensaje
        if ("AUDIO".equalsIgnoreCase(mensaje.getTipo())) {
            HBox audioBox = new HBox(10);
            audioBox.setAlignment(Pos.CENTER_LEFT);

            Button btnPlay = new Button("▶️");
            btnPlay.setStyle("-fx-font-size: 16px;");
            btnPlay.setOnAction(e -> {
                System.out.println("🎵 [VistaCanal]: Reproducir audio - FileId: " + mensaje.getFileId());
                // TODO: Implementar reproducción de audio
                btnPlay.setText("⏳");
                btnPlay.setDisable(true);

                // Simulación - en producción usar controlador.reproducirAudio()
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        btnPlay.setText("✅");
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            Platform.runLater(() -> {
                                btnPlay.setText("▶️");
                                btnPlay.setDisable(false);
                            });
                        }).start();
                    });
                }).start();
            });

            Label audioLabel = new Label("🎤 Mensaje de audio");
            audioLabel.setStyle("-fx-font-size: 12px; -fx-font-style: italic;");

            audioBox.getChildren().addAll(btnPlay, audioLabel);
            burbuja.getChildren().addAll(autorLabel, audioBox);
        } else if ("ARCHIVO".equalsIgnoreCase(mensaje.getTipo()) || mensaje.getFileId() != null) {
            Button btnDescargar = new Button("📎 Descargar archivo");
            btnDescargar.setStyle("-fx-font-size: 12px;");
            btnDescargar.setOnAction(e -> {
                System.out.println("📥 [VistaCanal]: Descargar archivo - FileId: " + mensaje.getFileId());
                // TODO: Implementar descarga
            });
            burbuja.getChildren().addAll(autorLabel, btnDescargar);

            if (mensaje.getContenido() != null && !mensaje.getContenido().isEmpty()) {
                Text contenidoText = new Text(mensaje.getContenido());
                contenidoText.setWrappingWidth(380);
                burbuja.getChildren().add(contenidoText);
            }
        } else if (mensaje.getContenido() != null && !mensaje.getContenido().isEmpty()) {
            Text contenidoText = new Text(mensaje.getContenido());
            contenidoText.setWrappingWidth(380);
            burbuja.getChildren().addAll(autorLabel, contenidoText);
        }

        HBox wrapper = new HBox(burbuja);
        wrapper.setAlignment(alineacion);
        return new VBox(wrapper);
    }

    private void mostrarError(String mensaje) {
        Label errorLabel = new Label("❌ Error: " + mensaje);
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        mensajesBox.getChildren().add(errorLabel);
        System.err.println("❌ [VistaCanal]: " + mensaje);
    }
}
