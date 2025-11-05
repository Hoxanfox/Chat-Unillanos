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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Implementación del servicio de chat que AHORA depende de la FachadaContactos.
 */
public class ServicioChatImpl implements IServicioChat, IObservador {

    private final List<IObservador> observadores = new ArrayList<>();
    // CORRECCIÓN: La dependencia ahora es con la fachada de contactos.
    private final IFachadaContactos fachadaContactos;
    private final IFachadaArchivos fachadaArchivos;
    private final GestorAudio gestorAudio;

    public ServicioChatImpl() {
        System.out.println("🔧 [ServicioChat]: Inicializando servicio de chat...");

        // Obtiene las fachadas desde la Fachada General
        this.fachadaContactos = FachadaGeneralImpl.getInstancia().getFachadaContactos();
        this.fachadaArchivos = FachadaGeneralImpl.getInstancia().getFachadaArchivos();
        this.gestorAudio = GestorAudio.getInstancia();

        // Se suscribe a la fachada de contactos para recibir notificaciones (de nuevos mensajes, etc.)
        this.fachadaContactos.registrarObservador(this);

        System.out.println("✅ [ServicioChat]: Servicio inicializado con FachadaContactos, FachadaArchivos y GestorAudio");
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
        System.out.println("⚠️ [ServicioChat]: Método LEGACY - Usando reproducción en memoria en su lugar");
        // Delegar al nuevo método de reproducción en memoria
        reproducirAudioEnMemoria(fileId);
    }

    @Override
    public CompletableFuture<Void> reproducirAudioEnMemoria(String fileId) {
        System.out.println("➡️ [ServicioChat]: Iniciando reproducción de audio EN MEMORIA - FileId: " + fileId);

        // 1. Descargar el audio en memoria (como bytes)
        return fachadaArchivos.descargarArchivoEnMemoria(fileId)
            .thenCompose(audioBytes -> {
                System.out.println("✅ [ServicioChat]: Audio descargado en memoria - Tamaño: " + audioBytes.length + " bytes");

                // 2. Reproducir directamente desde los bytes usando GestorAudio
                try {
                    reproducirAudioDesdeBytes(audioBytes);
                    return CompletableFuture.completedFuture(null);
                } catch (Exception e) {
                    System.err.println("❌ [ServicioChat]: Error al reproducir audio desde bytes: " + e.getMessage());
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    future.completeExceptionally(e);
                    return future;
                }
            })
            .exceptionally(ex -> {
                System.err.println("❌ [ServicioChat]: Error al descargar/reproducir audio en memoria: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
    }

    /**
     * Reproduce audio directamente desde un array de bytes en memoria usando GestorAudio
     */
    private void reproducirAudioDesdeBytes(byte[] audioBytes) throws Exception {
        System.out.println("🔊 [ServicioChat]: Reproduciendo audio desde bytes - Tamaño: " + audioBytes.length);

        new Thread(() -> {
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(
                    new java.io.ByteArrayInputStream(audioBytes))) {

                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
                audioLine.open(format);
                audioLine.start();

                System.out.println("▶️ [ServicioChat]: Reproducción iniciada desde memoria");

                byte[] bufferBytes = new byte[4096];
                int readBytes = 0;

                while ((readBytes = audioStream.read(bufferBytes)) != -1) {
                    audioLine.write(bufferBytes, 0, readBytes);
                }

                audioLine.drain();
                audioLine.stop();
                audioLine.close();

                System.out.println("✅ [ServicioChat]: Reproducción completada desde memoria");

            } catch (Exception e) {
                System.err.println("❌ [ServicioChat]: Error durante reproducción desde memoria: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Reproduce un archivo de audio WAV usando Java Sound API
     */
    private void reproducirArchivoAudio(File archivoAudio) throws Exception {
        System.out.println("🔊 [ServicioChat]: Reproduciendo audio - " + archivoAudio.getName());

        new Thread(() -> {
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(archivoAudio)) {
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
                audioLine.open(format);
                audioLine.start();

                System.out.println("▶️ [ServicioChat]: Reproducción iniciada");

                byte[] bufferBytes = new byte[4096];
                int readBytes = 0;

                while ((readBytes = audioStream.read(bufferBytes)) != -1) {
                    audioLine.write(bufferBytes, 0, readBytes);
                }

                audioLine.drain();
                audioLine.stop();
                audioLine.close();

                System.out.println("✅ [ServicioChat]: Reproducción completada");

            } catch (Exception e) {
                System.err.println("❌ [ServicioChat]: Error durante reproducción: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("📢 [ServicioChat]: Recibida notificación de la fachada - Tipo: " + tipoDeDato);
        // Pasa la notificación (ej. "NUEVO_MENSAJE_PRIVADO") hacia arriba a la vista.
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
