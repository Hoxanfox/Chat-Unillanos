package com.unillanos.server.gui.controller;

import com.unillanos.server.gui.SharedContext;
import com.unillanos.server.gui.components.DebouncedSearchField;
import com.unillanos.server.gui.components.ToastNotification;
import com.unillanos.server.gui.styles.DesignSystem;
import com.unillanos.server.gui.styles.StyleUtils;
import com.unillanos.server.dto.DTOUsuario;
import com.unillanos.server.service.impl.AdminUsersService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controlador de la vista de usuarios.
 * Muestra tabla con paginación ligera, búsqueda y acciones básicas.
 */
public class UsersController {

    private final BorderPane root;
    private final DebouncedSearchField searchField;
    private final ComboBox<String> estadoFilter;
    private final TableView<DTOUsuario> table;
    private final Pagination pagination;
    private final ObservableList<DTOUsuario> data;

    private final AdminUsersService usersService;

    private static final int PAGE_SIZE = 20;

    public UsersController() {
        this.root = new BorderPane();
        this.root.setPadding(new Insets(16));

        usersService = SharedContext.get().getBean(AdminUsersService.class);

        Label header = new Label("Usuarios");
        header.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1F2937; -fx-padding: 16px 0px;");

        searchField = new DebouncedSearchField(
            "Buscar por nombre o email...",
            searchTerm -> loadPage(0),
            300 // 300ms delay
        );

        estadoFilter = new ComboBox<>();
        estadoFilter.getItems().addAll("", "ONLINE", "OFFLINE", "AWAY");
        estadoFilter.setPromptText("Estado");

        HBox topBar = new HBox(8, header, new Label(" "), searchField, estadoFilter);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 12, 0));

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<DTOUsuario, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNombre()));
        TableColumn<DTOUsuario, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEmail()));
        TableColumn<DTOUsuario, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEstado()));
        table.getColumns().addAll(colNombre, colEmail, colEstado);
        
        // Aplicar estilos a las columnas

        data = FXCollections.observableArrayList();
        table.setItems(data);

        // Inicializar paginación antes de acciones que la referencian
        pagination = new Pagination(1, 0);
        pagination.setPageFactory(this::loadPage);

        TableColumn<DTOUsuario, Void> colActions = new TableColumn<>("Acciones");
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnLogout = new Button("Forzar logout");
            private final Button btnOffline = new Button("OFFLINE");
            {
                btnLogout.setOnAction(e -> {
                    DTOUsuario u = getTableView().getItems().get(getIndex());
                    try {
                        usersService.forceLogout(u.getId());
                        ToastNotification.showSuccess(
                            SharedContext.getPrimaryStage(),
                            "Usuario " + u.getNombre() + " desconectado exitosamente"
                        );
                        loadPage(pagination.getCurrentPageIndex());
                    } catch (Exception ex) {
                        ToastNotification.showError(
                            SharedContext.getPrimaryStage(),
                            "Error al desconectar usuario: " + ex.getMessage()
                        );
                    }
                });
                btnOffline.setOnAction(e -> {
                    DTOUsuario u = getTableView().getItems().get(getIndex());
                    try {
                        usersService.changeEstado(u.getId(), "OFFLINE");
                        ToastNotification.showInfo(
                            SharedContext.getPrimaryStage(),
                            "Estado del usuario " + u.getNombre() + " cambiado a OFFLINE"
                        );
                        loadPage(pagination.getCurrentPageIndex());
                    } catch (Exception ex) {
                        ToastNotification.showError(
                            SharedContext.getPrimaryStage(),
                            "Error al cambiar estado: " + ex.getMessage()
                        );
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(6, btnLogout, btnOffline);
                    setGraphic(box);
                }
            }
        });
        table.getColumns().add(colActions);

        VBox center = new VBox(8, table, pagination);
        root.setTop(topBar);
        root.setCenter(center);

        // Cargar primera página
        loadPage(0);
    }

    private long lastSearchAt = 0;
    private void debounceLoad() {
        long now = System.currentTimeMillis();
        lastSearchAt = now;
        new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            if (now == lastSearchAt) javafx.application.Platform.runLater(() -> loadPage(0));
        }).start();
    }

    private Node loadPage(int pageIndex) {
        try {
            int offset = pageIndex * PAGE_SIZE;
            String q = searchField.getText();
            String estado = estadoFilter.getValue();
            if ("TODOS".equals(estado)) {
                estado = null;
            }
            
            List<DTOUsuario> list = usersService.listUsers(q, PAGE_SIZE, offset, estado);
            data.setAll(list);
            pagination.setPageCount(list.size() < PAGE_SIZE && pageIndex == 0 ? 1 : pageIndex + 2);
            
            // Mostrar información en la barra de estado si está disponible
            if (list.isEmpty() && (q != null && !q.trim().isEmpty())) {
                ToastNotification.showWarning(
                    SharedContext.getPrimaryStage(),
                    "No se encontraron usuarios con el criterio de búsqueda: " + q
                );
            }
            
        } catch (Exception e) {
            ToastNotification.showError(
                SharedContext.getPrimaryStage(),
                "Error al cargar usuarios: " + e.getMessage()
            );
        }
        return table;
    }

    public Node getView() {
        return root;
    }
}


