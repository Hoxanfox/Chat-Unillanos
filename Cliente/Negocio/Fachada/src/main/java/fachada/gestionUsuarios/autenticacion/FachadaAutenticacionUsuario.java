package fachada.gestionUsuarios.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import dto.vistaRegistro.DTORegistro;
import gestionUsuario.autenticacion.AutenticarUsuario;
import gestionUsuario.autenticacion.IAutenticarUsuario;
import gestionUsuario.registro.IRegistroUsuario;
import gestionUsuario.registro.RegistroUsuarioImpl;
import observador.IObservador;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada que gestiona la autenticación y registro de usuarios.
 * Orquesta los diferentes componentes de la capa de gestión.
 */
public class FachadaAutenticacionUsuario implements IFachadaAutenticacionUsuario {

    // La Fachada ahora depende de los dos componentes de gestión.
    private final IAutenticarUsuario gestionAutenticacion;
    private final IRegistroUsuario gestionRegistro;

    public FachadaAutenticacionUsuario() {
        // En una aplicación real, estas dependencias se inyectarían.
        this.gestionAutenticacion = new AutenticarUsuario();
        this.gestionRegistro = new RegistroUsuarioImpl();
    }

    @Override
    public CompletableFuture<Boolean> autenticarUsuario(DTOAutenticacion dto) {
        // Delega la autenticación a su especialista.
        return gestionAutenticacion.autenticar(dto);
    }

    @Override
    public CompletableFuture<Boolean> registrarUsuario(DTORegistro dto, byte[] fotoBytes) {
        // Delega el registro a su especialista.
        return gestionRegistro.registrar(dto, fotoBytes);
    }

    @Override
    public void registrarObservadorAutenticacion(IObservador observador) {
        System.out.println("🔔 [FachadaAutenticacionUsuario]: Registrando observador en Autenticación");
        gestionAutenticacion.registrarObservador(observador);
    }

    @Override
    public void registrarObservadorRegistro(IObservador observador) {
        System.out.println("🔔 [FachadaAutenticacionUsuario]: Registrando observador en Registro");
        gestionRegistro.registrarObservador(observador);
    }
}
