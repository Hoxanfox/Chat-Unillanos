package controlador.p2p;

import dto.p2p.DTOPeerDetails;
import java.util.Arrays;
import java.util.List;

/**
 * Controlador específico para la interfaz de texto.
 * Su única responsabilidad es parsear Strings y llamar al ControladorP2P puro.
 * INCLUYE DEPURACIÓN VISUAL Y SOPORTE PARA PUERTOS LÓGICOS.
 */
public class ControladorConsola {

    private final ControladorP2P controladorPuro;

    // --- ESTILOS VACANOS (ANSI) ---
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String ROJO = "\u001B[31m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String BLANCO = "\u001B[37m";

    public ControladorConsola() {
        printDebug("Inicializando el intérprete de comandos (ControladorConsola)...");
        this.controladorPuro = new ControladorP2P();
        printDebug("Enlace con ControladorP2P puro establecido.");
    }

    public void procesarComando(String lineaCompleta) {
        if (lineaCompleta == null || lineaCompleta.trim().isEmpty()) return;

        String[] partes = lineaCompleta.trim().split("\\s+");
        String comando = partes[0].toUpperCase();

        String argumentos = partes.length > 1
                ? String.join(" ", Arrays.copyOfRange(partes, 1, partes.length))
                : "";

        try {
            switch (comando) {
                case "START":
                case "INICIAR":
                    printInfo("Ejecutando secuencia de inicio...");
                    controladorPuro.iniciarRed();
                    printSuccess("Orden de inicio enviada (Check logs para estado).");
                    break;

                case "CHAT":
                    if (argumentos.isEmpty()) {
                        printError("Falta el mensaje. Uso: CHAT <mensaje>");
                    } else {
                        printInfo("Enviando mensaje global...");
                        controladorPuro.enviarMensajeChat(argumentos);
                    }
                    break;

                case "PRIVADO":
                    if (partes.length < 3) {
                        printError("Uso incorrecto. Formato: PRIVADO <id> <mensaje>");
                    } else {
                        String idDestino = partes[1];
                        String msgPrivado = String.join(" ", Arrays.copyOfRange(partes, 2, partes.length));
                        printInfo("Enviando privado a ID: " + idDestino);
                        controladorPuro.enviarMensajePrivado(idDestino, msgPrivado);
                    }
                    break;

                case "LIST":
                case "LISTA":
                    // printInfo("Solicitando lista de peers...");
                    List<DTOPeerDetails> peers = controladorPuro.obtenerListaPeers();
                    System.out.println(MAGENTA + "\n--- Peers Conectados (" + peers.size() + ") ---" + RESET);
                    if (peers.isEmpty()) {
                        System.out.println(AMARILLO + " (Ninguno) " + RESET);
                    } else {
                        // AQUI MOSTRAMOS EL PUERTO SERVIDOR (EL REAL DE ESCUCHA)
                        peers.forEach(p -> {
                            int puertoMostrar = p.getPuertoServidor() > 0 ? p.getPuertoServidor() : p.getPuerto();
                            String estadoColor = "ONLINE".equals(p.getEstado()) ? VERDE : ROJO;

                            System.out.println(
                                    VERDE + " ➤ " + RESET + BLANCO + p.getId() + RESET +
                                            " | " + CYAN + p.getIp() + ":" + puertoMostrar + RESET +
                                            " [" + estadoColor + p.getEstado() + RESET + "]"
                            );
                        });
                    }
                    System.out.println(MAGENTA + "--------------------------------" + RESET);
                    break;

                case "CONNECT":
                    if (partes.length < 3) {
                        printError("Uso incorrecto. Formato: CONNECT <ip> <puerto>");
                    } else {
                        String ip = partes[1];
                        int puerto = Integer.parseInt(partes[2]);
                        printInfo("Conectando manualmente a " + ip + ":" + puerto);
                        controladorPuro.conectarManual(ip, puerto);
                    }
                    break;

                case "EXIT":
                case "SALIR":
                    printInfo("Apagando sistema...");
                    controladorPuro.detenerRed();
                    printSuccess("Sistema detenido. ¡Hasta luego!");
                    System.exit(0);
                    break;

                case "HELP":
                case "AYUDA":
                    mostrarAyuda();
                    break;

                default:
                    printError("Comando desconocido '" + comando + "'. Escribe HELP.");
            }
        } catch (Exception e) {
            printError("Excepción al ejecutar comando: " + e.getMessage());
        }
    }

    private void mostrarAyuda() {
        System.out.println(CYAN + "\n┌──[AYUDA DE COMANDOS]──────────────────────────────┐" + RESET);
        System.out.println("│ " + VERDE + "START" + RESET + "            : Inicia el nodo y conecta.      │");
        System.out.println("│ " + VERDE + "CHAT <msg>" + RESET + "       : Chat global (Broadcast).       │");
        System.out.println("│ " + VERDE + "PRIVADO <id> <m>" + RESET + " : Chat privado.                  │");
        System.out.println("│ " + VERDE + "LIST" + RESET + "             : Ver lista de peers.            │");
        System.out.println("│ " + VERDE + "CONNECT <ip> <p>" + RESET + " : Conexión manual.               │");
        System.out.println("│ " + ROJO + "EXIT" + RESET + "             : Salir.                         │");
        System.out.println(CYAN + "└───────────────────────────────────────────────────┘" + RESET);
    }

    // --- MÉTODOS AUXILIARES ---

    private void printDebug(String msg) {
        System.out.println(MAGENTA + "[DEBUG-CMD] " + msg + RESET);
    }

    private void printInfo(String msg) {
        System.out.println(CYAN + "[CMD] " + msg + RESET);
    }

    private void printSuccess(String msg) {
        System.out.println(VERDE + "[OK] " + msg + RESET);
    }

    private void printError(String msg) {
        System.out.println(ROJO + "[ERROR] " + msg + RESET);
    }
}