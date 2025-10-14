package com.unillanos.server.gui.controller;

import com.unillanos.server.gui.SharedContext;
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
    private final TextField searchField;
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
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        searchField = new TextField();
        searchField.setPromptText("Buscar por nombre o email...");
        Button btnSearch = new Button("Buscar");
        btnSearch.setOnAction(e -> debounceLoad());

        ComboBox<String> estadoFilter = new ComboBox<>();
        estadoFilter.getItems().addAll("", "ONLINE", "OFFLINE", "AWAY");
        estadoFilter.setPromptText("Estado");

        HBox topBar = new HBox(8, header, new Label(" "), searchField, estadoFilter, btnSearch);
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
                    usersService.forceLogout(u.getId());
                    loadPage(pagination.getCurrentPageIndex());
                });
                btnOffline.setOnAction(e -> {
                    DTOUsuario u = getTableView().getItems().get(getIndex());
                    usersService.changeEstado(u.getId(), "OFFLINE");
                    loadPage(pagination.getCurrentPageIndex());
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
        int offset = pageIndex * PAGE_SIZE;
        String q = searchField.getText();
        String estado = null;
        if (root.getTop() instanceof HBox top) {
            for (var node : top.getChildren()) {
                if (node instanceof ComboBox<?> cb) {
                    Object v = ((ComboBox<?>) node).getValue();
                    estado = v != null ? v.toString() : null;
                }
            }
        }
        List<DTOUsuario> list = usersService.listUsers(q, PAGE_SIZE, offset, estado);
        data.setAll(list);
        pagination.setPageCount(list.size() < PAGE_SIZE && pageIndex == 0 ? 1 : pageIndex + 2);
        return table;
    }

    public Node getView() {
        return root;
    }
}


