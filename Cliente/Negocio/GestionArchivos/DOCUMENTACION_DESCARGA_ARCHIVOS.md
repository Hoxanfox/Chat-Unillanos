    public PanelCanal() {
        gestionArchivos = FachadaNegocio.getInstance().getGestionArchivos();
        gestionArchivos.registrarObservador(this);
    }
    
    public void descargarArchivoDeCanal(String fileId, String nombreArchivo) {
        // Crear directorio específico para el canal
        File dirDestino = new File("./descargas/" + canalActual);
        dirDestino.mkdirs();
        
        // Iniciar descarga
        gestionArchivos.descargarArchivo(fileId, dirDestino)
            .thenAccept(archivo -> {
                // Opcionalmente, abrir el archivo
                Desktop.getDesktop().open(archivo);
            })
            .exceptionally(ex -> {
                // Ya se notificó al observador
                return null;
            });
    }
    
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        // Manejar eventos de descarga
        // Ver ejemplo anterior
    }
    
    @Override
    public void dispose() {
        // Importante: remover observador al cerrar
        gestionArchivos.removerObservador(this);
    }
}
```

## Conclusión

Este sistema proporciona una forma robusta y escalable de:
- ✅ Descargar archivos del servidor por chunks
- ✅ Notificar a la UI sobre el progreso en tiempo real
- ✅ Mantener la UI actualizada con cambios en la base de datos
- ✅ Desacoplar la lógica de negocio de la presentación
# Sistema de Descarga de Archivos con Patrón Observador

## Resumen

Se ha implementado un sistema completo para **descargar archivos del servidor** usando el patrón Observador para notificar a la UI sobre el progreso y estados.

## Componentes Creados

### 1. DTOs para Descarga de Archivos

#### DTOStartDownload
```java
// Solicita iniciar la descarga de un archivo
String fileId
```

#### DTODownloadInfo
```java
// Información recibida del servidor sobre el archivo a descargar
String downloadId
String fileName
long fileSize
int totalChunks
String mimeType
```

#### DTORequestChunk
```java
// Solicita un chunk específico durante la descarga
String downloadId
int chunkNumber
```

#### DTODownloadChunk
```java
// Chunk de datos recibido del servidor
String downloadId
int chunkNumber
String chunkData (Base64)
boolean isLast
```

### 2. Método de Descarga

**IGestionArchivos.descargarArchivo(String fileId, File directorioDestino)**

Este método:
1. Solicita al servidor iniciar la descarga
2. Recibe información del archivo (nombre, tamaño, chunks)
3. Descarga todos los chunks secuencialmente
4. Ensambla el archivo completo
5. Notifica a los observadores en cada paso

### 3. Eventos del Observador

El sistema notifica los siguientes eventos:

| Evento | Datos | Descripción |
|--------|-------|-------------|
| `DESCARGA_INICIADA` | `String fileId` | Se inició la solicitud de descarga |
| `DESCARGA_INFO` | `DTODownloadInfo` | Se recibió información del archivo |
| `DESCARGA_PROGRESO` | `Integer` (0-100) | Progreso de descarga en porcentaje |
| `DESCARGA_COMPLETADA` | `File` | Archivo descargado exitosamente |
| `DESCARGA_ERROR` | `String mensaje` | Error durante la descarga |

## Uso del Sistema

### En la Capa de Negocio

```java
IGestionArchivos gestionArchivos = new GestionArchivosImpl();

// Descargar un archivo
File directorioDestino = new File("./descargas");
gestionArchivos.descargarArchivo("archivo-id-123", directorioDestino)
    .thenAccept(archivo -> {
        System.out.println("Descargado: " + archivo.getAbsolutePath());
    })
    .exceptionally(ex -> {
        System.err.println("Error: " + ex.getMessage());
        return null;
    });
```

### En la UI (Implementando IObservador)

```java
public class MiVentana extends JFrame implements IObservador {
    
    private JProgressBar progressBar;
    private JLabel lblEstado;
    private IGestionArchivos gestionArchivos;
    
