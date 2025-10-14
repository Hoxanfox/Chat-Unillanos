package com.unillanos.server.gui.controller;

import com.unillanos.server.gui.SharedContext;
import com.unillanos.server.service.impl.AdminMonitoringService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

/**
 * Controlador del Dashboard. Muestra tarjetas de estado básicas.
 */
public class DashboardController {

    private final VBox root;
    private final Label valueServer;
    private final Label valuePort;
    private final Label valueConnections;
    private final Label valueUptime;
    private final ListView<String> logsList;

    private final AdminMonitoringService monitoringService;
    private final Timeline refreshTimeline;

    public DashboardController() {
        this.root = new VBox(16);
        this.root.setPadding(new Insets(16));
        this.root.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("Dashboard");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        GridPane cards = new GridPane();
        cards.setHgap(16);
        cards.setVgap(16);

        valueServer = new Label("-");
        valuePort = new Label("-");
        valueConnections = new Label("-");
        valueUptime = new Label("-");

        cards.add(buildCard("Servidor", valueServer), 0, 0);
        cards.add(buildCard("Puerto", valuePort), 1, 0);
        cards.add(buildCard("Conexiones", valueConnections), 2, 0);
        cards.add(buildCard("Uptime", valueUptime), 3, 0);

        logsList = new ListView<>();
        logsList.setPrefHeight(300);

        root.getChildren().addAll(header, cards);

        Label logsHeader = new Label("Logs recientes");
        logsHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        root.getChildren().addAll(logsHeader, logsList);

        // Obtener servicio desde Spring
        monitoringService = SharedContext.get().getBean(AdminMonitoringService.class);

        // Refresco periódico cada 2 segundos
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), ev -> safelyRefresh()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.playFromStart();
    }

    private HBox buildCard(String title, Label valueNode) {
        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        valueNode.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox box = new VBox(4, lTitle, valueNode);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #f7f7f7; -fx-background-radius: 6; -fx-border-color: #ddd; -fx-border-radius: 6;");

        HBox wrapper = new HBox(box);
        return wrapper;
    }

    public Node getView() {
        return root;
    }

    private void refresh() {
        var status = monitoringService.getServerStatus();
        valueServer.setText(Boolean.TRUE.equals(status.get("active")) ? "Activo" : "Inactivo");
        valuePort.setText(String.valueOf(status.get("port")));
        valueConnections.setText(String.valueOf(status.get("connections")));
        valueUptime.setText(String.valueOf(status.get("uptime")));

        var items = monitoringService.getRecentLogs(50);
        logsList.getItems().setAll(items);
        if (!items.isEmpty()) {
            logsList.scrollTo(items.size() - 1);
        }
    }

    private void safelyRefresh() {
        try {
            refresh();
        } catch (Exception ignored) {}
    }
}

