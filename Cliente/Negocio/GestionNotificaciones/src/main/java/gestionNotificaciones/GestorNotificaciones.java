package gestionNotificaciones;

import dto.featureNotificaciones.DTONotificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gestor de notificaciones del sistema.
 * Maneja la lógica de negocio relacionada con las notificaciones.
 * POR AHORA trabaja con datos de ejemplo hasta que se implemente la comunicación con el servidor.
 */
public class GestorNotificaciones {
    
    public GestorNotificaciones() {
        System.out.println("✅ [GestorNotificaciones]: Gestor inicializado");
    }
    
    /**
     * Obtiene la lista de notificaciones del usuario actual.
     * POR AHORA devuelve notificaciones de ejemplo para desarrollo.
     */
    public CompletableFuture<List<DTONotificacion>> obtenerNotificaciones() {
        System.out.println("📡 [GestorNotificaciones]: Obteniendo notificaciones...");

        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implementar comunicación real con el servidor
            System.out.println("ℹ️ [GestorNotificaciones]: Usando notificaciones de ejemplo");
            return crearNotificacionesEjemplo();
        });
    }
    
    /**
     * Marca una notificación específica como leída.
     */
    public CompletableFuture<Void> marcarComoLeida(String notificacionId) {
        System.out.println("📝 [GestorNotificaciones]: Marcando notificación como leída: " + notificacionId);
        
        return CompletableFuture.runAsync(() -> {
            // TODO: Implementar comunicación real con el servidor
            System.out.println("✅ [GestorNotificaciones]: Notificación marcada como leída (simulado)");
        });
    }
    
    /**
     * Marca todas las notificaciones como leídas.
     */
    public CompletableFuture<Void> marcarTodasComoLeidas() {
        System.out.println("📝 [GestorNotificaciones]: Marcando todas las notificaciones como leídas");
        
        return CompletableFuture.runAsync(() -> {
            // TODO: Implementar comunicación real con el servidor
            System.out.println("✅ [GestorNotificaciones]: Todas las notificaciones marcadas como leídas (simulado)");
        });
    }
    
    /**
     * Crea notificaciones de ejemplo para desarrollo y testing.
     */
    private List<DTONotificacion> crearNotificacionesEjemplo() {
        List<DTONotificacion> notificaciones = new ArrayList<>();
        
        notificaciones.add(new DTONotificacion(
            "notif1",
            "MENCION",
            "NEW (3)",
            "alice123 mentioned you in Team Alpha",
            LocalDateTime.now().minusMinutes(2),
            false,
            "alice123"
        ));
        
        notificaciones.add(new DTONotificacion(
            "notif2",
            "MENSAJE",
            "bob_smith sent you a message",
            "Do you have time to chat?",
            LocalDateTime.now().minusMinutes(16),
            false,
            "bob_smith"
        ));
        
        notificaciones.add(new DTONotificacion(
            "notif3",
            "SOLICITUD_AMISTAD",
            "emma_j sends a friend request",
            "Review 'Project Beta' again!",
            LocalDateTime.now().minusHours(5),
            true,
            "emma_j"
        ));
        
        return notificaciones;
    }
}
