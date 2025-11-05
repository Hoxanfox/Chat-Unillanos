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
import observador.IObservador;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Vista para el formulario de registro de nuevos usuarios.
 * Implementa IObservador para recibir notificaciones de eventos de registro.
 */
public class VistaRegistro extends VBox implements IObservador {

    private final IControladorAutenticacion controlador;
    private File archivoFotoSeleccionada;
    private final Label etiquetaNombreArchivo;
    private final Label etiquetaError;
    private final Button btnRegistro;
    private Runnable onRegistroExitoso;

    public VistaRegistro(Runnable onRegistroExitoso, Runnable onIrALogin, IControladorAutenticacion controlador) {
        super(15);
        this.controlador = controlador;
        this.onRegistroExitoso = onRegistroExitoso;
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: #ecf0f1;");

        // Registrarse como observador
        controlador.registrarObservadorRegistro(this);
        System.out.println("✅ [VistaRegistro]: Registrada como observador de registro");

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

        btnRegistro = new Button("Registrarse");
        btnRegistro.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;");

        Hyperlink linkLogin = new Hyperlink("¿Ya tienes cuenta? Inicia Sesión");
        linkLogin.setOnAction(e -> onIrALogin.run());

        etiquetaError = new Label();
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
                        // No navegamos aquí, esperamos la notificación del observador
                    } else {
                        etiquetaError.setText("No se pudo completar el registro. El email ya podría estar en uso.");
                        btnRegistro.setDisable(false);
                    }
                });
            });
        });

        this.getChildren().addAll(titulo, campoNombre, campoEmail, campoPassword, layoutFoto, btnRegistro, linkLogin, etiquetaError);
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [VistaRegistro]: Notificación recibida - Tipo: " + tipoDeDato);

        Platform.runLater(() -> {
            switch (tipoDeDato) {
                case "REGISTRO_INICIADO":
                    System.out.println("🔄 [VistaRegistro]: Registro iniciado...");
                    etiquetaError.setText("");
                    break;

                case "REGISTRO_EXITOSO":
                    System.out.println("✅ [VistaRegistro]: Registro exitoso");
                    etiquetaError.setTextFill(Color.GREEN);
                    etiquetaError.setText("¡Registro exitoso! Redirigiendo al login...");
                    // Navegar al login después de un registro exitoso
                    if (onRegistroExitoso != null) {
                        // Pequeño delay para que el usuario vea el mensaje
                        new Thread(() -> {
                            try {
                                Thread.sleep(1500);
                                Platform.runLater(onRegistroExitoso);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                    }
                    break;

                case "REGISTRO_ERROR":
                    System.out.println("❌ [VistaRegistro]: Error en registro");
                    etiquetaError.setTextFill(Color.RED);
                    etiquetaError.setText("Error: " + datos);
                    btnRegistro.setDisable(false);
                    break;

                default:
                    System.out.println("⚠️ [VistaRegistro]: Tipo de notificación desconocido: " + tipoDeDato);
            }
        });
    }
}
