package interfazEscritorio.dashboard.featureHeader;

import controlador.notificaciones.IControladorNotificaciones;
import controlador.usuario.IControladorUsuario;
import dto.featureNotificaciones.DTONotificacion;
import dto.vistaLobby.DTOUsuario;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import observador.IObservador;

import java.util.List;

/**
 * Barra de encabezado con información del usuario y controles de sesión.
 * Implementa IObservador para actualizar el contador de notificaciones.
 */
public class FeatureHeader extends BorderPane implements IObservador {

    private final Label notificacionesIcono;
    private final Label nombreUsuarioLabel;
    private final Label estadoUsuarioLabel;
    private final ImageView avatarImageView;
    private final IControladorUsuario controladorUsuario;
    private final Runnable onLogoutSuccess;

    public FeatureHeader(
            IControladorNotificaciones controladorNotificaciones,
            IControladorUsuario controladorUsuario,
            Runnable onLogoutSuccess
    ) {
        this.controladorUsuario = controladorUsuario;
        this.onLogoutSuccess = onLogoutSuccess;

        this.setPadding(new Insets(10, 15, 10, 15));
        this.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 0 2 0;");

        // Registrarse como observador de notificaciones
        controladorNotificaciones.registrarObservador(this);
        System.out.println("✅ [FeatureHeader]: Registrado como observador de notificaciones");

        // --- Información del Usuario (Izquierda) ---
        HBox infoUsuario = new HBox(15);
        infoUsuario.setAlignment(Pos.CENTER_LEFT);

        // Avatar con ImageView circular
        avatarImageView = new ImageView();
        avatarImageView.setFitWidth(40);
        avatarImageView.setFitHeight(40);
        avatarImageView.setPreserveRatio(true);
        avatarImageView.setSmooth(true);

        // Hacer el ImageView circular
        Circle clip = new Circle(20, 20, 20);
        avatarImageView.setClip(clip);

        // Por defecto sin imagen (transparente)
        avatarImageView.setStyle("-fx-background-color: #34495e;");

        nombreUsuarioLabel = new Label("Cargando...");
        nombreUsuarioLabel.setFont(Font.font("System", 14));
        nombreUsuarioLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        estadoUsuarioLabel = new Label("●");
        estadoUsuarioLabel.setFont(Font.font(12));
        estadoUsuarioLabel.setStyle("-fx-text-fill: #2ecc71;"); // Verde por defecto

        infoUsuario.getChildren().addAll(avatarImageView, nombreUsuarioLabel, estadoUsuarioLabel);

        // --- Controles (Derecha) ---
        HBox controlesLayout = new HBox(20);
        controlesLayout.setAlignment(Pos.CENTER_RIGHT);

        notificacionesIcono = new Label("🔔 0");
        notificacionesIcono.setFont(Font.font(16));
        notificacionesIcono.setStyle("-fx-text-fill: white; -fx-cursor: hand;");

        // Botón de cerrar sesión
        Button btnCerrarSesion = new Button("Cerrar Sesión");
        btnCerrarSesion.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 16; " +
            "-fx-cursor: hand; " +
            "-fx-background-radius: 5;"
        );
        btnCerrarSesion.setOnMouseEntered(e ->
            btnCerrarSesion.setStyle(btnCerrarSesion.getStyle() + "-fx-background-color: #c0392b;")
        );
        btnCerrarSesion.setOnMouseExited(e ->
            btnCerrarSesion.setStyle(btnCerrarSesion.getStyle().replace("-fx-background-color: #c0392b;", "-fx-background-color: #e74c3c;"))
        );
        btnCerrarSesion.setOnAction(e -> cerrarSesion());

        controlesLayout.getChildren().addAll(notificacionesIcono, btnCerrarSesion);

        this.setLeft(infoUsuario);
        this.setRight(controlesLayout);

        // Cargar información del usuario
        cargarInformacionUsuario();

