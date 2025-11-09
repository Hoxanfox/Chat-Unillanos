package fachada.gestionArchivos;

import gestionArchivos.ArchivoServiceImpl;
import gestionArchivos.GestionArchivosImpl;
import gestionArchivos.IArchivoService;
import gestionArchivos.IGestionArchivos;
import repositorio.archivo.IRepositorioArchivo;
import repositorio.archivo.RepositorioArchivoImpl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada que gestiona las operaciones de archivos.
 * Delega la lógica al componente de gestión correspondiente.
 */
public class FachadaArchivosImpl implements IFachadaArchivos {

    // La fachada depende del componente de gestión.
    private final IGestionArchivos gestionArchivos;

    // Servicio especializado para obtener archivos con caché
    private final IArchivoService archivoService;

    public FachadaArchivosImpl() {
        // Asegurar que los directorios base existan ANTES de inicializar servicios
        asegurarDirectoriosBase();

        // En una aplicación real, esto se inyectaría.
        this.gestionArchivos = new GestionArchivosImpl();

        // Inicializar el servicio de archivos con repositorio
        IRepositorioArchivo repositorioArchivo = new RepositorioArchivoImpl();
        this.archivoService = new ArchivoServiceImpl(
            repositorioArchivo,
            gestionArchivos,
            new File("data/archivos")
        );

        System.out.println("✅ [FachadaArchivos]: Inicializada con ArchivoService integrado");
    }

