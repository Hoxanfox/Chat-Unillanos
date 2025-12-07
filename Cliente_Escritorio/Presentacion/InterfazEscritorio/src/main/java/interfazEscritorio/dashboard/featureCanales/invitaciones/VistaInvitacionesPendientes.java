package interfazEscritorio.dashboard.featureCanales.invitaciones;

import controlador.canales.IControladorCanales;
import dto.canales.DTOCanalCreado;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import observador.IObservador;

import java.util.*;

/**
 * Vista para gestionar invitaciones pendientes a canales.
 * Muestra las invitaciones recibidas y permite aceptarlas o rechazarlas.
 */
public class VistaInvitacionesPendientes extends BorderPane implements IObservador {

    private final IControladorCanales controladorCanales;
    private final VBox invitacionesBox;
    private final Label lblEstado;
    private final Badge badgeInvitaciones;

    public VistaInvitacionesPendientes(Runnable onVolver, IControladorCanales controladorCanales) {
        this.controladorCanales = controladorCanales;
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f4f4f4;");

        System.out.println("🔔 [VistaInvitacionesPendientes]: Constructor - Registrándose como observador");

        // Registrarse como observador
        controladorCanales.registrarObservadorInvitaciones(this);

        System.out.println("✅ [VistaInvitacionesPendientes]: Registro completado");

        VBox mainLayout = new VBox(15);
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // Header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("📨 Pending Invitations");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        badgeInvitaciones = new Badge();
        badgeInvitaciones.setVisible(false);

        headerBox.getChildren().addAll(titulo, badgeInvitaciones);
        mainLayout.getChildren().add(headerBox);

        Label instrucciones = new Label("You have been invited to join the following channels:");
        instrucciones.setTextFill(Color.GRAY);
        instrucciones.setFont(Font.font("Arial", 12));
        mainLayout.getChildren().add(instrucciones);

        invitacionesBox = new VBox(10);
        invitacionesBox.setPadding(new Insets(10));
        invitacionesBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-radius: 5;");

        ScrollPane scrollPane = new ScrollPane(invitacionesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        mainLayout.getChildren().add(scrollPane);

        lblEstado = new Label();
        lblEstado.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        mainLayout.getChildren().add(lblEstado);

        this.setCenter(mainLayout);

        // Botones
        HBox botonesBox = new HBox(10);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);
        botonesBox.setPadding(new Insets(15, 0, 0, 0));

        Button btnVolver = new Button("← Volver");
        btnVolver.setOnAction(e -> onVolver.run());

        Button btnRefrescar = new Button("🔄 Refresh");
        btnRefrescar.setOnAction(e -> cargarInvitaciones());

        botonesBox.getChildren().addAll(btnRefrescar, btnVolver);
        this.setBottom(botonesBox);

        cargarInvitaciones();
    }

