package gestionArchivos;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Gestor para reproducir archivos de audio descargados en memoria.
 * Permite reproducir audio sin necesidad de guardarlo en disco.
 */
public class GestorAudio {

    private final IGestionArchivos gestionArchivos;
    private Clip clipActual;
    private boolean reproduciendose;

    public GestorAudio(IGestionArchivos gestionArchivos) {
        this.gestionArchivos = gestionArchivos;
        this.reproduciendose = false;
        System.out.println("[GestorAudio] Componente inicializado");
    }

    /**
     * Descarga y reproduce un archivo de audio en memoria.
     *
     * @param fileId ID del archivo de audio en el servidor
     * @return CompletableFuture que se completa cuando el audio está listo para reproducirse
     */
    public CompletableFuture<Void> reproducirAudio(String fileId) {
        System.out.println("[GestorAudio] Iniciando reproducción de audio: " + fileId);

        return gestionArchivos.descargarArchivoEnMemoria(fileId)
                .thenAccept(audioBytes -> {
                    try {
                        reproducirDesdeBytes(audioBytes);
                    } catch (Exception e) {
                        System.err.println("[GestorAudio] ERROR al reproducir audio: " + e.getMessage());
                        throw new RuntimeException("Error al reproducir audio", e);
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("[GestorAudio] ERROR al descargar/reproducir audio: " + ex.getMessage());
                    return null;
                });
    }

    /**
     * Reproduce audio desde un array de bytes en memoria.
     *
     * @param audioBytes Los bytes del archivo de audio
     * @throws UnsupportedAudioFileException Si el formato de audio no es soportado
     * @throws IOException Si hay un error de I/O
     * @throws LineUnavailableException Si la línea de audio no está disponible
     */
    private void reproducirDesdeBytes(byte[] audioBytes) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        System.out.println("[GestorAudio] Reproduciendo audio desde memoria - Tamaño: " + audioBytes.length + " bytes");

        // Detener audio actual si existe
        detener();

        // Crear stream de audio desde los bytes
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bais);

        // Obtener formato y crear clip
        AudioFormat format = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);

        clipActual = (Clip) AudioSystem.getLine(info);
        clipActual.open(audioInputStream);

        // Agregar listener para cuando termine
        clipActual.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                System.out.println("[GestorAudio] Reproducción finalizada");
                reproduciendose = false;
            }
        });

        // Iniciar reproducción
        clipActual.start();
        reproduciendose = true;
        System.out.println("[GestorAudio] Reproducción iniciada - Duración: " + 
                         (clipActual.getMicrosecondLength() / 1000000.0) + " segundos");
    }

    /**
     * Pausa la reproducción actual.
     */
    public void pausar() {
        if (clipActual != null && reproduciendose) {
            clipActual.stop();
            reproduciendose = false;
            System.out.println("[GestorAudio] Reproducción pausada");
        }
    }

    /**
     * Reanuda la reproducción pausada.
     */
    public void reanudar() {
        if (clipActual != null && !reproduciendose) {
            clipActual.start();
            reproduciendose = true;
            System.out.println("[GestorAudio] Reproducción reanudada");
        }
    }

    /**
     * Detiene completamente la reproducción actual.
     */
    public void detener() {
        if (clipActual != null) {
            clipActual.stop();
            clipActual.close();
            reproduciendose = false;
            System.out.println("[GestorAudio] Reproducción detenida y recursos liberados");
        }
    }

    /**
     * Verifica si hay audio reproduciéndose actualmente.
     *
     * @return true si hay audio reproduciéndose, false en caso contrario
     */
    public boolean estaReproduciendo() {
        return reproduciendose;
    }

    /**
     * Obtiene la posición actual de reproducción en microsegundos.
     *
     * @return La posición actual, o 0 si no hay audio cargado
     */
    public long getPosicionActual() {
        return clipActual != null ? clipActual.getMicrosecondPosition() : 0;
    }

    /**
     * Obtiene la duración total del audio en microsegundos.
     *
     * @return La duración total, o 0 si no hay audio cargado
     */
    public long getDuracionTotal() {
        return clipActual != null ? clipActual.getMicrosecondLength() : 0;
    }

    /**
     * Establece la posición de reproducción en microsegundos.
     *
     * @param microsegundos La posición deseada
     */
    public void setPosicion(long microsegundos) {
        if (clipActual != null) {
            clipActual.setMicrosecondPosition(microsegundos);
            System.out.println("[GestorAudio] Posición establecida a: " + (microsegundos / 1000000.0) + " segundos");
        }
    }

    /**
     * Libera todos los recursos utilizados por el gestor de audio.
     */
    public void dispose() {
        detener();
        System.out.println("[GestorAudio] Recursos liberados");
    }
}

