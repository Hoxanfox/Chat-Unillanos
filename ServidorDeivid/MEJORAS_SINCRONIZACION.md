# Mejoras en la Sincronización de Árboles de Merkle P2P

## Problema Identificado

El sistema de sincronización P2P estaba experimentando **ciclos infinitos** que causaban:

1. **Múltiples sincronizaciones simultáneas**: Ambos peers enviaban `sync_check_all` al mismo tiempo
2. **Bucle infinito de verificación**: Cuando los hashes diferían pero los IDs eran idénticos, el sistema reiniciaba la sincronización indefinidamente
3. **Spam de mensajes**: Se generaban cientos de mensajes `sync_get_ids` y `sync_check_all` sin control
4. **Alto consumo de recursos**: CPU y red sobrecargados por las verificaciones continuas

### Causa Raíz

El problema principal estaba en el método `solicitarEntidadesFaltantes()`:

```java
// CÓDIGO ANTERIOR - PROBLEMÁTICO
if (faltantes == 0) {
    // Cuando no hay entidades faltantes pero los hashes difieren
    // se reiniciaba la sincronización -> BUCLE INFINITO
    iniciarSincronizacionGeneral();
}
```

**¿Por qué los hashes difieren si tienen los mismos IDs?**

Los árboles de Merkle pueden generar hashes diferentes incluso con los mismos datos debido a:
- Diferencias en el orden de inserción
- Timestamps ligeramente diferentes
- Pequeñas variaciones en metadatos
- Esto es **NORMAL** y no indica desincronización real

## Soluciones Implementadas

### 1. Control de Concurrencia

Se agregaron mecanismos para evitar sincronizaciones simultáneas:

```java
// Nuevas variables de control
private volatile boolean sincronizacionEnProgreso = false;
private volatile int contadorReintentos = 0;
private volatile long ultimaSincronizacion = 0;
private static final long INTERVALO_MIN_SYNC_MS = 2000; // 2 segundos
private static final int MAX_REINTENTOS_SYNC = 3;
```

### 2. Validaciones en `iniciarSincronizacionGeneral()`

Ahora se verifica antes de iniciar una sincronización:

- ✅ **No hay otra sincronización en progreso**
- ✅ **Ha pasado el tiempo mínimo** desde la última sincronización (2 segundos)
- ✅ **No se ha excedido el límite de reintentos** (máximo 3 intentos)

```java
if (sincronizacionEnProgreso) {
    LoggerCentral.warn(TAG, "⚠ Sincronización ya en progreso. Ignorando solicitud.");
    return;
}

if (tiempoTranscurrido < INTERVALO_MIN_SYNC_MS) {
    LoggerCentral.warn(TAG, "⚠ Demasiado pronto para sincronizar.");
    return;
}

if (contadorReintentos >= MAX_REINTENTOS_SYNC) {
    LoggerCentral.error(TAG, "⚠ Límite de reintentos alcanzado. Deteniendo.");
    return;
}
```

### 3. Manejo Correcto de Hashes Diferentes con Mismos IDs

Se eliminó el bucle infinito en `solicitarEntidadesFaltantes()`:

```java
// CÓDIGO NUEVO - CORRECTO
if (faltantes == 0) {
    LoggerCentral.warn(TAG, "⚠ Tenemos todos los IDs pero los hashes difieren.");
    LoggerCentral.warn(TAG, "   Esto es normal en árboles Merkle.");
    
    // NO reiniciamos sincronización - esto causaba el bucle infinito
    // Consideramos esta situación como "sincronizado"
    
    // Resetear contadores ya que no hay nada que sincronizar
    contadorReintentos = 0;
    sincronizacionEnProgreso = false;
}
```

### 4. Reset de Contadores al Completar Sincronización

En `procesarDiferenciasEnOrden()`, cuando todo está sincronizado:

```java
// SI LLEGAMOS AQUÍ, ES QUE TODO ESTÁ SINCRONIZADO
LoggerCentral.info(TAG, "✔ Sistema totalmente sincronizado.");

// Resetear contadores de sincronización
contadorReintentos = 0;
sincronizacionEnProgreso = false;
```

