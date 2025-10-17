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
 * Implementación CORREGIDA del componente de negocio que gestiona la subida y descarga de archivos por chunks.
 * Separa claramente las acciones para subidas autenticadas vs. subidas de registro.
 */
public class GestionArchivosImpl implements IGestionArchivos {

    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IRepositorioArchivo repositorioArchivo;
    private final Gson gson;
    private static final int CHUNK_SIZE = 2097152;  // 2 MB (sincronizado con servidor)

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

    // ===== SUBIDA AUTENTICADA =====

    @Override
    public CompletableFuture<String> subirArchivo(File archivo) {
        System.out.println("[GestionArchivos] Iniciando subida AUTENTICADA de archivo: " + archivo.getName());
        return procesarSubida(archivo, false);
    }

    // ===== SUBIDA PARA REGISTRO (SIN AUTENTICACIÓN) =====

    public CompletableFuture<String> subirArchivoParaRegistro(File archivo) {
        System.out.println("[GestionArchivos] Iniciando subida para REGISTRO (sin autenticación) de archivo: " + archivo.getName());
        return procesarSubida(archivo, true);
    }

    // ===== MÉTODO UNIFICADO DE PROCESAMIENTO =====

    private CompletableFuture<String> procesarSubida(File archivo, boolean esRegistro) {
        CompletableFuture<String> futuroSubida = new CompletableFuture<>();

        try {
            byte[] fileBytes = Files.readAllBytes(archivo.toPath());
            String fileHash = calcularHashSHA256(fileBytes);
            int totalChunks = (int) Math.ceil((double) fileBytes.length / CHUNK_SIZE);

            System.out.println("[GestionArchivos] Archivo leído - Tamaño: " + fileBytes.length + 
                             " bytes, Hash: " + fileHash + ", Total chunks: " + totalChunks);

            // Usar método de inicio apropiado
            CompletableFuture<String> futuroInicio = esRegistro 
                ? iniciarSubidaParaRegistro(archivo, totalChunks)
                : iniciarSubida(archivo, totalChunks);

            futuroInicio
                .thenCompose(sessionId -> {
                    System.out.println("[GestionArchivos] Sesión iniciada con ID: " + sessionId);
                    // *** PUNTO CLAVE: Pasar el flag esRegistro a transferirChunks ***
                    return transferirChunks(sessionId, fileBytes, totalChunks, esRegistro);
                })
                .thenCompose(sessionId -> {
                    System.out.println("[GestionArchivos] Chunks transferidos. Finalizando subida...");
                    // *** PUNTO CLAVE: Usar método de finalización apropiado ***
                    return esRegistro 
                        ? finalizarSubidaParaRegistro(sessionId, fileHash)
                        : finalizarSubida(sessionId, fileHash);
                })
                .thenAccept(archivoId -> {
                    System.out.println("[GestionArchivos] Subida completada exitosamente. ID: " + archivoId);
                    futuroSubida.complete(archivoId);
                })
                .exceptionally(ex -> {
                    System.err.println("[GestionArchivos] ERROR en subida: " + ex.getMessage());
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

    // ===== MÉTODOS DE INICIO =====

    private CompletableFuture<String> iniciarSubida(File archivo, int totalChunks) {
        final String ACCION = "startFileUpload";
        System.out.println("[GestionArchivos] Registrando manejador para acción: " + ACCION);

        CompletableFuture<String> futuroSessionId = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para " + ACCION + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                String sessionId = gson.fromJson(gson.toJson(res.getData()), SessionIdResponse.class).sessionId;
                System.out.println("[GestionArchivos] SessionId obtenido: " + sessionId);
                futuroSessionId.complete(sessionId);
            } else {
                System.err.println("[GestionArchivos] ERROR en " + ACCION + ": " + res.getMessage());
                futuroSessionId.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOStartUpload payload = new DTOStartUpload(archivo.getName(), getMimeType(archivo), totalChunks);
        System.out.println("[GestionArchivos] Enviando petición " + ACCION);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroSessionId;
    }

    private CompletableFuture<String> iniciarSubidaParaRegistro(File archivo, int totalChunks) {
        final String ACCION = "uploadFileForRegistration";
        System.out.println("[GestionArchivos] Registrando manejador para acción: " + ACCION);

        CompletableFuture<String> futuroSessionId = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para " + ACCION + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                String sessionId = gson.fromJson(gson.toJson(res.getData()), SessionIdResponse.class).sessionId;
                System.out.println("[GestionArchivos] SessionId para registro obtenido: " + sessionId);
                futuroSessionId.complete(sessionId);
            } else {
                System.err.println("[GestionArchivos] ERROR en " + ACCION + ": " + res.getMessage());
                futuroSessionId.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOStartUpload payload = new DTOStartUpload(archivo.getName(), getMimeType(archivo), totalChunks);
        System.out.println("[GestionArchivos] Enviando petición " + ACCION);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroSessionId;
    }

    // ===== MÉTODOS DE TRANSFERENCIA DE CHUNKS =====

    private CompletableFuture<String> transferirChunks(String sessionId, byte[] fileBytes, int totalChunks, boolean esRegistro) {
        System.out.println("[GestionArchivos] Iniciando transferencia de " + totalChunks + " chunks para sessionId: " + sessionId);
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
            
            // *** PUNTO CLAVE: Pasar el flag esRegistro a enviarChunk ***
            futuroChunkActual = futuroChunkActual.thenCompose(v -> 
                enviarChunk(sessionId, chunkNumber, totalChunks, fileBytes.length, chunkBase64, esRegistro)
            );
        }

        futuroChunkActual.thenRun(() -> {
            System.out.println("[GestionArchivos] Todos los chunks transferidos exitosamente para sessionId: " + sessionId);
            futuroTransferencia.complete(sessionId);
        }).exceptionally(ex -> {
            System.err.println("[GestionArchivos] ERROR durante transferencia de chunks: " + ex.getMessage());
            futuroTransferencia.completeExceptionally(ex);
            return null;
        });

        return futuroTransferencia;
    }

    private CompletableFuture<Void> enviarChunk(String sessionId, int chunkNumber, int totalChunks, 
                                                 int tamanoTotal, String chunkBase64, boolean esRegistro) {
        // *** CORRECCIÓN CRÍTICA: Usar acción diferente según el tipo de subida ***
        final String ACCION = esRegistro ? "uploadFileChunkForRegistration" : "uploadFileChunk";
        final String ACCION_RESPUESTA = ACCION + "_" + sessionId + "_" + chunkNumber;
        
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

        // Crear DTO con todos los campos necesarios
        DTOUploadChunk payload = new DTOUploadChunk(sessionId, chunkNumber, chunkBase64);
        // Si tu DTO soporta más campos, agrégalos:
        // payload.setTotalChunks(totalChunks);
        // payload.setTamanoTotal(tamanoTotal);

        System.out.println("[GestionArchivos] Enviando chunk " + chunkNumber + " con acción: " + ACCION + 
                         " - SessionId: " + sessionId + ", Tamaño base64: " + chunkBase64.length());
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroChunk;
    }

    // ===== MÉTODOS DE FINALIZACIÓN =====

    private CompletableFuture<String> finalizarSubida(String sessionId, String fileHash) {
        final String ACCION = "endFileUpload";
        System.out.println("[GestionArchivos] Registrando manejador para finalizar subida: " + ACCION);

        CompletableFuture<String> futuroFinal = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para " + ACCION + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                String archivoId = gson.fromJson(gson.toJson(res.getData()), ArchivoIdResponse.class).archivoId;
                System.out.println("[GestionArchivos] Subida finalizada. Archivo ID: " + archivoId);
                futuroFinal.complete(archivoId);
            } else {
                System.err.println("[GestionArchivos] ERROR al finalizar subida: " + res.getMessage());
                futuroFinal.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOEndUpload payload = new DTOEndUpload(sessionId, fileHash);
        System.out.println("[GestionArchivos] Enviando petición " + ACCION);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroFinal;
    }

    private CompletableFuture<String> finalizarSubidaParaRegistro(String sessionId, String fileHash) {
        final String ACCION = "endFileUploadForRegistration";
        System.out.println("[GestionArchivos] Registrando manejador para finalizar subida de registro: " + ACCION);

        CompletableFuture<String> futuroFinal = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para " + ACCION + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                String archivoId = gson.fromJson(gson.toJson(res.getData()), ArchivoIdResponse.class).archivoId;
                System.out.println("[GestionArchivos] Subida de registro finalizada. Archivo ID: " + archivoId);
                futuroFinal.complete(archivoId);
            } else {
                System.err.println("[GestionArchivos] ERROR al finalizar subida de registro: " + res.getMessage());
                futuroFinal.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOEndUpload payload = new DTOEndUpload(sessionId, fileHash);
        System.out.println("[GestionArchivos] Enviando petición " + ACCION);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroFinal;
    }

    // ===== MÉTODOS AUXILIARES =====

    private String getMimeType(File file) {
        try {
            String mimeType = Files.probeContentType(file.toPath());
            System.out.println("[GestionArchivos] MIME type detectado: " + mimeType);
            return mimeType != null ? mimeType : "application/octet-stream";
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

    // ===== CLASES INTERNAS PARA RESPUESTAS =====

    private static class SessionIdResponse { 
        String sessionId; 
        String uploadId; // Alias para compatibilidad
    }
    
    private static class ArchivoIdResponse { 
        String archivoId;
        String id; // Alias para compatibilidad
        String fileName; // Para backward compatibility
    }

    // ===== MÉTODOS DE DESCARGA (sin cambios) =====

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
                                        try {
                                            File archivoFisico = new File(directorioDestino, archivo.getNombreArchivo());
                                            byte[] contenido = Base64.getDecoder().decode(archivo.getContenidoBase64());
                                            Files.write(archivoFisico.toPath(), contenido);

                                            notificarObservadores("DESCARGA_COMPLETADA", archivoFisico);
                                            futuroDescarga.complete(archivoFisico);
                                            return archivoFisico;
                                        } catch (IOException e) {
                                            System.err.println("[GestionArchivos] Error al crear archivo desde BD: " + e.getMessage());
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
                        return CompletableFuture.completedFuture(archivoExistente);
                    }

                    notificarObservadores("DESCARGA_INICIADA", fileId);

                    return solicitarInicioDescarga(fileId)
                            .thenCompose(downloadInfo -> {
                                System.out.println("[GestionArchivos] Info de descarga recibida - Archivo: " + downloadInfo.getFileName() +
                                                 ", Total chunks: " + downloadInfo.getTotalChunks());

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
                        int progreso = (chunkNumber * 100) / downloadInfo.getTotalChunks();
                        System.out.println("[GestionArchivos] Chunk " + chunkNumber + "/" +
                                         downloadInfo.getTotalChunks() + " recibido (" + progreso + "%)");
                        notificarObservadores("DESCARGA_PROGRESO", progreso);
                    })
            );
        }

        futuroChunkActual.thenRun(() -> {
            try {
                try (FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                    for (byte[] chunk : chunks) {
                        fos.write(chunk);
                    }
                }

                byte[] contenidoCompleto = Files.readAllBytes(archivoDestino.toPath());
                String contenidoBase64 = Base64.getEncoder().encodeToString(contenidoCompleto);
                String hashCalculado = calcularHashSHA256(contenidoCompleto);

                repositorioArchivo.actualizarContenido(fileId, contenidoBase64)
                        .thenRun(() -> {
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
        System.out.println("[GestionArchivos] Solicitando chunk " + chunkNumber);

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
        enviadorPeticiones.enviar(new DTORequest("requestFileChunk", payload));

        return futuroChunk;
    }

    private CompletableFuture<DTODownloadInfo> solicitarInicioDescarga(String fileId) {
        final String ACCION = "startFileDownload";
        System.out.println("[GestionArchivos] Solicitando inicio de descarga");

        CompletableFuture<DTODownloadInfo> futuroInfo = new CompletableFuture<>();

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            System.out.println("[GestionArchivos] Respuesta recibida para " + ACCION + " - Exitoso: " + res.fueExitoso());
            if (res.fueExitoso()) {
                DTODownloadInfo downloadInfo = gson.fromJson(gson.toJson(res.getData()), DTODownloadInfo.class);
                System.out.println("[GestionArchivos] DownloadInfo obtenido - ID: " + downloadInfo.getDownloadId());
                futuroInfo.complete(downloadInfo);
            } else {
                System.err.println("[GestionArchivos] ERROR en " + ACCION + ": " + res.getMessage());
                futuroInfo.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOStartDownload payload = new DTOStartDownload(fileId);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));

        return futuroInfo;
    }
}

