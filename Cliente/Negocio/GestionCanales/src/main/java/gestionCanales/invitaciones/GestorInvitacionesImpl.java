package gestionCanales.invitaciones;

import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.canales.DTOCanalCreado;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;
import repositorio.canal.IRepositorioCanal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del gestor de invitaciones a canales.
 * Maneja la solicitud de invitaciones pendientes y la respuesta a las mismas.
 */
public class GestorInvitacionesImpl implements IGestorInvitaciones {

    private final GestorSesionUsuario gestorSesion;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IRepositorioCanal repositorioCanal;
    private final List<IObservador> observadores;

    public GestorInvitacionesImpl(IRepositorioCanal repositorioCanal) {
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.repositorioCanal = repositorioCanal;
        this.observadores = new ArrayList<>();
        
        // Inicializar manejadores de notificaciones push
        inicializarManejadoresNotificaciones();
        
        System.out.println("✅ [GestorInvitaciones]: Inicializado");
    }

    private void inicializarManejadoresNotificaciones() {
        // Manejador para notificaciones push de nuevas invitaciones
        gestorRespuesta.registrarManejador("notificacionInvitacionCanal", this::manejarNuevaInvitacion);
        System.out.println("🔔 [GestorInvitaciones]: Manejadores de notificaciones push registrados");
    }

    private void manejarNuevaInvitacion(DTOResponse respuesta) {
        System.out.println("🔔 [GestorInvitaciones]: Nueva invitación recibida por PUSH");
        
        try {
            Object data = respuesta.getData();
            
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> invitacionData = (Map<String, Object>) data;
                
                String channelId = invitacionData.get("channelId") != null ? 
                    invitacionData.get("channelId").toString() : null;
                String channelName = invitacionData.get("channelName") != null ? 
                    invitacionData.get("channelName").toString() : null;
                String inviterName = invitacionData.get("inviterName") != null ? 
                    invitacionData.get("inviterName").toString() : null;
                
                System.out.println("   → Canal: " + channelName);
                System.out.println("   → Invitado por: " + inviterName);
                
                // Convertir a mapa simple para los observadores
                Map<String, String> notificationData = new HashMap<>();
                notificationData.put("channelId", channelId);
                notificationData.put("channelName", channelName);
                notificationData.put("inviterName", inviterName);
                
                // Notificar a la UI
                notificarObservadores("NUEVA_INVITACION_CANAL", notificationData);
            }
            
        } catch (Exception e) {
            System.err.println("❌ [GestorInvitaciones]: Error procesando nueva invitación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<List<DTOCanalCreado>> solicitarInvitacionesPendientes() {
        CompletableFuture<List<DTOCanalCreado>> future = new CompletableFuture<>();
        
        String userId = gestorSesion.getUserId();
        if (userId == null) {
            future.completeExceptionally(new IllegalStateException("Usuario no autenticado"));
            return future;
        }

        System.out.println("📨 [GestorInvitaciones]: Solicitando invitaciones pendientes para usuario: " + userId);

        // Crear payload
        Map<String, String> payload = new HashMap<>();
        payload.put("usuarioId", userId);
        
        DTORequest request = new DTORequest("obtenerInvitaciones", payload);

        // Registrar manejador de respuesta
        gestorRespuesta.registrarManejador("obtenerInvitaciones", (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                try {
                    Object data = respuesta.getData();
                    List<DTOCanalCreado> invitaciones = new ArrayList<>();
                    
                    if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) data;
                        Object invitacionesList = dataMap.get("invitaciones");
                        
                        if (invitacionesList instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> listaInvitaciones = (List<Map<String, Object>>) invitacionesList;
                            
                            for (Map<String, Object> invitacionMap : listaInvitaciones) {
                                String id = invitacionMap.get("channelId") != null ? 
                                    invitacionMap.get("channelId").toString() : null;
                                String nombre = invitacionMap.get("channelName") != null ? 
                                    invitacionMap.get("channelName").toString() : null;
                                String tipo = invitacionMap.get("channelType") != null ? 
                                    invitacionMap.get("channelType").toString() : "GRUPO";
                                
                                if (id != null && nombre != null) {
                                    DTOCanalCreado dtoCanal = new DTOCanalCreado(id, nombre);
                                    dtoCanal.setTipo(tipo);
                                    
                                    // Agregar información del owner si existe
                                    if (invitacionMap.get("owner") instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> ownerMap = (Map<String, Object>) invitacionMap.get("owner");
                                        String ownerId = ownerMap.get("userId") != null ? 
                                            ownerMap.get("userId").toString() : null;
                                        String ownerUsername = ownerMap.get("username") != null ? 
                                            ownerMap.get("username").toString() : null;
                                        
                                        if (ownerId != null && ownerUsername != null) {
                                            dto.featureContactos.DTOContacto owner = 
                                                new dto.featureContactos.DTOContacto(ownerId, ownerUsername, "", "", "");
                                            dtoCanal.setOwner(owner);
                                        }
                                    }
                                    
                                    invitaciones.add(dtoCanal);
                                }
                            }
                        }
                    }
                    
                    System.out.println("✅ [GestorInvitaciones]: " + invitaciones.size() + " invitaciones recibidas");
                    
                    // Notificar a observadores
                    notificarObservadores("INVITACIONES_PENDIENTES", invitaciones);
                    
                    future.complete(invitaciones);
                    
                } catch (Exception e) {
                    System.err.println("❌ [GestorInvitaciones]: Error procesando invitaciones: " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            } else {
                String error = "Error al obtener invitaciones: " + respuesta.getMessage();
                System.err.println("❌ [GestorInvitaciones]: " + error);
                future.completeExceptionally(new RuntimeException(error));
            }
        });

        // Enviar petición
        enviadorPeticiones.enviar(request);
        
        return future;
    }

