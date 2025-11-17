package conexion;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.DTOEstadoConexion;
import observador.IObservador;
import observador.ISujeto;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Gestor Singleton que almacena y gestiona el ciclo de vida de la DTOSesion activa.
 * Actúa como un "Sujeto" observable para notificar cambios de sesión y además
 * mantiene pools sencillos para clientes y peers.
 */
public class GestorConexion implements ISujeto {

    private static GestorConexion instancia;

    // Sesión individual de referencia (legacy / conveniencia)
    private DTOSesion sesionActiva;

    // Pools reemplazados por BlockingQueue para manejo seguro de concurrencia
    private final BlockingQueue<DTOSesion> poolClientes = new LinkedBlockingQueue<>();
    private final BlockingQueue<DTOSesion> poolPeers = new LinkedBlockingQueue<>();

    private final List<IObservador> observadores = new ArrayList<>();

    private GestorConexion() {}

    public static synchronized GestorConexion getInstancia() {
        if (instancia == null) {
            instancia = new GestorConexion();
        }
        return instancia;
    }

    /**
     * Establece la sesión activa (legacy) y notifica a observadores.
     */
    public synchronized void setSesion(DTOSesion sesion) {
        if (this.sesionActiva != null && this.sesionActiva.estaActiva()) {
            cerrarSesion();
        }
        this.sesionActiva = sesion;

        // Notificar a observadores el nuevo estado de la conexión
        DTOEstadoConexion estado = buildEstadoDesdeSesion(sesion);
        notificarObservadores("ACTUALIZAR_ESTADO_CONEXION", estado);
    }

    /**
     * Devuelve la sesión de conexión activa (legacy).
     * @return la DTOSesion activa, o null si no hay ninguna.
     */
    public synchronized DTOSesion getSesion() {
        return sesionActiva;
    }

    /**
     * Cierra la sesión activa y todos sus recursos asociados de forma segura.
     */
    public synchronized void cerrarSesion() {
        if (sesionActiva != null && sesionActiva.estaActiva()) {
            try {
                if (sesionActiva.getIn() != null) sesionActiva.getIn().close();
                if (sesionActiva.getOut() != null) sesionActiva.getOut().close();
                if (sesionActiva.getSocket() != null) sesionActiva.getSocket().close();
            } catch (IOException e) {
                System.err.println("Error al cerrar los recursos de la sesión: " + e.getMessage());
            } finally {
                this.sesionActiva = null;

                // Notificar a observadores que la sesión se ha cerrado
                DTOEstadoConexion estado = new DTOEstadoConexion(false, "Desconocido", 0, "Desconectado");
                notificarObservadores("ACTUALIZAR_ESTADO_CONEXION", estado);
            }
        }
    }

    private DTOEstadoConexion buildEstadoDesdeSesion(DTOSesion sesion) {
        if (sesion == null || !sesion.estaActiva()) {
            return new DTOEstadoConexion(false, "Desconocido", 0, "Desconectado");
        }
        String servidor = "Desconocido";
        try {
            if (sesion.getSocket() != null && sesion.getSocket().getInetAddress() != null) {
                servidor = sesion.getSocket().getInetAddress().getHostAddress();
            }
        } catch (Exception ignored) {}
        // Ping no disponible desde aquí; se mantiene en 0.
        return new DTOEstadoConexion(true, servidor, 0, "Conectado");
    }

    // --- Pool management (ahora con BlockingQueue) ---

    /**
     * Añade una sesión al pool de clientes (si está activa) evitando duplicados.
     */
    public void agregarSesionCliente(DTOSesion sesion) {
        if (sesion != null && sesion.estaActiva()) {
            // Evitar añadir duplicados
            if (!poolClientes.contains(sesion)) {
                boolean offered = poolClientes.offer(sesion);
                if (!offered) System.err.println("No se pudo añadir la sesión al poolClientes");
                notificarObservadores("POOL_CLIENTES_ACTUALIZADO", poolClientes.size());
            }
        }
    }

