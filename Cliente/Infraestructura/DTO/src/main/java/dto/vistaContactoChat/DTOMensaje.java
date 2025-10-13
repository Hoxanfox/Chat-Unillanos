package dto.vistaContactoChat;

/**
 * DTO para representar un mensaje en la interfaz de usuario.
 */
public final class DTOMensaje {
    private final String autorConFecha; // Ej: "john_doe - 20:15"
    private final String contenido;
    private final boolean esMio; // Para saber si alinear a la derecha o a la izquierda

    public DTOMensaje(String autorConFecha, String contenido, boolean esMio) {
        this.autorConFecha = autorConFecha;
        this.contenido = contenido;
        this.esMio = esMio;
    }

    // Getters...
    public String getAutorConFecha() { return autorConFecha; }
    public String getContenido() { return contenido; }
    public boolean esMio() { return esMio; }
}
