package dto.peticion;

/**
 * DTO para la petición de listar los miembros de un canal.
 */
public class DTOListarMiembros {
    private String canalId;
    private String solicitanteId;

    public DTOListarMiembros(String canalId, String solicitanteId) {
        this.canalId = canalId;
        this.solicitanteId = solicitanteId;
    }

    // Getters y Setters
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
}
