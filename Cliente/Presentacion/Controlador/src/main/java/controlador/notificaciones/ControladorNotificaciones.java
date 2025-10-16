package controlador.notificaciones;

import observador.IObservador;
import servicio.notificaciones.IServicioNotificaciones;
import servicio.notificaciones.ServicioNotificacionesImpl;

import java.util.concurrent.CompletableFuture;

/**
 * Controlador que gestiona las notificaciones del sistema.
 * Delega la lógica al servicio correspondiente.
 */
public class ControladorNotificaciones implements IControladorNotificaciones {

    private final IServicioNotificaciones servicioNotificaciones;

    public ControladorNotificaciones() {
        this.servicioNotificaciones = new ServicioNotificacionesImpl();
        System.out.println("✅ [ControladorNotificaciones]: Controlador inicializado");
    }

    @Override
    public void solicitarActualizacionNotificaciones() {
        System.out.println("➡️ [ControladorNotificaciones]: Delegando solicitud al servicio");
        servicioNotificaciones.solicitarActualizacionNotificaciones();
    }

    @Override
    public CompletableFuture<Void> marcarComoLeida(String notificacionId) {
        System.out.println("➡️ [ControladorNotificaciones]: Delegando marcar como leída al servicio");
        return servicioNotificaciones.marcarComoLeida(notificacionId);
    }

    @Override
    public CompletableFuture<Void> marcarTodasComoLeidas() {
        System.out.println("➡️ [ControladorNotificaciones]: Delegando marcar todas como leídas al servicio");
        return servicioNotificaciones.marcarTodasComoLeidas();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        System.out.println("➡️ [ControladorNotificaciones]: Delegando registro de observador al servicio");
        servicioNotificaciones.registrarObservador(observador);
    }

    @Override
    public void removerObservador(IObservador observador) {
        System.out.println("➡️ [ControladorNotificaciones]: Delegando remoción de observador al servicio");
        servicioNotificaciones.removerObservador(observador);
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        // Este método puede que no sea necesario en el controlador
        // ya que el servicio maneja las notificaciones internamente
        System.out.println("⚠️ [ControladorNotificaciones]: notificarObservadores llamado directamente");
    }
}
