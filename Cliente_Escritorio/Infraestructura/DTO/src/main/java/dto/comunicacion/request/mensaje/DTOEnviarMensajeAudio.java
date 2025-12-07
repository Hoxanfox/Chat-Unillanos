package dto.comunicacion.request.mensaje;

/** Wrapper reorganizado: Request para enviar mensaje de audio. Usa composición. */
public class DTOEnviarMensajeAudio {
    private final dto.comunicacion.peticion.mensaje.DTOEnviarMensajeAudio original;

    public DTOEnviarMensajeAudio(dto.comunicacion.peticion.mensaje.DTOEnviarMensajeAudio original) {
        this.original = original;
    }

    public dto.comunicacion.peticion.mensaje.DTOEnviarMensajeAudio getOriginal() { return original; }
}

