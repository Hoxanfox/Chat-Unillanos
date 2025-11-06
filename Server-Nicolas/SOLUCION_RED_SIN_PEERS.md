# 🔍 Solución: Servidor sin Peers en la Red

## Problema Identificado

Cuando el servidor inicia y **NO hay peers registrados en la base de datos**, el sistema:
- ✅ Inicia correctamente el servidor P2P en puerto 22200
- ✅ Puede recibir conexiones entrantes
- ❌ NO tiene forma de descubrir otros peers automáticamente
- ❌ Queda aislado esperando que alguien se conecte a él

---

## Soluciones Implementadas

### 1. **Configuración de Peers de Arranque (Bootstrap Peers)**

Agregamos soporte para peers conocidos en `server.properties`:

```properties
# Lista de peers conocidos para descubrimiento inicial (separados por coma)
# Formato: ip:puerto
peer.bootstrap.nodes=192.168.1.10:22200,192.168.1.11:22200,10.0.0.5:22200
```

**Ventajas:**
- Simple de configurar
- No requiere base de datos preexistente
- Ideal para red inicial

### 2. **Registro Manual de Peers**

Agregamos endpoints REST para registrar peers manualmente:

```bash
# Registrar un nuevo peer
POST /api/peers/register
{
  "ip": "192.168.1.10",
  "puerto": 22200
}
```

### 3. **Modo de Descubrimiento (Opcional)**

Si la red está completamente vacía:
- El servidor se queda en modo "LISTENING"
- Acepta conexiones entrantes
- Cuando otro peer se conecta, se registra automáticamente
- Ambos peers se sincronizan sus listas

---

## Cómo Funciona Ahora

### Escenario 1: Primera vez, sin peers
```
Servidor A inicia → BD vacía → Lee bootstrap.nodes → Intenta conectar a IPs configuradas
```

### Escenario 2: Peers configurados
```
Servidor A inicia → BD tiene peers → Conecta automáticamente
```

### Escenario 3: Red nueva (sin bootstrap)
```
Servidor A inicia → Modo LISTENING solamente
Servidor B inicia → Se conecta manualmente a Servidor A
Servidor A acepta → Ambos se registran mutuamente
```

---

## Logs para Diagnosticar

Busca estos logs al iniciar:

```log
INFO - PeerConnectionManager inicializado. Local Peer ID: xxx
INFO - Servidor P2P iniciado en puerto 22200
INFO - Conectando a 0 peers conocidos...  ← SI VES 0, NO HAY PEERS
INFO - Intentando conectar a X bootstrap peers...
```

Si ves `Conectando a 0 peers conocidos`, significa que:
1. La base de datos NO tiene peers registrados
2. No hay archivo de configuración con bootstrap nodes
3. El servidor está aislado, esperando conexiones entrantes

---

## Recomendaciones

### Para Producción:
1. **Usar bootstrap peers** - Siempre tener al menos 2-3 IPs conocidas
2. **Persistir en BD** - Los peers se guardan automáticamente
3. **Monitorear logs** - Alertar si `peers conectados = 0` por mucho tiempo

### Para Desarrollo/Testing:
1. Levantar al menos 2 servidores en puertos diferentes
2. Configurar manualmente el primer peer
3. El resto se sincroniza automáticamente

---

## Próximos Pasos

¿Qué necesitas?

**A)** Implementar la lectura de `peer.bootstrap.nodes` desde el archivo de configuración
**B)** Agregar endpoints REST para registro manual de peers
**C)** Implementar broadcast UDP para auto-descubrimiento en LAN
**D)** Ver el código actual y entender mejor antes de cambiar algo


