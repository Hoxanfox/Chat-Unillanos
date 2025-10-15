package com.unillanos.server.gui.styles;

import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.chart.Chart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.geometry.Side;

/**
 * Utilidades para aplicar estilos dinámicamente a componentes JavaFX.
 * Proporciona métodos para estilizar componentes de forma programática.
 */
public final class StyleUtils {

    private StyleUtils() {
        // Clase utilitaria
    }

    // ===== MÉTODOS PARA TABLAS =====

    /**
     * Aplica estilos modernos a una TableView.
     */
    public static void styleTableView(TableView<?> tableView) {
        tableView.setStyle(ComponentStyles.getTableViewStyle());
        
        // Aplicar estilos a las columnas
        tableView.getColumns().forEach(column -> {
            column.setStyle(ComponentStyles.getTableHeaderStyle());
        });
        
        // Los estilos de filas se aplicarán automáticamente por CSS
    }

    /**
     * Aplica estilos a columnas de tabla.
     */
    public static void styleTableColumns(TableColumn<?, ?>... columns) {
        for (TableColumn<?, ?> column : columns) {
            column.setStyle(ComponentStyles.getTableHeaderStyle());
        }
    }

    // ===== MÉTODOS PARA GRÁFICOS =====

    /**
     * Aplica estilos modernos a un gráfico PieChart.
     */
    public static void stylePieChart(PieChart pieChart) {
        pieChart.setStyle(ComponentStyles.getChartBaseStyle());
        pieChart.setTitle(ComponentStyles.getChartTitleStyle());
        pieChart.setLegendVisible(true);
        pieChart.setLegendSide(Side.BOTTOM);
        
        // Estilizar colores de los datos
        pieChart.getData().forEach(data -> {
            // Aplicar colores del sistema de diseño
            data.getNode().setStyle(String.format(
                "-fx-pie-color: %s;",
                getColorForPieSlice(data.getName())
            ));
        });
    }

    /**
     * Aplica estilos modernos a un gráfico LineChart.
     */
    public static void styleLineChart(LineChart<String, Number> lineChart) {
        lineChart.setStyle(ComponentStyles.getChartBaseStyle());
        lineChart.setTitle(ComponentStyles.getChartTitleStyle());
        
        // Estilizar ejes
        if (lineChart.getXAxis() instanceof CategoryAxis) {
            ((CategoryAxis) lineChart.getXAxis()).setStyle(ComponentStyles.getChartAxisStyle());
        }
        if (lineChart.getYAxis() instanceof NumberAxis) {
            ((NumberAxis) lineChart.getYAxis()).setStyle(ComponentStyles.getChartAxisStyle());
        }
        
        // Estilizar series
        lineChart.getData().forEach(series -> {
            series.getNode().setStyle(String.format(
                "-fx-stroke: %s; -fx-stroke-width: 2px;",
                DesignSystem.PRIMARY
            ));
        });
    }

    /**
     * Aplica estilos modernos a un gráfico BarChart.
     */
    public static void styleBarChart(BarChart<String, Number> barChart) {
        barChart.setStyle(ComponentStyles.getChartBaseStyle());
        barChart.setTitle(ComponentStyles.getChartTitleStyle());
        
        // Estilizar ejes
        if (barChart.getXAxis() instanceof CategoryAxis) {
            ((CategoryAxis) barChart.getXAxis()).setStyle(ComponentStyles.getChartAxisStyle());
        }
        if (barChart.getYAxis() instanceof NumberAxis) {
            ((NumberAxis) barChart.getYAxis()).setStyle(ComponentStyles.getChartAxisStyle());
        }
        
        // Estilizar barras
        barChart.getData().forEach(series -> {
            series.getNode().setStyle(String.format(
                "-fx-bar-fill: %s;",
                DesignSystem.PRIMARY
            ));
        });
    }

    /**
     * Obtiene un color para un slice del pie chart basado en su nombre.
     */
    private static String getColorForPieSlice(String sliceName) {
        return switch (sliceName.toLowerCase()) {
            case "directos" -> DesignSystem.PRIMARY;
            case "canales" -> DesignSystem.SECONDARY;
            case "imágenes" -> DesignSystem.SUCCESS;
            case "videos" -> DesignSystem.WARNING;
            case "documentos" -> DesignSystem.INFO;
            default -> DesignSystem.PRIMARY;
        };
    }

    // ===== MÉTODOS PARA LISTAS =====

    /**
     * Aplica estilos modernos a una ListView.
     */
    public static void styleListView(ListView<?> listView) {
        listView.setStyle(ComponentStyles.getListViewStyle());
        
        // Los estilos de celdas se aplicarán automáticamente por CSS
    }

    // ===== MÉTODOS PARA CONTROLES DE FORMULARIO =====