        // Solicitar notificaciones iniciales
        controladorNotificaciones.solicitarActualizacionNotificaciones();
    }

    private void cargarInformacionUsuario() {
        System.out.println("🔄 [FeatureHeader]: Cargando información del usuario...");

        controladorUsuario.cargarInformacionUsuarioLogueado()
            .thenAccept(usuario -> Platform.runLater(() -> {
                actualizarInfoUsuario(usuario);
            }))
            .exceptionally(error -> {
                Platform.runLater(() -> {
                    nombreUsuarioLabel.setText("Error al cargar usuario");
                    System.err.println("❌ [FeatureHeader]: Error al cargar usuario: " + error.getMessage());
                });
                return null;
            });
    }

    private void actualizarInfoUsuario(DTOUsuario usuario) {
        nombreUsuarioLabel.setText(usuario.getNombre());

        // Actualizar color del estado
        String estado = usuario.getEstado();
        if (estado != null) {
            switch (estado.toLowerCase()) {
                case "activo":
                case "online":
                    estadoUsuarioLabel.setStyle("-fx-text-fill: #2ecc71;"); // Verde
                    estadoUsuarioLabel.setText("● En línea");
                    break;
                case "inactivo":
                case "offline":
                    estadoUsuarioLabel.setStyle("-fx-text-fill: #95a5a6;"); // Gris
                    estadoUsuarioLabel.setText("● Desconectado");
                    break;
                case "baneado":
                case "banned":
                    estadoUsuarioLabel.setStyle("-fx-text-fill: #e74c3c;"); // Rojo
                    estadoUsuarioLabel.setText("● Baneado");
                    break;
                default:
                    estadoUsuarioLabel.setStyle("-fx-text-fill: #f39c12;"); // Naranja
                    estadoUsuarioLabel.setText("● " + estado);
            }
        }

        // ✅ Cargar avatar usando la arquitectura de capas
        if (usuario.getAvatarUrl() != null && !usuario.getAvatarUrl().isEmpty()) {
            cargarAvatar(usuario.getAvatarUrl());
        } else {
            System.out.println("⚠️ [FeatureHeader]: Usuario sin avatarUrl");
        }

        System.out.println("✅ [FeatureHeader]: Información del usuario actualizada - " + usuario.getNombre());
    }

    /**
     * Carga el avatar siguiendo la arquitectura de capas:
     * Vista → Controlador → Servicio → Fachada → Gestor de Archivos
     */
    private void cargarAvatar(String fileId) {
        System.out.println("📸 [FeatureHeader]: Solicitando carga de avatar - FileId: " + fileId);

        controladorUsuario.obtenerFotoPerfil(fileId)
            .thenAccept(archivoFoto -> {
                if (archivoFoto != null && archivoFoto.exists()) {
                    Platform.runLater(() -> {
                        try {
                            Image image = new Image(archivoFoto.toURI().toString(), true);
                            avatarImageView.setImage(image);
                            System.out.println("✅ [FeatureHeader]: Avatar cargado exitosamente");
                        } catch (Exception e) {
                            System.err.println("❌ [FeatureHeader]: Error al crear imagen: " + e.getMessage());
                        }
                    });
                } else {
                    System.out.println("⚠️ [FeatureHeader]: Archivo de avatar no disponible");
                }
            })
            .exceptionally(error -> {
                System.err.println("❌ [FeatureHeader]: Error al cargar avatar: " + error.getMessage());
                return null;
            });
    }

    private void cerrarSesion() {
        System.out.println("🚪 [FeatureHeader]: Cerrando sesión...");

        controladorUsuario.cerrarSesion()
            .thenAccept(exitoso -> Platform.runLater(() -> {
                if (exitoso) {
                    System.out.println("✅ [FeatureHeader]: Sesión cerrada exitosamente");
                    if (onLogoutSuccess != null) {
                        onLogoutSuccess.run();
                    }
                } else {
                    System.err.println("⚠️ [FeatureHeader]: Error al cerrar sesión");
                }
            }))
            .exceptionally(error -> {
                Platform.runLater(() -> {
                    System.err.println("❌ [FeatureHeader]: Error: " + error.getMessage());
                });
                return null;
            });
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [FeatureHeader]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            if ("ACTUALIZAR_NOTIFICACIONES".equals(tipoDeDato) && datos instanceof List) {
                @SuppressWarnings("unchecked")
                List<DTONotificacion> notificaciones = (List<DTONotificacion>) datos;
                long noLeidas = notificaciones.stream().filter(n -> !n.isLeida()).count();
                notificacionesIcono.setText("🔔 " + noLeidas);
                System.out.println("✅ [FeatureHeader]: Contador actualizado - " + noLeidas + " no leídas");
            }
        });
    }
}
