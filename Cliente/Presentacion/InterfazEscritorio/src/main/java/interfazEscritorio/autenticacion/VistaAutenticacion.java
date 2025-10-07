package interfazEscritorio.autenticacion;

import controlador.autenticacion.ControladorAutenticacion;
import controlador.autenticacion.IControladorAutenticacion;
import controlador.conexion.ControladorConexion;
import controlador.conexion.IControladorConexion;
import interfazEscritorio.autenticacion.login.VistaLogin;
import interfazEscritorio.autenticacion.registro.VistaRegistro;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Contenedor principal que gestiona todo el flujo de autenticación.
 */
public class VistaAutenticacion extends StackPane {

    private final Runnable onAuthExitoso;
    private final IControladorConexion controladorConexion;
    private final IControladorAutenticacion controladorAutenticacion; // 1. Se declara el controlador de autenticación

    public VistaAutenticacion(Runnable onAuthExitoso) {
        this.onAuthExitoso = onAuthExitoso;
        this.controladorConexion = new ControladorConexion();
        this.controladorAutenticacion = new ControladorAutenticacion(); // 2. Se instancia el controlador

        mostrarSeleccionServidor();
    }

    private void mostrarSeleccionServidor() {
        Node vistaServidor = crearVistaServidor();
        this.getChildren().setAll(vistaServidor);
    }

    private Node crearVistaServidor() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #ecf0f1;");

        Label titulo = new Label("Conectar al Servidor");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        TextField campoIpServidor = new TextField("127.0.0.1");
        campoIpServidor.setPromptText("Dirección IP del servidor");
        campoIpServidor.setMaxWidth(250);

        Button btnConectar = new Button("Conectar");
        btnConectar.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;"
        );

        Label etiquetaEstado = new Label();

        btnConectar.setOnAction(e -> {
            String ip = campoIpServidor.getText();
            if (controladorConexion.conectar(ip)) {
                mostrarLogin();
            } else {
                etiquetaEstado.setTextFill(Color.RED);
                etiquetaEstado.setText("No se pudo conectar al servidor.");
            }
        });

        layout.getChildren().addAll(titulo, campoIpServidor, btnConectar, etiquetaEstado);
        return layout;
    }

    private void mostrarLogin() {
        // 3. Se pasa la instancia del controlador como tercer argumento.
        VistaLogin vistaLogin = new VistaLogin(onAuthExitoso, this::mostrarRegistro, controladorAutenticacion);
        this.getChildren().setAll(vistaLogin);
    }

    private void mostrarRegistro() {
        // Nota: Si la vista de registro también necesita un controlador,
        // habría que crearlo y pasarlo aquí también.
        VistaRegistro vistaRegistro = new VistaRegistro(onAuthExitoso, this::mostrarLogin);
        this.getChildren().setAll(vistaRegistro);
    }
}

