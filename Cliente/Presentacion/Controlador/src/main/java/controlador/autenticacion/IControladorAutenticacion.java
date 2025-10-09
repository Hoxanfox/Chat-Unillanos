package controlador.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import dto.vistaRegistro.DTOFormularioRegistro;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el controlador que gestiona la autenticación y el registro.
 */
public interface IControladorAutenticacion {
    CompletableFuture<Boolean> autenticar(DTOAutenticacion datos);
    CompletableFuture<Boolean> registrar(DTOFormularioRegistro datos);
}

