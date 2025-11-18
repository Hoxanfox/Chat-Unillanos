package conexion;

import conexion.fabrica.ITransporteFactory;
import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.DTOEstadoConexion;
import dto.gestionConexion.transporte.DTOConexion;
import observador.IObservador;
import observador.ISujeto;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import logger.LoggerCentral;
import transporte.ITransporte;
import transporte.TransporteServidor;

/**
 * Gestor Singleton que almacena y gestiona el ciclo de vida de la DTOSesion activa.
 * Actúa como un "Sujeto" observable para notificar cambios de sesión y además
 * mantiene pools sencillos para clientes y peers.
 */
public class GestorConexion implements ISujeto, IGestorConexion {

    private static GestorConexion instancia;

    // Sesión individual de referencia (legacy / conveniencia)
    private DTOSesion sesionActiva;

    // Pools reemplazados por BlockingQueue para manejo seguro de concurrencia
    private final BlockingQueue<DTOSesion> poolClientes = new LinkedBlockingQueue<>();
    private final BlockingQueue<DTOSesion> poolPeers = new LinkedBlockingQueue<>();

    private final List<IObservador> observadores = new ArrayList<>();

    // Transport servers para escuchar conexiones entrantes (clientes y peers)
    private TransporteServidor servidorClientes;
    private TransporteServidor servidorPeers;

    // Fábrica inyectable para crear transportes (por defecto delega en FabricaTransporte)
    private ITransporteFactory transporteFactory = new ITransporteFactory() {
        @Override
        public transporte.ITransporte crearTransporte(String tipo) {
            return transporte.FabricaTransporte.crearTransporte(tipo);
        }
    };

    /**
     * Permite inyectar una fábrica de transportes (útil para mock en tests).
     */
    public synchronized void setTransporteFactory(ITransporteFactory factory) {
        if (factory != null) this.transporteFactory = factory;
    }

    /** Devuelve el tamaño actual del pool de clientes. */
    public int getPoolClientesSize() { return poolClientes.size(); }

