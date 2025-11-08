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

        // Pasa solo notificaciones relevantes de mensajes hacia arriba a la vista.
        notificarObservadores(tipoDeDato, datos);
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
