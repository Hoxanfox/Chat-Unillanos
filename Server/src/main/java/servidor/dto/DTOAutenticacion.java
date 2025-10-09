package servidor.dto;

/**
 * DTO para datos de autenticación del cliente.
 */
public class DTOAutenticacion {
    private String emailUsuario;
    private String passwordUsuario;

    public DTOAutenticacion() {}

    public DTOAutenticacion(String emailUsuario, String passwordUsuario) {
        this.emailUsuario = emailUsuario;
        this.passwordUsuario = passwordUsuario;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public void setEmailUsuario(String emailUsuario) {
        this.emailUsuario = emailUsuario;
    }

    public String getPasswordUsuario() {
        return passwordUsuario;
    }

    public void setPasswordUsuario(String passwordUsuario) {
        this.passwordUsuario = passwordUsuario;
    }
}

