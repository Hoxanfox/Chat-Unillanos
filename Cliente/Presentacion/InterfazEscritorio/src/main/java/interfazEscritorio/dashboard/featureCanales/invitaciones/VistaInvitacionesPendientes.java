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
 * Implementa IObservador para recibir notificaciones de nuevas invitaciones en tiempo real.
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

        // Registrarse como observador para recibir notificaciones de nuevas invitaciones
        controladorCanales.registrarObservadorInvitaciones(this);
        System.out.println("✅ [VistaInvitacionesPendientes]: Registrada como observador de invitaciones");

        VBox mainLayout = new VBox(15);
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // Header con título y badge
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label titulo = new Label("📨 Pending Invitations");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        
        badgeInvitaciones = new Badge();
        badgeInvitaciones.setVisible(false);
        
        headerBox.getChildren().addAll(titulo, badgeInvitaciones);
        mainLayout.getChildren().add(headerBox);

        // Instrucciones
        Label instrucciones = new Label("You have been invited to join the following channels:");
        instrucciones.setTextFill(Color.GRAY);
        instrucciones.setFont(Font.font("Arial", 12));
        mainLayout.getChildren().add(instrucciones);

        // Lista de invitaciones
        invitacionesBox = new VBox(10);
        invitacionesBox.setPadding(new Insets(10));
        invitacionesBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label cargando = new Label("⏳ Cargando invitaciones...");
        cargando.setTextFill(Color.GRAY);
        invitacionesBox.getChildren().add(cargando);

        ScrollPane scrollPane = new ScrollPane(invitacionesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        mainLayout.getChildren().add(scrollPane);

        // Estado
        lblEstado = new Label();
        lblEstado.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        mainLayout.getChildren().add(lblEstado);

        this.setCenter(mainLayout);

        // Botón volver
        HBox botonesBox = new HBox(10);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);
        botonesBox.setPadding(new Insets(15, 0, 0, 0));

        Button btnVolver = new Button("← Volver");
        btnVolver.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 8 15;");
        btnVolver.setOnAction(e -> onVolver.run());

        Button btnRefrescar = new Button("🔄 Refresh");
        btnRefrescar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8 15;");
        btnRefrescar.setOnAction(e -> cargarInvitaciones());

        botonesBox.getChildren().addAll(btnRefrescar, btnVolver);
        this.setBottom(botonesBox);

        // Solicitar invitaciones pendientes
        cargarInvitaciones();
    }

    private void cargarInvitaciones() {
        System.out.println("📡 [VistaInvitacionesPendientes]: Solicitando invitaciones pendientes...");
        lblEstado.setText("⏳ Cargando invitaciones...");
        lblEstado.setTextFill(Color.BLUE);

        controladorCanales.solicitarInvitacionesPendientes()
                .thenAccept(invitaciones -> {
                    Platform.runLater(() -> {
                        System.out.println("✅ [VistaInvitacionesPendientes]: Recibidas " + invitaciones.size() + " invitaciones");
                        mostrarInvitaciones(invitaciones);
                        actualizarBadge(invitaciones.size());
                        lblEstado.setText("");
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        System.err.println("❌ [VistaInvitacionesPendientes]: Error al cargar invitaciones: " + ex.getMessage());
                        lblEstado.setText("❌ Error al cargar invitaciones: " + ex.getMessage());
                        lblEstado.setTextFill(Color.RED);
                        mostrarError("No se pudieron cargar las invitaciones");
                    });
                    return null;
                });
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("📥 [VistaInvitacionesPendientes]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "NUEVA_INVITACION_CANAL":
                    // Nueva invitación recibida en tiempo real
                    if (datos instanceof Map) {
                        Map<String, String> invitacionData = (Map<String, String>) datos;
                        System.out.println("🔔 [VistaInvitacionesPendientes]: Nueva invitación recibida en tiempo real");
                        System.out.println("   → Canal: " + invitacionData.get("channelName"));
                        
                        mostrarNotificacionNuevaInvitacion(invitacionData);
                        
                        // Recargar la lista completa
                        cargarInvitaciones();
                    }
                    break;

                case "INVITACIONES_PENDIENTES":
                    // Lista completa de invitaciones
                    if (datos instanceof List) {
                        List<DTOCanalCreado> invitaciones = (List<DTOCanalCreado>) datos;
                        System.out.println("✅ [VistaInvitacionesPendientes]: Lista actualizada con " + invitaciones.size() + " invitaciones");
                        mostrarInvitaciones(invitaciones);
                        actualizarBadge(invitaciones.size());
                    }
                    break;

                case "INVITACION_ACEPTADA":
                    lblEstado.setText("✅ Invitación aceptada. ¡Ahora eres miembro del canal!");
                    lblEstado.setTextFill(Color.GREEN);
                    // Recargar invitaciones
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Platform.runLater(this::cargarInvitaciones);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                    break;