    /**
     * Añade una sesión al pool de peers (si está activa) evitando duplicados.
     */
    public void agregarSesionPeer(DTOSesion sesion) {
        if (sesion != null && sesion.estaActiva()) {
            if (!poolPeers.contains(sesion)) {
                boolean offered = poolPeers.offer(sesion);
                if (!offered) System.err.println("No se pudo añadir la sesión al poolPeers");
                notificarObservadores("POOL_PEERS_ACTUALIZADO", poolPeers.size());
            }
        }
    }

    /**
     * Obtiene (y elimina del pool) una sesión de clientes. Espera hasta timeoutMs si el pool está vacío.
     * NO se utiliza fallback a `sesionActiva` para evitar compartir la misma instancia entre hilos.
     */
    public DTOSesion obtenerSesionCliente(long timeoutMs) {
        long start = System.currentTimeMillis();
        long remaining = Math.max(0, timeoutMs);
        try {
            while (remaining >= 0) {
                DTOSesion s = poolClientes.poll(remaining, TimeUnit.MILLISECONDS);
                if (s == null) return null; // timeout
                if (s.estaActiva()) return s;
                // si no está activa, seguir intentando con el tiempo restante
                remaining = timeoutMs - (System.currentTimeMillis() - start);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return null;
    }

    /**
     * Obtiene (y elimina del pool) una sesión de peers. Espera hasta timeoutMs si el pool está vacío.
     * NO se utiliza fallback a `sesionActiva` para evitar compartir la misma instancia entre hilos.
     */
    public DTOSesion obtenerSesionPeer(long timeoutMs) {
        long start = System.currentTimeMillis();
        long remaining = Math.max(0, timeoutMs);
        try {
            while (remaining >= 0) {
                DTOSesion s = poolPeers.poll(remaining, TimeUnit.MILLISECONDS);
                if (s == null) return null; // timeout
                if (s.estaActiva()) return s;
                remaining = timeoutMs - (System.currentTimeMillis() - start);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return null;
    }

    /**
     * Busca y extrae (temporariamente) una sesión que coincida con la IP y puerto indicados.
     * Si se encuentra, devuelve la sesión y las demás sesiones extraídas durante la búsqueda
     * se reinsertan en el pool. Respeta timeoutMs.
     * @param ip IP remota a buscar (host address)
     * @param port puerto remoto a buscar
     * @param timeoutMs tiempo máximo a esperar
     * @param buscarEnPeers true para poolPeers, false para poolClientes
     * @return DTOSesion encontrada o null si no se encontró en el timeout
     */
    public DTOSesion obtenerSesionPorDireccion(String ip, int port, long timeoutMs, boolean buscarEnPeers) {
        BlockingQueue<DTOSesion> queue = buscarEnPeers ? poolPeers : poolClientes;
        List<DTOSesion> temp = new ArrayList<>();
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMs);
        DTOSesion encontrada = null;

        try {
            while (System.currentTimeMillis() <= deadline) {
                long remaining = Math.max(0, deadline - System.currentTimeMillis());
                DTOSesion s = queue.poll(remaining, TimeUnit.MILLISECONDS);
                if (s == null) break; // timeout

                // Si la sesión no está activa, se cierra y se ignora
                if (!s.estaActiva()) {
                    try {
                        if (s.getIn() != null) s.getIn().close();
                        if (s.getOut() != null) s.getOut().close();
                        if (s.getSocket() != null) s.getSocket().close();
                    } catch (IOException ignored) {}
                    continue;
                }

                try {
                    if (s.getSocket() != null && s.getSocket().getInetAddress() != null) {
                        InetAddress addr = s.getSocket().getInetAddress();
                        int remotePort = s.getSocket().getPort();
                        if (addr.getHostAddress().equals(ip) && remotePort == port) {
                            encontrada = s;
                            break; // encontrada, no reinsertar todavía
                        }
                    }
                } catch (Exception ignored) {
                    // en caso de fallo al inspeccionar, tratar como no encontrada y seguir
                }

                // no es la sesión buscada, guardarla temporalmente para reinsertar
                temp.add(s);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Reinsertar sesiones temporales al pool
            for (DTOSesion sTemp : temp) {
                if (sTemp == null) continue; // proteger contra nulls
                if (sTemp.estaActiva()) {
                    // evitar duplicados
                    if (!queue.contains(sTemp)) {
                        boolean offered = queue.offer(sTemp);
                        if (!offered) System.err.println("No se pudo reinsertar sesión temporal en el pool");
                    }
                } else {
                    try {
                        if (sTemp.getIn() != null) sTemp.getIn().close();
                        if (sTemp.getOut() != null) sTemp.getOut().close();
                        if (sTemp.getSocket() != null) sTemp.getSocket().close();
                    } catch (IOException ignored) {}
                }
            }
        }

        return encontrada;
    }

    /**
     * Libera (vuelve a añadir) una sesión en el pool de clientes si sigue activa.
     */
    public void liberarSesionCliente(DTOSesion sesion) {
        if (sesion == null) return;
        if (sesion.estaActiva()) {
            if (!poolClientes.contains(sesion)) {
                boolean offered = poolClientes.offer(sesion);
                if (!offered) System.err.println("No se pudo añadir la sesión al poolClientes (liberar)");
                notificarObservadores("POOL_CLIENTES_ACTUALIZADO", poolClientes.size());
            }
        } else {
            // Intentar cerrar recursos si es necesario
            try {
                if (sesion.getIn() != null) sesion.getIn().close();
                if (sesion.getOut() != null) sesion.getOut().close();
                if (sesion.getSocket() != null) sesion.getSocket().close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Libera (vuelve a añadir) una sesión en el pool de peers si sigue activa.
     */
    public void liberarSesionPeer(DTOSesion sesion) {
        if (sesion == null) return;
        if (sesion.estaActiva()) {
            if (!poolPeers.contains(sesion)) {
                boolean offered = poolPeers.offer(sesion);
                if (!offered) System.err.println("No se pudo añadir la sesión al poolPeers (liberar)");
                notificarObservadores("POOL_PEERS_ACTUALIZADO", poolPeers.size());
            }
        } else {
            try {
                if (sesion.getIn() != null) sesion.getIn().close();
                if (sesion.getOut() != null) sesion.getOut().close();
                if (sesion.getSocket() != null) sesion.getSocket().close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Cierra y vacía todos los pools y la sesión activa.
     */
    public synchronized void cerrarTodo() {
        // Cerrar sesión activa
        cerrarSesion();

        // Vaciar y cerrar pools
        List<DTOSesion> listaClientes = new ArrayList<>();
        poolClientes.drainTo(listaClientes);
        for (DTOSesion s : listaClientes) {
            try {
                if (s.getIn() != null) s.getIn().close();
                if (s.getOut() != null) s.getOut().close();
                if (s.getSocket() != null) s.getSocket().close();
            } catch (IOException ignored) {}
        }
        notificarObservadores("POOL_CLIENTES_ACTUALIZADO", 0);

        List<DTOSesion> listaPeers = new ArrayList<>();
        poolPeers.drainTo(listaPeers);
        for (DTOSesion s : listaPeers) {
            try {
                if (s.getIn() != null) s.getIn().close();
                if (s.getOut() != null) s.getOut().close();
                if (s.getSocket() != null) s.getSocket().close();
            } catch (IOException ignored) {}
        }
        notificarObservadores("POOL_PEERS_ACTUALIZADO", 0);
    }

    // --- Implementación de ISujeto ---
    @Override
    public synchronized void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    @Override
    public synchronized void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }

    @Override
    public synchronized void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : new ArrayList<>(observadores)) {
            try {
                observador.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                System.err.println("Error notificando observador: " + e.getMessage());
            }
        }
    }
}