    public MiVentana() {
        gestionArchivos = new GestionArchivosImpl();
        gestionArchivos.registrarObservador(this);
        
        // Configurar UI...
    }
    
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        SwingUtilities.invokeLater(() -> {
            switch (tipoDeDato) {
                case "DESCARGA_INICIADA":
                    lblEstado.setText("Iniciando descarga...");
                    progressBar.setValue(0);
                    break;
                    
                case "DESCARGA_INFO":
                    DTODownloadInfo info = (DTODownloadInfo) datos;
                    lblEstado.setText("Descargando: " + info.getFileName());
                    break;
                    
                case "DESCARGA_PROGRESO":
                    Integer progreso = (Integer) datos;
                    progressBar.setValue(progreso);
                    break;
                    
                case "DESCARGA_COMPLETADA":
                    File archivo = (File) datos;
                    lblEstado.setText("Completado: " + archivo.getName());
                    JOptionPane.showMessageDialog(this, 
                        "Archivo descargado exitosamente");
                    break;
                    
                case "DESCARGA_ERROR":
                    String error = (String) datos;
                    JOptionPane.showMessageDialog(this, 
                        "Error: " + error, "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    break;
                    
                case "BASE_DATOS_ACTUALIZADA":
                    // Refrescar la vista según el tipo de actualización
                    String tipo = (String) datos;
                    if (tipo.equals("CANALES")) {
                        refrescarListaCanales();
                    } else if (tipo.equals("CONTACTOS")) {
                        refrescarListaContactos();
                    }
                    break;
            }
        });
    }
    
    private void btnDescargarClick() {
        String fileId = txtFileId.getText();
        File destino = new File("./descargas");
        
        gestionArchivos.descargarArchivo(fileId, destino)
            .exceptionally(ex -> {
                // El error ya se notifica al observador
                return null;
            });
    }
}
```

## Integración con Base de Datos

Para notificar a la UI cuando la base de datos se actualiza, usa el mismo sistema:

```java
// En tu repositorio o servicio
public class RepositorioCanales {
    
    private IGestionArchivos gestionArchivos; // O cualquier ISujeto
    
    public void sincronizarCanales(List<Canal> canales) {
        // Guardar en BD...
        
        // Notificar a la UI
        gestionArchivos.notificarObservadores("BASE_DATOS_ACTUALIZADA", "CANALES");
    }
}
```

## Protocolo de Comunicación con el Servidor

### Flujo de Descarga

1. **Cliente → Servidor**: `startFileDownload` con `DTOStartDownload`
2. **Servidor → Cliente**: Respuesta con `DTODownloadInfo`
3. **For each chunk**:
   - **Cliente → Servidor**: `requestFileChunk` con `DTORequestChunk`
   - **Servidor → Cliente**: Respuesta con `DTODownloadChunk`
4. **Cliente**: Ensambla todos los chunks y guarda el archivo

### Acciones Registradas

- `startFileDownload`: Iniciar descarga
- `downloadFileChunk_{downloadId}_{chunkNumber}`: Recibir cada chunk
- `requestFileChunk`: Solicitar un chunk específico

## Ventajas del Patrón Observador

1. **Desacoplamiento**: La lógica de negocio no conoce los detalles de la UI
2. **Múltiples observadores**: Puedes tener varios componentes escuchando
3. **Actualizaciones en tiempo real**: La UI se actualiza automáticamente
4. **Facilita testing**: Puedes crear observadores mock para pruebas
5. **Escalable**: Fácil agregar nuevos tipos de notificaciones

## Mejores Prácticas

1. **Siempre registra el observador antes de iniciar operaciones**
2. **Usa SwingUtilities.invokeLater() en Swing para actualizaciones UI**
3. **Maneja errores en los CompletableFuture**
4. **Remueve observadores cuando ya no se necesiten** (para evitar memory leaks)
5. **Usa tipos de datos descriptivos** para las notificaciones

## Ejemplo Completo: Descargar Archivo de un Canal

```java
public class PanelCanal extends JPanel implements IObservador {
    
    private IGestionArchivos gestionArchivos;
    private String canalActual;
    

