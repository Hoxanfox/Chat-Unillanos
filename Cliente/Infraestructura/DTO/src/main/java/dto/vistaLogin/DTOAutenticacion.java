package dto.vistaLogin;

/**
 * DTO (Data Transfer Object) para transportar los datos de autenticación
 * desde la vista hacia las capas de lógica de negocio.
 */
public class DTOAutenticacion {
    private final String nombreUsuario;
    private final String password;

    public DTOAutenticacion(String nombreUsuario, String password) {
        this.nombreUsuario = nombreUsuario;
        this.password = password;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getPassword() {
        return password;
    }
}
