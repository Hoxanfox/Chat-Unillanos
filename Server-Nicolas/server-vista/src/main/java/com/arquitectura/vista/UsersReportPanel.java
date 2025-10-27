package com.arquitectura.vista;

import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class UsersReportPanel extends JPanel {
    private final ServerViewController controller;
    private final JTextArea reportTextArea;

    public UsersReportPanel(ServerViewController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());

        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(reportTextArea), BorderLayout.CENTER);
    }

    public void refreshReport() {
        List<UserResponseDto> users = controller.obtenerUsuariosRegistrados();
        StringBuilder reportContent = new StringBuilder();
        reportContent.append("--- INFORME DE USUARIOS REGISTRADOS ---\n\n");
        reportContent.append(String.format("%-5s | %-20s | %-25s\n", "ID", "Username", "Email"));
        reportContent.append("------------------------------------------------------\n");

        if (users == null || users.isEmpty()) {
            reportContent.append("No hay usuarios registrados.");
        } else {
            for (UserResponseDto user : users) {
                reportContent.append(String.format("%-5d | %-20s | %-25s\n",
                        user.getUserId(), user.getUsername(), user.getEmail()));
            }
        }
        reportTextArea.setText(reportContent.toString());
    }
}
