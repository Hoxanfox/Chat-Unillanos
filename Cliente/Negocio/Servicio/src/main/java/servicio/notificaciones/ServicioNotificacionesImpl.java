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
        System.out.println("📡 [ServicioNotificaciones]: Solicitando lista de notificaciones a la Fachada...");

        fachada.obtenerNotificaciones()
                .thenAccept(notificaciones -> {
                    System.out.println("✅ [ServicioNotificaciones]: Recibidas " + notificaciones.size() + " notificaciones del servidor");

                    // También incluir notificaciones del caché local (como invitaciones recibidas por PUSH)
                    List<DTONotificacion> notificacionesCache = fachada.obtenerNotificacionesCache();
                    System.out.println("📦 [ServicioNotificaciones]: " + notificacionesCache.size() + " notificaciones en caché local");

                    List<DTONotificacion> todasNotificaciones = new ArrayList<>(notificaciones);
                    todasNotificaciones.addAll(notificacionesCache);

                    System.out.println("📋 [ServicioNotificaciones]: Total notificaciones a mostrar: " + todasNotificaciones.size());
                    notificarObservadores("ACTUALIZAR_NOTIFICACIONES", todasNotificaciones);
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