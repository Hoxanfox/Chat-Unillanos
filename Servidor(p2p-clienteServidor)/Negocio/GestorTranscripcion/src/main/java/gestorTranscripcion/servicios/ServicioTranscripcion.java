package gestorTranscripcion.servicios;

import dto.transcripcion.DTOAudioTranscripcion;
import logger.LoggerCentral;
import observador.IObservador;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.File;
import java.util.List;
import java.util.concurrent.*;

/**
 * Servicio de Transcripción de Audios usando Vosk
 * Maneja la transcripción automática de archivos de audio
 */
public class ServicioTranscripcion {

    private static final String TAG = "ServicioTranscripcion";
    private static ServicioTranscripcion instancia;

    // Configuración de Vosk
    private Model modelo;
    private boolean modeloCargado = false;
    private String rutaModelo;

    // Executor para transcripciones asíncronas
    private final ExecutorService executor;
    private final List<IObservador> observadores;

    // Cola de transcripciones pendientes
    private final BlockingQueue<DTOAudioTranscripcion> colaPendientes;
    private Thread trabajadorTranscripcion;
    private volatile boolean activo = false;

    private ServicioTranscripcion() {
        this.executor = Executors.newFixedThreadPool(2);
        this.observadores = new CopyOnWriteArrayList<>();
        this.colaPendientes = new LinkedBlockingQueue<>();
        LoggerCentral.info(TAG, "ServicioTranscripcion inicializado");
    }

    public static synchronized ServicioTranscripcion getInstance() {
        if (instancia == null) {
            instancia = new ServicioTranscripcion();
        }
        return instancia;
    }

