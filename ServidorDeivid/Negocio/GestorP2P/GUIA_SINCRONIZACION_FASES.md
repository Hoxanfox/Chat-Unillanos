# Guía de Sincronización por Fases

## Descripción General

La sincronización P2P se ha modularizado en **6 fases independientes** y un **coordinador** que las orquesta. Esto mejora la mantenibilidad, testabilidad y comprensión del proceso.

---

## Arquitectura

```
CoordinadorSincronizacion (Orquestador)
    │
    ├── Fase1ConstruccionArboles (Merkle Trees)
    ├── Fase2ComparacionHashes (Detectar diferencias)
    ├── Fase3SolicitudIDs (Pedir IDs remotos)
    ├── Fase4DeteccionFaltantes (Identificar qué falta)
    ├── Fase5ComparacionContenido (Resolver conflictos)
    └── Fase6TransferenciaArchivos (Descargar archivos físicos)
```

---

## Flujo de Sincronización

### **Fase 1: Construcción de Árboles Merkle**
**Clase:** `Fase1ConstruccionArboles`

**Responsabilidad:**
- Construir árboles Merkle para cada tipo de entidad (USUARIO, CANAL, MIEMBRO, MENSAJE, ARCHIVO)
- Generar hashes raíz para comparación
- Proporcionar acceso a entidades locales

**Métodos principales:**
```java
void reconstruirTodosLosArboles()
void reconstruirArbol(String tipo)
JsonObject obtenerHashesRaiz()
String obtenerHashPorTipo(String tipo)
List<? extends IMerkleEntity> obtenerEntidadesPorTipo(String tipo)
IMerkleEntity buscarEntidad(String tipo, String id)
```

**Cuándo se ejecuta:**
- Al iniciar el servicio
- Antes de cada ciclo de sincronización
- Cuando se detectan cambios en BD

---

### **Fase 2: Comparación de Hashes**
**Clase:** `Fase2ComparacionHashes`

**Responsabilidad:**
- Comparar hashes locales vs remotos
- Identificar tipos de entidades que difieren
- Determinar si todo está sincronizado

**Métodos principales:**
```java
ResultadoComparacion compararHashes(JsonObject hashesRemotos)
```

**ResultadoComparacion contiene:**
- `List<String> tiposConDiferencias` - Tipos que necesitan sincronización
- `List<String> tiposSincronizados` - Tipos ya sincronizados
- `boolean todoSincronizado()` - Si no hay diferencias

**Cuándo se ejecuta:**
- Después de recibir respuesta a `sync_check_all`

**Decisión:**
- Si `todoSincronizado()` → Finalizar sincronización ✓
- Si hay diferencias → Ir a Fase 3 con el primer tipo

---

### **Fase 3: Solicitud de IDs**
**Clase:** `Fase3SolicitudIDs`

**Responsabilidad:**
- Solicitar lista de IDs de un tipo específico
- Obtener IDs locales para comparación
- Convertir entre formatos (JsonArray ↔ List)

**Métodos principales:**
```java
void solicitarIDs(String tipo)
JsonArray obtenerIDsLocales(String tipo)
List<String> obtenerListaIDsLocales(String tipo)
List<String> convertirIDsRemotos(JsonArray idsRemotos)
```

**Cuándo se ejecuta:**
- Cuando Fase 2 detecta diferencias en un tipo

**Acción:**
- Envía request `sync_get_ids` con el tipo
- Espera respuesta con lista de IDs remotos

---

### **Fase 4: Detección de Entidades Faltantes**
**Clase:** `Fase4DeteccionFaltantes`

**Responsabilidad:**
- Comparar IDs locales vs remotos
- Identificar entidades que faltan localmente
- Solicitar entidades faltantes

**Métodos principales:**
```java
ResultadoDeteccion detectarYSolicitarFaltantes(String tipo, JsonArray idsRemotos)
```

**ResultadoDeteccion contiene:**
- `List<String> idsFaltantes` - IDs que faltan localmente
- `List<String> idsCoincidentes` - IDs que existen en ambos lados
- `boolean hayFaltantes()` - Si faltan entidades
- `boolean todosLosIDsCoinciden()` - Si todos los IDs existen

**Cuándo se ejecuta:**
- Después de recibir IDs remotos (respuesta a `sync_get_ids`)

