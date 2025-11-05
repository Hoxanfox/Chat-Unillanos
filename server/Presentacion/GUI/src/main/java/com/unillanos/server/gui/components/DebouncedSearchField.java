package com.unillanos.server.gui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Componente reutilizable de búsqueda con debouncing.
 * Retrasa la ejecución de la búsqueda hasta que el usuario deje de escribir por un tiempo determinado.
 */
public class DebouncedSearchField extends HBox {
    
    private static final Logger logger = LoggerFactory.getLogger(DebouncedSearchField.class);
    
    private final TextField searchField;
    private final Consumer<String> searchCallback;
    private final ScheduledExecutorService scheduler;
    private final long debounceDelayMs;
    
    private ScheduledFuture<?> debounceTask;

    /**
     * Crea un campo de búsqueda con debouncing.
     *
     * @param placeholder Texto de placeholder para el campo
     * @param callback Función a ejecutar cuando se realice la búsqueda
     * @param delayMs Tiempo de retardo en milisegundos
     */
    public DebouncedSearchField(String placeholder, Consumer<String> callback, long delayMs) {
        this.searchCallback = callback;
        this.debounceDelayMs = delayMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
        
        // Configurar el campo de búsqueda
        searchField = new TextField();
        searchField.setPromptText(placeholder);
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-font-size: 14px;");
        
        // Icono de búsqueda
        Label searchIcon = new Label("\uD83D\uDD0D"); // Unicode search icon
        searchIcon.setStyle("-fx-padding: 5; -fx-font-size: 16px;");
        
        // Botón para limpiar
        Button clearBtn = new Button("✕");
        clearBtn.setStyle("-fx-font-size: 12px; -fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-background-radius: 3;");
        clearBtn.setPrefSize(25, 25);
        clearBtn.setOnAction(e -> {
            searchField.clear();
            triggerSearch("");
        });
        
        // Configurar layout
        setSpacing(8);
        setPadding(new Insets(5));
        getChildren().addAll(searchIcon, searchField, clearBtn);
        
        // Configurar listener con debouncing
        searchField.textProperty().addListener((obs, oldVal, newVal) -> debounce(newVal));
        
        logger.debug("DebouncedSearchField creado con delay de {}ms", delayMs);
    }

    /**
     * Implementa el debouncing: cancela la tarea anterior y programa una nueva.
     */
    private void debounce(String searchTerm) {
        // Cancelar tarea anterior si existe
        if (debounceTask != null && !debounceTask.isDone()) {
            debounceTask.cancel(false);
        }
        
        // Programar nueva tarea
        debounceTask = scheduler.schedule(
            () -> triggerSearch(searchTerm),
            debounceDelayMs,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Ejecuta la búsqueda en el hilo de la UI.
     */
    private void triggerSearch(String term) {
        Platform.runLater(() -> {
            try {
                searchCallback.accept(term);
                logger.debug("Búsqueda ejecutada para: '{}'", term);
            } catch (Exception e) {
                logger.error("Error al ejecutar búsqueda para '{}': {}", term, e.getMessage(), e);
            }
        });
    }

    /**
     * Obtiene el texto actual del campo de búsqueda.
     *
     * @return Texto del campo
     */
    public String getText() {
        return searchField.getText();
    }

    /**
     * Establece el texto del campo de búsqueda.
     *
     * @param text Texto a establecer
     */
    public void setText(String text) {
        searchField.setText(text);
    }

    /**
     * Limpia el campo de búsqueda y ejecuta la búsqueda.
     */
    public void clear() {
        searchField.clear();
        triggerSearch("");
    }

    /**
     * Establece si el campo está habilitado.
     *
     * @param enabled true para habilitar, false para deshabilitar
     */
    public void setEnabled(boolean enabled) {
        searchField.setDisable(!enabled);
    }

    /**
     * Libera los recursos del scheduler.
     * Debe llamarse cuando el componente ya no se use.
     */
    public void dispose() {
        if (debounceTask != null && !debounceTask.isDone()) {
            debounceTask.cancel(false);
        }
        scheduler.shutdown();
        logger.debug("DebouncedSearchField disposed");
    }

    /**
     * Obtiene el campo de texto para personalización adicional.
     *
     * @return TextField interno
     */
    public TextField getTextField() {
        return searchField;
    }
}
