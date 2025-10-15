package com.unillanos.server.gui;

import com.unillanos.server.gui.components.StatusBar;
import com.unillanos.server.gui.controller.ChannelsController;
import com.unillanos.server.gui.controller.DashboardController;
import com.unillanos.server.gui.controller.LogsController;
import com.unillanos.server.gui.controller.UsersController;
import com.unillanos.server.gui.styles.AnimationEffects;
import com.unillanos.server.gui.styles.DesignSystem;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Ventana principal de la GUI de administración con diseño moderno y elegante.
 * Contiene barra lateral de navegación minimalista y un área central con las vistas.
 */
public class MainWindow extends Application {

    private BorderPane root;
    private StackPane contentPane;
    private StatusBar statusBar;
    private VBox sidebar;
    
    private DashboardController dashboardController;
    private UsersController usersController;
    private ChannelsController channelsController;
    private LogsController logsController;
    
    private Map<String, Button> navButtons;
    private String currentView = "dashboard";

    @Override
    public void start(Stage primaryStage) {
        initializeComponents(primaryStage);
        buildModernLayout();
        setupAnimations();
        showDashboard();

        Scene scene = new Scene(root, 1200, 800);
        scene.setFill(Color.valueOf(DesignSystem.BACKGROUND_SECONDARY));
        
        primaryStage.setTitle("Chat-Unillanos • Panel de Administración");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setOnCloseRequest(e -> {
            if (statusBar != null) {
                statusBar.dispose();
            }
            System.exit(0);
        });
        primaryStage.show();
        
        // Aplicar efectos de entrada
        AnimationEffects.applyFadeInEffect(root);
    }

    private void initializeComponents(Stage primaryStage) {
        this.root = new BorderPane();
        this.contentPane = new StackPane();
        this.statusBar = new StatusBar();
        this.navButtons = new HashMap<>();

        // Configurar contexto compartido
        SharedContext.setPrimaryStage(primaryStage);

        // Obtener ApplicationContext compartido
        ApplicationContext ctx = SharedContext.get();

        // Inicializar controladores
        this.dashboardController = new DashboardController();
        this.usersController = new UsersController();
        this.channelsController = new ChannelsController();
        this.logsController = new LogsController();
    }

    private void buildModernLayout() {
        root.setStyle(DesignSystem.getMainBackgroundStyle());
        root.setPrefSize(1200, 800);

        // Construir sidebar moderno
        this.sidebar = buildModernSidebar();
        root.setLeft(sidebar);
        
        // Área de contenido con padding
        VBox contentContainer = new VBox();
        contentContainer.setStyle(DesignSystem.getContentContainerStyle());
        contentContainer.setPadding(new Insets(24));
        contentContainer.getChildren().add(contentPane);
        root.setCenter(contentContainer);
        
        // Barra de estado
        root.setBottom(statusBar);
    }

    private VBox buildModernSidebar() {
        VBox sidebar = new VBox();
        sidebar.setStyle(DesignSystem.getSidebarStyle());
        sidebar.setMinWidth(280);
        sidebar.setMaxWidth(280);
        sidebar.setSpacing(16);

        // Header con logo/brand
        VBox header = createSidebarHeader();
        
        // Navegación principal
        VBox navigation = createNavigationSection();
        
        // Separador
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: " + DesignSystem.GRAY_200 + ";");
        
        // Footer con información del sistema
        VBox footer = createSidebarFooter();
        
        sidebar.getChildren().addAll(header, separator, navigation, footer);
        return sidebar;
    }

    private VBox createSidebarHeader() {
        VBox header = new VBox();
        header.setSpacing(12);
        header.setPadding(new Insets(24, 16, 24, 16));

        // Avatar/Logo
        Circle avatar = new Circle(24);
        avatar.setFill(Color.valueOf(DesignSystem.PRIMARY));
        
        // Título principal
        Label title = new Label("Chat-Unillanos");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1F2937; -fx-padding: 16px 0px;");
        
        // Subtítulo
        Label subtitle = new Label("Panel de Administración");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");
        
        // Indicador de estado del servidor
        HBox statusIndicator = createServerStatusIndicator();
        
        header.getChildren().addAll(avatar, title, subtitle, statusIndicator);
        return header;
    }

