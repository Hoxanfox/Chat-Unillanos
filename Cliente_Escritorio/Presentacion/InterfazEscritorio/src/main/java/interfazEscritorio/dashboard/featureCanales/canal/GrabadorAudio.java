package interfazEscritorio.dashboard.featureCanales.canal;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Clase para grabar audio desde el micrófono del sistema.
 */
public class GrabadorAudio {
    
    private TargetDataLine targetLine;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private Thread recordThread;
    private boolean isRecording = false;
    private File audioFile;
    
    /**
     * Formato de audio: 16 kHz, 16 bits, mono
     */
    private AudioFormat getAudioFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1; // Mono
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
    
    /**
     * Inicia la grabación de audio.
     * @return El archivo donde se está grabando
     * @throws LineUnavailableException Si no hay línea de audio disponible
     */
    public File iniciarGrabacion() throws LineUnavailableException {
        if (isRecording) {
            throw new IllegalStateException("Ya hay una grabación en curso");
        }
        
        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Línea de audio no soportada");
        }
        
        targetLine = (TargetDataLine) AudioSystem.getLine(info);
        targetLine.open(format);
        targetLine.start();
        
        // Crear archivo temporal
        try {
            audioFile = File.createTempFile("audio_", ".wav");
            audioFile.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Error al crear archivo temporal", e);
        }
        
        isRecording = true;
        
        // Iniciar grabación en un hilo separado
        recordThread = new Thread(() -> {
            try {
                AudioSystem.write(
                    new AudioInputStream(targetLine),
                    fileType,
                    audioFile
                );
            } catch (IOException e) {
                System.err.println("Error durante la grabación: " + e.getMessage());
            }
        });
        
        recordThread.start();
        System.out.println("🎤 Grabación iniciada: " + audioFile.getAbsolutePath());
        
        return audioFile;
    }
    
    /**
     * Detiene la grabación actual.
     * @return El archivo con la grabación completa
     */
    public File detenerGrabacion() {
        if (!isRecording) {
            throw new IllegalStateException("No hay grabación en curso");
        }
        
        isRecording = false;
        
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
        }
        
        // Esperar a que el hilo termine
        if (recordThread != null) {
            try {
                recordThread.join(2000); // Esperar máximo 2 segundos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("⏹️ Grabación detenida: " + audioFile.getAbsolutePath());
        
        return audioFile;
    }
    
    /**
     * Verifica si hay una grabación en curso.
     */
    public boolean isGrabando() {
        return isRecording;
    }
    
    /**
     * Cancela la grabación actual y elimina el archivo.
     */
    public void cancelarGrabacion() {
        if (isRecording) {
            detenerGrabacion();
        }
        
        if (audioFile != null && audioFile.exists()) {
            audioFile.delete();
            audioFile = null;
        }
    }
}

