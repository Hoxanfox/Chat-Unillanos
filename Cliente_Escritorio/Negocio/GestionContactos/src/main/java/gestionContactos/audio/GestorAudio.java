package gestionContactos.audio;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gestor para grabar audio usando la API de Java Sound.
 * Graba en formato WAV y permite detener/cancelar la grabación.
 */
public class GestorAudio {
    
    private static GestorAudio instancia;
    
    private TargetDataLine microphone;
    private AudioInputStream audioInputStream;
    private File archivoTemporal;
    private Thread hiloGrabacion;
    private boolean grabando = false;
    
    // Formato de audio: 16 kHz, 16 bits, mono
    private static final AudioFormat FORMATO = new AudioFormat(
        16000.0f,  // Sample rate
        16,        // Sample size in bits
        1,         // Channels (mono)
        true,      // Signed
        false      // Big endian
    );
    
    private GestorAudio() {
        System.out.println("🎤 [GestorAudio]: Inicializando gestor de audio...");
    }
    
    public static synchronized GestorAudio getInstancia() {
        if (instancia == null) {
            instancia = new GestorAudio();
        }
        return instancia;
    }
    
    /**
     * Inicia la grabación de audio desde el micrófono.
     * @throws Exception Si hay un error al acceder al micrófono
     */
    public void iniciarGrabacion() throws Exception {
        if (grabando) {
            System.out.println("⚠️ [GestorAudio]: Ya hay una grabación en curso");
            return;
        }
        
        System.out.println("🔴 [GestorAudio]: Iniciando grabación de audio...");
        
        try {
            // Obtener el micrófono
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMATO);
            
            if (!AudioSystem.isLineSupported(info)) {
                throw new Exception("El formato de audio no es soportado por el sistema");
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(FORMATO);
            microphone.start();
            
            // Crear archivo temporal para guardar la grabación
            archivoTemporal = File.createTempFile("audio_", ".wav");
            System.out.println("📁 [GestorAudio]: Archivo temporal creado: " + archivoTemporal.getAbsolutePath());
            
            // Crear el stream de audio
            audioInputStream = new AudioInputStream(microphone);
            
            // Iniciar hilo de grabación
            grabando = true;
            hiloGrabacion = new Thread(() -> {
                try {
                    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, archivoTemporal);
                } catch (IOException e) {
                    if (grabando) {
                        System.err.println("❌ [GestorAudio]: Error durante la grabación: " + e.getMessage());
                    }
                }
            });
            hiloGrabacion.start();
            
            System.out.println("✅ [GestorAudio]: Grabación iniciada exitosamente");
            
        } catch (LineUnavailableException e) {
            System.err.println("❌ [GestorAudio]: No se pudo acceder al micrófono: " + e.getMessage());
            throw new Exception("No se pudo acceder al micrófono. Verifica los permisos.", e);
        }
    }
    
    /**
     * Detiene la grabación actual y retorna el archivo de audio.
     * @return El archivo WAV con la grabación
     * @throws Exception Si hay un error al detener la grabación
     */
    public File detenerGrabacion() throws Exception {
        if (!grabando) {
            System.out.println("⚠️ [GestorAudio]: No hay ninguna grabación en curso");
            return null;
        }
        
        System.out.println("⏹️ [GestorAudio]: Deteniendo grabación...");
        
        try {
            grabando = false;
            
            // Detener el micrófono
            if (microphone != null) {
                microphone.stop();
                microphone.close();
            }
            
            // Esperar a que termine el hilo de grabación
            if (hiloGrabacion != null && hiloGrabacion.isAlive()) {
                hiloGrabacion.join(1000); // Esperar máximo 1 segundo
            }
            
            System.out.println("✅ [GestorAudio]: Grabación detenida exitosamente");
            System.out.println("📊 [GestorAudio]: Tamaño del archivo: " + archivoTemporal.length() + " bytes");
            
            return archivoTemporal;
            
        } catch (Exception e) {
            System.err.println("❌ [GestorAudio]: Error al detener la grabación: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Cancela la grabación actual sin guardar el audio.
     */
    public void cancelarGrabacion() {
        if (!grabando) {
            System.out.println("⚠️ [GestorAudio]: No hay ninguna grabación en curso para cancelar");
            return;
        }
        
        System.out.println("❌ [GestorAudio]: Cancelando grabación...");
        
        grabando = false;
        
        // Detener el micrófono
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        
        // Eliminar archivo temporal
        if (archivoTemporal != null && archivoTemporal.exists()) {
            archivoTemporal.delete();
            System.out.println("🗑️ [GestorAudio]: Archivo temporal eliminado");
        }
        
        System.out.println("✅ [GestorAudio]: Grabación cancelada");
    }
    
    /**
     * Verifica si hay una grabación en curso.
     */
    public boolean estaGrabando() {
        return grabando;
    }
}

