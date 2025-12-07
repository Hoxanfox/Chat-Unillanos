# 📝 Configuración del Modelo Vosk para Transcripción de Audio

## ¿Qué es Vosk?

Vosk es un motor de reconocimiento de voz offline que permite transcribir archivos de audio a texto sin necesidad de servicios en la nube.

## 📥 Pasos para Configurar Vosk

### 1. Descargar el Modelo de Vosk

Los modelos están disponibles en: https://alphacephei.com/vosk/models

**Modelos Recomendados para Español:**

- **vosk-model-small-es-0.42** (50 MB) - Modelo ligero, rápido pero menos preciso
- **vosk-model-es-0.42** (1.4 GB) - Modelo completo, más preciso pero requiere más recursos

**Para Inglés:**
- **vosk-model-small-en-us-0.15** (40 MB) - Ligero
- **vosk-model-en-us-0.22** (1.8 GB) - Completo

### 2. Ubicación del Modelo

Después de descargar, extrae el modelo en una de estas ubicaciones:

```
./modelos/vosk-model-es-0.42/
```
o
```
C:/vosk/models/vosk-model-es-0.42/
```

### 3. Configurar la Ruta en el Código

En tu clase Main o de inicialización, configura la ruta del modelo:

```java
// Ejemplo en Main.java o clase de inicialización del servidor
FachadaTranscripcion fachada = FachadaTranscripcion.getInstance();

// Ruta al modelo (ajustar según tu instalación)
String rutaModelo = "./modelos/vosk-model-es-0.42";
// o en Windows:
// String rutaModelo = "C:/vosk/models/vosk-model-es-0.42";

boolean exitoso = fachada.inicializarModeloTranscripcion(rutaModelo);

if (exitoso) {
    System.out.println("✅ Modelo Vosk cargado correctamente");
} else {
    System.err.println("❌ Error al cargar modelo Vosk");
    System.err.println("Descarga el modelo desde: https://alphacephei.com/vosk/models");
}
```

### 4. Verificar la Estructura del Modelo

El directorio del modelo debe contener:

```
vosk-model-es-0.42/
├── am/              (Modelo acústico)
├── conf/            (Configuración)
├── graph/           (Grafo de decodificación)
├── ivector/         (Vectores i)
└── README           (Información del modelo)
```

## 🎯 Uso del Sistema de Transcripción

### Transcripción Automática

Una vez configurado, el sistema puede transcribir audios automáticamente:

```java
// Encolar un audio para transcripción
FachadaTranscripcion fachada = FachadaTranscripcion.getInstance();
boolean encolado = fachada.iniciarTranscripcionAutomatica(audioId);

if (encolado) {
    System.out.println("Audio encolado para transcripción");
}
```

### Características del Sistema

- ✅ **Transcripción en segundo plano**: Los audios se procesan en una cola sin bloquear la aplicación
- ✅ **Notificaciones en tiempo real**: Las vistas se actualizan automáticamente cuando termina una transcripción
- ✅ **Soporte para múltiples formatos**: WAV, MP3, OGG, etc.
- ✅ **Detección automática de sample rate**: El sistema se adapta al formato del audio

## 🔧 Requisitos del Sistema

### Dependencia Maven (ya incluida en pom.xml)

```xml
<dependency>
    <groupId>com.alphacephei</groupId>
    <artifactId>vosk</artifactId>
    <version>0.3.45</version>
</dependency>
```

### Recursos de Sistema

- **Memoria RAM**: Mínimo 2GB libres (4GB recomendado para modelo completo)
- **CPU**: Procesador multi-core recomendado
- **Disco**: Espacio para el modelo (50MB - 2GB según el modelo)

## 📊 Rendimiento

### Modelo Ligero (small)
- Velocidad: ~2x tiempo real (un audio de 1 min tarda ~30 seg)
- Precisión: ~85-90%
- Uso de RAM: ~500MB

### Modelo Completo
- Velocidad: ~1x tiempo real (un audio de 1 min tarda ~1 min)
- Precisión: ~92-95%
- Uso de RAM: ~1-2GB

## 🐛 Solución de Problemas

### Error: "Modelo no encontrado"

Verifica que:
1. La ruta sea correcta y absoluta
2. El directorio contenga todos los archivos necesarios
3. Los permisos de lectura estén configurados

### Error: "Out of Memory"

Soluciones:
1. Usa un modelo más pequeño (small)
2. Aumenta la memoria heap de Java: `-Xmx2G`
3. Procesa menos audios simultáneamente

### Transcripción vacía o con errores

Posibles causas:
1. Audio de mala calidad o muy bajo volumen
2. Idioma del audio no coincide con el modelo
3. Formato de audio no compatible (convertir a WAV PCM)

## 📱 Integración con el Sistema

El sistema está completamente integrado:

1. **Cuando llega un mensaje de audio**: Se notifica automáticamente al sistema de transcripción
2. **Descarga de archivos P2P**: Cuando se descarga un audio, se notifica a las vistas
3. **Filtrado por canal/contacto**: Los repositorios permiten filtrar transcripciones específicas
4. **Observadores**: Las vistas se actualizan automáticamente cuando hay cambios

## 🎓 Ejemplo Completo

```java
// 1. Inicializar el sistema (en Main.java)
FachadaTranscripcion fachada = FachadaTranscripcion.getInstance();
fachada.inicializarModeloTranscripcion("./modelos/vosk-model-es-0.42");

// 2. Cargar audios existentes
fachada.cargarAudiosDesdeBaseDatos();

// 3. Iniciar actualización automática cada 60 segundos
fachada.iniciarActualizacionAutomatica(60);

// 4. Suscribirse a notificaciones (en la vista)
fachada.registrarObservador(miVista);

// 5. Transcribir todos los audios pendientes
int encolados = fachada.transcribirTodosPendientes();
System.out.println(encolados + " audios encolados para transcripción");
```

## 🔔 Eventos que Disparan Notificaciones

El sistema notifica a las vistas en estos eventos:

- `NUEVO_AUDIO_RECIBIDO`: Cuando llega un nuevo mensaje de audio
- `ARCHIVO_DESCARGADO`: Cuando se completa la descarga de un archivo (audio, texto, etc.)
- `TRANSCRIPCION_COMPLETADA`: Cuando termina la transcripción automática
- `TRANSCRIPCION_ENCOLADA`: Cuando se encola un audio para transcripción
- `TRANSCRIPCION_ERROR`: Si hay un error en la transcripción
- `AUDIOS_CARGADOS`: Cuando se cargan los audios desde la BD

---

**Nota**: La primera vez que se carga el modelo, puede tomar unos segundos. Las transcripciones posteriores serán más rápidas.

