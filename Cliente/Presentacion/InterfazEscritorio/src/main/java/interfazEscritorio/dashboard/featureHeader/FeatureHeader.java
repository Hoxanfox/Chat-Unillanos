package interfazEscritorio.dashboard.featureHeader;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

/**
 * Representa el header de la aplicación, incluyendo el menú y los iconos de estado.
 */
public class FeatureHeader extends BorderPane {

    public FeatureHeader() {
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

        Label notificacionesIcono = new Label("🔔 9"); // Placeholder para icono
        notificacionesIcono.setFont(Font.font(14));

        Label perfilIcono = new Label("👤"); // Placeholder para icono
        perfilIcono.setFont(Font.font(14));

        iconosLayout.getChildren().addAll(notificacionesIcono, perfilIcono);

        this.setLeft(menuBar);
        this.setRight(iconosLayout);
    }
}
