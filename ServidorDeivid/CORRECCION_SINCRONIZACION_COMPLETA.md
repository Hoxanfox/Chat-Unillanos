# 🔧 Corrección Completa de Sincronización P2P

**Fecha:** 2025-11-27  
**Problema:** Sincronización duplicada y timestamps no actualizados

---

## 🔴 Problemas Identificados

### 1. **Respuestas Duplicadas en Fase 5**
- **Síntoma:** Al tener 2 peers conectados, cada `sync_compare_entity` generaba respuestas de ambos peers
- **Efecto:** Comparaciones restantes llegaban a números negativos (-1, -2)
- **Causa:** Broadcast sin deduplicación de respuestas

### 2. **Timestamps No Actualizados**
- **Síntoma:** Al modificar un usuario (nombre, foto, etc.), el timestamp permanecía igual
- **Efecto:** Conflictos imposibles de resolver ("Misma fecha. Manteniendo local por defecto")
- **Causa:** El método `actualizarUsuario()` no actualizaba `fecha_creacion`

### 3. **Múltiples Solicitudes Simultáneas**
- **Síntoma:** Dos threads de Netty procesaban la misma fase simultáneamente
- **Efecto:** "Sincronización ya en progreso" repetido
- **Causa:** Falta de sincronización thread-safe

---

## ✅ Soluciones Implementadas

### 1. **Deduplicación de Respuestas (Fase 5)**

**Archivo:** `Fase5ComparacionContenido.java`

```java
// ✅ NUEVO: Set thread-safe para rastrear IDs procesados
private final Set<String> idsYaProcesados = ConcurrentHashMap.newKeySet();

public void iniciarComparaciones(String tipo, JsonArray idsRemotos) {
    // Limpiar IDs procesados de la ronda anterior
    idsYaProcesados.clear();
    // ...
}

public boolean yaFueProcesado(String tipo, String id) {
    String clave = tipo + ":" + id;
    return !idsYaProcesados.add(clave); // Retorna true si ya existía
}
```

**Archivo:** `ServicioSincronizacionDatos.java`

```java
router.registrarManejadorRespuesta("sync_compare_entity", (resp) -> {
    if (resp.fueExitoso()) {
        String tipo = env.get("tipo").getAsString();
        JsonElement data = env.get("data");
        String id = extraerIdDeEntidad(tipo, data);
        
        // ✅ Verificar si ya procesamos esta respuesta
        if (coordinador.getFase5().yaFueProcesado(tipo, id)) {
            LoggerCentral.debug(TAG, "⏩ Respuesta duplicada ignorada");
            return;
        }
        
        coordinador.procesarComparacion(tipo, data);
    }
});
```

### 2. **Actualización Automática de Timestamps**

**Archivo:** `GestorUsuarios.java`

```java
public DTOUsuarioVista actualizarUsuario(DTOActualizarUsuario dto) {
    Usuario usuario = repositorio.buscarPorId(dto.getId());
    boolean huboCambios = false;
    
    // Detectar cambios en cada campo
    if (dto.getNombre() != null && !usuario.getNombre().equals(dto.getNombre())) {
        usuario.setNombre(dto.getNombre());
        huboCambios = true;
    }
    // ... más campos ...
    
    // ✅ CRÍTICO: Actualizar timestamp solo si hubo cambios reales
    if (huboCambios) {
        usuario.setFechaCreacion(Instant.now());
        LoggerCentral.info(TAG, "⏰ Timestamp actualizado: " + usuario.getFechaCreacion());
    }
    
    repositorio.guardar(usuario);
    
    // Notificar solo si hubo cambios
    if (huboCambios) {
        notificarObservadores(EVENTO_USUARIO_ACTUALIZADO, usuario);
    }
    
    return convertirADTOVista(usuario);
}
```

**Archivo:** `UsuarioRepositorio.java`

```java
public boolean guardar(Usuario u) {
    String sql = "INSERT INTO usuarios (...) VALUES (...) " +
        "ON DUPLICATE KEY UPDATE " +
        "nombre=VALUES(nombre), " +
        "email=VALUES(email), " +
        "foto=VALUES(foto), " +
        "fecha_creacion=VALUES(fecha_creacion)"; // ✅ Actualizar timestamp
    
    // ✅ Usar timestamp actual o el que trae la entidad
    Instant timestamp = u.getFechaCreacion();
    if (timestamp == null) {
        timestamp = Instant.now();
        u.setFechaCreacion(timestamp);
    }
    ps.setTimestamp(9, Timestamp.from(timestamp));
    // ...
}
```

