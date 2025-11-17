package servicio.p2p;

import fachada.p2p.IRedP2PFacade;
import fachada.p2p.RedP2PFacadeImpl;
import observador.IObservador;
import logger.LoggerCentral;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class P2PServiceImpl implements IP2PService {

    private final IRedP2PFacade facade;
    // Map from external observer -> wrapper registered on facade
    private final Map<IObservador, IObservador> observerWrappers = new ConcurrentHashMap<>();

    public P2PServiceImpl() {
        this.facade = new RedP2PFacadeImpl();
    }

    // Constructor para inyección/testing
    public P2PServiceImpl(IRedP2PFacade facade) {
        this.facade = facade;
    }

    @Override
    public CompletableFuture<Void> iniciarRed() {
        return facade.iniciarRed();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (observador == null) return;
        LoggerCentral.debug("P2PServiceImpl.registrarObservador: creando wrapper simple para observador=" + observador.getClass().getName());
        // Wrapper mínimo: reenviador directo (el Gestor ahora emite DTOPeer y paquetes que la UI espera)
        IObservador wrapper = new IObservador() {
            @Override
            public void actualizar(String tipoDeDato, Object datos) {
                try {
                    observador.actualizar(tipoDeDato, datos);
                } catch (Exception ignored) {
                    // proteger contra fallos en la notificación del observador externo
                }
            }
        };
        observerWrappers.put(observador, wrapper);
        LoggerCentral.debug("P2PServiceImpl.registrarObservador: registrando wrapper en la fachada para observador=" + observador.getClass().getName());
        facade.registrarObservador(wrapper);
    }

    @Override
    public void removerObservador(IObservador observador) {
        if (observador == null) return;
        IObservador wrapper = observerWrappers.remove(observador);
        if (wrapper != null) {
            facade.removerObservador(wrapper);
        }
    }
}
