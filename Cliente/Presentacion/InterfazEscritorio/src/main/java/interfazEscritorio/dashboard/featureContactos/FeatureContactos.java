package interfazEscritorio.dashboard.featureContactos;

import controlador.contactos.IControladorContactos;
import observador.IObservador; // CORRECCIÓN: Paquete correcto
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import dto.featureContactos.DTOContacto;

import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Representa el feature que muestra la lista de usuarios en línea.
 * AHORA trabaja con el objeto DTOContacto completo.
 */
public class FeatureContactos extends VBox implements IObservador {

    // CORRECCIÓN: El consumidor ahora espera el objeto DTOContacto completo.
    private final Consumer<DTOContacto> onContactoSeleccionado;
    private final Label tituloUsuarios;
    private final VBox listaContactosContainer;
    // Mapa para mantener referencia a las vistas de cada contacto
    private final Map<String, HBox> contactoViews;

    public FeatureContactos(Consumer<DTOContacto> onContactoSeleccionado, IControladorContactos controladorContactos) {
        super(10);
        this.onContactoSeleccionado = onContactoSeleccionado;
        this.contactoViews = new HashMap<>();
        this.setPadding(new Insets(15, 15, 10, 15));

        System.out.println("✅ [FeatureContactos]: Creado. Registrándose como observador en el Controlador.");
        controladorContactos.registrarObservador(this);

        tituloUsuarios = new Label("ONLINE USERS (0)");
        tituloUsuarios.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        tituloUsuarios.setTextFill(Color.WHITE);

        listaContactosContainer = new VBox(10);
        this.getChildren().addAll(tituloUsuarios, listaContactosContainer);

        System.out.println("➡️ [FeatureContactos]: Solicitando lista de contactos inicial al servidor...");
        controladorContactos.solicitarActualizacionContactos();
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [FeatureContactos]: ¡Notificación recibida! Tipo: " + tipoDeDato);

        if ("ACTUALIZAR_CONTACTOS".equals(tipoDeDato) && datos instanceof List) {
            Platform.runLater(() -> {
                System.out.println("  -> [UI Thread]: Ejecutando actualización de la lista de contactos en la vista.");
                redibujarContactos((List<DTOContacto>) datos);
            });
        } else if ("CONTACT_PHOTO_READY".equals(tipoDeDato) && datos instanceof DTOContacto) {
            Platform.runLater(() -> {
                DTOContacto contacto = (DTOContacto) datos;
                System.out.println("📸 [FeatureContactos]: Actualizando foto para contacto: " + contacto.getNombre() + " - Path: " + contacto.getLocalPhotoPath());
                actualizarFotoContacto(contacto);
            });
        }
    }

    private void redibujarContactos(List<DTOContacto> contactos) {
        if (contactos == null) return;

        System.out.println("  -> [UI Thread]: Limpiando y redibujando la lista con " + contactos.size() + " contactos.");
        listaContactosContainer.getChildren().clear();
        contactoViews.clear();
        tituloUsuarios.setText("ONLINE USERS (" + contactos.size() + ")");

        for (DTOContacto contacto : contactos) {
            HBox contactoView = crearEntradaUsuario(contacto);
            contactoViews.put(contacto.getId(), contactoView);
            listaContactosContainer.getChildren().add(contactoView);
        }
    }

