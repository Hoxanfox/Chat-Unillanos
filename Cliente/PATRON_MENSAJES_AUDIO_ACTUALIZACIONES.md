            
        // Para CANALES:
        case "HISTORIAL_CANAL_RECIBIDO":      // Historial del canal
        case "MENSAJE_CANAL_RECIBIDO":        // Nuevo mensaje en canal
        case "NUEVO_MENSAJE_CANAL":           // Nuevo mensaje (alternativo)
        case "ERROR_OPERACION":               // Error genérico
    }
}
```

### **Validaciones importantes:**

```java
case "NUEVO_MENSAJE_PRIVADO":
    if (datos instanceof DTOMensaje) {
        DTOMensaje mensaje = (DTOMensaje) datos;
        
        // ✅ Validación null-safe
        if (mensaje.getRemitenteId() == null) {
            System.err.println("⚠️ Mensaje con remitenteId null, ignorando...");
            break;
        }
        
        // ✅ Filtrar solo mensajes del contacto actual
        if (mensaje.getRemitenteId().equals(contacto.getId()) || mensaje.esMio()) {
            Platform.runLater(() -> agregarMensaje(mensaje));
        }
    }
    break;

case "MENSAJE_ENVIADO_EXITOSO":
    if (datos instanceof DTOMensaje) {
        DTOMensaje mensaje = (DTOMensaje) datos;
        
        // ✅ Verificar que sea para este chat/canal
        if (mensaje.getDestinatarioId() != null && 
            mensaje.getDestinatarioId().equals(contacto.getId())) {
            
            // Descargar audio si es necesario
            if (mensaje.esAudio() && mensaje.getContenido() != null) {
                controlador.descargarAudioALocal(mensaje.getContenido());
            }
            
            Platform.runLater(() -> agregarMensaje(mensaje));
        }
    }
    break;
```

---

## 🎨 CREACIÓN DE BURBUJAS DE MENSAJES

### **Alineación según propietario:**

```java
// ✅ EN CHATS PRIVADOS (VistaContactoChat):
// Mensajes del usuario a la DERECHA, del contacto a la IZQUIERDA
Pos alineacion = mensaje.esMio() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;

// ✅ EN CANALES (VistaCanal):
// Mensajes propios a la IZQUIERDA (verde), otros a la DERECHA (blanco)
Pos alineacion = mensaje.isEsPropio() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT;
```

### **Validaciones antes de agregar:**

```java
private void agregarMensaje(DTOMensaje mensaje) {
    // 1. Verificar duplicados
    String id = mensaje.getMensajeId();
    if (id != null && !id.isEmpty() && mensajesMostrados.contains(id)) {
        return;
    }
    
    // 2. Verificar que tenga contenido
    boolean hasText = mensaje.getContenido() != null && !mensaje.getContenido().trim().isEmpty();
    boolean hasFile = mensaje.getFileId() != null && !mensaje.getFileId().isEmpty();
    
    if (!hasText && !hasFile) {
        System.out.println("⚠️ Mensaje vacío, no se mostrará");
        return;
    }
    
    // 3. Determinar tipo y crear burbuja
    VBox burbuja;
    if (mensaje.esTexto()) {
        burbuja = crearBurbujaMensaje(mensaje, ...);
    } else if (mensaje.esAudio()) {
        burbuja = crearBurbujaAudio(mensaje, ...);
    } else if (mensaje.esImagen()) {
        burbuja = crearBurbujaImagen(mensaje, ...);
    } else if (mensaje.esArchivo()) {
        burbuja = crearBurbujaArchivo(mensaje, ...);
    }
    
    // 4. Agregar a UI
    mensajesBox.getChildren().add(burbuja);
    mensajesMostrados.add(id);
}
```

### **Tipos de mensaje (case-insensitive):**

```java
// ⚠️ IMPORTANTE: El servidor puede enviar "AUDIO", "TEXT", "ARCHIVO" en mayúsculas
if ("AUDIO".equalsIgnoreCase(mensaje.getTipo())) {
    // Crear burbuja de audio
} else if ("ARCHIVO".equalsIgnoreCase(mensaje.getTipo())) {
    // Crear burbuja de archivo
} else {
    // Texto por defecto
}
```

---

## 🔄 USO DE Platform.runLater()

**SIEMPRE** usar `Platform.runLater()` cuando se actualice la UI desde el método `actualizar()`:

```java
@Override
public void actualizar(String tipoDeDato, Object datos) {
    Platform.runLater(() -> {
        // Actualizar UI aquí
        agregarMensaje(mensaje);
        mensajesBox.getChildren().clear();
        // etc.
    });
}
```

---

## 📥 FLUJO COMPLETO DE ENVÍO DE AUDIO

### **1. Usuario presiona botón de micrófono:**
```
Usuario → btnAudio.click() 
       → controlador.iniciarGrabacionAudio()
       → isRecording = true
       → UI: btnAudio muestra "❌", campoMensaje deshabilitado
