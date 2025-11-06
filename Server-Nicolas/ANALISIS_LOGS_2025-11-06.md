- ⚠️ Modelo Vosk no disponible (transcripción deshabilitada)
- ⚠️ Peer inválido en BD (ignorado por nueva validación)
- ⚠️ Red P2P aislada (1 solo peer - este servidor)

### ❌ Sin Peers Conectados:
```log
Conectando a 2 peers conocidos en BD...
⚠️ Peer b137e993-bbb2-46d6-917f-14e2061fdaa3 tiene puerto inválido: 0. Ignorando...
✓ Intentando conectar a 0 peers válidos
```

**Resultado:** El servidor está **aislado**, no hay otros servidores en la red.

---

## 🛠️ **Acciones Recomendadas**

### Acción 1: Limpiar Peers Inválidos (URGENTE)
```bash
cd /home/deivid/Documents/Chat-Unillanos/Server-Nicolas
mysql -u root -p chat_unillanos < scripts/limpiar_peers_invalidos.sql
```

### Acción 2: Optimizar Configuración de Heartbeat
Editar `config/server.properties`:
```properties
peer.heartbeat.interval.ms=30000
```

### Acción 3: Configurar Bootstrap Peers (Si hay otros servidores)
Si tienes otro servidor corriendo, agregar en `config/server.properties`:
```properties
peer.bootstrap.nodes=192.168.1.X:22200
```

### Acción 4: Instalar Modelo Vosk (OPCIONAL)
Solo si necesitas transcripción de audios:
```bash
wget https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip
unzip vosk-model-small-es-0.42.zip
```

---

## 🎯 **Próximos Logs Esperados**

Después de aplicar las soluciones, deberías ver:

```log
✓ PeerConnectionManager inicializado. Local Peer ID: xxx
✓ Puerto P2P: 22200, Max conexiones: 50
✓ Servidor P2P iniciado en puerto 22200
✓ Conectando a 1 peers conocidos en BD...
⚠️ Se encontraron 0 peers con datos inválidos en la BD  ← Debería ser 0
⚠️ Modelo Vosk no encontrado (OK si no lo necesitas)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚠️  RED AISLADA - Este servidor NO tiene peers configurados
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 📝 **Resumen**

| Problema | Severidad | Estado | Acción |
|----------|-----------|--------|--------|
| Peer con puerto 0 | 🔴 Alta | ✅ Solucionado | Limpiar BD |
| Modelo Vosk faltante | 🟡 Media | ✅ Manejado | Opcional instalarlo |
| Consultas repetitivas | 🟢 Baja | ⚙️ Optimizable | Ajustar config |
| Red aislada | 🟡 Media | ℹ️ Normal | Configurar bootstrap si hay otros servers |

---

**Conclusión:** El servidor está funcionando correctamente, pero necesita limpieza de datos y optimización de configuración.
# 🔍 Análisis de Logs - Problemas Encontrados y Solucionados

## Fecha: 6 de Noviembre 2025

---

## 📋 Resumen de Problemas

### ✅ **Problemas Identificados en los Logs:**

1. **Peer con puerto inválido (172.19.0.1:0)** - CRÍTICO
2. **Modelo Vosk no encontrado** - ADVERTENCIA
3. **Consultas repetitivas a la base de datos** - OPTIMIZACIÓN

---

## 🔴 **Problema 1: Peer con Puerto Inválido**

### Log del Error:
```log
2025-11-06 02:52:34 [Thread-2] INFO  - Iniciando conexión saliente a peer b137e993-bbb2-46d6-917f-14e2061fdaa3 (172.19.0.1:0)
2025-11-06 02:52:34 [pool-1-thread-1] ERROR - Connection refused to peer (172.19.0.1:0)
```

### Causa:
- Existe un registro en la base de datos con **puerto = 0**, lo cual es inválido
- El sistema intentó conectarse 3 veces sin éxito (reintentos configurados)
- IP `172.19.0.1` es la IP del Docker bridge, probablemente de una prueba anterior

### Impacto:
- ❌ Retrasos de 15 segundos en el inicio (3 reintentos × 5 segundos)
- ❌ Logs llenos de errores de conexión
- ❌ Recursos desperdiciados intentando conectar a un peer inválido

### Solución Implementada:

**Archivo:** `transporte/server-Transporte/.../PeerConnectionManager.java`

Agregué validación antes de intentar conectar:

```java
// Validar que el peer tenga IP y puerto válidos
if (peer.getIp() == null || peer.getIp().trim().isEmpty()) {
    log.warn("⚠️ Peer {} tiene IP inválida o vacía. Ignorando...", peer.getPeerId());
    invalidCount++;
    continue;
}

