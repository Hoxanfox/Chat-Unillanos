# 🎯 Resolución de Conflictos y Manejo de Fechas en P2P

## 📋 ¿Cómo se determina quién tiene la "Verdad"?

En un sistema P2P **descentralizado**, no hay un servidor central que tenga "la verdad". 
Cada peer mantiene su propia copia de los datos y deben sincronizarse entre sí.

### **Sistema de 3 Niveles de Verificación**

```
Nivel 1: HASHES MERKLE (Detección rápida)
    │
    ├─→ ¿Hashes iguales? → ✅ SINCRONIZADO
    └─→ ¿Hashes diferentes? → Ir a Nivel 2
              │
Nivel 2: COMPARACIÓN DE IDs
    │
    ├─→ Peer A tiene IDs que Peer B no tiene → COPIAR a Peer B
    ├─→ Peer B tiene IDs que Peer A no tiene → COPIAR a Peer A
    └─→ Ambos tienen los mismos IDs pero hashes difieren → Ir a Nivel 3
              │
Nivel 3: COMPARACIÓN DE CONTENIDO + TIMESTAMPS
    │
    └─→ Comparar campo por campo
        └─→ Si hay diferencias → USAR TIMESTAMPS para decidir
```

---

## 📅 **Fechas Utilizadas por Tipo de Entidad**

| Entidad | Campo de Fecha | Estrategia | Justificación |
|---------|---------------|------------|---------------|
| **Usuario** | `fechaCreacion` | MÁS RECIENTE gana | Permite actualizaciones de perfil (nombre, foto, email) |
| **Canal** | `fechaCreacion` | MÁS RECIENTE gana | Permite renombrar canales |
| **Mensaje** | `fechaEnvio` | MÁS RECIENTE gana | Permite editar mensajes enviados |
| **Archivo** | `fechaUltimaActualizacion` | MÁS RECIENTE gana | Refleja la versión más actualizada del archivo |
| **CanalInvitacion** | `fechaCreacion` | MÁS RECIENTE gana | Refleja el estado actual (aceptada/rechazada) |
| **CanalMiembro** | ❌ Sin fecha | Versión remota gana | Relación simple sin modificaciones |

---

## 🔄 **Estrategia: "Más Reciente Gana"**

### **¿Por qué esta estrategia?**

✅ **Ventajas:**
- Refleja la **última intención del usuario**
- Permite **actualizaciones** de datos
- Intuitivo para aplicaciones de chat
- Los cambios más recientes se propagan

❌ **Desventajas:**
- Requiere que los relojes de los peers estén sincronizados
- Un peer con reloj adelantado podría dominar
- No es apropiado para datos inmutables

### **Ejemplo Práctico**

```
ESCENARIO:
- Peer A: Usuario "Juan" cambia su nombre a "Juan García" a las 10:30
- Peer B: Usuario "Juan" cambia su nombre a "Juan Pérez" a las 10:45
- Ambos peers se sincronizan a las 11:00

RESULTADO:
- fechaCreacion local (10:30) < fechaCreacion remota (10:45)
- ✅ "Juan Pérez" gana (es más reciente)
- Ambos peers quedan con "Juan Pérez"
```

---

## 🛡️ **Protección contra Desincronización de Relojes**

### **Problema:**
Si el reloj de un peer está adelantado, sus cambios siempre ganarán.

### **Soluciones Implementadas:**

1. **Tolerancia de Conflictos:**
   ```java
   if (fechaRemota.isAfter(fechaLocal)) {
       // Remoto gana
   } else if (fechaRemota.isBefore(fechaLocal)) {
       // Local gana
   } else {
       // MISMA FECHA → Mantener local por defecto
   }
   ```

2. **Logs Detallados:**
   ```
   [Fase5] Comparando timestamps:
     Local:  2025-01-15T10:30:00Z
     Remoto: 2025-01-15T10:45:00Z
   [Fase5] ⚠ Versión REMOTA es más reciente. Actualizando...
   [Fase5] ✓ Usuario actualizado
   ```

3. **Comparación Campo por Campo:**
   - No se actualiza todo ciegamente
   - Solo si hay diferencias reales en los campos
   - Permite detectar inconsistencias

### **Mejora Recomendada (Opcional):**

Si quieres protección extra contra desincronización:

```java
// Detectar diferencias de tiempo sospechosas
long diferenciaSegundos = Math.abs(
    fechaRemota.getEpochSecond() - fechaLocal.getEpochSecond()
);

if (diferenciaSegundos > 86400) { // Más de 24 horas
    LoggerCentral.warn(TAG, "⚠️ Diferencia de tiempo sospechosa: " 
        + diferenciaSegundos + " segundos");
    // Podría pedir confirmación o usar otra estrategia
}
```

---

## 🔍 **Flujo Completo de Resolución de Conflictos**

### **Caso: Usuario actualiza su perfil en dos peers diferentes**

