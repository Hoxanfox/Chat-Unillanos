package gestionCanales.mensajes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dominio.MensajeEnviadoCanal;
import dominio.MensajeRecibidoCanal;
import dto.canales.DTOMensajeCanal;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.comunicacion.peticion.canal.DTOEnviarMensajeCanal;
import dto.comunicacion.peticion.canal.DTOSolicitarHistorialCanal;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;
import repositorio.mensaje.IRepositorioMensajeCanal;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementación del gestor de mensajes de canal.
 * Maneja el envío, recepción y persistencia de mensajes de canal.
 * Implementa el patrón Observer para notificar a la UI sobre cambios.
 */
public class GestorMensajesCanalImpl implements IGestorMensajesCanal {

    private final List<IObservador> observadores = new CopyOnWriteArrayList<>();
    private final IRepositorioMensajeCanal repositorioMensajes;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final GestorSesionUsuario gestorSesion;
    private final Gson gson;

    public GestorMensajesCanalImpl(IRepositorioMensajeCanal repositorioMensajes) {
        this.repositorioMensajes = repositorioMensajes;
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.gson = new Gson();
    }

    @Override
    public void inicializarManejadores() {
        // Manejador para nuevos mensajes (notificación push del servidor)
        gestorRespuesta.registrarManejador("nuevoMensajeCanal", this::manejarNuevoMensaje);

        // Manejador para historial de mensajes (usando el nombre correcto de la acción)
        gestorRespuesta.registrarManejador("solicitarHistorialCanal", this::manejarHistorial);

        // Manejador para confirmación de envío
        gestorRespuesta.registrarManejador("enviarMensajeCanal", this::manejarConfirmacionEnvio);

        System.out.println("✓ Manejadores de mensajes de canal inicializados");
    }

