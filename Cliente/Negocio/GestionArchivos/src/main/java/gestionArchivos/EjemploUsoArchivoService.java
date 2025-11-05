package gestionArchivos;

import repositorio.archivo.IRepositorioArchivo;
import repositorio.archivo.RepositorioArchivoImpl;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Ejemplo de uso del ArchivoService para descargar fotos de perfil
 * después de la autenticación de un usuario.
 * 
 * CASO DE USO: Después de recibir la respuesta de autenticación con el fileId
 * de la foto de perfil, descargar automáticamente la imagen.
 */
public class EjemploUsoArchivoService {

    public static void main(String[] args) {
        // 1. Inicializar dependencias
        IRepositorioArchivo repositorioArchivo = new RepositorioArchivoImpl();
        IGestionArchivos gestionArchivos = new GestionArchivosImpl();
        
        // 2. Crear el servicio con directorio raíz personalizado
        File directorioRaiz = new File("data/archivos");
        IArchivoService archivoService = new ArchivoServiceImpl(
            repositorioArchivo,
            gestionArchivos,
            directorioRaiz
        );

        // 3. Ejemplo 1: Obtener archivo con directorio específico
        String fileId = "user_photos/deivid1.jpg"; // Este viene del servidor
        File directorioFotos = new File("data/archivos/user_photos");
        
        System.out.println("=== EJEMPLO 1: Descargar con directorio específico ===");
        archivoService.obtenerPorFileId(fileId, directorioFotos)
            .thenAccept(archivo -> {
                System.out.println("✅ Foto de perfil obtenida: " + archivo.getAbsolutePath());
                System.out.println("   Tamaño: " + archivo.length() + " bytes");
                System.out.println("   Puede leer: " + archivo.canRead());
                
                // Aquí puedes usar el archivo para mostrarlo en la UI
                // Por ejemplo: cargarImagenEnInterfaz(archivo);
            })
            .exceptionally(ex -> {
                System.err.println("❌ Error al obtener foto: " + ex.getMessage());
                return null;
            });

        // 4. Ejemplo 2: Obtener archivo con directorio automático
        System.out.println("\n=== EJEMPLO 2: Descargar con directorio automático ===");
        archivoService.obtenerPorFileId(fileId)
            .thenAccept(archivo -> {
                System.out.println("✅ Archivo obtenido: " + archivo.getAbsolutePath());
            })
            .exceptionally(ex -> {
                System.err.println("❌ Error: " + ex.getMessage());
                return null;
            });

        // 5. Ejemplo 3: Verificar si existe antes de descargar
        System.out.println("\n=== EJEMPLO 3: Verificar existencia local ===");
        archivoService.existeLocalmente(fileId)
            .thenAccept(existe -> {
                if (existe) {
                    System.out.println("✅ El archivo ya existe localmente");
                    // Obtener la ruta sin descargarlo
                    archivoService.obtenerRutaLocal(fileId)
                        .thenAccept(archivo -> {
                            if (archivo != null) {
                                System.out.println("   Ruta: " + archivo.getAbsolutePath());
                            }
                        });
                } else {
                    System.out.println("⚠️ El archivo no existe, se necesita descargar");
                }
            });

        // 6. Ejemplo 4: Integración con respuesta de autenticación
        System.out.println("\n=== EJEMPLO 4: Uso después de autenticación ===");
        procesarRespuestaAutenticacion(archivoService);

        // Mantener el programa corriendo para ver los resultados asincrónicos
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Simula el procesamiento de una respuesta de autenticación del servidor.
     * Este método muestra cómo integrar el ArchivoService en tu código existente.
     */
    private static void procesarRespuestaAutenticacion(IArchivoService archivoService) {
        // Datos que vienen del servidor (simulados)
        String userId = "b2c3d979-9567-451d-b0e1-f3a6b2459a01";
        String nombre = "deivid1";
        String email = "deivid@unillanos.edu.co";
        String fileId = "user_photos/deivid1.jpg";

        System.out.println("📥 Respuesta de autenticación recibida:");
        System.out.println("   UserId: " + userId);
        System.out.println("   Nombre: " + nombre);
        System.out.println("   Email: " + email);
        System.out.println("   FileId: " + fileId);

        // Descargar la foto de perfil automáticamente
        System.out.println("\n🔽 Descargando foto de perfil...");
        
        archivoService.obtenerPorFileId(fileId)
            .thenAccept(fotoPerfil -> {
                System.out.println("✅ Foto de perfil lista para usar: " + fotoPerfil.getAbsolutePath());
                
                // Aquí puedes:
                // 1. Cargar la imagen en la interfaz gráfica
                // 2. Guardar la ruta en el objeto Usuario
                // 3. Notificar a los observadores
                // 4. Actualizar la sesión actual
                
                System.out.println("\n💡 Ahora puedes usar este archivo para:");
                System.out.println("   - Mostrarlo en la UI con JavaFX ImageView");
                System.out.println("   - Guardarlo en el objeto Usuario");
                System.out.println("   - Cachear la ruta para acceso rápido");
            })
            .exceptionally(ex -> {
                System.err.println("❌ Error al descargar foto de perfil: " + ex.getMessage());
                System.out.println("💡 Puedes usar una imagen por defecto");
                return null;
            });
    }

    /**
     * Ejemplo de integración con tu código de autenticación existente.
     * Este método se llamaría en AutenticarUsuario después de recibir la respuesta.
     */
    public static CompletableFuture<File> descargarFotoPerfilUsuario(
            IArchivoService archivoService,
            String fileId) {
        
        if (fileId == null || fileId.isEmpty()) {
            System.out.println("⚠️ Usuario sin foto de perfil");
            return CompletableFuture.completedFuture(null);
        }

        System.out.println("🔽 [AutenticarUsuario] Descargando foto de perfil: " + fileId);

        return archivoService.obtenerPorFileId(fileId)
            .thenApply(archivo -> {
                System.out.println("✅ [AutenticarUsuario] Foto de perfil descargada: " + archivo.getAbsolutePath());
                return archivo;
            })
            .exceptionally(ex -> {
                System.err.println("❌ [AutenticarUsuario] Error al descargar foto: " + ex.getMessage());
                // Retornar null para usar foto por defecto
                return null;
            });
    }

    /**
     * Ejemplo de cómo verificar múltiples archivos en paralelo.
     */
    public static CompletableFuture<Void> verificarVariosArchivos(
            IArchivoService archivoService,
            String... fileIds) {
        
        CompletableFuture<?>[] futuros = new CompletableFuture[fileIds.length];
        
        for (int i = 0; i < fileIds.length; i++) {
            final String fileId = fileIds[i];
            futuros[i] = archivoService.existeLocalmente(fileId)
                .thenAccept(existe -> {
                    System.out.println(fileId + " -> " + (existe ? "✅ Local" : "❌ Necesita descarga"));
                });
        }

        return CompletableFuture.allOf(futuros);
    }
}