**Decisión:**
- Si `hayFaltantes()` → Solicita entidades con `sync_get_entity`
- Si `todosLosIDsCoinciden()` → Los hashes difieren por contenido → Ir a Fase 5
- Si no hay diferencias → Reiniciar sincronización (verificar siguiente tipo)

---

### **Fase 5: Comparación de Contenido**
**Clase:** `Fase5ComparacionContenido`

**Responsabilidad:**
- Resolver conflictos cuando IDs coinciden pero hashes difieren
- Comparar campo por campo
- Decidir qué versión conservar basándose en timestamps

**Métodos principales:**
```java
void iniciarComparaciones(String tipo, JsonArray idsRemotos)
boolean compararYResolver(String tipo, JsonElement dataRemota)
boolean decrementarComparacion() // Retorna true si terminaron todas
void resetearComparaciones()
```

**Estrategia de resolución de conflictos:**
- **Regla:** El registro con `fecha_creacion` más antigua gana (fue creado primero)
- Compara campo por campo: nombre, email, foto, contraseña, etc.
- Si el remoto es más antiguo → Actualiza local
- Si el local es más antiguo → Mantiene local

**Cuándo se ejecuta:**
- Cuando Fase 4 determina que todos los IDs coinciden pero hashes difieren

**Control de concurrencia:**
- Usa `AtomicInteger` para contar comparaciones pendientes
- Solo continúa sincronización cuando todas las comparaciones terminan

---

### **Fase 6: Transferencia de Archivos Físicos**
**Clase:** `Fase6TransferenciaArchivos`

**Responsabilidad:**
- Descargar archivos físicos después de sincronizar metadatos
- Verificar que archivos referenciados en BD existan en `Bucket/`

**Métodos principales:**
```java
void setServicioTransferencia(ServicioTransferenciaArchivos servicio)
void verificarYDescargarFaltantes()
boolean estaConfigurado()
```

**Cuándo se ejecuta:**
- Al final de una sincronización exitosa
- Solo si `ServicioTransferenciaArchivos` está configurado
- Se ejecuta en thread separado (no bloqueante)

**Proceso:**
1. Espera 1 segundo a que se persistan metadatos
2. Verifica archivos en BD vs Bucket/
3. Descarga archivos faltantes desde peers

---

## Coordinador de Sincronización

**Clase:** `CoordinadorSincronizacion`

**Responsabilidad:**
- Orquestar las 6 fases secuencialmente
- Controlar el flujo de sincronización
- Manejar reintentos y validaciones
- Notificar cambios a clientes CS

**Flujo principal:**

```java
iniciarSincronizacion()
    ↓
[Validaciones: no en progreso, intervalo mínimo, límite reintentos, peers online]
    ↓
ejecutarSincronizacion()
    ↓
Fase 1: Construir árboles Merkle
    ↓
Enviar sync_check_all (broadcast)
    ↓
Recibir respuesta → procesarDiferencias()
    ↓
Fase 2: Comparar hashes
    ↓
¿Todo sincronizado? → SÍ → finalizarSincronizacionExitosa()
    ↓ NO
Fase 3: Solicitar IDs del primer tipo con diferencias
    ↓
Recibir IDs → procesarIDsRecibidos()
    ↓
Fase 4: Detectar faltantes
    ↓
¿Hay faltantes? → SÍ → Solicitar entidades
    ↓ NO (pero hashes difieren)
Fase 5: Comparar contenido
    ↓
Resolver conflictos por timestamp
    ↓
¿Terminaron comparaciones? → SÍ → Reiniciar sincronización
    ↓
[Ciclo se repite hasta que todo esté sincronizado]
    ↓
finalizarSincronizacionExitosa()
    ↓
Fase 6: Transferir archivos físicos
    ↓
Notificar cambios a clientes CS
```

**Control de estado:**
```java
volatile boolean sincronizacionEnProgreso
volatile int contadorReintentos (máx 3)
volatile long ultimaSincronizacion (intervalo mín 2s)
volatile boolean huboCambiosEnEsteCiclo
```

---

## Ventajas de la Separación en Fases

### **1. Mantenibilidad**
- Cada clase tiene una responsabilidad única y clara
- Fácil ubicar dónde hacer cambios
- Código más corto y comprensible

### **2. Testabilidad**
- Cada fase se puede testear independientemente
- Fácil crear mocks para dependencias
- Tests unitarios más simples

### **3. Extensibilidad**
- Fácil agregar nuevas fases (ej: Fase7ValidationIntegridad)
- Fácil modificar comportamiento de una fase sin afectar otras
- Estrategias de resolución de conflictos intercambiables

