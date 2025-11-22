package servicio.p2p;

import dto.p2p.DTOPeerDetails;
import gestorP2P.FachadaP2P;
import gestorP2P.servicios.ServicioChat;
import gestorP2P.servicios.ServicioDescubrimiento;
import gestorP2P.servicios.ServicioGestionRed;
// CORRECCIÓN: Asegúrate de importar desde el paquete .interfaces correcto
import servicio.p2p.IServicioP2PControl;

import java.util.Collections;
import java.util.List;

/**
 * Capa de Aplicación limpia.
 * INCLUYE LOGS DE DEPURACIÓN.
 */
public class ServicioP2P implements IServicioP2PControl {

    private final FachadaP2P fachada;
    private ServicioChat servicioChat;
    private boolean running;

    public ServicioP2P() {
        System.out.println("[ServicioP2P] >> Creando instancia del Servicio Principal...");
        this.running = false;

        System.out.println("[ServicioP2P] Inicializando FachadaP2P...");
        this.fachada = new FachadaP2P();

        System.out.println("[ServicioP2P] Configurando servicios internos...");
        configurarServicios();
        System.out.println("[ServicioP2P] << Instancia creada y configurada.");
    }

    private void configurarServicios() {
        // A. Gestión de Red
        System.out.println("[ServicioP2P] Registrando ServicioGestionRed...");
        ServicioGestionRed srvRed = new ServicioGestionRed();
        fachada.registrarServicio(srvRed);

        // B. Chat
        System.out.println("[ServicioP2P] Registrando ServicioChat...");
        this.servicioChat = new ServicioChat();
        fachada.registrarServicio(servicioChat);

        // C. Descubrimiento
        System.out.println("[ServicioP2P] Registrando ServicioDescubrimiento...");
        ServicioDescubrimiento srvDiscovery = new ServicioDescubrimiento();
        fachada.registrarServicio(srvDiscovery);
    }

    @Override
    public void iniciarRed() {
        System.out.println("[ServicioP2P] >> Solicitud de INICIO de red recibida.");
        if (running) {
            System.out.println("[ServicioP2P] ! La red ya se encuentra en ejecución. Omitiendo.");
            return;
        }
        try {
            System.out.println("[ServicioP2P] Delegando inicio a la Fachada...");
            fachada.iniciar();
            running = true;
            System.out.println("[ServicioP2P] << Red iniciada correctamente (Flag running=true).");
        } catch (Exception e) {
            System.err.println("[ServicioP2P] ERROR CRÍTICO al iniciar la red: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void detenerRed() {
        System.out.println("[ServicioP2P] >> Solicitud de PARADA de red recibida.");
        if (!running) {
            System.out.println("[ServicioP2P] ! La red no estaba corriendo. Omitiendo.");
            return;
        }
        try {
            fachada.detener();
            running = false;
            System.out.println("[ServicioP2P] << Red detenida y recursos liberados.");
        } catch (Exception e) {
            System.err.println("[ServicioP2P] Error al detener la red: " + e.getMessage());
        }
    }

    @Override
    public List<DTOPeerDetails> obtenerListaPeers() {
        // Logs reducidos aquí para no saturar la consola si se llama muy seguido
        if (!running) {
            System.out.println("[ServicioP2P] obtenerListaPeers llamado, pero la red está detenida.");
            return Collections.emptyList();
        }
        List<DTOPeerDetails> lista = fachada.obtenerPeersConectados();
        // System.out.println("[ServicioP2P] Consultando lista de peers. Total encontrados: " + lista.size());
        return lista;
    }

    @Override
    public void enviarMensajeGlobal(String usuario, String mensaje) {
        System.out.println("[ServicioP2P] >> Enviando mensaje global. Usuario: " + usuario + ", Msg: " + mensaje);
        if (!running) {
            System.err.println("[ServicioP2P] Error: No se puede enviar mensaje, red detenida.");
            return;
        }
        if (servicioChat != null) {
            servicioChat.enviarMensajePublico(usuario, mensaje);
        } else {
            System.err.println("[ServicioP2P] Error: ServicioChat no está inicializado.");
        }
    }

    @Override
    public void enviarMensajePrivado(String idPeerDestino, String usuario, String mensaje) {
        System.out.println("[ServicioP2P] >> Enviando privado a " + idPeerDestino);
        if (!running) return;
        servicioChat.enviarMensajePrivado(idPeerDestino, usuario, mensaje);
    }

    @Override
    public void conectarManual(String ip, int puerto) {
        System.out.println("[ServicioP2P] >> Solicitud manual de conexión a " + ip + ":" + puerto);
        if (!running) {
            System.err.println("[ServicioP2P] Error: Debe iniciar la red antes de conectar manual.");
            return;
        }
        fachada.conectarAPeer(ip, puerto);
    }

    @Override
    public boolean estaCorriendo() {
        return running;
    }
}