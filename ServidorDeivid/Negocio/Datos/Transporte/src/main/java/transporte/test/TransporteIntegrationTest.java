package transporte.test;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;
import transporte.FabricaTransporte;
import transporte.TransporteServidor;
import transporte.TransporteTCP;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class TransporteIntegrationTest {

    public static void main(String[] args) throws Exception {
        final int puerto = 9100;
        System.out.println("[TEST] Iniciando TransporteServidor en 127.0.0.1:" + puerto);
        TransporteServidor servidor = new TransporteServidor();
        servidor.setSoTimeoutMs(500);

        servidor.iniciar("127.0.0.1", puerto, true, sesion -> {
            // handler: procesar la sesión en un hilo separado
            new Thread(() -> {
                System.out.println("[SERVER] Sesion aceptada: " + sesion);
                try {
                    // Intentar asignar lector
                    if (!sesion.intentarAsignarLector()) {
                        System.out.println("[SERVER] Sesion ya tiene lector, saliendo.");
                        return;
                    }
                    BufferedReader in = sesion.getIn();
                    PrintWriter out = sesion.getOut();
                    String linea = in.readLine();
                    System.out.println("[SERVER] Recibido: " + linea);
                    // responder
                    out.println("{\"action\":\"echo\",\"data\":\"Recibido: " + linea + "\"}");
                    out.flush();
                } catch (Exception e) {
                    System.err.println("[SERVER] Error procesando sesion: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try { sesion.liberarLector(); } catch (Exception ignored) {}
                    try { if (sesion.getIn() != null) sesion.getIn().close(); } catch (Exception ignored) {}
                    try { if (sesion.getOut() != null) sesion.getOut().close(); } catch (Exception ignored) {}
                    try { if (sesion.getSocket() != null) sesion.getSocket().close(); } catch (Exception ignored) {}
                    System.out.println("[SERVER] Sesion finalizada.");
                }
            }, "Server-Session-Handler").start();
        });

        // Esperar un momento a que el servidor arranque
        Thread.sleep(300);

        System.out.println("[TEST] Cliente intentando conectar usando TransporteTCP...");
        DTOConexion datos = new DTOConexion("127.0.0.1", puerto);
        DTOSesion cliente = FabricaTransporte.crearTransporte("TCP").conectar(datos);
        if (cliente == null || !cliente.estaActiva()) {
            System.err.println("[CLIENT] No se pudo crear la sesión cliente.");
            servidor.detener();
            return;
        }

        try {
            PrintWriter out = cliente.getOut();
            BufferedReader in = cliente.getIn();
            String mensaje = "{\"action\":\"test\",\"data\":\"hola servidor\"}";
            System.out.println("[CLIENT] Enviando: " + mensaje);
            out.println(mensaje);
            out.flush();

            String respuesta = in.readLine();
            System.out.println("[CLIENT] Recibido: " + respuesta);
        } catch (Exception e) {
            System.err.println("[CLIENT] Error en cliente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (cliente.getIn() != null) cliente.getIn().close(); } catch (Exception ignored) {}
            try { if (cliente.getOut() != null) cliente.getOut().close(); } catch (Exception ignored) {}
            try { if (cliente.getSocket() != null) cliente.getSocket().close(); } catch (Exception ignored) {}
        }

        // dar tiempo a que el servidor procese y luego detener
        Thread.sleep(300);
        servidor.detener();
        System.out.println("[TEST] Test finalizado.");
    }
}
