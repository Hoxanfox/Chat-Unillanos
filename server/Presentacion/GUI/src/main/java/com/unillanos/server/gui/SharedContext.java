package com.unillanos.server.gui;

import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

/**
 * Contexto compartido para acceder a Spring ApplicationContext y referencias globales desde la GUI.
 */
public final class SharedContext {

    private static ApplicationContext context;
    private static Stage primaryStage;

    private SharedContext() { }

    public static void set(ApplicationContext ctx) {
        context = ctx;
    }

    public static ApplicationContext get() {
        return context;
    }
    
    /**
     * Establece la referencia al Stage principal de la aplicación.
     * Necesario para mostrar toast notifications.
     *
     * @param stage Stage principal de JavaFX
     */
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }
    
    /**
     * Obtiene la referencia al Stage principal de la aplicación.
     *
     * @return Stage principal o null si no se ha establecido
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}


