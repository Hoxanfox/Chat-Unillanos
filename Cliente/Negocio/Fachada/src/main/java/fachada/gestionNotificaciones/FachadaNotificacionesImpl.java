package fachada.gestionNotificaciones;

import dto.featureNotificaciones.DTONotificacion;
import gestionNotificaciones.GestorNotificaciones;
import observador.IObservador;
import fachada.gestionCanales.FachadaCanalesImpl; // [NUEVO] Import para conectar con GestorInvitaciones

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la fachada de notificaciones.
 * Delega las operaciones al gestor correspondiente.
 * Implementa IObservador para recibir notificaciones del GestorNotificaciones
 * y redistribuirlas a sus propios observadores.
 */
public class FachadaNotificacionesImpl implements IFachadaNotificaciones, IObservador {

    private final GestorNotificaciones gestorNotificaciones;
    private final List<IObservador> observadores;

    public FachadaNotificacionesImpl() {
        this.gestorNotificaciones = new GestorNotificaciones();
        this.observadores = new ArrayList<>();
        this.gestorNotificaciones.inicializarManejadores();

        // ✨ CLAVE: Registrarse como observador del gestor
        this.gestorNotificaciones.registrarObservador(this);

        System.err.println("✅ [FachadaNotificaciones]: Fachada inicializada y registrada como observador del gestor");

        // 🔥🔥 CLAVE: Registrar GestorNotificaciones como observador de GestorInvitaciones
        // Esto evita la dependencia circular
        try {
            FachadaCanalesImpl fachadaCanales = FachadaCanalesImpl.getInstancia();
            fachadaCanales.getGestorInvitaciones().registrarObservador(this.gestorNotificaciones);
            System.err.println("✅ [FachadaNotificaciones]: ⭐ GestorNotificaciones registrado como observador de GestorInvitaciones");
        } catch (Exception e) {
            System.err.println("⚠️ [FachadaNotificaciones]: Error al registrar en GestorInvitaciones: " + e.getMessage());
        }
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
    public CompletableFuture<Void> rechazarInvitacionCanal(String invitacionId, String canalId) {
        System.out.println("❌ [FachadaNotificaciones]: Rechazando invitación: " + invitacionId + " del canal: " + canalId);
        return gestorNotificaciones.rechazarInvitacionCanal(invitacionId, canalId);
    }

    @Override
    public List<DTONotificacion> obtenerNotificacionesCache() {
        System.out.println("📋 [FachadaNotificaciones]: Obteniendo notificaciones del caché local");
        return gestorNotificaciones.obtenerNotificacionesCache();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("🔔 [FachadaNotificaciones]: Observador registrado. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🔕 [FachadaNotificaciones]: Observador removido. Total: " + observadores.size());
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("📢 [FachadaNotificaciones]: Notificación recibida del gestor - Tipo: " + tipoDeDato);

        // Redistribuir la notificación a los observadores de la fachada
        notificarObservadores(tipoDeDato, datos);
    }

    private void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📣 [FachadaNotificaciones]: Notificando a " + observadores.size() + " observadores - Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }

    /**
     * Obtiene la instancia del gestor de notificaciones para permitir registro de observadores.
     */
    public GestorNotificaciones getGestorNotificaciones() {
        return gestorNotificaciones;
    }
}