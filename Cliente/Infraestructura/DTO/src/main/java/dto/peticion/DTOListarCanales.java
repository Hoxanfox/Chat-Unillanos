package dto.peticion;

/**
 * DTO para la petición de listar canales, permitiendo paginación.
 */
public class DTOListarCanales {
    private String usuarioId;
    private int limit;
    private int offset;

    public DTOListarCanales(String usuarioId, int limit, int offset) {
        this.usuarioId = usuarioId;
        this.limit = limit;
        this.offset = offset;
    }

    // Getters y Setters
    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
