# 🔧 Cambios Realizados para Solucionar el Problema de Puertos

## ❌ Problema Original

Los dos servidores intentaban iniciarse en el **mismo puerto (22100)**, causando el error:
```
Address already in use: bind
```

Además, el **timeout era muy corto (90 segundos)**, marcando peers como OFFLINE prematuramente.

---

## ✅ Soluciones Implementadas

### 1. Scripts Mejorados con Argumentos de Línea de Comandos

En lugar de depender de archivos de configuración separados, ahora los scripts pasan los parámetros directamente:

**server1.bat** - Servidor 1 en puerto 22100:
```batch
java -jar server-app.jar ^
  --server.port=22100 ^
  --p2p.puerto=22100 ^
  --p2p.nombre.servidor=Servidor-1 ^
  --spring.datasource.url=jdbc:h2:file:./data/chatdb ^
  --logging.file.name=logs/server.log ^
  --p2p.peers.bootstrap=172.29.128.1:22101 ^
  --p2p.heartbeat.timeout=300000
```

**server2.bat** - Servidor 2 en puerto 22101:
```batch
java -jar server-app.jar ^
  --server.port=22101 ^
  --p2p.puerto=22101 ^
  --p2p.nombre.servidor=Servidor-2 ^
  --spring.datasource.url=jdbc:h2:file:./data/chatdb2 ^
  --logging.file.name=logs/server2.log ^
  --p2p.peers.bootstrap=172.29.128.1:22100 ^
  --p2p.heartbeat.timeout=300000
```

### 2. Timeout Aumentado

- **Antes**: 90 segundos (90000 ms)
- **Ahora**: 5 minutos (300000 ms)

Esto evita que los peers se marquen como OFFLINE por pequeños retrasos en la red.

### 3. Configuración Separada para Cada Servidor

| Parámetro | Servidor 1 | Servidor 2 |
|-----------|------------|------------|
| Puerto | 22100 | 22101 |
| Base de datos | `./data/chatdb` | `./data/chatdb2` |
| Logs | `logs/server.log` | `logs/server2.log` |
| Nombre | Servidor-1 | Servidor-2 |
| Bootstrap | 172.29.128.1:22101 | 172.29.128.1:22100 |

### 4. Scripts de Utilidad Creados

#### `iniciar-ambos-servidores.bat`
Inicia ambos servidores automáticamente en ventanas separadas con un delay de 5 segundos entre ellos.

#### `verificar-puertos.bat`
Verifica si los puertos 22100 y 22101 están en uso.

#### `detener-servidores.bat`
Detiene todos los procesos Java que estén usando los puertos 22100 y 22101.

### 5. Documentación Completa

- **README_INICIO.md**: Guía de inicio rápido
- **GUIA_P2P.md**: Documentación completa de funcionalidades P2P
- **INSTRUCCIONES_RAPIDAS.md**: Referencia rápida
- **CAMBIOS_REALIZADOS.md**: Este archivo

---

## 🎯 Cómo Usar Ahora

### Opción 1: Inicio Automático (Más Fácil)

```cmd
cd Server-Nicolas
iniciar-ambos-servidores.bat
```

Esto abrirá 2 ventanas, cada una con un servidor.

### Opción 2: Inicio Manual

**Terminal 1:**
```cmd
cd Server-Nicolas
server1.bat
```

**Terminal 2:**
```cmd
cd Server-Nicolas
server2.bat
```

---

## 🔍 Verificación

### 1. Verificar que los puertos están en uso

```cmd
verificar-puertos.bat
```

Deberías ver:
```
[✓] Puerto 22100 está EN USO
[✓] Puerto 22101 está EN USO
```

### 2. Verificar conexión P2P en los logs

Busca en cualquiera de las ventanas de los servidores:

```
✓ [HeartbeatService] Heartbeats enviados: 1 exitosos, 0 fallidos
✓ [HeartbeatService] Estadísticas de red P2P:
- Total de peers: 2
- Peers activos: 2
- Peers offline: 0
```

---

## 📊 Comparación Antes vs Ahora

| Aspecto | Antes | Ahora |
|---------|-------|-------|
| **Puertos** | Ambos en 22100 ❌ | 22100 y 22101 ✅ |
| **Timeout** | 90 segundos ⚠️ | 5 minutos ✅ |
| **Configuración** | Archivo único | Argumentos por servidor ✅ |
| **Inicio** | Manual complicado | Scripts automáticos ✅ |
| **Verificación** | Manual | Script de verificación ✅ |
| **Documentación** | Mínima | Completa ✅ |

---

## 🎉 Resultado

Ahora puedes:

1. ✅ Iniciar 2 servidores en puertos diferentes
2. ✅ Verificar fácilmente que están corriendo
3. ✅ Ver la conexión P2P funcionando
4. ✅ Detener los servidores cuando quieras
5. ✅ Entender qué hace cada servidor

---

## 🚀 Próximos Pasos

Para ver las funcionalidades P2P en acción:

1. Inicia ambos servidores
2. Conecta un cliente al puerto 22100
3. Conecta otro cliente al puerto 22101
4. Registra usuarios en cada servidor
5. Crea canales y envía mensajes
6. Observa cómo se sincronizan entre servidores

---

## 📝 Notas Técnicas

### ¿Por qué argumentos de línea de comandos?

Los argumentos de línea de comandos (`--server.port=22101`) tienen **mayor prioridad** que los archivos de configuración en Spring Boot. Esto garantiza que cada servidor use su configuración específica sin importar qué archivo `application.properties` esté en el JAR.

### ¿Por qué 5 minutos de timeout?

Un timeout de 90 segundos es muy corto para redes reales. Con 5 minutos:
- Tolera retrasos de red temporales
- Permite reiniciar un servidor sin que el otro lo marque como offline inmediatamente
- Es más realista para entornos de producción

### ¿Por qué bases de datos separadas?

Cada servidor necesita su propia base de datos para:
- Evitar conflictos de escritura
- Simular servidores realmente independientes
- Probar la sincronización P2P correctamente
