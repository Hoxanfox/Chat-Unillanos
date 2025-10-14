package com.unillanos.server.gui;

import com.unillanos.server.gui.controller.ChannelsController;
import com.unillanos.server.gui.controller.DashboardController;
import com.unillanos.server.gui.controller.LogsController;
import com.unillanos.server.gui.controller.UsersController;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

/**
 * Ventana principal de la GUI de administración.
 * Contiene barra lateral de navegación y un área central con las vistas.
 */
public class MainWindow extends Application {

    private BorderPane root;
    private StackPane contentPane;

    private DashboardController dashboardController;
    private UsersController usersController;
    private ChannelsController channelsController;
    private LogsController logsController;

    @Override
    public void start(Stage primaryStage) {
        this.root = new BorderPane();
        this.contentPane = new StackPane();

        // Obtener ApplicationContext compartido para permitir que controladores accedan a beans
        ApplicationContext ctx = com.unillanos.server.gui.SharedContext.get();

        this.dashboardController = new DashboardController();
        this.usersController = new UsersController();
        this.channelsController = new ChannelsController();
        this.logsController = new LogsController();

        buildLayout();
        showDashboard();

        Scene scene = new Scene(root, 1100, 720);
        primaryStage.setTitle("Chat-Unillanos - Administración");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            System.exit(0);
        });
        primaryStage.show();
    }

    private void buildLayout() {
        root.setPrefSize(1100, 720);
        root.setPadding(new Insets(0));

        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);
        root.setCenter(contentPane);
    }

    private VBox buildSidebar() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setMinWidth(220);
        box.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Chat-Unillanos Admin");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button btnDashboard = new Button("Dashboard");
        btnDashboard.setMaxWidth(Double.MAX_VALUE);
        btnDashboard.setOnAction(e -> showDashboard());

        Button btnUsers = new Button("Usuarios");
        btnUsers.setMaxWidth(Double.MAX_VALUE);
        btnUsers.setOnAction(e -> showUsers());

        Button btnChannels = new Button("Canales");
        btnChannels.setMaxWidth(Double.MAX_VALUE);
        btnChannels.setOnAction(e -> showChannels());

        Button btnLogs = new Button("Logs");
        btnLogs.setMaxWidth(Double.MAX_VALUE);
        btnLogs.setOnAction(e -> showLogs());

        box.getChildren().addAll(title, btnDashboard, btnUsers, btnChannels, btnLogs);
        return box;
    }

    private void showDashboard() {
        contentPane.getChildren().setAll(dashboardController.getView());
    }

    private void showUsers() {
        contentPane.getChildren().setAll(usersController.getView());
    }

    private void showChannels() {
        contentPane.getChildren().setAll(channelsController.getView());
    }

    private void showLogs() {
        contentPane.getChildren().setAll(logsController.getView());
    }
}


