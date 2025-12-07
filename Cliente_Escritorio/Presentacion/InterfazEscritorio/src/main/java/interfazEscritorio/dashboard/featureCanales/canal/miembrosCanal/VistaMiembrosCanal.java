package interfazEscritorio.dashboard.featureCanales.canal.miembrosCanal;

import controlador.canales.IControladorCanales;
import dto.canales.DTOMiembroCanal;
import javafx.application.Platform;
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
import observador.IObservador;

import java.util.List;
import java.util.function.Consumer;

/**
 * Vista que muestra los miembros de un canal.
 * Implementa IObservador para recibir actualizaciones de la lista de miembros.
 */
public class VistaMiembrosCanal extends BorderPane implements IObservador {

    private final IControladorCanales controlador;
    private final String canalId;
    private final VBox miembrosBox;
    private final Label tituloMiembros;

    public VistaMiembrosCanal(String canalId, String nombreCanal, Runnable onVolver, Consumer<String> onInvitar, IControladorCanales controlador) {
        this.controlador = controlador;
        this.canalId = canalId;
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f4f4f4;");

        // Registrarse como observador
        controlador.registrarObservadorMiembros(this);
        System.out.println("✅ [VistaMiembrosCanal]: Registrada como observador de miembros");

        VBox mainContent = new VBox(20);
        mainContent.setAlignment(Pos.TOP_CENTER);

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        tituloMiembros = new Label("CHANNEL MEMBERS (0)");
        tituloMiembros.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnInvitar = new Button("+ Invite");
        btnInvitar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnInvitar.setOnAction(e -> onInvitar.accept(canalId));

        Button btnVolver = new Button("← Volver");
        btnVolver.setOnAction(e -> onVolver.run());

        header.getChildren().addAll(tituloMiembros, spacer, btnInvitar, btnVolver);
        mainContent.getChildren().add(header);

        // Lista de miembros
        miembrosBox = new VBox(10);
        Label cargando = new Label("Cargando miembros...");
        cargando.setTextFill(Color.GRAY);
        miembrosBox.getChildren().add(cargando);
        mainContent.getChildren().add(miembrosBox);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        this.setCenter(scrollPane);

        // Solicitar lista de miembros
        controlador.solicitarMiembrosCanal(canalId);
        System.out.println("📡 [VistaMiembrosCanal]: Solicitando miembros del canal...");
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [VistaMiembrosCanal]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "MIEMBROS_CANAL_RECIBIDOS":
                    if (datos instanceof List) {
                        List<DTOMiembroCanal> miembros = (List<DTOMiembroCanal>) datos;
                        System.out.println("✅ [VistaMiembrosCanal]: Actualizando lista con " + miembros.size() + " miembros");
                        cargarMiembros(miembros);
                    }
                    break;

                case "MIEMBRO_AGREGADO":
                    if (datos instanceof DTOMiembroCanal) {
                        DTOMiembroCanal miembro = (DTOMiembroCanal) datos;
                        System.out.println("✅ [VistaMiembrosCanal]: Nuevo miembro agregado");
                        agregarMiembro(miembro);
                    }
                    break;

                case "ERROR_OPERACION":
                    System.out.println("❌ [VistaMiembrosCanal]: Error: " + datos);
                    mostrarError(datos.toString());
                    break;

                default:
                    System.out.println("⚠️ [VistaMiembrosCanal]: Tipo de notificación desconocido: " + tipoDeDato);
            }
        });
    }

    private void cargarMiembros(List<DTOMiembroCanal> miembros) {
        miembrosBox.getChildren().clear();
        tituloMiembros.setText("CHANNEL MEMBERS (" + miembros.size() + ")");

        if (miembros.isEmpty()) {
            Label sinMiembros = new Label("No hay miembros en este canal");
            sinMiembros.setTextFill(Color.GRAY);
            miembrosBox.getChildren().add(sinMiembros);
        } else {
            for (DTOMiembroCanal miembro : miembros) {
                miembrosBox.getChildren().add(crearTarjetaMiembro(miembro));
            }
        }
    }

    private void agregarMiembro(DTOMiembroCanal miembro) {
        miembrosBox.getChildren().add(crearTarjetaMiembro(miembro));
        int count = miembrosBox.getChildren().size();
        tituloMiembros.setText("CHANNEL MEMBERS (" + count + ")");
    }

    private VBox crearTarjetaMiembro(DTOMiembroCanal miembro) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Indicador de estado (por defecto gris, ya que no tenemos estado en el DTO)
        Circle status = new Circle(5, Color.LIGHTGRAY);

        Label nameLabel = new Label(miembro.getNombreUsuario());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        topRow.getChildren().addAll(status, nameLabel);

        card.getChildren().add(topRow);
        card.getChildren().add(new Label("- Role: " + miembro.getRol()));

        if (miembro.getFechaUnion() != null) {
            card.getChildren().add(new Label("- Joined: " + miembro.getFechaUnion()));
        }

        return card;
    }

    private void mostrarError(String mensaje) {
        Label errorLabel = new Label("Error: " + mensaje);
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        miembrosBox.getChildren().add(errorLabel);
    }
}
