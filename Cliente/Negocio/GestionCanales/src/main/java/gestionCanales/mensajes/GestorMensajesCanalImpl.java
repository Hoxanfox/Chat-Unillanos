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
import gestionArchivos.IGestionArchivos;
import gestionNotificaciones.GestorSincronizacionGlobal;
import observador.IObservador;
import repositorio.mensaje.IRepositorioMensajeCanal;
import repositorio.canal.IRepositorioCanal;

import java.io.File;
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
 *
 * ✅ AHORA implementa IObservador para recibir señales del GestorSincronizacionGlobal
 */
public class GestorMensajesCanalImpl implements IGestorMensajesCanal, IObservador {

    private final List<IObservador> observadores = new CopyOnWriteArrayList<>();
    private final IRepositorioMensajeCanal repositorioMensajes;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final GestorSesionUsuario gestorSesion;
    private final IGestionArchivos gestionArchivos;
    private final Gson gson;

    // 🆕 Referencia al repositorio de canales para obtener la lista de canales
    private final IRepositorioCanal repositorioCanal;

    // 🆕 Campo para almacenar el ID del canal actualmente abierto
    private String canalActivoId = null;

    public GestorMensajesCanalImpl(IRepositorioMensajeCanal repositorioMensajes, IGestionArchivos gestionArchivos, IRepositorioCanal repositorioCanal) {
        this.repositorioMensajes = repositorioMensajes;
        this.gestionArchivos = gestionArchivos;
        this.repositorioCanal = repositorioCanal;
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.gson = new Gson();

        // 🆕 Registrarse como observador del GestorSincronizacionGlobal
        GestorSincronizacionGlobal.getInstancia().registrarObservador(this);
        System.out.println("✅ [GestorMensajesCanal]: Registrado como observador del GestorSincronizacionGlobal");
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
     * 🆕 Implementación de IObservador.
     * Recibe señales del GestorSincronizacionGlobal.
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [GestorMensajesCanal]: Señal recibida del GestorSincronizacionGlobal - Tipo: " + tipoDeDato);

        if ("ACTUALIZAR_MENSAJES_CANALES".equals(tipoDeDato)) {
            System.out.println("📨 [GestorMensajesCanal]: Procesando ACTUALIZAR_MENSAJES_CANALES");
            System.out.println("🔄 [GestorMensajesCanal]: Solicitando historial de TODOS los canales...");

            // ✅ Obtener todos los canales del repositorio
            repositorioCanal.obtenerTodos()
                .thenAccept(canales -> {
                    System.out.println("📋 [GestorMensajesCanal]: " + canales.size() + " canales encontrados en caché");

                    // Solicitar historial de cada canal
                    for (dominio.Canal canal : canales) {
                        String canalId = canal.getIdCanal().toString();
                        System.out.println("   → Solicitando historial del canal: " + canal.getNombre() + " (ID: " + canalId + ")");
                        solicitarHistorialCanal(canalId, 50);
                    }

                    System.out.println("✅ [GestorMensajesCanal]: Historial solicitado para todos los canales");
                })
                .exceptionally(ex -> {
                    System.err.println("❌ [GestorMensajesCanal]: Error al obtener canales del repositorio: " + ex.getMessage());
                    return null;
                });
        }
    }

    /**
     * 🆕 Establece el canal actualmente abierto en la UI.
     * Las vistas deben llamar a este método cuando un usuario abre un canal.
     *
     * @param canalId El ID del canal que está actualmente abierto, o null si ninguno está abierto
     */
    public void setCanalActivo(String canalId) {
        this.canalActivoId = canalId;
        System.out.println("📍 [GestorMensajesCanal]: Canal activo establecido: " + canalId);
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
                        System.out.println("✓ Nuevo mensaje de canal recibido e guardado: " + mensaje.getMensajeId());
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
                // ✅ Marcar correctamente si el mensaje es propio comparando IDs
                mensaje.setEsPropio(mensaje.getRemitenteId().equals(usuarioActual));
                historial.add(mensaje);
            }

            // ✅ ORDENAR MENSAJES POR TIMESTAMP (del más antiguo al más reciente)
            historial.sort((m1, m2) -> {
                if (m1.getFechaEnvio() == null && m2.getFechaEnvio() == null) return 0;
                if (m1.getFechaEnvio() == null) return 1; // null al final
                if (m2.getFechaEnvio() == null) return -1; // null al final
                return m1.getFechaEnvio().compareTo(m2.getFechaEnvio());
            });

            System.out.println("📋 [GestorMensajesCanal]: Historial ordenado por timestamp - Total: " + historial.size());

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

