// Explanation: Añadir interfaz para el controlador P2P que expone operaciones de alto nivel para la presentación.
package controlador.p2p;

import observador.IObservador;

import java.util.concurrent.CompletableFuture;

/**
 * Interfaz del controlador P2P para la capa de presentación.
 * Aplica DIP: la presentación depende de esta abstracción en lugar de la implementación concreta.
 */
public interface IP2PController {

    /**
     * Inicia la red P2P y devuelve un CompletableFuture para manejar asíncronamente el resultado.
     */
    CompletableFuture<Void> iniciarRed();

    /**
     * Permite que la UI (u otro componente) se suscriba para recibir eventos del controlador/servicio.
     */
    void registrarObservador(IObservador observador);

    /**
     * Permite que la UI (u otro componente) se desuscriba de los eventos.
     */
    void removerObservador(IObservador observador);

    /**
     * Cierra/limpia recursos mantenidos por el controlador (por ejemplo, quitar observadores).
     */
    void cerrar();

    // Métodos para registrar listeners de alto nivel (no dependen de la infraestructura Observador)
    void addListener(P2PListener listener);
    void removeListener(P2PListener listener);
}
