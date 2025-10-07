package interfazEscritorio.dashboard.featureCanales.canal.miembrosCanal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Representa el panel para invitar nuevos miembros a un canal.
 */
public class VistaInvitarMiembro extends BorderPane {

    /**
     * @param nombreCanal El nombre del canal al que se invitará.
     * @param onVolver    Acción para regresar a la vista de miembros.
     */
    public VistaInvitarMiembro(String nombreCanal, Runnable onVolver) {
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f4f4f4;");

        // --- Contenido Principal ---
        VBox mainLayout = new VBox(15);
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // --- Header ---
        Label titulo = new Label("Invite Members to " + nombreCanal);
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        mainLayout.getChildren().add(titulo);

        // --- Barra de Búsqueda ---
        TextField campoBusqueda = new TextField();
        campoBusqueda.setPromptText("Search users to invite...");
        mainLayout.getChildren().add(campoBusqueda);

        // --- Lista de Contactos ---
        VBox contactosBox = new VBox(10);
        contactosBox.setPadding(new Insets(10));
        contactosBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");
        Label subtitulo = new Label("AVAILABLE CONTACTS");
        subtitulo.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        subtitulo.setTextFill(Color.GRAY);
        contactosBox.getChildren().add(subtitulo);

        // Contactos de ejemplo
        contactosBox.getChildren().add(crearEntradaContacto("david_w", true));
        contactosBox.getChildren().add(crearEntradaContacto("jullan53", true));

        ScrollPane scrollPane = new ScrollPane(contactosBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        mainLayout.getChildren().add(scrollPane);

        this.setCenter(mainLayout);

        // --- Botones de Acción (Abajo) ---
        HBox botonesBox = new HBox(10);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);
        botonesBox.setPadding(new Insets(15, 0, 0, 0));
        Button btnCancelar = new Button("Cancel");
        btnCancelar.setOnAction(e -> onVolver.run());
        Button btnInvitar = new Button("Invite");
        btnInvitar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        botonesBox.getChildren().addAll(btnCancelar, btnInvitar);

        this.setBottom(botonesBox);
    }

    private HBox crearEntradaContacto(String nombre, boolean online) {
        HBox entrada = new HBox(10);
        entrada.setAlignment(Pos.CENTER_LEFT);

        CheckBox checkBox = new CheckBox();
        Circle status = new Circle(5, online ? Color.GREEN : Color.LIGHTGRAY);

        VBox infoBox = new VBox();
        Label nombreLabel = new Label(nombre);
        nombreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        Label activoLabel = new Label("last activate: Just Now");
        activoLabel.setTextFill(Color.GRAY);
        infoBox.getChildren().addAll(nombreLabel, activoLabel);

        entrada.getChildren().addAll(checkBox, status, infoBox);
        return entrada;
    }
}
