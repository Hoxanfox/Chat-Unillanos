package servicio.p2p;

import dto.p2p.DTOPeerDetails;
import gestorP2P.FachadaP2P;
import gestorP2P.servicios.ServicioChat;
import gestorP2P.servicios.ServicioDescubrimiento;
import gestorP2P.servicios.ServicioGestionRed;
import servicio.p2p.IServicioP2PControl;

import java.util.Collections;
import java.util.List;

/**
 * Capa de Aplicación limpia.
 * Ya no conoce 'IGestorConexiones'.
 */
public class ServicioP2P implements IServicioP2PControl {

    private final FachadaP2P fachada;
    private ServicioChat servicioChat;
    private boolean running;

    public ServicioP2P() {
        this.running = false;
        this.fachada = new FachadaP2P();
        configurarServicios();
    }

    private void configurarServicios() {
        // AHORA: Instanciamos servicios con constructores vacíos (o con config propia).
        // La Fachada les inyectará el GestorConexiones internamente al registrarlos.

        // A. Gestión de Red
        ServicioGestionRed srvRed = new ServicioGestionRed();
        fachada.registrarServicio(srvRed);

        // B. Chat
        this.servicioChat = new ServicioChat();
        fachada.registrarServicio(servicioChat);

        // C. Descubrimiento
        ServicioDescubrimiento srvDiscovery = new ServicioDescubrimiento();
        fachada.registrarServicio(srvDiscovery);
    }

    @Override
    public void iniciarRed() {
        if (running) return;
        try {
            fachada.iniciar();
            running = true;
        } catch (Exception e) {
            System.err.println("[ServicioP2P] Error crítico: " + e.getMessage());
        }
    }

    @Override
    public void detenerRed() {
        if (!running) return;
        fachada.detener();
        running = false;
    }

    @Override
    public List<DTOPeerDetails> obtenerListaPeers() {
        if (!running) return Collections.emptyList();
        return fachada.obtenerPeersConectados();
    }

    @Override
    public void enviarMensajeGlobal(String usuario, String mensaje) {
        if (!running) return;
        servicioChat.enviarMensajePublico(usuario, mensaje);
    }

    @Override
    public void enviarMensajePrivado(String idPeerDestino, String usuario, String mensaje) {
        if (!running) return;
        servicioChat.enviarMensajePrivado(idPeerDestino, usuario, mensaje);
    }

    @Override
    public void conectarManual(String ip, int puerto) {
        if (!running) return;
        fachada.conectarAPeer(ip, puerto);
    }

    @Override
    public boolean estaCorriendo() {
        return running;
    }
}