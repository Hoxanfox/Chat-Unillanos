package dto.vistaLogin;

/**
 * DTO (Data Transfer Object) para transportar los datos de autenticación
 * desde la vista hacia las capas de lógica de negocio.
 */
public class DTOAutenticacion {
    private final String emailUsuario;
    private final String passwordUsuario;

    public DTOAutenticacion(String emailUsuario, String passwordUsuario) {
        this.emailUsuario = emailUsuario;
        this.passwordUsuario = passwordUsuario;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public String getPasswordUsuario() {
        return passwordUsuario;
    }
}

