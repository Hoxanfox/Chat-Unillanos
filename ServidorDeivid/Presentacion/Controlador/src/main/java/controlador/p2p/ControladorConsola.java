package controlador.p2p;

import dto.p2p.DTOPeerDetails;
import java.util.Arrays;
import java.util.List;

public class ControladorConsola {

    private final ControladorP2P controladorPuro;

    // Colores ANSI
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String ROJO = "\u001B[31m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String BLANCO = "\u001B[37m";
    private static final String GRIS = "\u001B[90m";

    public ControladorConsola() {
        printDebug("Inicializando ControladorConsola...");
        this.controladorPuro = new ControladorP2P();
        printDebug("Enlace con Servicio establecido.");
    }

    public void procesarComando(String lineaCompleta) {
        if (lineaCompleta == null || lineaCompleta.trim().isEmpty()) return;

        String[] partes = lineaCompleta.trim().split("\\s+");
        String comando = partes[0].toUpperCase();
        String argumentos = partes.length > 1 ? String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)) : "";

        try {
            switch (comando) {
                case "START":
                case "INICIAR":
                    printInfo("Iniciando secuencia de arranque...");
                    controladorPuro.iniciarRed();
                    printSuccess("Comando de inicio enviado.");
                    break;

                // NUEVO COMANDO SYNC
                case "SYNC":
                case "SINCRONIZAR":
                    if (!controladorPuro.isRedIniciada()) {
                        printError("La red no está iniciada.");
                    } else {
                        printInfo("Forzando sincronización de datos (Merkle Tree)...");
                        controladorPuro.sincronizarManual();
                        printSuccess("Solicitud de sincronización enviada a la red.");
                    }
                    break;

                case "CHAT":
                    if (argumentos.isEmpty()) printError("Falta mensaje. Uso: CHAT <msg>");
                    else controladorPuro.enviarMensajeChat(argumentos);
                    break;

                case "PRIVADO":
                    if (partes.length < 3) printError("Uso: PRIVADO <id> <mensaje>");
                    else {
                        String id = partes[1];
                        String msg = String.join(" ", Arrays.copyOfRange(partes, 2, partes.length));
                        controladorPuro.enviarMensajePrivado(id, msg);
                        printInfo("Mensaje privado enviado.");
                    }
                    break;

                case "LIST":
                case "LISTA":
                    List<DTOPeerDetails> peers = controladorPuro.obtenerListaPeers();
                    System.out.println(MAGENTA + "\n=== PEERS CONOCIDOS (" + peers.size() + ") ===" + RESET);
                    if (peers.isEmpty()) {
                        System.out.println(GRIS + " (Base de datos vacía)" + RESET);
                    } else {
                        for (DTOPeerDetails p : peers) {
                            int puertoReal = p.getPuertoServidor() > 0 ? p.getPuertoServidor() : p.getPuerto();
                            boolean isOnline = "ONLINE".equalsIgnoreCase(p.getEstado());
                            String color = isOnline ? VERDE : ROJO;
                            String icono = isOnline ? "●" : "○";
                            System.out.printf(" %s%s%s %s%-36s%s | %s%s:%d%s\n",
                                    color, icono, RESET, BLANCO, p.getId(), RESET, CYAN, p.getIp(), puertoReal, RESET);
                        }
                    }
                    System.out.println(MAGENTA + "================================" + RESET);
                    break;

                case "CONNECT":
                    if (partes.length < 3) printError("Uso: CONNECT <ip> <puerto>");
                    else {
                        controladorPuro.conectarManual(partes[1], Integer.parseInt(partes[2]));
                        printInfo("Conectando...");
                    }
                    break;

                case "EXIT":
                    printInfo("Cerrando...");
                    controladorPuro.detenerRed();
                    System.exit(0);
                    break;

                case "HELP":
                    mostrarAyuda();
                    break;

                default:
                    printError("Comando desconocido. Usa HELP.");
            }
        } catch (Exception e) {
            printError("Error: " + e.getMessage());
        }
    }

    private void mostrarAyuda() {
        System.out.println(CYAN + "\n┌── COMANDOS ───────────────────────┐" + RESET);
        System.out.println("│ " + VERDE + "START" + RESET + "   : Iniciar red P2P.        │");
        System.out.println("│ " + VERDE + "SYNC" + RESET + "    : Sincronizar datos BD.   │");
        System.out.println("│ " + VERDE + "CHAT" + RESET + "    : Enviar mensaje global.  │");
        System.out.println("│ " + VERDE + "LIST" + RESET + "    : Ver lista de peers.     │");
        System.out.println("│ " + ROJO + "EXIT" + RESET + "    : Salir.                  │");
        System.out.println(CYAN + "└───────────────────────────────────┘" + RESET);
    }

    private void printDebug(String msg) { System.out.println(MAGENTA + "[DEBUG] " + msg + RESET); }
    private void printInfo(String msg) { System.out.println(CYAN + "[INFO] " + msg + RESET); }
    private void printSuccess(String msg) { System.out.println(VERDE + "[OK] " + msg + RESET); }
    private void printError(String msg) { System.out.println(ROJO + "[ERROR] " + msg + RESET); }
}