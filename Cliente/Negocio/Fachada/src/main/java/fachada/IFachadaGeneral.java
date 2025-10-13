package fachada;

import fachada.gestionContactos.IFachadaChat;
import fachada.gestionConexion.IFachadaConexion;
import fachada.gestionArchivos.IFachadaArchivos;
import fachada.gestionUsuarios.IFachadaAutenticacionUsuario;
import fachada.gestionContactos.IFachadaContactos; // Se importa la fachada de contactos

/**
 * Contrato para la Fachada General que centraliza el acceso
 * a todas las fachadas específicas.
 */
public interface IFachadaGeneral {
    IFachadaAutenticacionUsuario getFachadaAutenticacion();
    IFachadaArchivos getFachadaArchivos();
    IFachadaConexion getFachadaConexion();
    IFachadaChat getFachadaChat();

    /**
     * Devuelve la instancia de la fachada que gestiona la lógica de contactos.
     * @return una instancia de IFachadaContactos.
     */
    IFachadaContactos getFachadaContactos();
}

