package com.unillanos.server.gui.controller;

import com.unillanos.server.dto.DTOCanal;
import com.unillanos.server.dto.DTOMiembroCanal;
import com.unillanos.server.gui.SharedContext;
import com.unillanos.server.gui.components.ToastNotification;
import com.unillanos.server.gui.styles.DesignSystem;
import com.unillanos.server.gui.styles.StyleUtils;
import com.unillanos.server.service.impl.AdminChannelsService;
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
 * Controlador de la vista de canales.
 */
public class ChannelsController {

    private final BorderPane root;
    private final TableView<DTOCanal> channelsTable;
    private final TableView<DTOMiembroCanal> membersTable;
    private final ObservableList<DTOCanal> channelsData;
    private final ObservableList<DTOMiembroCanal> membersData;
    private final Pagination pagination;

    private final AdminChannelsService channelsService;

    private static final int PAGE_SIZE = 20;

    public ChannelsController() {
        this.root = new BorderPane();
        this.root.setPadding(new Insets(16));

        channelsService = SharedContext.get().getBean(AdminChannelsService.class);

            Label header = new Label("Canales");
            header.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1F2937; -fx-padding: 16px 0px;");

        channelsTable = new TableView<>();
        channelsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<DTOCanal, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNombre()));
        TableColumn<DTOCanal, String> colActivo = new TableColumn<>("Activo");
        colActivo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Boolean.toString(c.getValue().isActivo())));
        TableColumn<DTOCanal, String> colMiembros = new TableColumn<>("Miembros");
        colMiembros.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Integer.toString(c.getValue().getCantidadMiembros())));
        channelsTable.getColumns().addAll(colNombre, colActivo, colMiembros);

        channelsData = FXCollections.observableArrayList();
        channelsTable.setItems(channelsData);

        membersTable = new TableView<>();
        membersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<DTOMiembroCanal, String> colUsuario = new TableColumn<>("Usuario");
        colUsuario.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNombreUsuario()));
        TableColumn<DTOMiembroCanal, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRol()));
        membersTable.getColumns().addAll(colUsuario, colRol);

        membersData = FXCollections.observableArrayList();
        membersTable.setItems(membersData);

        // Cargar miembros al seleccionar canal
        channelsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                List<DTOMiembroCanal> miembros = channelsService.listMembers(sel.getId());
                membersData.setAll(miembros);
            } else {
                membersData.clear();
            }
        });

        Button btnCreate = new Button("Crear canal");
        btnCreate.setOnAction(e -> showCreateDialog());
        Button btnActivate = new Button("Activar");
        btnActivate.setOnAction(e -> withSelectedChannel(c -> {
            try {
                channelsService.setActive(c.getId(), true);
                ToastNotification.showSuccess(
                    SharedContext.getPrimaryStage(),
                    "Canal '" + c.getNombre() + "' activado exitosamente"
                );
            } catch (Exception ex) {
                ToastNotification.showError(
                    SharedContext.getPrimaryStage(),
                    "Error al activar canal: " + ex.getMessage()
                );
            }
        }));
        Button btnDeactivate = new Button("Desactivar");
        btnDeactivate.setOnAction(e -> withSelectedChannel(c -> {
            try {
                channelsService.setActive(c.getId(), false);
                ToastNotification.showWarning(
                    SharedContext.getPrimaryStage(),
                    "Canal '" + c.getNombre() + "' desactivado exitosamente"
                );
            } catch (Exception ex) {
                ToastNotification.showError(
                    SharedContext.getPrimaryStage(),
                    "Error al desactivar canal: " + ex.getMessage()
                );
            }
        }));

        HBox actions = new HBox(8, btnCreate, btnActivate, btnDeactivate);
        actions.setAlignment(Pos.CENTER_LEFT);

        pagination = new Pagination(1, 0);
        pagination.setPageFactory(this::loadPage);

        HBox center = new HBox(12, channelsTable, membersTable);
        center.setAlignment(Pos.CENTER_LEFT);
        VBox container = new VBox(10, header, actions, center, pagination);
        root.setCenter(container);

        // Cargar primera página
        loadPage(0);
    }

    private Node loadPage(int pageIndex) {
        try {
            int offset = pageIndex * PAGE_SIZE;
            List<DTOCanal> canales = channelsService.listChannels(PAGE_SIZE, offset);
            channelsData.setAll(canales);
            pagination.setPageCount(canales.size() < PAGE_SIZE && pageIndex == 0 ? 1 : pageIndex + 2);
        } catch (Exception e) {
            ToastNotification.showError(
                SharedContext.getPrimaryStage(),
                "Error al cargar canales: " + e.getMessage()
            );
        }
        return channelsTable;
    }

    public Node getView() {
        return root;
    }

    private void showCreateDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Crear canal");
        TextField tfNombre = new TextField();
        tfNombre.setPromptText("Nombre");
        TextField tfDesc = new TextField();
        tfDesc.setPromptText("Descripción");
        TextField tfCreador = new TextField();
        tfCreador.setPromptText("CreadorId");
        VBox content = new VBox(8, new Label("Nombre"), tfNombre, new Label("Descripción"), tfDesc, new Label("CreadorId"), tfCreador);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        ButtonType btCrear = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btCrear, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> {
            if (bt == btCrear) {
                try {
                    var dto = channelsService.createChannel(tfNombre.getText(), tfDesc.getText(), tfCreador.getText());
                    ToastNotification.showSuccess(
                        SharedContext.getPrimaryStage(),
                        "Canal '" + tfNombre.getText() + "' creado exitosamente"
                    );
                    loadPage(0);
                } catch (Exception ex) {
                    ToastNotification.showError(
                        SharedContext.getPrimaryStage(),
                        "Error al crear canal: " + ex.getMessage()
                    );
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void withSelectedChannel(java.util.function.Consumer<DTOCanal> action) {
        DTOCanal sel = channelsTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            action.accept(sel);
            loadPage(0);
        } else {
            ToastNotification.showWarning(
                SharedContext.getPrimaryStage(),
                "Por favor selecciona un canal primero"
            );
        }
    }
}



