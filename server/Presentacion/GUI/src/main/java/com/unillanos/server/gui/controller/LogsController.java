package com.unillanos.server.gui.controller;

import com.unillanos.server.gui.SharedContext;
import com.unillanos.server.service.impl.LoggerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Controlador de la vista de logs con stream en vivo y exportación CSV.
 */
public class LogsController {

    private final VBox root;
    private final ListView<String> listView;
    private final CheckBox chkLive;
    private final ComboBox<String> cmbTipo;
    private final TextField txtUsuario;
    private final Button btnExportCsv;
    private final LoggerService loggerService;
    private Consumer<String> listener;

    public LogsController() {
        this.root = new VBox(10);
        this.root.setPadding(new Insets(16));
        this.root.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("Logs");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        chkLive = new CheckBox("En vivo");
        chkLive.setSelected(true);
        cmbTipo = new ComboBox<>();
        cmbTipo.getItems().addAll("", "INFO", "ERROR", "SYSTEM", "LOGIN", "LOGOUT");
        cmbTipo.setPromptText("Tipo");
        txtUsuario = new TextField();
        txtUsuario.setPromptText("UsuarioId");
        btnExportCsv = new Button("Exportar CSV");

        HBox filters = new HBox(8, chkLive, cmbTipo, txtUsuario, btnExportCsv);
        filters.setAlignment(Pos.CENTER_LEFT);

        listView = new ListView<>();
        listView.setPrefHeight(520);

        root.getChildren().addAll(header, filters, listView);

        loggerService = SharedContext.get().getBean(LoggerService.class);
        attachLiveListener();

        btnExportCsv.setOnAction(e -> exportCsv());
        chkLive.setOnAction(e -> {
            if (chkLive.isSelected()) attachLiveListener(); else detachLiveListener();
        });
    }

    private void attachLiveListener() {
        detachLiveListener();
        listener = line -> {
            // Filtros básicos en cliente por tipo/usuarioId si están informados
            String tipo = cmbTipo.getValue();
            String usuario = txtUsuario.getText();
            boolean okTipo = (tipo == null || tipo.isBlank()) || line.contains("[" + tipo + "]");
            boolean okUsuario = (usuario == null || usuario.isBlank()) || line.contains(usuario);
            if (okTipo && okUsuario) {
                javafx.application.Platform.runLater(() -> {
                    listView.getItems().add(line);
                    listView.scrollTo(listView.getItems().size() - 1);
                });
            }
        };
        loggerService.addListener(listener);
    }

    private void detachLiveListener() {
        if (listener != null) {
            loggerService.removeListener(listener);
            listener = null;
        }
    }

    private void exportCsv() {
        StringBuilder sb = new StringBuilder();
        for (String s : listView.getItems()) {
            String csvLine = s.replaceAll(",", " ");
            sb.append(csvLine).append("\n");
        }
        // Mostrar en diálogo simple (en un caso real, guardar a archivo)
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setWrapText(false);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("CSV");
        dialog.getDialogPane().setContent(ta);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    public Node getView() {
        return root;
    }
}



