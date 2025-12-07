package interfazEscritorio.app;

import interfazEscritorio.autenticacion.VistaAutenticacion;
import interfazEscritorio.dashboard.VistaLobby;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Clase principal que renderiza la ventana de la aplicación de escritorio.
 * Gestiona el cambio entre la vista de autenticación y el panel principal (Lobby).
 */
public class VentanaPrincipal extends Application {

    private BorderPane root;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();

        // Iniciar la aplicación mostrando la vista de autenticación
        mostrarVistaAutenticacion();

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Mi Aplicación de Escritorio");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Muestra la vista de autenticación, que contiene tanto el login como el registro.
     */
    private void mostrarVistaAutenticacion() {
        // Se crea la vista de autenticación y se le pasa un callback
        // que se ejecutará cuando el login o registro sea exitoso.
        VistaAutenticacion vistaAuth = new VistaAutenticacion(this::mostrarLobby);
        root.setCenter(vistaAuth);
        root.setLeft(null); // Nos aseguramos que no haya menú lateral
    }

    /**
     * Muestra la vista del Lobby después de una autenticación exitosa.
     */
    private void mostrarLobby() {
        // Crea una instancia de la nueva VistaLobby y la coloca como la vista principal.
        VistaLobby lobby = new VistaLobby();
        root.setCenter(lobby);
        root.setLeft(null); // Se limpia por si acaso, aunque VistaLobby ya gestiona su propio layout.
    }
}

