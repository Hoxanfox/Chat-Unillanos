package servicio.clienteServidor;

import dto.cliente.DTOSesionCliente;
import gestorClientes.FachadaClientes;
import gestorClientes.servicios.usuario.ServicioAutenticacion;
import gestorClientes.servicios.ServicioNotificacionCliente;
import gestorClientes.servicios.ServicioGestionRed;
import gestorClientes.servicios.usuario.ServicioListarContactos;
import gestorClientes.servicios.ServicioListarCanales;
import gestorClientes.servicios.ServicioListarMiembros;
import gestorClientes.servicios.ServicioCrearCanal;
import gestorClientes.servicios.ServicioInvitarMiembro;
import gestorClientes.servicios.ServicioUnirseCanal;
import gestorClientes.servicios.ServicioRechazarInvitacion;
import gestorClientes.servicios.ServiciosMensajeContactos;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;
import observador.IObservador;

import java.util.List;

/**
 * Capa de Aplicación para el subsistema de Clientes.
 * Orquesta los servicios que atienden a los usuarios finales (Apps/Web).
 */
public class ServicioCliente implements IServicioClienteControl {

    private static final String TAG = "ServicioCliente";
    private final FachadaClientes fachada;

    // Referencias a servicios clave
    private ServicioNotificacionCliente servicioNotificacion;
    private ServicioAutenticacion servicioAuth;
    private ServicioListarContactos servicioListarContactos;
    private ServicioListarCanales servicioListarCanales;
    private ServicioListarMiembros servicioListarMiembros;
    private ServicioCrearCanal servicioCrearCanal;
    private ServicioInvitarMiembro servicioInvitarMiembro;
    private ServicioUnirseCanal servicioUnirseCanal;
    private ServicioRechazarInvitacion servicioRechazarInvitacion;
    private ServiciosMensajeContactos servicioMensajes;

    private boolean running;

    public ServicioCliente() {
        LoggerCentral.info(TAG, ">> Inicializando Subsistema de Clientes...");
        this.running = false;
        this.fachada = new FachadaClientes();
        configurarServicios();
    }

    private void configurarServicios() {
        // 1. Autenticación (Login/Registro)
        LoggerCentral.info(TAG, "Registrando ServicioAutenticacion...");
        this.servicioAuth = new ServicioAutenticacion();
        fachada.registrarServicio(servicioAuth);

        // 2. Notificaciones Push (Pasarela hacia clientes)
        LoggerCentral.info(TAG, "Registrando ServicioNotificacionCliente...");
        this.servicioNotificacion = new ServicioNotificacionCliente();
        fachada.registrarServicio(servicioNotificacion);

        // 3. Listar Contactos (Todos los usuarios del sistema)
        LoggerCentral.info(TAG, "Registrando ServicioListarContactos...");
        this.servicioListarContactos = new ServicioListarContactos();
        fachada.registrarServicio(servicioListarContactos);

        // 4. Listar Canales (Canales del usuario)
        LoggerCentral.info(TAG, "Registrando ServicioListarCanales...");
        this.servicioListarCanales = new ServicioListarCanales();
        fachada.registrarServicio(servicioListarCanales);

        // 5. Listar Miembros de un Canal
        LoggerCentral.info(TAG, "Registrando ServicioListarMiembros...");
        this.servicioListarMiembros = new ServicioListarMiembros();
        fachada.registrarServicio(servicioListarMiembros);

        // 6. Crear Canal (Nuevo canal con sincronización P2P)
        LoggerCentral.info(TAG, "Registrando ServicioCrearCanal...");
        this.servicioCrearCanal = new ServicioCrearCanal();
        fachada.registrarServicio(servicioCrearCanal);

        // 7. Servicios de Mensajería (Texto + Audio)
        LoggerCentral.info(TAG, "Registrando ServiciosMensajeContactos...");
        this.servicioMensajes = new ServiciosMensajeContactos();
        // Inyectar notificaciones en los servicios de mensajería
        this.servicioMensajes.setServicioNotificacion(servicioNotificacion);
        fachada.registrarServicio(servicioMensajes);

        // 8. Invitar Miembro (Agregar nuevos miembros a un canal)
        LoggerCentral.info(TAG, "Registrando ServicioInvitarMiembro...");
        this.servicioInvitarMiembro = new ServicioInvitarMiembro();
        fachada.registrarServicio(servicioInvitarMiembro);

        // 9. Unirse a Canal (Unirse a un canal existente)
        LoggerCentral.info(TAG, "Registrando ServicioUnirseCanal...");
        this.servicioUnirseCanal = new ServicioUnirseCanal();
        fachada.registrarServicio(servicioUnirseCanal);

        // 10. Rechazar Invitación (Rechazar una invitación a canal)
        LoggerCentral.info(TAG, "Registrando ServicioRechazarInvitacion...");
        this.servicioRechazarInvitacion = new ServicioRechazarInvitacion();
        fachada.registrarServicio(servicioRechazarInvitacion);

        LoggerCentral.info(TAG, "✅ Todos los servicios CS registrados");
    }

