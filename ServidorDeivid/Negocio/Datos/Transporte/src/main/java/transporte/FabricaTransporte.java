package transporte;

/**
 * Fábrica simple para crear instancias de transporte.
 * Actualmente soporta "TCP" y devuelve una implementación de TransporteTCP.
 */
public final class FabricaTransporte {

    private FabricaTransporte() {}

    public static ITransporte crearTransporte(String tipo) {
        if (tipo == null) return new TransporteTCP();
        switch (tipo.trim().toUpperCase()) {
            case "TCP":
                return new TransporteTCP();
            // En el futuro se pueden añadir otros tipos (UDP, TLS, MOCK, etc.)
            default:
                return new TransporteTCP();
        }
    }
}