### **4. Debugging**
- Logs organizados por fase
- Fácil identificar en qué fase ocurre un problema
- Flujo más claro al leer logs

### **5. Reusabilidad**
- Fases pueden usarse individualmente si es necesario
- Ej: Solo reconstruir árboles sin sincronizar
- Ej: Solo comparar hashes sin solicitar IDs

---

## Uso del Coordinador

### **Inicialización:**

```java
// Crear coordinador
CoordinadorSincronizacion coordinador = new CoordinadorSincronizacion(gestor, gson);

// Configurar servicios opcionales
coordinador.configurarNotificaciones(notificador, servicioNotificacionCliente);
coordinador.configurarTransferenciaArchivos(servicioTransferenciaArchivos);

// Reconstruir árboles al inicio
coordinador.reconstruirArboles();
```

### **Iniciar sincronización:**

```java
// Sincronización automática (respeta intervalos y límites)
coordinador.iniciarSincronizacion();

// Sincronización forzada (ignora restricciones)
coordinador.forzarSincronizacion();

// Marcar cambios para notificar
coordinador.marcarCambios();
```

### **Procesar respuestas P2P:**

```java
// En el manejador de sync_check_all
router.registrarManejadorRespuesta("sync_check_all", (resp) -> {
    if (resp.fueExitoso()) {
        coordinador.procesarDiferencias(resp.getData().getAsJsonObject());
    }
});

// En el manejador de sync_get_ids
router.registrarManejadorRespuesta("sync_get_ids", (resp) -> {
    if (resp.fueExitoso()) {
        JsonObject res = resp.getData().getAsJsonObject();
        String tipo = res.get("tipo").getAsString();
        JsonArray ids = res.get("ids").getAsJsonArray();
        coordinador.procesarIDsRecibidos(tipo, ids);
    }
});

// En el manejador de sync_get_entity
router.registrarManejadorRespuesta("sync_get_entity", (resp) -> {
    if (resp.fueExitoso()) {
        JsonObject env = resp.getData().getAsJsonObject();
        coordinador.procesarEntidadRecibida(
            env.get("tipo").getAsString(),
            env.get("data")
        );
    }
});

// En el manejador de sync_compare_entity
router.registrarManejadorRespuesta("sync_compare_entity", (resp) -> {
    if (resp.fueExitoso()) {
        JsonObject env = resp.getData().getAsJsonObject();
        coordinador.procesarComparacion(
            env.get("tipo").getAsString(),
            env.get("data")
        );
    }
});
```

---

## Logs Organizados

Cada fase tiene su propio TAG para logs:

- `Fase1-Merkle` - Construcción de árboles
- `Fase2-Comparacion` - Comparación de hashes
- `Fase3-IDs` - Solicitud de IDs
- `Fase4-Faltantes` - Detección de faltantes
- `Fase5-Contenido` - Comparación de contenido
- `Fase6-Archivos` - Transferencia de archivos
- `CoordinadorSync` - Orquestación general

Ejemplo de log:
```
[Fase1-Merkle] === Reconstruyendo árboles Merkle ===
[Fase1-Merkle]   - USUARIO: 5 entidades, hash: 3a4f2e1b
[CoordinadorSync] ▶ FASE 2: Comparando hashes
[Fase2-Comparacion] ⚠ Diferencia en MENSAJE (L:7f3a != R:9b2c)
[CoordinadorSync] ▶ FASE 3: Solicitando IDs para MENSAJE
[Fase4-Faltantes] ⬇ Solicitando 3 entidades faltantes de MENSAJE
```

---

## Próximos Pasos

1. **Refactorizar ServicioSincronizacionDatos** para usar el coordinador
2. **Testing:** Crear tests unitarios para cada fase
3. **Métricas:** Agregar medición de tiempos por fase
4. **Dashboard:** Mostrar estado de sincronización en tiempo real
5. **Configuración:** Permitir ajustar intervalos y reintentos por archivo de configuración

---

## Notas Técnicas

- **Thread-safety:** Las fases no mantienen estado mutable (excepto Fase5 con contador)
- **Inmutabilidad:** Los resultados son objetos inmutables
- **Separación de concerns:** Cada fase solo conoce lo necesario
- **Dependency Injection:** Las fases reciben dependencias en el constructor
- **Single Responsibility:** Cada clase tiene una única razón para cambiar