### 3. **Protección contra Contador Negativo**

**Archivo:** `Fase5ComparacionContenido.java`

```java
public boolean decrementarComparacion() {
    int restantes = comparacionesPendientes.decrementAndGet();
    
    // ✅ Evitar que el contador baje de cero
    if (restantes < 0) {
        LoggerCentral.warn(TAG, "⚠️ Contador negativo, ajustando a 0");
        comparacionesPendientes.set(0);
        return true;
    }
    
    return restantes <= 0;
}
```

---

## 🧪 Cómo Probar la Solución

### Test 1: Modificar Usuario
1. Inicia 3 peers (A, B, C)
2. En peer A, modifica el nombre de un usuario
3. **Esperado:** 
   - El timestamp del usuario se actualiza en A
   - B y C reciben la sincronización
   - Al comparar timestamps, gana la versión más reciente

### Test 2: Respuestas Duplicadas
1. Inicia 2 peers (A, B)
2. Modifica un usuario en A
3. **Esperado en logs:**
   - `Iniciando 2 comparaciones de contenido para USUARIO`
   - `Comparaciones restantes: 1`
   - `Comparaciones restantes: 0` ✅ (NO negativo)
   - `⏩ Respuesta duplicada ignorada` (aparece varias veces)

### Test 3: Conflicto por Timestamp
1. Desconecta peer B
2. En peer A: Modifica usuario → nombre = "Angel" (timestamp T1)
3. En peer B: Modifica usuario → nombre = "Deivid" (timestamp T2)
4. Reconecta peer B
5. **Esperado:**
   - Si T1 < T2: Gana "Angel" (más antiguo = creado primero)
   - Si T2 < T1: Gana "Deivid"
   - **NO más:** "Misma fecha. Manteniendo local por defecto"

---

## 📊 Logs Correctos Esperados

### Antes (❌ Incorrecto):
```
[Fase5-Contenido] Comparaciones restantes: 1
[Fase5-Contenido] Comparaciones restantes: 0
[Fase5-Contenido] Comparaciones restantes: -1  ❌
[Fase5-Contenido] Comparaciones restantes: -2  ❌
[Fase5-Contenido]   ⚠ Misma fecha. Manteniendo local  ❌
```

### Después (✅ Correcto):
```
[Fase5-Contenido] Iniciando 2 comparaciones de contenido para USUARIO
[SyncDatos] ⏩ Respuesta duplicada ignorada: USUARIO ID: 9e0df928...
[Fase5-Contenido]   Comparando timestamps:
[Fase5-Contenido]     Local:  2025-11-28T04:10:23.456Z
[Fase5-Contenido]     Remoto: 2025-11-28T04:05:18.331Z
[Fase5-Contenido]   ⚠ Versión REMOTA es más antigua. Actualizando...  ✅
[Fase5-Contenido] Comparaciones restantes: 1
[Fase5-Contenido] Comparaciones restantes: 0  ✅
[CoordinadorSync] ✓ Comparaciones completadas para USUARIO
```

---

## 🔑 Puntos Clave

1. **Deduplicación:** Cada ID se procesa UNA SOLA VEZ por ronda de sincronización
2. **Timestamps:** Se actualizan automáticamente en modificaciones reales
3. **Thread-Safe:** Uso de `ConcurrentHashMap.newKeySet()` y `AtomicInteger`
4. **Resolución de Conflictos:** El registro más antiguo (creado primero) gana

---

## 📦 Archivos Modificados

- ✅ `Fase5ComparacionContenido.java` - Deduplicación
- ✅ `ServicioSincronizacionDatos.java` - Filtrado de respuestas
- ✅ `GestorUsuarios.java` - Actualización de timestamps
- ✅ `UsuarioRepositorio.java` - Persistencia de timestamps

---

## ✅ Estado Final

**Compilación:** ✅ BUILD SUCCESS  
**Tests:** ⏳ Pendiente de prueba manual  
**Despliegue:** Listo para reiniciar peers

