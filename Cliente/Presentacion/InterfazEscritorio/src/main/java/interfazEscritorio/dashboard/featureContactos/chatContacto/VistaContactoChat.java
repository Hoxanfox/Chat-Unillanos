// java
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
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

/**
 * Vista de chat privado que AHORA gestiona el estado de grabación de audio.
 * Ajustes: mensajes del usuario actual a la izquierda, del contacto a la derecha.
 * Evita burbujas vacías o duplicadas.
 */
public class VistaContactoChat extends BorderPane implements IObservador {

    private final IControladorChat controlador;
    private final DTOContacto contacto;
    private final Runnable onVolver;
    private final VBox mensajesBox;
    private boolean isRecording = false; // Estado para saber si se está grabando

    // Evitar mensajes duplicados
    private final Set<String> mensajesMostrados = Collections.synchronizedSet(new HashSet<>());

    public VistaContactoChat(DTOContacto contacto, IControladorChat controlador, Runnable onVolver) {
        System.out.println("🔧 [VistaContactoChat]: Inicializando vista de chat...");
        System.out.println("   → Contacto: " + contacto.getNombre() + " (ID: " + contacto.getId() + ")");

        this.contacto = contacto;
        this.controlador = controlador;
        this.onVolver = onVolver;

        // 1. Suscribirse para recibir nuevos mensajes
        System.out.println("🔔 [VistaContactoChat]: Registrándose como observador del controlador...");
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
        System.out.println("📡 [VistaContactoChat]: Solicitando historial de mensajes al controlador...");
        this.controlador.solicitarHistorial(contacto.getId());
        System.out.println("✅ [VistaContactoChat]: Vista inicializada correctamente");
    }

