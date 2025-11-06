# 📋 Organización de Rutas del Servidor

## Estructura del RequestDispatcher

Las rutas están organizadas en **5 secciones principales**:

---

## 🔐 SECCIÓN 1: RUTAS DE AUTENTICACIÓN (CLIENTES)
Rutas para que los clientes inicien y cierren sesión.

| Acción | Descripción | Autenticación Requerida |
|--------|-------------|-------------------------|
| `authenticateuser` | Login de usuario | ❌ No |
| `logoutuser` | Cierre de sesión | ✅ Sí |

---

## 👥 SECCIÓN 2: RUTAS DE CONTACTOS Y USUARIOS (CLIENTES)
Rutas para que los clientes gestionen contactos.

| Acción | Descripción | Autenticación Requerida |
|--------|-------------|-------------------------|
| `listarcontactos` | Listar todos los contactos del usuario | ✅ Sí |

---

## 📢 SECCIÓN 3: RUTAS DE CANALES (CLIENTES)
Rutas para que los clientes gestionen canales de chat.

| Acción | Descripción | Autenticación Requerida |
|--------|-------------|-------------------------|
| `listarcanales` | Listar canales del usuario | ✅ Sí |

---

## 📁 SECCIÓN 4: RUTAS DE TRANSFERENCIA DE ARCHIVOS (CLIENTES)
Rutas para que los clientes suban y descarguen archivos (fotos, audios).

| Acción | Descripción | Autenticación Requerida |
|--------|-------------|-------------------------|
| `startfileupload` | Iniciar subida de archivo (autenticado) | ✅ Sí |
| `uploadfileforregistration` | Subir foto para registro | ❌ No |
| `uploadfilechunk` | Enviar chunk de archivo | ❌ No |
| `endfileupload` | Finalizar subida de archivo | ❌ No |
| `startfiledownload` | Iniciar descarga de archivo | ✅ Sí |
| `requestfilechunk` | Solicitar chunk de descarga | ✅ Sí |

---

## 🌐 SECCIÓN 5: RUTAS DE PEERS (P2P - SERVER TO SERVER)
**Rutas exclusivas para comunicación entre servidores en la red P2P.**

| Acción | Descripción | Autenticación Requerida |
|--------|-------------|-------------------------|
| `reportarlatido` | Reportar heartbeat del peer | ❌ No (P2P) |
| `añadirpeer` | Añadir un nuevo peer a la red | ❌ No (P2P) |
| `listarpeersdisponibles` | Listar peers disponibles en la red | ✅ Sí |
| `verificarestadopeer` | Verificar estado de un peer específico | ✅ Sí |
| `retransmitirpeticion` | Retransmitir petición de cliente a través de peer | ❌ No (P2P) |
| `actualizarlistapeers` | **[NUEVA]** Recibir actualización push de la lista completa de peers | ❌ No (P2P) |

---

## 📝 Notas Importantes

### Acciones Públicas (Sin Autenticación)
Las siguientes acciones NO requieren que el cliente esté autenticado:
- `authenticateuser` - Login
- `uploadfileforregistration` - Registro de foto
- `uploadfilechunk` - Chunks de archivo
- `endfileupload` - Finalizar upload
- **RUTAS P2P:**
  - `reportarlatido`
  - `añadirpeer`
  - `retransmitirpeticion`
  - `actualizarlistapeers` ⭐ **NUEVA**

### Rutas P2P vs Rutas de Clientes
- **Rutas de Clientes (Secciones 1-4)**: Usadas por aplicaciones cliente (usuarios finales)
- **Rutas de Peers (Sección 5)**: Usadas exclusivamente para comunicación server-to-server en la red P2P

### Nueva Ruta: `actualizarlistapeers`
Esta ruta permite que otros servidores envíen actualizaciones push de su lista completa de peers, manteniendo sincronizada la red P2P.

**Request:**
```json
{
  "action": "actualizarListaPeers",
  "payload": {
    "listaPeers": [
      {
        "peerId": "uuid-peer-A1",
        "ip": "192.168.1.5",
        "puerto": 9000,
        "conectado": "ONLINE"
      }
    ]
  }
}
```

**Response (Success):**
```json
{
  "action": "actualizarListaPeers",
  "status": "success",
  "message": "Peer añadido y lista de peers actualizada",
  "data": {
    "listaPeers": [...]
  }
}
```

---

## 🎯 Beneficios de la Nueva Organización

1. ✅ **Código más legible**: Fácil encontrar y mantener rutas
2. ✅ **Separación clara**: Rutas P2P separadas de rutas de clientes
3. ✅ **Mejor mantenimiento**: Agregar nuevas rutas es más sencillo
4. ✅ **Documentación implícita**: Los comentarios indican el propósito de cada sección
5. ✅ **Escalabilidad**: Fácil agregar nuevas secciones según sea necesario

