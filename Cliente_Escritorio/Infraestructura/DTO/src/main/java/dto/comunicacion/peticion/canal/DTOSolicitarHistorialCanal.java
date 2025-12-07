package dto.comunicacion.peticion.canal;

/**
 * DTO para solicitar el historial de mensajes de un canal.
 */
public class DTOSolicitarHistorialCanal {
    private String canalId;
    private String usuarioId;
    private int limite;
    private int offset;

    public DTOSolicitarHistorialCanal() {
    }

    public DTOSolicitarHistorialCanal(String canalId, String usuarioId, int limite, int offset) {
        this.canalId = canalId;
        this.usuarioId = usuarioId;
        this.limite = limite;
        this.offset = offset;
    }

    // Getters y Setters
    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getLimite() {
        return limite;
    }

    public void setLimite(int limite) {
        this.limite = limite;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
