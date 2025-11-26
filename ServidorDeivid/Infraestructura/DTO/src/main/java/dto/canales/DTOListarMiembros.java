package dto.canales;

/**
 * DTO para solicitar la lista de miembros de un canal.
 * Usado en la acción: listarMiembros
 */
public class DTOListarMiembros {
    private String canalId;
    private String solicitanteId;

    public DTOListarMiembros() {}

    public DTOListarMiembros(String canalId, String solicitanteId) {
        this.canalId = canalId;
        this.solicitanteId = solicitanteId;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getSolicitanteId() {
        return solicitanteId;
    }

    public void setSolicitanteId(String solicitanteId) {
        this.solicitanteId = solicitanteId;
    }

    @Override
    public String toString() {
        return "DTOListarMiembros{" +
                "canalId='" + canalId + '\'' +
                ", solicitanteId='" + solicitanteId + '\'' +
                '}';
    }
}

