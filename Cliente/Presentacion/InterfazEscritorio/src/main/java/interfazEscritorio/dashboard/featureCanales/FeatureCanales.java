package interfazEscritorio.dashboard.featureCanales;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

/**
 * Representa el feature que muestra la lista de canales disponibles.
 */
public class FeatureCanales extends VBox {

    /**
     * @param onCanalSeleccionado Callback que se ejecuta al seleccionar un canal.
     * @param onCrearCanal        Callback que se ejecuta al hacer clic en 'Crear Canal'.
     */
    public FeatureCanales(Consumer<String> onCanalSeleccionado, Runnable onCrearCanal) {
        super(10);
        this.setPadding(new Insets(10, 15, 15, 15));

        Label tituloCanales = new Label("CHANNELS");
        tituloCanales.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        tituloCanales.setTextFill(Color.WHITE);

        this.getChildren().add(tituloCanales);
        this.getChildren().add(crearEntradaCanal("General", Color.GRAY, onCanalSeleccionado));
        this.getChildren().add(crearEntradaCanal("Team Alpha", Color.GOLD, onCanalSeleccionado));
        this.getChildren().add(crearEntradaCanal("Project X", Color.ORANGE, onCanalSeleccionado));

        Hyperlink crearCanalLink = new Hyperlink("+ Create Channel");
        crearCanalLink.setTextFill(Color.LIGHTBLUE);
        crearCanalLink.setOnAction(e -> onCrearCanal.run()); // Asignar acción

        this.getChildren().add(crearCanalLink);
    }

    private HBox crearEntradaCanal(String nombre, Color colorEstado, Consumer<String> onCanalSeleccionado) {
        HBox channelEntry = new HBox(10);
        channelEntry.setAlignment(Pos.CENTER_LEFT);
        channelEntry.setCursor(Cursor.HAND); // Hacer que parezca clickeable
        channelEntry.setOnMouseClicked(e -> onCanalSeleccionado.accept(nombre)); // Asignar acción

        Circle statusIndicator = new Circle(5, colorEstado);
        Label nameLabel = new Label(nombre);
        nameLabel.setTextFill(Color.WHITE);
        channelEntry.getChildren().addAll(statusIndicator, nameLabel);
        return channelEntry;
    }
}

