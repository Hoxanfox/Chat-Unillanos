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
        System.out.println("🔔 [FeatureCanales]: Tipo de datos: " + (datos != null ? datos.getClass().getName() : "null"));

        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "CANALES_ACTUALIZADOS":
                    if (datos instanceof List) {
                        List<DTOCanalCreado> canales = (List<DTOCanalCreado>) datos;
                        System.out.println("✅ [FeatureCanales]: Actualizando lista con " + canales.size() + " canales");

                        // Log detallado de cada canal
                        for (int i = 0; i < canales.size(); i++) {
                            DTOCanalCreado canal = canales.get(i);
                            System.out.println("   Canal " + (i + 1) + ": ID=" + canal.getId() + ", Nombre=" + canal.getNombre());
                        }

                        actualizarListaCanales(canales);
                    } else {
                        System.err.println("❌ [FeatureCanales]: Los datos no son una lista. Tipo: " + (datos != null ? datos.getClass().getName() : "null"));
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
        System.out.println("🎨 [FeatureCanales]: Iniciando actualización de UI - Limpiando container...");
        System.out.println("🎨 [FeatureCanales]: Container visible: " + canalesContainer.isVisible());
        System.out.println("🎨 [FeatureCanales]: Container manejado: " + canalesContainer.isManaged());
        System.out.println("🎨 [FeatureCanales]: Tamaño container: " + canalesContainer.getWidth() + "x" + canalesContainer.getHeight());

        canalesContainer.getChildren().clear();
        System.out.println("🎨 [FeatureCanales]: Container limpiado. Canales a dibujar: " + canales.size());

        if (canales.isEmpty()) {
            System.out.println("⚠️ [FeatureCanales]: Lista vacía, mostrando mensaje");
            Label sinCanales = new Label("No hay canales disponibles");
            sinCanales.setTextFill(Color.LIGHTGRAY);
            sinCanales.setStyle("-fx-font-size: 12px;");
            canalesContainer.getChildren().add(sinCanales);
            System.out.println("✅ [FeatureCanales]: Mensaje 'sin canales' agregado");
        } else {
            System.out.println("✏️ [FeatureCanales]: Dibujando " + canales.size() + " canales...");
            for (int i = 0; i < canales.size(); i++) {
                DTOCanalCreado canal = canales.get(i);
                System.out.println("   Dibujando canal " + (i + 1) + ": " + canal.getNombre());
                HBox canalEntry = crearEntradaCanal(canal);
                canalesContainer.getChildren().add(canalEntry);
                System.out.println("   ✓ Canal agregado al container. Total en container: " + canalesContainer.getChildren().size());
            }
            System.out.println("✅ [FeatureCanales]: Todos los canales dibujados. Total final en container: " + canalesContainer.getChildren().size());

            // Forzar actualización del layout
            canalesContainer.requestLayout();
            this.requestLayout();
            System.out.println("🔄 [FeatureCanales]: Layout actualizado");
        }
    }

    private HBox crearEntradaCanal(DTOCanalCreado canal) {
        System.out.println("🔨 [FeatureCanales]: Creando HBox para canal: " + canal.getNombre());
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
        System.out.println("✅ [FeatureCanales]: HBox creado para: " + canal.getNombre());
        return channelEntry;
    }
}
