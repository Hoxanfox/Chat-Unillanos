package gestorTranscripcion;

import configuracion.Configuracion;
import dominio.clienteServidor.Archivo;
import dominio.clienteServidor.Mensaje;
import dominio.clienteServidor.Transcripcion;
import dto.transcripcion.DTOAudioTranscripcion;
import gestorTranscripcion.servicios.ServicioTranscripcion;
import gestorTranscripcion.servicios.ServicioActualizacionAudios;
import logger.LoggerCentral;
import observador.IObservador;
import observador.ISujeto;
import repositorio.clienteServidor.ArchivoRepositorio;
import repositorio.clienteServidor.MensajeRepositorio;
import repositorio.clienteServidor.TranscripcionRepositorio;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fachada para gestionar transcripciones de audios
 * Implementa patrón Singleton y patrón Observador
 */
public class FachadaTranscripcion implements ISujeto, IObservador {

    private static final String TAG = "FachadaTranscripcion";
    private static FachadaTranscripcion instancia;
    private final List<IObservador> observadores;
    private final List<DTOAudioTranscripcion> audios;
    private final ServicioTranscripcion servicioTranscripcion;
    private final ServicioActualizacionAudios servicioActualizacion;
    private final ArchivoRepositorio archivoRepo;
    private final MensajeRepositorio mensajeRepo;
    private final TranscripcionRepositorio transcripcionRepo;

    private FachadaTranscripcion() {
        this.observadores = new ArrayList<>();
        this.audios = new ArrayList<>();
        this.servicioTranscripcion = ServicioTranscripcion.getInstance();
        this.servicioActualizacion = ServicioActualizacionAudios.getInstance();
        this.archivoRepo = new ArchivoRepositorio();
        this.mensajeRepo = new MensajeRepositorio();
        this.transcripcionRepo = new TranscripcionRepositorio();

        // Registrarse como observador del servicio de transcripción
        this.servicioTranscripcion.registrarObservador(this);

        // ✅ NUEVO: Registrarse como observador del repositorio de transcripciones
        this.transcripcionRepo.registrarObservador(this);

        LoggerCentral.info(TAG, "FachadaTranscripcion inicializada");
        LoggerCentral.info(TAG, "✓ Suscrita a eventos del repositorio de transcripciones");
    }

    public static synchronized FachadaTranscripcion getInstance() {
        if (instancia == null) {
            instancia = new FachadaTranscripcion();
        }
        return instancia;
    }

    /**
     * Inicializa el servicio de transcripción con el modelo de Vosk
     * @param rutaModelo Ruta al directorio del modelo de Vosk
     */
    public boolean inicializarModeloTranscripcion(String rutaModelo) {
        boolean exito = servicioTranscripcion.inicializarModelo(rutaModelo);
        if (exito) {
            LoggerCentral.info(TAG, "✓ Modelo de transcripción inicializado");
        } else {
            LoggerCentral.warn(TAG, "⚠ No se pudo inicializar el modelo de transcripción");
        }
        return exito;
    }

    /**
     * Inicia el servicio de actualización automática de audios
     */
    public void iniciarActualizacionAutomatica(int intervaloSegundos) {
        servicioActualizacion.iniciar(intervaloSegundos);
        LoggerCentral.info(TAG, "✓ Servicio de actualización automática iniciado");
    }

    /**
     * Detiene el servicio de actualización automática
     */
    public void detenerActualizacionAutomatica() {
        servicioActualizacion.detener();
        LoggerCentral.info(TAG, "Servicio de actualización automática detenido");
    }

