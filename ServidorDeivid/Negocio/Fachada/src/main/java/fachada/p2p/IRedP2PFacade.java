package fachada.p2p;

import java.util.concurrent.CompletableFuture;
import observador.IObservador;

public interface IRedP2PFacade {
    CompletableFuture<Void> iniciarRed();
    void registrarObservador(IObservador observador);
    void removerObservador(IObservador observador);
}
