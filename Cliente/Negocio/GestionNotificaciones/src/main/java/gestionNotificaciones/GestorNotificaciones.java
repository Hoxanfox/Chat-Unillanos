package gestionNotificaciones;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.featureNotificaciones.DTONotificacion;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;
import observador.ISujeto;
import repositorio.notificacion.IRepositorioNotificacion;
import repositorio.notificacion.RepositorioNotificacionImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Gestor de notificaciones del sistema.
 * Maneja la lógica de negocio relacionada con las notificaciones.
 *
 * <p>Este gestor se encarga de:
 * <ul>
 * <li>Comunicación con el servidor para solicitar/responder notificaciones</li>
 * <li>Usar el repositorio para almacenamiento en caché local</li>
 * <li>Implementar el patrón Observer para notificar cambios</li>
 * <li>Aceptar/rechazar solicitudes de amistad e invitaciones a canales</li>
 * <li>Registrarse en el GestorSincronizacionGlobal para actualizaciones automáticas</li>
 * </ul>
 */
public class GestorNotificaciones implements ISujeto, IObservador {

    private final IRepositorioNotificacion repositorioNotificacion;
    private final GestorSesionUsuario gestorSesion;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final Gson gson;
    private final List<IObservador> observadores;

    public GestorNotificaciones() {
        this.repositorioNotificacion = RepositorioNotificacionImpl.getInstancia();
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gson = new Gson();
        this.observadores = new ArrayList<>();

        System.err.println("✅ [GestorNotificaciones]: Gestor inicializado con comunicación y repositorio SINGLETON");

        // 🔥 NUEVO: Registrarse en el GestorSincronizacionGlobal para recibir ACTUALIZAR_NOTIFICACIONES
        GestorSincronizacionGlobal.getInstancia().registrarObservador(this);
        System.err.println("✅ [GestorNotificaciones]: Registrado en GestorSincronizacionGlobal para actualizaciones automáticas");

        // NOTA: El registro como observador de GestorInvitaciones se hace desde FachadaNotificacionesImpl
        // para evitar dependencia circular
    }

    /**
     * Obtiene la lista de notificaciones del usuario actual desde el servidor.
     */
    public CompletableFuture<List<DTONotificacion>> obtenerNotificaciones() {
        System.out.println("📨 [GestorNotificaciones]: Solicitando notificaciones...");

        CompletableFuture<List<DTONotificacion>> future = new CompletableFuture<>();

        // ✅ SOLUCIÓN: Primero obtener del caché local
        List<DTONotificacion> notificacionesCache = repositorioNotificacion.obtenerTodas();
        System.out.println("📦 [GestorNotificaciones]: Encontradas " + notificacionesCache.size() + " notificaciones en caché");

        // Si hay notificaciones en caché, devolverlas inmediatamente
        if (!notificacionesCache.isEmpty()) {
            System.out.println("✅ [GestorNotificaciones]: Devolviendo notificaciones del caché");
            notificarObservadores("NOTIFICACIONES_RECIBIDAS", notificacionesCache);
            future.complete(notificacionesCache);
            return future;
        }

        // Si no hay en caché, devolver lista vacía (el servidor no soporta esta acción)
        System.out.println("⚠️ [GestorNotificaciones]: La acción 'obtenerNotificaciones' no está implementada en el servidor");
        System.out.println("📋 [GestorNotificaciones]: Devolviendo lista vacía de notificaciones");

        // Notificar con lista vacía para que la UI se actualice correctamente
        notificarObservadores("NOTIFICACIONES_RECIBIDAS", new ArrayList<>());
        future.complete(new ArrayList<>());

        return future;
    }

    /**
     * Obtiene notificaciones desde la caché local (sin consultar servidor).
     */
    public List<DTONotificacion> obtenerNotificacionesCache() {
        return repositorioNotificacion.obtenerTodas();
    }

