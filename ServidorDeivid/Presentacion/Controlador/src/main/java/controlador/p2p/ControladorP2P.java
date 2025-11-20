package controlador.p2p;

import dto.p2p.DTOPeerDetails;
import servicio.p2p.ServicioP2P;
import servicio.p2p.IServicioP2PControl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ControladorP2P {

    // Usamos la Interfaz para desacoplar (Buenas prácticas)
    private final IServicioP2PControl servicio;

    /**
     * Constructor SIN parámetros (Cascada).
     * La Vista llama a este, y este crea el Servicio.
     */
    public ControladorP2P() {
        this.servicio = new ServicioP2P();
    }

    /**
     * Constructor CON parámetros (Inyección manual para Tests).
     */
    public ControladorP2P(IServicioP2PControl servicio) {
        this.servicio = servicio;
    }

    /**
     * Procesa la línea de texto que ingresa el usuario.
     * Separa el comando de los argumentos.
     * Ej: "CHAT Hola mundo" -> comando="CHAT", args=["Hola", "mundo"]
     */
    public void procesarComando(String lineaCompleta) {
        if (lineaCompleta == null || lineaCompleta.trim().isEmpty()) return;

        String[] partes = lineaCompleta.trim().split("\\s+");
        String comando = partes[0].toUpperCase();

        // Argumentos (todo lo que sigue al comando)
        String argumentos = partes.length > 1
                ? String.join(" ", Arrays.copyOfRange(partes, 1, partes.length))
                : "";

        try {
            switch (comando) {
                case "START":
                case "INICIAR":
                    System.out.println("[Controlador] Iniciando red P2P...");
                    servicio.iniciarRed();
                    break;

                case "CHAT":
                    if (argumentos.isEmpty()) {
                        System.out.println("[Controlador] Uso: CHAT <mensaje>");
                    } else {
                        // Por defecto usamos un nombre de usuario genérico o lo pedimos antes
                        servicio.enviarMensajeGlobal("UsuarioLocal", argumentos);
                    }
                    break;

                case "LIST":
                case "LISTA":
                    List<DTOPeerDetails> peers = servicio.obtenerListaPeers();
                    System.out.println("--- Peers Conectados (" + peers.size() + ") ---");
                    if (peers.isEmpty()) {
                        System.out.println("(Ninguno)");
                    } else {
                        peers.forEach(p ->
                                System.out.println(String.format("ID: %s | IP: %s:%d | Estado: %s",
                                        p.getId(), p.getIp(), p.getPuerto(), p.getEstado()))
                        );
                    }
                    System.out.println("--------------------------------");
                    break;

                case "CONNECT":
                    // Formato: CONNECT 192.168.1.50 9000
                    if (partes.length < 3) {
                        System.out.println("[Controlador] Uso: CONNECT <ip> <puerto>");
                    } else {
                        String ip = partes[1];
                        int puerto = Integer.parseInt(partes[2]);
                        servicio.conectarManual(ip, puerto);
                    }
                    break;

                case "EXIT":
                case "SALIR":
                    System.out.println("[Controlador] Deteniendo servicios...");
                    servicio.detenerRed();
                    System.out.println("[Controlador] Cerrando aplicación. ¡Adiós!");
                    System.exit(0);
                    break;

                case "HELP":
                case "AYUDA":
                    mostrarAyuda();
                    break;

                default:
                    System.out.println("[Controlador] Comando no reconocido. Escribe HELP.");
            }
        } catch (Exception e) {
            System.err.println("[Controlador] Error procesando comando: " + e.getMessage());
            e.printStackTrace(); // Útil para debug
        }
    }

    private void mostrarAyuda() {
        System.out.println("\n--- Comandos Disponibles ---");
        System.out.println(" START            -> Inicia el nodo y conecta a la red.");
        System.out.println(" LIST             -> Muestra los peers conectados.");
        System.out.println(" CHAT <msg>       -> Envía un mensaje público a todos.");
        System.out.println(" CONNECT <ip> <p> -> Conecta manualmente a otro nodo.");
        System.out.println(" EXIT             -> Apaga todo y sale.");
        System.out.println("----------------------------");
    }
}