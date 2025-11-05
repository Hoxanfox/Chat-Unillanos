package gestionContactos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton para gestionar el mapeo entre contactos y sus peer IDs.
 * Esto permite saber el peerId de un contacto para enviarle mensajes.
 */
public class GestorContactoPeers {
    
    private static GestorContactoPeers instancia;
    private final Map<String, String> contactoIdToPeerId = new ConcurrentHashMap<>();
    
    private GestorContactoPeers() {}
    
    public static synchronized GestorContactoPeers getInstancia() {
        if (instancia == null) {
            instancia = new GestorContactoPeers();
        }
        return instancia;
    }
    
    /**
     * Registra el peerId de un contacto.
     * @param contactoId El ID del usuario contacto
     * @param peerId El ID del peer WebRTC del contacto
     */
    public void registrarPeerDeContacto(String contactoId, String peerId) {
        if (contactoId != null && peerId != null) {
            contactoIdToPeerId.put(contactoId, peerId);
            System.out.println("🔗 [GestorContactoPeers]: Registrado peer para contacto " + contactoId + " -> " + peerId);
        }
    }
    
    /**
     * Obtiene el peerId de un contacto.
     * @param contactoId El ID del usuario contacto
     * @return El peerId del contacto o null si no está registrado
     */
    public String getPeerIdDeContacto(String contactoId) {
        String peerId = contactoIdToPeerId.get(contactoId);
        if (peerId == null) {
            System.out.println("⚠️ [GestorContactoPeers]: No se encontró peerId para contacto " + contactoId);
        }
        return peerId;
    }
    
    /**
     * Verifica si un contacto tiene un peerId registrado.
     */
    public boolean tienePeerIdRegistrado(String contactoId) {
        return contactoIdToPeerId.containsKey(contactoId);
    }
    
    /**
     * Elimina el registro de un contacto.
     */
    public void removerContacto(String contactoId) {
        contactoIdToPeerId.remove(contactoId);
        System.out.println("🗑️ [GestorContactoPeers]: Removido peer de contacto " + contactoId);
    }
    
    /**
     * Limpia todos los registros (útil al cerrar sesión).
     */
    public void limpiar() {
        contactoIdToPeerId.clear();
        System.out.println("🧹 [GestorContactoPeers]: Limpiados todos los registros de peers");
    }
}

