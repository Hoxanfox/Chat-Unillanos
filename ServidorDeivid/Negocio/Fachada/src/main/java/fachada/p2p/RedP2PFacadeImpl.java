package fachada.p2p;

import gestorP2P.GestorP2PImpl;
import gestorP2P.IGestorP2P;
import observador.IObservador;

import java.util.concurrent.CompletableFuture;

public class RedP2PFacadeImpl implements IRedP2PFacade {

    private final IGestorP2P gestor;

    // Constructor por defecto: crea la implementación concreta del gestor
    public RedP2PFacadeImpl() {
        this(new GestorP2PImpl());
    }

    // Constructor para inyección (tests / flexibilidad)
    public RedP2PFacadeImpl(IGestorP2P gestor) {
        this.gestor = gestor;
    }

    @Override
    public CompletableFuture<Void> iniciarRed() {
        // Delegar al gestor P2P
        return gestor.iniciarRed();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        gestor.registrarObservador(observador);
    }

    @Override
    public void removerObservador(IObservador observador) {
        gestor.removerObservador(observador);
    }
}