    private Node crearHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);
        header.setPadding(new Insets(0, 0, 10, 0));
        Label tituloChat = new Label("Private Chat: " + contacto.getNombre());
        tituloChat.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        Button btnVolver = new Button("← Volver");
        btnVolver.setOnAction(e -> {
            System.out.println("🔙 [VistaContactoChat]: Regresando a la lista de contactos");
            onVolver.run();
        });
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
                System.out.println("🎤 [VistaContactoChat]: Cancelando grabación...");
                controlador.cancelarGrabacion();
                isRecording = false;
                btnAudio.setText("🎤");
                campoMensaje.setDisable(false);
                System.out.println("🎤 [VistaContactoChat]: Grabación cancelada");
            } else {
                // Si no se está grabando, inicia la grabación
                System.out.println("🔴 [VistaContactoChat]: Iniciando grabación...");
                controlador.iniciarGrabacionAudio();
                isRecording = true;
                btnAudio.setText("❌"); // Cambia a un ícono de "cancelar"
                campoMensaje.setDisable(true); // Deshabilita el texto mientras se graba
                System.out.println("🔴 [VistaContactoChat]: Modo grabación activado");
            }
        });

        // Lógica del botón de Enviar
        btnEnviar.setOnAction(e -> {
            if (isRecording) {
                // Si se está grabando, "Send" detiene y envía el audio
                System.out.println("➡️ [VistaContactoChat]: Deteniendo y enviando grabación de audio...");
                controlador.detenerYEnviarGrabacion(contacto.getId());
                isRecording = false;
                btnAudio.setText("🎤");
                campoMensaje.setDisable(false);
            } else {
                // Si no se está grabando, envía el mensaje de texto
                String texto = campoMensaje.getText();
                if (texto != null && !texto.trim().isEmpty()) {
                    System.out.println("➡️ [VistaContactoChat]: Enviando mensaje de texto...");
                    System.out.println("   → Destinatario: " + contacto.getId());
                    System.out.println("   → Contenido: " + texto);
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
        System.out.println("📥 [VistaContactoChat]: Notificación recibida - Tipo: " + tipoDeDato);

        switch (tipoDeDato) {
            case "REFRESCAR_MENSAJES":
                // Señal de actualización global - refrescar el historial
                System.out.println("🔄 [VistaContactoChat]: Refrescando mensajes por SIGNAL_UPDATE");
                Platform.runLater(() -> {
                    controlador.solicitarHistorial(contacto.getId());
                });
                break;

            case "NUEVO_MENSAJE_PRIVADO":
                // Mensaje recibido de otro usuario (PUSH del servidor)
                if (datos instanceof DTOMensaje) {
                    DTOMensaje mensaje = (DTOMensaje) datos;

                    // Validación null-safe para prevenir NullPointerException
                    if (mensaje.getRemitenteId() == null) {
                        System.err.println("⚠️ [VistaContactoChat]: Mensaje recibido con remitenteId null, ignorando...");
                        break;
                    }

                    // Solo mostrar si es de nuestro contacto actual o si somos nosotros
                    if (mensaje.getRemitenteId().equals(contacto.getId()) || mensaje.esMio()) {
                        System.out.println("💬 [VistaContactoChat]: Nuevo mensaje recibido");
                        System.out.println("   → De: " + mensaje.getRemitenteNombre());
                        System.out.println("   → Tipo: " + mensaje.getTipo());
                        System.out.println("   → Contenido: " + mensaje.getContenido());

                        // ✅ NUEVO: Si es un mensaje de audio, detectar si viene como Base64 o fileId
                        if (mensaje.esAudio() && mensaje.getContenido() != null && !mensaje.getContenido().isEmpty()) {
                            String contenido = mensaje.getContenido();

                            // Detectar si es Base64 de audio WAV (empieza con "UklGR" = RIFF header)
                            boolean esBase64Audio = contenido.startsWith("UklGR") ||
                                                   contenido.startsWith("data:audio/") ||
                                                   contenido.length() > 1000; // Los fileId son cortos, Base64 es largo

                            if (esBase64Audio) {
                                System.out.println("🎵 [VistaContactoChat]: Audio recibido en Base64, guardando localmente...");
                                controlador.guardarAudioDesdeBase64(contenido, mensaje.getMensajeId())
                                        .thenAccept(archivo -> {
                                            if (archivo != null) {
                                                System.out.println("✅ [VistaContactoChat]: Audio guardado en caché: " + archivo.getAbsolutePath());
                                                // Actualizar el mensaje para usar la ruta local en lugar del Base64
                                                mensaje.setContenido(archivo.getAbsolutePath());
                                            }
                                        })
                                        .exceptionally(ex -> {
                                            System.err.println("❌ [VistaContactoChat]: Error al guardar audio: " + ex.getMessage());
                                            return null;
                                        });
                            } else {
                                // Es un fileId, descargar normalmente
                                System.out.println("📥 [VistaContactoChat]: Descargando audio desde servidor - FileId: " + contenido);
                                controlador.descargarAudioALocal(contenido)
                                        .thenAccept(archivo -> {
                                            if (archivo != null) {
                                                System.out.println("✅ [VistaContactoChat]: Audio descargado a caché: " + archivo.getAbsolutePath());
                                            }
                                        })
                                        .exceptionally(ex -> {
                                            System.err.println("❌ [VistaContactoChat]: Error al descargar audio: " + ex.getMessage());
                                            return null;
                                        });
                            }
                        }

                        // Ejecutar en UI thread y dejar que agregarMensaje maneje duplicados/vacíos
                        Platform.runLater(() -> agregarMensaje(mensaje));
                    } else {
                        System.out.println("⚠️ [VistaContactoChat]: Mensaje ignorado (no es del contacto actual)");
                    }
                }
                break;

            case "NUEVO_MENSAJE_AUDIO_PRIVADO":
                // ✅ NUEVO: Mensaje de audio PUSH (ya procesado por ServicioChat)
                if (datos instanceof DTOMensaje) {
                    DTOMensaje mensaje = (DTOMensaje) datos;

                    // Validación null-safe
                    if (mensaje.getRemitenteId() == null) {
                        System.err.println("⚠️ [VistaContactoChat]: Audio PUSH con remitenteId null, ignorando...");
                        break;
                    }

                    // Solo mostrar si es de nuestro contacto actual o si somos nosotros
                    if (mensaje.getRemitenteId().equals(contacto.getId()) || mensaje.esMio()) {
                        System.out.println("🎵 [VistaContactoChat]: Nuevo audio PUSH recibido");
                        System.out.println("   → De: " + mensaje.getRemitenteNombre());
                        System.out.println("   → FileId: " + mensaje.getFileId());

                        // El ServicioChat ya procesó el Base64 y guardó el archivo
                        // Solo necesitamos agregar el mensaje a la vista
                        Platform.runLater(() -> agregarMensaje(mensaje));
                    } else {
                        System.out.println("⚠️ [VistaContactoChat]: Audio PUSH ignorado (no es del contacto actual)");
                    }
                }
                break;

            case "MENSAJE_ENVIADO_EXITOSO":
            case "MENSAJE_AUDIO_ENVIADO_EXITOSO":
                // Confirmación de que nuestro mensaje fue enviado
                if (datos instanceof DTOMensaje) {
                    DTOMensaje mensaje = (DTOMensaje) datos;
                    System.out.println("✅ [VistaContactoChat]: Mensaje enviado exitosamente");
                    System.out.println("   → ID: " + mensaje.getMensajeId());
                    System.out.println("   → Tipo: " + mensaje.getTipo());

                    // ✅ IMPORTANTE: Verificar que sea para este contacto
                    if (mensaje.getDestinatarioId() != null && mensaje.getDestinatarioId().equals(contacto.getId())) {
                        // ✅ NUEVO: Si es un mensaje de audio que YO envié, descargar a caché local
                        if (mensaje.esAudio() && mensaje.getContenido() != null && !mensaje.getContenido().isEmpty()) {
                            String fileId = mensaje.getContenido();
                            System.out.println("📥 [VistaContactoChat]: Descargando mi audio enviado a caché - FileId: " + fileId);
                            controlador.descargarAudioALocal(fileId)
                                    .thenAccept(archivo -> {
                                        if (archivo != null) {
                                            System.out.println("✅ [VistaContactoChat]: Mi audio descargado a caché: " + archivo.getAbsolutePath());
                                        }
                                    })
                                    .exceptionally(ex -> {
                                        System.err.println("❌ [VistaContactoChat]: Error al descargar mi audio: " + ex.getMessage());
                                        return null;
                                    });
                        }

                        // Agregar en UI (agregarMensaje ignorará duplicados/vacíos)
                        Platform.runLater(() -> agregarMensaje(mensaje));
                    } else {
                        System.out.println("⚠️ [VistaContactoChat]: Mensaje enviado ignorado (no es para este chat)");
                    }
                }
                break;

            case "HISTORIAL_MENSAJES_RECIBIDO":
            case "HISTORIAL_MENSAJES":
                // Historial completo recibido
                if (datos instanceof List) {
                    List<?> lista = (List<?>) datos;
                    System.out.println("📜 [VistaContactoChat]: Historial recibido - Total mensajes: " + lista.size());
                    Platform.runLater(() -> {
                        mensajesBox.getChildren().clear();
                        mensajesMostrados.clear();

                        // ✅ NUEVO: Descargar todos los audios del historial a caché local
                        for (Object obj : lista) {
                            if (obj instanceof DTOMensaje) {
                                DTOMensaje mensaje = (DTOMensaje) obj;
                                agregarMensaje(mensaje);

                                // ✅ CORRECCIÓN: Si es audio, descargar usando el FILEID (no el contenido)
                                if (mensaje.esAudio() && mensaje.getFileId() != null && !mensaje.getFileId().isEmpty()) {
                                    String fileId = mensaje.getFileId();
                                    System.out.println("📥 [VistaContactoChat]: Descargando audio del historial - FileId: " + fileId);
                                    controlador.descargarAudioALocal(fileId)
                                            .thenAccept(archivo -> {
                                                if (archivo != null) {
                                                    System.out.println("✅ [VistaContactoChat]: Audio del historial descargado: " + archivo.getName());
                                                }
                                            })
                                            .exceptionally(ex -> {
                                                System.err.println("⚠️ [VistaContactoChat]: Error al descargar audio del historial: " + ex.getMessage());
                                                return null;
                                            });
                                }
                            }
                        }
                        System.out.println("✅ [VistaContactoChat]: Historial cargado en la vista");
                    });
                }
                break;

            case "ERROR_ENVIO_MENSAJE":
            case "ERROR_ENVIO_MENSAJE_AUDIO": // ✅ Agregado para errores de audio
                // Error al enviar mensaje
                String error = datos != null ? datos.toString() : "Error desconocido";
                System.err.println("❌ [VistaContactoChat]: Error al enviar mensaje: " + error);
                Platform.runLater(() -> {
                    // TODO: Mostrar notificación de error en la UI
                    System.err.println("❌ UI: Mostrar error al usuario: " + error);
                });
                break;

            case "ERROR_HISTORIAL":
                // Error al obtener historial
                String errorHist = datos != null ? datos.toString() : "Error desconocido";
                System.err.println("❌ [VistaContactoChat]: Error al obtener historial: " + errorHist);
                Platform.runLater(() -> {
                    // TODO: Mostrar notificación de error en la UI
                    System.err.println("❌ UI: Mostrar error al usuario: " + errorHist);
                });
                break;

            default:
                System.out.println("⚠️ [VistaContactoChat]: Tipo de notificación no manejado: " + tipoDeDato);
        }
    }

    private void agregarMensaje(DTOMensaje mensaje) {
        // Validaciones para evitar burbujas vacías o duplicadas
        String id = mensaje.getMensajeId();
        if (id != null && !id.isEmpty() && mensajesMostrados.contains(id)) {
            System.out.println("⚠️ [VistaContactoChat]: Mensaje ya mostrado, ignorando ID: " + id);
            return;
        }

        boolean hasText = mensaje.getContenido() != null && !mensaje.getContenido().trim().isEmpty();
        boolean hasFile = mensaje.getFileId() != null && !mensaje.getFileId().isEmpty();

        if (!hasText && !hasFile) {
            System.out.println("⚠️ [VistaContactoChat]: Mensaje vacío, no se mostrará");
            return;
        }

        // ✅ CORRECTO: Mensajes del usuario a la DERECHA, mensajes del contacto a la IZQUIERDA
        Pos alineacion = mensaje.esMio() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;

        // 🔍 DEBUG: Ver todos los datos del mensaje
        System.out.println("🔍 [VistaContactoChat]: Analizando mensaje para mostrar:");
        System.out.println("   → Tipo: " + mensaje.getTipo());
        System.out.println("   → esMio: " + mensaje.esMio());
        System.out.println("   → Alineación: " + (mensaje.esMio() ? "DERECHA (usuario)" : "IZQUIERDA (contacto)"));

        // Crear burbuja según el tipo de mensaje
        VBox burbuja;
        if (mensaje.esTexto()) {
            System.out.println("✅ [VistaContactoChat]: Mostrando como TEXTO");
            burbuja = crearBurbujaMensaje(mensaje, mensaje.getAutorConFecha(), mensaje.getContenido(), alineacion);
        } else if (mensaje.esAudio()) {
            System.out.println("🎵 [VistaContactoChat]: Mostrando como AUDIO");
            burbuja = crearBurbujaAudio(mensaje, alineacion);
        } else if (mensaje.esImagen()) {
            System.out.println("🖼️ [VistaContactoChat]: Mostrando como IMAGEN");
            burbuja = crearBurbujaImagen(mensaje, alineacion);
        } else if (mensaje.esArchivo()) {
            System.out.println("📎 [VistaContactoChat]: Mostrando como ARCHIVO");
            burbuja = crearBurbujaArchivo(mensaje, alineacion);
        } else {
            System.out.println("⚠️ [VistaContactoChat]: Tipo desconocido, mostrando como texto");
            // Tipo desconocido, mostrar como texto
            burbuja = crearBurbujaMensaje(mensaje, mensaje.getAutorConFecha(),
                    "[" + mensaje.getTipo() + "] " + mensaje.getContenido(), alineacion);
        }

        mensajesBox.getChildren().add(burbuja);

        if (id != null && !id.isEmpty()) {
            mensajesMostrados.add(id);
        }

        System.out.println("✅ [VistaContactoChat]: Mensaje agregado a la vista - " +
                (mensaje.esMio() ? "Enviado (izquierda)" : "Recibido (derecha)") + " - Tipo: " + mensaje.getTipo());
    }

    private VBox crearBurbujaMensaje(DTOMensaje mensaje, String autor, String contenido, Pos alineacion) {
        VBox burbuja = new VBox(3);
        burbuja.setPadding(new Insets(8));
        burbuja.setMaxWidth(300);
        Label autorLabel = new Label(autor);
        autorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        Text contenidoText = new Text(contenido);
        contenidoText.setWrappingWidth(280);
        // Estilo basado en si es mío (color verde) o no (blanco)
        if (mensaje.esMio()) {
            burbuja.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 10;");
        } else {
            burbuja.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");
        }
        burbuja.getChildren().addAll(autorLabel, contenidoText);
        HBox wrapper = new HBox(burbuja);
        wrapper.setAlignment(alineacion);
        return new VBox(wrapper);
    }

    /**
     * Crea una burbuja para mensajes de audio con botón de reproducción
     */
    private VBox crearBurbujaAudio(DTOMensaje mensaje, Pos alineacion) {
        VBox burbuja = new VBox(5);
        burbuja.setPadding(new Insets(8));
        burbuja.setMaxWidth(300);

        // Header con autor y fecha
        Label autorLabel = new Label(mensaje.getAutorConFecha());
        autorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        // Contenedor del audio
        HBox audioBox = new HBox(10);
        audioBox.setAlignment(Pos.CENTER_LEFT);

        Button btnPlay = new Button("▶️");
        btnPlay.setStyle("-fx-font-size: 16px;");
        btnPlay.setOnAction(e -> {
            System.out.println("🎵 [VistaContactoChat]: Reproducir audio EN MEMORIA - FileId: " + mensaje.getFileId());
            btnPlay.setDisable(true);
            btnPlay.setText("⏳");

            // Reproducir el audio EN MEMORIA a través del controlador
            controlador.reproducirAudioEnMemoria(mensaje.getFileId())
                    .thenRun(() -> {
                        Platform.runLater(() -> {
                            btnPlay.setText("✅");
                            System.out.println("✅ [VistaContactoChat]: Audio reproducido exitosamente");
                        });

                        // Re-habilitar el botón después de 2 segundos
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            Platform.runLater(() -> {
                                btnPlay.setDisable(false);
                                btnPlay.setText("▶️");
                            });
                        }).start();
                    })
                    .exceptionally(ex -> {
                        System.err.println("❌ [VistaContactoChat]: Error al reproducir audio: " + ex.getMessage());
                        Platform.runLater(() -> {
                            btnPlay.setText("❌");
                            btnPlay.setDisable(false);

                            // Restaurar después de 2 segundos
                            new Thread(() -> {
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e2) {
                                    e2.printStackTrace();
                                }
                                Platform.runLater(() -> btnPlay.setText("▶️"));
                            }).start();
                        });
                        return null;
                    });
        });

        Label audioLabel = new Label("🎤 Audio" + (mensaje.getFileName() != null ? " - " + mensaje.getFileName() : ""));
        audioLabel.setStyle("-fx-font-size: 12px;");

        audioBox.getChildren().addAll(btnPlay, audioLabel);

        // Estilo de la burbuja según propietario
        if (mensaje.esMio()) {
            burbuja.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 10;");
        } else {
            burbuja.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");
        }

        burbuja.getChildren().addAll(autorLabel, audioBox);
        HBox wrapper = new HBox(burbuja);
        wrapper.setAlignment(alineacion);
        return new VBox(wrapper);
    }

    /**
     * Crea una burbuja para mensajes con imagen
     */
    private VBox crearBurbujaImagen(DTOMensaje mensaje, Pos alineacion) {
        VBox burbuja = new VBox(5);
        burbuja.setPadding(new Insets(8));
        burbuja.setMaxWidth(300);

        // Header con autor y fecha
        Label autorLabel = new Label(mensaje.getAutorConFecha());
        autorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        // Placeholder de imagen
        Button btnVerImagen = new Button("🖼️ Ver imagen: " +
                (mensaje.getFileName() != null ? mensaje.getFileName() : "imagen.jpg"));
        btnVerImagen.setStyle("-fx-font-size: 12px;");
        btnVerImagen.setOnAction(e -> {
            System.out.println("🖼️ [VistaContactoChat]: Descargar/Ver imagen - FileId: " + mensaje.getFileId());
            // TODO: Implementar descarga y visualización de imagen
            // controlador.descargarYMostrarImagen(mensaje.getFileId(), mensaje.getFileName());
        });

        // Texto que acompaña la imagen (si existe)
        if (mensaje.getContenido() != null && !mensaje.getContenido().isEmpty()) {
            Text contenidoText = new Text(mensaje.getContenido());
            contenidoText.setWrappingWidth(280);
            burbuja.getChildren().addAll(autorLabel, btnVerImagen, contenidoText);
        } else {
            burbuja.getChildren().addAll(autorLabel, btnVerImagen);
        }

        // Estilo de la burbuja
        if (mensaje.esMio()) {
            burbuja.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 10;");
        } else {
            burbuja.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");
        }

        HBox wrapper = new HBox(burbuja);
        wrapper.setAlignment(alineacion);
        return new VBox(wrapper);
    }

    /**
     * Crea una burbuja para mensajes con archivo adjunto
     */
    private VBox crearBurbujaArchivo(DTOMensaje mensaje, Pos alineacion) {
        VBox burbuja = new VBox(5);
        burbuja.setPadding(new Insets(8));
        burbuja.setMaxWidth(300);

        // Header con autor y fecha
        Label autorLabel = new Label(mensaje.getAutorConFecha());
        autorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        // Botón de descarga
        Button btnDescargar = new Button("📎 Descargar: " +
                (mensaje.getFileName() != null ? mensaje.getFileName() : "archivo"));
        btnDescargar.setStyle("-fx-font-size: 12px;");
        btnDescargar.setOnAction(e -> {
            System.out.println("📥 [VistaContactoChat]: Descargar archivo - FileId: " + mensaje.getFileId());
            // TODO: Implementar descarga de archivo
            // controlador.descargarArchivo(mensaje.getFileId(), mensaje.getFileName());
        });

        // Texto que acompaña el archivo (si existe)
        if (mensaje.getContenido() != null && !mensaje.getContenido().isEmpty()) {
            Text contenidoText = new Text(mensaje.getContenido());
            contenidoText.setWrappingWidth(280);
            burbuja.getChildren().addAll(autorLabel, btnDescargar, contenidoText);
        } else {
            burbuja.getChildren().addAll(autorLabel, btnDescargar);
        }

        // Estilo de la burbuja
        if (mensaje.esMio()) {
            burbuja.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 10;");
        } else {
            burbuja.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");
        }

        HBox wrapper = new HBox(burbuja);
        wrapper.setAlignment(alineacion);
        return new VBox(wrapper);
    }
}