    /**
     * ✅ NUEVO: Inyecta el servicio de sincronización P2P en los servicios de mensajería.
     * Debe llamarse DESPUÉS de iniciar la red P2P.
     */
    public void setServicioSincronizacionP2P(ServicioSincronizacionDatos servicioSyncP2P) {
        if (servicioMensajes != null) {
            servicioMensajes.setServicioSincronizacionP2P(servicioSyncP2P);
            LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en mensajería");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo inyectar servicio P2P: servicioMensajes es null");
        }

        // Inyectar P2P en el servicio de crear canal
        if (servicioCrearCanal != null) {
            servicioCrearCanal.setServicioSincronizacionP2P(servicioSyncP2P);
            servicioCrearCanal.setServicioNotificacion(servicioNotificacion);
            LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en creación de canales");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo inyectar servicio P2P: servicioCrearCanal es null");
        }

        // Inyectar P2P en el servicio de invitar miembro
        if (servicioInvitarMiembro != null) {
            servicioInvitarMiembro.setServicioSincronizacionP2P(servicioSyncP2P);
            servicioInvitarMiembro.setServicioNotificacion(servicioNotificacion);
            LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en invitaciones");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo inyectar servicio P2P: servicioInvitarMiembro es null");
        }

        // Inyectar P2P en el servicio de unirse a canal
        if (servicioUnirseCanal != null) {
            servicioUnirseCanal.setServicioSincronizacionP2P(servicioSyncP2P);
            servicioUnirseCanal.setServicioNotificacion(servicioNotificacion);
            LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en unirse a canal");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo inyectar servicio P2P: servicioUnirseCanal es null");
        }

        // Inyectar P2P en el servicio de rechazar invitación
        if (servicioRechazarInvitacion != null) {
            servicioRechazarInvitacion.setServicioSincronizacionP2P(servicioSyncP2P);
            servicioRechazarInvitacion.setServicioNotificacion(servicioNotificacion);
            LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en rechazar invitación");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo inyectar servicio P2P: servicioRechazarInvitacion es null");
        }
    }

    // --- IMPLEMENTACIÓN DE LA INTERFAZ DE CONTROL ---

    @Override
    public void iniciarServidor(int puerto) {
        if (running) {
            LoggerCentral.warn(TAG, "Servidor de clientes ya corriendo.");
            return;
        }
        try {
            LoggerCentral.info(TAG, "Levantando servidor de clientes en puerto " + puerto + "...");
            fachada.iniciar(puerto);
            running = true;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Fallo al iniciar servidor clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void detenerServidor() {
        if (!running) return;
        fachada.detener();
        running = false;
        LoggerCentral.info(TAG, "Servidor de clientes detenido.");
    }

    @Override
    public boolean estaCorriendo() {
        return running;
    }

    @Override
    public ServicioNotificacionCliente getServicioNotificacion() {
        return servicioNotificacion;
    }

    @Override
    public int getNumeroClientesConectados() {
        ServicioGestionRed gestionRed = fachada.getServicioGestionRed();
        return gestionRed != null ? gestionRed.getNumeroClientesConectados() : 0;
    }

    @Override
    public List<DTOSesionCliente> getSesionesActivas() {
        ServicioGestionRed gestionRed = fachada.getServicioGestionRed();
        return gestionRed != null ? gestionRed.getSesionesActivas() : List.of();
    }

    // --- PATRÓN OBSERVER (delegado al ServicioGestionRed) ---

    public void registrarObservador(IObservador observador) {
        ServicioGestionRed gestionRed = fachada.getServicioGestionRed();
        if (gestionRed != null) {
            gestionRed.registrarObservador(observador);
        }
    }

    public void removerObservador(IObservador observador) {
        ServicioGestionRed gestionRed = fachada.getServicioGestionRed();
        if (gestionRed != null) {
            gestionRed.removerObservador(observador);
        }
    }

    public void notificarObservadores(String tipo, Object datos) {
        ServicioGestionRed gestionRed = fachada.getServicioGestionRed();
        if (gestionRed != null) {
            gestionRed.notificarObservadores(tipo, datos);
        }
    }

    /**
     * ✅ NUEVO: Expone el ServicioGestionRed interno para que otros servicios puedan observarlo.
     * Útil para conectar con ServicioTopologiaRed y actualizar la topología cuando cambien los clientes.
     */
    public ServicioGestionRed getServicioGestionRed() {
        return fachada.getServicioGestionRed();
    }
}

