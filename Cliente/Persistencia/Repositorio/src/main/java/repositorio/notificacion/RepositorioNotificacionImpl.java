package repositorio.notificacion;

import dto.featureNotificaciones.DTONotificacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del repositorio de notificaciones.
 * SOLO maneja almacenamiento en caché local.
 * La comunicación con el servidor es responsabilidad de los gestores.
 *
 * ⭐ SINGLETON: Garantiza que todas las clases usen la misma instancia y compartan el caché.
 */
public class RepositorioNotificacionImpl implements IRepositorioNotificacion {

    private static RepositorioNotificacionImpl instancia;
    private final List<DTONotificacion> notificacionesCache;

    private RepositorioNotificacionImpl() {
        this.notificacionesCache = new ArrayList<>();
        System.err.println("✅✅✅ [RepositorioNotificacion]: Instancia SINGLETON creada (caché compartido)");
    }

    /**
     * Obtiene la instancia única del repositorio (patrón Singleton).
     */
    public static synchronized RepositorioNotificacionImpl getInstancia() {
        if (instancia == null) {
            instancia = new RepositorioNotificacionImpl();
        }
        return instancia;
    }

    @Override
    public void guardar(DTONotificacion notificacion) {
        // Agregar al inicio para que las más recientes aparezcan primero
        notificacionesCache.add(0, notificacion);
        System.err.println("💾💾💾 [RepositorioNotificacion]: Notificación guardada en caché: " + notificacion.getId());
        System.err.println("📊 [RepositorioNotificacion]: Total en caché después de guardar: " + notificacionesCache.size());
    }

    @Override
    public void guardarTodas(List<DTONotificacion> notificaciones) {
        notificacionesCache.clear();
        notificacionesCache.addAll(notificaciones);
        System.err.println("💾💾💾 [RepositorioNotificacion]: " + notificaciones.size() + " notificaciones guardadas en caché");
    }

    @Override
    public List<DTONotificacion> obtenerTodas() {
        System.err.println("📋📋📋 [RepositorioNotificacion]: obtenerTodas() llamado - Retornando " + notificacionesCache.size() + " notificaciones");
        return new ArrayList<>(notificacionesCache);
    }

    @Override
    public void remover(String notificacionId) {
        boolean removido = notificacionesCache.removeIf(n -> n.getId().equals(notificacionId));
        if (removido) {
            System.out.println("🗑️ [RepositorioNotificacion]: Notificación removida: " + notificacionId);
        }
    }

    @Override
    public void limpiarCache() {
        notificacionesCache.clear();
        System.out.println("🧹 [RepositorioNotificacion]: Caché limpiada");
    }

    @Override
    public DTONotificacion buscarPorId(String notificacionId) {
        Optional<DTONotificacion> notificacion = notificacionesCache.stream()
            .filter(n -> n.getId().equals(notificacionId))
            .findFirst();
        return notificacion.orElse(null);
    }
}