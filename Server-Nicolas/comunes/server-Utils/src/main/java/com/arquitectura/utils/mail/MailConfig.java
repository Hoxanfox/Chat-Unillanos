package com.arquitectura.utils.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

// En una clase de configuración, por ejemplo una nueva 'MailConfig.java'
@Configuration
@PropertySource(value = "file:./config/mail.properties", ignoreResourceNotFound = true)
public class MailConfig {

    @Value("${mail.host:smtp.gmail.com}") private String host;
    @Value("${mail.port:587}") private int port;
    @Value("${mail.username:}") private String username;
    @Value("${mail.password:}") private String password;

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true"); // Útil para depurar

        return mailSender;
    }
}