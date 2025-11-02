package com.arquitectura.transporte;

import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.controlador.IClientHandler; // <-- Asegúrate de que este import exista
import com.arquitectura.controlador.RequestDispatcher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientHandler implements Runnable, IClientHandler {

    private final Socket clientSocket;
    private final RequestDispatcher requestDispatcher;
    private final ServerListener serverListener;
    private PrintWriter out;
    private BufferedReader in;
    private final Consumer<ClientHandler> onDisconnect;
    private UserResponseDto authenticatedUser = null;

    public ClientHandler(Socket socket, RequestDispatcher dispatcher, ServerListener serverListener, Consumer<ClientHandler> onDisconnect) {
        this.clientSocket = socket;
        this.requestDispatcher = dispatcher;
        this.serverListener = serverListener;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                requestDispatcher.dispatch(inputLine, this);
            }

        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + e.getMessage());
        } finally {
            onDisconnect.accept(this);
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void forceDisconnect() {
        try {
            // Cierra el socket. Esto causará una SocketException en el hilo
            // que está leyendo (en el método run()), lo que terminará el bucle
            // y limpiará la conexión de forma natural.
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            // Usar el logger que deberíamos tener aquí
            System.err.println("Error al forzar la desconexión: " + e.getMessage());
        }
    }

    @Override
    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public String getClientIpAddress() {
        return clientSocket.getInetAddress().getHostAddress();
    }
    @Override
    public void setAuthenticatedUser(UserResponseDto user) {
        this.authenticatedUser = user;
        if (user != null) {
            this.serverListener.registerAuthenticatedClient(this);
        }
    }

    @Override
    public UserResponseDto getAuthenticatedUser() {
        return this.authenticatedUser;
    }

    @Override
    public boolean isAuthenticated() {
        return this.authenticatedUser != null;
    }
    @Override
    public void clearAuthenticatedUser() {
        this.authenticatedUser = null;
    }
}