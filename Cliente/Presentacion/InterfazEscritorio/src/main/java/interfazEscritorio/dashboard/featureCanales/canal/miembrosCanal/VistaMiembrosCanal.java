package interfazEscritorio.dashboard.featureCanales.canal.miembrosCanal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.function.Consumer;

/**
 * Representa el panel que muestra los miembros de un canal.
 */
public class VistaMiembrosCanal extends BorderPane {

    /**
     * @param nombreCanal El nombre del canal.
     * @param onVolver    Acción para regresar a la vista anterior (el chat del canal).
     * @param onInvitar   Acción para abrir la vista de invitación de miembros.
     */
    public VistaMiembrosCanal(String nombreCanal, Runnable onVolver, Consumer<String> onInvitar) {
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f4f4f4;");

        // --- Contenido Principal ---
        VBox mainContent = new VBox(20);
        mainContent.setAlignment(Pos.TOP_CENTER);

        // --- Header ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("CHANNEL MEMBERS (2)");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Button btnInvitar = new Button("+ Invite");
        btnInvitar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        // Llama al callback para mostrar la vista de invitar
        btnInvitar.setOnAction(e -> onInvitar.accept(nombreCanal));

        Button btnVolver = new Button("← Volver");
        btnVolver.setOnAction(e -> onVolver.run());
        header.getChildren().addAll(titulo, spacer, btnInvitar, btnVolver);
        mainContent.getChildren().add(header);

        // --- Lista de Miembros ---
        VBox miembrosBox = new VBox(10);
        miembrosBox.getChildren().add(crearTarjetaMiembro("bob_smith", true));
        miembrosBox.getChildren().add(crearTarjetaMiembro("ana_eliana", false));
        mainContent.getChildren().add(miembrosBox);

        // --- Invitaciones Pendientes ---
        VBox pendientesBox = new VBox(10);
        Label tituloPendientes = new Label("PENDING INVITATION");
        tituloPendientes.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        pendientesBox.getChildren().add(tituloPendientes);
        pendientesBox.getChildren().add(crearTarjetaInvitacion("david_w", "john_doe"));
        pendientesBox.getChildren().add(crearTarjetaInvitacion("another_user", "john_doe"));
        mainContent.getChildren().add(pendientesBox);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        this.setCenter(scrollPane);
    }

    private VBox crearTarjetaMiembro(String nombre, boolean online) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Circle status = new Circle(5, online ? Color.GREEN : Color.LIGHTGRAY);
        Label nameLabel = new Label(nombre);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        topRow.getChildren().addAll(status, nameLabel);

        card.getChildren().add(topRow);
        card.getChildren().add(new Label("- Status: " + (online ? "Online" : "Offline")));
        card.getChildren().add(new Label("- Joined: Mar 16, 2025"));
        card.getChildren().add(new Label("- Last active: 5 min ago"));

        return card;
    }

    private VBox crearTarjetaInvitacion(String invitado, String anfitrion) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        Label info = new Label(invitado + " - Invited by " + anfitrion + " on Mar 30");
        info.setWrapText(true);

        HBox botones = new HBox(10);
        Button btnCancelar = new Button("Cancel Invitation");
        Button btnReenviar = new Button("Resend");
        botones.getChildren().addAll(btnCancelar, btnReenviar);

        card.getChildren().addAll(info, botones);
        return card;
    }
}