    /**
     * Fuerza una actualización manual inmediata de la tabla de audios
     */
    public void actualizarTablaAudios() {
        servicioActualizacion.actualizarAhora();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
            LoggerCentral.debug(TAG, "Observador registrado");
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        LoggerCentral.debug(TAG, "Observador removido");
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            try {
                observador.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error al notificar: " + e.getMessage());
            }
        }
    }

    /**
     * Recibe eventos del ServicioTranscripcion y ArchivoRepositorio
     */
    @Override
    public void actualizar(String tipo, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido: " + tipo);

        // ✅ NUEVO: Manejar eventos del ArchivoRepositorio
        if ("AUDIO_PERSISTIDO".equals(tipo) && datos instanceof Archivo) {
            Archivo archivo = (Archivo) datos;
            LoggerCentral.info(TAG, "🔔 Archivo de audio persistido: " + archivo.getFileId());

            // Recargar la lista de audios desde la BD
            cargarAudiosDesdeBaseDatos();

            // Notificar a las vistas
            notificarObservadores("NUEVO_AUDIO_RECIBIDO", archivo.getFileId());
            return;
        }

        if ("ARCHIVO_PERSISTIDO".equals(tipo) && datos instanceof Archivo) {
            Archivo archivo = (Archivo) datos;
            LoggerCentral.info(TAG, "📁 Archivo persistido: " + archivo.getFileId());

            // Si es un archivo de audio, recargar
            if (archivo.getMimeType() != null && archivo.getMimeType().startsWith("audio/")) {
                cargarAudiosDesdeBaseDatos();
                notificarObservadores("NUEVO_AUDIO_RECIBIDO", archivo.getFileId());
            }
            return;
        }

        // Propagar eventos a los observadores de la fachada
        notificarObservadores(tipo, datos);

        // Actualizar estado interno y guardar en BD si es necesario
        if ("TRANSCRIPCION_COMPLETADA".equals(tipo) && datos instanceof DTOAudioTranscripcion) {
            DTOAudioTranscripcion audioActualizado = (DTOAudioTranscripcion) datos;
            actualizarAudioEnLista(audioActualizado);
            
            // ✅ NUEVO: Guardar la transcripción en la base de datos
            guardarTranscripcionEnBD(audioActualizado);
        }
    }

    /**
     * ✅ NUEVO: Guarda la transcripción completada en la base de datos
     */
    private void guardarTranscripcionEnBD(DTOAudioTranscripcion audio) {
        try {
            LoggerCentral.info(TAG, "💾 Guardando transcripción en BD para: " + audio.getAudioId());
            
            // Buscar el archivo correspondiente
            Archivo archivo = archivoRepo.buscarPorFileId(audio.getAudioId());
            if (archivo == null) {
                LoggerCentral.error(TAG, "No se encontró el archivo para: " + audio.getAudioId());
                return;
            }
            
            UUID archivoId;
            try {
                archivoId = UUID.fromString(archivo.getId());
            } catch (IllegalArgumentException e) {
                LoggerCentral.error(TAG, "ID de archivo inválido: " + archivo.getId());
                return;
            }
            
            // Buscar si ya existe una transcripción para este archivo
            Transcripcion existente = transcripcionRepo.buscarPorArchivoId(archivoId);
            
            if (existente != null) {
                // Actualizar transcripción existente
                existente.setTranscripcion(audio.getTranscripcion());
                existente.setEstado(Transcripcion.EstadoTranscripcion.COMPLETADA);
                existente.setFechaProcesamiento(Instant.now());
                existente.setFechaActualizacion(Instant.now());
                existente.setIdioma("es"); // Por defecto español
                
                boolean actualizado = transcripcionRepo.actualizar(existente);
                if (actualizado) {
                    LoggerCentral.info(TAG, "✅ Transcripción actualizada en BD: " + audio.getAudioId());
                } else {
                    LoggerCentral.error(TAG, "❌ Error al actualizar transcripción en BD");
                }
            } else {
                // Crear nueva transcripción
                Transcripcion nueva = new Transcripcion();
                nueva.setId(UUID.randomUUID());
                nueva.setArchivoId(archivoId);
                nueva.setTranscripcion(audio.getTranscripcion());
                nueva.setEstado(Transcripcion.EstadoTranscripcion.COMPLETADA);
                nueva.setFechaCreacion(Instant.now());
                nueva.setFechaProcesamiento(Instant.now());
                nueva.setFechaActualizacion(Instant.now());
                nueva.setIdioma("es");
                
                // Agregar mensaje_id si está disponible
                if (audio.getMensajeId() != null) {
                    try {
                        nueva.setMensajeId(UUID.fromString(audio.getMensajeId()));
                    } catch (IllegalArgumentException e) {
                        LoggerCentral.debug(TAG, "Mensaje ID inválido, ignorando: " + audio.getMensajeId());
                    }
                }
                
                boolean guardado = transcripcionRepo.guardar(nueva);
                if (guardado) {
                    LoggerCentral.info(TAG, "✅ Transcripción guardada en BD: " + audio.getAudioId());
                } else {
                    LoggerCentral.error(TAG, "❌ Error al guardar transcripción en BD");
                }
            }
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al guardar transcripción en BD: " + e.getMessage());
        }
    }

    /**
     * Actualiza un audio en la lista interna
     */
    private void actualizarAudioEnLista(DTOAudioTranscripcion audioActualizado) {
        for (int i = 0; i < audios.size(); i++) {
            if (audios.get(i).getAudioId().equals(audioActualizado.getAudioId())) {
                audios.set(i, audioActualizado);
                LoggerCentral.debug(TAG, "Audio actualizado en lista: " + audioActualizado.getAudioId());
                break;
            }
        }
    }

    /**
     * Obtiene todos los audios disponibles
     */
    public List<DTOAudioTranscripcion> obtenerAudios() {
        LoggerCentral.debug(TAG, "Obteniendo lista de audios: " + audios.size());
        return new ArrayList<>(audios);
    }

    /**
     * Agrega un nuevo audio
     */
    public void agregarAudio(DTOAudioTranscripcion audio) {
        if (audio != null && audio.getAudioId() != null) {
            audios.add(audio);
            LoggerCentral.info(TAG, "Audio agregado: " + audio.getAudioId());
            notificarObservadores("AUDIO_AGREGADO", audio);
        }
    }

    /**
     * Transcribe un audio manualmente
     */
    public boolean transcribirAudio(String audioId, String transcripcion) {
        try {
            DTOAudioTranscripcion audio = buscarAudioPorId(audioId);
            if (audio == null) {
                LoggerCentral.warn(TAG, "Audio no encontrado: " + audioId);
                return false;
            }

            audio.setTranscripcion(transcripcion);
            audio.setTranscrito(true);
            LoggerCentral.info(TAG, "Audio transcrito: " + audioId);
            notificarObservadores("AUDIO_TRANSCRITO", audio);
            return true;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al transcribir audio: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inicia transcripción automática de un audio
     */
    public boolean iniciarTranscripcionAutomatica(String audioId) {
        DTOAudioTranscripcion audio = buscarAudioPorId(audioId);
        if (audio == null) {
            LoggerCentral.warn(TAG, "Audio no encontrado: " + audioId);
            return false;
        }

        if (!servicioTranscripcion.isModeloCargado()) {
            LoggerCentral.warn(TAG, "Modelo de transcripción no disponible");
            notificarObservadores("TRANSCRIPCION_NO_DISPONIBLE", audio);
            return false;
        }

        return servicioTranscripcion.encolarTranscripcion(audio);
    }

    /**
     * Inicia transcripción automática de múltiples audios
     */
    public int iniciarTranscripcionMasiva(List<String> audioIds) {
        int exitosos = 0;
        for (String audioId : audioIds) {
            if (iniciarTranscripcionAutomatica(audioId)) {
                exitosos++;
            }
        }
        LoggerCentral.info(TAG, String.format("Transcripción masiva: %d/%d encolados", exitosos, audioIds.size()));
        return exitosos;
    }

    /**
     * Inicia transcripción de todos los audios pendientes
     */
    public int transcribirTodosPendientes() {
        List<DTOAudioTranscripcion> pendientes = audios.stream()
                .filter(a -> !a.isTranscrito())
                .collect(Collectors.toList());

        int exitosos = 0;
        for (DTOAudioTranscripcion audio : pendientes) {
            if (servicioTranscripcion.encolarTranscripcion(audio)) {
                exitosos++;
            }
        }

        LoggerCentral.info(TAG, String.format("Transcripción masiva: %d audios encolados", exitosos));
        notificarObservadores("TRANSCRIPCION_MASIVA_INICIADA", exitosos);
        return exitosos;
    }

    /**
     * Obtiene el número de transcripciones pendientes
     */
    public int getNumeroTranscripcionesPendientes() {
        return servicioTranscripcion.getNumeroAudiosPendientes();
    }

    /**
     * Verifica si el servicio de transcripción está disponible
     */
    public boolean isTranscripcionDisponible() {
        return servicioTranscripcion.isModeloCargado();
    }

    /**
     * Busca un audio por su ID
     */
    public DTOAudioTranscripcion buscarAudioPorId(String audioId) {
        return audios.stream()
                .filter(a -> a.getAudioId().equals(audioId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Filtra audios por tipo (CANAL o CONTACTO)
     */
    public List<DTOAudioTranscripcion> filtrarPorTipo(String tipo) {
        if ("CANAL".equals(tipo)) {
            return audios.stream()
                    .filter(DTOAudioTranscripcion::isEsCanal)
                    .collect(Collectors.toList());
        } else if ("CONTACTO".equals(tipo)) {
            return audios.stream()
                    .filter(a -> !a.isEsCanal())
                    .collect(Collectors.toList());
        }
        return obtenerAudios();
    }

    /**
     * ✅ NUEVO: Filtra audios por canal específico usando el repositorio
     */
    public List<DTOAudioTranscripcion> filtrarPorCanal(UUID canalId) {
        try {
            LoggerCentral.info(TAG, "🔍 Filtrando audios por canal: " + canalId);

            // Obtener transcripciones del canal desde el repositorio
            List<Transcripcion> transcripciones = transcripcionRepo.obtenerPorCanal(canalId);

            // Convertir a DTOs y buscar en la lista de audios
            return audios.stream()
                    .filter(a -> a.getCanalId() != null && a.getCanalId().equals(canalId.toString()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error filtrando por canal: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ✅ NUEVO: Filtra audios por contacto (mensajes directos entre dos usuarios)
     */
    public List<DTOAudioTranscripcion> filtrarPorContacto(UUID usuario1Id, UUID usuario2Id) {
        try {
            LoggerCentral.info(TAG, "🔍 Filtrando audios entre contactos");

            // Obtener transcripciones entre contactos desde el repositorio
            List<Transcripcion> transcripciones = transcripcionRepo.obtenerPorContactos(usuario1Id, usuario2Id);

            // Convertir a DTOs
            return audios.stream()
                    .filter(a -> !a.isEsCanal())
                    .filter(a -> {
                        String remitenteId = a.getRemitenteId();
                        String contactoId = a.getContactoId();

                        if (remitenteId == null || contactoId == null) return false;

                        return (remitenteId.equals(usuario1Id.toString()) && contactoId.equals(usuario2Id.toString())) ||
                               (remitenteId.equals(usuario2Id.toString()) && contactoId.equals(usuario1Id.toString()));
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error filtrando por contacto: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Filtra audios por estado de transcripción
     */
    public List<DTOAudioTranscripcion> filtrarPorEstado(boolean transcritos) {
        return audios.stream()
                .filter(a -> a.isTranscrito() == transcritos)
                .collect(Collectors.toList());
    }

    /**
     * Busca audios por texto
     */
    public List<DTOAudioTranscripcion> buscarAudios(String textoBusqueda) {
        String textoLower = textoBusqueda.toLowerCase();
        return audios.stream()
                .filter(a ->
                    a.getNombreRemitente().toLowerCase().contains(textoLower) ||
                    (a.getNombreCanal() != null && a.getNombreCanal().toLowerCase().contains(textoLower)) ||
                    (a.getNombreContacto() != null && a.getNombreContacto().toLowerCase().contains(textoLower)) ||
                    (a.getTranscripcion() != null && a.getTranscripcion().toLowerCase().contains(textoLower))
                )
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas de transcripción
     */
    public EstadisticasTranscripcion obtenerEstadisticas() {
        long total = audios.size();
        long transcritos = audios.stream().filter(DTOAudioTranscripcion::isTranscrito).count();
        long pendientes = total - transcritos;
        int enCola = servicioTranscripcion.getNumeroAudiosPendientes();

        return new EstadisticasTranscripcion(total, transcritos, pendientes, enCola);
    }

    /**
     * ✅ NUEVO: Carga los audios desde la base de datos
     */
    public void cargarAudiosDesdeBaseDatos() {
        try {
            LoggerCentral.info(TAG, "🔄 Cargando audios desde la base de datos...");
            audios.clear();

            // Obtener la ruta del bucket desde configuración
            String bucketPath = Configuracion.getInstance().getBucketRuta();
            
            // 1. Obtener todos los mensajes de tipo AUDIO
            List<Mensaje> mensajesAudio = mensajeRepo.obtenerTodosParaSync().stream()
                    .filter(m -> m.getTipo() == Mensaje.Tipo.AUDIO)
                    .collect(Collectors.toList());

            LoggerCentral.info(TAG, "📁 Mensajes de audio encontrados: " + mensajesAudio.size());

            // 2. Para cada mensaje de audio, buscar su archivo asociado
            for (Mensaje mensaje : mensajesAudio) {
                try {
                    String fileId = mensaje.getContenido(); // El contenido es el fileId del audio

                    // Buscar el archivo en el repositorio
                    Archivo archivo = archivoRepo.buscarPorFileId(fileId);

                    if (archivo == null) {
                        LoggerCentral.debug(TAG, "Archivo no encontrado para mensaje: " + fileId);
                        continue;
                    }

                    // Buscar transcripción si existe
                    Transcripcion transcripcion = null;
                    try {
                        UUID archivoIdUUID = UUID.fromString(archivo.getId());
                        transcripcion = transcripcionRepo.buscarPorArchivoId(archivoIdUUID);
                    } catch (Exception e) {
                        LoggerCentral.debug(TAG, "Error buscando transcripción: " + e.getMessage());
                    }

                    // Construir la ruta correcta al archivo
                    String rutaArchivo = construirRutaArchivo(bucketPath, archivo);

                    // Crear DTO
                    DTOAudioTranscripcion dto = new DTOAudioTranscripcion();
                    dto.setAudioId(archivo.getFileId());
                    dto.setMensajeId(mensaje.getId() != null ? mensaje.getId().toString() : null);
                    dto.setRutaArchivo(rutaArchivo);

                    // Convertir Instant a LocalDateTime
                    if (mensaje.getFechaEnvio() != null) {
                        dto.setFechaEnvio(java.time.LocalDateTime.ofInstant(
                            mensaje.getFechaEnvio(),
                            java.time.ZoneId.systemDefault()
                        ));
                    }

                    dto.setRemitenteId(mensaje.getRemitenteId() != null ? mensaje.getRemitenteId().toString() : null);
                    dto.setNombreRemitente("Usuario");

                    // Información de transcripción
                    if (transcripcion != null) {
                        dto.setTranscrito(transcripcion.getEstado() == Transcripcion.EstadoTranscripcion.COMPLETADA);
                        dto.setTranscripcion(transcripcion.getTranscripcion());
                    } else {
                        dto.setTranscrito(false);
                    }

                    // Determinar si es canal o contacto
                    if (mensaje.getCanalId() != null) {
                        dto.setEsCanal(true);
                        dto.setCanalId(mensaje.getCanalId().toString());
                        dto.setNombreCanal("Canal");
                    } else {
                        dto.setEsCanal(false);
                        dto.setContactoId(mensaje.getDestinatarioUsuarioId() != null ?
                            mensaje.getDestinatarioUsuarioId().toString() : null);
                        dto.setNombreContacto("Contacto");
                    }

                    audios.add(dto);
                    LoggerCentral.debug(TAG, "✓ Audio cargado: " + dto.getAudioId());

                } catch (Exception e) {
                    LoggerCentral.error(TAG, "Error procesando mensaje: " + e.getMessage());
                }
            }

            LoggerCentral.info(TAG, "✅ Audios cargados exitosamente: " + audios.size());
            notificarObservadores("AUDIOS_CARGADOS", audios.size());

        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error al cargar audios desde BD: " + e.getMessage());
        }
    }

    /**
     * ✅ NUEVO: Notifica que ha llegado un nuevo audio
     * Este método será llamado desde ServicioMensajesAudio
     */
    public void notificarNuevoAudio(String audioId) {
        try {
            LoggerCentral.info(TAG, "🔔 Nuevo audio recibido: " + audioId);

            // Recargar la lista completa
            cargarAudiosDesdeBaseDatos();

            // Notificar a los observadores (interfaz)
            notificarObservadores("NUEVO_AUDIO_RECIBIDO", audioId);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al procesar nuevo audio: " + e.getMessage());
        }
    }

    /**
     * Limpia todos los audios (útil para testing)
     */
    public void limpiar() {
        audios.clear();
        LoggerCentral.info(TAG, "Audios limpiados");
    }

    /**
     * Construye la ruta completa al archivo de audio, verificando que exista.
     * Maneja diferentes formatos de fileId (con o sin carpeta).
     */
    private String construirRutaArchivo(String bucketPath, Archivo archivo) {
        // El fileId puede ser "audio/nombre.wav" o solo "nombre.wav"
        String fileId = archivo.getFileId();
        String rutaRelativa = archivo.getRutaRelativa();
        
        // Intentar con la ruta relativa primero
        if (rutaRelativa != null && !rutaRelativa.isEmpty()) {
            File archivoConRutaRelativa = new File(bucketPath, rutaRelativa);
            if (archivoConRutaRelativa.exists()) {
                LoggerCentral.debug(TAG, "Archivo encontrado con ruta relativa: " + archivoConRutaRelativa.getPath());
                return archivoConRutaRelativa.getAbsolutePath();
            }
        }
        
        // Intentar con fileId directamente
        File archivoConFileId = new File(bucketPath, fileId);
        if (archivoConFileId.exists()) {
            LoggerCentral.debug(TAG, "Archivo encontrado con fileId: " + archivoConFileId.getPath());
            return archivoConFileId.getAbsolutePath();
        }
        
        // Si fileId no incluye "audio/", intentar agregarlo
        if (!fileId.startsWith("audio/")) {
            File archivoEnAudio = new File(bucketPath + "/audio", fileId);
            if (archivoEnAudio.exists()) {
                LoggerCentral.debug(TAG, "Archivo encontrado en carpeta audio: " + archivoEnAudio.getPath());
                return archivoEnAudio.getAbsolutePath();
            }
        }
        
        // Última opción: usar ruta por defecto
        String rutaDefault = new File(bucketPath, fileId).getAbsolutePath();
        LoggerCentral.warn(TAG, "Archivo no encontrado, usando ruta por defecto: " + rutaDefault);
        return rutaDefault;
    }

    /**
     * Detiene el servicio de transcripción
     */
    public void detener() {
        servicioTranscripcion.detener();
        detenerActualizacionAutomatica();
        LoggerCentral.info(TAG, "Fachada detenida");
    }

    // ===== CLASE INTERNA PARA ESTADÍSTICAS =====

    public static class EstadisticasTranscripcion {
        public final long total;
        public final long transcritos;
        public final long pendientes;
        public final int enCola;

        public EstadisticasTranscripcion(long total, long transcritos, long pendientes, int enCola) {
            this.total = total;
            this.transcritos = transcritos;
            this.pendientes = pendientes;
            this.enCola = enCola;
        }

        @Override
        public String toString() {
            return String.format("Total: %d | Transcritos: %d | Pendientes: %d | En cola: %d",
                    total, transcritos, pendientes, enCola);
        }
    }
}
