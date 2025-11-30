package dto.comunicacion.request.canal;

/** Wrapper reorganizado: Request para enviar mensaje a canal. Usa composición. */
public class DTOEnviarMensajeCanal {
    private final dto.comunicacion.peticion.canal.DTOEnviarMensajeCanal original;

    public DTOEnviarMensajeCanal(dto.comunicacion.peticion.canal.DTOEnviarMensajeCanal original) {
        this.original = original;
    }

    public dto.comunicacion.peticion.canal.DTOEnviarMensajeCanal getOriginal() { return original; }

    public String getRemitenteId() { return original.getRemitenteId(); }
    public String getCanalId() { return original.getCanalId(); }
    public String getTipo() { return original.getTipo(); }
    public String getContenido() { return original.getContenido(); }
    public String getFileId() { return original.getFileId(); }
}
