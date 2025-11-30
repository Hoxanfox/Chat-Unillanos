package servicio.chat;

import fachada.FachadaGeneralImpl;
import fachada.gestionArchivos.IFachadaArchivos;
import gestionContactos.audio.GestorAudio;
import observador.IObservador;
import fachada.gestionContactos.contactos.IFachadaContactos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de chat que AHORA depende de las Fachadas.
 * ✅ Respeta la arquitectura: Servicio -> Fachada -> Gestión
 */
public class ServicioChatImpl implements IServicioChat, IObservador {

    private final List<IObservador> observadores = new ArrayList<>();
    private final IFachadaContactos fachadaContactos;
    private final IFachadaArchivos fachadaArchivos;
    private final GestorAudio gestorAudio; // ✅ Solo para grabación (entrada de audio)

    public ServicioChatImpl() {
        System.out.println("🔧 [ServicioChat]: Inicializando servicio de chat...");

        // Obtiene las fachadas desde la Fachada General
        this.fachadaContactos = FachadaGeneralImpl.getInstancia().getFachadaContactos();
        this.fachadaArchivos = FachadaGeneralImpl.getInstancia().getFachadaArchivos();
        this.gestorAudio = GestorAudio.getInstancia(); // ✅ Solo para captura de audio del micrófono

        // Se suscribe a la fachada de contactos para recibir notificaciones
        this.fachadaContactos.registrarObservador(this);

        System.out.println("✅ [ServicioChat]: Servicio inicializado respetando arquitectura");
    }

    @Override
    public void solicitarHistorial(String contactoId) {
        System.out.println("➡️ [ServicioChat]: Delegando solicitud de historial a la fachada - ContactoId: " + contactoId);
        fachadaContactos.solicitarHistorial(contactoId);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
        System.out.println("➡️ [ServicioChat]: Delegando envío de mensaje de texto a la fachada");
        System.out.println("   → DestinatarioId: " + destinatarioId);
        System.out.println("   → Contenido: " + contenido);
        return fachadaContactos.enviarMensajeTexto(destinatarioId, contenido);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId) {
        System.out.println("➡️ [ServicioChat]: Delegando envío de mensaje de audio a la fachada");
        System.out.println("   → DestinatarioId: " + destinatarioId);
        System.out.println("   → AudioFileId: " + audioFileId);
        return fachadaContactos.enviarMensajeAudio(destinatarioId, audioFileId);
    }

    @Override
    public void iniciarGrabacionAudio() throws Exception {
        System.out.println("➡️ [ServicioChat]: Iniciando grabación de audio a través del gestor");
        gestorAudio.iniciarGrabacion();
        System.out.println("✅ [ServicioChat]: Grabación de audio iniciada");
    }

