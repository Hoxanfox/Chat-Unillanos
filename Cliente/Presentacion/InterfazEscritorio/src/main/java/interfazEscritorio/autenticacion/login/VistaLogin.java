package interfazEscritorio.autenticacion.login;

import controlador.autenticacion.IControladorAutenticacion;
import dto.vistaLogin.DTOAutenticacion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Vista para el inicio de sesión que se comunica con un controlador.
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
        campoEmail.setPromptText("Email"); // Actualizado para pedir el email
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

        btnLogin.setOnAction(e -> {
            etiquetaError.setText("");
            String email = campoEmail.getText();
            String password = campoPassword.getText();

            // Se empaquetan los datos en el DTO usando el constructor actualizado.
            DTOAutenticacion datosAuth = new DTOAutenticacion(email, password);

            if (controlador.autenticar(datosAuth)) {
                System.out.println("Login exitoso para el usuario: " + email);
                onLoginExitoso.run();
            } else {
                System.out.println("Intento de login fallido.");
                etiquetaError.setText("Email o contraseña incorrectos."); // Mensaje de error actualizado
            }
        });

        this.getChildren().addAll(titulo, campoEmail, campoPassword, btnLogin, linkRegistro, etiquetaError);
    }
}

