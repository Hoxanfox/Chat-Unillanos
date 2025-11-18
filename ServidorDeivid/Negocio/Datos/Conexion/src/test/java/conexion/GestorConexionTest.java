package conexion;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.DTOEstadoConexion;
import dto.gestionConexion.transporte.DTOConexion;
import observador.IObservador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import conexion.fabrica.ITransporteFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GestorConexionTest {

    @BeforeEach
    void setup() {
        GestorConexion.getInstancia().cerrarTodo();
    }

    @AfterEach
    void tearDown() {
        GestorConexion.getInstancia().cerrarTodo();
    }

    static class TestObserver implements IObservador {
        static class Pair { final String tipo; final Object datos; Pair(String t, Object d){ tipo = t; datos = d; } }
        private final List<Pair> events = new ArrayList<>();
        @Override
        public void actualizar(String tipoDeDato, Object datos) {
            synchronized (events) {
                events.add(new Pair(tipoDeDato, datos));
            }
        }
        List<Pair> getEvents() { synchronized (events) { return new ArrayList<>(events); } }
    }

    // Util: obtener puerto libre
    private int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) { return s.getLocalPort(); }
    }

    @Test
    void testNotificacionSetSesionConNull() {
        TestObserver obs = new TestObserver();
        GestorConexion g = GestorConexion.getInstancia();
        g.registrarObservador(obs);

        g.setSesion(null);

        List<TestObserver.Pair> ev = obs.getEvents();
        assertFalse(ev.isEmpty(), "El observador debe recibir al menos una notificación");
        TestObserver.Pair p = ev.get(0);
        assertEquals("ACTUALIZAR_ESTADO_CONEXION", p.tipo);
        assertInstanceOf(DTOEstadoConexion.class, p.datos);
        DTOEstadoConexion estado = (DTOEstadoConexion) p.datos;
        assertFalse(estado.isConectado());

        g.removerObservador(obs);
    }

    @Test
    void testPoolAgregarObtenerLiberarCliente() throws Exception {
        TestObserver obs = new TestObserver();
        GestorConexion g = GestorConexion.getInstancia();
        g.registrarObservador(obs);

        Socket socket = new Socket(); // socket no conectado, pero no cerrado
        DTOSesion sesion = new DTOSesion(socket, null, null);

        g.agregarSesionCliente(sesion);

        boolean sawPoolUpdate = obs.getEvents().stream().anyMatch(e -> "POOL_CLIENTES_ACTUALIZADO".equals(e.tipo));
        assertTrue(sawPoolUpdate, "Se debe notificar actualización de pool al agregar sesión");

        DTOSesion obtained = g.obtenerSesionCliente(1000);
        assertNotNull(obtained, "Debe obtenerse la sesión añadida");
        assertSame(sesion, obtained);

        g.liberarSesionCliente(obtained);

        long updates = obs.getEvents().stream().filter(e -> "POOL_CLIENTES_ACTUALIZADO".equals(e.tipo)).count();
        assertTrue(updates >= 1, "Debe haberse notificado al menos una vez la actualización del pool");

        g.removerObservador(obs);
        socket.close();
    }

    @Test
    void testObtenerSesionPorDireccion() throws Exception {
        GestorConexion g = GestorConexion.getInstancia();

        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();

            final Socket[] accepted = new Socket[1];
            Thread t = new Thread(() -> {
                try { accepted[0] = server.accept(); } catch (Exception e) { /* ignore */ }
            });
            t.start();

            Socket client = new Socket("127.0.0.1", port);

            t.join(1000);

            DTOSesion sesion = new DTOSesion(client, null, null);
            g.agregarSesionPeer(sesion);

            DTOSesion found = g.obtenerSesionPorDireccion("127.0.0.1", port, 1000, true);
            assertNotNull(found, "Debe encontrarse la sesión por dirección");
            assertSame(sesion, found);

            client.close();
            if (accepted[0] != null) accepted[0].close();
        }
    }

    @Test
    void testObtenerSesionClienteTimeout() {
        GestorConexion g = GestorConexion.getInstancia();
        // Asegurarse pool vacío
        DTOSesion s = g.obtenerSesionCliente(200);
        assertNull(s, "Debe ser null cuando no hay sesiones y se agota el timeout");
    }

    @Test
    void testCerrarTodoVacíaNotifica() {
        TestObserver obs = new TestObserver();
        GestorConexion g = GestorConexion.getInstancia();
        g.registrarObservador(obs);

        // Asegurarse pools vacíos y luego cerrarTodo
        g.cerrarTodo();

        // Debe haberse notificado pools actualizados; la notificación de estado es opcional
        List<TestObserver.Pair> ev = obs.getEvents();
        boolean sawEstado = ev.stream().anyMatch(e -> "ACTUALIZAR_ESTADO_CONEXION".equals(e.tipo));
        boolean sawPoolClientes = ev.stream().anyMatch(e -> "POOL_CLIENTES_ACTUALIZADO".equals(e.tipo));
        boolean sawPoolPeers = ev.stream().anyMatch(e -> "POOL_PEERS_ACTUALIZADO".equals(e.tipo));

        // Pools deben notificarse siempre
        assertTrue(sawPoolClientes, "Debe notificar vaciado de pool clientes");
        assertTrue(sawPoolPeers, "Debe notificar vaciado de pool peers");
        // Estado: puede o no aparecer si no había sesión activa
        // Si quieres forzar siempre la notificación de estado, podemos cambiar la implementación de GestorConexion.

        g.removerObservador(obs);
    }

    @Test
    void testIniciarEscuchaClientesYObtenerSesion() throws Exception {
        GestorConexion g = GestorConexion.getInstancia();
        int port = freePort();
        g.iniciarEscuchaClientes("127.0.0.1", port);

        Socket client = new Socket("127.0.0.1", port);

        DTOSesion s = g.obtenerSesionCliente(2000);
        assertNotNull(s, "El listener debería aceptar la conexión y añadirla al pool de clientes");

        // limpiezas
        if (s.getSocket() != null && !s.getSocket().isClosed()) s.getSocket().close();
        client.close();
        g.detenerEscuchaClientes();
    }

    @Test
    void testIniciarEscuchaPeersYObtenerSesionPeer() throws Exception {
        GestorConexion g = GestorConexion.getInstancia();
        int port = freePort();
        g.iniciarEscuchaPeers("127.0.0.1", port);

        Socket client = new Socket("127.0.0.1", port);

        DTOSesion s = g.obtenerSesionPeer(2000);
        assertNotNull(s, "El listener debería aceptar la conexión y añadirla al pool de peers");

        if (s.getSocket() != null && !s.getSocket().isClosed()) s.getSocket().close();
        client.close();
        g.detenerEscuchaPeers();
    }

    @Test
    void testInicializarServidorArrancaAmbos() throws Exception {
        GestorConexion g = GestorConexion.getInstancia();
        int pc = freePort();
        int pp = freePort();
        g.inicializarServidor("127.0.0.1", pc, pp);

        Socket c1 = new Socket("127.0.0.1", pc);
        Socket c2 = new Socket("127.0.0.1", pp);

        DTOSesion s1 = g.obtenerSesionCliente(2000);
        DTOSesion s2 = g.obtenerSesionPeer(2000);
        assertNotNull(s1);
        assertNotNull(s2);

        if (s1.getSocket() != null && !s1.getSocket().isClosed()) s1.getSocket().close();
        if (s2.getSocket() != null && !s2.getSocket().isClosed()) s2.getSocket().close();
        c1.close(); c2.close();
        g.detenerEscuchaClientes(); g.detenerEscuchaPeers();
    }

    @Test
    void testInyectarFabricaYConectarComoCliente() throws Exception {
        GestorConexion g = GestorConexion.getInstancia();

        // Factory mock que devuelve una sesión con socket no conectado (suficiente para este test)
        ITransporteFactory factory = new ITransporteFactory() {
            @Override
            public transporte.ITransporte crearTransporte(String tipo) {
                return new transporte.ITransporte() {
                    @Override
                    public DTOSesion conectar(DTOConexion datosConexion) {
                        try {
                            Socket s = new Socket();
                            return new DTOSesion(s, null, null);
                        } catch (Exception e) { return null; }
                    }
                };
            }
        };

        g.setTransporteFactory(factory);

        DTOSesion ses = g.conectarComoCliente("no-importa", 12345, false);
        assertNotNull(ses, "La fábrica inyectada debe permitir crear una DTOSesion simulada");
        // verificar que el pool de clientes contiene la sesión (size >=1)
        assertTrue(g.getPoolClientesSize() >= 1);

        // limpieza
        g.liberarSesionCliente(ses);
        if (ses.getSocket() != null && !ses.getSocket().isClosed()) ses.getSocket().close();
    }

    @Test
    void testInicializarComoClienteGuardaSesionActiva() throws Exception {
        GestorConexion g = GestorConexion.getInstancia();

        ITransporteFactory factory = new ITransporteFactory() {
            @Override
            public transporte.ITransporte crearTransporte(String tipo) {
                return new transporte.ITransporte() {
                    @Override
                    public DTOSesion conectar(DTOConexion datosConexion) {
                        try { return new DTOSesion(new Socket(), null, null); } catch (Exception e) { return null; }
                    }
                };
            }
        };
        g.setTransporteFactory(factory);

        DTOSesion s = g.inicializarComoCliente("x", 1, false);
        assertNotNull(s);
        assertNotNull(g.getSesion(), "La llamada a inicializarComoCliente debe guardar la sesión activa");

        // limpieza
        g.cerrarSesion();
        if (s.getSocket() != null && !s.getSocket().isClosed()) s.getSocket().close();
    }
}
