package com.arquitectura.utils.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía las credenciales de bienvenida a un nuevo usuario.
     * @param toEmail El email del destinatario.
     * @param username El nombre de usuario.
     * @param rawPassword La contraseña en texto plano.
     */
    public void enviarCredenciales(String toEmail, String username, String rawPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@chatapp.com"); // Puedes poner el email que quieras
            message.setTo(toEmail);
            message.setSubject("¡Bienvenido a nuestro Chat!");
            message.setText("Hola " + username + ",\n\n" +
                            "Tu cuenta ha sido creada exitosamente.\n" +
                            "Tus credenciales de acceso son:\n" +
                            "Usuario: " + username + "\n" +
                            "Contraseña: " + rawPassword + "\n\n");

            mailSender.send(message);
            System.out.println("Correo de credenciales enviado a: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error al enviar el correo de credenciales: " + e.getMessage());
            // En una aplicación real, aquí deberías usar un logger.
        }
    }
}