        try {
            // El servidor devuelve el mensaje confirmado con su ID definitivo
            Map<String, Object> data = (Map<String, Object>) respuesta.getData();

            // Construir el DTO del mensaje desde la respuesta del servidor
            DTOMensajeCanal mensaje = construirDTOMensajeDesdeMap(data);

            // Marcar el mensaje como propio
            String usuarioActual = gestorSesion.getUserId();
            mensaje.setEsPropio(mensaje.getRemitenteId().equals(usuarioActual));

            // Notificar a la UI para que muestre el mensaje
            notificarObservadores("MENSAJE_CANAL_ENVIADO", mensaje);

            System.out.println("✓ Mensaje propio confirmado por servidor y notificado a la UI: " + mensaje.getMensajeId());
        } catch (Exception e) {
            System.err.println("✗ Error procesando confirmación de envío: " + e.getMessage());
            e.printStackTrace();
        }
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

        String content = getString(data, "content");
        String fileId = getString(data, "fileId");

        // ✅ DETECCIÓN AUTOMÁTICA: Si el content contiene una ruta de archivo, ajustar el tipo
        if (content != null && (content.startsWith("audio_files/") || content.startsWith("image_files/") ||
            content.startsWith("document_files/") || content.endsWith(".wav") || content.endsWith(".mp3") ||
            content.endsWith(".jpg") || content.endsWith(".png") || content.endsWith(".pdf"))) {

            // Es un archivo, mover el content a fileId
            fileId = content;

            // Determinar el tipo real del archivo
            if (content.startsWith("audio_files/") || content.endsWith(".wav") || content.endsWith(".mp3")) {
                messageType = "AUDIO";
                System.out.println("🔄 [GestorMensajesCanal]: Mensaje detectado como AUDIO - FileId: " + fileId);
            } else if (content.endsWith(".jpg") || content.endsWith(".png") || content.endsWith(".gif") || content.startsWith("image_files/")) {
                messageType = "IMAGEN";
                System.out.println("🔄 [GestorMensajesCanal]: Mensaje detectado como IMAGEN - FileId: " + fileId);
            } else {
                messageType = "ARCHIVO";
                System.out.println("🔄 [GestorMensajesCanal]: Mensaje detectado como ARCHIVO - FileId: " + fileId);
            }

            content = null; // Limpiar el contenido ya que es un archivo
        }

        mensaje.setTipo(messageType);
        mensaje.setContenido(content);
        mensaje.setFileId(fileId);

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

        // ✅ NUEVA FUNCIONALIDAD: Descargar automáticamente archivos cuando lleguen
        if (fileId != null && !fileId.isEmpty()) {
            descargarArchivoAutomaticamente(mensaje);
        }

        return mensaje;
    }

    /**
     * Descarga automáticamente un archivo del servidor cuando llega un mensaje con fileId.
     * Similar al comportamiento del chat de contactos.
     *
     * @param mensaje El mensaje que contiene el fileId a descargar
     */
    private void descargarArchivoAutomaticamente(DTOMensajeCanal mensaje) {
        String fileId = mensaje.getFileId();
        String tipo = mensaje.getTipo();

        System.out.println("📥 [GestorMensajesCanal]: Iniciando descarga automática de archivo");
        System.out.println("   → FileId: " + fileId);
        System.out.println("   → Tipo: " + tipo);

        // Determinar el directorio de destino según el tipo de archivo
        File directorioDestino;
        if ("AUDIO".equalsIgnoreCase(tipo)) {
            directorioDestino = new File("data/archivos/audios");
        } else if ("IMAGEN".equalsIgnoreCase(tipo)) {
            directorioDestino = new File("data/archivos/images");
        } else {
            directorioDestino = new File("data/archivos/documents");
        }

        // Asegurar que el directorio existe
        if (!directorioDestino.exists()) {
            directorioDestino.mkdirs();
        }

        // Descargar el archivo de forma asíncrona
        gestionArchivos.descargarArchivo(fileId, directorioDestino)
            .thenAccept(archivoDescargado -> {
                System.out.println("✅ [GestorMensajesCanal]: Archivo descargado exitosamente");
                System.out.println("   → Ruta local: " + archivoDescargado.getAbsolutePath());

                // Actualizar el mensaje con la ruta local del archivo descargado
                mensaje.setContenido(archivoDescargado.getAbsolutePath());

                // Notificar a la UI que el archivo está listo para ser usado
                notificarObservadores("ARCHIVO_DESCARGADO", mensaje);
            })
            .exceptionally(ex -> {
                System.err.println("✗ [GestorMensajesCanal]: Error al descargar archivo automáticamente");
                System.err.println("   → FileId: " + fileId);
                System.err.println("   → Error: " + ex.getMessage());

                // Notificar a la UI del error
                notificarObservadores("ERROR_DESCARGA_ARCHIVO",
                    "No se pudo descargar el archivo: " + fileId);
                return null;
            });
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
        // Establecer el ID del usuario actual como destinatario
        mensaje.setIdDestinatario(UUID.fromString(gestorSesion.getUserId()));
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

