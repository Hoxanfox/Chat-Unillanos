package controlador.p2p;

/**
 * Listener de alto nivel para la capa de presentación. Evita que la UI tenga que depender
 * de la interfaz `observador.IObservador` de infraestructura.
 */
public interface P2PListener {
    void onEvent(String tipoDeDato, Object datos);
}