    private void cargarInvitaciones() {
        System.out.println("[Invitaciones] Iniciando carga de invitaciones...");
        lblEstado.setText("⏳ Cargando invitaciones...");
        lblEstado.setTextFill(Color.BLUE);

        controladorCanales.solicitarInvitacionesPendientes()
                .thenAccept(invitaciones -> Platform.runLater(() -> {
                    System.out.println("[Invitaciones] Invitaciones cargadas exitosamente. Total: " + (invitaciones != null ? invitaciones.size() : 0));
                    mostrarInvitaciones(invitaciones);
                    actualizarBadge(invitaciones.size());
                    lblEstado.setText("");
                }))
                .exceptionally(ex -> {
                    System.out.println("[Invitaciones] Error al cargar invitaciones: " + ex.getMessage());
                    Platform.runLater(() -> {
                        lblEstado.setText("❌ Error: " + ex.getMessage());
                        lblEstado.setTextFill(Color.RED);
                    });
                    return null;
                });
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [VistaInvitacionesPendientes]: Notificación recibida - Tipo: " + tipoDeDato + ", Datos: " + (datos != null ? datos.toString() : "null"));
        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "INVITACIONES_PENDIENTES":
                    if (datos instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<DTOCanalCreado> invitaciones = (List<DTOCanalCreado>) datos;
                        System.out.println("✅ [VistaInvitacionesPendientes]: Actualizando UI con " + invitaciones.size() + " invitaciones");
                        mostrarInvitaciones(invitaciones);
                        actualizarBadge(invitaciones.size());
                        lblEstado.setText("");
                    }
                    break;
                case "NUEVA_INVITACION_CANAL":
                    if (datos instanceof Map) {
                        System.out.println("🔔 [VistaInvitacionesPendientes]: Nueva invitación recibida, refrescando...");
                        cargarInvitaciones();
                    }
                    break;
                case "INVITACION_ACEPTADA":
                case "INVITACION_RECHAZADA":
                    System.out.println("✅ [VistaInvitacionesPendientes]: Invitación procesada, refrescando...");
                    cargarInvitaciones();
                    break;
            }
        });
    }

    private void mostrarInvitaciones(List<DTOCanalCreado> invitaciones) {
        System.out.println("[Invitaciones] Mostrando " + (invitaciones != null ? invitaciones.size() : 0) + " invitaciones en la UI");
        invitacionesBox.getChildren().clear();

        if (invitaciones == null || invitaciones.isEmpty()) {
            System.out.println("[Invitaciones] No hay invitaciones pendientes para mostrar");
            Label mensaje = new Label("📭 No hay invitaciones pendientes");
            mensaje.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            mensaje.setTextFill(Color.GRAY);
            invitacionesBox.getChildren().add(mensaje);
            return;
        }

        for (DTOCanalCreado invitacion : invitaciones) {
            System.out.println("[Invitaciones] Invitación: CanalID=" + invitacion.getId() + ", Nombre=" + invitacion.getNombre());
            invitacionesBox.getChildren().add(crearTarjetaInvitacion(invitacion));
        }
    }

    private VBox crearTarjetaInvitacion(DTOCanalCreado canal) {
        System.out.println("[Invitaciones] Creando tarjeta para invitación: CanalID=" + canal.getId() + ", Nombre=" + canal.getNombre());
        VBox tarjeta = new VBox(10);
        tarjeta.setPadding(new Insets(15));
        tarjeta.setStyle("-fx-background-color: #ffffff; -fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 8;");

        Label nombreLabel = new Label(canal.getNombre());
        nombreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        HBox botonesBox = new HBox(10);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnAceptar = new Button("✓ Accept");
        btnAceptar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        btnAceptar.setOnAction(e -> responderInvitacion(canal.getId(), true));

        Button btnRechazar = new Button("✗ Decline");
        btnRechazar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnRechazar.setOnAction(e -> responderInvitacion(canal.getId(), false));

        botonesBox.getChildren().addAll(btnRechazar, btnAceptar);
        tarjeta.getChildren().addAll(nombreLabel, botonesBox);

        return tarjeta;
    }

    private void responderInvitacion(String canalId, boolean aceptar) {
        System.out.println("[Invitaciones] Respondiendo invitación: CanalID=" + canalId + ", Acción=" + (aceptar ? "ACEPTAR" : "RECHAZAR"));
        controladorCanales.responderInvitacion(canalId, aceptar)
                .thenRun(() -> Platform.runLater(() -> {
                    System.out.println("[Invitaciones] Invitación " + (aceptar ? "aceptada" : "rechazada") + " correctamente para CanalID=" + canalId);
                    lblEstado.setText(aceptar ? "✅ Invitación aceptada" : "❌ Invitación rechazada");
                    lblEstado.setTextFill(aceptar ? Color.GREEN : Color.ORANGE);
                    cargarInvitaciones();
                }))
                .exceptionally(ex -> {
                    System.out.println("[Invitaciones] Error al responder invitación para CanalID=" + canalId + ": " + ex.getMessage());
                    Platform.runLater(() -> {
                        lblEstado.setText("❌ Error: " + ex.getMessage());
                        lblEstado.setTextFill(Color.RED);
                    });
                    return null;
                });
    }

    private void actualizarBadge(int cantidad) {
        if (cantidad > 0) {
            badgeInvitaciones.setCount(cantidad);
            badgeInvitaciones.setVisible(true);
        } else {
            badgeInvitaciones.setVisible(false);
        }
    }

    private static class Badge extends StackPane {
        private final Label label;

        public Badge() {
            label = new Label();
            label.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            label.setTextFill(Color.WHITE);
            this.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 10;");
            this.setMinSize(20, 20);
            this.getChildren().add(label);
        }

        public void setCount(int count) {
            label.setText(String.valueOf(count));
        }
    }
}
