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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import dto.featureContactos.DTOContacto;

import java.util.List;
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

    public FeatureContactos(Consumer<DTOContacto> onContactoSeleccionado, IControladorContactos controladorContactos) {
        super(10);
        this.onContactoSeleccionado = onContactoSeleccionado;
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
        }
    }

    private void redibujarContactos(List<DTOContacto> contactos) {
        if (contactos == null) return;

        System.out.println("  -> [UI Thread]: Limpiando y redibujando la lista con " + contactos.size() + " contactos.");
        listaContactosContainer.getChildren().clear();
        tituloUsuarios.setText("ONLINE USERS (" + contactos.size() + ")");

        for (DTOContacto contacto : contactos) {
            listaContactosContainer.getChildren().add(crearEntradaUsuario(contacto));
        }
    }

    private Node crearEntradaUsuario(DTOContacto contacto) {
        HBox userEntry = new HBox(10);
        userEntry.setAlignment(Pos.CENTER_LEFT);

        boolean isOnline = "Online".equalsIgnoreCase(contacto.getEstado());
        Color colorEstado = isOnline ? Color.GREEN : Color.LIGHTGRAY;

        Circle statusIndicator = new Circle(5, colorEstado);
        Label nameLabel = new Label(contacto.getNombre());
        nameLabel.setTextFill(Color.WHITE);

        if (isOnline) {
            userEntry.setCursor(Cursor.HAND);
            userEntry.setOnMouseClicked(event -> {
                System.out.println("🖱️ [FeatureContactos]: Clic en el contacto: " + contacto.getNombre() + " (ID: " + contacto.getId() + ")");
                // CORRECCIÓN: Se pasa el objeto DTOContacto completo al hacer clic.
                onContactoSeleccionado.accept(contacto);
            });
        }

        userEntry.getChildren().addAll(statusIndicator, nameLabel);
        return userEntry;
    }
}

