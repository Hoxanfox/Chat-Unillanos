package interfazEscritorio.dashboard.featureContactos;

import controlador.contactos.IControladorContactos;
import observador.IObservador;
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
 * Implementa IObservador para actualizarse dinámicamente a través del Controlador.
 */
public class FeatureContactos extends VBox implements IObservador {

    private final Consumer<String> onContactoSeleccionado;
    private final Label tituloUsuarios;
    private final VBox listaContactosContainer;

    // Ahora depende del Controlador, no del Servicio.
    public FeatureContactos(Consumer<String> onContactoSeleccionado, IControladorContactos controladorContactos) {
        super(10);
        this.onContactoSeleccionado = onContactoSeleccionado;
        this.setPadding(new Insets(15, 15, 10, 15));

        // 1. Suscribirse a los cambios a través del Controlador.
        controladorContactos.registrarObservador(this);

        tituloUsuarios = new Label("ONLINE USERS (0)");
        tituloUsuarios.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        tituloUsuarios.setTextFill(Color.WHITE);

        listaContactosContainer = new VBox(10);

        this.getChildren().addAll(tituloUsuarios, listaContactosContainer);

        // 2. Obtener la lista inicial a través del Controlador.
        redibujarContactos(controladorContactos.getContactos());
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        if ("ACTUALIZAR_CONTACTOS".equals(tipoDeDato) && datos instanceof List) {
            Platform.runLater(() -> {
                redibujarContactos((List<DTOContacto>) datos);
            });
        }
    }

    private void redibujarContactos(List<DTOContacto> contactos) {
        if (contactos == null) return;

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
                onContactoSeleccionado.accept(contacto.getNombre());
            });
        }

        userEntry.getChildren().addAll(statusIndicator, nameLabel);
        return userEntry;
    }
}

