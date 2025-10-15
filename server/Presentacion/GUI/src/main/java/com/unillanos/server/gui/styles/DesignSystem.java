package com.unillanos.server.gui.styles;

/**
 * Sistema de diseño centralizado para la aplicación Chat-Unillanos.
 * Define colores, tipografías, espaciado y estilos consistentes.
 */
public final class DesignSystem {

    private DesignSystem() {
        // Clase utilitaria
    }

    // ===== PALETA DE COLORES =====
    
    // Colores Primarios
    public static final String PRIMARY = "#2563EB";        // Azul moderno
    public static final String PRIMARY_DARK = "#1D4ED8";   // Azul oscuro
    public static final String PRIMARY_LIGHT = "#3B82F6";  // Azul claro
    public static final String PRIMARY_SUBTLE = "#EFF6FF"; // Azul muy claro

    // Colores Secundarios
    public static final String SECONDARY = "#7C3AED";      // Púrpura elegante
    public static final String SECONDARY_DARK = "#6D28D9";
    public static final String SECONDARY_LIGHT = "#8B5CF6";
    public static final String SECONDARY_SUBTLE = "#F5F3FF";

    // Colores de Estado
    public static final String SUCCESS = "#10B981";        // Verde esmeralda
    public static final String SUCCESS_LIGHT = "#ECFDF5";
    public static final String WARNING = "#F59E0B";        // Ámbar
    public static final String WARNING_LIGHT = "#FFFBEB";
    public static final String ERROR = "#EF4444";          // Rojo moderno
    public static final String ERROR_LIGHT = "#FEF2F2";
    public static final String INFO = "#06B6D4";           // Cian
    public static final String INFO_LIGHT = "#ECFEFF";

    // Colores Neutros
    public static final String GRAY_50 = "#F9FAFB";
    public static final String GRAY_100 = "#F3F4F6";
    public static final String GRAY_200 = "#E5E7EB";
    public static final String GRAY_300 = "#D1D5DB";
    public static final String GRAY_400 = "#9CA3AF";
    public static final String GRAY_500 = "#6B7280";
    public static final String GRAY_600 = "#4B5563";
    public static final String GRAY_700 = "#374151";
    public static final String GRAY_800 = "#1F2937";
    public static final String GRAY_900 = "#111827";

    // Colores de Fondo
    public static final String BACKGROUND_PRIMARY = "#FFFFFF";
    public static final String BACKGROUND_SECONDARY = GRAY_50;
    public static final String BACKGROUND_ELEVATED = "#FFFFFF";
    public static final String BACKGROUND_OVERLAY = "rgba(0, 0, 0, 0.5)";

    // ===== TIPOGRAFÍA =====
    
    public static final String FONT_FAMILY = "'Inter', 'Segoe UI', 'Roboto', sans-serif";
    public static final String FONT_MONO = "'JetBrains Mono', 'Fira Code', monospace";

    // Tamaños de Fuente
    public static final String FONT_SIZE_XS = "11px";
    public static final String FONT_SIZE_SM = "12px";
    public static final String FONT_SIZE_BASE = "14px";
    public static final String FONT_SIZE_LG = "16px";
    public static final String FONT_SIZE_XL = "18px";
    public static final String FONT_SIZE_2XL = "20px";
    public static final String FONT_SIZE_3XL = "24px";
    public static final String FONT_SIZE_4XL = "30px";

    // Pesos de Fuente
    public static final String FONT_WEIGHT_NORMAL = "400";
    public static final String FONT_WEIGHT_MEDIUM = "500";
    public static final String FONT_WEIGHT_SEMIBOLD = "600";
    public static final String FONT_WEIGHT_BOLD = "700";

    // ===== ESPACIADO =====
    
    public static final String SPACE_1 = "4px";
    public static final String SPACE_2 = "8px";
    public static final String SPACE_3 = "12px";
    public static final String SPACE_4 = "16px";
    public static final String SPACE_5 = "20px";
    public static final String SPACE_6 = "24px";
    public static final String SPACE_8 = "32px";
    public static final String SPACE_10 = "40px";
    public static final String SPACE_12 = "48px";
    public static final String SPACE_16 = "64px";

    // ===== BORDES Y RADIOS =====
    
    public static final String BORDER_RADIUS_SM = "4px";
    public static final String BORDER_RADIUS_MD = "6px";
    public static final String BORDER_RADIUS_LG = "8px";
    public static final String BORDER_RADIUS_XL = "12px";
    public static final String BORDER_RADIUS_2XL = "16px";
    public static final String BORDER_RADIUS_FULL = "9999px";

    // ===== SOMBRAS =====
    
