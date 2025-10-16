package interfazEscritorio.dashboard.featureCanales;

import controlador.canales.IControladorCanales;
import dto.canales.DTOCanalCreado;
import javafx.application.Platform;
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
import observador.IObservador;

import java.util.List;
import java.util.function.Consumer;

/**
 * Representa el feature que muestra la lista de canales disponibles.
 * Implementa IObservador para recibir actualizaciones de la lista de canales.
 */
public class FeatureCanales extends VBox implements IObservador {

    private final IControladorCanales controlador;
    private final VBox canalesContainer;
    private final Consumer<DTOCanalCreado> onCanalSeleccionado;

    /**
     * @param onCanalSeleccionado Callback que se ejecuta al seleccionar un canal.
     * @param onCrearCanal        Callback que se ejecuta al hacer clic en 'Crear Canal'.
     * @param controlador         Controlador de canales.
     */
    public FeatureCanales(Consumer<DTOCanalCreado> onCanalSeleccionado, Runnable onCrearCanal, IControladorCanales controlador) {
        super(10);
        this.controlador = controlador;
        this.onCanalSeleccionado = onCanalSeleccionado;
        this.setPadding(new Insets(10, 15, 15, 15));

        // Registrarse como observador
        controlador.registrarObservadorListado(this);
        System.out.println("✅ [FeatureCanales]: Registrada como observador de listado");

        Label tituloCanales = new Label("CHANNELS");
        tituloCanales.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        tituloCanales.setTextFill(Color.WHITE);

        // Container para los canales (se actualizará dinámicamente)
        canalesContainer = new VBox(5);
        canalesContainer.setAlignment(Pos.TOP_LEFT);

        Hyperlink crearCanalLink = new Hyperlink("+ Create Channel");
        crearCanalLink.setTextFill(Color.LIGHTBLUE);
        crearCanalLink.setOnAction(e -> onCrearCanal.run());

        this.getChildren().addAll(tituloCanales, canalesContainer, crearCanalLink);

        // Solicitar la lista de canales al cargar
        controlador.solicitarCanalesUsuario();
        System.out.println("📡 [FeatureCanales]: Solicitando lista de canales...");
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [FeatureCanales]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "CANALES_ACTUALIZADOS":
                    if (datos instanceof List) {
                        List<DTOCanalCreado> canales = (List<DTOCanalCreado>) datos;
                        System.out.println("✅ [FeatureCanales]: Actualizando lista con " + canales.size() + " canales");
                        actualizarListaCanales(canales);
                    }
                    break;

                case "CANAL_CREADO_EXITOSAMENTE":
                    System.out.println("✅ [FeatureCanales]: Nuevo canal creado, refrescando lista...");
                    // Solicitar lista actualizada
                    controlador.solicitarCanalesUsuario();
                    break;

                default:
                    System.out.println("⚠️ [FeatureCanales]: Tipo de notificación desconocido: " + tipoDeDato);
            }
        });
    }

    private void actualizarListaCanales(List<DTOCanalCreado> canales) {
        canalesContainer.getChildren().clear();

        if (canales.isEmpty()) {
            Label sinCanales = new Label("No hay canales disponibles");
            sinCanales.setTextFill(Color.LIGHTGRAY);
            canalesContainer.getChildren().add(sinCanales);
        } else {
            for (DTOCanalCreado canal : canales) {
                HBox canalEntry = crearEntradaCanal(canal);
                canalesContainer.getChildren().add(canalEntry);
            }
        }
    }

    private HBox crearEntradaCanal(DTOCanalCreado canal) {
        HBox channelEntry = new HBox(10);
        channelEntry.setAlignment(Pos.CENTER_LEFT);
        channelEntry.setCursor(Cursor.HAND);

        // Al hacer clic, pasar el objeto DTOCanalCreado completo
        channelEntry.setOnMouseClicked(e -> {
            System.out.println("🖱️ [FeatureCanales]: Canal seleccionado: " + canal.getNombre());
            onCanalSeleccionado.accept(canal);
        });

        Circle statusIndicator = new Circle(5, Color.GREEN); // Verde = activo
        Label nameLabel = new Label(canal.getNombre());
        nameLabel.setTextFill(Color.WHITE);

        channelEntry.getChildren().addAll(statusIndicator, nameLabel);
        return channelEntry;
    }
}
