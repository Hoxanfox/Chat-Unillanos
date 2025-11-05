package com.unillanos.server.gui.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Componente de notificación toast con animaciones para feedback no bloqueante.
 * Se muestra en la esquina superior derecha de la ventana principal.
 */
public class ToastNotification {
    
    public enum Type { 
        SUCCESS, 
        ERROR, 
        INFO, 
        WARNING 
    }
    
    private static final double FADE_DURATION = 300; // ms
    private static final double DISPLAY_DURATION = 3000; // ms
    private static final double MAX_WIDTH = 400;
    private static final double TOAST_HEIGHT = 60;
    
    /**
     * Muestra una notificación toast con el mensaje y tipo especificados.
     *
     * @param owner Ventana padre (Stage principal)
     * @param message Mensaje a mostrar
     * @param type Tipo de notificación (SUCCESS, ERROR, INFO, WARNING)
     */
    public static void show(Stage owner, String message, Type type) {
        if (owner == null) {
            System.err.println("ToastNotification: No se puede mostrar toast sin Stage padre");
            return;
        }
        
        // Crear Stage para el toast
        Stage toastStage = new Stage();
        toastStage.initOwner(owner);
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);
        toastStage.setResizable(false);
        
        // Crear contenido del toast
        Label label = new Label(message);
        label.setStyle(getStyleForType(type));
        label.setPadding(new Insets(10, 20, 10, 20));
        label.setMaxWidth(MAX_WIDTH);
        label.setWrapText(true);
        label.setPrefHeight(TOAST_HEIGHT);
        
        // Crear contenedor principal
        StackPane root = new StackPane(label);
        root.setStyle("-fx-background-radius: 8;");
        root.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.3)));
        root.setOpacity(0);
        
        // Configurar escena
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);
        
        // Posicionar en la esquina superior derecha
        double x = owner.getX() + owner.getWidth() - MAX_WIDTH - 20;
        double y = owner.getY() + 20;
        toastStage.setX(x);
        toastStage.setY(y);
        
        // Ajustar tamaño automáticamente
        root.layout();
        toastStage.sizeToScene();
        
        // Mostrar el toast
        toastStage.show();
        
        // Animación fade-in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(FADE_DURATION), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        // Auto-cerrar después de DISPLAY_DURATION
        PauseTransition delay = new PauseTransition(Duration.millis(DISPLAY_DURATION));
        delay.setOnFinished(e -> {
            // Animación fade-out
            FadeTransition fadeOut = new FadeTransition(Duration.millis(FADE_DURATION), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(evt -> toastStage.close());
            fadeOut.play();
        });
        delay.play();
    }
    
    /**
     * Obtiene el estilo CSS para cada tipo de notificación.
     *
     * @param type Tipo de notificación
     * @return String con el estilo CSS
     */
    private static String getStyleForType(Type type) {
        return switch(type) {
            case SUCCESS -> "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;";
            case ERROR -> "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;";
            case INFO -> "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;";
            case WARNING -> "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;";
        };
    }
    
    /**
     * Muestra una notificación de éxito.
     *
     * @param owner Stage padre
     * @param message Mensaje de éxito
     */
    public static void showSuccess(Stage owner, String message) {
        show(owner, "✓ " + message, Type.SUCCESS);
    }
    
    /**
     * Muestra una notificación de error.
     *
     * @param owner Stage padre
     * @param message Mensaje de error
     */
    public static void showError(Stage owner, String message) {
        show(owner, "✗ " + message, Type.ERROR);
    }
    
    /**
     * Muestra una notificación informativa.
     *
     * @param owner Stage padre
     * @param message Mensaje informativo
     */
    public static void showInfo(Stage owner, String message) {
        show(owner, "ℹ " + message, Type.INFO);
    }
    
    /**
     * Muestra una notificación de advertencia.
     *
     * @param owner Stage padre
     * @param message Mensaje de advertencia
     */
    public static void showWarning(Stage owner, String message) {
        show(owner, "⚠ " + message, Type.WARNING);
    }
}
