package gestionCanales.invitaciones;

import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.canales.DTOCanalCreado;
import dto.canales.DTOInvitacionCanal;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.featureNotificaciones.DTONotificacion;
import gestionNotificaciones.GestorSincronizacionGlobal;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;
import repositorio.canal.IRepositorioCanal;
import repositorio.notificacion.IRepositorioNotificacion;
import repositorio.notificacion.RepositorioNotificacionImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del gestor de invitaciones a canales.
 * Maneja la solicitud de invitaciones pendientes y la respuesta a las mismas.
 * Ahora escucha eventos globales de sincronización.
 */
public class GestorInvitacionesImpl implements IGestorInvitaciones, IObservador { // [NUEVO] Implementa IObservador

    private final GestorSesionUsuario gestorSesion;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IRepositorioCanal repositorioCanal;
    private final IRepositorioNotificacion repositorioNotificacion;
    private final List<IObservador> observadores;

    // 🆕 Caché de invitaciones pendientes
    private List<DTOCanalCreado> invitacionesCache = new ArrayList<>();

    public GestorInvitacionesImpl(IRepositorioCanal repositorioCanal) {
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.repositorioCanal = repositorioCanal;
        this.repositorioNotificacion = RepositorioNotificacionImpl.getInstancia();
        this.observadores = new ArrayList<>();

        // Inicializar manejadores de notificaciones push
        inicializarManejadoresNotificaciones();

        // [NUEVO] Registrarse en el Gestor Global para escuchar el evento ACTUALIZAR_NOTIFICACIONES
        GestorSincronizacionGlobal.getInstancia().registrarObservador(this);

        System.err.println("✅ [GestorInvitaciones]: Inicializado con repositorio SINGLETON y escuchando sincronización global");
    }

    private void inicializarManejadoresNotificaciones() {
        // Manejador para notificaciones push de nuevas invitaciones
        gestorRespuesta.registrarManejador("notificacionInvitacionCanal", this::manejarNuevaInvitacion);
        System.err.println("🔔 [GestorInvitaciones]: Manejadores de notificaciones push registrados");
    }