    /**
     * Asegura que los directorios base existan antes de que la interfaz los necesite.
     * Compatible con Linux y Windows.
     */
    private void asegurarDirectoriosBase() {
        try {
            // Directorio base de datos
            Path dataDir = Paths.get("data");

            // Directorio de archivos y subdirectorios
            Path archivosDir = dataDir.resolve("archivos");
            Path userPhotosDir = archivosDir.resolve("user_photos");
            Path audiosDir = archivosDir.resolve("audios");
            Path audioDir = archivosDir.resolve("audio");
            Path imagesDir = archivosDir.resolve("images");
            Path documentsDir = archivosDir.resolve("documents");
            Path otrosDir = archivosDir.resolve("otros");

            // Crear todos los directorios necesarios
            Files.createDirectories(dataDir);
            Files.createDirectories(archivosDir);
            Files.createDirectories(userPhotosDir);
            Files.createDirectories(audiosDir);
            Files.createDirectories(audioDir);
            Files.createDirectories(imagesDir);
            Files.createDirectories(documentsDir);
            Files.createDirectories(otrosDir);

            System.out.println("📁 [FachadaArchivos]: Directorios asegurados en: " + archivosDir.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ [FachadaArchivos]: Error al crear directorios base: " + e.getMessage());
            e.printStackTrace();
            // No lanzar excepción para no romper la aplicación
            // Los métodos de escritura deberían ser defensivos también
        }
    }

    @Override
    public CompletableFuture<String> subirArchivo(File archivo) {
        // Simplemente delega la llamada.
        return gestionArchivos.subirArchivo(archivo);
    }

    @Override
    public CompletableFuture<String> subirArchivoParaRegistro(File archivo) {
        // Simplemente delega la llamada.
        return gestionArchivos.subirArchivoParaRegistro(archivo);
    }

    @Override
    public CompletableFuture<File> descargarArchivo(String fileId, File directorioDestino) {
        // Delega la descarga al componente de gestión
        return gestionArchivos.descargarArchivo(fileId, directorioDestino);
    }

    @Override
    public CompletableFuture<byte[]> descargarArchivoEnMemoria(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Delegando descarga en memoria al gestor - FileId: " + fileId);
        return gestionArchivos.descargarArchivoEnMemoria(fileId);
    }

    @Override
    public CompletableFuture<Void> reproducirAudio(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Iniciando reproducción de audio - FileId: " + fileId);

        // Crear directorio de audios si no existe
        File directorioAudios = new File("data/archivos/audios");
        String nombreArchivo = extraerNombreDeFileId(fileId);
        File archivoLocal = new File(directorioAudios, nombreArchivo);

        if (archivoLocal.exists()) {
            System.out.println("✅ [FachadaArchivos]: Audio encontrado en caché, reproduciendo desde archivo local");
            return CompletableFuture.supplyAsync(() -> {
                try {
                    reproducirArchivoAudio(archivoLocal);
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException("Error al reproducir audio desde archivo local", e);
                }
            });
        }

        // Si no existe localmente, descargar en memoria y reproducir
        System.out.println("⚠️ [FachadaArchivos]: Audio no en caché, descargando en memoria");
        return gestionArchivos.descargarArchivoEnMemoria(fileId)
            .thenCompose(audioBytes -> {
                System.out.println("✅ [FachadaArchivos]: Audio descargado, reproduciendo desde memoria");
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        reproducirAudioDesdeBytes(audioBytes);
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException("Error al reproducir audio desde bytes", e);
                    }
                });
            });
    }

    @Override
    public CompletableFuture<File> descargarAudioALocal(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Descargando audio a carpeta local - FileId: " + fileId);

        File directorioAudios = new File("data/archivos/audios");
        if (!directorioAudios.exists()) {
            directorioAudios.mkdirs();
            System.out.println("📁 [FachadaArchivos]: Directorio de audios creado");
        }

        String nombreArchivo = extraerNombreDeFileId(fileId);
        File archivoLocal = new File(directorioAudios, nombreArchivo);

        if (archivoLocal.exists()) {
            System.out.println("✅ [FachadaArchivos]: Audio ya existe en caché local");
            return CompletableFuture.completedFuture(archivoLocal);
        }

        return gestionArchivos.descargarArchivo(fileId, directorioAudios);
    }

    @Override
    public CompletableFuture<File> guardarAudioDesdeBase64(String base64Audio, String mensajeId) {
        System.out.println("💾 [FachadaArchivos]: Guardando audio desde Base64 - MensajeId: " + mensajeId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Crear directorio de audios si no existe
                File directorioAudios = new File("data/archivos/audios");
                if (!directorioAudios.exists()) {
                    directorioAudios.mkdirs();
                    System.out.println("📁 [FachadaArchivos]: Directorio de audios creado");
                }

                // Generar nombre único para el archivo usando el mensajeId y timestamp
                String nombreArchivo = "audio_" + mensajeId + "_" + System.currentTimeMillis() + ".wav";
                File archivoAudio = new File(directorioAudios, nombreArchivo);

                // Decodificar Base64 a bytes
                byte[] audioBytes = java.util.Base64.getDecoder().decode(base64Audio);
                System.out.println("📦 [FachadaArchivos]: Audio decodificado - Tamaño: " + audioBytes.length + " bytes");

                // Guardar archivo físico
                java.nio.file.Files.write(archivoAudio.toPath(), audioBytes);
                System.out.println("✅ [FachadaArchivos]: Audio guardado físicamente: " + archivoAudio.getAbsolutePath());

                // Guardar en base de datos para persistencia offline
                String fileId = "audios_push/" + nombreArchivo;
                String contenidoBase64 = base64Audio;
                String hashCalculado = calcularHashSHA256(audioBytes);

                dominio.Archivo archivoDTO = new dominio.Archivo(
                    fileId,
                    nombreArchivo,
                    "audio/wav",
                    audioBytes.length
                );
                archivoDTO.setContenidoBase64(contenidoBase64);
                archivoDTO.setHashSHA256(hashCalculado);
                archivoDTO.setEstado("completo");

                // Guardar en repositorio de forma asíncrona
                repositorio.archivo.IRepositorioArchivo repositorio = new repositorio.archivo.RepositorioArchivoImpl();
                repositorio.guardar(archivoDTO)
                    .thenAccept(guardado -> {
                        if (guardado) {
                            System.out.println("✅ [FachadaArchivos]: Audio guardado en BD para uso offline");
                        } else {
                            System.err.println("⚠️ [FachadaArchivos]: No se pudo guardar en BD");
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("⚠️ [FachadaArchivos]: Error al guardar en BD: " + ex.getMessage());
                        return null;
                    });

                return archivoAudio;

            } catch (Exception e) {
                System.err.println("❌ [FachadaArchivos]: Error al guardar audio desde Base64: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Error al guardar audio desde Base64", e);
            }
        });
    }

    /**
     * Calcula el hash SHA-256 de un array de bytes
     */
    private String calcularHashSHA256(byte[] data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            System.err.println("❌ [FachadaArchivos]: Error al calcular hash: " + e.getMessage());
            return "";
        }
    }

    /**
     * Reproduce un archivo de audio WAV usando Java Sound API
     */
    private void reproducirArchivoAudio(File archivoAudio) throws Exception {
        System.out.println("🔊 [FachadaArchivos]: Reproduciendo audio desde archivo");
        new Thread(() -> {
            try (javax.sound.sampled.AudioInputStream audioStream =
                    javax.sound.sampled.AudioSystem.getAudioInputStream(archivoAudio)) {

                javax.sound.sampled.AudioFormat format = audioStream.getFormat();
                javax.sound.sampled.DataLine.Info info =
                    new javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine.class, format);

                javax.sound.sampled.SourceDataLine audioLine =
                    (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
                audioLine.open(format);
                audioLine.start();

                byte[] bufferBytes = new byte[4096];
                int readBytes;
                while ((readBytes = audioStream.read(bufferBytes)) != -1) {
                    audioLine.write(bufferBytes, 0, readBytes);
                }

                audioLine.drain();
                audioLine.stop();
                audioLine.close();
                System.out.println("✅ [FachadaArchivos]: Reproducción completada");
            } catch (Exception e) {
                System.err.println("❌ [FachadaArchivos]: Error al reproducir: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Reproduce audio desde un array de bytes en memoria
     */
    private void reproducirAudioDesdeBytes(byte[] audioBytes) throws Exception {
        System.out.println("🔊 [FachadaArchivos]: Reproduciendo audio desde memoria");
        new Thread(() -> {
            try (javax.sound.sampled.AudioInputStream audioStream =
                    javax.sound.sampled.AudioSystem.getAudioInputStream(
                        new java.io.ByteArrayInputStream(audioBytes))) {

                javax.sound.sampled.AudioFormat format = audioStream.getFormat();
                javax.sound.sampled.DataLine.Info info =
                    new javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine.class, format);

                javax.sound.sampled.SourceDataLine audioLine =
                    (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
                audioLine.open(format);
                audioLine.start();

                byte[] bufferBytes = new byte[4096];
                int readBytes;
                while ((readBytes = audioStream.read(bufferBytes)) != -1) {
                    audioLine.write(bufferBytes, 0, readBytes);
                }

                audioLine.drain();
                audioLine.stop();
                audioLine.close();
                System.out.println("✅ [FachadaArchivos]: Reproducción completada");
            } catch (Exception e) {
                System.err.println("❌ [FachadaArchivos]: Error al reproducir: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public CompletableFuture<File> obtenerArchivoPorFileId(String fileId, File directorioDestino) {
        System.out.println("➡️ [FachadaArchivos]: Obteniendo archivo - FileId: " + fileId);
        System.out.println("   Directorio destino: " + directorioDestino.getAbsolutePath());

        // Delega al servicio especializado que maneja caché y descarga inteligente
        return archivoService.obtenerPorFileId(fileId, directorioDestino)
            .thenApply(archivo -> {
                System.out.println("✅ [FachadaArchivos]: Archivo obtenido exitosamente");
                return archivo;
            })
            .exceptionally(ex -> {
                System.err.println("❌ [FachadaArchivos]: Error al obtener archivo: " + ex.getMessage());
                throw new RuntimeException("Error al obtener archivo: " + fileId, ex);
            });
    }

    @Override
    public CompletableFuture<File> obtenerArchivoPorFileId(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Obteniendo archivo con directorio automático - FileId: " + fileId);

        // Delega al servicio especializado usando directorio automático
        return archivoService.obtenerPorFileId(fileId)
            .thenApply(archivo -> {
                System.out.println("✅ [FachadaArchivos]: Archivo obtenido exitosamente");
                return archivo;
            })
            .exceptionally(ex -> {
                System.err.println("❌ [FachadaArchivos]: Error al obtener archivo: " + ex.getMessage());
                throw new RuntimeException("Error al obtener archivo: " + fileId, ex);
            });
    }

    @Override
    public CompletableFuture<Boolean> existeArchivoLocalmente(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Verificando existencia local - FileId: " + fileId);
        return archivoService.existeLocalmente(fileId);
    }

    @Override
    public CompletableFuture<Boolean> existeLocalmente(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Verificando existencia local - FileId: " + fileId);
        return archivoService.existeLocalmente(fileId);
    }

    @Override
    public CompletableFuture<File> obtenerRutaLocal(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Obteniendo ruta local - FileId: " + fileId);

        // Verificar si existe localmente
        return archivoService.existeLocalmente(fileId)
            .thenCompose(existe -> {
                if (existe) {
                    // Si existe, obtenerlo del servicio
                    return archivoService.obtenerPorFileId(fileId);
                } else {
                    // Si no existe, retornar null
                    System.out.println("⚠️ [FachadaArchivos]: Archivo no existe localmente");
                    return CompletableFuture.completedFuture(null);
                }
            });
    }

    /**
     * Extrae el nombre del archivo desde un fileId
     */
    private String extraerNombreDeFileId(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return "audio_unknown.wav";
        }
        if (fileId.contains("/")) {
            String[] partes = fileId.split("/");
            return partes[partes.length - 1];
        }
        return fileId;
    }
}