    /**
     * Inicializa el servicio con el modelo de Vosk
     * @param rutaModelo Ruta al directorio del modelo de Vosk
     */
    public boolean inicializarModelo(String rutaModelo) {
        try {
            this.rutaModelo = rutaModelo;
            LibVosk.setLogLevel(LogLevel.WARNINGS);

            File modeloDir = new File(rutaModelo);
            if (!modeloDir.exists()) {
                LoggerCentral.error(TAG, "Modelo no encontrado en: " + rutaModelo);
                LoggerCentral.warn(TAG, "Descarga el modelo desde: https://alphacephei.com/vosk/models");
                return false;
            }

            this.modelo = new Model(rutaModelo);
            this.modeloCargado = true;
            LoggerCentral.info(TAG, "✓ Modelo Vosk cargado exitosamente");

            iniciarTrabajador();
            return true;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al cargar modelo Vosk: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inicia el trabajador que procesa la cola de transcripciones
     */
    private void iniciarTrabajador() {
        activo = true;
        trabajadorTranscripcion = new Thread(() -> {
            LoggerCentral.info(TAG, "Trabajador de transcripción iniciado");

            while (activo) {
                try {
                    DTOAudioTranscripcion audio = colaPendientes.poll(1, TimeUnit.SECONDS);
                    if (audio != null) {
                        procesarTranscripcion(audio);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LoggerCentral.error(TAG, "Error en trabajador: " + e.getMessage());
                }
            }

            LoggerCentral.info(TAG, "Trabajador de transcripción detenido");
        });
        trabajadorTranscripcion.setDaemon(true);
        trabajadorTranscripcion.start();
    }

    /**
     * Agrega un audio a la cola de transcripción
     */
    public boolean encolarTranscripcion(DTOAudioTranscripcion audio) {
        if (!modeloCargado) {
            LoggerCentral.warn(TAG, "Modelo no cargado. Transcripción no disponible.");
            return false;
        }

        if (audio == null || audio.getRutaArchivo() == null) {
            LoggerCentral.warn(TAG, "Audio inválido para transcripción");
            return false;
        }

        try {
            colaPendientes.put(audio);
            LoggerCentral.info(TAG, "Audio encolado para transcripción: " + audio.getAudioId());
            notificarObservadores("TRANSCRIPCION_ENCOLADA", audio);
            return true;
        } catch (InterruptedException e) {
            LoggerCentral.error(TAG, "Error al encolar audio: " + e.getMessage());
            return false;
        }
    }

    /**
     * Procesa la transcripción de un audio
     */
    private void procesarTranscripcion(DTOAudioTranscripcion audio) {
        LoggerCentral.info(TAG, "🎤 Iniciando transcripción de: " + audio.getAudioId());
        notificarObservadores("TRANSCRIPCION_INICIADA", audio);

        try {
            File archivoAudio = new File(audio.getRutaArchivo());
            if (!archivoAudio.exists()) {
                LoggerCentral.error(TAG, "Archivo no encontrado: " + audio.getRutaArchivo());
                notificarObservadores("TRANSCRIPCION_ERROR", audio);
                return;
            }

            String transcripcion = transcribirArchivo(archivoAudio);

            if (transcripcion != null && !transcripcion.isEmpty()) {
                audio.setTranscripcion(transcripcion);
                audio.setTranscrito(true);
                LoggerCentral.info(TAG, "✓ Transcripción completada: " + audio.getAudioId());
                notificarObservadores("TRANSCRIPCION_COMPLETADA", audio);
            } else {
                LoggerCentral.warn(TAG, "Transcripción vacía para: " + audio.getAudioId());
                notificarObservadores("TRANSCRIPCION_VACIA", audio);
            }

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al transcribir: " + e.getMessage());
            notificarObservadores("TRANSCRIPCION_ERROR", audio);
        }
    }

    /**
     * Transcribe un archivo de audio usando Vosk
     */
    private String transcribirArchivo(File archivoAudio) throws Exception {
        StringBuilder transcripcionCompleta = new StringBuilder();

        try (AudioInputStream ais = AudioSystem.getAudioInputStream(archivoAudio);
             BufferedInputStream bis = new BufferedInputStream(ais)) {

            // Crear recognizer con sample rate del audio
            float sampleRate = ais.getFormat().getSampleRate();
            Recognizer recognizer = new Recognizer(modelo, sampleRate);

            byte[] buffer = new byte[4096];
            int bytesRead;
            JSONParser parser = new JSONParser();

            while ((bytesRead = bis.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String resultado = recognizer.getResult();
                    JSONObject json = (JSONObject) parser.parse(resultado);
                    String texto = (String) json.get("text");

                    if (texto != null && !texto.isEmpty()) {
                        transcripcionCompleta.append(texto).append(" ");
                    }
                }
            }

            // Obtener resultado final
            String resultadoFinal = recognizer.getFinalResult();
            JSONObject jsonFinal = (JSONObject) parser.parse(resultadoFinal);
            String textoFinal = (String) jsonFinal.get("text");

            if (textoFinal != null && !textoFinal.isEmpty()) {
                transcripcionCompleta.append(textoFinal);
            }

            recognizer.close();
        }

        return transcripcionCompleta.toString().trim();
    }

    /**
     * Transcribe un audio de forma síncrona
     */
    public String transcribirAudioSincrono(String rutaArchivo) {
        if (!modeloCargado) {
            LoggerCentral.warn(TAG, "Modelo no cargado");
            return null;
        }

        try {
            File archivo = new File(rutaArchivo);
            return transcribirArchivo(archivo);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en transcripción síncrona: " + e.getMessage());
            return null;
        }
    }

    /**
     * Transcribe un audio de forma asíncrona
     */
    public CompletableFuture<String> transcribirAudioAsincrono(String rutaArchivo) {
        return CompletableFuture.supplyAsync(() -> transcribirAudioSincrono(rutaArchivo), executor);
    }

    /**
     * Obtiene el número de audios pendientes de transcripción
     */
    public int getNumeroAudiosPendientes() {
        return colaPendientes.size();
    }

    /**
     * Verifica si el modelo está cargado
     */
    public boolean isModeloCargado() {
        return modeloCargado;
    }

    /**
     * Detiene el servicio
     */
    public void detener() {
        activo = false;

        if (trabajadorTranscripcion != null) {
            trabajadorTranscripcion.interrupt();
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        if (modelo != null) {
            modelo.close();
        }

        LoggerCentral.info(TAG, "Servicio detenido");
    }

    // ===== PATRÓN OBSERVADOR =====

    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            LoggerCentral.debug(TAG, "Observador registrado");
        }
    }

    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        LoggerCentral.debug(TAG, "Observador removido");
    }

    private void notificarObservadores(String tipo, Object datos) {
        for (IObservador observador : observadores) {
            try {
                observador.actualizar(tipo, datos);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error al notificar observador: " + e.getMessage());
            }
        }
    }
}

