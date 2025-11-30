package gestionCanales.inicializador;

import gestionCanales.notificaciones.IGestorNotificacionesCanal;
import gestionCanales.notificaciones.GestorNotificacionesCanal;
import gestionCanales.listarMiembros.ListadorMiembros;
import gestionCanales.listarMiembros.IListadorMiembros;
import gestionCanales.invitarMiembro.InvitadorMiembro;
import gestionCanales.invitarMiembro.IInvitadorMiembro;
import gestionCanales.aceptarInvitacion.AceptadorInvitacion;
import gestionCanales.aceptarInvitacion.IAceptadorInvitacion;
import gestionCanales.listarCanales.IListadorCanales;
import gestionCanales.listarCanales.ListadorCanales;
import gestionCanales.nuevoCanal.CreadorCanal;
import gestionCanales.nuevoCanal.ICreadorCanal;
import gestionCanales.mensajes.IGestorMensajesCanal;
import gestionCanales.mensajes.GestorMensajesCanalImpl;
import gestionArchivos.IGestionArchivos;
import gestionArchivos.GestionArchivosImpl;
import repositorio.canal.IRepositorioCanal;
import repositorio.canal.RepositorioCanalImpl;
import repositorio.mensaje.IRepositorioMensajeCanal;
import repositorio.mensaje.RepositorioMensajeCanalImpl;

/**
 * Inicializador central del sistema de gestión de canales.
 * Configura todas las dependencias, repositorios y manejadores de respuestas.
 * 
 * IMPORTANTE: Este inicializador debe llamarse UNA VEZ después de que el usuario
 * se conecte exitosamente al servidor.
 */
public class InicializadorGestionCanales {

    private static InicializadorGestionCanales instancia;
    
    // Componentes del sistema
    private IRepositorioCanal repositorioCanal;
    private IRepositorioMensajeCanal repositorioMensajeCanal;
    
    private ICreadorCanal creadorCanal;
    private IListadorCanales listadorCanales;
    private IGestorMensajesCanal gestorMensajesCanal;
    private IGestorNotificacionesCanal gestorNotificacionesCanal;
    private IInvitadorMiembro invitadorMiembro;
    private IAceptadorInvitacion aceptadorInvitacion;
    private IListadorMiembros listadorMiembros;
    
    private boolean inicializado = false;

    private InicializadorGestionCanales() {
        // Constructor privado para Singleton
    }

    public static InicializadorGestionCanales getInstancia() {
        if (instancia == null) {
            synchronized (InicializadorGestionCanales.class) {
                if (instancia == null) {
                    instancia = new InicializadorGestionCanales();
                }
            }
        }
        return instancia;
    }

    /**
     * Inicializa todos los componentes del sistema de gestión de canales.
     * Debe llamarse después de conectarse al servidor.
     */
    public void inicializar() {
        if (inicializado) {
            System.out.println("⚠ Sistema de gestión de canales ya inicializado");
            return;
        }

        System.out.println("═════════════════════════════════════════════════");
        System.out.println("    INICIALIZANDO SISTEMA DE GESTIÓN DE CANALES");
        System.out.println("═════════════════════════════════════════════════");

        try {
            // 1. Inicializar repositorios (Capa de Persistencia)
            inicializarRepositorios();

            // 2. Inicializar componentes de negocio
            inicializarComponentesNegocio();

            // 3. Inicializar manejadores de respuestas del servidor
            inicializarManejadores();

            inicializado = true;
            System.out.println("✓ Sistema de gestión de canales inicializado correctamente");
            System.out.println("═════════════════════════════════════════════════\n");

        } catch (Exception e) {
            System.err.println("✗ ERROR al inicializar sistema de gestión de canales: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Fallo crítico en inicialización", e);
        }
    }

    private void inicializarRepositorios() {
        System.out.println("\n[1/3] Inicializando Repositorios...");
        
        repositorioCanal = new RepositorioCanalImpl();
        System.out.println("  ✓ RepositorioCanal creado");
        
        repositorioMensajeCanal = new RepositorioMensajeCanalImpl();
        System.out.println("  ✓ RepositorioMensajeCanal creado");
    }

