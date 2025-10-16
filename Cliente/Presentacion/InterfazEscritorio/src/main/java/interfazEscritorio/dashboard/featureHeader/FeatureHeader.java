package interfazEscritorio.dashboard.featureHeader;

import controlador.notificaciones.IControladorNotificaciones;
import dto.featureNotificaciones.DTONotificacion;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import observador.IObservador;

import java.util.List;

/**
 * Barra de encabezado con menú y estado del usuario.
 * AHORA implementa IObservador para actualizar el contador de notificaciones.
 */
public class FeatureHeader extends BorderPane implements IObservador {

    private final Label notificacionesIcono;

    public FeatureHeader(Runnable onCerrarSesion, IControladorNotificaciones controladorNotificaciones) {
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");

        // Registrarse como observador de notificaciones
        controladorNotificaciones.registrarObservador(this);
        System.out.println("✅ [FeatureHeader]: Registrado como observador de notificaciones");

        // --- Barra de Menú (Izquierda) ---
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        Menu menuEdit = new Menu("Edit");
        Menu menuView = new Menu("View");
        Menu menuHelp = new Menu("Help");
        menuBar.getMenus().addAll(menuFile, menuEdit, menuView, menuHelp);
        menuBar.setStyle("-fx-background-color: transparent;");

        // --- Iconos de Estado (Derecha) ---
        HBox iconosLayout = new HBox(15);
        iconosLayout.setAlignment(Pos.CENTER);

        notificacionesIcono = new Label("🔔 0");
        notificacionesIcono.setFont(Font.font(14));
        notificacionesIcono.setStyle("-fx-cursor: hand;");

        Label perfilIcono = new Label("👤");
        perfilIcono.setFont(Font.font(14));

        // Botón de cerrar sesión
        Button btnCerrarSesion = new Button("Cerrar Sesión");
        btnCerrarSesion.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px;");
        btnCerrarSesion.setOnAction(e -> onCerrarSesion.run());

        iconosLayout.getChildren().addAll(notificacionesIcono, perfilIcono, btnCerrarSesion);

        this.setLeft(menuBar);
        this.setRight(iconosLayout);

        // Solicitar notificaciones iniciales para actualizar el contador
        controladorNotificaciones.solicitarActualizacionNotificaciones();
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [FeatureHeader]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            if ("ACTUALIZAR_NOTIFICACIONES".equals(tipoDeDato) && datos instanceof List) {
                List<DTONotificacion> notificaciones = (List<DTONotificacion>) datos;
                long noLeidas = notificaciones.stream().filter(n -> !n.isLeida()).count();
                notificacionesIcono.setText("🔔 " + noLeidas);
                System.out.println("✅ [FeatureHeader]: Contador actualizado - " + noLeidas + " no leídas");
            }
        });
    }
}
