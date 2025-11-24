package servicio.p2p;

import dto.p2p.DTOPeerDetails;
import gestorP2P.FachadaP2P;
import gestorP2P.servicios.ServicioChat;
import gestorP2P.servicios.ServicioDescubrimiento;
import gestorP2P.servicios.ServicioGestionRed;
import gestorP2P.servicios.ServicioInformacion;
import gestorP2P.servicios.ServicioNotificacionCambios; // Importar Notificador
import gestorP2P.servicios.ServicioSincronizacionDatos; // Importar Sync
import logger.LoggerCentral;

import java.util.Collections;
import java.util.List;

/**
 * Capa de Aplicación limpia.
 * Actúa como el "Director de Orquesta" conectando los servicios entre sí.
 */
public class ServicioP2P implements IServicioP2PControl {

    private static final String TAG = "ServicioP2P";
    private final FachadaP2P fachada;

    // Referencias a servicios funcionales para uso directo desde el controlador
    private ServicioChat servicioChat;
    private ServicioInformacion servicioInfo;
    private ServicioSincronizacionDatos servicioSync;
    private ServicioNotificacionCambios notificador;

    private boolean running;

    public ServicioP2P() {
        LoggerCentral.info(TAG, ">> Creando instancia del Servicio Principal...");
        this.running = false;
        this.fachada = new FachadaP2P();
        LoggerCentral.info(TAG, "Configurando servicios internos...");
        configurarServicios();
    }

    /**
     * AQUÍ OCURRE LA MAGIA DE LA SINCRONIZACIÓN AUTOMÁTICA.
     * Registramos los servicios y los conectamos (Patrón Observador e Inyección).
     */
    private void configurarServicios() {
        // 1. Notificador (El Centro de Eventos - Bus de Datos)
        // Recibe avisos de cambios en BD y los propaga
        LoggerCentral.info(TAG, "Registrando ServicioNotificacionCambios...");
        this.notificador = new ServicioNotificacionCambios();
        fachada.registrarServicio(notificador);

        // 2. Gestión de Red (El Portero - Sujeto)
        // Maneja conexiones, heartbeats y avisa cuando alguien entra/sale.
        LoggerCentral.info(TAG, "Registrando ServicioGestionRed...");
        ServicioGestionRed srvRed = new ServicioGestionRed();
        fachada.registrarServicio(srvRed);

        // NUEVO: Registrar callback para detectar desconexiones automáticamente
        fachada.obtenerGestorConexionesImpl().setOnPeerDisconnectedCallback(peerId -> {
            LoggerCentral.warn(TAG, "🔴 Peer desconectado detectado: " + peerId);
            srvRed.onPeerDesconectado(peerId);
        });

        // 3. Sincronización (El Auditor - Observador)
        // Sabe comparar bases de datos usando Merkle Trees.
        LoggerCentral.info(TAG, "Registrando ServicioSincronizacionDatos...");
        this.servicioSync = new ServicioSincronizacionDatos();

        // Inyección inversa: Sync avisa al notificador si recupera datos antiguos
        this.servicioSync.setNotificador(notificador);
        fachada.registrarServicio(servicioSync);

        // --- CABLEADO DE OBSERVADORES (Conexiones neuronales del sistema) ---

        // A. Cold Sync: Sync vigila a Red. Si entra un peer, inicia verificación.
        srvRed.registrarObservador(servicioSync);

        // B. Hot Sync: Sync vigila al Notificador. Si hay cambios en BD local, recalcula el árbol.
        notificador.registrarObservador(servicioSync);

        // 4. Chat (El Productor de Datos)
        LoggerCentral.info(TAG, "Registrando ServicioChat...");
        this.servicioChat = new ServicioChat();

        // CORREGIDO: Chat ahora activa sincronización automática en lugar de notificar directamente
        this.servicioChat.setServicioSync(servicioSync);
        fachada.registrarServicio(servicioChat);

        // 5. Descubrimiento (Gossip simple opcional)
        LoggerCentral.info(TAG, "Registrando ServicioDescubrimiento...");
        ServicioDescubrimiento srvDiscovery = new ServicioDescubrimiento();
        fachada.registrarServicio(srvDiscovery);

        // 6. Información (Consultas para la UI/Consola)
        LoggerCentral.info(TAG, "Registrando ServicioInformacion...");
        this.servicioInfo = new ServicioInformacion();
        fachada.registrarServicio(servicioInfo);
    }

    // --- IMPLEMENTACIÓN DE IServicioP2PControl ---

    @Override
    public void iniciarRed() {
        if (running) {
            LoggerCentral.warn(TAG, "La red ya está corriendo.");
            return;
        }
        try {
            LoggerCentral.info(TAG, "Encendiendo motores...");
            fachada.iniciar();
            running = true;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error fatal al iniciar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void detenerRed() {
        if (!running) return;
        fachada.detener();
        running = false;
        LoggerCentral.info(TAG, "Red detenida.");
    }

    @Override
    public void sincronizarManual() {
        if (!running) {
            LoggerCentral.error(TAG, "Error: Red no iniciada.");
            return;
        }
        if (servicioSync != null) {
            servicioSync.forzarSincronizacion();
        } else {
            LoggerCentral.error(TAG, "Servicio de Sincronización no disponible.");
        }
    }

    @Override
    public List<DTOPeerDetails> obtenerListaPeers() {
        // Usamos el servicio de información para ver historial completo (Online/Offline)
        if (servicioInfo != null) {
            return servicioInfo.obtenerHistorialCompleto();
        }
        return running ? fachada.obtenerPeersConectados() : Collections.emptyList();
    }

    @Override
    public void enviarMensajeGlobal(String usuario, String mensaje) {
        if (!running) {
            LoggerCentral.error(TAG, "Error: Red detenida.");
            return;
        }
        if (servicioChat != null) {
            servicioChat.enviarMensajePublico(usuario, mensaje);
        }
    }

    @Override
    public void enviarMensajePrivado(String idPeerDestino, String usuario, String mensaje) {
        if (!running) return;
        if (servicioChat != null) {
            servicioChat.enviarMensajePrivado(idPeerDestino, usuario, mensaje);
        }
    }

    @Override
    public void conectarManual(String ip, int puerto) {
        if (!running) {
            LoggerCentral.error(TAG, "Inicia la red primero.");
            return;
        }
        fachada.conectarAPeer(ip, puerto);
    }

    @Override
    public boolean estaCorriendo() {
        return running;
    }
}