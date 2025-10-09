package servidor;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import servidor.dto.*;

import java.util.List;

/**
 * Manejador de mensajes del cliente usando Netty.
 * Procesa cada petición JSON y envía la respuesta correspondiente.
 */
public class ManejadorCliente extends SimpleChannelInboundHandler<String> {
    
    private final Gson gson;
    private final GestorSesiones gestorSesiones;
    
    public ManejadorCliente() {
        this.gson = new Gson();
        this.gestorSesiones = GestorSesiones.getInstancia();
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Cuando un cliente se conecta
        gestorSesiones.registrarCanal(ctx.channel());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Cuando un cliente se desconecta
        gestorSesiones.removerCanal(ctx.channel());
        
        // Notificar a todos los demás clientes que la lista de contactos cambió
        notificarActualizacionContactos();
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String mensajeJson) throws Exception {
        System.out.println("📩 Mensaje recibido: " + mensajeJson);
        
        try {
            DTORequest peticion = gson.fromJson(mensajeJson, DTORequest.class);
            
            if (peticion == null || peticion.getAction() == null) {
                enviarError(ctx.channel(), "unknown", "Petición inválida");
                return;
            }
            
            procesarPeticion(ctx.channel(), peticion);
            
        } catch (JsonSyntaxException e) {
            System.err.println("❌ Error al parsear JSON: " + e.getMessage());
            enviarError(ctx.channel(), "unknown", "JSON inválido");
        }
    }
    
    /**
     * Procesa la petición según la acción solicitada.
     */
    private void procesarPeticion(Channel canal, DTORequest peticion) {
        String action = peticion.getAction();
        
        switch (action) {
            case "authenticateUser":
                manejarAutenticacion(canal, peticion);
                break;
                
            case "solicitarListaContactos":
                manejarSolicitudContactos(canal);
                break;
                
            case "enviarMensaje":
                manejarEnvioMensaje(canal, peticion);
                break;
                
            default:
                enviarError(canal, action, "Acción no implementada: " + action);
        }
    }
    
    /**
     * Maneja la autenticación de un usuario.
     * Por ahora, acepta cualquier email/password para pruebas.
     */
    private void manejarAutenticacion(Channel canal, DTORequest peticion) {
        try {
            DTOAutenticacion datosAuth = gson.fromJson(peticion.getPayload(), DTOAutenticacion.class);
            
            if (datosAuth == null || datosAuth.getEmailUsuario() == null) {
                enviarError(canal, "authenticateUser", "Datos de autenticación inválidos");
                return;
            }
            
            String email = datosAuth.getEmailUsuario();
            String password = datosAuth.getPasswordUsuario();
            
            // Validación simple para pruebas: acepta cualquier email no vacío
            if (email.isEmpty()) {
                DTOResponse respuesta = new DTOResponse(
                    "authenticateUser",
                    "error",
                    "Email no puede estar vacío",
                    null
                );
                enviarRespuesta(canal, respuesta);
                return;
            }
            
            // Extraer el nombre del usuario del email (parte antes del @)
            String nombreUsuario = email.contains("@") ? email.split("@")[0] : email;
            
            // Registrar el usuario como autenticado
            gestorSesiones.autenticarUsuario(canal, nombreUsuario);
            
            // Enviar respuesta exitosa
            DTOResponse respuesta = new DTOResponse(
                "authenticateUser",
                "success",
                "Autenticación exitosa",
                nombreUsuario
            );
            enviarRespuesta(canal, respuesta);
            
            System.out.println("✓ Usuario autenticado: " + nombreUsuario);
            
            // Notificar a todos los clientes que hay un nuevo usuario en línea
            notificarActualizacionContactos();
            
        } catch (Exception e) {
            System.err.println("❌ Error en autenticación: " + e.getMessage());
            enviarError(canal, "authenticateUser", "Error al procesar autenticación");
        }
    }
    
    /**
     * Maneja la solicitud de lista de contactos.
     */
    private void manejarSolicitudContactos(Channel canal) {
        List<DTOContacto> contactos = gestorSesiones.obtenerContactos();
        
        DTOResponse respuesta = new DTOResponse(
            "actualizarListaContactos",
            "success",
            "Lista de contactos actualizada",
            contactos
        );
        
        enviarRespuesta(canal, respuesta);
        System.out.println("✓ Lista de contactos enviada: " + contactos.size() + " usuarios");
    }
    
    /**
     * Maneja el envío de un mensaje de chat.
     * (Funcionalidad básica para pruebas futuras)
     */
    private void manejarEnvioMensaje(Channel canal, DTORequest peticion) {
        // Implementación básica - puede expandirse después
        DTOResponse respuesta = new DTOResponse(
            "enviarMensaje",
            "success",
            "Mensaje enviado",
            null
        );
        enviarRespuesta(canal, respuesta);
    }
    
    /**
     * Notifica a todos los usuarios autenticados que la lista de contactos ha cambiado.
     */
    private void notificarActualizacionContactos() {
        List<DTOContacto> contactos = gestorSesiones.obtenerContactos();
        
        DTOResponse respuesta = new DTOResponse(
            "actualizarListaContactos",
            "success",
            "Lista de contactos actualizada",
            contactos
        );
        
        String mensaje = gson.toJson(respuesta);
        gestorSesiones.difundirAAutenticados(mensaje);
        
        System.out.println("📢 Actualización de contactos difundida a todos los usuarios");
    }
    
    /**
     * Envía una respuesta al canal.
     */
    private void enviarRespuesta(Channel canal, DTOResponse respuesta) {
        String json = gson.toJson(respuesta);
        canal.writeAndFlush(json + "\n");
        System.out.println("📤 Respuesta enviada: " + json);
    }
    
    /**
     * Envía un mensaje de error al canal.
     */
    private void enviarError(Channel canal, String action, String mensaje) {
        DTOResponse respuesta = new DTOResponse(action, "error", mensaje, null);
        enviarRespuesta(canal, respuesta);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("❌ Error en el canal: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}

