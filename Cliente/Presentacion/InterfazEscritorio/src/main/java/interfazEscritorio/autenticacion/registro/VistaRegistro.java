package interfazEscritorio.autenticacion.registro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Representa la interfaz de usuario para el registro de nuevos usuarios.
 */
public class VistaRegistro extends VBox {

    /**
     * Constructor que inicializa la interfaz de registro.
     * @param onRegistroExitoso Callback que se ejecuta cuando el registro es exitoso.
     * @param onIrALogin Callback para navegar de vuelta a la vista de login.
     */
    public VistaRegistro(Runnable onRegistroExitoso, Runnable onIrALogin) {
        super(15);
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: #ecf0f1;");

        Label titulo = new Label("Crear Cuenta");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        TextField campoUsuario = new TextField();
        campoUsuario.setPromptText("Elige un usuario");
        campoUsuario.setMaxWidth(250);

        PasswordField campoPassword = new PasswordField();
        campoPassword.setPromptText("Crea una contraseña");
        campoPassword.setMaxWidth(250);

        PasswordField campoConfirmarPassword = new PasswordField();
        campoConfirmarPassword.setPromptText("Confirma la contraseña");
        campoConfirmarPassword.setMaxWidth(250);

        Button btnRegistro = new Button("Registrarse");
        btnRegistro.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;");

        Hyperlink linkLogin = new Hyperlink("¿Ya tienes cuenta? Inicia sesión");
        linkLogin.setOnAction(e -> onIrALogin.run());

        // Llama al callback de éxito cuando el botón es presionado.
        btnRegistro.setOnAction(e -> {
            System.out.println("Intento de registro...");
            onRegistroExitoso.run(); // Simulación de registro exitoso.
        });

        this.getChildren().addAll(titulo, campoUsuario, campoPassword, campoConfirmarPassword, btnRegistro, linkLogin);
    }
}
