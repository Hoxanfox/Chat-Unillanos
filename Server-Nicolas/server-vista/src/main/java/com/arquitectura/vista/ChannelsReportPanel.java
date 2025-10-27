package com.arquitectura.vista;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ChannelsReportPanel extends JPanel {

    private final ServerViewController controller;
    private final JTextArea reportTextArea;

    public ChannelsReportPanel(ServerViewController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());

        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(reportTextArea), BorderLayout.CENTER);
    }

    public void refreshReport() {
        Map<ChannelResponseDto, List<UserResponseDto>> data = controller.obtenerCanalesConMiembros();
        
        StringBuilder reportContent = new StringBuilder();
        reportContent.append("--- INFORME DE CANALES Y SUS MIEMBROS ---\n\n");

        if (data == null || data.isEmpty()) {
            reportContent.append("No hay canales creados en el sistema.");
        } else {
            for (Map.Entry<ChannelResponseDto, List<UserResponseDto>> entry : data.entrySet()) {
                ChannelResponseDto channel = entry.getKey();
                List<UserResponseDto> members = entry.getValue();

                reportContent.append("======================================================\n");
                reportContent.append(String.format("CANAL: %s (ID: %d)\n", channel.getChannelName(), channel.getChannelId()));
                reportContent.append(String.format("TIPO: %s | PROPIETARIO: %s\n", channel.getChannelType(), channel.getOwner().getUsername()));
                reportContent.append("------------------------------------------------------\n");
                reportContent.append("Miembros Activos:\n");

                if (members.isEmpty()) {
                    reportContent.append("  - (No hay miembros activos además del propietario en canales de grupo)\n");
                } else {
                    for (UserResponseDto member : members) {
                        reportContent.append(String.format("  - %s (ID: %d)\n", member.getUsername(), member.getUserId()));
                    }
                }
                reportContent.append("\n");
            }
        }
        reportTextArea.setText(reportContent.toString());
        reportTextArea.setCaretPosition(0); // Mueve el scroll al principio
    }
}