    /**
     * Marca una notificación específica como leída.
     */
    public CompletableFuture<Void> marcarComoLeida(String notificacionId) {
        System.out.println("📝 [GestorNotificaciones]: Marcando notificación como leída: " + notificacionId);

        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject payload = new JsonObject();
        payload.addProperty("notificacionId", notificacionId);
        DTORequest request = new DTORequest("marcarNotificacionLeida", payload);

        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                System.out.println("✅ [GestorNotificaciones]: Notificación marcada como leída");

                // Remover de caché local
                repositorioNotificacion.remover(notificacionId);

                future.complete(null);
            } else {
                String error = "Error al marcar notificación: " + respuesta.getMessage();
                System.err.println("❌ [GestorNotificaciones]: " + error);
                future.completeExceptionally(new RuntimeException(error));
            }
        });

        enviadorPeticiones.enviar(request);
        return future;
    }

    /**
     * Marca todas las notificaciones como leídas.
     */
    public CompletableFuture<Void> marcarTodasComoLeidas() {
        System.out.println("📝 [GestorNotificaciones]: Marcando todas las notificaciones como leídas");

        CompletableFuture<Void> future = new CompletableFuture<>();
        String usuarioId = gestorSesion.getUserId();

        JsonObject payload = new JsonObject();
        payload.addProperty("usuarioId", usuarioId);
        DTORequest request = new DTORequest("marcarTodasNotificacionesLeidas", payload);

        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                System.out.println("✅ [GestorNotificaciones]: Todas las notificaciones marcadas");

                // Limpiar caché local
                repositorioNotificacion.limpiarCache();

                notificarObservadores("TODAS_NOTIFICACIONES_LEIDAS", null);
                future.complete(null);
            } else {
                String error = "Error: " + respuesta.getMessage();
                System.err.println("❌ [GestorNotificaciones]: " + error);
                future.completeExceptionally(new RuntimeException(error));
            }
        });

        enviadorPeticiones.enviar(request);
        return future;
    }

    /**
     * Acepta una solicitud de amistad.
     */
    public CompletableFuture<Void> aceptarSolicitudAmistad(String solicitudId, String usuarioOrigenId) {
        System.out.println("✅ [GestorNotificaciones]: Aceptando solicitud de amistad");

        if (solicitudId == null || solicitudId.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("ID de solicitud inválido")
            );
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        String usuarioId = gestorSesion.getUserId();

        JsonObject payload = new JsonObject();
        payload.addProperty("solicitudId", solicitudId);
        payload.addProperty("usuarioId", usuarioId);
        payload.addProperty("usuarioOrigenId", usuarioOrigenId);
        payload.addProperty("accion", "ACEPTAR");

        DTORequest request = new DTORequest("responderSolicitudAmistad", payload);

        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                System.out.println("✅ [GestorNotificaciones]: Solicitud de amistad aceptada exitosamente");

                // Remover de caché
                repositorioNotificacion.remover(solicitudId);

                notificarObservadores("SOLICITUD_AMISTAD_ACEPTADA", solicitudId);
                notificarObservadores("CONTACTO_AGREGADO", usuarioOrigenId);
                future.complete(null);
            } else {
                String error = "Error al aceptar: " + respuesta.getMessage();
                System.err.println("❌ [GestorNotificaciones]: " + error);
                future.completeExceptionally(new RuntimeException(error));
            }
        });

        enviadorPeticiones.enviar(request);
        return future;
    }

    /**
     * Rechaza una solicitud de amistad.
     */
    public CompletableFuture<Void> rechazarSolicitudAmistad(String solicitudId) {
        System.out.println("❌ [GestorNotificaciones]: Rechazando solicitud de amistad");

        if (solicitudId == null || solicitudId.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("ID de solicitud inválido")
            );
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        String usuarioId = gestorSesion.getUserId();

        JsonObject payload = new JsonObject();
        payload.addProperty("solicitudId", solicitudId);
        payload.addProperty("usuarioId", usuarioId);
        payload.addProperty("accion", "RECHAZAR");

        DTORequest request = new DTORequest("responderSolicitudAmistad", payload);

        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                System.out.println("✅ [GestorNotificaciones]: Solicitud rechazada");

                // Remover de caché
                repositorioNotificacion.remover(solicitudId);

                notificarObservadores("SOLICITUD_AMISTAD_RECHAZADA", solicitudId);
                future.complete(null);
            } else {
                String error = "Error al rechazar: " + respuesta.getMessage();
                System.err.println("❌ [GestorNotificaciones]: " + error);
                future.completeExceptionally(new RuntimeException(error));
            }
        });

        enviadorPeticiones.enviar(request);
        return future;
    }

    /**
     * Acepta una invitación a un canal.
     */
    public CompletableFuture<Void> aceptarInvitacionCanal(String invitacionId, String canalId) {
        System.out.println("✅ [GestorNotificaciones]: Aceptando invitación a canal");

        if (invitacionId == null || invitacionId.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("ID de invitación inválido")
            );
        }

        if (canalId == null || canalId.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("ID de canal inválido")
            );
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        String usuarioId = gestorSesion.getUserId();

        JsonObject payload = new JsonObject();
        payload.addProperty("channelId", canalId);
        payload.addProperty("accepted", true);

        // ✨ CORREGIDO: Cambiar de "responderInvitacionCanal" a "responderInvitacion"
        DTORequest request = new DTORequest("responderInvitacion", payload);

        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                System.out.println("✅ [GestorNotificaciones]: Invitación a canal aceptada exitosamente");

                // ✅ SOLUCIÓN: Eliminar del caché y solicitar actualización de invitaciones
                repositorioNotificacion.remover(invitacionId);
                System.out.println("🗑️ [GestorNotificaciones]: Notificación eliminada del caché: " + invitacionId);

                JsonObject data = new JsonObject();
                data.addProperty("invitacionId", invitacionId);
                data.addProperty("canalId", canalId);

                // Notificar eventos
                notificarObservadores("INVITACION_CANAL_ACEPTADA", data);
                notificarObservadores("CANAL_UNIDO", canalId);

                // ✅ CLAVE: Solicitar actualización de invitaciones al servidor
                System.out.println("🔄 [GestorNotificaciones]: Solicitando actualización de invitaciones pendientes...");
                GestorSincronizacionGlobal.getInstancia().notificarObservadores("ACTUALIZAR_INVITACIONES", null);

                future.complete(null);
            } else {
                String error = "Error al aceptar invitación: " + respuesta.getMessage();
                System.err.println("❌ [GestorNotificaciones]: " + error);
                future.completeExceptionally(new RuntimeException(error));
            }
        });

        enviadorPeticiones.enviar(request);
        return future;
    }

    /**
     * Rechaza una invitación a un canal.
     */
    public CompletableFuture<Void> rechazarInvitacionCanal(String invitacionId, String canalId) {
        System.out.println("❌ [GestorNotificaciones]: Rechazando invitación a canal");
        System.out.println("   → Notificación ID: " + invitacionId);
        System.out.println("   → Canal ID: " + canalId);

        if (canalId == null || canalId.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("ID de canal inválido")
            );
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        String usuarioId = gestorSesion.getUserId();

        JsonObject payload = new JsonObject();
        payload.addProperty("channelId", canalId);
        payload.addProperty("accepted", false);

        DTORequest request = new DTORequest("responderInvitacion", payload);

        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                System.out.println("✅ [GestorNotificaciones]: Invitación rechazada");

                // ✅ SOLUCIÓN: Eliminar del caché y solicitar actualización de invitaciones
                repositorioNotificacion.remover(invitacionId);
                System.out.println("🗑️ [GestorNotificaciones]: Notificación eliminada del caché: " + invitacionId);

                notificarObservadores("INVITACION_CANAL_RECHAZADA", canalId);

                // ✅ CLAVE: Solicitar actualización de invitaciones al servidor (igual que en aceptar)
                System.out.println("🔄 [GestorNotificaciones]: Solicitando actualización de invitaciones pendientes...");
                GestorSincronizacionGlobal.getInstancia().notificarObservadores("ACTUALIZAR_INVITACIONES", null);

                future.complete(null);
            } else {
                String error = "Error al rechazar: " + respuesta.getMessage();
                System.err.println("❌ [GestorNotificaciones]: " + error);
                future.completeExceptionally(new RuntimeException(error));
            }
        });

        enviadorPeticiones.enviar(request);
        return future;
    }

    /**
     * Inicializa los manejadores de mensajes entrantes en tiempo real.
     */
    public void inicializarManejadores() {
        System.out.println("🔧 [GestorNotificaciones]: Inicializando manejadores");
        gestorRespuesta.registrarManejador("nuevaNotificacion", this::manejarNuevaNotificacion);
        gestorRespuesta.registrarManejador("solicitudAceptada", this::manejarSolicitudAceptada);
        gestorRespuesta.registrarManejador("notificacionInvitacionCanal", this::manejarInvitacionCanal);
        System.out.println("✅ [GestorNotificaciones]: Manejadores inicializados");
    }

    // ==================== IMPLEMENTACIÓN DE IObservador ====================

    /**
     * Recibe actualizaciones del GestorSincronizacionGlobal.
     * Cuando llega SIGNAL_UPDATE del servidor, este método es invocado.
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.err.println("🔔🔔🔔 [GestorNotificaciones]: ============ ACTUALIZAR INVOCADO ============");
        System.err.println("🔔 [GestorNotificaciones]: Actualización recibida del GestorSincronizacionGlobal - Tipo: " + tipoDeDato);
        System.err.println("🔔 [GestorNotificaciones]: Datos recibidos: " + (datos != null ? datos.getClass().getName() : "null"));

        if ("ACTUALIZAR_NOTIFICACIONES".equals(tipoDeDato)) {
            System.err.println("📡📡📡 [GestorNotificaciones]: Refrescando notificaciones desde el caché local...");
            System.err.println("📡 [GestorNotificaciones]: Consultando repositorioNotificacion.obtenerTodas()...");

            // Obtener todas las notificaciones del caché y notificar a los observadores (UI)
            List<DTONotificacion> notificacionesCache = repositorioNotificacion.obtenerTodas();
            System.err.println("📦📦📦 [GestorNotificaciones]: " + notificacionesCache.size() + " notificaciones encontradas en caché");

            if (notificacionesCache.isEmpty()) {
                System.err.println("⚠️⚠️⚠️ [GestorNotificaciones]: CACHÉ VACÍO - El repositorio no tiene notificaciones guardadas");
            } else {
                System.err.println("✅✅✅ [GestorNotificaciones]: Notificaciones en caché:");
                for (int i = 0; i < notificacionesCache.size(); i++) {
                    DTONotificacion n = notificacionesCache.get(i);
                    System.err.println("   [" + (i+1) + "] ID: " + n.getId() + ", Tipo: " + n.getTipo() + ", Título: " + n.getTitulo());
                }
            }

            System.err.println("📢 [GestorNotificaciones]: Notificando a " + observadores.size() + " observadores - Tipo: ACTUALIZAR_NOTIFICACIONES");
            // Notificar a los observadores registrados (ServicioNotificaciones -> UI)
            notificarObservadores("ACTUALIZAR_NOTIFICACIONES", notificacionesCache);
            System.err.println("🔔🔔🔔 [GestorNotificaciones]: ============ ACTUALIZAR FINALIZADO ============");
        } else if ("INVITACIONES_PENDIENTES".equals(tipoDeDato)) {
            System.err.println("📩📩📩 [GestorNotificaciones]: Recibido evento INVITACIONES_PENDIENTES");
            System.err.println("📩 [GestorNotificaciones]: Datos: " + datos);
            // Este evento viene del GestorInvitaciones pero NO debería procesar aquí
            // porque GestorInvitaciones ya guarda las notificaciones en el repositorio
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private void manejarInvitacionCanal(DTOResponse respuesta) {
        System.out.println("🔔 [GestorNotificaciones]: Nueva invitación a canal recibida por PUSH");

        try {
            Object data = respuesta.getData();

            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> invitacionData = (Map<String, Object>) data;

                String channelId = invitacionData.get("channelId") != null ?
                        invitacionData.get("channelId").toString() : null;
                String channelName = invitacionData.get("channelName") != null ?
                        invitacionData.get("channelName").toString() : null;

                // Extraer información del invitador (owner)
                String inviterName = null;
                if (invitacionData.get("owner") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ownerMap = (Map<String, Object>) invitacionData.get("owner");
                    inviterName = ownerMap.get("username") != null ?
                            ownerMap.get("username").toString() : null;
                }

                System.out.println("   → Canal: " + channelName);
                System.out.println("   → Invitado por: " + inviterName);

                // Crear y guardar una DTONotificacion en el repositorio
                String notificacionId = java.util.UUID.randomUUID().toString();
                String titulo = "Invitación a canal";
                String contenido = inviterName != null ?
                        inviterName + " te ha invitado al canal '" + channelName + "'" :
                        "Has sido invitado al canal '" + channelName + "'";

                DTONotificacion notificacion = new DTONotificacion(
                        notificacionId,
                        "INVITACION_CANAL",
                        titulo,
                        contenido,
                        LocalDateTime.now(),
                        false,
                        channelId
                );

                // Guardar en repositorio
                repositorioNotificacion.guardar(notificacion);
                System.out.println("💾 [GestorNotificaciones]: Notificación de invitación guardada - ID: " + notificacionId);

                // Notificar a observadores
                notificarObservadores("NUEVA_NOTIFICACION", notificacion);
            }

        } catch (Exception e) {
            System.err.println("❌ [GestorNotificaciones]: Error procesando invitación a canal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<DTONotificacion> parsearNotificaciones(DTOResponse respuesta) {
        List<DTONotificacion> notificaciones = new ArrayList<>();

        if (respuesta.getData() != null) {
            JsonElement element = gson.toJsonTree(respuesta.getData());

            // El servidor envía un objeto con estructura: {"notificaciones":[], "totalNoLeidas":0, "totalNotificaciones":0}
            if (element.isJsonObject()) {
                JsonObject dataObj = element.getAsJsonObject();

                // Extraer el array de notificaciones del objeto
                if (dataObj.has("notificaciones") && dataObj.get("notificaciones").isJsonArray()) {
                    JsonArray array = dataObj.getAsJsonArray("notificaciones");
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                    for (JsonElement item : array) {
                        JsonObject obj = item.getAsJsonObject();

                        String id = obj.get("id").getAsString();
                        String tipo = obj.get("tipo").getAsString();
                        String titulo = obj.get("titulo").getAsString();
                        String contenido = obj.get("contenido").getAsString();
                        LocalDateTime fecha = LocalDateTime.parse(obj.get("fecha").getAsString(), formatter);
                        boolean leida = obj.get("leida").getAsBoolean();
                        String origenId = obj.get("origenId").getAsString();

                        DTONotificacion notif = new DTONotificacion(id, tipo, titulo, contenido, fecha, leida, origenId);
                        notificaciones.add(notif);
                    }

                    System.out.println("✅ [GestorNotificaciones]: " + notificaciones.size() + " notificaciones parseadas correctamente");
                } else {
                    System.out.println("ℹ️ [GestorNotificaciones]: No hay notificaciones en la respuesta");
                }
            }
        }

        return notificaciones;
    }

    private void manejarNuevaNotificacion(DTOResponse respuesta) {
        System.out.println("🔔 [GestorNotificaciones]: Nueva notificación en tiempo real");

        try {
            JsonElement element = gson.toJsonTree(respuesta.getData());
            JsonObject data = element.getAsJsonObject();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            String id = data.get("id").getAsString();
            String tipo = data.get("tipo").getAsString();
            String titulo = data.get("titulo").getAsString();
            String contenido = data.get("contenido").getAsString();
            LocalDateTime fecha = LocalDateTime.parse(data.get("fecha").getAsString(), formatter);
            boolean leida = false;
            String origenId = data.get("origenId").getAsString();

            DTONotificacion notificacion = new DTONotificacion(id, tipo, titulo, contenido, fecha, leida, origenId);

            // Guardar en repositorio (caché)
            repositorioNotificacion.guardar(notificacion);

            // Notificar a observadores
            notificarObservadores("NUEVA_NOTIFICACION", notificacion);
        } catch (Exception e) {
            System.err.println("❌ [GestorNotificaciones]: Error al procesar: " + e.getMessage());
        }
    }

    private void manejarSolicitudAceptada(DTOResponse respuesta) {
        System.out.println("✅ [GestorNotificaciones]: Una solicitud fue aceptada");

        try {
            JsonElement element = gson.toJsonTree(respuesta.getData());
            JsonObject data = element.getAsJsonObject();
            notificarObservadores("TU_SOLICITUD_ACEPTADA", data);
        } catch (Exception e) {
            System.err.println("❌ [GestorNotificaciones]: Error: " + e.getMessage());
        }
    }

    // ==================== PATRÓN OBSERVER ====================

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("🔔 [GestorNotificaciones]: Observador registrado. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🔕 [GestorNotificaciones]: Observador removido. Total: " + observadores.size());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📢 [GestorNotificaciones]: Notificando a " + observadores.size() + " observadores - Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
