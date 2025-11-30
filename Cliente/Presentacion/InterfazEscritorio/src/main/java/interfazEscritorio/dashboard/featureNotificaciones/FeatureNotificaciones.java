package interfazEscritorio.dashboard.featureNotificaciones;

import controlador.notificaciones.IControladorNotificaciones;
import dto.featureNotificaciones.DTONotificacion;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import observador.IObservador;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa el panel central que muestra una lista de notificaciones.
 * AHORA implementa IObservador para recibir actualizaciones en tiempo real.
 */
public class FeatureNotificaciones extends VBox implements IObservador {

    private final IControladorNotificaciones controlador;
    private final Label titulo;
    private final VBox tarjetasContainer;

    public FeatureNotificaciones(IControladorNotificaciones controlador) {
        super(15);
        this.controlador = controlador;
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #ecf0f1;");

        // Registrarse como observador
        controlador.registrarObservador(this);
        System.out.println("✅ [FeatureNotificaciones]: Registrada como observador");

        // --- Cabecera de Notificaciones ---
        BorderPane header = new BorderPane();
        titulo = new Label("NOTIFICATIONS (0)");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Hyperlink verTodoLink = new Hyperlink("Mark All Read");
        verTodoLink.setOnAction(e -> {
            System.out.println("🖱️ [FeatureNotificaciones]: Marcando todas como leídas");
            controlador.marcarTodasComoLeidas();
        });

        header.setLeft(titulo);
        header.setRight(verTodoLink);

        // --- Contenedor de Tarjetas ---
        tarjetasContainer = new VBox(10);

        Label cargando = new Label("Cargando notificaciones...");
        cargando.setStyle("-fx-text-fill: gray;");
        tarjetasContainer.getChildren().add(cargando);

        // --- Scroll Pane para las tarjetas ---
        ScrollPane scrollPane = new ScrollPane(tarjetasContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        this.getChildren().addAll(header, scrollPane);

        // Solicitar notificaciones iniciales
        controlador.solicitarActualizacionNotificaciones();
        System.out.println("📡 [FeatureNotificaciones]: Solicitando lista de notificaciones...");
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.err.println("🔔🔔🔔 [FeatureNotificaciones]: ========== ACTUALIZAR INVOCADO ==========");
        System.err.println("🔔 [FeatureNotificaciones]: Notificación recibida - Tipo: " + tipoDeDato);
        System.err.println("🔔 [FeatureNotificaciones]: Tipo de datos: " + (datos != null ? datos.getClass().getName() : "null"));

        Platform.runLater(() -> {
            if ("ACTUALIZAR_NOTIFICACIONES".equals(tipoDeDato)) {
                // Simplemente solicitar los datos actualizados al controlador
                System.err.println("🔄🔄🔄 [FeatureNotificaciones]: Solicitando actualización de notificaciones al controlador...");
                controlador.solicitarActualizacionNotificaciones();

            } else if ("LISTA_NOTIFICACIONES".equals(tipoDeDato) && datos instanceof List) {
                // Este es el evento que trae los datos reales
                List<?> listaRaw = (List<?>) datos;

                System.err.println("📋📋📋 [FeatureNotificaciones]: Recibido evento LISTA_NOTIFICACIONES con " + listaRaw.size() + " elementos");

                if (listaRaw.isEmpty()) {
                    System.err.println("⚠️⚠️⚠️ [FeatureNotificaciones]: Lista vacía recibida");
                    tarjetasContainer.getChildren().clear();
                    Label sinNotificaciones = new Label("No hay invitaciones a canales");
                    sinNotificaciones.setStyle("-fx-text-fill: gray; -fx-font-size: 14px;");
                    tarjetasContainer.getChildren().add(sinNotificaciones);
                    return;
                }

                // Validar que sean DTONotificacion
                Object primerElemento = listaRaw.get(0);
                System.err.println("🔍🔍🔍 [FeatureNotificaciones]: Tipo de objeto recibido: " + primerElemento.getClass().getSimpleName());

                if (primerElemento instanceof DTONotificacion) {
                    @SuppressWarnings("unchecked")
                    List<DTONotificacion> listaNotificaciones = (List<DTONotificacion>) listaRaw;
                    System.err.println("✅✅✅ [FeatureNotificaciones]: Actualizando UI con " + listaNotificaciones.size() + " notificaciones");
                    cargarNotificaciones(listaNotificaciones);
                } else {
                    System.err.println("❌❌❌ [FeatureNotificaciones]: Tipo de dato no soportado: " + primerElemento.getClass().getName());
                }

            } else if ("ERROR_NOTIFICACIONES".equals(tipoDeDato)) {
                System.err.println("❌ [FeatureNotificaciones]: Error recibido");
                mostrarError(datos != null ? datos.toString() : "Error desconocido");
            }
        });
        System.err.println("🔔🔔🔔 [FeatureNotificaciones]: ========== ACTUALIZAR FINALIZADO ==========");
    }

    private void cargarNotificaciones(List<DTONotificacion> notificaciones) {
        tarjetasContainer.getChildren().clear();

        // Filtrar solo notificaciones de invitaciones a canales
        List<DTONotificacion> invitacionesCanal = notificaciones.stream()
                .filter(n -> "INVITACION_CANAL".equals(n.getTipo()))
                .collect(Collectors.toList());

        long noLeidas = invitacionesCanal.stream().filter(n -> !n.isLeida()).count();
        titulo.setText("CANAL INVITATIONS (" + invitacionesCanal.size() + (noLeidas > 0 ? " - " + noLeidas + " new" : "") + ")");

        if (invitacionesCanal.isEmpty()) {
            Label sinNotificaciones = new Label("No hay invitaciones a canales");
            sinNotificaciones.setStyle("-fx-text-fill: gray; -fx-font-size: 14px;");
            tarjetasContainer.getChildren().add(sinNotificaciones);
        } else {
            for (DTONotificacion notificacion : invitacionesCanal) {
                tarjetasContainer.getChildren().add(crearTarjetaNotificacion(notificacion));
            }
        }
    }

    private VBox crearTarjetaNotificacion(DTONotificacion notificacion) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));

        String estilo = notificacion.isLeida()
                ? "-fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-radius: 5; -fx-background-radius: 5;"
                : "-fx-background-color: white; -fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;";
        card.setStyle(estilo);

        Label tituloLabel = new Label(notificacion.getTitulo());
        tituloLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Text contenidoText = new Text(notificacion.getContenido());
        contenidoText.setWrappingWidth(350);

        Label tiempoLabel = new Label(notificacion.getTiempoRelativo());
        tiempoLabel.setStyle("-fx-text-fill: gray;");

        // Crear barra de botones para aceptar/rechazar
        HBox botonesBox = new HBox(10);
        botonesBox.setPadding(new Insets(5, 0, 0, 0));

        Button btnAceptar = new Button("✓ Aceptar");
        btnAceptar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnAceptar.setOnAction(e -> {
            System.out.println("✅ [FeatureNotificaciones]: Aceptando invitación: " + notificacion.getId() + " para canal: " + notificacion.getOrigenId());
            btnAceptar.setDisable(true);
            controlador.aceptarInvitacionCanal(notificacion.getId(), notificacion.getOrigenId())
                    .thenRun(() -> System.out.println("✅ Invitación aceptada con éxito"))
                    .exceptionally(ex -> {
                        System.err.println("❌ Error al aceptar invitación: " + ex.getMessage());
                        Platform.runLater(() -> btnAceptar.setDisable(false));
                        return null;
                    });
        });

        Button btnRechazar = new Button("✗ Rechazar");
        btnRechazar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnRechazar.setOnAction(e -> {
            System.out.println("❌ [FeatureNotificaciones]: Rechazando invitación: " + notificacion.getId() + " para canal: " + notificacion.getOrigenId());
            btnRechazar.setDisable(true);
            controlador.rechazarInvitacionCanal(notificacion.getId(), notificacion.getOrigenId()) // ✅ Pasar ambos IDs: notificationId y channelId
                    .thenRun(() -> System.out.println("✅ Invitación rechazada con éxito"))
                    .exceptionally(ex -> {
                        System.err.println("❌ Error al rechazar invitación: " + ex.getMessage());
                        Platform.runLater(() -> btnRechazar.setDisable(false));
                        return null;
                    });
        });

        botonesBox.getChildren().addAll(btnAceptar, btnRechazar);

        card.getChildren().addAll(tituloLabel, contenidoText, tiempoLabel, botonesBox);

        return card;
    }

    private void mostrarError(String mensaje) {
        tarjetasContainer.getChildren().clear();
        Label errorLabel = new Label("Error: " + mensaje);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        tarjetasContainer.getChildren().add(errorLabel);
    }
}
