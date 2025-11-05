package com.arquitectura.app;

import com.arquitectura.controlador.ServerViewController;
import com.arquitectura.utils.mail.MailConfig;
import com.arquitectura.configdb.ConfiguracionPersistencia;
import com.arquitectura.transporte.ServerListener;
import com.arquitectura.vista.ServerMainWindow;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.SwingUtilities;

public class ServerLauncher {

    public static void main(String[] args) {
        // 1. Crear el contexto de Spring a partir de nuestras clases de configuración.
        // ApplicationConfig es clave aquí porque tiene la anotación @ComponentScan("com.arquitectura"),
        // que le dice a Spring que busque todos los @Component, @Service, @Controller, etc.
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            ConfiguracionPersistencia.class,
            MailConfig.class,
            ApplicationConfig.class
        );

        // Asegurarse de que el contexto se cierre correctamente al terminar la aplicación.
        context.registerShutdownHook();

        // 2. Obtener los beans principales que necesitamos para arrancar.
        ServerListener serverListener = context.getBean(ServerListener.class);
        ServerViewController viewController = context.getBean(ServerViewController.class);

        // 3. Iniciar el servidor de sockets en un hilo separado para no bloquear la GUI.
        Thread serverThread = new Thread(serverListener::startServer);
        serverThread.setDaemon(true); // Esto permite que la JVM se cierre aunque este hilo esté corriendo
        serverThread.start();

        // 4. Lanzar la interfaz gráfica de administrador en el hilo de eventos de Swing.
        SwingUtilities.invokeLater(() -> {
            ServerMainWindow mainWindow = new ServerMainWindow(viewController);
            mainWindow.display();
        });
    }
}