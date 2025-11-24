package servicio.p2p;

import dto.p2p.DTOPeerDetails;
import gestorP2P.FachadaP2P;
import gestorP2P.servicios.ServicioChat;
import gestorP2P.servicios.ServicioDescubrimiento;
import gestorP2P.servicios.ServicioGestionRed;
import gestorP2P.servicios.ServicioInformacion;
import gestorP2P.servicios.ServicioNotificacionCambios;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import gestorP2P.servicios.ServicioTransferenciaArchivos; // ✅ NUEVO
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
        LoggerCentral.info(TAG, "ServicioP2P inicializado correctamente.");
    }

    /**
     * AQUÍ OCURRE LA MAGIA DE LA SINCRONIZACIÓN AUTOMÁTICA.
     * Registramos los servicios y los conectamos (Patrón Observador e Inyección).
     */
    private void configurarServicios() {
        LoggerCentral.debug(TAG, "Iniciando configuración de servicios internos...");

        // 1. Notificador (El Centro de Eventos - Bus de Datos)
        // Recibe avisos de cambios en BD y los propaga
        LoggerCentral.info(TAG, "Registrando ServicioNotificacionCambios...");
        this.notificador = new ServicioNotificacionCambios();
        fachada.registrarServicio(notificador);
        LoggerCentral.debug(TAG, "✓ ServicioNotificacionCambios registrado.");

        // 2. Gestión de Red (El Portero - Sujeto)
        // Maneja conexiones, heartbeats y avisa cuando alguien entra/sale.
        LoggerCentral.info(TAG, "Registrando ServicioGestionRed...");
        ServicioGestionRed srvRed = new ServicioGestionRed();
        fachada.registrarServicio(srvRed);
        LoggerCentral.debug(TAG, "✓ ServicioGestionRed registrado.");

        // NUEVO: Registrar callback para detectar desconexiones automáticamente
        LoggerCentral.debug(TAG, "Configurando callback de desconexión en GestorConexiones...");
        fachada.obtenerGestorConexionesImpl().setOnPeerDisconnectedCallback(peerId -> {
            LoggerCentral.warn(TAG, "🔴 Peer desconectado detectado: " + peerId);
            srvRed.onPeerDesconectado(peerId);
        });
        LoggerCentral.debug(TAG, "✓ Callback de desconexión configurado.");

        // 3. Sincronización (El Auditor - Observador)
        // Sabe comparar bases de datos usando Merkle Trees.
        LoggerCentral.info(TAG, "Registrando ServicioSincronizacionDatos...");
        this.servicioSync = new ServicioSincronizacionDatos();

        // Inyección inversa: Sync avisa al notificador si recupera datos antiguos
        LoggerCentral.debug(TAG, "Inyectando Notificador en ServicioSync...");
        this.servicioSync.setNotificador(notificador);
        fachada.registrarServicio(servicioSync);
        LoggerCentral.debug(TAG, "✓ ServicioSincronizacionDatos registrado.");

        // ✅ 3.5. Transferencia de Archivos P2P (El Transportista)
        // Descarga automáticamente archivos físicos después de sincronizar metadatos
        LoggerCentral.info(TAG, "Registrando ServicioTransferenciaArchivos...");
        ServicioTransferenciaArchivos servicioTransferencia = new ServicioTransferenciaArchivos();
        fachada.registrarServicio(servicioTransferencia);
        LoggerCentral.debug(TAG, "✓ ServicioTransferenciaArchivos registrado.");

        // Inyectar servicio de transferencia en Sync para activarlo después de sincronizar metadatos
        LoggerCentral.debug(TAG, "Inyectando ServicioTransferenciaArchivos en ServicioSync...");
        this.servicioSync.setServicioTransferenciaArchivos(servicioTransferencia);
        LoggerCentral.debug(TAG, "✓ ServicioTransferenciaArchivos conectado a ServicioSync.");

        // --- CABLEADO DE OBSERVADORES (Conexiones neuronales del sistema) ---
        LoggerCentral.debug(TAG, "Cableando observadores entre servicios...");

        // A. Cold Sync: Sync vigila a Red. Si entra un peer, inicia verificación.
        srvRed.registrarObservador(servicioSync);
        LoggerCentral.debug(TAG, "✓ ServicioSync observando ServicioGestionRed (Cold Sync).");

        // B. Hot Sync: Sync vigila al Notificador. Si hay cambios en BD local, recalcula el árbol.
        notificador.registrarObservador(servicioSync);
        LoggerCentral.debug(TAG, "✓ ServicioSync observando Notificador (Hot Sync).");

        // 4. Chat (El Productor de Datos)
        LoggerCentral.info(TAG, "Registrando ServicioChat...");
        this.servicioChat = new ServicioChat();

        // CORREGIDO: Chat ahora activa sincronización automática en lugar de notificar directamente
        LoggerCentral.debug(TAG, "Inyectando ServicioSync en ServicioChat...");
        this.servicioChat.setServicioSync(servicioSync);
        fachada.registrarServicio(servicioChat);
        LoggerCentral.debug(TAG, "✓ ServicioChat registrado.");

        // 5. Descubrimiento (Gossip simple opcional)
        LoggerCentral.info(TAG, "Registrando ServicioDescubrimiento...");
        ServicioDescubrimiento srvDiscovery = new ServicioDescubrimiento();
        fachada.registrarServicio(srvDiscovery);
        LoggerCentral.debug(TAG, "✓ ServicioDescubrimiento registrado.");

        // 6. Información (Consultas para la UI/Consola)
        LoggerCentral.info(TAG, "Registrando ServicioInformacion...");
        this.servicioInfo = new ServicioInformacion();
        fachada.registrarServicio(servicioInfo);
        LoggerCentral.debug(TAG, "✓ ServicioInformacion registrado.");

        LoggerCentral.info(TAG, "Configuración de servicios completada exitosamente.");
    }

    // --- IMPLEMENTACIÓN DE IServicioP2PControl ---

    @Override
    public void iniciarRed() {
        if (running) {
            LoggerCentral.warn(TAG, "La red ya está corriendo.");
            return;
        }
        try {
            LoggerCentral.info(TAG, "Encendiendo motores de la red P2P...");
            fachada.iniciar();
            running = true;
            LoggerCentral.info(TAG, "✅ Red P2P iniciada exitosamente. Estado: EN EJECUCIÓN");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error fatal al iniciar red P2P: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void detenerRed() {
        if (!running) {
            LoggerCentral.warn(TAG, "La red no está corriendo.");
            return;
        }
        LoggerCentral.info(TAG, "Deteniendo red P2P...");
        fachada.detener();
        running = false;
        LoggerCentral.info(TAG, "✅ Red P2P detenida correctamente.");
    }

    @Override
    public void sincronizarManual() {
        if (!running) {
            LoggerCentral.error(TAG, "Error: Red no iniciada. No se puede sincronizar.");
            return;
        }
        if (servicioSync != null) {
            LoggerCentral.info(TAG, "Solicitando sincronización manual de datos...");
            servicioSync.forzarSincronizacion();
            LoggerCentral.debug(TAG, "Solicitud de sincronización enviada a ServicioSync.");
        } else {
            LoggerCentral.error(TAG, "❌ Servicio de Sincronización no disponible.");
        }
    }

    @Override
    public List<DTOPeerDetails> obtenerListaPeers() {
        LoggerCentral.debug(TAG, "Obteniendo lista de peers...");

        // Usamos el servicio de información para ver historial completo (Online/Offline)
        if (servicioInfo != null) {
            List<DTOPeerDetails> peers = servicioInfo.obtenerHistorialCompleto();
            LoggerCentral.debug(TAG, "Lista obtenida de ServicioInfo: " + peers.size() + " peers");
            return peers;
        }

        if (running) {
            List<DTOPeerDetails> peers = fachada.obtenerPeersConectados();
            LoggerCentral.debug(TAG, "Lista obtenida de FachadaP2P: " + peers.size() + " peers conectados");
            return peers;
        }

        LoggerCentral.warn(TAG, "Red no iniciada. Retornando lista vacía.");
        return Collections.emptyList();
    }

    @Override
    public void enviarMensajeGlobal(String usuario, String mensaje) {
        if (!running) {
            LoggerCentral.error(TAG, "Error: Red detenida. No se puede enviar mensaje.");
            return;
        }
        if (servicioChat != null) {
            LoggerCentral.info(TAG, "Enviando mensaje global de [" + usuario + "]: [" + mensaje + "]");
            servicioChat.enviarMensajePublico(usuario, mensaje);
            LoggerCentral.debug(TAG, "Mensaje global enviado a ServicioChat.");
        } else {
            LoggerCentral.error(TAG, "❌ ServicioChat no disponible.");
        }
    }

    @Override
    public void enviarMensajePrivado(String idPeerDestino, String usuario, String mensaje) {
        if (!running) {
            LoggerCentral.error(TAG, "Error: Red detenida. No se puede enviar mensaje privado.");
            return;
        }
        if (servicioChat != null) {
            LoggerCentral.info(TAG, "Enviando mensaje privado a [" + idPeerDestino + "] de [" + usuario + "]: [" + mensaje + "]");
            servicioChat.enviarMensajePrivado(idPeerDestino, usuario, mensaje);
            LoggerCentral.debug(TAG, "Mensaje privado enviado a ServicioChat.");
        } else {
            LoggerCentral.error(TAG, "❌ ServicioChat no disponible.");
        }
    }

    @Override
    public void conectarManual(String ip, int puerto) {
        if (!running) {
            LoggerCentral.error(TAG, "Error: Inicia la red primero.");
            return;
        }
        LoggerCentral.info(TAG, "Conectando manualmente a peer: " + ip + ":" + puerto);
        fachada.conectarAPeer(ip, puerto);
        LoggerCentral.debug(TAG, "Solicitud de conexión enviada a FachadaP2P.");
    }

    @Override
    public boolean estaCorriendo() {
        LoggerCentral.debug(TAG, "Estado consultado: " + (running ? "CORRIENDO" : "DETENIDA"));
        return running;
    }

    // === NUEVO: MÉTODOS PARA SUSCRIPCIÓN DE OBSERVADORES ===

    /**
     * Permite que el controlador registre un observador para recibir notificaciones
     * de cambios en los peers conectados (conexiones/desconexiones).
     */
    public void registrarObservadorConexiones(observador.IObservador observador) {
        LoggerCentral.info(TAG, "Registrando observador de conexiones en GestorConexiones...");

        if (fachada != null) {
            // Accedemos al GestorConexiones a través de la fachada
            conexion.p2p.impl.GestorConexionesImpl gestor = fachada.obtenerGestorConexionesImpl();
            if (gestor != null) {
                gestor.registrarObservador(observador);
                LoggerCentral.info(TAG, "✅ Observador registrado en GestorConexiones exitosamente.");
            } else {
                LoggerCentral.error(TAG, "❌ No se pudo obtener el GestorConexiones desde la fachada.");
            }
        } else {
            LoggerCentral.error(TAG, "❌ Fachada no disponible. No se pudo registrar observador.");
        }
    }
}