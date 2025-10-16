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
import fachada.gestionUsuarios.session.IFachadaLobby;
import fachada.gestionUsuarios.session.FachadaLobby;
import fachada.gestionCanales.FachadaCanalesImpl;
import fachada.gestionCanales.IFachadaCanales;
import fachada.gestionNotificaciones.FachadaNotificacionesImpl;
import fachada.gestionNotificaciones.IFachadaNotificaciones;

/**
 * Implementación del Singleton de la Fachada General.
 * Crea y gestiona las instancias de TODAS las fachadas específicas.
 */
public class FachadaGeneralImpl implements IFachadaGeneral {

    private static FachadaGeneralImpl instancia;

    private final IFachadaAutenticacionUsuario fachadaAutenticacion;
    private final IFachadaArchivos fachadaArchivos;
    private final IFachadaConexion fachadaConexion;
    private final IFachadaChat fachadaChat;
    private final IFachadaContactos fachadaContactos;
    private final IFachadaUsuarios fachadaUsuarios;
    private final IFachadaCanales fachadaCanales;
    private final IFachadaNotificaciones fachadaNotificaciones;
    private IFachadaLobby fachadaLobby;

    private FachadaGeneralImpl() {
        // Inicializar todas las fachadas
        this.fachadaAutenticacion = new FachadaAutenticacionUsuario();
        this.fachadaArchivos = new FachadaArchivosImpl();
        this.fachadaConexion = new FachadaConexionImpl();
        this.fachadaChat = new FachadaChatImpl();
        this.fachadaContactos = new FachadaContactosImpl();
        this.fachadaUsuarios = new FachadaUsuariosImpl();
        this.fachadaCanales = new FachadaCanalesImpl();
        this.fachadaNotificaciones = new FachadaNotificacionesImpl();
    }

    public static synchronized IFachadaGeneral getInstancia() {
        if (instancia == null) {
            instancia = new FachadaGeneralImpl();
        }
        return instancia;
    }

    @Override
    public IFachadaLobby getFachadaLobby() {
        if (fachadaLobby == null) {
            fachadaLobby = new FachadaLobby();
        }
        return fachadaLobby;
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

    @Override
    public IFachadaCanales getFachadaCanales() {
        return fachadaCanales;
    }

    @Override
    public IFachadaNotificaciones getFachadaNotificaciones() {
        return fachadaNotificaciones;
    }
}
