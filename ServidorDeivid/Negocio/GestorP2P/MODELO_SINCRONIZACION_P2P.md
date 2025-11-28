# Modelo de Sincronización P2P - Explicación Detallada

## 🤔 Preguntas Frecuentes

### **1. ¿Cómo funciona la sincronización entre peers?**

**Respuesta:**
- **TODOS los peers preguntan entre sí** mediante broadcast cuando se conectan o detectan cambios
- **NO hay un "líder" único** - es un sistema descentralizado donde cada peer es igual
- Cada peer envía `sync_check_all` con sus hashes Merkle
- Los demás peers comparan y responden si hay diferencias

### **2. ¿El peer con información más reciente avisa a los demás?**

**Respuesta Actual:**
- **NO automáticamente** - El sistema actual funciona así:
  1. Peer A se conecta o detecta cambios locales
  2. Peer A envía `sync_check_all` (broadcast a todos)
  3. Peer B compara sus hashes con los de Peer A
  4. Si Peer B tiene MENOS entidades, las solicita a Peer A
  5. Si Peer B tiene MÁS entidades, Peer A las solicitará cuando haga su comparación

**Problema Identificado:**
- Si Peer A tiene información más reciente pero Peer B nunca pregunta, B no se entera
- **Solución:** Cuando un peer detecta cambios locales (ej: nuevo mensaje), debe iniciar sincronización automáticamente

### **3. ¿Los demás piden sincronización?**

**Sí, pero solo cuando:**
1. Un nuevo peer se conecta → `actualizar("PEER_CONECTADO")` → inicia sincronización
2. Se guarda un nuevo mensaje → `sincronizarMensajes()` → inicia sincronización
3. Se fuerza manualmente → `forzarSincronizacion()`

**NO se sincroniza automáticamente cuando:**
- Un peer remoto actualiza su BD (a menos que ese peer inicie sincronización)
- Pasa tiempo sin actividad (no hay polling periódico por defecto)

---

## 🔄 Flujo de Sincronización Detallado

### **Escenario: Peer A tiene un mensaje nuevo que Peer B no conoce**

```
PEER A (tiene mensaje nuevo)          PEER B (desactualizado)
        │                                      │
        │ 1. Guarda mensaje en BD              │
        │    sincronizarMensajes()              │
        │                                      │
        │ 2. Reconstruye árboles Merkle        │
        │    Hash MENSAJE: 7a3f2e1b             │
        │                                      │
        │ 3. Broadcast sync_check_all ─────────>
        │    {MENSAJE: "7a3f2e1b", ...}        │
        │                                      │
        │                            4. Compara hashes
        │                               Hash MENSAJE: 9b2c4d5e
        │                               (DIFERENTE! ⚠)
        │                                      │
        │ <──────────────── 5. Responde con sus hashes
        │                    {MENSAJE: "9b2c4d5e", ...}
        │                                      │
        │ 6. Detecta diferencia en MENSAJE     │
        │    Envía sync_get_ids ───────────────>
        │    {tipo: "MENSAJE"}                 │
        │                                      │
        │                            7. Responde con IDs
        │ <──────────────────────── [id1, id2, id3]
        │                                      │
        │ 8. Compara IDs locales vs remotos    │
        │    - Peer A tiene: [id1, id2, id3, id4]
        │    - Peer B tiene: [id1, id2, id3]   │
        │    - FALTA: id4 (el nuevo mensaje)   │
        │                                      │
        │    Envía sync_get_entity ────────────>
        │    {tipo: "MENSAJE", id: "id4"}      │
        │                                      │
        │                            9. Envía la entidad
        │ <──────────── {tipo: "MENSAJE", data: {...}}
        │                                      │
        │                            10. Guarda mensaje
        │                                ÉXITO ✓
        │                                      │
        │ 11. Reinicia sincronización          │
        │     para verificar otros tipos       │
        │     Broadcast sync_check_all ────────>
        │                                      │
        │                            12. Ahora hashes coinciden
        │                                Hash MENSAJE: 7a3f2e1b
        │                                (IGUAL ✓)
        │                                      │
        │                            13. Sistema sincronizado
        │                                      │
```

---

## 🆚 Comparación de Atributos

### **¿Cuándo se comparan atributos individuales?**

**Fase 5 (Comparación de Contenido)** se activa cuando:
- ✅ Los IDs coinciden en ambos peers
- ❌ Pero los hashes Merkle son diferentes

**Ejemplo:**

```
Peer A tiene Usuario:
  id: "abc-123"
  nombre: "Juan Pérez"
  email: "juan@example.com"
  foto: "foto1.jpg"
  fecha_creacion: 2025-01-15 10:00:00

Peer B tiene Usuario:
  id: "abc-123"  ← MISMO ID
  nombre: "Juan García"  ← DIFERENTE
  email: "juan@example.com"
  foto: "foto2.jpg"  ← DIFERENTE
  fecha_creacion: 2025-01-15 09:55:00  ← MÁS ANTIGUA
```

**¿Qué pasa?**

