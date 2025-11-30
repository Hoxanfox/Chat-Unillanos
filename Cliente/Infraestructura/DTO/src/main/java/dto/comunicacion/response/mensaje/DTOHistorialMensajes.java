package dto.comunicacion.response.mensaje;

/** Wrapper reorganizado: Response que contiene historial de mensajes. Usa composición. */
public class DTOHistorialMensajes {
    private final dto.comunicacion.respuesta.DTOHistorialMensajes original;

    public DTOHistorialMensajes(dto.comunicacion.respuesta.DTOHistorialMensajes original) {
        this.original = original;
    }

    public dto.comunicacion.respuesta.DTOHistorialMensajes getOriginal() { return original; }
}