```
T=0: ESTADO INICIAL
  Peer A: {id: "123", nombre: "Juan", email: "juan@mail.com", fechaCreacion: 2025-01-15T10:00:00Z}
  Peer B: {id: "123", nombre: "Juan", email: "juan@mail.com", fechaCreacion: 2025-01-15T10:00:00Z}
  ✅ Sincronizados

T=1: CAMBIOS OFFLINE (peers desconectados)
  Peer A: Usuario cambia nombre → "Juan García" (fecha: 10:30:00Z)
  Peer B: Usuario cambia email → "juan.nuevo@mail.com" (fecha: 10:45:00Z)

T=2: PEERS SE RECONECTAN
  Peer A envía: sync_check_all
  Hash USUARIO de A: 7a3f2e1b
  Hash USUARIO de B: 9b2c4d5e
  ❌ DIFERENTES!

T=3: COMPARACIÓN DE IDs
  Ambos tienen ID "123" → Ir a comparación de contenido

T=4: COMPARACIÓN CAMPO POR CAMPO
  Peer B compara:
    - nombre: "Juan" ≠ "Juan García" ⚠️
    - email: "juan.nuevo@mail.com" ≠ "juan@mail.com" ⚠️
    - fechaCreacion: 10:45:00Z > 10:30:00Z ✅

  DECISIÓN: Peer B es más reciente → Peer A actualiza con datos de B

T=5: RESULTADO FINAL
  Peer A: {id: "123", nombre: "Juan", email: "juan.nuevo@mail.com", fechaCreacion: 10:45:00Z}
  Peer B: {id: "123", nombre: "Juan", email: "juan.nuevo@mail.com", fechaCreacion: 10:45:00Z}
  
  ⚠️ NOTA: Se perdió el cambio de nombre porque B tenía fecha más reciente
```

### **¿Cómo evitar perder cambios?**

**Opción 1: Campos con timestamps independientes**
```java
class Usuario {
    Instant fechaCreacion;
    Instant fechaModificacionNombre;  // ✅ Timestamp por campo
    Instant fechaModificacionEmail;   // ✅ Timestamp por campo
}
```

**Opción 2: Sistema de versiones**
```java
class Usuario {
    long version;  // Se incrementa en cada cambio
    // Si remoto.version > local.version → actualizar
}
```

**Opción 3: Log de cambios (CRDT)**
```java
// Guardar todos los cambios y mergearlos inteligentemente
List<Cambio> historialCambios;
```

---

## 📊 **Logs de Sincronización**

El sistema genera logs detallados para auditoría:

```
[Fase5] === Comparando USUARIO ===
[Fase5]   Diferencia en NOMBRE
[Fase5]     Local: Juan García
[Fase5]     Remoto: Juan Pérez
[Fase5]   Diferencia en EMAIL
[Fase5]   Comparando timestamps:
[Fase5]     Local:  2025-01-15T10:30:00Z
[Fase5]     Remoto: 2025-01-15T10:45:00Z
[Fase5]   ⚠ Versión REMOTA es más reciente. Actualizando...
[Fase5]   ✓ Usuario actualizado
```

Estos logs te permiten:
- ✅ Ver exactamente qué versión ganó
- ✅ Detectar problemas de sincronización
- ✅ Auditar cambios de datos
- ✅ Depurar conflictos

---

## 🎓 **Resumen Ejecutivo**

### **¿Quién tiene la verdad?**
**El peer con la versión más reciente según el timestamp.**

### **¿Qué pasa si no hay timestamp?**
**Se acepta la versión remota por defecto** (caso de CanalMiembro).

### **¿Qué pasa si los timestamps son iguales?**
**Se mantiene la versión local** (conservador, no se sobrescribe sin razón).

### **¿Es confiable?**
**Sí, si los relojes están sincronizados.** Usa NTP en producción para sincronizar relojes.

### **¿Se pueden perder cambios?**
**Sí, si dos peers modifican el mismo registro offline.** El más reciente gana, el más antiguo se pierde.
Para evitar esto, considera implementar **timestamps por campo** o **CRDT**.

---

## 🚀 **Mejoras Futuras Recomendadas**

### **1. Sincronización de Relojes (NTP)**
```java
// Ajustar todas las fechas al tiempo del servidor NTP
Instant ahora = NTPService.getCurrentTime();
```

### **2. Vector Clocks (Detección de Concurrencia)**
```java
class Usuario {
    Map<String, Long> vectorClock; // peer_id → version
}
// Permite detectar cambios concurrentes y mergearlos
```

### **3. Timestamps por Campo**
```java
class Usuario {
    String nombre;
    Instant fechaModificacionNombre;
    
    String email;
    Instant fechaModificacionEmail;
}
// Permite sincronizar campos independientemente
```

### **4. Historial de Cambios (Event Sourcing)**
```sql
CREATE TABLE cambios_usuario (
    id UUID,
    usuario_id UUID,
    campo VARCHAR(50),
    valor_anterior TEXT,
    valor_nuevo TEXT,
    timestamp TIMESTAMP,
    peer_id VARCHAR(50)
);
```

### **5. Resolución Manual de Conflictos**
```java
if (detectarConflicto()) {
    // Notificar al usuario
    mostrarDialogoResolucion(versionLocal, versionRemota);
}
```

---

## 📝 **Checklist de Implementación**

- [x] Comparación de hashes Merkle
- [x] Comparación de IDs
- [x] Comparación campo por campo
- [x] Resolución por timestamps
- [x] Logs detallados
- [x] Manejo de fechas para USUARIO, CANAL, MENSAJE
- [x] Manejo de fechas para ARCHIVO (fechaUltimaActualizacion)
- [x] Manejo de fechas para CANAL_INVITACION
- [x] Deduplicación de respuestas
- [ ] Sincronización de relojes (NTP)
- [ ] Vector clocks para detección de concurrencia
- [ ] Timestamps por campo
- [ ] Resolución manual de conflictos
- [ ] Testing de escenarios de conflicto

---

**Última actualización:** 2025-12-01
**Autor:** Sistema de Sincronización P2P

