package controlador.conexion;

import dto.gestionConexion.DTOEstadoConexion;
import observador.IObservador;
import servicio.conexion.IServicioConexion;
import servicio.conexion.ServicioConexion;
import servicio.negocio.IServicioNegocio;
import servicio.negocio.ServicioNegocioImpl;
import conexion.GestorConexion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del controlador que gestiona la conexión y notifica cambios de estado.
 * Ahora implementado como singleton para asegurar una única instancia compartida
 * entre la UI y la lógica de conexión.
 */
public class ControladorConexion implements IControladorConexion, IObservador {

    private static ControladorConexion instancia;

    private final IServicioConexion servicioConexion;
    private final IServicioNegocio servicioNegocio;
    private final List<IObservador> observadores;
    private DTOEstadoConexion estadoActual;

    // Constructor privado para singleton
    private ControladorConexion() {
        this.servicioConexion = new ServicioConexion();
        this.servicioNegocio = new ServicioNegocioImpl();
        this.observadores = new ArrayList<>();
        this.estadoActual = new DTOEstadoConexion(false, "Desconocido", 0, "Desconectado");
        System.out.println("✅ [ControladorConexion]: Controlador inicializado (Thread:" + Thread.currentThread().getName() + ")");

        // Registrarse como observador del Gestor de conexión para recibir eventos de sesión
        try {
            GestorConexion.getInstancia().registrarObservador(this);
            System.out.println("✅ [ControladorConexion]: Registrado en GestorConexion como observador");
        } catch (Exception e) {
            System.err.println("⚠️ [ControladorConexion]: No se pudo registrar en GestorConexion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método de acceso al singleton (lazy, sincronizado)
    public static synchronized ControladorConexion getInstancia() {
        if (instancia == null) {
            instancia = new ControladorConexion();
        }
        return instancia;
    }

    @Override
    public CompletableFuture<Boolean> conectar() {
        System.out.println("📡 [ControladorConexion]: Iniciando proceso de conexión... (Thread:" + Thread.currentThread().getName() + ")");
        actualizarEstado(false, "Conectando...", 0, "Conectando...");

        return servicioConexion.conectar()
                .thenApply(conectado -> {
                    System.out.println("📡 [ControladorConexion]: Resultado servicioConexion.conectar() -> " + conectado);
                    if (conectado) {
                        System.out.println("✅ [ControladorConexion]: Conexión exitosa. Inicializando negocio...");
                        servicioNegocio.inicializar();
                        // Usar la información real de la sesión si está disponible en el GestorConexion
                        String servidor = "Desconocido";
                        try {
                            var sesion = GestorConexion.getInstancia().getSesion();
                            if (sesion != null && sesion.getSocket() != null && sesion.getSocket().getInetAddress() != null) {
                                servidor = sesion.getSocket().getInetAddress().getHostAddress();
                            }
                        } catch (Exception ignored) {}
                        // Ping placeholder (mejorar midiendo latencia si se necesita)
                        actualizarEstado(true, servidor, 45, "Conectado exitosamente");
                    } else {
                        System.err.println("❌ [ControladorConexion]: Error en la conexión (servicio devolvió false)");
                        actualizarEstado(false, "Sin servidor", 0, "Error al conectar");
                    }
                    return conectado;
                })
                .exceptionally(ex -> {
                    System.err.println("❌ [ControladorConexion]: Excepción durante conexión: " + ex.getMessage());
                    ex.printStackTrace();
                    actualizarEstado(false, "Sin servidor", 0, "Error: " + ex.getMessage());
                    return false;
                });
    }

    @Override
    public void solicitarActualizacionEstado() {
        System.out.println("📡 [ControladorConexion]: Solicitando actualización de estado (re-emitiendo estadoActual)");
        notificarObservadores("ACTUALIZAR_ESTADO_CONEXION", estadoActual);
    }

    private void actualizarEstado(boolean conectado, String servidor, int ping, String mensaje) {
        this.estadoActual = new DTOEstadoConexion(conectado, servidor, ping, mensaje);
        System.out.println("📢 [ControladorConexion]: Estado actualizado - " + mensaje + " | conectado=" + conectado + " | servidor=" + servidor + " | ping=" + ping);
        notificarObservadores("ACTUALIZAR_ESTADO_CONEXION", estadoActual);
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("✅ [ControladorConexion]: Observador registrado. Total: " + observadores.size());
            // Enviar estado actual inmediatamente al nuevo observador para sincronizar la UI
            try {
                observador.actualizar("ACTUALIZAR_ESTADO_CONEXION", this.estadoActual);
                System.out.println("📢 [ControladorConexion]: Estado actual enviado al nuevo observador");
            } catch (Exception ex) {
                System.err.println("[ControladorConexion]: Error enviando estado inicial al observador: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            System.out.println("⚠️ [ControladorConexion]: Intento de registrar observador ya existente. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🗑️ [ControladorConexion]: Observador removido. Total: " + observadores.size());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📢 [ControladorConexion]: Notificando a " + observadores.size() + " observadores. Tipo: " + tipoDeDato + " | Datos: " + (datos != null ? datos.getClass().getSimpleName() : "null"));
        for (IObservador observador : observadores) {
            try {
                observador.actualizar(tipoDeDato, datos);
            } catch (Exception ex) {
                System.err.println("[ControladorConexion] Error notificando observador: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // --- Implementación de IObservador: recibe eventos del GestorConexion ---
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [ControladorConexion]: Notificación recibida desde GestorConexion. Tipo: " + tipoDeDato + " | Datos clase: " + (datos != null ? datos.getClass().getSimpleName() : "null"));
        if ("ACTUALIZAR_ESTADO_CONEXION".equals(tipoDeDato) && datos instanceof DTOEstadoConexion) {
            DTOEstadoConexion estado = (DTOEstadoConexion) datos;
            System.out.println("🔔 [ControladorConexion]: Procesando DTOEstadoConexion -> conectado=" + estado.isConectado() + ", servidor=" + estado.getServidor() + ", ping=" + estado.getPing() + ", mensaje='" + estado.getMensaje() + "'");
            // Re-broadcast a los observadores de la UI
            this.estadoActual = estado;
            notificarObservadores(tipoDeDato, estado);
        }
    }
}
