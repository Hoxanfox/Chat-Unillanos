package controlador.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import dto.vistaRegistro.DTOFormularioRegistro;
import observador.IObservador;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el controlador que gestiona la autenticación y el registro.
 */
public interface IControladorAutenticacion {
    CompletableFuture<Boolean> autenticar(DTOAutenticacion datos);
    CompletableFuture<Boolean> registrar(DTOFormularioRegistro datos);

    /**
     * Registra un observador para eventos de autenticación.
     */
    void registrarObservadorAutenticacion(IObservador observador);

    /**
     * Registra un observador para eventos de registro.
     */
    void registrarObservadorRegistro(IObservador observador);
}
