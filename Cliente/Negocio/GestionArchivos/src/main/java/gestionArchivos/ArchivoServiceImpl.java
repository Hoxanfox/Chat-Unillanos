package gestionArchivos;

import dominio.Archivo;
import repositorio.archivo.IRepositorioArchivo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de archivos que coordina entre el repositorio
 * (metadatos/caché) y el gestor de archivos (descarga desde servidor).
 * 
 * Esta clase NO almacena el binario completo en la BD, solo metadatos.
 * Los archivos físicos se guardan en el sistema de archivos local.
 */
public class ArchivoServiceImpl implements IArchivoService {

    private final IRepositorioArchivo repositorioArchivo;
    private final IGestionArchivos gestionArchivos;
    private final File directorioRaiz;

    /**
     * Constructor principal con todas las dependencias.
     * 
     * @param repositorioArchivo Repositorio para gestionar metadatos de archivos
     * @param gestionArchivos Gestor para descargar archivos desde el servidor
     * @param directorioRaiz Directorio raíz donde se almacenarán los archivos
     */
    public ArchivoServiceImpl(IRepositorioArchivo repositorioArchivo, 
                             IGestionArchivos gestionArchivos,
                             File directorioRaiz) {
        this.repositorioArchivo = repositorioArchivo;
        this.gestionArchivos = gestionArchivos;
        this.directorioRaiz = directorioRaiz;
        
        System.out.println("[ArchivoService] Servicio inicializado con directorio raíz: " + directorioRaiz.getAbsolutePath());
    }

    /**
     * Constructor con directorio por defecto.
     */
    public ArchivoServiceImpl(IRepositorioArchivo repositorioArchivo, 
                             IGestionArchivos gestionArchivos) {
        this(repositorioArchivo, gestionArchivos, new File("data/archivos"));
    }

    @Override
    public CompletableFuture<File> obtenerPorFileId(String fileId, File directorioDestino) {
        // Validar parámetros
        if (fileId == null || fileId.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("El fileId no puede ser nulo o vacío")
            );
        }

        System.out.println("[ArchivoService] Solicitando archivo con fileId: " + fileId);
        System.out.println("[ArchivoService] Directorio destino: " + directorioDestino.getAbsolutePath());

        CompletableFuture<File> resultado = new CompletableFuture<>();

