package gestionArchivos;
import com.google.gson.Gson;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dominio.Archivo;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.gestionArchivos.*;
import observador.IObservador;
import repositorio.archivo.IRepositorioArchivo;
import repositorio.archivo.RepositorioArchivoImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del componente de negocio que gestiona la subida y descarga de archivos por chunks.
 * Implementa el patrón Observador para notificar a la UI sobre el progreso.
 * Almacena archivos descargados en la BD local con Base64 usando IDs del servidor.
 */
public class GestionArchivosImpl implements IGestionArchivos {

    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IRepositorioArchivo repositorioArchivo;
    private final Gson gson;
    private static final int CHUNK_SIZE = 256;  // 1.5 MB

    // Patrón Observador
    private final List<IObservador> observadores;

    public GestionArchivosImpl() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.repositorioArchivo = new RepositorioArchivoImpl();
        this.gson = new Gson();
        this.observadores = new ArrayList<>();
        System.out.println("[GestionArchivos] Componente inicializado. CHUNK_SIZE: " + CHUNK_SIZE + " bytes");
    }

    // Implementación del patrón Observador
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("[GestionArchivos] Observador registrado: " + observador.getClass().getSimpleName());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("[GestionArchivos] Observador removido: " + observador.getClass().getSimpleName());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("[GestionArchivos] Notificando a " + observadores.size() + " observadores - Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }

    @Override
    public CompletableFuture<String> subirArchivo(File archivo) {
        System.out.println("[GestionArchivos] Iniciando subida autenticada de archivo: " + archivo.getName());
        CompletableFuture<String> futuroSubida = new CompletableFuture<>();

        try {
            byte[] fileBytes = Files.readAllBytes(archivo.toPath());
            String fileHash = calcularHashSHA256(fileBytes);
            int totalChunks = (int) Math.ceil((double) fileBytes.length / CHUNK_SIZE);

            System.out.println("[GestionArchivos] Archivo leído - Tamaño: " + fileBytes.length + " bytes, Hash: " + fileHash + ", Total chunks: " + totalChunks);

            iniciarSubida(archivo, totalChunks)
                    .thenCompose(uploadId -> {
                        System.out.println("[GestionArchivos] Upload iniciado con ID: " + uploadId);
                        return transferirChunks(uploadId, fileBytes, totalChunks);
                    })
                    .thenCompose(uploadId -> {
                        System.out.println("[GestionArchivos] Chunks transferidos. Finalizando subida...");
                        return finalizarSubida(uploadId, fileHash);
                    })
                    .thenAccept(fileName -> {
                        System.out.println("[GestionArchivos] Subida completada exitosamente. Archivo final: " + fileName);
                        futuroSubida.complete(fileName);
                    })
                    .exceptionally(ex -> {
                        System.err.println("[GestionArchivos] ERROR en subida autenticada: " + ex.getMessage());
                        ex.printStackTrace();
                        futuroSubida.completeExceptionally(ex);
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("[GestionArchivos] ERROR al leer archivo: " + e.getMessage());
            e.printStackTrace();
            futuroSubida.completeExceptionally(e);
        }

        return futuroSubida;
    }

    public CompletableFuture<String> subirArchivoParaRegistro(File archivo) {
        System.out.println("[GestionArchivos] Iniciando subida para REGISTRO (sin autenticación) de archivo: " + archivo.getName());
        CompletableFuture<String> futuroSubida = new CompletableFuture<>();

        try {
            byte[] fileBytes = Files.readAllBytes(archivo.toPath());
            String fileHash = calcularHashSHA256(fileBytes);
            int totalChunks = (int) Math.ceil((double) fileBytes.length / CHUNK_SIZE);

            System.out.println("[GestionArchivos] Archivo leído para registro - Tamaño: " + fileBytes.length + " bytes, Hash: " + fileHash + ", Total chunks: " + totalChunks);

            iniciarSubidaParaRegistro(archivo, totalChunks)
                    .thenCompose(uploadId -> {
                        System.out.println("[GestionArchivos] Upload para registro iniciado con ID: " + uploadId);
                        return transferirChunks(uploadId, fileBytes, totalChunks);
                    })
                    .thenCompose(uploadId -> {
                        System.out.println("[GestionArchivos] Chunks para registro transferidos. Finalizando subida...");
                        return finalizarSubida(uploadId, fileHash);
                    })
                    .thenAccept(fileName -> {
                        System.out.println("[GestionArchivos] Subida para registro completada exitosamente. Archivo final: " + fileName);
                        futuroSubida.complete(fileName);
                    })
                    .exceptionally(ex -> {
                        System.err.println("[GestionArchivos] ERROR en subida para registro: " + ex.getMessage());
                        ex.printStackTrace();
                        futuroSubida.completeExceptionally(ex);
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("[GestionArchivos] ERROR al leer archivo para registro: " + e.getMessage());
            e.printStackTrace();
            futuroSubida.completeExceptionally(e);
        }

        return futuroSubida;
    }

    private CompletableFuture<String> iniciarSubida(File archivo, int totalChunks) {
        final String ACCION = "startFileUpload";
        System.out.println("[GestionArchivos] Registrando manejador para acción: " + ACCION);

        CompletableFuture<String> futuroUploadId = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para " + ACCION + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                String uploadId = gson.fromJson(gson.toJson(res.getData()), UploadIdResponse.class).uploadId;
                System.out.println("[GestionArchivos] UploadId obtenido: " + uploadId);
                futuroUploadId.complete(uploadId);
            } else {
                System.err.println("[GestionArchivos] ERROR en " + ACCION + ": " + res.getMessage());
                futuroUploadId.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOStartUpload payload = new DTOStartUpload(archivo.getName(), getMimeType(archivo), totalChunks);
        System.out.println("[GestionArchivos] Enviando petición " + ACCION + " - Archivo: " + archivo.getName() + ", MIME: " + getMimeType(archivo) + ", Chunks: " + totalChunks);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroUploadId;
    }

    private CompletableFuture<String> iniciarSubidaParaRegistro(File archivo, int totalChunks) {
        final String ACCION = "uploadFileForRegistration";
        System.out.println("[GestionArchivos] Registrando manejador para acción: " + ACCION);

        CompletableFuture<String> futuroUploadId = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para " + ACCION + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                String uploadId = gson.fromJson(gson.toJson(res.getData()), UploadIdResponse.class).uploadId;
                System.out.println("[GestionArchivos] UploadId para registro obtenido: " + uploadId);
                futuroUploadId.complete(uploadId);
            } else {
                System.err.println("[GestionArchivos] ERROR en " + ACCION + ": " + res.getMessage());
                futuroUploadId.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOStartUpload payload = new DTOStartUpload(archivo.getName(), getMimeType(archivo), totalChunks);
        System.out.println("[GestionArchivos] Enviando petición " + ACCION + " - Archivo: " + archivo.getName() + ", MIME: " + getMimeType(archivo) + ", Chunks: " + totalChunks);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroUploadId;
    }

    private CompletableFuture<String> transferirChunks(String uploadId, byte[] fileBytes, int totalChunks) {
        System.out.println("[GestionArchivos] Iniciando transferencia de " + totalChunks + " chunks para uploadId: " + uploadId);
        CompletableFuture<String> futuroTransferencia = new CompletableFuture<>();
        CompletableFuture<Void> futuroChunkActual = CompletableFuture.completedFuture(null);

        for (int i = 0; i < totalChunks; i++) {
            final int chunkNumber = i + 1;
            int offset = i * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, fileBytes.length - offset);
            byte[] chunkData = new byte[length];
            System.arraycopy(fileBytes, offset, chunkData, 0, length);
            String chunkBase64 = Base64.getEncoder().encodeToString(chunkData);

            System.out.println("[GestionArchivos] Preparando chunk " + chunkNumber + "/" + totalChunks + " - Tamaño: " + length + " bytes");
            futuroChunkActual = futuroChunkActual.thenCompose(v -> enviarChunk(uploadId, chunkNumber, chunkBase64));
        }

        futuroChunkActual.thenRun(() -> {
            System.out.println("[GestionArchivos] Todos los chunks transferidos exitosamente para uploadId: " + uploadId);
            futuroTransferencia.complete(uploadId);
        }).exceptionally(ex -> {
            System.err.println("[GestionArchivos] ERROR durante transferencia de chunks: " + ex.getMessage());
            futuroTransferencia.completeExceptionally(ex);
            return null;
        });

        return futuroTransferencia;
    }

    private CompletableFuture<Void> enviarChunk(String uploadId, int chunkNumber, String chunkBase64) {
        final String ACCION_RESPUESTA = "uploadFileChunk_" + uploadId + "_" + chunkNumber;
        System.out.println("[GestionArchivos] Registrando manejador para chunk: " + ACCION_RESPUESTA);

        CompletableFuture<Void> futuroChunk = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION_RESPUESTA, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para chunk " + chunkNumber + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                System.out.println("[GestionArchivos] Chunk " + chunkNumber + " enviado exitosamente");
                futuroChunk.complete(null);
            } else {
                System.err.println("[GestionArchivos] ERROR en chunk " + chunkNumber + ": " + res.getMessage());
                futuroChunk.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOUploadChunk payload = new DTOUploadChunk(uploadId, chunkNumber, chunkBase64);
        System.out.println("[GestionArchivos] Enviando chunk " + chunkNumber + " - UploadId: " + uploadId + ", Tamaño base64: " + chunkBase64.length());
        enviadorPeticiones.enviar(new DTORequest("uploadFileChunk", payload));

        return futuroChunk;
    }

    private CompletableFuture<String> finalizarSubida(String uploadId, String fileHash) {
        final String ACCION = "endFileUpload";
        System.out.println("[GestionArchivos] Registrando manejador para finalizar subida: " + ACCION);

        CompletableFuture<String> futuroFinal = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para " + ACCION + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                // ✅ CORRECCIÓN: Retornar fileId en lugar de fileName
                FileUploadResponse response = gson.fromJson(gson.toJson(res.getData()), FileUploadResponse.class);
                System.out.println("[GestionArchivos] Subida finalizada exitosamente");
                System.out.println("   → FileId: " + response.fileId);
                System.out.println("   → FileName: " + response.fileName);
                System.out.println("   → Size: " + response.size + " bytes");
                futuroFinal.complete(response.fileId); // ← Retornar fileId, no fileName
            } else {
                System.err.println("[GestionArchivos] ERROR al finalizar subida: " + res.getMessage());
                futuroFinal.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOEndUpload payload = new DTOEndUpload(uploadId, fileHash);
        System.out.println("[GestionArchivos] Enviando petición " + ACCION + " - UploadId: " + uploadId + ", Hash: " + fileHash);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroFinal;
    }

    private String getMimeType(File file) {
        try {
            String mimeType = Files.probeContentType(file.toPath());
            System.out.println("[GestionArchivos] MIME type detectado: " + mimeType + " para archivo: " + file.getName());
            return mimeType;
        } catch (IOException e) {
            System.out.println("[GestionArchivos] No se pudo detectar MIME type, usando por defecto: application/octet-stream");
            return "application/octet-stream";
        }
    }

    private String calcularHashSHA256(byte[] fileBytes) {
        try {
            System.out.println("[GestionArchivos] Calculando hash SHA-256...");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String hash = hexString.toString();
            System.out.println("[GestionArchivos] Hash calculado: " + hash);
            return hash;
        } catch (Exception e) {
            System.err.println("[GestionArchivos] ERROR al calcular hash: " + e.getMessage());
            throw new RuntimeException("No se pudo calcular el hash del archivo", e);
        }
    }

    private static class UploadIdResponse { String uploadId; }
    private static class FileNameResponse { String fileName; }
    private static class FileUploadResponse {
        String fileId;
        String fileName;
        long size;
        String mimeType;
        String hash;
    }

    @Override
    public CompletableFuture<File> descargarArchivo(String fileId, File directorioDestino) {
        System.out.println("[GestionArchivos] Iniciando descarga de archivo con ID: " + fileId);
        CompletableFuture<File> futuroDescarga = new CompletableFuture<>();

        // Verificar si ya existe en BD local
        repositorioArchivo.existe(fileId)
                .thenCompose(existe -> {
                    if (existe) {
                        System.out.println("[GestionArchivos] Archivo ya existe en BD local, recuperando...");
                        return repositorioArchivo.buscarPorFileIdServidor(fileId)
                                .thenApply(archivo -> {
                                    if (archivo != null && archivo.getEstado().equals("completo")) {
                                        // Archivo completo en BD, crear archivo físico desde Base64
                                        try {
                                            File archivoFisico = new File(directorioDestino, archivo.getNombreArchivo());
                                            byte[] contenido = Base64.getDecoder().decode(archivo.getContenidoBase64());
                                            Files.write(archivoFisico.toPath(), contenido);

                                            notificarObservadores("DESCARGA_COMPLETADA", archivoFisico);
                                            futuroDescarga.complete(archivoFisico);
                                            return archivoFisico;
                                        } catch (IOException e) {
                                            System.err.println("[GestionArchivos] Error al crear archivo desde BD: " + e.getMessage());
                                            // Si falla, continuar con descarga normal
                                            return null;
                                        }
                                    }
                                    return null;
                                });
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenCompose(archivoExistente -> {
                    if (archivoExistente != null) {
                        // Ya se completó desde BD local
                        return CompletableFuture.completedFuture(archivoExistente);
                    }

                    // Descargar desde servidor
                    notificarObservadores("DESCARGA_INICIADA", fileId);

                    return solicitarInicioDescarga(fileId)
                            .thenCompose(downloadInfo -> {
                                System.out.println("[GestionArchivos] Info de descarga recibida - Archivo: " + downloadInfo.getFileName() +
                                                 ", Total chunks: " + downloadInfo.getTotalChunks());

                                // Crear entrada en BD con estado "descargando"
                                Archivo archivo = new Archivo(fileId, downloadInfo.getFileName(),
                                                             downloadInfo.getMimeType(), downloadInfo.getFileSize());
                                archivo.setEstado("descargando");

                                return repositorioArchivo.guardar(archivo)
                                        .thenCompose(guardado -> {
                                            notificarObservadores("DESCARGA_INFO", downloadInfo);
                                            return recibirChunksYAlmacenar(downloadInfo, directorioDestino, fileId);
                                        });
                            })
                            .thenApply(archivoDescargado -> {
                                System.out.println("[GestionArchivos] Descarga completada: " + archivoDescargado.getAbsolutePath());
                                notificarObservadores("DESCARGA_COMPLETADA", archivoDescargado);
                                futuroDescarga.complete(archivoDescargado);
                                return archivoDescargado;
                            });
                })
                .exceptionally(ex -> {
                    System.err.println("[GestionArchivos] ERROR en descarga: " + ex.getMessage());
                    ex.printStackTrace();

                    // Actualizar estado a error en BD
                    repositorioArchivo.actualizarEstado(fileId, "error");
                    notificarObservadores("DESCARGA_ERROR", ex.getMessage());
                    futuroDescarga.completeExceptionally(ex);
                    return null;
                });

        return futuroDescarga;
    }

    private CompletableFuture<File> recibirChunksYAlmacenar(DTODownloadInfo downloadInfo, File directorioDestino, String fileId) {
        System.out.println("[GestionArchivos] Iniciando recepción de " + downloadInfo.getTotalChunks() +
                         " chunks para: " + downloadInfo.getFileName());

        CompletableFuture<File> futuroArchivo = new CompletableFuture<>();
        File archivoDestino = new File(directorioDestino, downloadInfo.getFileName());

        List<byte[]> chunks = new ArrayList<>();
        CompletableFuture<Void> futuroChunkActual = CompletableFuture.completedFuture(null);

        for (int i = 0; i < downloadInfo.getTotalChunks(); i++) {
            final int chunkNumber = i + 1;

            futuroChunkActual = futuroChunkActual.thenCompose(v ->
                solicitarChunk(downloadInfo.getDownloadId(), chunkNumber)
                    .thenAccept(chunkData -> {
                        chunks.add(chunkData);

                        // Calcular progreso
                        int progreso = (chunkNumber * 100) / downloadInfo.getTotalChunks();
                        System.out.println("[GestionArchivos] Chunk " + chunkNumber + "/" +
                                         downloadInfo.getTotalChunks() + " recibido (" + progreso + "%)");

                        notificarObservadores("DESCARGA_PROGRESO", progreso);
                    })
            );
        }

        futuroChunkActual.thenRun(() -> {
            try {
                // Ensamblar todos los chunks en el archivo final
                try (FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                    for (byte[] chunk : chunks) {
                        fos.write(chunk);
                    }
                }

                // Calcular contenido completo en Base64 para la BD
                byte[] contenidoCompleto = Files.readAllBytes(archivoDestino.toPath());
                String contenidoBase64 = Base64.getEncoder().encodeToString(contenidoCompleto);
                String hashCalculado = calcularHashSHA256(contenidoCompleto);

                // Actualizar archivo en BD con contenido completo
                repositorioArchivo.actualizarContenido(fileId, contenidoBase64)
                        .thenRun(() -> {
                            // Actualizar hash
                            repositorioArchivo.buscarPorFileIdServidor(fileId)
                                    .thenAccept(archivo -> {
                                        archivo.setHashSHA256(hashCalculado);
                                        repositorioArchivo.guardar(archivo);
                                    });
                        });

                System.out.println("[GestionArchivos] Archivo ensamblado y guardado en BD: " + archivoDestino.getAbsolutePath());
                futuroArchivo.complete(archivoDestino);

            } catch (IOException e) {
                System.err.println("[GestionArchivos] ERROR al ensamblar archivo: " + e.getMessage());
                repositorioArchivo.actualizarEstado(fileId, "error");
                futuroArchivo.completeExceptionally(e);
            }
        }).exceptionally(ex -> {
            System.err.println("[GestionArchivos] ERROR durante recepción de chunks: " + ex.getMessage());
            repositorioArchivo.actualizarEstado(fileId, "error");
            futuroArchivo.completeExceptionally(ex);
            return null;
        });

        return futuroArchivo;
    }

    private CompletableFuture<byte[]> solicitarChunk(String downloadId, int chunkNumber) {
        final String ACCION_RESPUESTA = "downloadFileChunk_" + downloadId + "_" + chunkNumber;
        System.out.println("[GestionArchivos] Solicitando chunk " + chunkNumber + " para downloadId: " + downloadId);

        CompletableFuture<byte[]> futuroChunk = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION_RESPUESTA, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para chunk " + chunkNumber + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                DTODownloadChunk chunkDto = gson.fromJson(gson.toJson(res.getData()), DTODownloadChunk.class);
                byte[] chunkData = Base64.getDecoder().decode(chunkDto.getChunkData());
                System.out.println("[GestionArchivos] Chunk " + chunkNumber + " decodificado - Tamaño: " + chunkData.length + " bytes");
                futuroChunk.complete(chunkData);
            } else {
                System.err.println("[GestionArchivos] ERROR en chunk " + chunkNumber + ": " + res.getMessage());
                futuroChunk.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTORequestChunk payload = new DTORequestChunk(downloadId, chunkNumber);
        System.out.println("[GestionArchivos] Enviando petición requestFileChunk - Chunk: " + chunkNumber);
        enviadorPeticiones.enviar(new DTORequest("requestFileChunk", payload));

        return futuroChunk;
    }

    private CompletableFuture<DTODownloadInfo> solicitarInicioDescarga(String fileId) {
        final String ACCION = "startFileDownload";
        System.out.println("[GestionArchivos] Solicitando inicio de descarga para fileId: " + fileId);

        CompletableFuture<DTODownloadInfo> futuroInfo = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para " + ACCION + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                DTODownloadInfo downloadInfo = gson.fromJson(gson.toJson(res.getData()), DTODownloadInfo.class);
                System.out.println("[GestionArchivos] DownloadInfo obtenido - ID: " + downloadInfo.getDownloadId() +
                                 ", Archivo: " + downloadInfo.getFileName());
                futuroInfo.complete(downloadInfo);
            } else {
                System.err.println("[GestionArchivos] ERROR en " + ACCION + ": " + res.getMessage());
                futuroInfo.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOStartDownload payload = new DTOStartDownload(fileId);
        System.out.println("[GestionArchivos] Enviando petición " + ACCION + " - FileId: " + fileId);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroInfo;
    }

    @Override
    public CompletableFuture<byte[]> descargarArchivoEnMemoria(String fileId) {
        System.out.println("[GestionArchivos] Iniciando descarga EN MEMORIA de archivo con ID: " + fileId);
        CompletableFuture<byte[]> futuroDescarga = new CompletableFuture<>();

        // Verificar si ya existe en BD local para usar caché
        repositorioArchivo.existe(fileId)
                .thenCompose(existe -> {
                    if (existe) {
                        System.out.println("[GestionArchivos] Archivo existe en BD local, usando caché...");
                        return repositorioArchivo.buscarPorFileIdServidor(fileId)
                                .thenApply(archivo -> {
                                    if (archivo != null && archivo.getEstado().equals("completo")) {
                                        // Archivo completo en BD, retornar bytes directamente
                                        byte[] contenido = Base64.getDecoder().decode(archivo.getContenidoBase64());
                                        System.out.println("[GestionArchivos] Archivo recuperado de caché - Tamaño: " + contenido.length + " bytes");
                                        futuroDescarga.complete(contenido);
                                        return contenido;
                                    }
                                    return null;
                                });
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenCompose(bytesExistentes -> {
                    if (bytesExistentes != null) {
                        // Ya se completó desde caché
                        return CompletableFuture.completedFuture(bytesExistentes);
                    }

                    // Descargar desde servidor en memoria
                    System.out.println("[GestionArchivos] Descargando desde servidor en memoria...");

                    return solicitarInicioDescarga(fileId)
                            .thenCompose(downloadInfo -> {
                                System.out.println("[GestionArchivos] Info de descarga recibida - Archivo: " + downloadInfo.getFileName() +
                                                 ", Total chunks: " + downloadInfo.getTotalChunks() +
                                                 ", Tamaño: " + downloadInfo.getFileSize() + " bytes");

                                return recibirChunksEnMemoria(downloadInfo, fileId);
                            })
                            .thenApply(bytes -> {
                                System.out.println("[GestionArchivos] Descarga en memoria completada - Tamaño: " + bytes.length + " bytes");
                                futuroDescarga.complete(bytes);
                                return bytes;
                            });
                })
                .exceptionally(ex -> {
                    System.err.println("[GestionArchivos] ERROR en descarga en memoria: " + ex.getMessage());
                    ex.printStackTrace();
                    futuroDescarga.completeExceptionally(ex);
                    return null;
                });

        return futuroDescarga;
    }

    /**
     * Recibe los chunks del servidor y los ensambla en memoria (sin guardar en disco).
     */
    private CompletableFuture<byte[]> recibirChunksEnMemoria(DTODownloadInfo downloadInfo, String fileId) {
        System.out.println("[GestionArchivos] Iniciando recepción de " + downloadInfo.getTotalChunks() +
                         " chunks en memoria para: " + downloadInfo.getFileName());

        CompletableFuture<byte[]> futuroBytes = new CompletableFuture<>();
        List<byte[]> chunks = new ArrayList<>();
        CompletableFuture<Void> futuroChunkActual = CompletableFuture.completedFuture(null);

        for (int i = 0; i < downloadInfo.getTotalChunks(); i++) {
            final int chunkNumber = i + 1;

            futuroChunkActual = futuroChunkActual.thenCompose(v ->
                solicitarChunk(downloadInfo.getDownloadId(), chunkNumber)
                    .thenAccept(chunkData -> {
                        chunks.add(chunkData);

                        // Calcular progreso
                        int progreso = (chunkNumber * 100) / downloadInfo.getTotalChunks();
                        System.out.println("[GestionArchivos] Chunk " + chunkNumber + "/" +
                                         downloadInfo.getTotalChunks() + " recibido en memoria (" + progreso + "%)");
                    })
            );
        }

        futuroChunkActual.thenRun(() -> {
            try {
                // Ensamblar todos los chunks en un solo array de bytes
                int totalSize = chunks.stream().mapToInt(chunk -> chunk.length).sum();
                byte[] contenidoCompleto = new byte[totalSize];

                int offset = 0;
                for (byte[] chunk : chunks) {
                    System.arraycopy(chunk, 0, contenidoCompleto, offset, chunk.length);
                    offset += chunk.length;
                }

                System.out.println("[GestionArchivos] Archivo ensamblado en memoria - Tamaño total: " + contenidoCompleto.length + " bytes");

                // Opcional: Guardar en caché local para futuros usos
                String contenidoBase64 = Base64.getEncoder().encodeToString(contenidoCompleto);
                String hashCalculado = calcularHashSHA256(contenidoCompleto);

                // Crear entrada en BD para caché
                Archivo archivo = new Archivo(fileId, downloadInfo.getFileName(),
                                             downloadInfo.getMimeType(), downloadInfo.getFileSize());
                archivo.setEstado("completo");
                archivo.setContenidoBase64(contenidoBase64);
                archivo.setHashSHA256(hashCalculado);
                repositorioArchivo.guardar(archivo);

                System.out.println("[GestionArchivos] Archivo guardado en caché local");

                futuroBytes.complete(contenidoCompleto);

            } catch (Exception e) {
                System.err.println("[GestionArchivos] ERROR al ensamblar chunks en memoria: " + e.getMessage());
                futuroBytes.completeExceptionally(e);
            }
        }).exceptionally(ex -> {
            System.err.println("[GestionArchivos] ERROR durante recepción de chunks: " + ex.getMessage());
            futuroBytes.completeExceptionally(ex);
            return null;
        });

        return futuroBytes;
    }
}
