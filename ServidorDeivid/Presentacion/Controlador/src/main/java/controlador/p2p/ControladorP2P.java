package controlador.p2p;

import dto.p2p.DTOPeerDetails;
import servicio.p2p.ServicioP2P;
// CORRECCIÓN: Importar desde el paquete correcto
import servicio.p2p.IServicioP2PControl;

import java.util.List;

/**
 * Controlador P2P Puro.
 * Actúa como puente entre la Vista y el Servicio.
 */
public class ControladorP2P {

    private final IServicioP2PControl servicio;

    public ControladorP2P() {
        this.servicio = new ServicioP2P();
    }

    public ControladorP2P(IServicioP2PControl servicio) {
        this.servicio = servicio;
    }

    public void iniciarRed() {
        servicio.iniciarRed();
    }

    public void detenerRed() {
        servicio.detenerRed();
    }

    public void enviarMensajeChat(String mensaje) {
        servicio.enviarMensajeGlobal("UsuarioLocal", mensaje);
    }

    public void enviarMensajePrivado(String idPeer, String mensaje) {
        servicio.enviarMensajePrivado(idPeer, "UsuarioLocal", mensaje);
    }

    public List<DTOPeerDetails> obtenerListaPeers() {
        return servicio.obtenerListaPeers();
    }

    public void conectarManual(String ip, int puerto) {
        servicio.conectarManual(ip, puerto);
    }

    public boolean isRedIniciada() {
        return servicio.estaCorriendo();
    }
}