        // Asegurar que el directorio destino existe
        try {
            crearDirectorioSiNoExiste(directorioDestino);
        } catch (IOException e) {
            System.err.println("[ArchivoService] Error al crear directorio: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }

        // Verificar si el archivo ya existe localmente
        repositorioArchivo.existe(fileId)
            .thenCompose(existe -> {
                if (existe) {
                    System.out.println("[ArchivoService] El archivo existe en el repositorio, verificando en disco...");
                    return verificarYObtenerArchivoLocal(fileId, directorioDestino);
                } else {
                    System.out.println("[ArchivoService] El archivo NO existe localmente, iniciando descarga...");
                    return descargarDesdeServidor(fileId, directorioDestino);
                }
            })
            .thenAccept(archivo -> {
                if (archivo != null && archivo.exists()) {
                    System.out.println("[ArchivoService] ✅ Archivo obtenido exitosamente: " + archivo.getAbsolutePath());
                    resultado.complete(archivo);
                } else {
                    System.err.println("[ArchivoService] ❌ No se pudo obtener el archivo");
                    resultado.completeExceptionally(
                        new RuntimeException("No se pudo obtener el archivo: " + fileId)
                    );
                }
            })
            .exceptionally(ex -> {
                System.err.println("[ArchivoService] ❌ Error al obtener archivo: " + ex.getMessage());
                ex.printStackTrace();
                resultado.completeExceptionally(ex);
                return null;
            });

        return resultado;
    }

    @Override
    public CompletableFuture<File> obtenerPorFileId(String fileId) {
        // Determinar directorio según el tipo de archivo
        File directorio = determinarDirectorioPorTipo(fileId);
        return obtenerPorFileId(fileId, directorio);
    }

    @Override
    public CompletableFuture<Boolean> existeLocalmente(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        System.out.println("[ArchivoService] Verificando existencia local de: " + fileId);

        return repositorioArchivo.existe(fileId)
            .thenCompose(existeEnRepo -> {
                if (!existeEnRepo) {
                    System.out.println("[ArchivoService] No existe en repositorio");
                    return CompletableFuture.completedFuture(false);
                }

                return repositorioArchivo.buscarPorFileIdServidor(fileId)
                    .thenApply(archivo -> {
                        if (archivo == null) {
                            return false;
                        }

                        // Verificar si el archivo físico existe
                        File archivoFisico = construirRutaArchivo(fileId, archivo.getNombreArchivo());
                        boolean existe = archivoFisico.exists() && archivoFisico.canRead();
                        
                        System.out.println("[ArchivoService] Archivo en disco: " + 
                            (existe ? "✅ Existe" : "❌ No existe") + 
                            " - Estado en BD: " + archivo.getEstado());
                        
                        return existe && "completo".equals(archivo.getEstado());
                    });
            })
            .exceptionally(ex -> {
                System.err.println("[ArchivoService] Error al verificar existencia: " + ex.getMessage());
                return false;
            });
    }

    @Override
    public CompletableFuture<File> obtenerRutaLocal(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        System.out.println("[ArchivoService] Obteniendo ruta local de: " + fileId);

        return repositorioArchivo.buscarPorFileIdServidor(fileId)
            .thenApply(archivo -> {
                if (archivo == null) {
                    System.out.println("[ArchivoService] Archivo no encontrado en repositorio");
                    return null;
                }

                File archivoFisico = construirRutaArchivo(fileId, archivo.getNombreArchivo());
                
                if (archivoFisico.exists() && archivoFisico.canRead()) {
                    System.out.println("[ArchivoService] ✅ Ruta local encontrada: " + archivoFisico.getAbsolutePath());
                    return archivoFisico;
                }

                System.out.println("[ArchivoService] ⚠️ Archivo en BD pero no en disco físico");
                return null;
            })
            .exceptionally(ex -> {
                System.err.println("[ArchivoService] Error al obtener ruta local: " + ex.getMessage());
                return null;
            });
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Verifica si el archivo existe en disco y lo devuelve.
     * Si no existe en disco pero sí en BD, intenta regenerarlo desde la BD.
     * Si eso falla, descarga desde el servidor.
     */
    private CompletableFuture<File> verificarYObtenerArchivoLocal(String fileId, File directorioDestino) {
        return repositorioArchivo.buscarPorFileIdServidor(fileId)
            .thenCompose(archivo -> {
                if (archivo == null) {
                    System.out.println("[ArchivoService] No se encontró metadata en repositorio");
                    return descargarDesdeServidor(fileId, directorioDestino);
                }

                File archivoLocal = construirRutaArchivoEnDirectorio(
                    directorioDestino, 
                    archivo.getNombreArchivo()
                );

                // Verificar si el archivo físico existe
                if (archivoLocal.exists() && archivoLocal.canRead()) {
                    System.out.println("[ArchivoService] ✅ Archivo encontrado en disco: " + archivoLocal.getAbsolutePath());
                    return CompletableFuture.completedFuture(archivoLocal);
                }

                System.out.println("[ArchivoService] ⚠️ Archivo en BD pero no en disco físico");
                
                // Si tiene contenido Base64 en BD, intentar regenerar el archivo
                if (archivo.getContenidoBase64() != null && 
                    !archivo.getContenidoBase64().isEmpty() &&
                    "completo".equals(archivo.getEstado())) {
                    
                    System.out.println("[ArchivoService] Intentando regenerar archivo desde BD...");
                    return regenerarArchivoDesdeBase64(archivo, archivoLocal);
                }

                // Si no tiene contenido en BD, descargar desde servidor
                System.out.println("[ArchivoService] No hay contenido en BD, descargando desde servidor...");
                return descargarDesdeServidor(fileId, directorioDestino);
            });
    }

    /**
     * Descarga el archivo desde el servidor usando GestionArchivos.
     */
    private CompletableFuture<File> descargarDesdeServidor(String fileId, File directorioDestino) {
        System.out.println("[ArchivoService] 🔽 Iniciando descarga desde servidor: " + fileId);
        
        return gestionArchivos.descargarArchivo(fileId, directorioDestino)
            .thenApply(archivoDescargado -> {
                System.out.println("[ArchivoService] ✅ Descarga completada: " + archivoDescargado.getAbsolutePath());
                return archivoDescargado;
            })
            .exceptionally(ex -> {
                System.err.println("[ArchivoService] ❌ Error en descarga: " + ex.getMessage());
                throw new RuntimeException("Error al descargar archivo desde servidor: " + fileId, ex);
            });
    }

    /**
     * Regenera un archivo físico desde su contenido Base64 almacenado en BD.
     */
    private CompletableFuture<File> regenerarArchivoDesdeBase64(Archivo archivo, File archivoDestino) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("[ArchivoService] Regenerando archivo desde Base64...");
                
                byte[] contenido = Base64.getDecoder().decode(archivo.getContenidoBase64());
                Files.write(archivoDestino.toPath(), contenido);
                
                System.out.println("[ArchivoService] ✅ Archivo regenerado exitosamente: " + archivoDestino.getAbsolutePath());
                return archivoDestino;
                
            } catch (Exception e) {
                System.err.println("[ArchivoService] ❌ Error al regenerar archivo: " + e.getMessage());
                throw new RuntimeException("Error al regenerar archivo desde BD", e);
            }
        });
    }

    /**
     * Crea el directorio si no existe, manejando errores de permisos.
     */
    private void crearDirectorioSiNoExiste(File directorio) throws IOException {
        if (!directorio.exists()) {
            System.out.println("[ArchivoService] Creando directorio: " + directorio.getAbsolutePath());
            
            Path path = directorio.toPath();
            try {
                Files.createDirectories(path);
                System.out.println("[ArchivoService] ✅ Directorio creado exitosamente");
            } catch (IOException e) {
                System.err.println("[ArchivoService] ❌ Error al crear directorio: " + e.getMessage());
                throw new IOException("No se pudo crear el directorio: " + directorio.getAbsolutePath(), e);
            }
        } else if (!directorio.isDirectory()) {
            throw new IOException("La ruta existe pero no es un directorio: " + directorio.getAbsolutePath());
        } else if (!directorio.canWrite()) {
            throw new IOException("No hay permisos de escritura en el directorio: " + directorio.getAbsolutePath());
        }
    }

    /**
     * Construye la ruta completa del archivo basándose en el fileId y nombre.
     * Compatible con Linux y Windows, previene path traversal.
     */
    private File construirRutaArchivo(String fileId, String nombreArchivo) {
        try {
            // Normalizar el fileId: reemplazar backslashes por forward slashes
            String fileIdNormalizado = fileId.replace("\\", "/");

            // Prevenir path traversal eliminando ".."
            if (fileIdNormalizado.contains("..")) {
                System.err.println("[ArchivoService] ⚠️ Path traversal detectado y bloqueado: " + fileId);
                fileIdNormalizado = fileIdNormalizado.replace("..", "");
            }
            
            // Remover barras iniciales
            fileIdNormalizado = fileIdNormalizado.replaceAll("^/+", "");

            int lastSlash = fileIdNormalizado.lastIndexOf('/');

            if (lastSlash > 0) {
                // El fileId contiene subdirectorios
                String subdirectorio = fileIdNormalizado.substring(0, lastSlash);

                // Usar Path para construcción multiplataforma
                Path directorioCompleto = directorioRaiz.toPath().resolve(subdirectorio).normalize();

                // Crear directorio si no existe
                crearDirectorioSiNoExiste(directorioCompleto.toFile());

                return new File(directorioCompleto.toFile(), nombreArchivo);
            }

            return new File(directorioRaiz, nombreArchivo);

        } catch (Exception e) {
            System.err.println("[ArchivoService] Error al construir ruta: " + e.getMessage());
            // Fallback seguro
            return new File(directorioRaiz, nombreArchivo);
        }
    }

    /**
     * Construye la ruta del archivo en un directorio específico.
     */
    private File construirRutaArchivoEnDirectorio(File directorio, String nombreArchivo) {
        return new File(directorio, nombreArchivo);
    }

    /**
     * Determina el directorio apropiado según el tipo de archivo (basado en fileId).
     */
    private File determinarDirectorioPorTipo(String fileId) {
        String fileIdLower = fileId.toLowerCase();
        
        if (fileIdLower.contains("user_photo") || fileIdLower.contains("profile") || fileIdLower.contains("avatar")) {
            return new File(directorioRaiz, "user_photos");
        } else if (fileIdLower.contains("audio") || fileIdLower.endsWith(".mp3") || fileIdLower.endsWith(".wav") || 
                   fileIdLower.endsWith(".ogg") || fileIdLower.endsWith(".m4a")) {
            return new File(directorioRaiz, "audio");
        } else if (fileIdLower.contains("image") || fileIdLower.endsWith(".jpg") || fileIdLower.endsWith(".jpeg") || 
                   fileIdLower.endsWith(".png") || fileIdLower.endsWith(".gif")) {
            return new File(directorioRaiz, "images");
        } else if (fileIdLower.contains("document") || fileIdLower.endsWith(".pdf") || fileIdLower.endsWith(".doc") || 
                   fileIdLower.endsWith(".docx")) {
            return new File(directorioRaiz, "documents");
        } else {
            return new File(directorioRaiz, "otros");
        }
    }
}

