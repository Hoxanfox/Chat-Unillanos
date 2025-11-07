package controlador.chat;

import observador.IObservador;
import servicio.chat.IServicioChat;
import servicio.chat.ServicioChatImpl;

import java.util.concurrent.CompletableFuture;
import java.io.File;

/**
 * Implementación del controlador que gestiona las interacciones
 * de una ventana de chat privado.
 */
public class ControladorChat implements IControladorChat {

    private final IServicioChat servicioChat;

    public ControladorChat() {
        System.out.println("🔧 [ControladorChat]: Inicializando controlador de chat...");
        this.servicioChat = new ServicioChatImpl();
        System.out.println("✅ [ControladorChat]: Controlador creado con ServicioChat");
    }

    @Override
    public void solicitarHistorial(String contactoId) {
        System.out.println("➡️ [ControladorChat]: Delegando solicitud de historial al Servicio para el contacto: " + contactoId);
        servicioChat.solicitarHistorial(contactoId);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
        System.out.println("➡️ [ControladorChat]: Delegando envío de mensaje de texto al Servicio");
        System.out.println("   → DestinatarioId: " + destinatarioId);
        System.out.println("   → Contenido: " + contenido);
        return servicioChat.enviarMensajeTexto(destinatarioId, contenido);
    }

    @Override
    public void iniciarGrabacionAudio() {
        System.out.println("➡️ [ControladorChat]: Delegando inicio de grabación al Servicio");
        try {
            servicioChat.iniciarGrabacionAudio();
            System.out.println("✅ [ControladorChat]: Grabación iniciada exitosamente");
        } catch (Exception e) {
            System.err.println("❌ [ControladorChat]: Error al iniciar grabación de audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> detenerYEnviarGrabacion(String destinatarioId) {
        System.out.println("➡️ [ControladorChat]: Delegando detención, subida y envío de audio al Servicio");
        System.out.println("   → DestinatarioId: " + destinatarioId);

        // 1. Detener y subir el audio (todo lo maneja el servicio)
        return servicioChat.detenerYSubirGrabacion()
                .thenCompose(audioFileId -> {
                    System.out.println("✅ [ControladorChat]: Audio procesado - FileId: " + audioFileId);

                    // 2. Enviar el mensaje de audio
                    System.out.println("📤 [ControladorChat]: Enviando mensaje de audio");
                    return servicioChat.enviarMensajeAudio(destinatarioId, audioFileId);
                })
                .exceptionally(ex -> {
                    System.err.println("❌ [ControladorChat]: Error al procesar/enviar audio: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    @Override
    public void cancelarGrabacion() {
        System.out.println("➡️ [ControladorChat]: Delegando cancelación de grabación al Servicio");
        servicioChat.cancelarGrabacion();
        System.out.println("✅ [ControladorChat]: Grabación cancelada");
    }

    @Override
    public void reproducirAudio(String fileId) {
        System.out.println("➡️ [ControladorChat]: Delegando reproducción de audio al Servicio");
        System.out.println("   → FileId: " + fileId);
        servicioChat.reproducirAudio(fileId);
    }

    @Override
    public CompletableFuture<Void> reproducirAudioEnMemoria(String fileId) {
        System.out.println("➡️ [ControladorChat]: Delegando reproducción de audio al Servicio - FileId: " + fileId);
        return servicioChat.reproducirAudioEnMemoria(fileId)
                .thenRun(() -> System.out.println("✅ [ControladorChat]: Reproducción completada"))
                .exceptionally(ex -> {
                    System.err.println("❌ [ControladorChat]: Error en reproducción: " + ex.getMessage());
                    return null;
                });
    }

    @Override
    public CompletableFuture<File> descargarAudioALocal(String fileId) {
        System.out.println("➡️ [ControladorChat]: Delegando descarga de audio al Servicio - FileId: " + fileId);
        return servicioChat.descargarAudioALocal(fileId)
                .thenApply(archivo -> {
                    System.out.println("✅ [ControladorChat]: Audio descargado a: " + (archivo != null ? archivo.getAbsolutePath() : "null"));
                    return archivo;
                })
                .exceptionally(ex -> {
                    System.err.println("❌ [ControladorChat]: Error al descargar audio: " + ex.getMessage());
                    return null;
                });
    }

    @Override
    public CompletableFuture<File> guardarAudioDesdeBase64(String base64Audio, String mensajeId) {
        System.out.println("➡️ [ControladorChat]: Delegando guardado de audio desde Base64 al Servicio");
        System.out.println("   → MensajeId: " + mensajeId);
        return servicioChat.guardarAudioDesdeBase64(base64Audio, mensajeId)
                .thenApply(archivo -> {
                    System.out.println("✅ [ControladorChat]: Audio guardado en: " + (archivo != null ? archivo.getAbsolutePath() : "null"));
                    return archivo;
                })
                .exceptionally(ex -> {
                    System.err.println("❌ [ControladorChat]: Error al guardar audio: " + ex.getMessage());
                    return null;
                });
    }

    @Override
    public void registrarObservador(IObservador observador) {
        System.out.println("➡️ [ControladorChat]: Delegando registro de observador (VistaChat) al Servicio");
        servicioChat.registrarObservador(observador);
    }
}
