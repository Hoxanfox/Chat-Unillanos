package interfazEscritorio.dashboard.featureHeader;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

public class FeatureHeader extends BorderPane {

    public FeatureHeader(Runnable onCerrarSesion) {
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");

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

        Label notificacionesIcono = new Label("🔔 9");
        notificacionesIcono.setFont(Font.font(14));

        Label perfilIcono = new Label("👤");
        perfilIcono.setFont(Font.font(14));

        // Botón de cerrar sesión
        Button btnCerrarSesion = new Button("Cerrar Sesión");
        btnCerrarSesion.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px;");
        btnCerrarSesion.setOnAction(e -> onCerrarSesion.run());

        iconosLayout.getChildren().addAll(notificacionesIcono, perfilIcono, btnCerrarSesion);

        this.setLeft(menuBar);
        this.setRight(iconosLayout);
    }
}
