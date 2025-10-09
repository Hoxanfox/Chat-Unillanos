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

import java.util.concurrent.CompletableFuture;

/**
 * Vista para el inicio de sesión que se comunica con un controlador de forma asíncrona.
 */
public class VistaLogin extends VBox {

    private final IControladorAutenticacion controlador;

    public VistaLogin(Runnable onLoginExitoso, Runnable onIrARegistro, IControladorAutenticacion controlador) {
        super(15);
        this.controlador = controlador;
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: #ecf0f1;");

        Label titulo = new Label("Iniciar Sesión");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        TextField campoEmail = new TextField();
        campoEmail.setPromptText("Email");
        campoEmail.setMaxWidth(250);

        PasswordField campoPassword = new PasswordField();
        campoPassword.setPromptText("Contraseña");
        campoPassword.setMaxWidth(250);

        Button btnLogin = new Button("Ingresar");
        btnLogin.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px;");

        Hyperlink linkRegistro = new Hyperlink("¿No tienes cuenta? Regístrate");
        linkRegistro.setOnAction(e -> onIrARegistro.run());

        Label etiquetaError = new Label();
        etiquetaError.setTextFill(Color.RED);

        // --- LÓGICA ASÍNCRONA ---
        btnLogin.setOnAction(e -> {
            etiquetaError.setText("");
            btnLogin.setDisable(true); // Deshabilitar el botón para evitar múltiples clics
            String email = campoEmail.getText();
            String password = campoPassword.getText();

            DTOAutenticacion datosAuth = new DTOAutenticacion(email, password);

            // 1. Llamamos al método asíncrono, que nos devuelve una "promesa".
            CompletableFuture<Boolean> futuroLogin = controlador.autenticar(datosAuth);

            // 2. Le decimos qué hacer CUANDO la promesa se complete.
            futuroLogin.thenAccept(fueExitoso -> {
                // Este código se ejecutará cuando llegue la respuesta del servidor.

                // 3. ¡IMPORTANTE! Las actualizaciones de la UI deben hacerse en el hilo de JavaFX.
                Platform.runLater(() -> {
                    if (fueExitoso) {
                        System.out.println("Login exitoso para el usuario: " + email);
                        onLoginExitoso.run(); // Navegar al Lobby
                    } else {
                        System.out.println("Intento de login fallido.");
                        etiquetaError.setText("Email o contraseña incorrectos.");
                    }
                    btnLogin.setDisable(false); // Volver a habilitar el botón
                });
            });
        });

        this.getChildren().addAll(titulo, campoEmail, campoPassword, btnLogin, linkRegistro, etiquetaError);
    }
}