1. **Fase 2** detecta que hash USUARIO difiere
2. **Fase 3** solicita IDs de usuarios
3. **Fase 4** compara IDs → Ambos tienen "abc-123"
4. **Fase 5** se activa → Compara campo por campo:
   - ⚠ Diferencia en NOMBRE
   - ⚠ Diferencia en FOTO
   - ✅ Comparar timestamps

5. **Resolución de conflicto:**
   - Peer B tiene `fecha_creacion` más antigua (09:55 < 10:00)
   - **Regla: El más antiguo gana** (fue creado primero)
   - Resultado: **Peer A actualiza con datos de Peer B**

---

## 🎯 Mejoras Implementadas

### **1. Comparación Completa de Atributos**

Antes:
```java
// Solo verificaba existencia
if (local == null) {
    guardar(remoto);
}
```

Ahora:
```java
// Compara campo por campo
if (!local.getNombre().equals(remoto.getNombre())) {
    hayDiferencias = true;
}
if (!local.getEmail().equals(remoto.getEmail())) {
    hayDiferencias = true;
}
// ... todos los campos

if (hayDiferencias) {
    resolverConflictoTemporal(fechaLocal, fechaRemota);
}
```

### **2. Servicio Refactorizado (Facade)**

**Antes:** ServicioSincronizacionDatos tenía 900+ líneas con toda la lógica mezclada

**Ahora:** ServicioSincronizacionDatosRefactorizado tiene ~250 líneas y delega al coordinador

```java
// API simple y clara
public void forzarSincronizacion() {
    coordinador.forzarSincronizacion();
}

public void sincronizarMensajes() {
    coordinador.marcarCambios();
    coordinador.iniciarSincronizacion();
}
```

### **3. Modularidad**

```
ServicioSincronizacionDatos (Facade/API)
    ↓ delega a
CoordinadorSincronizacion (Orquestador)
    ↓ usa
Fase1, Fase2, Fase3, Fase4, Fase5, Fase6 (Lógica específica)
```

---

## 📝 Recomendaciones de Uso

### **Para iniciar sincronización desde la aplicación:**

```java
// Obtener el servicio
ServicioSincronizacionDatos servicioSync = ...; 

// 1. Sincronización manual (ignora restricciones)
servicioSync.forzarSincronizacion();

// 2. Sincronización por nuevo mensaje (respeta intervalos)
servicioSync.sincronizarMensajes();

// 3. Sincronización cuando cambia BD
servicioSync.onBaseDeDatosCambio();

// 4. Sincronización automática al conectar peer (ya implementado)
// Se activa automáticamente por el patrón observador
```

### **Configuración recomendada:**

```java
// En ServicioP2P o Main
ServicioSincronizacionDatos servicioSync = new ServicioSincronizacionDatos();

// Configurar servicios auxiliares
servicioSync.setNotificador(notificador);
servicioSync.setServicioTransferenciaArchivos(servicioTransferencia);
servicioSync.setServicioNotificacionCliente(servicioNotificacionCS);

// Inicializar
servicioSync.inicializar(gestor, router);
servicioSync.iniciar();
```

---

## 🔧 Configuración Avanzada

### **Ajustar intervalos y reintentos:**

En `CoordinadorSincronizacion.java`:
```java
private static final int MAX_REINTENTOS = 3;  // Cambiar según necesidad
private static final long INTERVALO_MIN_MS = 2000;  // 2 segundos
```

### **Estrategia de resolución de conflictos:**

En `Fase5ComparacionContenido.java`:
```java
// Estrategia actual: El más antiguo gana
if (fechaRemota.isBefore(fechaLocal)) {
    guardarRemoto.run();
    return true;
}

// Alternativas:
// - El más reciente gana: fechaRemota.isAfter(fechaLocal)
// - Prioridad por ID de peer
// - Votación entre peers
// - Merge de campos específicos
```

---

## ✅ Verificación del Sistema

### **¿Cómo saber si está funcionando?**

**Logs a observar:**

```
[CoordinadorSync] === INICIANDO SINCRONIZACIÓN ===
[CoordinadorSync] Peers online: 2 | Intento: 1/3
[Fase1-Merkle] === Reconstruyendo árboles Merkle ===
[Fase2-Comparacion] ⚠ Diferencia en MENSAJE (L:7a3f != R:9b2c)
[Fase3-IDs] Solicitando IDs para tipo: MENSAJE
[Fase4-Faltantes] ⬇ Solicitando 1 entidades faltantes de MENSAJE
[Fase5-Contenido] 🔍 Iniciando 5 comparaciones de contenido para USUARIO
[Fase5-Contenido]   ✓ Usuario idéntico
[CoordinadorSync] ✔ SISTEMA TOTALMENTE SINCRONIZADO
[Fase6-Archivos] 🔄 Verificando archivos físicos faltantes...
```

### **Métricas importantes:**

- Tiempo de sincronización (desde inicio hasta "totalmente sincronizado")
- Cantidad de entidades transferidas
- Conflictos resueltos
- Reintentos necesarios

---

## 🚀 Próximos Pasos

1. **Implementar sincronización periódica** (polling cada X minutos)
2. **Dashboard de sincronización** para visualizar estado en tiempo real
3. **Métricas y estadísticas** de sincronización
4. **Pruebas de estrés** con múltiples peers
5. **Optimización de bandwidth** (solo transferir campos cambiados)

