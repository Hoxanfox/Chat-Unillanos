package servicio.autenticacion;

import dto.vistaLogin.DTOAutenticacion;

/**
 * Implementación del servicio que contiene la lógica de negocio para la autenticación.
 * Aquí es donde se conectaría a una base de datos o una API externa.
 */
public class ServicioAutenticacionImpl implements IServicioAutenticacion {

    @Override
    public boolean autenticar(DTOAutenticacion datos) {
        // Lógica de negocio de ejemplo.
        // Se usan los nuevos métodos get del DTO.
        return "admin@example.com".equals(datos.getEmailUsuario()) && "1234".equals(datos.getPasswordUsuario());
    }
}

