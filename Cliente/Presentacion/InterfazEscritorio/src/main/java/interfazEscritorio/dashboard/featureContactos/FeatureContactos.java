package interfazEscritorio.dashboard.featureContactos;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

/**
 * Representa el feature que muestra la lista de usuarios en línea.
 * Notifica a su contenedor padre cuando un contacto es seleccionado.
 */
public class FeatureContactos extends VBox {

    private final Consumer<String> onContactoSeleccionado;

    public FeatureContactos(Consumer<String> onContactoSeleccionado) {
        super(10);
        this.onContactoSeleccionado = onContactoSeleccionado;
        this.setPadding(new Insets(15, 15, 10, 15));

        Label tituloUsuarios = new Label("ONLINE USERS (42)");
        tituloUsuarios.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        tituloUsuarios.setTextFill(Color.WHITE);

        this.getChildren().add(tituloUsuarios);
        this.getChildren().add(crearEntradaUsuario("john_doe", Color.GREEN, true));
        this.getChildren().add(crearEntradaUsuario("alice123", Color.GREEN, true));
        this.getChildren().add(crearEntradaUsuario("emma_j", Color.LIGHTGREEN, true));
        this.getChildren().add(crearEntradaUsuario("david_kk", Color.LIGHTGRAY, false));
    }

    private HBox crearEntradaUsuario(String nombre, Color colorEstado, boolean isOnline) {
        HBox userEntry = new HBox(10);
        userEntry.setAlignment(Pos.CENTER_LEFT);
        Circle statusIndicator = new Circle(5, colorEstado);
        Label nameLabel = new Label(nombre);
        nameLabel.setTextFill(Color.WHITE);

        if (isOnline) {
            userEntry.setCursor(Cursor.HAND);
            userEntry.setOnMouseClicked(event -> {
                onContactoSeleccionado.accept(nombre);
            });
        }

        userEntry.getChildren().addAll(statusIndicator, nameLabel);
        return userEntry;
    }
}

