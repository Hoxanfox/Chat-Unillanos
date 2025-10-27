package com.arquitectura.vista;

import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import java.awt.*;

public class BroadcastMessagePanel extends JPanel {

    private final ServerViewController controller;
    private JTextArea messageTextArea;
    private JButton sendButton;

    public BroadcastMessagePanel(ServerViewController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Título del panel
        JLabel title = new JLabel("Enviar Mensaje a Todos (Broadcast)", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 16));
        add(title, BorderLayout.NORTH);

        // Área de texto para el mensaje
        messageTextArea = new JTextArea();
        messageTextArea.setLineWrap(true);
        messageTextArea.setWrapStyleWord(true);
        add(new JScrollPane(messageTextArea), BorderLayout.CENTER);

        // Botón para enviar
        sendButton = new JButton("Enviar Mensaje");
        add(sendButton, BorderLayout.SOUTH);

        // Listener para el botón de enviar
        sendButton.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String message = messageTextArea.getText();
        if (message.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El mensaje no puede estar vacío.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Llama al controlador para enviar el mensaje
            controller.enviarMensajeBroadcast(message);
            JOptionPane.showMessageDialog(this, "Mensaje broadcast enviado con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            messageTextArea.setText(""); // Limpia el área de texto
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al enviar el mensaje: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}