    private HBox createServerStatusIndicator() {
        HBox statusBox = new HBox();
        statusBox.setSpacing(8);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Circle statusDot = new Circle(6);
        statusDot.setFill(Color.valueOf(DesignSystem.SUCCESS));
        
        Label statusText = new Label("Servidor Activo");
        statusText.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");
        
        statusBox.getChildren().addAll(statusDot, statusText);
        return statusBox;
    }

    private VBox createNavigationSection() {
        VBox navigation = new VBox();
        navigation.setSpacing(4);
        navigation.setPadding(new Insets(0, 16, 16, 16));

        // Crear botones de navegación
        Button dashboardBtn = createNavButton("📊", "Dashboard", "dashboard");
        Button usersBtn = createNavButton("👥", "Usuarios", "users");
        Button channelsBtn = createNavButton("💬", "Canales", "channels");
        Button logsBtn = createNavButton("📋", "Logs", "logs");

        navButtons.put("dashboard", dashboardBtn);
        navButtons.put("users", usersBtn);
        navButtons.put("channels", channelsBtn);
        navButtons.put("logs", logsBtn);

        navigation.getChildren().addAll(dashboardBtn, usersBtn, channelsBtn, logsBtn);
        return navigation;
    }

    private Button createNavButton(String icon, String text, String viewName) {
        Button button = new Button();
        button.setStyle(DesignSystem.getNavItemInactiveStyle());
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        
        // Layout con icono y texto
        HBox content = new HBox();
        content.setSpacing(12);
        content.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px;");
        
        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4B5563;");
        
        content.getChildren().addAll(iconLabel, textLabel);
        button.setGraphic(content);
        
        // Event handlers
        button.setOnAction(e -> navigateToView(viewName));
        
        // Efectos de hover
        AnimationEffects.applyHoverEffect(button);
        AnimationEffects.applyBounceClickEffect(button);
        
        return button;
    }

    private VBox createSidebarFooter() {
        VBox footer = new VBox();
        footer.setSpacing(8);
        footer.setPadding(new Insets(16));
        footer.setAlignment(Pos.CENTER);

        // Información de versión
        Label version = new Label("v1.0.0");
        version.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");
        
        // Copyright
        Label copyright = new Label("© 2024 Unillanos");
        copyright.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");
        
        footer.getChildren().addAll(version, copyright);
        return footer;
    }

    private void setupAnimations() {
        // Aplicar efectos de entrada a la sidebar
        Platform.runLater(() -> AnimationEffects.applySlideInFromLeft(sidebar));
    }

    private void navigateToView(String viewName) {
        if (viewName.equals(currentView)) {
            return; // Ya estamos en esta vista
        }

        // Actualizar estilos de navegación
        updateNavigationStyles(viewName);

        // Aplicar transición de contenido
        AnimationEffects.applyFadeOutEffect(contentPane, () -> {
            Platform.runLater(() -> {
                switch (viewName) {
                    case "dashboard":
                        contentPane.getChildren().setAll(dashboardController.getView());
                        AnimationEffects.applySlideInFromBottom(contentPane);
                        break;
                    case "users":
                        contentPane.getChildren().setAll(usersController.getView());
                        AnimationEffects.applySlideInFromBottom(contentPane);
                        break;
                    case "channels":
                        contentPane.getChildren().setAll(channelsController.getView());
                        AnimationEffects.applySlideInFromBottom(contentPane);
                        break;
                    case "logs":
                        contentPane.getChildren().setAll(logsController.getView());
                        AnimationEffects.applySlideInFromBottom(contentPane);
                        break;
                }
                currentView = viewName;
            });
        });
    }

    private void updateNavigationStyles(String activeView) {
        navButtons.forEach((viewName, button) -> {
            if (viewName.equals(activeView)) {
                button.setStyle(DesignSystem.getNavItemActiveStyle());
            } else {
                button.setStyle(DesignSystem.getNavItemInactiveStyle());
            }
        });
    }

    // Métodos legacy para compatibilidad
    private void showDashboard() {
        navigateToView("dashboard");
    }
    
    /**
     * Obtiene la referencia a la barra de estado.
     * Permite que los controladores actualicen el estado de la aplicación.
     *
     * @return StatusBar de la aplicación
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }
}