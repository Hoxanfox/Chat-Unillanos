package com.unillanos.server.gui.styles;

/**
 * Estilos específicos para componentes JavaFX.
 * Proporciona estilos consistentes para TableView, ListView, ComboBox, etc.
 */
public final class ComponentStyles {

    private ComponentStyles() {
        // Clase utilitaria
    }

    // ===== ESTILOS PARA TABLAS =====

    /**
     * Estilo para TableView principal.
     */
    public static String getTableViewStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: %s; " +
            "-fx-effect: %s; " +
            "-fx-padding: 0;",
            DesignSystem.BACKGROUND_ELEVATED,
            DesignSystem.BORDER_RADIUS_LG,
            DesignSystem.GRAY_200,
            DesignSystem.BORDER_RADIUS_LG,
            DesignSystem.SHADOW_SM
        );
    }

    /**
     * Estilo para encabezados de tabla.
     */
    public static String getTableHeaderStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-font-weight: %s; " +
            "-fx-padding: %s %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 0 0 1px 0;",
            DesignSystem.GRAY_50,
            DesignSystem.GRAY_700,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_BASE,
            DesignSystem.FONT_WEIGHT_SEMIBOLD,
            DesignSystem.SPACE_4,
            DesignSystem.SPACE_3,
            DesignSystem.GRAY_200
        );
    }

    /**
     * Estilo para celdas de tabla.
     */
    public static String getTableCellStyle() {
        return String.format(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-padding: %s %s; " +
            "-fx-border-color: transparent transparent %s transparent; " +
            "-fx-border-width: 0 0 1px 0;",
            DesignSystem.GRAY_600,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_BASE,
            DesignSystem.SPACE_3,
            DesignSystem.SPACE_4,
            DesignSystem.GRAY_100
        );
    }

    /**
     * Estilo para filas alternadas de tabla.
     */
    public static String getTableRowAlternateStyle() {
        return String.format(
            "-fx-background-color: %s;",
            DesignSystem.GRAY_50
        );
    }

    /**
     * Estilo para filas seleccionadas de tabla.
     */
    public static String getTableRowSelectedStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white;",
            DesignSystem.PRIMARY
        );
    }

    /**
     * Estilo para filas con hover de tabla.
     */
    public static String getTableRowHoverStyle() {
        return String.format(
            "-fx-background-color: %s;",
            DesignSystem.PRIMARY_SUBTLE
        );
    }

    // ===== ESTILOS PARA GRÁFICOS =====

    /**
     * Estilo base para gráficos JavaFX.
     */
    public static String getChartBaseStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-effect: %s; " +
            "-fx-padding: %s;",
            DesignSystem.BACKGROUND_ELEVATED,
            DesignSystem.BORDER_RADIUS_LG,
            DesignSystem.SHADOW_SM,
            DesignSystem.SPACE_4
        );
    }

    /**
     * Estilo para títulos de gráficos.
     */
    public static String getChartTitleStyle() {
        return String.format(
            "-fx-text-fill: %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-font-weight: %s;",
            DesignSystem.GRAY_800,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_LG,
            DesignSystem.FONT_WEIGHT_SEMIBOLD
        );
    }

    /**
     * Estilo para leyendas de gráficos.
     */
    public static String getChartLegendStyle() {
        return String.format(
            "-fx-text-fill: %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s;",
            DesignSystem.GRAY_600,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_SM
        );
    }

    /**
     * Estilo para ejes de gráficos.
     */
    public static String getChartAxisStyle() {
        return String.format(
            "-fx-tick-label-fill: %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-tick-mark-visible: true; " +
            "-fx-tick-label-visible: true;",
            DesignSystem.GRAY_600,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_SM
        );
    }

    // ===== ESTILOS PARA LISTAS =====

    /**
     * Estilo para ListView.
     */
    public static String getListViewStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: %s; " +
            "-fx-effect: %s; " +
            "-fx-padding: %s;",
            DesignSystem.BACKGROUND_ELEVATED,
            DesignSystem.BORDER_RADIUS_LG,
            DesignSystem.GRAY_200,
            DesignSystem.BORDER_RADIUS_LG,
            DesignSystem.SHADOW_SM,
            DesignSystem.SPACE_2
        );
    }

    /**
     * Estilo para celdas de ListView.
     */
    public static String getListViewCellStyle() {
        return String.format(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-padding: %s %s; " +
            "-fx-border-color: transparent transparent %s transparent; " +
            "-fx-border-width: 0 0 1px 0;",
            DesignSystem.GRAY_600,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_BASE,
            DesignSystem.SPACE_3,
            DesignSystem.SPACE_4,
            DesignSystem.GRAY_100
        );
    }

    /**
     * Estilo para celdas seleccionadas de ListView.
     */
    public static String getListViewCellSelectedStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white;",
            DesignSystem.PRIMARY
        );
    }

    /**
     * Estilo para celdas con hover de ListView.
     */
    public static String getListViewCellHoverStyle() {
        return String.format(
            "-fx-background-color: %s;",
            DesignSystem.PRIMARY_SUBTLE
        );
    }

    // ===== ESTILOS PARA CONTROLES DE FORMULARIO =====

    /**
     * Estilo para ComboBox.
     */
    public static String getComboBoxStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-padding: %s %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-text-fill: %s;",
            DesignSystem.BACKGROUND_PRIMARY,
            DesignSystem.GRAY_300,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.SPACE_3,
            DesignSystem.SPACE_4,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_BASE,
            DesignSystem.GRAY_700
        );
    }

    /**
     * Estilo para ComboBox enfocado.
     */
    public static String getComboBoxFocusedStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-padding: %s %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-text-fill: %s;",
            DesignSystem.BACKGROUND_PRIMARY,
            DesignSystem.PRIMARY,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.SPACE_3,
            DesignSystem.SPACE_4,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_BASE,
            DesignSystem.GRAY_700
        );
    }

    /**
     * Estilo para CheckBox.
     */
    public static String getCheckBoxStyle() {
        return String.format(
            "-fx-text-fill: %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-padding: %s;",
            DesignSystem.GRAY_700,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_BASE,
            DesignSystem.SPACE_2
        );
    }

    // ===== ESTILOS PARA PAGINACIÓN =====

    /**
     * Estilo para Pagination.
     */
    public static String getPaginationStyle() {
        return String.format(
            "-fx-background-color: transparent; " +
            "-fx-padding: %s;",
            DesignSystem.SPACE_4
        );
    }

    /**
     * Estilo para botones de paginación.
     */
    public static String getPaginationButtonStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-font-weight: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-border-radius: %s; " +
            "-fx-padding: %s %s; " +
            "-fx-min-width: 32px; " +
            "-fx-min-height: 32px; " +
            "-fx-cursor: hand;",
            DesignSystem.BACKGROUND_ELEVATED,
            DesignSystem.GRAY_700,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_SM,
            DesignSystem.FONT_WEIGHT_MEDIUM,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.SPACE_2,
            DesignSystem.SPACE_2
        );
    }

    /**
     * Estilo para botones de paginación activos.
     */
    public static String getPaginationButtonActiveStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-font-weight: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-border-radius: %s; " +
            "-fx-padding: %s %s; " +
            "-fx-min-width: 32px; " +
            "-fx-min-height: 32px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: %s;",
            DesignSystem.PRIMARY,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_SM,
            DesignSystem.FONT_WEIGHT_SEMIBOLD,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.SPACE_2,
            DesignSystem.SPACE_2,
            DesignSystem.SHADOW_SM
        );
    }

    /**
     * Estilo para botones de paginación con hover.
     */
    public static String getPaginationButtonHoverStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-family: %s; " +
            "-fx-font-size: %s; " +
            "-fx-font-weight: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-border-radius: %s; " +
            "-fx-padding: %s %s; " +
            "-fx-min-width: 32px; " +
            "-fx-min-height: 32px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: %s;",
            DesignSystem.PRIMARY_DARK,
            DesignSystem.FONT_FAMILY,
            DesignSystem.FONT_SIZE_SM,
            DesignSystem.FONT_WEIGHT_SEMIBOLD,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.BORDER_RADIUS_MD,
            DesignSystem.SPACE_2,
            DesignSystem.SPACE_2,
            DesignSystem.SHADOW_MD
        );
    }

    // ===== ESTILOS PARA SCROLLBARS =====

    /**
     * Estilo para ScrollBar.
     */
    public static String getScrollBarStyle() {
        return String.format(
            "-fx-background-color: transparent; " +
            "-fx-background-radius: %s; " +
            "-fx-border-radius: %s;",
            DesignSystem.BORDER_RADIUS_SM,
            DesignSystem.BORDER_RADIUS_SM
        );
    }

    /**
     * Estilo para el thumb (manija) del ScrollBar.
     */
    public static String getScrollBarThumbStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-border-radius: %s;",
            DesignSystem.GRAY_400,
            DesignSystem.BORDER_RADIUS_SM,
            DesignSystem.BORDER_RADIUS_SM
        );
    }

    /**
     * Estilo para el thumb del ScrollBar con hover.
     */
    public static String getScrollBarThumbHoverStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-border-radius: %s;",
            DesignSystem.GRAY_500,
            DesignSystem.BORDER_RADIUS_SM,
            DesignSystem.BORDER_RADIUS_SM
        );
    }

    // ===== ESTILOS PARA TOOLTIPS =====

    /**
     * Estilo para Tooltip.
     */
    public static String getTooltipStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: %s; " +
            "-fx-padding: %s %s; " +
            "-fx-font-size: %s; " +
            "-fx-font-family: %s; " +
            "-fx-effect: %s;",
            DesignSystem.GRAY_800,
            DesignSystem.BORDER_RADIUS_SM,
            DesignSystem.SPACE_2,
            DesignSystem.SPACE_3,
            DesignSystem.FONT_SIZE_SM,
            DesignSystem.FONT_FAMILY,
            DesignSystem.SHADOW_MD
        );
    }

    // ===== ESTILOS PARA SEPARADORES =====

    /**
     * Estilo para Separator.
     */
    public static String getSeparatorStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-padding: %s 0;",
            DesignSystem.GRAY_200,
            DesignSystem.SPACE_2
        );
    }

    // ===== ESTILOS PARA CONTENEDORES =====

    /**
     * Estilo para contenedores de contenido con scroll.
     */
    public static String getScrollableContainerStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: %s; " +
            "-fx-effect: %s;",
            DesignSystem.BACKGROUND_ELEVATED,
            DesignSystem.BORDER_RADIUS_LG,
            DesignSystem.GRAY_200,
            DesignSystem.BORDER_RADIUS_LG,
            DesignSystem.SHADOW_SM
        );
    }

    /**
     * Estilo para contenedores de formulario.
     */
    public static String getFormContainerStyle() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: %s; " +
            "-fx-effect: %s; " +
            "-fx-padding: %s;",
            DesignSystem.BACKGROUND_ELEVATED,
            DesignSystem.BORDER_RADIUS_LG,
            DesignSystem.GRAY_200,
            DesignSystem.BORDER_RADIUS_LG,
            DesignSystem.SHADOW_SM,
            DesignSystem.SPACE_6
        );
    }
}