```

### **2. Usuario presiona Send:**
```
Usuario → btnEnviar.click() (mientras isRecording = true)
       → controlador.detenerYEnviarGrabacion(destinatarioId)
       → Controlador sube audio al servidor
       → Servidor envía confirmación
       → actualizar("MENSAJE_AUDIO_ENVIADO_EXITOSO", DTOMensaje)
       → Descargar audio a caché local
       → agregarMensaje(mensaje) en UI
       → isRecording = false
       → UI: Restaurar botones
```

### **3. Otro usuario recibe el audio:**
```
Servidor → PUSH notification
         → actualizar("NUEVO_MENSAJE_PRIVADO", DTOMensaje)
         → Detectar que es audio
         → descargarAudioALocal(fileId) [caché]
         → agregarMensaje(mensaje) con botón de reproducción
```

---

## 🎯 CHECKLIST DE IMPLEMENTACIÓN

Para implementar mensajes de audio en una nueva vista:

- [ ] **1. Implementar IObservador**
  ```java
  public class MiVista extends BorderPane implements IObservador
  ```

- [ ] **2. Registrarse como observador**
  ```java
  controlador.registrarObservador(this);
  ```

- [ ] **3. Agregar Set de mensajes mostrados**
  ```java
  private final Set<String> mensajesMostrados = Collections.synchronizedSet(new HashSet<>());
  ```

- [ ] **4. Agregar estado de grabación**
  ```java
  private boolean isRecording = false;
  ```

- [ ] **5. Crear botones de audio**
  - Botón micrófono (🎤)
  - Botón cancelar (❌) - oculto por defecto
  - Modificar comportamiento del botón Send

- [ ] **6. Implementar método actualizar()**
  - Manejar: NUEVO_MENSAJE, MENSAJE_ENVIADO, HISTORIAL, ERRORES
  - Usar Platform.runLater()
  - Validar null-safety

- [ ] **7. Implementar agregarMensaje()**
  - Validar duplicados
  - Validar contenido vacío
  - Determinar tipo de mensaje
  - Crear burbuja apropiada

- [ ] **8. Implementar crearBurbujaAudio()**
  - Botón de reproducción (▶️)
  - Llamar a controlador.reproducirAudioEnMemoria()
  - Manejar estados: ⏳, ✅, ❌

- [ ] **9. Implementar descarga automática**
  - En NUEVO_MENSAJE: descargarAudioALocal()
  - En HISTORIAL: iterar y descargar todos los audios

- [ ] **10. Solicitar historial al inicializar**
  ```java
  controlador.solicitarHistorial(destinatarioId);
  ```

---

## 🔍 DEBUGGING

### **Logs importantes:**
```java
System.out.println("🔧 Inicializando vista...");
System.out.println("🔔 Registrándose como observador...");
System.out.println("📡 Solicitando historial...");
System.out.println("📥 Notificación recibida - Tipo: " + tipoDeDato);
System.out.println("💬 Nuevo mensaje recibido");
System.out.println("   → De: " + mensaje.getRemitenteNombre());
System.out.println("   → Tipo: " + mensaje.getTipo());
System.out.println("   → esMio: " + mensaje.esMio());
System.out.println("⚠️ Mensaje ya mostrado, ignorando ID: " + id);
System.out.println("✅ Mensaje agregado a la vista");
```

---

## 📚 DIFERENCIAS CLAVE ENTRE VISTAS

| Aspecto | VistaContactoChat (Privado) | VistaCanal (Grupo) |
|---------|----------------------------|-------------------|
| **Registro Observador** | `controlador.registrarObservador(this)` | `controlador.registrarObservadorMensajes(this)` |
| **Solicitar Historial** | `controlador.solicitarHistorial(contactoId)` | `controlador.solicitarHistorialCanal(canalId, 50)` |
| **Notificación Nuevo Mensaje** | `NUEVO_MENSAJE_PRIVADO` | `NUEVO_MENSAJE_CANAL` / `MENSAJE_CANAL_RECIBIDO` |
| **Alineación Mensajes Propios** | DERECHA (Pos.CENTER_RIGHT) | IZQUIERDA (Pos.CENTER_LEFT) |
| **Color Burbuja Propia** | Verde (#dcf8c6) | Verde (#dcf8c6) |
| **Grabación Audio** | Usa controlador.iniciarGrabacionAudio() | Usa clase GrabadorAudio directa |
| **DTO Mensaje** | DTOMensaje | DTOMensajeCanal |
| **Método esMio** | `mensaje.esMio()` | `mensaje.isEsPropio()` |

---

## ✅ RESUMEN

### **Conceptos Clave:**
1. **Patrón Observador** para recibir actualizaciones en tiempo real
2. **Set sincronizado** para evitar duplicados
3. **Estado de grabación** (isRecording) para cambiar comportamiento de botones
4. **Descarga automática** de audios a caché local
5. **Platform.runLater()** para todas las actualizaciones de UI
6. **Validaciones** antes de agregar mensajes (null, vacío, duplicado)
7. **Tipos de mensaje case-insensitive** (AUDIO, TEXT, ARCHIVO)
8. **CompletableFuture** para operaciones asíncronas

### **Flujo General:**
```
Inicializar → Registrar Observador → Solicitar Historial
           ↓