    /**
     * Aplica estilos modernos a un ComboBox.
     */
    public static void styleComboBox(ComboBox<?> comboBox) {
        comboBox.setStyle(ComponentStyles.getComboBoxStyle());
        
        // Efectos de hover y focus
        comboBox.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (isFocused) {
                comboBox.setStyle(ComponentStyles.getComboBoxFocusedStyle());
            } else {
                comboBox.setStyle(ComponentStyles.getComboBoxStyle());
            }
        });
    }

    /**
     * Aplica estilos modernos a un CheckBox.
     */
    public static void styleCheckBox(CheckBox checkBox) {
        checkBox.setStyle(ComponentStyles.getCheckBoxStyle());
    }

    /**
     * Aplica estilos modernos a un TextField.
     */
    public static void styleTextField(TextField textField) {
        textField.setStyle(DesignSystem.getInputFieldStyle());
        
        // Efectos de focus
        textField.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (isFocused) {
                textField.setStyle(DesignSystem.getInputFieldFocusedStyle());
            } else {
                textField.setStyle(DesignSystem.getInputFieldStyle());
            }
        });
    }

    /**
     * Aplica estilos modernos a un Button.
     */
    public static void styleButton(Button button, String buttonType) {
        switch (buttonType.toLowerCase()) {
            case "primary" -> button.setStyle(DesignSystem.getPrimaryButtonStyle());
            case "secondary" -> button.setStyle(DesignSystem.getSecondaryButtonStyle());
            default -> button.setStyle(DesignSystem.getPrimaryButtonStyle());
        }
    }

    // ===== MÉTODOS PARA PAGINACIÓN =====

    /**
     * Aplica estilos modernos a una Pagination.
     */
    public static void stylePagination(Pagination pagination) {
        pagination.setStyle(ComponentStyles.getPaginationStyle());
        
        // Configurar factory de botones
        pagination.setPageFactory(pageIndex -> {
            Button pageButton = new Button(String.valueOf(pageIndex + 1));
            
            if (pageIndex == pagination.getCurrentPageIndex()) {
                pageButton.setStyle(ComponentStyles.getPaginationButtonActiveStyle());
            } else {
                pageButton.setStyle(ComponentStyles.getPaginationButtonStyle());
            }
            
            // Efectos de hover
            pageButton.hoverProperty().addListener((obs, oldVal, isHovering) -> {
                if (isHovering && pageIndex != pagination.getCurrentPageIndex()) {
                    pageButton.setStyle(ComponentStyles.getPaginationButtonHoverStyle());
                } else if (pageIndex != pagination.getCurrentPageIndex()) {
                    pageButton.setStyle(ComponentStyles.getPaginationButtonStyle());
                }
            });
            
            pageButton.setOnAction(e -> pagination.setCurrentPageIndex(pageIndex));
            return pageButton;
        });
    }

    // ===== MÉTODOS PARA SCROLLBARS =====

    /**
     * Aplica estilos modernos a un ScrollPane.
     */
    public static void styleScrollPane(ScrollPane scrollPane) {
        scrollPane.setStyle(ComponentStyles.getScrollableContainerStyle());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
    }

    // ===== MÉTODOS PARA SEPARADORES =====

    /**
     * Aplica estilos modernos a un Separator.
     */
    public static void styleSeparator(Separator separator) {
        separator.setStyle(ComponentStyles.getSeparatorStyle());
    }

    // ===== MÉTODOS PARA TOOLTIPS =====

    /**
     * Aplica estilos modernos a un Tooltip.
     */
    public static void styleTooltip(Tooltip tooltip) {
        tooltip.setStyle(ComponentStyles.getTooltipStyle());
    }

    // ===== MÉTODOS PARA CONTENEDORES =====

    /**
     * Aplica estilos de contenedor de formulario a una Region.
     */
    public static void styleFormContainer(Region container) {
        container.setStyle(ComponentStyles.getFormContainerStyle());
    }

    /**
     * Aplica estilos de contenedor con scroll a una Region.
     */
    public static void styleScrollableContainer(Region container) {
        container.setStyle(ComponentStyles.getScrollableContainerStyle());
    }

    // ===== MÉTODOS UTILITARIOS =====

    /**
     * Aplica estilos a un conjunto de botones.
     */
    public static void styleButtonGroup(Button[] buttons, String buttonType) {
        for (Button button : buttons) {
            styleButton(button, buttonType);
        }
    }

    /**
     * Aplica estilos a un conjunto de campos de texto.
     */
    public static void styleTextFieldGroup(TextField[] textFields) {
        for (TextField textField : textFields) {
            styleTextField(textField);
        }
    }

    /**
     * Aplica estilos a un conjunto de ComboBoxes.
     */
    public static void styleComboBoxGroup(ComboBox<?>[] comboBoxes) {
        for (ComboBox<?> comboBox : comboBoxes) {
            styleComboBox(comboBox);
        }
    }

    /**
     * Aplica estilos a un conjunto de CheckBoxes.
     */
    public static void styleCheckBoxGroup(CheckBox[] checkBoxes) {
        for (CheckBox checkBox : checkBoxes) {
            styleCheckBox(checkBox);
        }
    }

    /**
     * Aplica estilos a todos los componentes de un contenedor.
     * Nota: Este método es simplificado para evitar problemas de acceso a getChildren().
     */
    public static void styleAllComponents(Region container) {
        // Los estilos se aplicarán individualmente a cada componente
        // cuando se crean en los controladores
    }
}
