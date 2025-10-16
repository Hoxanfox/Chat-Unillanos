package interfazEscritorio.dashboard.featureCanales.crearCanal;

import controlador.canales.IControladorCanales;
import dto.canales.DTOCanalCreado;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import observador.IObservador;

import java.util.concurrent.CompletableFuture;

/**
 * Representa el formulario para crear un nuevo canal.
 * Implementa IObservador para recibir notificaciones sobre la creación del canal.
 */
public class VistaCrearCanal extends BorderPane implements IObservador {

    private final IControladorCanales controlador;
    private final Label etiquetaEstado;
    private final Button btnCrear;
    private final Runnable onVolver;

    /**
     * @param onVolver    Acción para cancelar y regresar a la vista principal.
     * @param controlador Controlador de canales.
     */
    public VistaCrearCanal(Runnable onVolver, IControladorCanales controlador) {
        this.controlador = controlador;
        this.onVolver = onVolver;
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #ecf0f1;");

        // Registrarse como observador
        controlador.registrarObservadorCreacion(this);
        System.out.println("✅ [VistaCrearCanal]: Registrada como observador de creación");

        // --- Header ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        Label titulo = new Label("Crear un Nuevo Canal");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        this.setTop(titulo);

        // --- Formulario (Centro) ---
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER_LEFT);

        Label labelNombre = new Label("Nombre del Canal:");
        TextField campoNombre = new TextField();
        campoNombre.setPromptText("Ej: marketing-global");

        Label labelDescripcion = new Label("Descripción (opcional):");
        TextArea campoDescripcion = new TextArea();
        campoDescripcion.setPromptText("¿De qué trata este canal?");
        campoDescripcion.setPrefHeight(100);

        etiquetaEstado = new Label();
        etiquetaEstado.setTextFill(Color.RED);

        form.getChildren().addAll(labelNombre, campoNombre, labelDescripcion, campoDescripcion, etiquetaEstado);

        // --- Botones (Abajo) ---
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> onVolver.run());

        btnCrear = new Button("Crear Canal");
        btnCrear.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");

        btnCrear.setOnAction(e -> {
            String nombre = campoNombre.getText().trim();
            String descripcion = campoDescripcion.getText().trim();

            if (nombre.isEmpty()) {
                etiquetaEstado.setTextFill(Color.RED);
                etiquetaEstado.setText("El nombre del canal es obligatorio.");
                return;
            }

            etiquetaEstado.setText("");
            btnCrear.setDisable(true);

            System.out.println("📤 [VistaCrearCanal]: Creando canal: " + nombre);
            CompletableFuture<DTOCanalCreado> futuroCanal = controlador.crearCanal(nombre, descripcion);

            futuroCanal.thenAccept(canal -> {
                Platform.runLater(() -> {
                    System.out.println("✅ [VistaCrearCanal]: Canal creado exitosamente");
                    // La notificación del observador manejará la navegación
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    etiquetaEstado.setTextFill(Color.RED);
                    etiquetaEstado.setText("Error al crear el canal: " + ex.getMessage());
                    btnCrear.setDisable(false);
                });
                return null;
            });
        });

        botones.getChildren().addAll(btnCancelar, btnCrear);
        form.getChildren().add(botones);

        this.setCenter(form);
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [VistaCrearCanal]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "CANAL_CREACION_INICIADA":
                    System.out.println("🔄 [VistaCrearCanal]: Creación iniciada...");
                    etiquetaEstado.setTextFill(Color.BLUE);
                    etiquetaEstado.setText("Creando canal...");
                    break;

                case "CANAL_CREADO_EXITOSAMENTE":
                    if (datos instanceof DTOCanalCreado) {
                        DTOCanalCreado canal = (DTOCanalCreado) datos;
                        System.out.println("✅ [VistaCrearCanal]: Canal creado: " + canal.getNombre());
                        etiquetaEstado.setTextFill(Color.GREEN);
                        etiquetaEstado.setText("¡Canal creado exitosamente!");

                        // Volver a la vista principal después de 1 segundo
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                Platform.runLater(() -> {
                                    if (onVolver != null) {
                                        onVolver.run();
                                    }
                                });
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                    }
                    break;

                case "CANAL_ERROR":
                    System.out.println("❌ [VistaCrearCanal]: Error al crear canal");
                    etiquetaEstado.setTextFill(Color.RED);
                    etiquetaEstado.setText("Error: " + datos);
                    btnCrear.setDisable(false);
                    break;

                default:
                    System.out.println("⚠️ [VistaCrearCanal]: Tipo de notificación desconocido: " + tipoDeDato);
            }
        });
    }
}
