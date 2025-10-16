package dto.peticion;

/**
 * DTO para encapsular los datos de la petición de gestión de miembros de un canal.
 * Se utiliza para agregar, remover o cambiar el rol de un usuario.
 */
public class DTOGestionarMiembro {
    private final String adminId;
    private final String canalId;
    private final String usuarioId;
    private final String accion;
    private final String nuevoRol; // Opcional, solo para la acción "CAMBIAR_ROL"

    /**
     * Constructor para acciones que no requieren un nuevo rol (AGREGAR, REMOVER).
     *
     * @param adminId   ID del administrador que ejecuta la acción.
     * @param canalId   ID del canal afectado.
     * @param usuarioId ID del usuario a gestionar.
     * @param accion    La acción a realizar ("AGREGAR" o "REMOVER").
     */
    public DTOGestionarMiembro(String adminId, String canalId, String usuarioId, String accion) {
        this.adminId = adminId;
        this.canalId = canalId;
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.nuevoRol = null;
    }

    /**
     * Constructor completo para la acción "CAMBIAR_ROL".
     *
     * @param adminId   ID del administrador que ejecuta la acción.
     * @param canalId   ID del canal afectado.
     * @param usuarioId ID del usuario a gestionar.
     * @param accion    La acción a realizar ("CAMBIAR_ROL").
     * @param nuevoRol  El nuevo rol a asignar ("ADMIN" o "MEMBER").
     */
    public DTOGestionarMiembro(String adminId, String canalId, String usuarioId, String accion, String nuevoRol) {
        this.adminId = adminId;
        this.canalId = canalId;
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.nuevoRol = nuevoRol;
    }

    // Getters para la serialización a JSON
    public String getAdminId() { return adminId; }
    public String getCanalId() { return canalId; }
    public String getUsuarioId() { return usuarioId; }
    public String getAccion() { return accion; }
    public String getNuevoRol() { return nuevoRol; }
}
