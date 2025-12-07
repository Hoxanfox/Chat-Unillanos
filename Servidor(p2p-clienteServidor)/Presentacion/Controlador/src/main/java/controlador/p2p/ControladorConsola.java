package controlador.p2p;

import dto.p2p.DTOPeerDetails;
import logger.LoggerCentral;
import java.util.Arrays;
import java.util.List;

public class ControladorConsola {

    private static final String TAG = "ControladorConsola";
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
        LoggerCentral.debug(TAG, "Inicializando ControladorConsola...");
        this.controladorPuro = new ControladorP2P();
        LoggerCentral.debug(TAG, "Enlace con ControladorP2P establecido.");

        // NUEVO: Suscribirse a eventos de conexión/desconexión
        configurarSuscripcionesObserver();
        LoggerCentral.info(TAG, "ControladorConsola inicializado correctamente.");
    }

    /**
     * Configura las suscripciones al patrón Observer para actualizar
     * automáticamente la consola cuando haya cambios en los peers.
     */
    private void configurarSuscripcionesObserver() {
        LoggerCentral.debug(TAG, "Configurando suscripciones de eventos Observer...");

        // Callback cuando se actualiza la lista completa de peers
        controladorPuro.suscribirActualizacionLista(peers -> {
            LoggerCentral.debug(TAG, "Evento LISTA_PEERS recibido: " + peers.size() + " peers en total");
        });

        // Callback cuando se conecta un nuevo peer
        controladorPuro.suscribirConexionPeer(peer -> {
            LoggerCentral.info(TAG, VERDE + "✓ Evento PEER_CONECTADO: " + peer.getIp() + ":" + peer.getPuerto() + RESET);
            System.out.println(VERDE + "\n🟢 [Evento] Peer CONECTADO: " + peer.getIp() + ":" + peer.getPuerto() + RESET);
        });

        // Callback cuando se desconecta un peer
        controladorPuro.suscribirDesconexionPeer(peer -> {
            LoggerCentral.warn(TAG, ROJO + "✗ Evento PEER_DESCONECTADO: " + peer.getIp() + ":" + peer.getPuerto() + RESET);
            System.out.println(ROJO + "\n🔴 [Evento] Peer DESCONECTADO: " + peer.getIp() + ":" + peer.getPuerto() + RESET);
        });

        LoggerCentral.info(TAG, "✅ Observadores configurados correctamente.");
    }

    public void procesarComando(String lineaCompleta) {
        if (lineaCompleta == null || lineaCompleta.trim().isEmpty()) return;

        String[] partes = lineaCompleta.trim().split("\\s+");
        String comando = partes[0].toUpperCase();
        String argumentos = partes.length > 1 ? String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)) : "";

        LoggerCentral.debug(TAG, "Procesando comando: [" + comando + "]" +
            (argumentos.isEmpty() ? "" : " con argumentos: [" + argumentos + "]"));

        try {
            switch (comando) {
                case "START":
                case "INICIAR":
                    LoggerCentral.info(TAG, "Ejecutando comando START...");
                    printInfo("Iniciando secuencia de arranque...");
                    controladorPuro.iniciarRed();

                    // NUEVO: Suscribirse a eventos de conexión DESPUÉS de iniciar la red
                    LoggerCentral.debug(TAG, "Registrando observador de conexiones en GestorConexiones...");
                    controladorPuro.suscribirseAEventosConexion();

                    printSuccess("Comando de inicio enviado.");
                    LoggerCentral.info(TAG, "Comando START ejecutado exitosamente.");
                    break;

                // NUEVO COMANDO SYNC
                case "SYNC":
                case "SINCRONIZAR":
                    LoggerCentral.info(TAG, "Ejecutando comando SYNC...");
                    if (!controladorPuro.isRedIniciada()) {
                        LoggerCentral.error(TAG, "Intento de sincronización con red no iniciada.");
                        printError("La red no está iniciada.");
                    } else {
                        printInfo("Forzando sincronización de datos (Merkle Tree)...");
                        controladorPuro.sincronizarManual();
                        printSuccess("Solicitud de sincronización enviada a la red.");
                        LoggerCentral.info(TAG, "Comando SYNC ejecutado exitosamente.");
                    }
                    break;

                case "CHAT":
                    if (argumentos.isEmpty()) {
                        LoggerCentral.warn(TAG, "Comando CHAT sin argumentos.");
                        printError("Falta mensaje. Uso: CHAT <msg>");
                    } else {
                        LoggerCentral.info(TAG, "Enviando mensaje de chat: [" + argumentos + "]");
                        controladorPuro.enviarMensajeChat(argumentos);
                    }
                    break;

                case "PRIVADO":
                    if (partes.length < 3) {
                        LoggerCentral.warn(TAG, "Comando PRIVADO con argumentos insuficientes.");
                        printError("Uso: PRIVADO <id> <mensaje>");
                    } else {
                        String id = partes[1];
                        String msg = String.join(" ", Arrays.copyOfRange(partes, 2, partes.length));
                        LoggerCentral.info(TAG, "Enviando mensaje privado a [" + id + "]: [" + msg + "]");
                        controladorPuro.enviarMensajePrivado(id, msg);
                        printInfo("Mensaje privado enviado.");
                    }
                    break;

                case "LIST":
                case "LISTA":
                    LoggerCentral.debug(TAG, "Obteniendo lista de peers...");
                    List<DTOPeerDetails> peers = controladorPuro.obtenerListaPeers();
                    LoggerCentral.info(TAG, "Lista de peers obtenida: " + peers.size() + " peers");

                    System.out.println(MAGENTA + "\n=== PEERS CONOCIDOS (" + peers.size() + ") ===" + RESET);
                    if (peers.isEmpty()) {
                        System.out.println(GRIS + " (Base de datos vacía)" + RESET);
                        LoggerCentral.debug(TAG, "No hay peers en la base de datos.");
                    } else {
                        for (DTOPeerDetails p : peers) {
                            int puertoReal = p.getPuertoServidor() > 0 ? p.getPuertoServidor() : p.getPuerto();
                            boolean isOnline = "ONLINE".equalsIgnoreCase(p.getEstado());
                            String color = isOnline ? VERDE : ROJO;
                            String icono = isOnline ? "●" : "○";
                            System.out.printf(" %s%s%s %s%-36s%s | %s%s:%d%s\n",
                                    color, icono, RESET, BLANCO, p.getId(), RESET, CYAN, p.getIp(), puertoReal, RESET);

                            LoggerCentral.debug(TAG, String.format("  - Peer %s | %s:%d | Estado: %s",
                                p.getId(), p.getIp(), puertoReal, p.getEstado()));
                        }
                    }
                    System.out.println(MAGENTA + "================================" + RESET);
                    break;

                case "CONNECT":
                    if (partes.length < 3) {
                        LoggerCentral.warn(TAG, "Comando CONNECT con argumentos insuficientes.");
                        printError("Uso: CONNECT <ip> <puerto>");
                    } else {
                        String ip = partes[1];
                        int puerto = Integer.parseInt(partes[2]);
                        LoggerCentral.info(TAG, "Conectando manualmente a " + ip + ":" + puerto);
                        controladorPuro.conectarManual(ip, puerto);
                        printInfo("Conectando...");
                    }
                    break;

                case "EXIT":
                    LoggerCentral.info(TAG, "Ejecutando comando EXIT...");
                    printInfo("Cerrando...");
                    controladorPuro.detenerRed();
                    LoggerCentral.info(TAG, "Sistema detenido. Saliendo...");
                    System.exit(0);
                    break;

                case "HELP":
                    LoggerCentral.debug(TAG, "Mostrando ayuda de comandos.");
                    mostrarAyuda();
                    break;

                default:
                    LoggerCentral.warn(TAG, "Comando desconocido: [" + comando + "]");
                    printError("Comando desconocido. Usa HELP.");
            }
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error procesando comando [" + comando + "]: " + e.getMessage());
            e.printStackTrace();
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