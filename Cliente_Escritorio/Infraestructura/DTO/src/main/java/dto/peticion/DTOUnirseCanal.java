package dto.peticion;

/**
 * DTO para la petición de un usuario que desea unirse a un canal.
 */
public class DTOUnirseCanal {
    private final String usuarioId;
    private final String canalId;

    public DTOUnirseCanal(String usuarioId, String canalId) {
        this.usuarioId = usuarioId;
        this.canalId = canalId;
    }

    // Getters para la serialización
    public String getUsuarioId() {
        return usuarioId;
    }

    public String getCanalId() {
        return canalId;
    }
}
