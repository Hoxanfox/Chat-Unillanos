package repositorio.notificacion;

import dto.featureNotificaciones.DTONotificacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del repositorio de notificaciones.
 * SOLO maneja almacenamiento en caché local.
 * La comunicación con el servidor es responsabilidad de los gestores.
 */
public class RepositorioNotificacionImpl implements IRepositorioNotificacion {

    private final List<DTONotificacion> notificacionesCache;

    public RepositorioNotificacionImpl() {
        this.notificacionesCache = new ArrayList<>();
        System.out.println("✅ [RepositorioNotificacion]: Inicializado (solo caché)");
    }

    @Override
    public void guardar(DTONotificacion notificacion) {
        // Agregar al inicio para que las más recientes aparezcan primero
        notificacionesCache.add(0, notificacion);
        System.out.println("💾 [RepositorioNotificacion]: Notificación guardada en caché: " + notificacion.getId());
    }

    @Override
    public void guardarTodas(List<DTONotificacion> notificaciones) {
        notificacionesCache.clear();
        notificacionesCache.addAll(notificaciones);
        System.out.println("💾 [RepositorioNotificacion]: " + notificaciones.size() + " notificaciones guardadas en caché");
    }

    @Override
    public List<DTONotificacion> obtenerTodas() {
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