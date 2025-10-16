package interfazEscritorio.dashboard.featureNotificaciones;

import controlador.notificaciones.IControladorNotificaciones;
import dto.featureNotificaciones.DTONotificacion;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import observador.IObservador;

import java.util.List;

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
        System.out.println("🔔 [FeatureNotificaciones]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            if ("ACTUALIZAR_NOTIFICACIONES".equals(tipoDeDato) && datos instanceof List) {
                List<DTONotificacion> notificaciones = (List<DTONotificacion>) datos;
                System.out.println("✅ [FeatureNotificaciones]: Actualizando lista con " + notificaciones.size() + " notificaciones");
                cargarNotificaciones(notificaciones);
            } else if ("ERROR_NOTIFICACIONES".equals(tipoDeDato)) {
                mostrarError(datos.toString());
            }
        });
    }

    private void cargarNotificaciones(List<DTONotificacion> notificaciones) {
        tarjetasContainer.getChildren().clear();

        long noLeidas = notificaciones.stream().filter(n -> !n.isLeida()).count();
        titulo.setText("NOTIFICATIONS (" + notificaciones.size() + (noLeidas > 0 ? " - " + noLeidas + " new" : "") + ")");

        if (notificaciones.isEmpty()) {
            Label sinNotificaciones = new Label("No hay notificaciones");
            sinNotificaciones.setStyle("-fx-text-fill: gray; -fx-font-size: 14px;");
            tarjetasContainer.getChildren().add(sinNotificaciones);
        } else {
            for (DTONotificacion notificacion : notificaciones) {
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

        // Si no está leída, agregar botón para marcar como leída
        if (!notificacion.isLeida()) {
            Hyperlink marcarLeida = new Hyperlink("Mark as read");
            marcarLeida.setOnAction(e -> {
                System.out.println("🖱️ [FeatureNotificaciones]: Marcando como leída: " + notificacion.getId());
                controlador.marcarComoLeida(notificacion.getId());
            });
            card.getChildren().addAll(tituloLabel, contenidoText, tiempoLabel, marcarLeida);
        } else {
            card.getChildren().addAll(tituloLabel, contenidoText, tiempoLabel);
        }

        return card;
    }

    private void mostrarError(String mensaje) {
        tarjetasContainer.getChildren().clear();
        Label errorLabel = new Label("Error: " + mensaje);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        tarjetasContainer.getChildren().add(errorLabel);
    }
}
