package fachada.gestionNotificaciones;

import dto.featureNotificaciones.DTONotificacion;
import gestionNotificaciones.GestorNotificaciones;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la fachada de notificaciones.
 * Delega las operaciones al gestor correspondiente.
 */
public class FachadaNotificacionesImpl implements IFachadaNotificaciones {
    
    private final GestorNotificaciones gestorNotificaciones;
    
    public FachadaNotificacionesImpl() {
        this.gestorNotificaciones = new GestorNotificaciones();
        System.out.println("✅ [FachadaNotificaciones]: Fachada inicializada");
    }
    
    @Override
    public CompletableFuture<List<DTONotificacion>> obtenerNotificaciones() {
        System.out.println("📡 [FachadaNotificaciones]: Solicitando notificaciones al gestor...");
        return gestorNotificaciones.obtenerNotificaciones();
    }
    
    @Override
    public CompletableFuture<Void> marcarNotificacionLeida(String notificacionId) {
        System.out.println("📝 [FachadaNotificaciones]: Marcando notificación como leída: " + notificacionId);
        return gestorNotificaciones.marcarComoLeida(notificacionId);
    }
    
    @Override
    public CompletableFuture<Void> marcarTodasNotificacionesLeidas() {
        System.out.println("📝 [FachadaNotificaciones]: Marcando todas las notificaciones como leídas");
        return gestorNotificaciones.marcarTodasComoLeidas();
    }

    @Override
    public CompletableFuture<Void> aceptarInvitacionCanal(String invitacionId, String canalId) {
        System.out.println("✅ [FachadaNotificaciones]: Aceptando invitación a canal: " + canalId);
        return gestorNotificaciones.aceptarInvitacionCanal(invitacionId, canalId);
    }

    @Override
    public CompletableFuture<Void> rechazarInvitacionCanal(String invitacionId) {
        System.out.println("❌ [FachadaNotificaciones]: Rechazando invitación: " + invitacionId);
        return gestorNotificaciones.rechazarInvitacionCanal(invitacionId);
    }
}