    /** Devuelve el tamaño actual del pool de peers. */
    public int getPoolPeersSize() { return poolPeers.size(); }

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
                LoggerCentral.debug("agregarSesionCliente: session añadida -> " + sesion + ". poolClientes.size=" + poolClientes.size());
                notificarObservadores("POOL_CLIENTES_ACTUALIZADO", poolClientes.size());
            } else {
                LoggerCentral.debug("agregarSesionCliente: sesión ya existe en poolClientes -> " + sesion);
            }
        } else {
            LoggerCentral.debug("agregarSesionCliente: sesión nula o inactiva, no añadida -> " + sesion);
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
                LoggerCentral.debug("agregarSesionPeer: session añadida -> " + sesion + ". poolPeers.size=" + poolPeers.size());
                notificarObservadores("POOL_PEERS_ACTUALIZADO", poolPeers.size());
            } else {
                LoggerCentral.debug("agregarSesionPeer: sesión ya existe en poolPeers -> " + sesion);
            }
        } else {
            LoggerCentral.debug("agregarSesionPeer: sesión nula o inactiva, no añadida -> " + sesion);
        }
    }

    /**
     * Obtiene (y elimina del pool) una sesión de clientes. Espera hasta timeoutMs si el pool está vacío.
     * NO se utiliza fallback a `sesionActiva` para evitar compartir la misma instancia entre hilos.
     */
    public DTOSesion obtenerSesionCliente(long timeoutMs) {
        LoggerCentral.debug("obtenerSesionCliente: solicitada con timeoutMs=" + timeoutMs + ". poolClientes.size=" + poolClientes.size());
        long start = System.currentTimeMillis();
        long remaining = Math.max(0, timeoutMs);
        try {
            while (remaining >= 0) {
                DTOSesion s = poolClientes.poll(remaining, TimeUnit.MILLISECONDS);
                if (s == null) {
                    LoggerCentral.debug("obtenerSesionCliente: timeout sin sesión disponible (timeoutMs=" + timeoutMs + ")");
                    return null; // timeout
                }
                if (s.estaActiva()) {
                    LoggerCentral.debug("obtenerSesionCliente: sesión obtenida -> " + s);
                    return s;
                }
                // si no está activa, seguir intentando con el tiempo restante
                LoggerCentral.debug("obtenerSesionCliente: sesión obtenida no activa, descartando -> " + s);
                remaining = timeoutMs - (System.currentTimeMillis() - start);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LoggerCentral.debug("obtenerSesionCliente: hilo interrumpido durante poll");
            return null;
        }
        LoggerCentral.debug("obtenerSesionCliente: final sin resultado");
        return null;
    }

    /**
     * Obtiene (y elimina del pool) una sesión de peers. Espera hasta timeoutMs si el pool está vacío.
     * NO se utiliza fallback a `sesionActiva` para evitar compartir la misma instancia entre hilos.
     */
    public DTOSesion obtenerSesionPeer(long timeoutMs) {
        LoggerCentral.debug("obtenerSesionPeer: solicitada con timeoutMs=" + timeoutMs + ". poolPeers.size=" + poolPeers.size());
        long start = System.currentTimeMillis();
        long remaining = Math.max(0, timeoutMs);
        try {
            while (remaining >= 0) {
                DTOSesion s = poolPeers.poll(remaining, TimeUnit.MILLISECONDS);
                if (s == null) {
                    LoggerCentral.debug("obtenerSesionPeer: timeout sin sesión disponible (timeoutMs=" + timeoutMs + ")");
                    return null; // timeout
                }
                if (s.estaActiva()) {
                    LoggerCentral.debug("obtenerSesionPeer: sesión obtenida -> " + s);
                    return s;
                }
                LoggerCentral.debug("obtenerSesionPeer: sesión obtenida no activa, descartando -> " + s);
                remaining = timeoutMs - (System.currentTimeMillis() - start);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LoggerCentral.debug("obtenerSesionPeer: hilo interrumpido durante poll");
            return null;
        }
        LoggerCentral.debug("obtenerSesionPeer: final sin resultado");
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
        LoggerCentral.debug("obtenerSesionPorDireccion: buscando ip=" + ip + " port=" + port + " buscarEnPeers=" + buscarEnPeers + " timeoutMs=" + timeoutMs + " poolSize=" + queue.size());
        List<DTOSesion> temp = new ArrayList<>();
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMs);
        DTOSesion encontrada = null;

        try {
            while (System.currentTimeMillis() <= deadline) {
                long remaining = Math.max(0, deadline - System.currentTimeMillis());
                DTOSesion s = queue.poll(remaining, TimeUnit.MILLISECONDS);
                if (s == null) {
                    LoggerCentral.debug("obtenerSesionPorDireccion: timeout sin encontrar sesion para " + ip + ":" + port);
                    break; // timeout
                }

                // Si la sesión no está activa, se cierra y se ignora
                if (!s.estaActiva()) {
                    LoggerCentral.debug("obtenerSesionPorDireccion: sesión extraída no activa, cerrando -> " + s);
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
                            LoggerCentral.debug("obtenerSesionPorDireccion: sesión encontrada -> " + s);
                            break; // encontrada, no reinsertar todavía
                        }
                    }
                } catch (Exception ex) {
                    LoggerCentral.debug("obtenerSesionPorDireccion: error inspeccionando sesion -> " + ex.getMessage());
                    // en caso de fallo al inspeccionar, tratar como no encontrada y seguir
                }

                // no es la sesión buscada, guardarla temporalmente para reinsertar
                temp.add(s);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LoggerCentral.debug("obtenerSesionPorDireccion: hilo interrumpido durante búsqueda");
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
                    LoggerCentral.debug("obtenerSesionPorDireccion: reinsertada sesion temporal -> " + sTemp + ". poolSize ahora=" + queue.size());
                } else {
                    try {
                        if (sTemp.getIn() != null) sTemp.getIn().close();
                        if (sTemp.getOut() != null) sTemp.getOut().close();
                        if (sTemp.getSocket() != null) sTemp.getSocket().close();
                    } catch (IOException ignored) {}
                    LoggerCentral.debug("obtenerSesionPorDireccion: sesión temporal inactiva cerrada -> " + sTemp);
                }
            }
        }

        return encontrada;
    }

    /**
     * Busca sin extraer una sesión que coincida con la IP y puerto indicados.
     * No modifica el pool; devuelve la primera sesión activa encontrada o null.
     */
    public DTOSesion buscarSesionPorDireccionSinExtraer(String ip, int port, boolean buscarEnPeers) {
        BlockingQueue<DTOSesion> queue = buscarEnPeers ? poolPeers : poolClientes;
        LoggerCentral.debug("buscarSesionPorDireccionSinExtraer: buscando ip=" + ip + " port=" + port + " buscarEnPeers=" + buscarEnPeers + " poolSize=" + queue.size());
        try {
            for (DTOSesion s : queue) {
                if (s == null) continue;
                try {
                    if (!s.estaActiva()) continue;
                    if (s.getSocket() == null || s.getSocket().getInetAddress() == null) continue;
                    String host = s.getSocket().getInetAddress().getHostAddress();
                    int remotePort = s.getSocket().getPort();
                    if (host.equals(ip) && remotePort == port) {
                        LoggerCentral.debug("buscarSesionPorDireccionSinExtraer: sesión encontrada -> " + s);
                        return s;
                    }
                } catch (Exception ex) {
                    LoggerCentral.debug("buscarSesionPorDireccionSinExtraer: error inspeccionando sesion -> " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            LoggerCentral.debug("buscarSesionPorDireccionSinExtraer: excepción iterando pool -> " + e.getMessage());
        }
        LoggerCentral.debug("buscarSesionPorDireccionSinExtraer: no se encontró sesión para " + ip + ":" + port);
        return null;
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
                LoggerCentral.debug("liberarSesionCliente: sesion reinsertada -> " + sesion + ". poolClientes.size=" + poolClientes.size());
                notificarObservadores("POOL_CLIENTES_ACTUALIZADO", poolClientes.size());
            } else {
                LoggerCentral.debug("liberarSesionCliente: sesion ya existia en poolClientes -> " + sesion);
            }
        } else {
            // Intentar cerrar recursos si es necesario
            try {
                if (sesion.getIn() != null) sesion.getIn().close();
                if (sesion.getOut() != null) sesion.getOut().close();
                if (sesion.getSocket() != null) sesion.getSocket().close();
            } catch (IOException ignored) {}
            LoggerCentral.debug("liberarSesionCliente: sesion inactiva cerrada -> " + sesion);
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
                LoggerCentral.debug("liberarSesionPeer: sesion reinsertada -> " + sesion + ". poolPeers.size=" + poolPeers.size());
                notificarObservadores("POOL_PEERS_ACTUALIZADO", poolPeers.size());
            } else {
                LoggerCentral.debug("liberarSesionPeer: sesion ya existia en poolPeers -> " + sesion);
            }
        } else {
            try {
                if (sesion.getIn() != null) sesion.getIn().close();
                if (sesion.getOut() != null) sesion.getOut().close();
                if (sesion.getSocket() != null) sesion.getSocket().close();
            } catch (IOException ignored) {}
            LoggerCentral.debug("liberarSesionPeer: sesion inactiva cerrada -> " + sesion);
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

    /**
     * Inicia un listener para conexiones de clientes en la dirección/puerto especificados.
     * Las sesiones aceptadas se añadirán automáticamente al pool de clientes.
     */
    public synchronized void iniciarEscuchaClientes(String host, int puerto) throws IOException {
        if (servidorClientes != null && servidorClientes.estaArrancado()) {
            LoggerCentral.warn("iniciarEscuchaClientes: ya arrancado en puerto " + puerto);
            return;
        }
        servidorClientes = new TransporteServidor();
        servidorClientes.iniciar(host, puerto, false, sesion -> {
            LoggerCentral.info("iniciarEscuchaClientes: sesión entrante añadida al poolClientes -> " + sesion);
            agregarSesionCliente(sesion);
        });
    }

    /**
     * Inicia un listener para conexiones de peers en la dirección/puerto especificados.
     * Las sesiones aceptadas se añadirán automáticamente al pool de peers.
     */
    public synchronized void iniciarEscuchaPeers(String host, int puerto) throws IOException {
        if (servidorPeers != null && servidorPeers.estaArrancado()) {
            LoggerCentral.warn("iniciarEscuchaPeers: ya arrancado en puerto " + puerto);
            return;
        }
        servidorPeers = new TransporteServidor();
        servidorPeers.iniciar(host, puerto, true, sesion -> {
            LoggerCentral.info("iniciarEscuchaPeers: sesión entrante añadida al poolPeers -> " + sesion);
            agregarSesionPeer(sesion);
        });
    }

    /**
     * Detiene el listener de clientes si está arrancado.
     */
    public synchronized void detenerEscuchaClientes() {
        if (servidorClientes != null) {
            servidorClientes.detener();
            servidorClientes = null;
            LoggerCentral.info("detenerEscuchaClientes: detenido");
        }
    }

    /**
     * Detiene el listener de peers si está arrancado.
     */
    public synchronized void detenerEscuchaPeers() {
        if (servidorPeers != null) {
            servidorPeers.detener();
            servidorPeers = null;
            LoggerCentral.info("detenerEscuchaPeers: detenido");
        }
    }

    /**
     * Inicializa el gestor en modo "servidor" arrancando listeners de clientes y peers.
     * Conveniencia para configurar ambos puertos en una sola llamada.
     */
    public synchronized void inicializarServidor(String host, int puertoClientes, int puertoPeers) throws IOException {
        // arrancar listeners de manera segura
        iniciarEscuchaClientes(host, puertoClientes);
        iniciarEscuchaPeers(host, puertoPeers);
    }

    /**
     * Inicializa el gestor en modo "cliente": conecta a la dirección indicada,
     * guarda la sesión activa (setSesion) y añade la sesión al pool correspondiente.
     * Su única responsabilidad es conectar, guardar la sesión e inicializar el sistema.
     */
    public DTOSesion inicializarComoCliente(String host, int puerto, boolean comoPeer) {
        DTOSesion sesion = conectarComoCliente(host, puerto, comoPeer);
        if (sesion != null) {
            // Guardar como sesión activa (legacy) y notificar
            setSesion(sesion);
        }
        return sesion;
    }

    /**
     * Conecta como cliente a la dirección/puerto indicados usando la fábrica de transporte.
     * Si la conexión tiene éxito, la sesión resultante se añade al pool correspondiente.
     * @param host host remoto
     * @param puerto puerto remoto
     * @param comoPeer true si la sesión debe tratarse como peer, false si como cliente
     * @return la DTOSesion creada o null si falló la conexión
     */
    public DTOSesion conectarComoCliente(String host, int puerto, boolean comoPeer) {
        DTOConexion datos = new DTOConexion(host, puerto);
        return conectarComoCliente(datos, comoPeer);
    }

    /**
     * Conecta usando un DTOConexion y añade la sesión al pool adecuado en caso de éxito.
     */
    public DTOSesion conectarComoCliente(DTOConexion datosConexion, boolean comoPeer) {
        if (datosConexion == null) return null;
        ITransporte transporte = transporteFactory.crearTransporte("TCP");
        try {
            DTOSesion sesion = transporte.conectar(datosConexion);
            if (sesion != null && sesion.estaActiva()) {
                if (comoPeer) agregarSesionPeer(sesion); else agregarSesionCliente(sesion);
                LoggerCentral.info("conectarComoCliente: conexión exitosa y añadida a pool (comoPeer=" + comoPeer + ") -> " + sesion);
                return sesion;
            }
        } catch (Exception e) {
            LoggerCentral.error("conectarComoCliente: error al conectar -> " + e.getMessage(), e);
        }
        return null;
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

    /**
     * Método de diagnóstico: devuelve un resumen (lista) de las sesiones actualmente en el poolPeers
     * en formato "ip:port" separados por comas. Útil para logging/debug.
     */
    public String listarResumenPoolPeers() {
        try {
            List<DTOSesion> lista = new ArrayList<>(poolPeers);
            StringBuilder sb = new StringBuilder();
            for (DTOSesion s : lista) {
                try {
                    if (s != null && s.getSocket() != null && s.getSocket().getInetAddress() != null) {
                        sb.append(s.getSocket().getInetAddress().getHostAddress()).append(":").append(s.getSocket().getPort()).append(",");
                    } else {
                        sb.append(s != null ? s.hashCode() : "null").append(",");
                    }
                } catch (Exception ignored) {
                    sb.append(s != null ? s.hashCode() : "null").append(",");
                }
            }
            if (sb.length() > 0) sb.setLength(sb.length() - 1);
            return sb.toString();
        } catch (Exception e) {
            return "error_obteniendo_resumen_poolPeers: " + e.getMessage();
        }
    }
}