Recibir Notificación → Validar → Platform.runLater() → agregarMensaje()
           ↓
Crear Burbuja → Agregar a mensajesBox → Marcar como mostrado
```

---

**Fecha:** 7 de Noviembre, 2025  
**Proyecto:** Chat Unillanos - Cliente  
**Vistas Analizadas:** VistaContactoChat, VistaCanal
# 📋 PATRÓN DE IMPLEMENTACIÓN: Mensajes de Audio y Actualizaciones en Tiempo Real

## 🎯 Resumen Ejecutivo
Documento que detalla los patrones de implementación usados en **VistaContactoChat** y **VistaCanal** para gestionar mensajes de audio y actualizaciones en tiempo real mediante el patrón Observador.

---

## 🏗️ ARQUITECTURA GENERAL

### 1. **Patrón Observador (IObservador)**
Ambas vistas implementan `IObservador` para recibir notificaciones del controlador:

```java
public class VistaContactoChat extends BorderPane implements IObservador {
    // Se registra como observador al inicializar
    this.controlador.registrarObservador(this);
    
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        // Maneja diferentes tipos de notificaciones
    }
}
```

### 2. **Prevención de Duplicados**
Usar un `Set` sincronizado para evitar mostrar mensajes duplicados:

```java
private final Set<String> mensajesMostrados = Collections.synchronizedSet(new HashSet<>());