    private void inicializarComponentesNegocio() {
        System.out.println("\n[2/3] Inicializando Componentes de Negocio...");
        
        // ✅ Inicializar IGestionArchivos para descarga automática de archivos
        IGestionArchivos gestionArchivos = new GestionArchivosImpl();
        System.out.println("  ✓ GestionArchivos inicializado");

        creadorCanal = new CreadorCanal(repositorioCanal);
        System.out.println("  ✓ CreadorCanal inicializado");
        
        listadorCanales = new ListadorCanales(repositorioCanal);
        System.out.println("  ✓ ListadorCanales inicializado");
        
        // 🆕 Pasar el repositorio de canales al gestor de mensajes
        gestorMensajesCanal = new GestorMensajesCanalImpl(repositorioMensajeCanal, gestionArchivos, repositorioCanal);
        System.out.println("  ✓ GestorMensajesCanal inicializado");
        
        gestorNotificacionesCanal = new GestorNotificacionesCanal();
        System.out.println("  ✓ GestorNotificacionesCanal inicializado");
        
        invitadorMiembro = new InvitadorMiembro(repositorioCanal);
        System.out.println("  ✓ InvitadorMiembro inicializado");
        
        aceptadorInvitacion = new AceptadorInvitacion(repositorioCanal);
        System.out.println("  ✓ AceptadorInvitacion inicializado");
        
        listadorMiembros = new ListadorMiembros(repositorioCanal);
        System.out.println("  ✓ ListadorMiembros inicializado");
    }

    private void inicializarManejadores() {
        System.out.println("\n[3/3] Registrando Manejadores de Respuestas...");
        
        // Los manejadores se registran automáticamente en los constructores,
        // pero algunos componentes necesitan inicialización explícita
        gestorMensajesCanal.inicializarManejadores();
        System.out.println("  ✓ Manejadores de mensajes registrados");
        
        gestorNotificacionesCanal.inicializarManejadores();
        System.out.println("  ✓ Manejadores de notificaciones registrados");

        listadorMiembros.inicializarManejadores();
        System.out.println("  ✓ Manejadores de cambios de miembros registrados");
    }

    /**
     * Verifica si el sistema está inicializado.
     */
    public boolean estaInicializado() {
        return inicializado;
    }

    /**
     * Reinicia el sistema (útil al cerrar sesión o reconectar).
     */
    public void reiniciar() {
        System.out.println("\n⟳ Reiniciando sistema de gestión de canales...");
        inicializado = false;
        inicializar();
    }

    // === GETTERS PARA ACCEDER A LOS COMPONENTES ===

    public ICreadorCanal getCreadorCanal() {
        validarInicializacion();
        return creadorCanal;
    }

    public IListadorCanales getListadorCanales() {
        validarInicializacion();
        return listadorCanales;
    }

    public IGestorMensajesCanal getGestorMensajesCanal() {
        validarInicializacion();
        return gestorMensajesCanal;
    }

    public IGestorNotificacionesCanal getGestorNotificacionesCanal() {
        validarInicializacion();
        return gestorNotificacionesCanal;
    }

    public IInvitadorMiembro getInvitadorMiembro() {
        validarInicializacion();
        return invitadorMiembro;
    }

    public IAceptadorInvitacion getAceptadorInvitacion() {
        validarInicializacion();
        return aceptadorInvitacion;
    }

    public IListadorMiembros getListadorMiembros() {
        validarInicializacion();
        return listadorMiembros;
    }

    public IRepositorioCanal getRepositorioCanal() {
        validarInicializacion();
        return repositorioCanal;
    }

    public IRepositorioMensajeCanal getRepositorioMensajeCanal() {
        validarInicializacion();
        return repositorioMensajeCanal;
    }

    private void validarInicializacion() {
        if (!inicializado) {
            throw new IllegalStateException(
                "El sistema de gestión de canales no ha sido inicializado. " +
                "Llame a InicializadorGestionCanales.getInstancia().inicializar() primero."
            );
        }
    }
}
