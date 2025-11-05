package interfazEscritorio.autenticacion;

import controlador.autenticacion.ControladorAutenticacion;
import controlador.autenticacion.IControladorAutenticacion;
import controlador.conexion.ControladorConexion;
import controlador.conexion.IControladorConexion;
import interfazEscritorio.autenticacion.login.VistaLogin;
import interfazEscritorio.autenticacion.registro.VistaRegistro;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.concurrent.CompletableFuture;

/**
 * Contenedor que AHORA gestiona el flujo de conexión automática.
 */
public class VistaAutenticacion extends StackPane {

    private final Runnable onAuthExitoso;
    private final IControladorConexion controladorConexion;
    private final IControladorAutenticacion controladorAutenticacion;

    public VistaAutenticacion(Runnable onAuthExitoso) {
        this.onAuthExitoso = onAuthExitoso;
        this.controladorConexion = new ControladorConexion();
        this.controladorAutenticacion = new ControladorAutenticacion();

        // Al iniciar, se muestra la pantalla de carga y se intenta conectar.
        iniciarConexionAutomatica();
    }

    /**
     * Muestra una pantalla de carga e inicia el proceso de conexión asíncrono.
     */
    private void iniciarConexionAutomatica() {
        // Vista de "Conectando..."
        VBox conectandoLayout = new VBox(20);
        conectandoLayout.setAlignment(Pos.CENTER);
        conectandoLayout.setStyle("-fx-background-color: #ecf0f1;");
        Label etiquetaConectando = new Label("Conectando con el servidor...");
        etiquetaConectando.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        ProgressIndicator progressIndicator = new ProgressIndicator();
        conectandoLayout.getChildren().addAll(etiquetaConectando, progressIndicator);

        this.getChildren().setAll(conectandoLayout);

        // Iniciar la conexión en segundo plano.
        CompletableFuture<Boolean> futuroConexion = controladorConexion.conectar();

        // Reaccionar al resultado de la conexión.
        futuroConexion.thenAccept(conectado -> {
            Platform.runLater(() -> {
                if (conectado) {
                    // Si la conexión es exitosa, mostrar la pantalla de login.
                    mostrarLogin();
                } else {
                    // Si falla, mostrar un mensaje de error.
                    mostrarErrorConexion();
                }
            });
        });
    }

    private void mostrarErrorConexion() {
        VBox errorLayout = new VBox(20);
        errorLayout.setAlignment(Pos.CENTER);
        errorLayout.setStyle("-fx-background-color: #ecf0f1;");
        Label etiquetaError = new Label("No se pudo conectar al servidor.\nVerifica el archivo 'config.txt' y tu conexión a internet.");
        etiquetaError.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        etiquetaError.setTextFill(Color.RED);

        this.getChildren().setAll(errorLayout);
    }

    private void mostrarLogin() {
        VistaLogin vistaLogin = new VistaLogin(onAuthExitoso, this::mostrarRegistro, controladorAutenticacion);
        this.getChildren().setAll(vistaLogin);
    }

    private void mostrarRegistro() {
        VistaRegistro vistaRegistro = new VistaRegistro(this::mostrarLogin, this::mostrarLogin, controladorAutenticacion);
        this.getChildren().setAll(vistaRegistro);
    }
}

