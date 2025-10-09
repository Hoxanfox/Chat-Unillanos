package servidor;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import servidor.dto.DTOContacto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor centralizado de sesiones de usuarios conectados.
 * Mantiene un registro de canales activos y usuarios autenticados.
 */
public class GestorSesiones {
    
    private static GestorSesiones instancia;
    
    // Grupo de todos los canales conectados
    private final ChannelGroup canalesActivos;
    
    // Mapa de canal -> nombre de usuario
    private final Map<Channel, String> usuariosPorCanal;
    
    // Mapa de nombre de usuario -> canal
    private final Map<String, Channel> canalesPorUsuario;
    
    private GestorSesiones() {
        this.canalesActivos = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.usuariosPorCanal = new ConcurrentHashMap<>();
        this.canalesPorUsuario = new ConcurrentHashMap<>();
    }
    
    public static synchronized GestorSesiones getInstancia() {
        if (instancia == null) {
            instancia = new GestorSesiones();
        }
        return instancia;
    }
    
    /**
     * Registra un nuevo canal conectado.
     */
    public void registrarCanal(Channel canal) {
        canalesActivos.add(canal);
        System.out.println("✓ Nuevo cliente conectado. Total de conexiones: " + canalesActivos.size());
    }
    
    /**
     * Asocia un canal con un usuario autenticado.
     */
    public void autenticarUsuario(Channel canal, String nombreUsuario) {
        usuariosPorCanal.put(canal, nombreUsuario);
        canalesPorUsuario.put(nombreUsuario, canal);
        System.out.println("✓ Usuario autenticado: " + nombreUsuario);
    }
    
    /**
     * Remueve un canal y su usuario asociado.
     */
    public void removerCanal(Channel canal) {
        String usuario = usuariosPorCanal.remove(canal);
        if (usuario != null) {
            canalesPorUsuario.remove(usuario);
            System.out.println("✗ Usuario desconectado: " + usuario);
        }
        canalesActivos.remove(canal);
        System.out.println("✗ Cliente desconectado. Total de conexiones: " + canalesActivos.size());
    }
    
    /**
     * Obtiene el nombre de usuario asociado a un canal.
     */
    public String obtenerUsuario(Channel canal) {
        return usuariosPorCanal.get(canal);
    }
    
    /**
     * Obtiene el canal asociado a un usuario.
     */
    public Channel obtenerCanal(String nombreUsuario) {
        return canalesPorUsuario.get(nombreUsuario);
    }
    
    /**
     * Obtiene la lista de contactos (usuarios conectados).
     */
    public List<DTOContacto> obtenerContactos() {
        List<DTOContacto> contactos = new ArrayList<>();
        for (String usuario : canalesPorUsuario.keySet()) {
            contactos.add(new DTOContacto(usuario, "Online"));
        }
        return contactos;
    }
    
    /**
     * Envía un mensaje a todos los canales conectados.
     */
    public void difundirATodos(String mensaje) {
        canalesActivos.writeAndFlush(mensaje + "\n");
    }
    
    /**
     * Envía un mensaje a todos los usuarios autenticados.
     */
    public void difundirAAutenticados(String mensaje) {
        for (Channel canal : usuariosPorCanal.keySet()) {
            canal.writeAndFlush(mensaje + "\n");
        }
    }
    
    public int getTotalConexiones() {
        return canalesActivos.size();
    }
    
    public int getTotalUsuariosAutenticados() {
        return usuariosPorCanal.size();
    }
}

