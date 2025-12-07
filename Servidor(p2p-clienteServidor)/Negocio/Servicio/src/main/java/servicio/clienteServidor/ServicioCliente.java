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
import gestorClientes.servicios.ServicioResponderInvitacion;
import gestorClientes.servicios.ServiciosMensajeContactos;
import gestorClientes.servicios.ServicioArchivos;
import gestorClientes.servicios.ServicioHistorialCanal;
import gestorClientes.servicios.ServicioEnviarMensajeCanal;
import gestorClientes.servicios.ServicioNotificarMensajeCanal;
import gestorClientes.servicios.ServicioNotificarInvitacionCanal;
import gestorClientes.servicios.ServicioObtenerInvitaciones;
import gestorClientes.servicios.ServicioObtenerNotificaciones;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import gestorP2P.servicios.ServicioNotificacionCambios; // ✅ NUEVO IMPORT
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
    private ServicioResponderInvitacion servicioResponderInvitacion;
    private ServiciosMensajeContactos servicioMensajes;
    private ServicioArchivos servicioArchivos;
    private ServicioHistorialCanal servicioHistorialCanal;
    private ServicioEnviarMensajeCanal servicioEnviarMensajeCanal;
    private ServicioNotificarMensajeCanal servicioNotificarCanal;
    private ServicioNotificarInvitacionCanal servicioNotificarInvitacion;

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

        // 11. Gestión de Archivos (Subida/Descarga de archivos por chunks)
        LoggerCentral.info(TAG, "Registrando ServicioArchivos...");
        this.servicioArchivos = new ServicioArchivos();
        fachada.registrarServicio(servicioArchivos);

        // 12. Historial de Canal (Consultar historial de mensajes de un canal)
        LoggerCentral.info(TAG, "Registrando ServicioHistorialCanal...");
        servicioHistorialCanal = new ServicioHistorialCanal();
        fachada.registrarServicio(servicioHistorialCanal);

        // 13. Enviar Mensaje a Canal (Enviar un mensaje a todos los miembros de un canal)
        LoggerCentral.info(TAG, "Registrando ServicioEnviarMensajeCanal...");
        servicioEnviarMensajeCanal = new ServicioEnviarMensajeCanal();
        fachada.registrarServicio(servicioEnviarMensajeCanal);

        // 14. Notificar Invitación a Canal (Enviar notificaciones push de invitaciones)
        LoggerCentral.info(TAG, "Registrando ServicioNotificarInvitacionCanal...");
        servicioNotificarInvitacion = new ServicioNotificarInvitacionCanal();
        fachada.registrarServicio(servicioNotificarInvitacion);

        // 15. Obtener Invitaciones (Consultar invitaciones pendientes del usuario)
        LoggerCentral.info(TAG, "Registrando ServicioObtenerInvitaciones...");
        ServicioObtenerInvitaciones servicioObtenerInvitaciones = new ServicioObtenerInvitaciones();
        fachada.registrarServicio(servicioObtenerInvitaciones);

        // 16. Obtener Notificaciones (Consultar notificaciones pendientes del usuario)
        LoggerCentral.info(TAG, "Registrando ServicioObtenerNotificaciones...");
        ServicioObtenerNotificaciones servicioObtenerNotificaciones = new ServicioObtenerNotificaciones();
        fachada.registrarServicio(servicioObtenerNotificaciones);

        // 17. Responder Invitación (Aceptar/Rechazar invitaciones a canales)
        LoggerCentral.info(TAG, "Registrando ServicioResponderInvitacion...");
        ServicioResponderInvitacion servicioResponderInvitacion = new ServicioResponderInvitacion();
        fachada.registrarServicio(servicioResponderInvitacion);

        LoggerCentral.info(TAG, "✅ Todos los servicios CS registrados");
    }

    /**
     * ✅ NUEVO: Inyecta el servicio de sincronización P2P en los servicios de mensajería.
     * Debe llamarse DESPUÉS de iniciar la red P2P.
     */
    public void setServicioSincronizacionP2P(ServicioSincronizacionDatos servicioSyncP2P) {
        // ✅ NUEVO: Inyectar en ServicioAutenticacion
        if (servicioAuth != null) {
            servicioAuth.setServicioSync(servicioSyncP2P);
            LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en autenticación");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo inyectar servicio P2P: servicioAuth es null");
        }

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
            servicioInvitarMiembro.setServicioNotificarInvitacion(servicioNotificarInvitacion);
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

        // Inyectar P2P en el servicio de responder invitación
        if (servicioResponderInvitacion != null) {
            servicioResponderInvitacion.setServicioSincronizacionP2P(servicioSyncP2P);
            servicioResponderInvitacion.setServicioNotificacion(servicioNotificacion);
            LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en responder invitación");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo inyectar servicio P2P: servicioResponderInvitacion es null");
        }

        // Inyectar P2P en el servicio de archivos
        if (servicioArchivos != null) {
            servicioArchivos.setServicioSync(servicioSyncP2P);
            LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en archivos");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo inyectar servicio P2P: servicioArchivos es null");
        }

        // ✅ NUEVO: Inyectar P2P y notificaciones en el servicio de enviar mensaje a canal
        if (servicioEnviarMensajeCanal != null) {
            servicioEnviarMensajeCanal.setServicioSincronizacionP2P(servicioSyncP2P);
            servicioEnviarMensajeCanal.setServicioNotificacion(servicioNotificacion);
            LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en envío de mensajes a canal");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo inyectar servicio P2P: servicioEnviarMensajeCanal es null");
        }

        // ✅ NUEVO: Obtener el notificador de cambios desde el servicio de sincronización
        ServicioNotificacionCambios notificadorCambios = servicioSyncP2P.getServicioNotificacionCambios();

        if (notificadorCambios != null) {
            LoggerCentral.info(TAG, CYAN + "🔔 Inyectando ServicioNotificacionCambios en servicios CS..." + RESET);

            // Inyectar en ServicioInvitarMiembro
            if (servicioInvitarMiembro != null) {
                servicioInvitarMiembro.setServicioNotificacionCambios(notificadorCambios);
                LoggerCentral.info(TAG, "✅ NotificacionCambios inyectado en ServicioInvitarMiembro");
            }

            // Inyectar en ServicioCrearCanal
            if (servicioCrearCanal != null) {
                servicioCrearCanal.setServicioNotificacionCambios(notificadorCambios);
                LoggerCentral.info(TAG, "✅ NotificacionCambios inyectado en ServicioCrearCanal");
            }

            // Inyectar en ServicioUnirseCanal
            if (servicioUnirseCanal != null) {
                servicioUnirseCanal.setServicioNotificacionCambios(notificadorCambios);
                LoggerCentral.info(TAG, "✅ NotificacionCambios inyectado en ServicioUnirseCanal");
            }

            // Inyectar en ServicioRechazarInvitacion
            if (servicioRechazarInvitacion != null) {
                servicioRechazarInvitacion.setServicioNotificacionCambios(notificadorCambios);
                LoggerCentral.info(TAG, "✅ NotificacionCambios inyectado en ServicioRechazarInvitacion");
            }

            // Inyectar en ServicioResponderInvitacion
            if (servicioResponderInvitacion != null) {
                servicioResponderInvitacion.setServicioNotificacionCambios(notificadorCambios);
                LoggerCentral.info(TAG, "✅ NotificacionCambios inyectado en ServicioResponderInvitacion");
            }

            // Inyectar en ServicioEnviarMensajeCanal
            if (servicioEnviarMensajeCanal != null) {
                servicioEnviarMensajeCanal.setServicioNotificacionCambios(notificadorCambios);
                LoggerCentral.info(TAG, "✅ NotificacionCambios inyectado en ServicioEnviarMensajeCanal");
            }

            // Inyectar en ServiciosMensajeContactos
            if (servicioMensajes != null) {
                servicioMensajes.setServicioNotificacionCambios(notificadorCambios);
                LoggerCentral.info(TAG, "✅ NotificacionCambios inyectado en ServiciosMensajeContactos");
            }

            LoggerCentral.info(TAG, VERDE + "✅ ServicioNotificacionCambios inyectado en TODOS los servicios CS" + RESET);
        } else {
            LoggerCentral.error(TAG, ROJO + "❌ ERROR: ServicioNotificacionCambios es NULL - La sincronización P2P automática NO funcionará" + RESET);
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

    @Override
    public void desconectarCliente(String idSesion) {
        LoggerCentral.info(TAG, "Desconectando cliente con sesión: " + idSesion);
        if (fachada != null && fachada.getGestorClientes() != null) {
            fachada.getGestorClientes().desconectar(idSesion);
            LoggerCentral.info(TAG, "✓ Cliente desconectado: " + idSesion);
        } else {
            LoggerCentral.error(TAG, "❌ No se pudo desconectar el cliente: Fachada no disponible");
        }
    }

    // --- PATRÓN OBSERVER (delegado al ServicioGestionRed y ServicioAutenticacion) ---

    public void registrarObservador(IObservador observador) {
        // Registrar en ServicioGestionRed para eventos de conexión/desconexión
        ServicioGestionRed gestionRed = fachada.getServicioGestionRed();
        if (gestionRed != null) {
            gestionRed.registrarObservador(observador);
            LoggerCentral.debug(TAG, "Observador registrado en ServicioGestionRed");
        }

        // ✅ NUEVO: Registrar también en ServicioAutenticacion para eventos de login/logout
        if (servicioAuth != null) {
            servicioAuth.registrarObservador(observador);
            LoggerCentral.debug(TAG, "Observador registrado en ServicioAutenticacion");
        }
    }

    public void removerObservador(IObservador observador) {
        ServicioGestionRed gestionRed = fachada.getServicioGestionRed();
        if (gestionRed != null) {
            gestionRed.removerObservador(observador);
        }

        // ✅ NUEVO: Remover también del ServicioAutenticacion
        if (servicioAuth != null) {
            servicioAuth.removerObservador(observador);
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

    private static final String CYAN = "\u001B[36m";
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";
    private static final String RESET = "\u001B[0m";
}