    /**
     * Maneja la llegada de un nuevo mensaje desde el servidor (notificación push).
     */
    private void manejarNuevoMensaje(DTOResponse respuesta) {
        if (!respuesta.fueExitoso()) {
            System.err.println("Error en notificación de mensaje: " + respuesta.getMessage());
            return;
        }

        try {
            // Convertir el objeto Data a DTOMensajeCanal
            Map<String, Object> data = (Map<String, Object>) respuesta.getData();

            // ✅ VALIDACIÓN: Verificar que sea un mensaje de canal válido
            String channelId = getString(data, "channelId");
            if (channelId == null || channelId.isEmpty()) {
                System.out.println("⚠️ [GestorMensajesCanal]: Mensaje recibido sin channelId - NO es un mensaje de canal, ignorando...");
                System.out.println("   → Este mensaje probablemente es un mensaje directo mal enrutado por el servidor");
                System.out.println("   → Debería llegar con action='nuevoMensajeDirecto', no 'nuevoMensajeCanal'");
                return;
            }

            DTOMensajeCanal mensaje = construirDTOMensajeDesdeMap(data);

            // Validación adicional: verificar que el canalId sea válido
            if (mensaje.getCanalId() == null || mensaje.getCanalId().isEmpty()) {
                System.out.println("⚠️ [GestorMensajesCanal]: Mensaje sin canalId válido después de construir - ignorando");
                return;
            }

            // Determinar si el mensaje es propio
            String usuarioActual = gestorSesion.getUserId();
            mensaje.setEsPropio(mensaje.getRemitenteId().equals(usuarioActual));

            // Persistir el mensaje recibido localmente
            MensajeRecibidoCanal mensajeDominio = convertirDTOAMensajeRecibido(mensaje);

            repositorioMensajes.guardarMensajeRecibido(mensajeDominio)
                .thenAccept(guardado -> {
                    if (guardado) {
                        // Notificar a la UI que hay un nuevo mensaje
                        notificarObservadores("MENSAJE_CANAL_RECIBIDO", mensaje);
                        System.out.println("✓ Nuevo mensaje de canal recibido y guardado: " + mensaje.getMensajeId());
                    } else {
                        System.err.println("✗ Error al guardar mensaje recibido");
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("✗ Excepción al guardar mensaje: " + ex.getMessage());
                    notificarObservadores("ERROR_OPERACION", "Error al guardar mensaje: " + ex.getMessage());
                    return null;
                });

        } catch (Exception e) {
            System.err.println("✗ Error procesando nuevo mensaje de canal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Maneja la respuesta del servidor con el historial de mensajes.
     */
    private void manejarHistorial(DTOResponse respuesta) {
        if (!respuesta.fueExitoso()) {
            System.err.println("Error al obtener historial: " + respuesta.getMessage());
            notificarObservadores("ERROR_OPERACION", respuesta.getMessage());
            return;
        }

        try {
            // El servidor envía un objeto con estructura: { mensajes: [...], hayMasMensajes: bool, ... }
            Map<String, Object> dataWrapper = (Map<String, Object>) respuesta.getData();
            List<Map<String, Object>> mensajesData = (List<Map<String, Object>>) dataWrapper.get("mensajes");

            if (mensajesData == null) {
                System.err.println("✗ No se encontró el campo 'mensajes' en la respuesta");
                notificarObservadores("HISTORIAL_CANAL_RECIBIDO", new ArrayList<>());
                return;
            }

            List<DTOMensajeCanal> historial = new ArrayList<>();
            String usuarioActual = gestorSesion.getUserId();

            for (Map<String, Object> mapa : mensajesData) {
                DTOMensajeCanal mensaje = construirDTOMensajeDesdeMap(mapa);
                mensaje.setEsPropio(mensaje.getRemitenteId().equals(usuarioActual));
                historial.add(mensaje);
            }

            // Sincronizar con la base de datos local
            if (!historial.isEmpty()) {
                String canalId = historial.get(0).getCanalId();
                repositorioMensajes.sincronizarHistorial(canalId, usuarioActual, historial)
                    .thenAccept(v -> {
                        // Notificar a la UI con el historial UNA SOLA VEZ
                        notificarObservadores("HISTORIAL_CANAL_RECIBIDO", historial);
                        System.out.println("✓ Historial de canal sincronizado: " + historial.size() + " mensajes");
                    })
                    .exceptionally(ex -> {
                        System.err.println("✗ Error sincronizando historial: " + ex.getMessage());
                        // Aún así notificamos a la UI con los datos del servidor
                        notificarObservadores("HISTORIAL_CANAL_RECIBIDO", historial);
                        return null;
                    });
            } else {
                notificarObservadores("HISTORIAL_CANAL_RECIBIDO", historial);
            }

        } catch (Exception e) {
            System.err.println("✗ Error procesando historial de canal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Maneja la confirmación de envío de mensaje.
     */
    private void manejarConfirmacionEnvio(DTOResponse respuesta) {
        if (!respuesta.fueExitoso()) {
            System.err.println("Error en la confirmación de envío: " + respuesta.getMessage());
            notificarObservadores("ERROR_OPERACION", respuesta.getMessage());
            return;
        }

        // La confirmación de envío no requiere procesamiento adicional por ahora
        System.out.println("✓ Confirmación de envío recibida: " + respuesta.getMessage());
    }

    @Override
    public void solicitarHistorialCanal(String canalId, int limite) {
        String usuarioId = gestorSesion.getUserId();
        if (usuarioId == null) {
            System.err.println("✗ No se puede solicitar historial: usuario no autenticado");
            return;
        }

        DTOSolicitarHistorialCanal payload = new DTOSolicitarHistorialCanal(canalId, usuarioId, limite, 0);
        DTORequest peticion = new DTORequest("solicitarHistorialCanal", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("→ Solicitando historial del canal: " + canalId);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String canalId, String contenido) {
        String remitenteId = gestorSesion.getUserId();

        if (remitenteId == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Usuario no autenticado"));
        }

        DTOEnviarMensajeCanal payload = DTOEnviarMensajeCanal.deTexto(remitenteId, canalId, contenido);

        MensajeEnviadoCanal mensajeLocal = new MensajeEnviadoCanal(
            UUID.randomUUID(),
            contenido.getBytes(),
            LocalDateTime.now(),
            "texto",
            UUID.fromString(remitenteId),
            UUID.fromString(canalId)
        );

        return repositorioMensajes.guardarMensajeEnviado(mensajeLocal)
            .thenCompose(guardado -> {
                if (guardado) {
                    DTORequest peticion = new DTORequest("enviarMensajeCanal", payload);
                    enviadorPeticiones.enviar(peticion);
                    System.out.println("→ Mensaje de texto enviado al canal: " + canalId);
                    return CompletableFuture.completedFuture(null);
                } else {
                    return CompletableFuture.failedFuture(new RuntimeException("Error al guardar mensaje localmente"));
                }
            });
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String canalId, String audioFileId) {
        String remitenteId = gestorSesion.getUserId();

        if (remitenteId == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Usuario no autenticado"));
        }

        DTOEnviarMensajeCanal payload = DTOEnviarMensajeCanal.deAudio(remitenteId, canalId, audioFileId);

        MensajeEnviadoCanal mensajeLocal = new MensajeEnviadoCanal(
            UUID.randomUUID(),
            audioFileId.getBytes(),
            LocalDateTime.now(),
            "audio",
            UUID.fromString(remitenteId),
            UUID.fromString(canalId)
        );

        return repositorioMensajes.guardarMensajeEnviado(mensajeLocal)
            .thenCompose(guardado -> {
                if (guardado) {
                    DTORequest peticion = new DTORequest("enviarMensajeCanal", payload);
                    enviadorPeticiones.enviar(peticion);
                    System.out.println("→ Mensaje de audio enviado al canal: " + canalId);
                    return CompletableFuture.completedFuture(null);
                } else {
                    return CompletableFuture.failedFuture(new RuntimeException("Error al guardar mensaje localmente"));
                }
            });
    }

    @Override
    public CompletableFuture<Void> enviarArchivo(String canalId, String fileId) {
        String remitenteId = gestorSesion.getUserId();

        if (remitenteId == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Usuario no autenticado"));
        }

        DTOEnviarMensajeCanal payload = DTOEnviarMensajeCanal.deArchivo(remitenteId, canalId, fileId);

        MensajeEnviadoCanal mensajeLocal = new MensajeEnviadoCanal(
            UUID.randomUUID(),
            fileId.getBytes(),
            LocalDateTime.now(),
            "archivo",
            UUID.fromString(remitenteId),
            UUID.fromString(canalId)
        );

        return repositorioMensajes.guardarMensajeEnviado(mensajeLocal)
            .thenCompose(guardado -> {
                if (guardado) {
                    DTORequest peticion = new DTORequest("enviarMensajeCanal", payload);
                    enviadorPeticiones.enviar(peticion);
                    System.out.println("→ Archivo enviado al canal: " + canalId);
                    return CompletableFuture.completedFuture(null);
                } else {
                    return CompletableFuture.failedFuture(new RuntimeException("Error al guardar mensaje localmente"));
                }
            });
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("✓ Observador registrado en GestorMensajesCanal");
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("✓ Observador removido de GestorMensajesCanal");
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }

    private DTOMensajeCanal construirDTOMensajeDesdeMap(Map<String, Object> data) {
        DTOMensajeCanal mensaje = new DTOMensajeCanal();

        // Lee los IDs principales (usando los nombres del log)
        mensaje.setMensajeId(getString(data, "messageId"));
        mensaje.setCanalId(getString(data, "channelId"));

        // Verifica si existe el objeto anidado "author"
        if (data.containsKey("author") && data.get("author") instanceof Map) {
            Map<String, Object> authorMap = (Map<String, Object>) data.get("author");
            mensaje.setRemitenteId(getString(authorMap, "userId"));
            mensaje.setNombreRemitente(getString(authorMap, "username"));
        } else {
            mensaje.setRemitenteId(getString(data, "usuarioId") != null ? getString(data, "usuarioId") : getString(data, "remitenteId"));
            mensaje.setNombreRemitente(getString(data, "nombreUsuario") != null ? getString(data, "nombreUsuario") : getString(data, "nombreRemitente"));
        }

        // ✅ FIX: Normalizar tipo de mensaje a MAYÚSCULAS (servidor envía "TEXT"/"AUDIO")
        String messageType = getString(data, "messageType");
        if (messageType != null) {
            messageType = messageType.toUpperCase(); // Normalizar a MAYÚSCULAS
        }
        mensaje.setTipo(messageType);

        mensaje.setContenido(getString(data, "content"));
        mensaje.setFileId(getString(data, "fileId"));

        // Manejo de la fecha
        String fechaStr = getString(data, "timestamp") != null ? getString(data, "timestamp") : getString(data, "fechaEnvio");
        if (fechaStr != null) {
            try {
                mensaje.setFechaEnvio(LocalDateTime.parse(fechaStr));
            } catch (Exception e) {
                try {
                    mensaje.setFechaEnvio(LocalDateTime.parse(fechaStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e2) {
                    mensaje.setFechaEnvio(LocalDateTime.now());
                }
            }
        }

        return mensaje;
    }

    private MensajeRecibidoCanal convertirDTOAMensajeRecibido(DTOMensajeCanal dto) {
        MensajeRecibidoCanal mensaje = new MensajeRecibidoCanal();

        // Generar un UUID si el ID no es un UUID válido (puede ser un número secuencial del servidor)
        UUID mensajeId;
        try {
            mensajeId = UUID.fromString(dto.getMensajeId());
        } catch (IllegalArgumentException e) {
            // Si no es un UUID válido, generar uno basado en un hash del ID
            mensajeId = UUID.nameUUIDFromBytes(dto.getMensajeId().getBytes());
        }
        mensaje.setIdMensaje(mensajeId);

        mensaje.setIdRemitenteCanal(UUID.fromString(dto.getCanalId()));
        mensaje.setTipo(dto.getTipo());
        mensaje.setFechaEnvio(dto.getFechaEnvio());

        String contenidoStr = "texto".equals(dto.getTipo()) ? dto.getContenido() : dto.getFileId();
        if (contenidoStr != null) {
            mensaje.setContenido(contenidoStr.getBytes());
        }

        return mensaje;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