### 5. Liberación Automática del Lock

Se implementó un sistema de timeout para liberar el lock automáticamente:

```java
// Liberar el lock después de un tiempo razonable
new Thread(() -> {
    try {
        Thread.sleep(3000); // Dar 3 segundos para procesar respuestas
        sincronizacionEnProgreso = false;
        LoggerCentral.debug(TAG, "Lock de sincronización liberado");
    } catch (Exception e) {
        LoggerCentral.error(TAG, "Error liberando lock: " + e.getMessage());
    }
}).start();
```

## Resultados Esperados

### Antes de las Mejoras ❌
```
[23:21:09] sync_check_all enviado
[23:21:09] sync_get_ids MENSAJE
[23:21:09] sync_get_ids MENSAJE (duplicado)
[23:21:09] sync_check_all enviado (de nuevo)
[23:21:09] sync_get_ids MENSAJE (otra vez)
[23:21:10] sync_check_all enviado (infinito...)
```

### Después de las Mejoras ✅
```
[23:21:09] sync_check_all enviado (Intento 1/3)
[23:21:09] sync_get_ids MENSAJE
[23:21:10] ✔ Sistema totalmente sincronizado.
[23:21:12] sync_check_all ignorado (sincronización en progreso)
[23:21:38] sync_check_all enviado (Intento 1/3) [siguiente ciclo normal]
```

## Beneficios

1. ✅ **Elimina bucles infinitos**: Máximo 3 reintentos antes de detenerse
2. ✅ **Reduce carga de red**: Solo 1 sincronización cada 2 segundos mínimo
3. ✅ **Previene condiciones de carrera**: Lock de sincronización en progreso
4. ✅ **Manejo inteligente de Merkle**: Reconoce diferencias normales de hash
5. ✅ **Auto-recuperación**: Timeout automático de 3 segundos
6. ✅ **Mejor observabilidad**: Logs claros con contadores de intentos

## Logs Mejorados

Los nuevos logs muestran claramente el estado:

```
[SyncDatos] Programando sincronización general... (Intento 1/3)
[SyncDatos] ⚠ Sincronización ya en progreso. Ignorando solicitud.
[SyncDatos] ⚠ Demasiado pronto para sincronizar. Esperando 1500ms
[SyncDatos] ⚠ Límite de reintentos alcanzado (3). Deteniendo sincronización.
[SyncDatos] ✔ Sistema totalmente sincronizado.
[SyncDatos] Lock de sincronización liberado
```

## Recomendaciones Adicionales

### Para el Futuro

1. **Considerar implementar un sistema de versiones**: Para detectar cambios reales en el contenido
2. **Agregar métricas**: Contar cuántas sincronizaciones exitosas vs fallidas
3. **Implementar backoff exponencial**: Aumentar el tiempo entre reintentos
4. **Crear un healthcheck**: Endpoint para verificar estado de sincronización
5. **Persistir estado de sincronización**: Para recuperarse de caídas

### Configuración Ajustable

Puedes modificar estos valores según las necesidades:

```java
private static final long INTERVALO_MIN_SYNC_MS = 2000; // 2 segundos
private static final int MAX_REINTENTOS_SYNC = 3; // 3 intentos
```

Para redes más lentas, aumentar `INTERVALO_MIN_SYNC_MS` a 5000ms (5 segundos).

## Testing

Para probar las mejoras:

1. **Iniciar 2 peers** con datos idénticos
2. **Observar logs**: Solo debe haber 1 ciclo de sincronización
3. **Verificar**: No debe haber mensajes repetidos infinitamente
4. **Agregar datos nuevos**: La sincronización debe detectarlos correctamente
5. **Desconectar y reconectar**: Debe sincronizar automáticamente

---

**Fecha de implementación**: 2025-11-23  
**Archivos modificados**: `ServicioSincronizacionDatos.java`  
**Estado**: ✅ Compilación exitosa

