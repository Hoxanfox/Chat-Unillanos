package fachada;

import fachada.gestionContactos.chat.IFachadaChat;
import fachada.gestionConexion.IFachadaConexion;
import fachada.gestionArchivos.IFachadaArchivos;
import fachada.gestionUsuarios.autenticacion.IFachadaAutenticacionUsuario;
import fachada.gestionUsuarios.insercionDB.IFachadaUsuarios;
import fachada.gestionContactos.contactos.IFachadaContactos;

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

    /**
     * Devuelve la instancia de la fachada que gestiona la lógica de usuarios.
     * @return una instancia de IFachadaUsuarios.
     */
    IFachadaUsuarios getFachadaUsuarios();
}