    private void manejarNuevaInvitacion(DTOResponse respuesta) {
        System.err.println("🔔 [GestorInvitaciones]: Nueva invitación recibida por PUSH");

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
                String inviterName = "Alguien";
                String invitadorId = null;
                String inviterPhoto = "";

                if (invitacionData.get("owner") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ownerMap = (Map<String, Object>) invitacionData.get("owner");
                    invitadorId = ownerMap.get("userId") != null ?
                            ownerMap.get("userId").toString() : null;
                    inviterName = ownerMap.get("username") != null ?
                            ownerMap.get("username").toString() : "Alguien";
                    inviterPhoto = ownerMap.get("userPhoto") != null ?
                            ownerMap.get("userPhoto").toString() : "";
                }

                System.err.println("   → Canal: " + channelName);
                System.err.println("   → Invitado por: " + inviterName);

                // Crear y guardar una DTONotificacion en el repositorio
                String notificacionId = UUID.randomUUID().toString();
                String titulo = "Invitación a canal";
                String contenido = inviterName + " te ha invitado al canal '" + channelName + "'";

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
                System.err.println("💾 [GestorInvitaciones]: Notificación de invitación guardada - ID: " + notificacionId);

                // 🆕 Invalidar caché para que se actualice en la próxima consulta
                invitacionesCache.clear();

                // Notificar a observadores sobre la nueva notificación
                notificarObservadores("NUEVA_NOTIFICACION", notificacion);
                notificarObservadores("ACTUALIZAR_NOTIFICACIONES", null);

            }

        } catch (Exception e) {
            System.err.println("❌ [GestorInvitaciones]: Error procesando nueva invitación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<List<DTOCanalCreado>> solicitarInvitacionesPendientes() {
        CompletableFuture<List<DTOCanalCreado>> future = new CompletableFuture<>();

        long startNano = System.nanoTime();
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.err.println("[" + startTime + "] 🔴🔴🔴 [GestorInvitaciones]: Iniciando solicitarInvitacionesPendientes()");

        String userId = gestorSesion.getUserId();
        if (userId == null) {
            System.err.println("[" + startTime + "] ❌ [GestorInvitaciones]: Usuario no autenticado -> abortando");
            future.completeExceptionally(new IllegalStateException("Usuario no autenticado"));
            return future;
        }

        System.err.println("[" + startTime + "] 🔴 [GestorInvitaciones]: Usuario ID: " + userId);
        System.err.println("[" + startTime + "] 🔴 [GestorInvitaciones]: Estado caché -> tamaño: " + invitacionesCache.size());

        // Si ya hay invitaciones en caché, retornarlas inmediatamente SIN hacer nueva solicitud
        if (!invitacionesCache.isEmpty()) {
            System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴 [GestorInvitaciones]: Devolviendo " + invitacionesCache.size() + " invitaciones desde CACHÉ (sin solicitar al servidor)");
            System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴 [GestorInvitaciones]: Notificando a " + observadores.size() + " observadores con datos del caché");
            notificarObservadores("INVITACIONES_PENDIENTES", invitacionesCache);
            long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
            System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔚 [GestorInvitaciones]: solicitarInvitacionesPendientes() finalizada (cache hit) - duracion: " + elapsedMs + " ms");
            future.complete(invitacionesCache);
            return future;
        }

        System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴 [GestorInvitaciones]: Caché vacío, preparando petición al servidor...");

        // Crear payload
        Map<String, String> payload = new HashMap<>();
        payload.put("usuarioId", userId);
        System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴 [GestorInvitaciones]: Payload -> " + payload);

        DTORequest request = new DTORequest("obtenerInvitaciones", payload);

        // Registrar manejador de respuesta
        System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴 [GestorInvitaciones]: Registrando manejador para 'obtenerInvitaciones'");
        gestorRespuesta.registrarManejador("obtenerInvitaciones", (respuesta) -> {
            String handlerTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            System.err.println("[" + handlerTime + "] 🔴🔴🔴 [GestorInvitaciones]: Manejador 'obtenerInvitaciones' invocado - status: " + respuesta.getStatus() + ", message: " + respuesta.getMessage());

            if ("success".equals(respuesta.getStatus())) {
                try {
                    System.err.println("[" + handlerTime + "] 🔴🔴🔴 [GestorInvitaciones]: ===== RESPUESTA RECIBIDA DEL SERVIDOR =====");

                    Object data = respuesta.getData();
                    System.err.println("[" + handlerTime + "] 🔴 [GestorInvitaciones]: Tipo de `data`: " + (data == null ? "null" : data.getClass().getName()) + " -> contenido: " + String.valueOf(data));

                    List<DTOCanalCreado> invitaciones = new ArrayList<>();
                    List<DTOInvitacionCanal> invitacionesCompletas = new ArrayList<>();

                    if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) data;
                        Object invitacionesList = dataMap.get("invitaciones");
                        System.err.println("[" + handlerTime + "] 🔴 [GestorInvitaciones]: Objeto 'invitaciones' encontrado -> tipo: " + (invitacionesList == null ? "null" : invitacionesList.getClass().getName()));

                        if (invitacionesList instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> listaInvitaciones = (List<Map<String, Object>>) invitacionesList;

                            System.err.println("[" + handlerTime + "] 🔴 [GestorInvitaciones]: Procesando lista con " + listaInvitaciones.size() + " entradas");

                            int idx = 0;
                            for (Map<String, Object> invitacionMap : listaInvitaciones) {
                                idx++;
                                System.err.println("[" + handlerTime + "] 🔎 [Invitación #" + idx + "] Raw map: " + invitacionMap);

                                // Extraer datos de la invitación
                                String invitacionId = invitacionMap.get("invitacionId") != null ?
                                        invitacionMap.get("invitacionId").toString() : UUID.randomUUID().toString();
                                String channelId = invitacionMap.get("channelId") != null ?
                                        invitacionMap.get("channelId").toString() : null;
                                String channelName = invitacionMap.get("channelName") != null ?
                                        invitacionMap.get("channelName").toString() : null;
                                String channelType = invitacionMap.get("channelType") != null ?
                                        invitacionMap.get("channelType").toString() : "GRUPO";
                                String estado = invitacionMap.get("estado") != null ?
                                        invitacionMap.get("estado").toString() : "PENDIENTE";

                                System.err.println("[" + handlerTime + "] 🔎 [Invitación #" + idx + "] invitacionId=" + invitacionId + ", channelId=" + channelId + ", channelName=" + channelName + ", channelType=" + channelType + ", estado=" + estado);

                                // Parsear fecha de creación
                                LocalDateTime fechaCreacion = LocalDateTime.now();
                                if (invitacionMap.get("fechaCreacion") != null) {
                                    String fechaRaw = invitacionMap.get("fechaCreacion").toString();
                                    try {
                                        fechaCreacion = LocalDateTime.parse(fechaRaw, DateTimeFormatter.ISO_DATE_TIME);
                                        System.err.println("[" + handlerTime + "] 🔎 [Invitación #" + idx + "] fechaCreacion parseada: " + fechaCreacion.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                                    } catch (Exception e) {
                                        System.err.println("[" + handlerTime + "] ⚠️ [Invitación #" + idx + "] Error parseando fecha (" + fechaRaw + "): " + e.getMessage() + " -> usando fecha actual: " + fechaCreacion);
                                    }
                                } else {
                                    System.err.println("[" + handlerTime + "] 🔎 [Invitación #" + idx + "] fechaCreacion ausente -> usando: " + fechaCreacion);
                                }

                                // Extraer información del invitador
                                dto.featureContactos.DTOContacto invitador = null;
                                String inviterName = "Alguien";
                                String inviterPhoto = "";

                                if (invitacionMap.get("invitador") instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> invitadorMap = (Map<String, Object>) invitacionMap.get("invitador");
                                    String invitadorId = invitadorMap.get("userId") != null ?
                                            invitadorMap.get("userId").toString() : null;
                                    inviterName = invitadorMap.get("username") != null ?
                                            invitadorMap.get("username").toString() : "Alguien";
                                    inviterPhoto = invitadorMap.get("userPhoto") != null ?
                                            invitadorMap.get("userPhoto").toString() : "";

                                    System.err.println("[" + handlerTime + "] 🔎 [Invitación #" + idx + "] invitadorMap: " + invitadorMap + " -> invitadorId=" + invitadorId + ", username=" + inviterName + ", userPhoto=" + inviterPhoto);

                                    if (invitadorId != null) {
                                        invitador = new dto.featureContactos.DTOContacto(
                                                invitadorId, inviterName, "", inviterPhoto, ""
                                        );
                                    }
                                } else {
                                    System.err.println("[" + handlerTime + "] 🔎 [Invitación #" + idx + "] invitador ausente o no es Map -> valor: " + invitacionMap.get("invitador"));
                                }

                                if (channelId != null && channelName != null) {
                                    DTOCanalCreado dtoCanal = new DTOCanalCreado(channelId, channelName);
                                    dtoCanal.setTipo(channelType);
                                    dtoCanal.setOwner(invitador);
                                    invitaciones.add(dtoCanal);

                                    DTOInvitacionCanal invitacionCompleta = new DTOInvitacionCanal(
                                            invitacionId,
                                            channelId,
                                            channelName,
                                            channelType,
                                            invitador,
                                            estado,
                                            fechaCreacion
                                    );
                                    invitacionesCompletas.add(invitacionCompleta);

                                    // Crear notificación
                                    String titulo = "Invitación a canal";
                                    String contenido = inviterName + " te ha invitado al canal '" + channelName + "'";
                                    DTONotificacion notificacion = new DTONotificacion(
                                            invitacionId,
                                            "INVITACION_CANAL",
                                            titulo,
                                            contenido,
                                            fechaCreacion,
                                            false,
                                            channelId
                                    );

                                    System.err.println("[" + handlerTime + "] 💾💾💾 [GestorInvitaciones]: ===== GUARDANDO NOTIFICACIÓN EN REPOSITORIO =====");
                                    System.err.println("[" + handlerTime + "] 💾 [GestorInvitaciones]: ID Notificación: " + invitacionId);
                                    System.err.println("[" + handlerTime + "] 💾 [GestorInvitaciones]: Tipo: " + notificacion.getTipo());
                                    System.err.println("[" + handlerTime + "] 💾 [GestorInvitaciones]: Título: " + notificacion.getTitulo());
                                    System.err.println("[" + handlerTime + "] 💾 [GestorInvitaciones]: Contenido: " + notificacion.getContenido());
                                    System.err.println("[" + handlerTime + "] 💾 [GestorInvitaciones]: Canal ID: " + channelId);

                                    // Guardar en repositorio
                                    repositorioNotificacion.guardar(notificacion);
                                    System.err.println("[" + handlerTime + "] 💾💾💾 [GestorInvitaciones]: ===== NOTIFICACIÓN GUARDADA EXITOSAMENTE =====");

                                    // Verificar que se guardó correctamente
                                    List<DTONotificacion> todasNotificaciones = repositorioNotificacion.obtenerTodas();
                                    System.err.println("[" + handlerTime + "] 🔍 [GestorInvitaciones]: Verificación post-guardado: " + todasNotificaciones.size() + " notificaciones en repositorio");

                                    System.err.println("[" + handlerTime + "] 🔴    ✓ Invitación procesada -> channelName=" + channelName + ", channelId=" + channelId + ", invitacionId=" + invitacionId);
                                    System.err.println("[" + handlerTime + "] 🔴      → Notificación guardada - ID: " + invitacionId);
                                } else {
                                    System.err.println("[" + handlerTime + "] ⚠️ [Invitación #" + idx + "] Faltan channelId o channelName -> se omite esta invitación");
                                }
                            }
                        } else {
                            System.err.println("[" + handlerTime + "] ⚠️ [GestorInvitaciones]: 'invitaciones' no es lista o es null -> valor: " + invitacionesList);
                        }
                    } else {
                        System.err.println("[" + handlerTime + "] ⚠️ [GestorInvitaciones]: `data` no es Map -> contenido: " + data);
                    }

                    System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴🔴🔴 [GestorInvitaciones]: Procesadas " + invitaciones.size() + " invitaciones");

                    // Guardar en caché las invitaciones recibidas
                    invitacionesCache = invitaciones;
                    System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴 [GestorInvitaciones]: Caché actualizado -> tamaño: " + invitacionesCache.size());

                    System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴🔴🔴 [GestorInvitaciones]: Notificando a " + observadores.size() + " observadores");

                    // 1. Notificar a la vista de "Mis Invitaciones" (usa DTOInvitacionCanal)
                    notificarObservadores("INVITACIONES_PENDIENTES", invitacionesCompletas);

                    // 2. Notificar que hay cambios en notificaciones (sin enviar datos, la vista solicitará)
                    System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴 [GestorInvitaciones]: Notificando ACTUALIZAR_NOTIFICACIONES (señal para refrescar)");
                    notificarObservadores("ACTUALIZAR_NOTIFICACIONES", null);

                    future.complete(invitaciones);

                    long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
                    System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔚 [GestorInvitaciones]: solicitarInvitacionesPendientes() finalizada - duracion: " + elapsedMs + " ms");

                } catch (Exception e) {
                    System.err.println("❌ [GestorInvitaciones]: Error procesando respuesta de invitaciones: " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            } else {
                System.err.println("❌ [GestorInvitaciones]: Error al obtener invitaciones: " + respuesta.getMessage());
                future.completeExceptionally(new RuntimeException(respuesta.getMessage()));
            }
        });

        // Enviar la petición
        try {
            enviadorPeticiones.enviar(request);
            System.err.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] 🔴 [GestorInvitaciones]: Petición enviada correctamente");
        } catch (Exception e) {
            System.err.println("❌ [GestorInvitaciones]: Error enviando petición: " + e.getMessage());
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public CompletableFuture<Void> responderInvitacion(String channelId, boolean aceptar) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String userId = gestorSesion.getUserId();
        if (userId == null) {
            future.completeExceptionally(new IllegalStateException("Usuario no autenticado"));
            return future;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("usuarioId", userId);
        payload.put("channelId", channelId);
        payload.put("aceptar", aceptar);

        DTORequest request = new DTORequest("responderInvitacion", payload);

        gestorRespuesta.registrarManejador("responderInvitacion", (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                System.out.println("✅ Invitación " + (aceptar ? "aceptada" : "rechazada") + " correctamente");

                // Invalidar caché para forzar actualización
                invitacionesCache.clear();

                // Si aceptó, podría notificar que se unió al canal
                if (aceptar) {
                    notificarObservadores("CANAL_UNIDO", channelId);
                }

                // Notificar que las invitaciones deben actualizarse
                notificarObservadores("ACTUALIZAR_INVITACIONES", null);

                future.complete(null);
            } else {
                System.err.println("❌ Error al responder invitación: " + respuesta.getMessage());
                future.completeExceptionally(new RuntimeException(respuesta.getMessage()));
            }
        });

        try {
            enviadorPeticiones.enviar(request);
        } catch (Exception e) {
            System.err.println("❌ Error enviando respuesta de invitación: " + e.getMessage());
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("📌 [GestorInvitaciones]: Observador registrado. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("📌 [GestorInvitaciones]: Observador removido. Total: " + observadores.size());
    }

    @Override
    public void notificarObservadores(String evento, Object data) {
        System.err.println("🔔 [GestorInvitaciones]: Notificando evento '" + evento + "' a " + observadores.size() + " observadores");
        for (IObservador observador : observadores) {
            try {
                observador.actualizar(evento, data);
            } catch (Exception e) {
                System.err.println("❌ [GestorInvitaciones]: Error notificando observador: " + e.getMessage());
            }
        }
    }

    @Override
    public void actualizar(String evento, Object data) {
        System.err.println("🔔 [GestorInvitaciones]: Recibido evento global: " + evento);

        if ("ACTUALIZAR_NOTIFICACIONES".equals(evento) || "ACTUALIZAR_INVITACIONES".equals(evento)) {
            System.err.println("🔄 [GestorInvitaciones]: Invalidando caché y solicitando actualización de invitaciones pendientes...");
            // Invalidar caché para forzar nueva consulta al servidor
            invitacionesCache.clear();
            // Solicitar invitaciones actualizadas
            solicitarInvitacionesPendientes();
        }
    }
}
