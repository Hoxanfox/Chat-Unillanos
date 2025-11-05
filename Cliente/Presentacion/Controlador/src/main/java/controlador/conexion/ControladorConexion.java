package controlador.conexion;

import dto.gestionConexion.DTOEstadoConexion;
import observador.IObservador;
import servicio.conexion.IServicioConexion;
import servicio.conexion.ServicioConexion;
import servicio.negocio.IServicioNegocio;
import servicio.negocio.ServicioNegocioImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del controlador que gestiona la conexión y notifica cambios de estado.
 */
public class ControladorConexion implements IControladorConexion {

    private final IServicioConexion servicioConexion;
    private final IServicioNegocio servicioNegocio;
    private final List<IObservador> observadores;
    private DTOEstadoConexion estadoActual;

    public ControladorConexion() {
        this.servicioConexion = new ServicioConexion();
        this.servicioNegocio = new ServicioNegocioImpl();
        this.observadores = new ArrayList<>();
        this.estadoActual = new DTOEstadoConexion(false, "Desconocido", 0, "Desconectado");
        System.out.println("✅ [ControladorConexion]: Controlador inicializado");
    }

    @Override
    public CompletableFuture<Boolean> conectar() {
        System.out.println("📡 [ControladorConexion]: Iniciando proceso de conexión...");
        actualizarEstado(false, "Conectando...", 0, "Conectando...");

        return servicioConexion.conectar()
                .thenApply(conectado -> {
                    if (conectado) {
                        System.out.println("✅ [ControladorConexion]: Conexión exitosa. Inicializando negocio...");
                        servicioNegocio.inicializar();
                        actualizarEstado(true, "chat.unillanos.com", 45, "Conectado exitosamente");
                    } else {
                        System.err.println("❌ [ControladorConexion]: Error en la conexión");
                        actualizarEstado(false, "Sin servidor", 0, "Error al conectar");
                    }
                    return conectado;
                })
                .exceptionally(ex -> {
                    System.err.println("❌ [ControladorConexion]: Excepción durante conexión: " + ex.getMessage());
                    actualizarEstado(false, "Sin servidor", 0, "Error: " + ex.getMessage());
                    return false;
                });
    }

    @Override
    public void solicitarActualizacionEstado() {
        System.out.println("📡 [ControladorConexion]: Solicitando actualización de estado");
        notificarObservadores("ACTUALIZAR_ESTADO_CONEXION", estadoActual);
    }

    private void actualizarEstado(boolean conectado, String servidor, int ping, String mensaje) {
        this.estadoActual = new DTOEstadoConexion(conectado, servidor, ping, mensaje);
        System.out.println("📢 [ControladorConexion]: Estado actualizado - " + mensaje);
        notificarObservadores("ACTUALIZAR_ESTADO_CONEXION", estadoActual);
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("✅ [ControladorConexion]: Observador registrado. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🗑️ [ControladorConexion]: Observador removido. Total: " + observadores.size());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📢 [ControladorConexion]: Notificando a " + observadores.size() + " observadores. Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
