package interfazEscritorio.dashboard.featureCanales.canal.miembrosCanal;

import controlador.canales.IControladorCanales;
import controlador.contactos.IControladorContactos;
import dto.featureContactos.DTOContacto;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import observador.IObservador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Vista para invitar miembros a un canal.
 * Implementa IObservador para recibir la lista de contactos disponibles.
 */
public class VistaInvitarMiembro extends BorderPane implements IObservador {

    private final IControladorCanales controladorCanales;
    private final IControladorContactos controladorContactos;
    private final String canalId;
    private final VBox contactosBox;
    private final List<String> contactosSeleccionados;
    private final Label etiquetaEstado;

    public VistaInvitarMiembro(String canalId, String nombreCanal, Runnable onVolver, IControladorContactos controladorContactos, IControladorCanales controladorCanales) {
        this.controladorCanales = controladorCanales;
        this.controladorContactos = controladorContactos;
        this.canalId = canalId;
        this.contactosSeleccionados = new ArrayList<>();
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f4f4f4;");

        // Registrarse como observador de contactos
        controladorContactos.registrarObservador(this);
        System.out.println("✅ [VistaInvitarMiembro]: Registrada como observador de contactos");

        VBox mainLayout = new VBox(15);
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // Header
        Label titulo = new Label("Invite Members to " + nombreCanal);
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        mainLayout.getChildren().add(titulo);

        // Barra de búsqueda
        TextField campoBusqueda = new TextField();
        campoBusqueda.setPromptText("Search users to invite...");
        campoBusqueda.textProperty().addListener((obs, oldVal, newVal) -> filtrarContactos(newVal));
        mainLayout.getChildren().add(campoBusqueda);

        // Lista de contactos
        contactosBox = new VBox(10);
        contactosBox.setPadding(new Insets(10));
        contactosBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");
        Label subtitulo = new Label("AVAILABLE CONTACTS");
        subtitulo.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        subtitulo.setTextFill(Color.GRAY);
        contactosBox.getChildren().add(subtitulo);

        Label cargando = new Label("Cargando contactos...");
        cargando.setTextFill(Color.GRAY);
        contactosBox.getChildren().add(cargando);

        ScrollPane scrollPane = new ScrollPane(contactosBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        mainLayout.getChildren().add(scrollPane);

        etiquetaEstado = new Label();
        etiquetaEstado.setTextFill(Color.RED);
        mainLayout.getChildren().add(etiquetaEstado);

        this.setCenter(mainLayout);

        // Botones de acción
        HBox botonesBox = new HBox(10);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);
        botonesBox.setPadding(new Insets(15, 0, 0, 0));

        Button btnCancelar = new Button("Cancel");
        btnCancelar.setOnAction(e -> onVolver.run());

        Button btnInvitar = new Button("Invite Selected");
        btnInvitar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnInvitar.setOnAction(e -> invitarSeleccionados());

        botonesBox.getChildren().addAll(btnCancelar, btnInvitar);
        this.setBottom(botonesBox);

        // Solicitar contactos
        controladorContactos.solicitarActualizacionContactos();
        System.out.println("📡 [VistaInvitarMiembro]: Solicitando lista de contactos...");
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [VistaInvitarMiembro]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            if ("ACTUALIZAR_CONTACTOS".equals(tipoDeDato) && datos instanceof List) {
                List<DTOContacto> contactos = (List<DTOContacto>) datos;
                System.out.println("✅ [VistaInvitarMiembro]: Actualizando lista con " + contactos.size() + " contactos");
                cargarContactos(contactos);
            }
        });
    }

    private void cargarContactos(List<DTOContacto> contactos) {
        contactosBox.getChildren().clear();

        Label subtitulo = new Label("AVAILABLE CONTACTS");
        subtitulo.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        subtitulo.setTextFill(Color.GRAY);
        contactosBox.getChildren().add(subtitulo);

        if (contactos.isEmpty()) {
            Label sinContactos = new Label("No hay contactos disponibles");
            sinContactos.setTextFill(Color.GRAY);
            contactosBox.getChildren().add(sinContactos);
        } else {
            for (DTOContacto contacto : contactos) {
                contactosBox.getChildren().add(crearEntradaContacto(contacto));
            }
        }
    }

    private HBox crearEntradaContacto(DTOContacto contacto) {
        HBox entrada = new HBox(10);
        entrada.setAlignment(Pos.CENTER_LEFT);
        entrada.setUserData(contacto); // Guardar el contacto completo

        CheckBox checkBox = new CheckBox();
        checkBox.setOnAction(e -> {
            if (checkBox.isSelected()) {
                contactosSeleccionados.add(contacto.getId());
                System.out.println("✅ Seleccionado: " + contacto.getNombre());
            } else {
                contactosSeleccionados.remove(contacto.getId());
                System.out.println("❌ Deseleccionado: " + contacto.getNombre());
            }
        });

        boolean online = "Online".equalsIgnoreCase(contacto.getEstado());
        Circle status = new Circle(5, online ? Color.GREEN : Color.LIGHTGRAY);

        VBox infoBox = new VBox();
        Label nombreLabel = new Label(contacto.getNombre());
        nombreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        Label estadoLabel = new Label("Status: " + contacto.getEstado());
        estadoLabel.setTextFill(Color.GRAY);
        estadoLabel.setFont(Font.font("Arial", 10));
        infoBox.getChildren().addAll(nombreLabel, estadoLabel);

        entrada.getChildren().addAll(checkBox, status, infoBox);
        return entrada;
    }

    private void filtrarContactos(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            // Mostrar todos los contactos
            contactosBox.getChildren().forEach(node -> node.setVisible(true));
        } else {
            // Filtrar por nombre
            contactosBox.getChildren().forEach(node -> {
                if (node instanceof HBox) {
                    HBox entrada = (HBox) node;
                    Object userData = entrada.getUserData();
                    if (userData instanceof DTOContacto) {
                        DTOContacto contacto = (DTOContacto) userData;
                        boolean matches = contacto.getNombre().toLowerCase().contains(filtro.toLowerCase());
                        entrada.setVisible(matches);
                        entrada.setManaged(matches);
                    }
                }
            });
        }
    }

    private void invitarSeleccionados() {
        if (contactosSeleccionados.isEmpty()) {
            etiquetaEstado.setTextFill(Color.RED);
            etiquetaEstado.setText("Por favor selecciona al menos un contacto");
            return;
        }

        etiquetaEstado.setTextFill(Color.BLUE);
        etiquetaEstado.setText("Enviando invitaciones...");
        System.out.println("📤 [VistaInvitarMiembro]: Enviando " + contactosSeleccionados.size() + " invitaciones");

        List<CompletableFuture<Void>> invitaciones = new ArrayList<>();

        for (String contactoId : contactosSeleccionados) {
            CompletableFuture<Void> futuro = controladorCanales.invitarMiembro(canalId, contactoId);
            invitaciones.add(futuro);
        }

        CompletableFuture.allOf(invitaciones.toArray(new CompletableFuture[0]))
            .thenRun(() -> Platform.runLater(() -> {
                etiquetaEstado.setTextFill(Color.GREEN);
                etiquetaEstado.setText("¡Invitaciones enviadas exitosamente!");
                System.out.println("✅ [VistaInvitarMiembro]: Todas las invitaciones enviadas");

                // Limpiar selección
                contactosSeleccionados.clear();
                contactosBox.getChildren().forEach(node -> {
                    if (node instanceof HBox) {
                        HBox entrada = (HBox) node;
                        entrada.getChildren().stream()
                            .filter(child -> child instanceof CheckBox)
                            .forEach(child -> ((CheckBox) child).setSelected(false));
                    }
                });
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    etiquetaEstado.setTextFill(Color.RED);
                    etiquetaEstado.setText("Error al enviar invitaciones: " + ex.getMessage());
                    System.err.println("❌ [VistaInvitarMiembro]: Error: " + ex.getMessage());
                });
                return null;
            });
    }
}
