package fachada;

import fachada.gestionContactos.chat.IFachadaChat;
import fachada.gestionConexion.IFachadaConexion;
import fachada.gestionArchivos.IFachadaArchivos;
import fachada.gestionUsuarios.autenticacion.IFachadaAutenticacionUsuario;
import fachada.gestionUsuarios.insercionDB.IFachadaUsuarios;
import fachada.gestionContactos.contactos.IFachadaContactos;
import fachada.gestionUsuarios.session.IFachadaLobby;

/**
 * Contrato para la Fachada General que centraliza el acceso
 * a todas las fachadas específicas.
 */
public interface IFachadaGeneral {
    IFachadaAutenticacionUsuario getFachadaAutenticacion();
    IFachadaArchivos getFachadaArchivos();
    IFachadaConexion getFachadaConexion();
    IFachadaChat getFachadaChat();
    IFachadaContactos getFachadaContactos();
    IFachadaUsuarios getFachadaUsuarios();
    IFachadaLobby getFachadaLobby();
}