if (peer.getPuerto() == null || peer.getPuerto() <= 0 || peer.getPuerto() > 65535) {
    log.warn("⚠️ Peer {} tiene puerto inválido: {}. Ignorando...", 
            peer.getPeerId(), peer.getPuerto());
    invalidCount++;
    continue;
}
```

### Cómo Limpiar la Base de Datos:

```bash
# Ejecutar el script SQL de limpieza
mysql -u root -p chat_unillanos < scripts/limpiar_peers_invalidos.sql
```

O manualmente:

```sql
DELETE FROM peers WHERE puerto IS NULL OR puerto <= 0 OR puerto > 65535;
```

---

## ⚠️ **Problema 2: Modelo Vosk No Encontrado**

### Log del Error:
```log
ERROR (VoskAPI:Model():model.cc:122) Folder 'vosk-model-small-es-0.42' does not contain model files.
ERROR CRÍTICO: No se pudo cargar el modelo de Vosk.
```

### Causa:
- La carpeta del modelo Vosk **no existe** en el directorio del servidor
- El modelo se usa para transcribir mensajes de voz a texto
- El servidor buscaba en: `vosk-model-small-es-0.42/`

### Impacto:
- ⚠️ La funcionalidad de **transcripción de audio está deshabilitada**
- ✅ El servidor sigue funcionando normalmente (no es crítico)
- ✅ Los mensajes de audio se pueden enviar/recibir, pero sin transcripción

### Solución Implementada:

**Archivo:** `negocio/server-LogicaMensajes/.../AudioTranscriptionService.java`

Mejoré el manejo del error para que sea **no crítico**:

1. Ahora muestra un mensaje claro con instrucciones
2. El servidor continúa funcionando sin transcripciones
3. Usa logging apropiado (SLF4J) en lugar de `System.err`

### Nuevo Log Esperado:
```log
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚠️  Modelo Vosk no encontrado en: /path/to/vosk-model-small-es-0.42
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
La transcripción de audio NO estará disponible.
Para habilitar transcripciones:
  1. Descarga el modelo desde: https://alphacephei.com/vosk/models
  2. Descomprime 'vosk-model-small-es-0.42' en la carpeta del servidor
  3. Reinicia el servidor
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Cómo Instalar el Modelo (Opcional):

```bash
# 1. Descargar el modelo
wget https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip

# 2. Descomprimir en la carpeta del servidor
unzip vosk-model-small-es-0.42.zip

# 3. Reiniciar el servidor
```

**Nota:** Solo necesario si quieres transcripción automática de audios a texto.

---

## 🔄 **Problema 3: Consultas Repetitivas a la BD**

### Log del Problema:
```log
Hibernate: select p1_0.id, p1_0.conectado... from peers p1_0
Hibernate: select p1_0.id, p1_0.conectado... from peers p1_0
Hibernate: select p1_0.id, p1_0.conectado... from peers p1_0
Hibernate: select p1_0.id, p1_0.conectado... from peers p1_0
Hibernate: select p1_0.id, p1_0.conectado... from peers p1_0
```

### Causa:
- Las tareas de mantenimiento (heartbeat, reconexión, sincronización) consultan la BD cada pocos segundos
- Es **normal** y necesario para mantener el estado actualizado
- Frecuencia configurada en `server.properties`

### Configuración Actual:
```properties
peer.heartbeat.interval.ms=1000           # Cada 1 segundo (muy frecuente)
peer.heartbeat.timeout.seconds=60         # 60 segundos sin respuesta = desconectado
peer.reconnect.delay.ms=5000              # Reintentos cada 5 segundos
```

### Recomendación de Optimización:

**Archivo:** `config/server.properties`

```properties
# Configuración optimizada para reducir consultas
peer.heartbeat.interval.ms=30000          # Cada 30 segundos (más razonable)
peer.heartbeat.timeout.seconds=90         # 90 segundos timeout
peer.reconnect.delay.ms=10000             # Reintentos cada 10 segundos
```

Esto reducirá las consultas a la BD de ~60 por minuto a ~2 por minuto.

---

## 📊 **Estado Actual del Sistema**

### ✅ Funcionando Correctamente:
- ✓ Servidor principal (puerto 22100)
- ✓ Servidor P2P (puerto 22200)
- ✓ Base de datos conectada (HikariCP)
- ✓ Repositorios JPA (6 interfaces encontradas)
- ✓ Tareas de mantenimiento programadas

### ⚠️ Con Advertencias:
-- Script para limpiar peers inválidos de la base de datos
-- Ejecutar este script para eliminar peers con datos incorrectos

-- 1. Ver peers con problemas
SELECT id, ip, puerto, conectado, ultimo_latido 
FROM peers 
WHERE puerto IS NULL 
   OR puerto <= 0 
   OR puerto > 65535
   OR ip IS NULL 
   OR ip = '';

-- 2. Eliminar peers con puerto inválido
DELETE FROM peers 
WHERE puerto IS NULL 
   OR puerto <= 0 
   OR puerto > 65535;

-- 3. Eliminar peers con IP inválida
DELETE FROM peers 
WHERE ip IS NULL 
   OR ip = '';

-- 4. Ver peers restantes
SELECT id, ip, puerto, conectado, ultimo_latido 
FROM peers 
ORDER BY ultimo_latido DESC;

-- 5. (Opcional) Limpiar completamente la tabla de peers si quieres empezar de cero
-- TRUNCATE TABLE peers;

-- 6. Ver estado final
SELECT 
    COUNT(*) as total_peers,
    SUM(CASE WHEN conectado = 'ONLINE' THEN 1 ELSE 0 END) as online,
    SUM(CASE WHEN conectado = 'OFFLINE' THEN 1 ELSE 0 END) as offline
FROM peers;

