package gestorP2P.inicio;

import java.util.concurrent.CompletableFuture;

public interface IStarterP2P {
    CompletableFuture<Void> iniciar();
}