    public static final String SHADOW_SM = "dropshadow(gaussian, rgba(0,0,0,0.05), 2, 0, 0, 1)";
    public static final String SHADOW_MD = "dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2)";
    public static final String SHADOW_LG = "dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 4)";
    public static final String SHADOW_XL = "dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 6)";

    // ===== ESTILOS PREDEFINIDOS =====

    /**
     * Estilo para títulos principales de la aplicación.
     */
    public static String getTitleStyle() {
        return String.format(
            "-fx-font-family: %s; -fx-font-size: %s; -fx-font-weight: %s; " +
            "-fx-text-fill: %s; -fx-padding: %s 0;",
            FONT_FAMILY, FONT_SIZE_3XL, FONT_WEIGHT_BOLD, GRAY_900, SPACE_4
        );
    }

    /**
     * Estilo para subtítulos.
     */
    public static String getSubtitleStyle() {
        return String.format(
            "-fx-font-family: %s; -fx-font-size: %s; -fx-font-weight: %s; " +
            "-fx-text-fill: %s; -fx-padding: %s 0;",
            FONT_FAMILY, FONT_SIZE_XL, FONT_WEIGHT_SEMIBOLD, GRAY_700, SPACE_2
        );
    }

    /**
     * Estilo para texto de cuerpo.
     */
    public static String getBodyStyle() {
        return String.format(
            "-fx-font-family: %s; -fx-font-size: %s; -fx-text-fill: %s;",
            FONT_FAMILY, FONT_SIZE_BASE, GRAY_600
        );
    }

    /**
     * Estilo para texto secundario.
     */
    public static String getSecondaryTextStyle() {
        return String.format(
            "-fx-font-family: %s; -fx-font-size: %s; -fx-text-fill: %s;",
            FONT_FAMILY, FONT_SIZE_SM, GRAY_500
        );
    }

