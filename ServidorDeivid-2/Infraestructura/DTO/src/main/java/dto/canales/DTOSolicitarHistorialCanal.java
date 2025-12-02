package dto.canales;

import java.io.Serializable;

/**
 * DTO para solicitar el historial de mensajes de un canal.
 */
public class DTOSolicitarHistorialCanal implements Serializable {

    private static final long serialVersionUID = 1L;

    private String canalId;
    private String usuarioId;
    private int limite;
    private int offset;

    public DTOSolicitarHistorialCanal() {
        this.limite = 50; // Valor por defecto
        this.offset = 0;
    }

    public DTOSolicitarHistorialCanal(String canalId, String usuarioId, int limite, int offset) {
        this.canalId = canalId;
        this.usuarioId = usuarioId;
        this.limite = limite > 0 ? limite : 50;
        this.offset = offset >= 0 ? offset : 0;
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

    @Override
    public String toString() {
        return "DTOSolicitarHistorialCanal{" +
                "canalId='" + canalId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", limite=" + limite +
                ", offset=" + offset +
                '}';
    }
}

