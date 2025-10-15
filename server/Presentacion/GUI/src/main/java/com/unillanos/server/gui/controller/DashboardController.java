package com.unillanos.server.gui.controller;

import com.unillanos.server.gui.SharedContext;
import com.unillanos.server.gui.styles.AnimationEffects;
import com.unillanos.server.gui.styles.ComponentStyles;
import com.unillanos.server.gui.styles.DesignSystem;
import com.unillanos.server.gui.styles.StyleUtils;
import com.unillanos.server.service.impl.AdminMonitoringService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controlador del Dashboard con gráficos en tiempo real.
 * Muestra métricas del servidor y estadísticas visuales.
 */
public class DashboardController {

    private final BorderPane root;
    private final Label valueServer;
    private final Label valuePort;
    private final Label valueConnections;
    private final Label valueUptime;
    private final ListView<String> logsList;

    // Componentes de gráficos
    private LineChart<String, Number> conexionesChart;
    private PieChart tiposMensajesChart;
    private BarChart<String, Number> actividadUsuariosChart;
    
    private ObservableList<PieChart.Data> pieChartData;
    private XYChart.Series<String, Number> conexionesSeries;

    private final AdminMonitoringService monitoringService;
    private final Timeline refreshTimeline;

    public DashboardController() {
        this.root = new BorderPane();
        this.root.setPadding(new Insets(16));

        // Header con diseño moderno
        VBox header = createModernHeader();
        root.setTop(header);

        // Tarjetas de estado
        GridPane cards = new GridPane();
        cards.setHgap(16);
        cards.setVgap(16);

        valueServer = new Label("-");
        valuePort = new Label("-");
        valueConnections = new Label("-");
        valueUptime = new Label("-");

        cards.add(buildCard("Servidor", valueServer, "🖥️"), 0, 0);
        cards.add(buildCard("Puerto", valuePort, "🔌"), 1, 0);
        cards.add(buildCard("Conexiones", valueConnections, "👥"), 2, 0);
        cards.add(buildCard("Uptime", valueUptime, "⏱️"), 3, 0);

        // Crear gráficos
        crearGraficoConexiones();
        crearGraficoTiposMensajes();
        crearGraficoActividadUsuarios();

        // Layout principal con gráficos
        VBox topSection = new VBox(16, cards, crearLayoutGraficos());
        
        // Logs recientes con diseño moderno
        logsList = new ListView<>();
        logsList.setPrefHeight(200);
        
        Label logsHeader = new Label("Logs Recientes");
        logsHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #374151; -fx-padding: 8px 0px;");
        VBox logsSection = new VBox(16, logsHeader, logsList);

        // Layout final
        VBox centerContent = new VBox(20, topSection, logsSection);
        root.setCenter(centerContent);

        // Obtener servicio desde Spring
        monitoringService = SharedContext.get().getBean(AdminMonitoringService.class);

        // Refresco periódico cada 3 segundos para gráficos
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), ev -> safelyRefresh()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.playFromStart();
    }

    private VBox buildCard(String title, Label valueNode, String icon) {
        // Título de la tarjeta
        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");
        
        // Valor principal
        valueNode.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        // Layout de la tarjeta
        VBox cardContent = new VBox();
        cardContent.setSpacing(8);
        cardContent.setAlignment(Pos.CENTER);
        
        // Header con icono y título
        HBox header = new HBox();
        header.setSpacing(8);
        header.setAlignment(Pos.CENTER);
        
        if (icon != null) {
            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 20px;");
            header.getChildren().add(iconLabel);
        }
        header.getChildren().add(lTitle);
        
        cardContent.getChildren().addAll(header, valueNode);
        
        // Contenedor de la tarjeta
        VBox card = new VBox();
        card.setStyle(DesignSystem.getMetricCardStyle());
        card.setPadding(new Insets(24));
        card.setMinHeight(120);
        card.setAlignment(Pos.CENTER);
        card.getChildren().add(cardContent);
        
        // Aplicar efectos
        AnimationEffects.applyHoverEffect(card);
        AnimationEffects.applyFadeInEffect(card);
        
        return card;
    }

    private void crearGraficoConexiones() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Tiempo");
        yAxis.setLabel("Conexiones");
        
        conexionesChart = new LineChart<>(xAxis, yAxis);
        conexionesChart.setTitle("Conexiones Activas en Tiempo Real");
        conexionesChart.setLegendVisible(false);
        conexionesChart.setPrefHeight(280);
        conexionesSeries = new XYChart.Series<>();
        conexionesSeries.setName("Conexiones");
        conexionesChart.getData().add(conexionesSeries);
        
        // Mantener solo últimos 20 puntos
        conexionesSeries.getData().addListener((ListChangeListener<XYChart.Data<String, Number>>) c -> {
            while (c.next()) {
                if (c.wasAdded() && conexionesSeries.getData().size() > 20) {
                    // Remover elementos antiguos de forma segura
                    Platform.runLater(() -> {
                        while (conexionesSeries.getData().size() > 20) {
                            conexionesSeries.getData().remove(0);
                        }
                    });
                }
            }
        });
    }

    private void crearGraficoTiposMensajes() {
        pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Directos", 0),
            new PieChart.Data("Canales", 0)
        );
        tiposMensajesChart = new PieChart(pieChartData);
        tiposMensajesChart.setTitle("Distribución de Mensajes");
        tiposMensajesChart.setPrefHeight(280);
    }

    private void crearGraficoActividadUsuarios() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Usuario");
        yAxis.setLabel("Mensajes");
        
        actividadUsuariosChart = new BarChart<>(xAxis, yAxis);
        actividadUsuariosChart.setTitle("Top 5 Usuarios Más Activos");
        actividadUsuariosChart.setLegendVisible(false);
        actividadUsuariosChart.setPrefHeight(280);
    }

    private VBox createModernHeader() {
        VBox header = new VBox();
        header.setSpacing(16);
        header.setPadding(new Insets(24, 0, 24, 0));

        // Título principal
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1F2937; -fx-padding: 16px 0px;");

        // Subtítulo
        Label subtitle = new Label("Centro de monitoreo en tiempo real");
        subtitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #374151; -fx-padding: 8px 0px;");

        // Indicador de estado en tiempo real
        HBox statusIndicator = createRealtimeStatusIndicator();

        header.getChildren().addAll(title, subtitle, statusIndicator);
        
        // Aplicar efectos
        AnimationEffects.applySlideInFromBottom(header);
        
        return header;
    }

    private HBox createRealtimeStatusIndicator() {
        HBox statusBox = new HBox();
        statusBox.setSpacing(12);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        // Indicador de estado pulsante
        Circle statusDot = new Circle(8);
        statusDot.setFill(Color.valueOf(DesignSystem.SUCCESS));
        AnimationEffects.applyPulseEffect(statusDot);
        
        Label statusText = new Label("Monitoreo en tiempo real activo");
        statusText.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");
        
        statusBox.getChildren().addAll(statusDot, statusText);
        return statusBox;
    }

    private GridPane crearLayoutGraficos() {
        GridPane chartsGrid = new GridPane();
        chartsGrid.setHgap(24);
        chartsGrid.setVgap(24);
        
        chartsGrid.add(conexionesChart, 0, 0);
        chartsGrid.add(tiposMensajesChart, 1, 0);
        chartsGrid.add(actividadUsuariosChart, 0, 1, 2, 1);
        
        return chartsGrid;
    }

    public Node getView() {
        return root;
    }

    private void refresh() {
        try {
            // Actualizar tarjetas de estado
            var status = monitoringService.getServerStatus();
            valueServer.setText(Boolean.TRUE.equals(status.get("active")) ? "Activo" : "Inactivo");
            valuePort.setText(String.valueOf(status.get("port")));
            valueConnections.setText(String.valueOf(status.get("connections")));
            valueUptime.setText(String.valueOf(status.get("uptime")));

            // Actualizar gráficos con estadísticas
            actualizarGraficos();

            // Actualizar logs
            var items = monitoringService.getRecentLogs(50);
            logsList.getItems().setAll(items);
            if (!items.isEmpty()) {
                logsList.scrollTo(items.size() - 1);
            }
        } catch (Exception e) {
            // En caso de error, mantener los datos actuales
            System.err.println("Error actualizando dashboard: " + e.getMessage());
        }
    }

    private void actualizarGraficos() {
        try {
            Map<String, Object> stats = monitoringService.getEstadisticas();
            
            // Actualizar gráfico de conexiones
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            int conexiones = (Integer) stats.getOrDefault("connections", 0);
            conexionesSeries.getData().add(new XYChart.Data<>(timestamp, conexiones));
            
            // Actualizar pie chart de tipos de mensajes
            int mensajesDirectos = (Integer) stats.getOrDefault("mensajesDirectos", 0);
            int mensajesCanal = (Integer) stats.getOrDefault("mensajesCanal", 0);
            pieChartData.get(0).setPieValue(mensajesDirectos);
            pieChartData.get(1).setPieValue(mensajesCanal);
            
            // Actualizar bar chart de actividad de usuarios
            actualizarActividadUsuarios((List<Map<String, Object>>) stats.getOrDefault("topUsuarios", List.of()));
            
        } catch (Exception e) {
            System.err.println("Error actualizando gráficos: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void actualizarActividadUsuarios(List<Map<String, Object>> topUsuarios) {
        // Limpiar datos anteriores
        actividadUsuariosChart.getData().clear();
        
        if (topUsuarios.isEmpty()) {
            return;
        }
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Mensajes");
        
        for (Map<String, Object> usuario : topUsuarios) {
            String nombre = (String) usuario.get("nombre");
            Number mensajes = (Number) usuario.get("mensajes");
            series.getData().add(new XYChart.Data<>(nombre, mensajes));
        }
        
        actividadUsuariosChart.getData().add(series);
    }

    private void safelyRefresh() {
        try {
            refresh();
        } catch (Exception ignored) {}
    }
}