private void agregarMensaje(DTOMensaje mensaje) {
    String id = mensaje.getMensajeId();
    if (id != null && !id.isEmpty() && mensajesMostrados.contains(id)) {
        System.out.println("⚠️ Mensaje ya mostrado, ignorando ID: " + id);
        return;
    }
    // ... agregar mensaje
    if (id != null && !id.isEmpty()) {
        mensajesMostrados.add(id);
    }
}
```

---

## 🎤 GESTIÓN DE MENSAJES DE AUDIO

### 1. **Estado de Grabación**
Mantener una variable de estado para controlar el modo de grabación:

```java
private boolean isRecording = false;
```

### 2. **Interfaz de Usuario Dinámica**

#### **Botones de Audio:**
- 🎤 (Micrófono) → Inicia grabación / Cancela si está grabando
- ❌ (Cancelar) → Aparece solo durante grabación
- Send → Envía texto normal O detiene/envía audio si está grabando

#### **Comportamiento del botón de Audio:**
```java
btnAudio.setOnAction(e -> {
    if (isRecording) {
        // Cancelar grabación
        controlador.cancelarGrabacion();
        isRecording = false;
        btnAudio.setText("🎤");
        campoMensaje.setDisable(false);
    } else {
        // Iniciar grabación
        controlador.iniciarGrabacionAudio();
        isRecording = true;
        btnAudio.setText("❌");
        campoMensaje.setDisable(true); // Deshabilitar texto mientras se graba
    }
});
```

#### **Comportamiento del botón Send:**
```java
btnEnviar.setOnAction(e -> {
    if (isRecording) {
        // Detener y enviar audio
        controlador.detenerYEnviarGrabacion(contacto.getId());
        isRecording = false;
        btnAudio.setText("🎤");
        campoMensaje.setDisable(false);
    } else {
        // Enviar mensaje de texto
        String texto = campoMensaje.getText();
        if (texto != null && !texto.trim().isEmpty()) {
            controlador.enviarMensajeTexto(contacto.getId(), texto);
            campoMensaje.clear();
        }
    }
});
```

### 3. **Descarga y Caché de Audios**

#### **En VistaContactoChat - Descarga Automática:**
```java
// Detectar si es Base64 o fileId
if (mensaje.esAudio() && mensaje.getContenido() != null) {
    String contenido = mensaje.getContenido();
    
    boolean esBase64Audio = contenido.startsWith("UklGR") || 
                           contenido.startsWith("data:audio/") ||
                           contenido.length() > 1000;
    
    if (esBase64Audio) {
        // Guardar desde Base64
        controlador.guardarAudioDesdeBase64(contenido, mensaje.getMensajeId())
                .thenAccept(archivo -> {
                    if (archivo != null) {
                        mensaje.setContenido(archivo.getAbsolutePath());
                    }
                });
    } else {
        // Descargar desde servidor usando fileId
        controlador.descargarAudioALocal(contenido)
                .thenAccept(archivo -> {
                    // Audio descargado a caché
                });
    }
}
```

#### **Descargar Audios del Historial:**
```java
case "HISTORIAL_MENSAJES_RECIBIDO":
    List<DTOMensaje> mensajes = (List<DTOMensaje>) datos;
    Platform.runLater(() -> {
        mensajesBox.getChildren().clear();
        mensajesMostrados.clear();
        
        for (DTOMensaje mensaje : mensajes) {
            agregarMensaje(mensaje);
            
            // Descargar audios a caché
            if (mensaje.esAudio() && mensaje.getFileId() != null) {
                controlador.descargarAudioALocal(mensaje.getFileId())
                        .thenAccept(archivo -> {
                            System.out.println("✅ Audio descargado: " + archivo.getName());
                        });
            }
        }
    });
    break;
```

### 4. **Reproducción de Audio**
```java
private VBox crearBurbujaAudio(DTOMensaje mensaje, Pos alineacion) {
    Button btnPlay = new Button("▶️");
    btnPlay.setOnAction(e -> {
        btnPlay.setDisable(true);
        btnPlay.setText("⏳");
        
        // Reproducir audio EN MEMORIA
        controlador.reproducirAudioEnMemoria(mensaje.getFileId())
                .thenRun(() -> {
                    Platform.runLater(() -> {
                        btnPlay.setText("✅");
                    });
                    
                    // Re-habilitar después de 2 segundos
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        Platform.runLater(() -> {
                            btnPlay.setDisable(false);
                            btnPlay.setText("▶️");
                        });
                    }).start();
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        btnPlay.setText("❌");
                        btnPlay.setDisable(false);
                    });
                    return null;
                });
    });
    
    Label audioLabel = new Label("🎤 Audio" + 
        (mensaje.getFileName() != null ? " - " + mensaje.getFileName() : ""));
    
    HBox audioBox = new HBox(10);
    audioBox.getChildren().addAll(btnPlay, audioLabel);
    
    return crearBurbuja(audioBox, mensaje, alineacion);
}
```

---

## 📨 TIPOS DE NOTIFICACIONES (actualizar())

### **Tipos clave a manejar:**

```java
@Override
public void actualizar(String tipoDeDato, Object datos) {
    switch (tipoDeDato) {
        case "NUEVO_MENSAJE_PRIVADO":         // Mensaje entrante de otro usuario
        case "MENSAJE_ENVIADO_EXITOSO":       // Confirmación de envío de texto
        case "MENSAJE_AUDIO_ENVIADO_EXITOSO": // Confirmación de envío de audio
        case "HISTORIAL_MENSAJES_RECIBIDO":   // Historial completo
        case "HISTORIAL_MENSAJES":            // Historial (alternativo)
        case "ERROR_ENVIO_MENSAJE":           // Error al enviar texto
        case "ERROR_ENVIO_MENSAJE_AUDIO":     // Error al enviar audio
        case "ERROR_HISTORIAL":               // Error al cargar historial

