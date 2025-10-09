package interfazEscritorio.autenticacion.registro;

import controlador.autenticacion.IControladorAutenticacion;
import dto.vistaRegistro.DTOFormularioRegistro;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Vista para el formulario de registro de nuevos usuarios.
 */
public class VistaRegistro extends VBox {

    private final IControladorAutenticacion controlador;
    private File archivoFotoSeleccionada;
    private final Label etiquetaNombreArchivo;

    public VistaRegistro(Runnable onRegistroExitoso, Runnable onIrALogin, IControladorAutenticacion controlador) {
        super(15);
        this.controlador = controlador;
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: #ecf0f1;");

        Label titulo = new Label("Crear Cuenta");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        TextField campoNombre = new TextField();
        campoNombre.setPromptText("Nombre Completo");
        campoNombre.setMaxWidth(300);

        TextField campoEmail = new TextField();
        campoEmail.setPromptText("Email");
        campoEmail.setMaxWidth(300);

        PasswordField campoPassword = new PasswordField();
        campoPassword.setPromptText("Contraseña");
        campoPassword.setMaxWidth(300);

        // Selector de archivo para la foto
        Button btnSeleccionarFoto = new Button("Seleccionar Foto de Perfil");
        etiquetaNombreArchivo = new Label("Ningún archivo seleccionado");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
        );
        btnSeleccionarFoto.setOnAction(e -> {
            archivoFotoSeleccionada = fileChooser.showOpenDialog(this.getScene().getWindow());
            if (archivoFotoSeleccionada != null) {
                etiquetaNombreArchivo.setText(archivoFotoSeleccionada.getName());
            }
        });
        HBox layoutFoto = new HBox(10, btnSeleccionarFoto, etiquetaNombreArchivo);
        layoutFoto.setAlignment(Pos.CENTER_LEFT);
        layoutFoto.setMaxWidth(300);


        Button btnRegistro = new Button("Registrarse");
        btnRegistro.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;");

        Hyperlink linkLogin = new Hyperlink("¿Ya tienes cuenta? Inicia Sesión");
        linkLogin.setOnAction(e -> onIrALogin.run());

        Label etiquetaError = new Label();
        etiquetaError.setTextFill(Color.RED);

        btnRegistro.setOnAction(e -> {
            etiquetaError.setText("");
            btnRegistro.setDisable(true);

            String nombre = campoNombre.getText();
            String email = campoEmail.getText();
            String password = campoPassword.getText();
            // Aquí se obtendría la IP real
            String ip = "127.0.0.1";

            DTOFormularioRegistro datosFormulario = new DTOFormularioRegistro(nombre, email, password, archivoFotoSeleccionada, ip);

            CompletableFuture<Boolean> futuroRegistro = controlador.registrar(datosFormulario);

            futuroRegistro.thenAccept(fueExitoso -> {
                Platform.runLater(() -> {
                    if (fueExitoso) {
                        System.out.println("Registro exitoso para: " + email);
                        onRegistroExitoso.run();
                    } else {
                        etiquetaError.setText("No se pudo completar el registro. El email ya podría estar en uso.");
                    }
                    btnRegistro.setDisable(false);
                });
            });
        });


        this.getChildren().addAll(titulo, campoNombre, campoEmail, campoPassword, layoutFoto, btnRegistro, linkLogin, etiquetaError);
    }
}

