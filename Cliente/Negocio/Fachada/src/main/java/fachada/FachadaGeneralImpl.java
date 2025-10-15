package fachada;

import fachada.gestionContactos.chat.FachadaChatImpl;
import fachada.gestionContactos.contactos.FachadaContactosImpl;
import fachada.gestionContactos.chat.IFachadaChat;

import fachada.gestionConexion.FachadaConexionImpl;
import fachada.gestionConexion.IFachadaConexion;
import fachada.gestionArchivos.FachadaArchivosImpl;
import fachada.gestionArchivos.IFachadaArchivos;
import fachada.gestionContactos.contactos.IFachadaContactos;
import fachada.gestionUsuarios.autenticacion.FachadaAutenticacionUsuario;
import fachada.gestionUsuarios.autenticacion.IFachadaAutenticacionUsuario;
import fachada.gestionUsuarios.insercionDB.IFachadaUsuarios;
import fachada.gestionUsuarios.insercionDB.FachadaUsuariosImpl;

/**
 * Implementación del Singleton de la Fachada General.
 * AHORA crea y gestiona las instancias de TODAS las fachadas específicas.
 */
public class FachadaGeneralImpl implements IFachadaGeneral {

    private static FachadaGeneralImpl instancia;

    private final IFachadaAutenticacionUsuario fachadaAutenticacion;
    private final IFachadaArchivos fachadaArchivos;
    private final IFachadaConexion fachadaConexion;
    private final IFachadaChat fachadaChat;
    private final IFachadaContactos fachadaContactos;
    private final IFachadaUsuarios fachadaUsuarios;

    private FachadaGeneralImpl() {
        // Al crearse, se instancian todas las fachadas específicas.
        this.fachadaAutenticacion = new FachadaAutenticacionUsuario();
        this.fachadaArchivos = new FachadaArchivosImpl();
        this.fachadaConexion = new FachadaConexionImpl();
        this.fachadaChat = new FachadaChatImpl();
        this.fachadaContactos = new FachadaContactosImpl();
        this.fachadaUsuarios = new FachadaUsuariosImpl();
    }

    public static synchronized IFachadaGeneral getInstancia() {
        if (instancia == null) {
            instancia = new FachadaGeneralImpl();
        }
        return instancia;
    }

    @Override
    public IFachadaAutenticacionUsuario getFachadaAutenticacion() {
        return fachadaAutenticacion;
    }

    @Override
    public IFachadaArchivos getFachadaArchivos() {
        return fachadaArchivos;
    }

    @Override
    public IFachadaConexion getFachadaConexion() {
        return fachadaConexion;
    }

    @Override
    public IFachadaChat getFachadaChat() {
        return fachadaChat;
    }

    @Override
    public IFachadaContactos getFachadaContactos() {
        return fachadaContactos;
    }

    @Override
    public IFachadaUsuarios getFachadaUsuarios() {
        return fachadaUsuarios;
    }
}
