package dto.comunicacion.request.mensaje;

/** Wrapper reorganizado: Payload para envío de audio. Usa composición. */
public class DTOEnviarMensajeAudioPayload {
    private final dto.comunicacion.peticion.mensaje.DTOEnviarMensajeAudioPayload original;

    public DTOEnviarMensajeAudioPayload(dto.comunicacion.peticion.mensaje.DTOEnviarMensajeAudioPayload original) {
        this.original = original;
    }

    public dto.comunicacion.peticion.mensaje.DTOEnviarMensajeAudioPayload getOriginal() { return original; }
}