    @Override
    public CompletableFuture<String> detenerYSubirGrabacion() {
        System.out.println("➡️ [ServicioChat]: Deteniendo grabación y subiendo audio");

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Detener la grabación
                File archivoAudio = gestorAudio.detenerGrabacion();

                if (archivoAudio == null || !archivoAudio.exists()) {
                    System.err.println("❌ [ServicioChat]: No se pudo obtener el archivo de audio");
                    throw new RuntimeException("No se pudo obtener el archivo de audio");
                }

                System.out.println("📁 [ServicioChat]: Audio grabado - Tamaño: " + archivoAudio.length() + " bytes");
                return archivoAudio;

            } catch (Exception e) {
                System.err.println("❌ [ServicioChat]: Error al detener grabación: " + e.getMessage());
                throw new RuntimeException("Error al detener grabación", e);
            }
        }).thenCompose(archivoAudio -> {
            System.out.println("📤 [ServicioChat]: Subiendo audio a través de la fachada de archivos");
            return fachadaArchivos.subirArchivo(archivoAudio);
        }).thenApply(audioFileId -> {
            System.out.println("✅ [ServicioChat]: Audio subido exitosamente - FileId: " + audioFileId);
            return audioFileId;
        }).exceptionally(ex -> {
            System.err.println("❌ [ServicioChat]: Error al detener/subir audio: " + ex.getMessage());
            throw new RuntimeException("Error al procesar audio", ex);
        });
    }

    @Override
    public void cancelarGrabacion() {
        System.out.println("➡️ [ServicioChat]: Cancelando grabación de audio");
        gestorAudio.cancelarGrabacion();
        System.out.println("✅ [ServicioChat]: Grabación cancelada");
    }

    @Override
    public void reproducirAudio(String fileId) {
        System.out.println("⚠️ [ServicioChat]: Delegando reproducción a FachadaArchivos");
        fachadaArchivos.reproducirAudio(fileId);
    }

    @Override
    public CompletableFuture<Void> reproducirAudioEnMemoria(String fileId) {
        System.out.println("➡️ [ServicioChat]: Delegando reproducción de audio a FachadaArchivos - FileId: " + fileId);
        // ✅ CORRECCIÓN: Delegar completamente a la fachada (respeta arquitectura)
        return fachadaArchivos.reproducirAudio(fileId);
    }

    /**
     * Descarga automáticamente un archivo de audio a la carpeta local.
     */
    public CompletableFuture<File> descargarAudioALocal(String fileId) {
        System.out.println("➡️ [ServicioChat]: Delegando descarga de audio a FachadaArchivos - FileId: " + fileId);
        // ✅ CORRECCIÓN: Delegar completamente a la fachada (respeta arquitectura)
        return fachadaArchivos.descargarAudioALocal(fileId);
    }

    /**
     * ✅ NUEVO: Guarda un audio que viene en Base64 (desde PUSH del servidor) como archivo físico
     * y en la base de datos local para uso offline.
     */
    @Override
    public CompletableFuture<File> guardarAudioDesdeBase64(String base64Audio, String mensajeId) {
        System.out.println("➡️ [ServicioChat]: Delegando guardado de audio desde Base64 a FachadaArchivos");
        System.out.println("   → MensajeId: " + mensajeId);
        return fachadaArchivos.guardarAudioDesdeBase64(base64Audio, mensajeId);
    }

    /**
     * Extrae el nombre del archivo desde un fileId del formato "audio_files/user_timestamp.wav"
     */
    private String extraerNombreDeFileId(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return "audio_unknown.wav";
        }

        // Si contiene '/', tomar la última parte
        if (fileId.contains("/")) {
            String[] partes = fileId.split("/");
            return partes[partes.length - 1];
        }

        return fileId;
    }


    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("📢 [ServicioChat]: Recibida notificación de la fachada - Tipo: " + tipoDeDato);

        // ✅ CORRECCIÓN: Filtrar solo notificaciones relacionadas con MENSAJES
        // No procesar notificaciones de actualización de contactos (eso lo hace ServicioContactos)
        if ("ACTUALIZAR_CONTACTOS".equals(tipoDeDato)) {
            System.out.println("⏭️ [ServicioChat]: Ignorando notificación de actualización de contactos (no es responsabilidad de ServicioChat)");
            return;
        }

        // ✅ NUEVO: Procesar mensajes de audio PUSH que vienen con Base64
        if ("NUEVO_MENSAJE_AUDIO_PRIVADO".equals(tipoDeDato) && datos instanceof dto.vistaContactoChat.DTOMensaje) {
            procesarAudioPush((dto.vistaContactoChat.DTOMensaje) datos);
            return;
        }

        // Pasa solo notificaciones relevantes de mensajes hacia arriba a la vista.
        notificarObservadores(tipoDeDato, datos);
    }

    /**
     * Procesa un mensaje de audio PUSH que viene con contenido Base64.
     * Guarda el audio como archivo local y actualiza el mensaje antes de notificar a la vista.
     */
    private void procesarAudioPush(dto.vistaContactoChat.DTOMensaje mensaje) {
        System.out.println("🎵 [ServicioChat]: Procesando audio PUSH - MensajeId: " + mensaje.getMensajeId());

        String contenido = mensaje.getContenido();

        // Verificar si el contenido es Base64 de audio
        if (contenido == null || contenido.isEmpty()) {
            System.err.println("❌ [ServicioChat]: Audio PUSH sin contenido, notificando directamente");
            notificarObservadores("NUEVO_MENSAJE_AUDIO_PRIVADO", mensaje);
            return;
        }

        // Si el contenido es un fileId corto (no Base64), notificar directamente
        boolean esBase64 = contenido.startsWith("UklGR") ||
                          contenido.startsWith("data:audio/") ||
                          contenido.length() > 1000;

        if (!esBase64) {
            System.out.println("✅ [ServicioChat]: Audio PUSH ya tiene fileId, notificando directamente");
            notificarObservadores("NUEVO_MENSAJE_AUDIO_PRIVADO", mensaje);
            return;
        }

        // El contenido es Base64, necesitamos guardarlo localmente
        System.out.println("💾 [ServicioChat]: Audio PUSH contiene Base64, guardando localmente...");

        // Extraer Base64 puro (eliminar prefijo data:audio si existe)
        String base64Puro = contenido;
        if (contenido.startsWith("data:audio/")) {
            int comaIndex = contenido.indexOf(",");
            if (comaIndex != -1) {
                base64Puro = contenido.substring(comaIndex + 1);
            }
        }

        // Guardar el audio usando la fachada
        guardarAudioDesdeBase64(base64Puro, mensaje.getMensajeId())
            .thenAccept(archivoGuardado -> {
                if (archivoGuardado != null && archivoGuardado.exists()) {
                    // Actualizar el mensaje con el fileId local
                    String fileId = "audios_push/" + archivoGuardado.getName();
                    mensaje.setFileId(fileId);
                    mensaje.setContenido(fileId); // Actualizar contenido con el path local

                    System.out.println("✅ [ServicioChat]: Audio guardado exitosamente");
                    System.out.println("   → Archivo: " + archivoGuardado.getAbsolutePath());
                    System.out.println("   → FileId: " + fileId);

                    // Ahora sí, notificar a la vista con el mensaje actualizado
                    notificarObservadores("NUEVO_MENSAJE_AUDIO_PRIVADO", mensaje);
                } else {
                    System.err.println("❌ [ServicioChat]: No se pudo guardar el audio");
                    // Notificar con el Base64 original (la vista puede intentar manejarlo)
                    notificarObservadores("NUEVO_MENSAJE_AUDIO_PRIVADO", mensaje);
                }
            })
            .exceptionally(ex -> {
                System.err.println("❌ [ServicioChat]: Error al guardar audio: " + ex.getMessage());
                ex.printStackTrace();
                // Notificar con el mensaje original (mejor que no notificar nada)
                notificarObservadores("NUEVO_MENSAJE_AUDIO_PRIVADO", mensaje);
                return null;
            });
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("🔔 [ServicioChat]: Observador registrado (Vista) - Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🔕 [ServicioChat]: Observador removido - Total: " + observadores.size());
    }

    // Este método es necesario para notificar a la VISTA (su observador).
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📣 [ServicioChat]: Notificando a " + observadores.size() + " observadores (Vista) - Tipo: " + tipoDeDato);
        for (IObservador obs : observadores) {
            obs.actualizar(tipoDeDato, datos);
        }
    }
}
