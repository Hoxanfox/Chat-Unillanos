package servicio.notificaciones;

import fachada.Fachada;
import fachada.IFachada;
import observador.IObservador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de notificaciones.
 * Actúa como intermediario entre el Controlador y la Fachada.
 */
public class ServicioNotificacionesImpl implements IServicioNotificaciones {

    private final IFachada fachada;
    private final List<IObservador> observadores;

    public ServicioNotificacionesImpl() {
        this.fachada = Fachada.obtenerInstancia();
        this.observadores = new ArrayList<>();
        System.out.println("✅ [ServicioNotificaciones]: Creado e instanciado la Fachada.");
    }

    @Override
    public void solicitarActualizacionNotificaciones() {
        System.out.println("📡 [ServicioNotificaciones]: Solicitando lista de notificaciones a la Fachada...");
        
        fachada.obtenerNotificaciones()
            .thenAccept(notificaciones -> {
                System.out.println("✅ [ServicioNotificaciones]: Recibidas " + notificaciones.size() + " notificaciones");
                notificarObservadores("ACTUALIZAR_NOTIFICACIONES", notificaciones);
            })
            .exceptionally(ex -> {
                System.err.println("❌ [ServicioNotificaciones]: Error al obtener notificaciones: " + ex.getMessage());
                notificarObservadores("ERROR_NOTIFICACIONES", ex.getMessage());
                return null;
            });
    }

    @Override
    public CompletableFuture<Void> marcarComoLeida(String notificacionId) {
        System.out.println("📝 [ServicioNotificaciones]: Marcando notificación como leída: " + notificacionId);
        
        return fachada.marcarNotificacionLeida(notificacionId)
            .thenRun(() -> {
                System.out.println("✅ [ServicioNotificaciones]: Notificación marcada como leída");
                solicitarActualizacionNotificaciones(); // Actualizar la lista
            })
            .exceptionally(ex -> {
                System.err.println("❌ [ServicioNotificaciones]: Error al marcar como leída: " + ex.getMessage());
                return null;
            });
    }

    @Override
    public CompletableFuture<Void> marcarTodasComoLeidas() {
        System.out.println("📝 [ServicioNotificaciones]: Marcando todas las notificaciones como leídas");
        
        return fachada.marcarTodasNotificacionesLeidas()
            .thenRun(() -> {
                System.out.println("✅ [ServicioNotificaciones]: Todas las notificaciones marcadas como leídas");
                solicitarActualizacionNotificaciones(); // Actualizar la lista
            })
            .exceptionally(ex -> {
                System.err.println("❌ [ServicioNotificaciones]: Error al marcar todas como leídas: " + ex.getMessage());
                return null;
            });
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("✅ [ServicioNotificaciones]: Observador registrado. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🗑️ [ServicioNotificaciones]: Observador removido. Total: " + observadores.size());
    }

    private void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📢 [ServicioNotificaciones]: Notificando a " + observadores.size() + " observadores. Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}

