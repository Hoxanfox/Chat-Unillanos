package com.arquitectura.vista;

import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class RegisterUserPanel extends JPanel {
    private final ServerViewController controller;
    private File selectedPhotoFile;

    public RegisterUserPanel(ServerViewController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel title = new JLabel("Registrar Usuario");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(title, gbc);

        // Fila Usuario
        gbc.gridy++;
        gbc.gridwidth = 1;
        add(new JLabel("Usuario:"), gbc);

        gbc.gridx = 1;
        JTextField userField = new JTextField(20);
        add(userField, gbc);

        // Fila Email
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        JTextField emailField = new JTextField(20);
        add(emailField, gbc);

        // Fila Contraseña
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Contraseña:"), gbc);

        gbc.gridx = 1;
        JPasswordField passField = new JPasswordField(20);
        add(passField, gbc);

        // Fila Foto
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Foto:"), gbc);

        gbc.gridx = 1;
        JPanel photoPanel = new JPanel(new BorderLayout(5, 0));
        JLabel photoFileNameLabel = new JLabel("(Ningún archivo seleccionado)");
        JButton btnSelectPhoto = new JButton("Seleccionar...");
        photoPanel.add(photoFileNameLabel, BorderLayout.CENTER);
        photoPanel.add(btnSelectPhoto, BorderLayout.EAST);
        add(photoPanel, gbc);

        // Botón
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnCreateUser = new JButton("Crear Usuario");
        add(btnCreateUser, gbc);

        // Listener para el botón de seleccionar foto
        btnSelectPhoto.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Imágenes (JPG, PNG, GIF)", "jpg", "png", "gif");
            fileChooser.setFileFilter(filter);
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedPhotoFile = fileChooser.getSelectedFile();
                photoFileNameLabel.setText(selectedPhotoFile.getName());
            }
        });

        // Listener boton creacion
        btnCreateUser.addActionListener(e -> {
            try {
                String photoPath = (selectedPhotoFile != null) ? selectedPhotoFile.getAbsolutePath() : null;

                UserRegistrationRequestDto dto = new UserRegistrationRequestDto(
                        userField.getText(),
                        emailField.getText(),
                        new String(passField.getPassword()),
                        photoPath // <-- Pasamos la ruta del archivo
                );
                controller.registrarNuevoUsuario(dto);
                JOptionPane.showMessageDialog(this, "Usuario creado exitosamente!", "Éxito", JOptionPane.INFORMATION_MESSAGE);

                // Limpiar campos y el archivo seleccionado
                userField.setText("");
                emailField.setText("");
                passField.setText("");
                photoFileNameLabel.setText("(Ningún archivo seleccionado)");
                selectedPhotoFile = null;

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al crear usuario: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
