package com.arquitectura.vista;

import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TranscriptionsReportPanel extends JPanel {

    private final ServerViewController controller;
    private final JTextArea reportTextArea;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TranscriptionsReportPanel(ServerViewController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());

        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(reportTextArea), BorderLayout.CENTER);
    }

    public void refreshReport() {
        List<TranscriptionResponseDto> data = controller.obtenerTranscripciones();
        
        StringBuilder reportContent = new StringBuilder();
        reportContent.append("--- INFORME DE TRANSCRIPCIONES DE AUDIO ---\n\n");

        if (data == null || data.isEmpty()) {
            reportContent.append("No hay transcripciones de audio en el sistema.");
        } else {
            for (TranscriptionResponseDto dto : data) {
                reportContent.append("==================================================================\n");
                reportContent.append(String.format("ID Mensaje: %d | Canal ID: %d\n", dto.getMessageId(), dto.getChannelId()));
                reportContent.append(String.format("Autor: %s | Fecha: %s\n", dto.getAuthor().getUsername(), dto.getProcessedDate().format(formatter)));
                reportContent.append("------------------------------------------------------------------\n");
                reportContent.append("Texto Transcrito:\n");
                reportContent.append(String.format("  \"%s\"\n\n", dto.getTranscribedText()));
            }
        }
        reportTextArea.setText(reportContent.toString());
        reportTextArea.setCaretPosition(0); // Mueve el scroll al principio
    }
}