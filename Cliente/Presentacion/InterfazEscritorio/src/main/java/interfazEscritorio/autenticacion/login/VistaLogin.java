package interfazEscritorio.autenticacion.login;

import controlador.autenticacion.IControladorAutenticacion;
import dto.vistaLogin.DTOAutenticacion;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import observador.IObservador;

import java.util.concurrent.CompletableFuture;

/**
 * Vista para el inicio de sesión que se comunica con un controlador de forma asíncrona.
 * Implementa IObservador para recibir notificaciones de eventos de autenticación.
 */
public class VistaLogin extends VBox implements IObservador {

    private final IControladorAutenticacion controlador;
    private final Label etiquetaError;
    private final Button btnLogin;
    private Runnable onLoginExitoso;

    public VistaLogin(Runnable onLoginExitoso, Runnable onIrARegistro, IControladorAutenticacion controlador) {
        super(15);
        this.controlador = controlador;
        this.onLoginExitoso = onLoginExitoso;
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: #ecf0f1;");

        // Registrarse como observador
        controlador.registrarObservadorAutenticacion(this);
        System.out.println("✅ [VistaLogin]: Registrada como observador de autenticación");

        Label titulo = new Label("Iniciar Sesión");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        TextField campoEmail = new TextField();
        campoEmail.setPromptText("Email");
        campoEmail.setMaxWidth(250);

        PasswordField campoPassword = new PasswordField();
        campoPassword.setPromptText("Contraseña");
        campoPassword.setMaxWidth(250);

        btnLogin = new Button("Ingresar");
        btnLogin.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px;");

        Hyperlink linkRegistro = new Hyperlink("¿No tienes cuenta? Regístrate");
        linkRegistro.setOnAction(e -> onIrARegistro.run());

        etiquetaError = new Label();
        etiquetaError.setTextFill(Color.RED);

        // --- LÓGICA ASÍNCRONA ---
        btnLogin.setOnAction(e -> {
            etiquetaError.setText("");
            btnLogin.setDisable(true);
            String email = campoEmail.getText();
            String password = campoPassword.getText();

            DTOAutenticacion datosAuth = new DTOAutenticacion(email, password);

            CompletableFuture<Boolean> futuroLogin = controlador.autenticar(datosAuth);

            futuroLogin.thenAccept(fueExitoso -> {
                Platform.runLater(() -> {
                    if (fueExitoso) {
                        System.out.println("Login exitoso para el usuario: " + email);
                        // No navegamos aquí, esperamos la notificación del observador
                    } else {
                        System.out.println("Intento de login fallido.");
                        etiquetaError.setText("Email o contraseña incorrectos.");
                        btnLogin.setDisable(false);
                    }
                });
            });
        });

        this.getChildren().addAll(titulo, campoEmail, campoPassword, btnLogin, linkRegistro, etiquetaError);
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [VistaLogin]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "AUTENTICACION_INICIADA":
                    System.out.println("🔄 [VistaLogin]: Autenticación iniciada...");
                    etiquetaError.setText("");
                    break;

                case "AUTENTICACION_EXITOSA":
                    System.out.println("✅ [VistaLogin]: Autenticación exitosa");
                    etiquetaError.setTextFill(Color.GREEN);
                    etiquetaError.setText("¡Inicio de sesión exitoso!");
                    break;

                case "USUARIO_LOGUEADO":
                    System.out.println("✅ [VistaLogin]: Usuario logueado, navegando al Lobby...");
                    // Navegar al lobby cuando el usuario esté completamente logueado
                    if (onLoginExitoso != null) {
                        onLoginExitoso.run();
                    }
                    break;

                case "USUARIO_BANEADO":
                    System.out.println("⚠️ [VistaLogin]: Usuario baneado");
                    etiquetaError.setTextFill(Color.RED);
                    etiquetaError.setText("Tu cuenta ha sido suspendida. Razón: " + datos);
                    btnLogin.setDisable(false);
                    break;

                case "AUTENTICACION_ERROR":
                    System.out.println("❌ [VistaLogin]: Error en autenticación");
                    etiquetaError.setTextFill(Color.RED);
                    etiquetaError.setText("Error: " + datos);
                    btnLogin.setDisable(false);
                    break;

                default:
                    System.out.println("⚠️ [VistaLogin]: Tipo de notificación desconocido: " + tipoDeDato);
            }
        });
    }
}
