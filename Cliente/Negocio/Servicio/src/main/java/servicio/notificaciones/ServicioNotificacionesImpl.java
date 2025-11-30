package servicio.notificaciones;

import dto.featureNotificaciones.DTONotificacion;
import fachada.Fachada;
import fachada.IFachada;
import observador.IObservador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de notificaciones.
 * Actúa como intermediario entre el Controlador y la Fachada.
 * Implementa IObservador para recibir notificaciones en tiempo real desde la fachada.
 */
public class ServicioNotificacionesImpl implements IServicioNotificaciones, IObservador {

    private final IFachada fachada;
    private final List<IObservador> observadores;

    public ServicioNotificacionesImpl() {
        this.fachada = Fachada.obtenerInstancia();
        this.observadores = new ArrayList<>();

        // ✨ CLAVE: Registrarse como observador de la fachada de notificaciones
        this.fachada.registrarObservadorNotificaciones(this);

        System.out.println("✅ [ServicioNotificaciones]: Creado, instanciado la Fachada y registrado como observador.");
    }

    @Override
    public void solicitarActualizacionNotificaciones() {
        System.err.println("📡📡📡 [ServicioNotificaciones]: ========== SOLICITANDO ACTUALIZACIÓN ==========");
        System.err.println("📡 [ServicioNotificaciones]: Solicitando lista de notificaciones a la Fachada...");

        // ✅ SOLUCIÓN: Obtener directamente del caché local
        List<DTONotificacion> notificacionesCache = fachada.obtenerNotificacionesCache();
        System.err.println("📦📦📦 [ServicioNotificaciones]: " + notificacionesCache.size() + " notificaciones en caché local");

        if (notificacionesCache.isEmpty()) {
            System.err.println("⚠️⚠️⚠️ [ServicioNotificaciones]: CACHÉ VACÍO - No hay notificaciones para mostrar");
        } else {
            System.err.println("✅✅✅ [ServicioNotificaciones]: Notificaciones encontradas:");
            for (int i = 0; i < notificacionesCache.size(); i++) {
                DTONotificacion n = notificacionesCache.get(i);
                System.err.println("   [" + (i+1) + "] ID: " + n.getId() + ", Tipo: " + n.getTipo() + ", Título: " + n.getTitulo());
            }
        }

        System.err.println("📋 [ServicioNotificaciones]: Total notificaciones a mostrar: " + notificacionesCache.size());
        System.err.println("📢 [ServicioNotificaciones]: Notificando a " + observadores.size() + " observadores con evento LISTA_NOTIFICACIONES");

        // ✅ Notificar inmediatamente con las notificaciones del caché
        notificarObservadores("LISTA_NOTIFICACIONES", notificacionesCache);
        System.err.println("📡📡📡 [ServicioNotificaciones]: ========== ACTUALIZACIÓN FINALIZADA ==========");
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
                    notificarObservadores("ERROR_NOTIFICACIONES", "Error al marcar como leída: " + ex.getMessage());
                    throw new RuntimeException(ex); // ✅ Propagar la excepción
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
                    notificarObservadores("ERROR_NOTIFICACIONES", "Error al marcar todas como leídas: " + ex.getMessage());
                    throw new RuntimeException(ex); // ✅ Propagar la excepción
                });
    }

    @Override
    public CompletableFuture<Void> aceptarInvitacionCanal(String invitacionId, String canalId) {
        System.out.println("✅ [ServicioNotificaciones]: Aceptando invitación a canal: " + canalId);

        return fachada.aceptarInvitacionCanal(invitacionId, canalId)
                .thenRun(() -> {
                    System.out.println("✅ [ServicioNotificaciones]: Invitación aceptada exitosamente");
                    solicitarActualizacionNotificaciones(); // Actualizar la lista
                    notificarObservadores("INVITACION_ACEPTADA", canalId);
                })
                .exceptionally(ex -> {
                    System.err.println("❌ [ServicioNotificaciones]: Error al aceptar invitación: " + ex.getMessage());
                    notificarObservadores("ERROR_NOTIFICACIONES", "Error al aceptar invitación: " + ex.getMessage());
                    throw new RuntimeException(ex); // ✅ Propagar la excepción
                });
    }

    @Override
    public CompletableFuture<Void> rechazarInvitacionCanal(String invitacionId, String canalId) {
        System.out.println("❌ [ServicioNotificaciones]: Rechazando invitación: " + invitacionId + " del canal: " + canalId);

        return fachada.rechazarInvitacionCanal(invitacionId, canalId)
                .thenRun(() -> {
                    System.out.println("✅ [ServicioNotificaciones]: Invitación rechazada exitosamente");
                    solicitarActualizacionNotificaciones(); // Actualizar la lista
                    notificarObservadores("INVITACION_RECHAZADA", invitacionId);
                })
                .exceptionally(ex -> {
                    System.err.println("❌ [ServicioNotificaciones]: Error al rechazar invitación: " + ex.getMessage());
                    notificarObservadores("ERROR_NOTIFICACIONES", "Error al rechazar invitación: " + ex.getMessage());
                    throw new RuntimeException(ex); // ✅ Propagar la excepción
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

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("📢 [ServicioNotificaciones]: Notificación recibida de la fachada - Tipo: " + tipoDeDato);

        // Si es una nueva notificación en tiempo real, actualizar la lista completa
        if ("NUEVA_NOTIFICACION".equals(tipoDeDato)) {
            System.out.println("🔔 [ServicioNotificaciones]: Nueva notificación en tiempo real, actualizando lista...");
            solicitarActualizacionNotificaciones();
        }

        // Redistribuir la notificación a los observadores del servicio (la UI)
        notificarObservadores(tipoDeDato, datos);
    }

    private void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📢 [ServicioNotificaciones]: Notificando a " + observadores.size() + " observadores. Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}