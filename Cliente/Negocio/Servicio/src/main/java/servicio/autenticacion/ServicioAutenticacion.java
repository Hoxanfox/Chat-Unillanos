package servicio.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import dto.vistaRegistro.DTORegistro;
import fachada.gestionUsuarios.FachadaAutenticacionUsuario;
import fachada.gestionUsuarios.IFachadaAutenticacionUsuario;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de autenticación y registro.
 * Delega toda la lógica a la Fachada, respetando la arquitectura.
 */
public class ServicioAutenticacion implements IServicioAutenticacion {

    // El servicio depende de la fachada, que es su único punto de entrada a la lógica de negocio.
    private final IFachadaAutenticacionUsuario fachada;

    public ServicioAutenticacion() {
        // En una aplicación real, esta dependencia se inyectaría.
        this.fachada = new FachadaAutenticacionUsuario();
    }

    @Override
    public CompletableFuture<Boolean> autenticar(DTOAutenticacion datos) {
        System.out.println("ServicioAutenticacion: Delegando autenticación a la Fachada...");
        return fachada.autenticarUsuario(datos);
    }

    @Override
    public CompletableFuture<Boolean> registrar(DTORegistro datos, byte[] fotoBytes) {
        System.out.println("ServicioAutenticacion: Delegando registro a la Fachada...");
        // Pasa ambos argumentos a la fachada.
        return fachada.registrarUsuario(datos, fotoBytes);
    }
}