    @Override
    public CompletableFuture<Void> responderInvitacion(String canalId, boolean aceptar) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        String userId = gestorSesion.getUserId();
        if (userId == null) {
            future.completeExceptionally(new IllegalStateException("Usuario no autenticado"));
            return future;
        }

        System.out.println((aceptar ? "✓" : "✗") + " [GestorInvitaciones]: Respondiendo invitación - Canal: " + canalId);

        // Crear payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", canalId);
        payload.put("accepted", aceptar);
        
        DTORequest request = new DTORequest("responderInvitacion", payload);

        // Registrar manejador de respuesta
        gestorRespuesta.registrarManejador("responderInvitacion", (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                System.out.println("✅ [GestorInvitaciones]: Invitación " + (aceptar ? "aceptada" : "rechazada"));
                
                // Notificar a observadores
                if (aceptar) {
                    notificarObservadores("INVITACION_ACEPTADA", canalId);
                } else {
                    notificarObservadores("INVITACION_RECHAZADA", canalId);
                }
                
                future.complete(null);
            } else {
                String error = "Error al responder invitación: " + respuesta.getMessage();
                System.err.println("❌ [GestorInvitaciones]: " + error);
                notificarObservadores("ERROR_RESPUESTA_INVITACION", error);
                future.completeExceptionally(new RuntimeException(error));
            }
        });

        // Enviar petición
        enviadorPeticiones.enviar(request);
        
        return future;
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("🔔 [GestorInvitaciones]: Observador registrado. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🔕 [GestorInvitaciones]: Observador removido. Total: " + observadores.size());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📢 [GestorInvitaciones]: Notificando a " + observadores.size() + " observadores - Tipo: " + tipoDeDato);
        
        // Crear copia para evitar ConcurrentModificationException
        for (IObservador observador : new ArrayList<>(observadores)) {
            try {
                observador.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                System.err.println("⚠️ [GestorInvitaciones]: Error al notificar observador: " + e.getMessage());
            }
        }
    }
}

