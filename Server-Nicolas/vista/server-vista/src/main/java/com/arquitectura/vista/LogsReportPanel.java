package com.arquitectura.vista;

import com.arquitectura.controlador.ServerViewController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LogsReportPanel extends JPanel {

    private final ServerViewController controller;
    private final JTextArea reportTextArea;
    private final JCheckBox autoRefreshCheckBox;
    private final Timer refreshTimer;

    public LogsReportPanel(ServerViewController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(5, 5));

        // Área de texto para los logs
        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(reportTextArea), BorderLayout.CENTER);
        
        // Panel inferior con controles
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        autoRefreshCheckBox = new JCheckBox("Auto-refrescar cada segundos", true);
        controlsPanel.add(autoRefreshCheckBox);
        add(controlsPanel, BorderLayout.SOUTH);

        // Temporizador para el auto-refresco
        refreshTimer = new Timer(1000, e -> {
            if (autoRefreshCheckBox.isSelected()) {
                refreshReport(false); // No mover el scroll al auto-refrescar
            }
        });

        // Listener para el CheckBox
        autoRefreshCheckBox.addActionListener(e -> {
            if (autoRefreshCheckBox.isSelected()) {
                refreshTimer.start();
            } else {
                refreshTimer.stop();
            }
        });
    }

    public void startAutoRefresh() {
        refreshTimer.start();
    }
    
    public void stopAutoRefresh() {
        refreshTimer.stop();
    }

    public void refreshReport(boolean scrollToTop) {
        String logs = controller.getLogContents();
        reportTextArea.setText(logs);
        if (scrollToTop) {
            reportTextArea.setCaretPosition(0);
        } else {
            // Mueve el scroll al final para ver los últimos logs
            reportTextArea.setCaretPosition(reportTextArea.getDocument().getLength());
        }
    }
}