    /**
     * Estilo para botones primarios.
     */
    public static String getPrimaryButtonStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-text-fill: white; " +
            "-fx-font-family: %s; -fx-font-size: %s; -fx-font-weight: %s; " +
            "-fx-background-radius: %s; -fx-border-radius: %s; " +
            "-fx-padding: %s %s; -fx-cursor: hand; " +
            "-fx-effect: %s;",
            PRIMARY, FONT_FAMILY, FONT_SIZE_BASE, FONT_WEIGHT_MEDIUM,
            BORDER_RADIUS_MD, BORDER_RADIUS_MD, SPACE_3, SPACE_4, SHADOW_SM
        );
    }

    /**
     * Estilo para botones secundarios.
     */
    public static String getSecondaryButtonStyle() {
        return String.format(
            "-fx-background-color: transparent; -fx-text-fill: %s; " +
            "-fx-font-family: %s; -fx-font-size: %s; -fx-font-weight: %s; " +
            "-fx-border-color: %s; -fx-border-width: 1px; " +
            "-fx-background-radius: %s; -fx-border-radius: %s; " +
            "-fx-padding: %s %s; -fx-cursor: hand;",
            PRIMARY, FONT_FAMILY, FONT_SIZE_BASE, FONT_WEIGHT_MEDIUM, PRIMARY,
            BORDER_RADIUS_MD, BORDER_RADIUS_MD, SPACE_3, SPACE_4
        );
    }

    /**
     * Estilo para tarjetas elevadas.
     */
    public static String getCardStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-background-radius: %s; " +
            "-fx-effect: %s; -fx-padding: %s;",
            BACKGROUND_ELEVATED, BORDER_RADIUS_LG, SHADOW_MD, SPACE_6
        );
    }

    /**
     * Estilo para tarjetas de métricas.
     */
    public static String getMetricCardStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-background-radius: %s; " +
            "-fx-effect: %s; -fx-padding: %s; -fx-border-color: %s; " +
            "-fx-border-width: 1px; -fx-border-radius: %s;",
            BACKGROUND_ELEVATED, BORDER_RADIUS_LG, SHADOW_SM, SPACE_6, GRAY_200, BORDER_RADIUS_LG
        );
    }

    /**
     * Estilo para campos de entrada.
     */
    public static String getInputFieldStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-border-color: %s; " +
            "-fx-border-width: 1px; -fx-border-radius: %s; " +
            "-fx-background-radius: %s; -fx-padding: %s %s; " +
            "-fx-font-family: %s; -fx-font-size: %s;",
            BACKGROUND_PRIMARY, GRAY_300, BORDER_RADIUS_MD, BORDER_RADIUS_MD,
            SPACE_3, SPACE_4, FONT_FAMILY, FONT_SIZE_BASE
        );
    }

    /**
     * Estilo para campos de entrada enfocados.
     */
    public static String getInputFieldFocusedStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-border-color: %s; " +
            "-fx-border-width: 2px; -fx-border-radius: %s; " +
            "-fx-background-radius: %s; -fx-padding: %s %s; " +
            "-fx-font-family: %s; -fx-font-size: %s;",
            BACKGROUND_PRIMARY, PRIMARY, BORDER_RADIUS_MD, BORDER_RADIUS_MD,
            SPACE_3, SPACE_4, FONT_FAMILY, FONT_SIZE_BASE
        );
    }

    /**
     * Estilo para la barra lateral de navegación.
     */
    public static String getSidebarStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-border-color: %s; " +
            "-fx-border-width: 0 1px 0 0; -fx-padding: %s;",
            BACKGROUND_ELEVATED, GRAY_200, SPACE_6
        );
    }

    /**
     * Estilo para elementos de navegación activos.
     */
    public static String getNavItemActiveStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-text-fill: white; " +
            "-fx-background-radius: %s; -fx-padding: %s %s; " +
            "-fx-font-weight: %s; -fx-cursor: hand;",
            PRIMARY, BORDER_RADIUS_MD, SPACE_3, SPACE_4, FONT_WEIGHT_MEDIUM
        );
    }

    /**
     * Estilo para elementos de navegación inactivos.
     */
    public static String getNavItemInactiveStyle() {
        return String.format(
            "-fx-background-color: transparent; -fx-text-fill: %s; " +
            "-fx-background-radius: %s; -fx-padding: %s %s; " +
            "-fx-font-weight: %s; -fx-cursor: hand;",
            GRAY_600, BORDER_RADIUS_MD, SPACE_3, SPACE_4, FONT_WEIGHT_NORMAL
        );
    }

    /**
     * Estilo para la barra de estado.
     */
    public static String getStatusBarStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-border-color: %s; " +
            "-fx-border-width: 1px 0 0 0; -fx-padding: %s %s;",
            BACKGROUND_SECONDARY, GRAY_200, SPACE_2, SPACE_4
        );
    }

    /**
     * Estilo para gráficos (charts).
     */
    public static String getChartStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-background-radius: %s; " +
            "-fx-effect: %s; -fx-padding: %s;",
            BACKGROUND_ELEVATED, BORDER_RADIUS_LG, SHADOW_SM, SPACE_4
        );
    }

    /**
     * Estilo para tooltips.
     */
    public static String getTooltipStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-text-fill: white; " +
            "-fx-background-radius: %s; -fx-padding: %s %s; " +
            "-fx-font-size: %s; -fx-effect: %s;",
            GRAY_800, BORDER_RADIUS_SM, SPACE_2, SPACE_3, FONT_SIZE_SM, SHADOW_MD
        );
    }

    /**
     * Estilo para badges/etiquetas.
     */
    public static String getBadgeStyle(String backgroundColor) {
        return String.format(
            "-fx-background-color: %s; -fx-text-fill: white; " +
            "-fx-background-radius: %s; -fx-padding: %s %s; " +
            "-fx-font-size: %s; -fx-font-weight: %s;",
            backgroundColor, BORDER_RADIUS_FULL, SPACE_1, SPACE_2, FONT_SIZE_XS, FONT_WEIGHT_MEDIUM
        );
    }

    /**
     * Estilo para avatares.
     */
    public static String getAvatarStyle(String backgroundColor) {
        return String.format(
            "-fx-background-color: %s; -fx-text-fill: white; " +
            "-fx-background-radius: %s; -fx-font-weight: %s; " +
            "-fx-alignment: center;",
            backgroundColor, BORDER_RADIUS_FULL, FONT_WEIGHT_SEMIBOLD
        );
    }

    /**
     * Estilo para indicadores de estado.
     */
    public static String getStatusIndicatorStyle(String color) {
        return String.format(
            "-fx-background-color: %s; -fx-background-radius: %s; " +
            "-fx-min-width: 8px; -fx-min-height: 8px; -fx-max-width: 8px; -fx-max-height: 8px;",
            color, BORDER_RADIUS_FULL
        );
    }

    /**
     * Estilo para el fondo principal de la aplicación.
     */
    public static String getMainBackgroundStyle() {
        return String.format(
            "-fx-background-color: %s;",
            BACKGROUND_SECONDARY
        );
    }

    /**
     * Estilo para contenedores de contenido.
     */
    public static String getContentContainerStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-background-radius: %s; " +
            "-fx-padding: %s; -fx-effect: %s;",
            BACKGROUND_ELEVATED, BORDER_RADIUS_LG, SPACE_6, SHADOW_SM
        );
    }
}
