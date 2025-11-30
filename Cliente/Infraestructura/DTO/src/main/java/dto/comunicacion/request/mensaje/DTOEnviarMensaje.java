package dto.comunicacion.request.mensaje;

/**
 * Wrapper reorganizado: Request para enviar mensaje (texto/archivo) a usuario.
 * Usa composición para evitar heredar de clases finales o con constructores privados.
 */
public class DTOEnviarMensaje {
    private final dto.comunicacion.peticion.mensaje.DTOEnviarMensaje original;

    public DTOEnviarMensaje(dto.comunicacion.peticion.mensaje.DTOEnviarMensaje original) {
        this.original = original;
    }

    public dto.comunicacion.peticion.mensaje.DTOEnviarMensaje getOriginal() { return original; }

}
