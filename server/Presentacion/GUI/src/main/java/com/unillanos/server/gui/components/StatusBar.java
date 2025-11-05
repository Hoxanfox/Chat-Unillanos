package com.unillanos.server.gui.components;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Barra de estado persistente que muestra información del sistema,
 * estado de conexión y reloj en tiempo real.
 */
public class StatusBar extends HBox {
    
    private final Label statusLabel;
    private final Label connectionLabel;
    private final Label timeLabel;
    private final Timeline clockTimeline;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    /**
     * Constructor de la barra de estado.
     */
    public StatusBar() {
        setSpacing(20);
        setPadding(new Insets(8, 15, 8, 15));
        setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        setAlignment(Pos.CENTER_LEFT);
        
        // Label de estado principal
        statusLabel = new Label("Listo");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");
        
        // Label de estado de conexión
        connectionLabel = new Label("Servidor: Activo");
        connectionLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        
        // Label de tiempo
        timeLabel = new Label();
        timeLabel.setStyle("-fx-font-family: 'Courier New', monospace; -fx-text-fill: #666666;");
        updateTime();
        
        // Spacer para empujar elementos hacia los lados
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Agregar elementos a la barra
        getChildren().addAll(statusLabel, spacer, connectionLabel, timeLabel);
        
        // Configurar reloj que se actualiza cada segundo
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTime()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }
    
    /**
     * Actualiza el reloj con la hora actual.
     */
    private void updateTime() {
        Platform.runLater(() -> {
            LocalDateTime now = LocalDateTime.now();
            timeLabel.setText(now.format(TIME_FORMATTER));
        });
    }
    
    /**
     * Establece el mensaje de estado principal.
     *
     * @param message Mensaje de estado
     */
    public void setStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            // Cambiar color temporalmente para indicar actividad
            statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
            
            // Restaurar color después de 2 segundos
            Timeline restoreColor = new Timeline(new KeyFrame(Duration.seconds(2), e -> 
                statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;")
            ));
            restoreColor.play();
        });
    }
    
    /**
     * Establece el estado de conexión del servidor.
     *
     * @param connected true si el servidor está conectado, false en caso contrario
     */
    public void setConnectionStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                connectionLabel.setText("Servidor: Activo");
                connectionLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                connectionLabel.setText("Servidor: Inactivo");
                connectionLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        });
    }
    
    /**
     * Muestra un mensaje de estado con un tipo específico.
     *
     * @param message Mensaje a mostrar
     * @param type Tipo de mensaje (determina el color)
     */
    public void setStatus(String message, StatusType type) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + getColorForType(type) + ";");
            
            // Restaurar color después de 3 segundos
            Timeline restoreColor = new Timeline(new KeyFrame(Duration.seconds(3), e -> 
                statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;")
            ));
            restoreColor.play();
        });
    }
    
    /**
     * Obtiene el color CSS para cada tipo de estado.
     *
     * @param type Tipo de estado
     * @return String con el color CSS
     */
    private String getColorForType(StatusType type) {
        return switch(type) {
            case SUCCESS -> "#4CAF50";
            case ERROR -> "#F44336";
            case WARNING -> "#FF9800";
            case INFO -> "#2196F3";
            case NORMAL -> "#333333";
        };
    }
    
    /**
     * Muestra información detallada del sistema en el tooltip.
     *
     * @param info Información del sistema
     */
    public void setSystemInfo(String info) {
        Platform.runLater(() -> {
            timeLabel.setTooltip(new javafx.scene.control.Tooltip(
                "Última actualización: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + "\n" + info
            ));
        });
    }
    
    /**
     * Detiene el reloj y libera recursos.
     */
    public void dispose() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
    }
    
    /**
     * Enum para tipos de estado en la barra de estado.
     */
    public enum StatusType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO,
        NORMAL
    }
}
