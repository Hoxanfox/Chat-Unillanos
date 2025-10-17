package controlador.notificaciones;

import dto.vistaContactoChat.DTOMensaje;
import observador.IObservador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor global de notificaciones de mensajes.
 * Mantiene el contador de mensajes no leídos por contacto.
 * 
 * Este gestor se suscribe al sistema de mensajería global y notifica
 * a las vistas (ej: lista de contactos) sobre nuevos mensajes.
 */
public class GestorNotificacionesMensajes implements IObservador {
    
    private static GestorNotificacionesMensajes instancia;
    
    // Contador de mensajes no leídos por contacto
    private final Map<String, Integer> mensajesNoLeidos = new HashMap<>();
    
    // Observadores que escuchan las notificaciones (ej: lista de contactos)
    private final List<IObservador> observadores = new ArrayList<>();
    
    // ID del chat actualmente abierto (para no contar esos mensajes)
    private String chatActivo = null;
    
    private GestorNotificacionesMensajes() {
        System.out.println("🔔 [GestorNotificacionesMensajes]: Inicializado");
    }
    
    public static synchronized GestorNotificacionesMensajes getInstancia() {
        if (instancia == null) {
            instancia = new GestorNotificacionesMensajes();
        }
        return instancia;
    }
    
    /**
     * Establece cuál es el chat actualmente abierto.
     * Los mensajes de este contacto NO incrementarán el contador.
     */
    public void setChatActivo(String contactoId) {
        System.out.println("📱 [GestorNotificacionesMensajes]: Chat activo: " + contactoId);
        this.chatActivo = contactoId;
        
        // Limpiar el contador de este contacto
        if (contactoId != null && mensajesNoLeidos.containsKey(contactoId)) {
            mensajesNoLeidos.put(contactoId, 0);
            notificarObservadores("BADGE_ACTUALIZADO", contactoId);
        }
    }
    
    /**
     * Limpia el chat activo (cuando se cierra un chat).
     */
    public void limpiarChatActivo() {
        System.out.println("📱 [GestorNotificacionesMensajes]: Chat activo limpiado");
        this.chatActivo = null;
    }
    
    /**
     * Obtiene el número de mensajes no leídos para un contacto.
     */
    public int getMensajesNoLeidos(String contactoId) {
        return mensajesNoLeidos.getOrDefault(contactoId, 0);
    }
    
    /**
     * Marca todos los mensajes de un contacto como leídos.
     */
    public void marcarComoLeido(String contactoId) {
        System.out.println("✅ [GestorNotificacionesMensajes]: Marcando mensajes como leídos - Contacto: " + contactoId);
        mensajesNoLeidos.put(contactoId, 0);
        notificarObservadores("BADGE_ACTUALIZADO", contactoId);
    }
    
    /**
     * Registra un observador (ej: lista de contactos).
     */
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("🔔 [GestorNotificacionesMensajes]: Observador registrado - Total: " + observadores.size());
        }
    }
    
    /**
     * Remueve un observador.
     */
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🔕 [GestorNotificacionesMensajes]: Observador removido - Total: " + observadores.size());
    }
    
    @Override
    public void actualizar(String tipo, Object datos) {
        // Este método recibe notificaciones del sistema de mensajería global
        
        if ("NUEVO_MENSAJE_PRIVADO".equals(tipo) && datos instanceof DTOMensaje) {
            DTOMensaje mensaje = (DTOMensaje) datos;
            
            // Si el mensaje NO es mío (yo no lo envié)
            if (!mensaje.esMio()) {
                String contactoId = mensaje.getRemitenteId();
                
                // Si el chat NO está activo o es de otro contacto
                if (chatActivo == null || !chatActivo.equals(contactoId)) {
                    // Incrementar contador
                    int count = mensajesNoLeidos.getOrDefault(contactoId, 0);
                    mensajesNoLeidos.put(contactoId, count + 1);
                    
                    System.out.println("🔔 [GestorNotificacionesMensajes]: Nuevo mensaje no leído");
                    System.out.println("   → De: " + mensaje.getRemitenteNombre());
                    System.out.println("   → Contacto: " + contactoId);
                    System.out.println("   → Total no leídos: " + (count + 1));
                    
                    // Notificar a los observadores para actualizar badges
                    Map<String, Object> datosNotificacion = new HashMap<>();
                    datosNotificacion.put("contactoId", contactoId);
                    datosNotificacion.put("count", count + 1);
                    datosNotificacion.put("mensaje", mensaje);
                    
                    notificarObservadores("NUEVO_MENSAJE_NO_LEIDO", datosNotificacion);
                }
            }
        }
    }
    
    /**
     * Notifica a todos los observadores registrados.
     */
    private void notificarObservadores(String tipo, Object datos) {
        System.out.println("📢 [GestorNotificacionesMensajes]: Notificando a " + observadores.size() + " observadores - Tipo: " + tipo);
        for (IObservador obs : observadores) {
            obs.actualizar(tipo, datos);
        }
    }
    
    /**
     * Obtiene el total de mensajes no leídos de todos los contactos.
     */
    public int getTotalMensajesNoLeidos() {
        return mensajesNoLeidos.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Reinicia todos los contadores (útil al cerrar sesión).
     */
    public void reiniciar() {
        System.out.println("🔄 [GestorNotificacionesMensajes]: Reiniciando contadores");
        mensajesNoLeidos.clear();
        chatActivo = null;
        notificarObservadores("CONTADORES_REINICIADOS", null);
    }
}

