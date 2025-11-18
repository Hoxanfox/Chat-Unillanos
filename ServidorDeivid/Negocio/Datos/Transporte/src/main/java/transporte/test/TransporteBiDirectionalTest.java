package transporte.test;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;
import transporte.FabricaTransporte;
import transporte.TransporteServidor;

import java.io.BufferedReader;
import java.io.PrintWriter;

/**
 * Prueba simple para demostrar que un nodo puede ser servidor y cliente a la vez.
 * Se arrancan dos servidores locales (A y B) en puertos distintos y cada uno actúa
 * como cliente hacia el otro, enviando un mensaje y esperando respuesta.
 */
public class TransporteBiDirectionalTest {

    public static void main(String[] args) throws Exception {
        final int portA = 9200;
        final int portB = 9201;

        System.out.println("[TEST] Iniciando dos servidores: A=" + portA + ", B=" + portB);

        TransporteServidor serverA = new TransporteServidor();
        TransporteServidor serverB = new TransporteServidor();
        serverA.setSoTimeoutMs(500);
        serverB.setSoTimeoutMs(500);

        // Handler para server A: recibe una línea, imprime y responde
        serverA.iniciar("127.0.0.1", portA, true, sesion -> {
            new Thread(() -> {
                System.out.println("[A-Server] Sesion aceptada: " + sesion);
                try {
                    if (!sesion.intentarAsignarLector()) {
                        System.out.println("[A-Server] Sesion ya tiene lector, saliendo.");
                        return;
                    }
                    BufferedReader in = sesion.getIn();
                    PrintWriter out = sesion.getOut();
                    String linea = in.readLine();
                    System.out.println("[A-Server] Recibido: " + linea);
                    out.println("A-OK: recibio -> " + linea);
                    out.flush();
                } catch (Exception e) {
                    System.err.println("[A-Server] Error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try { sesion.liberarLector(); } catch (Exception ignored) {}
                    try { if (sesion.getIn() != null) sesion.getIn().close(); } catch (Exception ignored) {}
                    try { if (sesion.getOut() != null) sesion.getOut().close(); } catch (Exception ignored) {}
                    try { if (sesion.getSocket() != null) sesion.getSocket().close(); } catch (Exception ignored) {}
                    System.out.println("[A-Server] Sesion finalizada.");
                }
            }, "A-Server-Handler").start();
        });

        // Handler para server B: recibe una línea, imprime y responde
        serverB.iniciar("127.0.0.1", portB, true, sesion -> {
            new Thread(() -> {
                System.out.println("[B-Server] Sesion aceptada: " + sesion);
                try {
                    if (!sesion.intentarAsignarLector()) {
                        System.out.println("[B-Server] Sesion ya tiene lector, saliendo.");
                        return;
                    }
                    BufferedReader in = sesion.getIn();
                    PrintWriter out = sesion.getOut();
                    String linea = in.readLine();
                    System.out.println("[B-Server] Recibido: " + linea);
                    out.println("B-OK: recibio -> " + linea);
                    out.flush();
                } catch (Exception e) {
                    System.err.println("[B-Server] Error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try { sesion.liberarLector(); } catch (Exception ignored) {}
                    try { if (sesion.getIn() != null) sesion.getIn().close(); } catch (Exception ignored) {}
                    try { if (sesion.getOut() != null) sesion.getOut().close(); } catch (Exception ignored) {}
                    try { if (sesion.getSocket() != null) sesion.getSocket().close(); } catch (Exception ignored) {}
                    System.out.println("[B-Server] Sesion finalizada.");
                }
            }, "B-Server-Handler").start();
        });

        // esperar a que ambos servidores estén en escucha
        Thread.sleep(300);

        System.out.println("[TEST] Nodo A actua como cliente hacia B");
        DTOConexion datosAB = new DTOConexion("127.0.0.1", portB);
        DTOSesion clientFromA = FabricaTransporte.crearTransporte("TCP").conectar(datosAB);
        if (clientFromA == null || !clientFromA.estaActiva()) {
            System.err.println("[TEST] Fallo: cliente A->B no pudo conectarse.");
        } else {
            try {
                PrintWriter out = clientFromA.getOut();
                BufferedReader in = clientFromA.getIn();
                String msg = "Hola B desde A";
                System.out.println("[A-Client] Enviando a B: " + msg);
                out.println(msg);
                out.flush();
                String resp = in.readLine();
                System.out.println("[A-Client] Recibio de B: " + resp);
            } finally {
                try { if (clientFromA.getIn() != null) clientFromA.getIn().close(); } catch (Exception ignored) {}
                try { if (clientFromA.getOut() != null) clientFromA.getOut().close(); } catch (Exception ignored) {}
                try { if (clientFromA.getSocket() != null) clientFromA.getSocket().close(); } catch (Exception ignored) {}
            }
        }

        System.out.println("[TEST] Nodo B actua como cliente hacia A");
        DTOConexion datosBA = new DTOConexion("127.0.0.1", portA);
        DTOSesion clientFromB = FabricaTransporte.crearTransporte("TCP").conectar(datosBA);
        if (clientFromB == null || !clientFromB.estaActiva()) {
            System.err.println("[TEST] Fallo: cliente B->A no pudo conectarse.");
        } else {
            try {
                PrintWriter out = clientFromB.getOut();
                BufferedReader in = clientFromB.getIn();
                String msg = "Hola A desde B";
                System.out.println("[B-Client] Enviando a A: " + msg);
                out.println(msg);
                out.flush();
                String resp = in.readLine();
                System.out.println("[B-Client] Recibio de A: " + resp);
            } finally {
                try { if (clientFromB.getIn() != null) clientFromB.getIn().close(); } catch (Exception ignored) {}
                try { if (clientFromB.getOut() != null) clientFromB.getOut().close(); } catch (Exception ignored) {}
                try { if (clientFromB.getSocket() != null) clientFromB.getSocket().close(); } catch (Exception ignored) {}
            }
        }

        // esperar y detener
        Thread.sleep(300);
        serverA.detener();
        serverB.detener();
        System.out.println("[TEST] Bi-directional test finalizado.");
    }
}