    private void actualizarFotoContacto(DTOContacto contacto) {
        HBox contactoView = contactoViews.get(contacto.getId());
        if (contactoView == null) {
            System.out.println("⚠️ [FeatureContactos]: No se encontró vista para contacto: " + contacto.getId());
            return;
        }

        String photoPath = contacto.getLocalPhotoPath();
        if (photoPath == null || photoPath.isEmpty()) {
            System.out.println("⚠️ [FeatureContactos]: No hay ruta de foto para contacto: " + contacto.getNombre());
            return;
        }

        // Buscar el StackPane que contiene el avatar
        StackPane avatarContainer = null;
        for (Node node : contactoView.getChildren()) {
            if (node instanceof StackPane) {
                avatarContainer = (StackPane) node;
                break;
            }
        }

        if (avatarContainer == null) {
            System.out.println("⚠️ [FeatureContactos]: No se encontró contenedor de avatar");
            return;
        }

        try {
            File imageFile = new File(photoPath);
            if (!imageFile.exists()) {
                System.out.println("⚠️ [FeatureContactos]: Archivo de imagen no existe: " + photoPath);
                return;
            }

            Image image = new Image(imageFile.toURI().toString(), 35, 35, false, true);

            // Limpiar el contenedor y agregar la nueva imagen
            avatarContainer.getChildren().clear();

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(35);
            imageView.setFitHeight(35);

            // Crear clip circular para la imagen
            Circle clip = new Circle(17.5, 17.5, 17.5);
            imageView.setClip(clip);

            // Indicador de estado
            boolean isOnline = "Online".equalsIgnoreCase(contacto.getEstado()) ||
                              "ONLINE".equalsIgnoreCase(contacto.getEstado());
            Color colorEstado = isOnline ? Color.GREEN : Color.LIGHTGRAY;
            Circle statusIndicator = new Circle(6, colorEstado);
            statusIndicator.setStroke(Color.web("#2c2f33"));
            statusIndicator.setStrokeWidth(2);

            avatarContainer.getChildren().addAll(imageView, statusIndicator);

            System.out.println("✅ [FeatureContactos]: Foto actualizada para contacto: " + contacto.getNombre());
        } catch (Exception e) {
            System.err.println("❌ [FeatureContactos]: Error al cargar imagen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private HBox crearEntradaUsuario(DTOContacto contacto) {
        HBox userEntry = new HBox(10);
        userEntry.setAlignment(Pos.CENTER_LEFT);

        boolean isOnline = "Online".equalsIgnoreCase(contacto.getEstado()) ||
                          "ONLINE".equalsIgnoreCase(contacto.getEstado());
        Color colorEstado = isOnline ? Color.GREEN : Color.LIGHTGRAY;

        // Crear contenedor para avatar + indicador de estado
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(35, 35);
        avatarContainer.setMinSize(35, 35);
        avatarContainer.setMaxSize(35, 35);

        // Si ya tiene la foto disponible, cargarla
        if (contacto.getLocalPhotoPath() != null && !contacto.getLocalPhotoPath().isEmpty()) {
            try {
                File imageFile = new File(contacto.getLocalPhotoPath());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString(), 35, 35, false, true);
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(35);
                    imageView.setFitHeight(35);

                    // Crear clip circular
                    Circle clip = new Circle(17.5, 17.5, 17.5);
                    imageView.setClip(clip);

                    // Indicador de estado
                    Circle statusIndicator = new Circle(6, colorEstado);
                    statusIndicator.setStroke(Color.web("#2c2f33"));
                    statusIndicator.setStrokeWidth(2);

                    StackPane.setAlignment(statusIndicator, Pos.BOTTOM_RIGHT);
                    avatarContainer.getChildren().addAll(imageView, statusIndicator);
                } else {
                    agregarAvatarPorDefecto(avatarContainer, colorEstado);
                }
            } catch (Exception e) {
                System.err.println("❌ [FeatureContactos]: Error al cargar imagen inicial: " + e.getMessage());
                agregarAvatarPorDefecto(avatarContainer, colorEstado);
            }
        } else {
            // Avatar por defecto mientras se carga la foto
            agregarAvatarPorDefecto(avatarContainer, colorEstado);
        }

        Label nameLabel = new Label(contacto.getNombre());
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", 13));

        if (isOnline) {
            userEntry.setCursor(Cursor.HAND);
            userEntry.setOnMouseClicked(event -> {
                System.out.println("🖱️ [FeatureContactos]: Clic en el contacto: " + contacto.getNombre() + " (ID: " + contacto.getId() + ")");
                // CORRECCIÓN: Se pasa el objeto DTOContacto completo al hacer clic.
                onContactoSeleccionado.accept(contacto);
            });
        }

        userEntry.getChildren().addAll(avatarContainer, nameLabel);
        return userEntry;
    }

    private void agregarAvatarPorDefecto(StackPane container, Color colorEstado) {
        Circle background = new Circle(17.5, Color.web("#7289da"));
        Circle statusIndicator = new Circle(6, colorEstado);
        statusIndicator.setStroke(Color.web("#2c2f33"));
        statusIndicator.setStrokeWidth(2);

        StackPane.setAlignment(statusIndicator, Pos.BOTTOM_RIGHT);
        container.getChildren().addAll(background, statusIndicator);
    }
}
