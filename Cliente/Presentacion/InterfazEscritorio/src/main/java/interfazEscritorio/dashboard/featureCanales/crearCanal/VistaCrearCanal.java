package interfazEscritorio.dashboard.featureCanales.crearCanal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Representa el formulario para crear un nuevo canal.
 */
public class VistaCrearCanal extends BorderPane {

    /**
     * @param onVolver Acción para cancelar y regresar a la vista principal.
     */
    public VistaCrearCanal(Runnable onVolver) {
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #ecf0f1;");

        // --- Header ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        Label titulo = new Label("Crear un Nuevo Canal");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        this.setTop(titulo);

        // --- Formulario (Centro) ---
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER_LEFT);

        Label labelNombre = new Label("Nombre del Canal:");
        TextField campoNombre = new TextField();
        campoNombre.setPromptText("Ej: marketing-global");

        Label labelDescripcion = new Label("Descripción (opcional):");
        TextArea campoDescripcion = new TextArea();
        campoDescripcion.setPromptText("¿De qué trata este canal?");
        campoDescripcion.setPrefHeight(100);

        form.getChildren().addAll(labelNombre, campoNombre, labelDescripcion, campoDescripcion);

        // --- Botones (Abajo) ---
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> onVolver.run());
        Button btnCrear = new Button("Crear Canal");
        btnCrear.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        // Aquí iría la lógica para crear el canal
        btnCrear.setOnAction(e -> {
            System.out.println("Creando canal...");
            onVolver.run(); // Simula éxito y vuelve
        });

        botones.getChildren().addAll(btnCancelar, btnCrear);
        form.getChildren().add(botones);

        this.setCenter(form);
    }
}
