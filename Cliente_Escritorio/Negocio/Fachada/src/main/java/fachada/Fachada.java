package fachada;

import dto.featureNotificaciones.DTONotificacion;
import observador.IObservador;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación simplificada de la fachada para uso directo en controladores.
 * Delega a la fachada general.
 */
public class Fachada implements IFachada {

    private static Fachada instancia;
    private final IFachadaGeneral fachadaGeneral;

    private Fachada() {
        this.fachadaGeneral = FachadaGeneralImpl.getInstancia();
    }

    public static synchronized Fachada obtenerInstancia() {
        if (instancia == null) {
            instancia = new Fachada();
        }
        return instancia;
    }

    @Override
    public CompletableFuture<List<DTONotificacion>> obtenerNotificaciones() {
        return fachadaGeneral.getFachadaNotificaciones().obtenerNotificaciones();
    }

    @Override
    public CompletableFuture<Void> marcarNotificacionLeida(String notificacionId) {
        return fachadaGeneral.getFachadaNotificaciones().marcarNotificacionLeida(notificacionId);
    }

    @Override
    public CompletableFuture<Void> marcarTodasNotificacionesLeidas() {
        return fachadaGeneral.getFachadaNotificaciones().marcarTodasNotificacionesLeidas();
    }

    @Override
    public CompletableFuture<Void> aceptarInvitacionCanal(String invitacionId, String canalId) {
        return fachadaGeneral.getFachadaNotificaciones().aceptarInvitacionCanal(invitacionId, canalId);
    }

    @Override
    public CompletableFuture<Void> rechazarInvitacionCanal(String invitacionId, String canalId) {
        return fachadaGeneral.getFachadaNotificaciones().rechazarInvitacionCanal(invitacionId, canalId);
    }

    @Override
    public List<DTONotificacion> obtenerNotificacionesCache() {
        return fachadaGeneral.getFachadaNotificaciones().obtenerNotificacionesCache();
    }

    @Override
    public void registrarObservadorNotificaciones(IObservador observador) {
        fachadaGeneral.getFachadaNotificaciones().registrarObservador(observador);
    }

    @Override
    public void removerObservadorNotificaciones(IObservador observador) {
        fachadaGeneral.getFachadaNotificaciones().removerObservador(observador);
    }
}