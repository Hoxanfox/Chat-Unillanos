package servicio.p2p;

import observador.IObservador;

import java.util.concurrent.CompletableFuture;

public interface IP2PService {
    CompletableFuture<Void> iniciarRed();
    void registrarObservador(IObservador observador);
    void removerObservador(IObservador observador);
}
