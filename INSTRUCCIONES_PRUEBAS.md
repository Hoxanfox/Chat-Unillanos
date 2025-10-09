# 🧪 Guía de Pruebas - Chat-Unillanos

Esta guía te ayudará a probar el sistema completo (Cliente + Servidor) de Chat-Unillanos.

## 📋 Requisitos Previos

Antes de comenzar, asegúrate de tener instalado:

- ✅ **Java JDK 17** o superior
- ✅ **Maven 3.6** o superior
- ✅ **JavaFX** (incluido en el JDK 17+)

### Verificar instalación:

```bash
java -version
mvn -version
```

---

## 🚀 Inicio Rápido

### Opción A: Usando Scripts (Recomendado en Windows)

#### 1. Iniciar el Servidor

En una terminal, navega al directorio del proyecto y ejecuta:

```cmd
cd Server
iniciar-servidor.bat
```

Deberías ver:
```
╔════════════════════════════════════════════════╗
║   SERVIDOR CHAT-UNILLANOS INICIADO            ║
╚════════════════════════════════════════════════╝
🚀 Escuchando en puerto: 8888
⏳ Esperando conexiones de clientes...
```

#### 2. Iniciar el Cliente (puedes abrir múltiples ventanas)

En **otra terminal**, ejecuta:

```cmd
cd Cliente
iniciar-cliente.bat
```

### Opción B: Usando Maven Directamente

#### 1. Iniciar el Servidor

```bash
cd Server
mvn clean compile
mvn exec:java
```

#### 2. Iniciar el Cliente

```bash
cd Cliente
mvn clean compile
cd Presentacion/Main
mvn javafx:run
```

---

## 🔍 Escenarios de Prueba

### ✅ Prueba 1: Conexión al Servidor

**Objetivo:** Verificar que el cliente puede conectarse al servidor.

**Pasos:**
1. Inicia el servidor (debe estar corriendo)
2. Inicia el cliente
3. En la ventana "Conectar al Servidor":
   - IP: `127.0.0.1`
   - Click en "Conectar"

**Resultado Esperado:**
- ✅ El servidor muestra: `✓ Nuevo cliente conectado. Total de conexiones: 1`
- ✅ El cliente avanza a la pantalla de login

**Si falla:**
- Verifica que el servidor esté corriendo
- Verifica que el puerto 8888 no esté en uso
- Revisa los logs de la consola

---

### ✅ Prueba 2: Autenticación de Usuario

**Objetivo:** Verificar que el cliente puede autenticarse correctamente.

**Pasos:**
1. Después de conectarte al servidor
2. En la pantalla de Login:
   - Email: `usuario1@test.com` (o cualquier email)
   - Contraseña: `123456` (o cualquier contraseña)
   - Click en "Ingresar"

**Resultado Esperado:**
- ✅ Servidor muestra:
  ```
  📩 Mensaje recibido: {"action":"authenticateUser",...}
  ✓ Usuario autenticado: usuario1
  📤 Respuesta enviada: {"action":"authenticateUser",...}
  ```
- ✅ Cliente avanza al dashboard (Lobby)
- ✅ Se muestra "ONLINE USERS (1)" con tu usuario

**Nota:** Por ahora, el servidor acepta cualquier email/password para pruebas.

---

### ✅ Prueba 3: Múltiples Usuarios Conectados

**Objetivo:** Verificar que múltiples usuarios pueden conectarse y verse entre sí.

**Pasos:**
1. Con el primer cliente ya autenticado (de la Prueba 2)
2. Abre **una segunda instancia del cliente** (ejecuta de nuevo)
3. Conéctate con otro email (ej: `usuario2@test.com`)
4. Inicia sesión

**Resultado Esperado:**
- ✅ Servidor muestra:
  ```
  ✓ Nuevo cliente conectado. Total de conexiones: 2
  ✓ Usuario autenticado: usuario2
  📢 Actualización de contactos difundida a todos los usuarios
  ```
- ✅ **Ambos clientes** actualizan su lista de contactos automáticamente
- ✅ Cliente 1 muestra: "ONLINE USERS (2)" → [usuario1, usuario2]
- ✅ Cliente 2 muestra: "ONLINE USERS (2)" → [usuario1, usuario2]

---

### ✅ Prueba 4: Desconexión de Usuario

**Objetivo:** Verificar que cuando un usuario se desconecta, los demás lo ven.

**Pasos:**
1. Con múltiples clientes conectados (de la Prueba 3)
2. Cierra una de las ventanas del cliente

**Resultado Esperado:**
- ✅ Servidor muestra:
  ```
  ✗ Usuario desconectado: usuario2
  ✗ Cliente desconectado. Total de conexiones: 1
  📢 Actualización de contactos difundida a todos los usuarios
  ```
- ✅ Los clientes restantes actualizan su lista automáticamente
- ✅ El usuario desconectado desaparece de la lista

---

### ✅ Prueba 5: Reconexión

**Objetivo:** Verificar que un usuario puede reconectarse después de desconectarse.

**Pasos:**
1. Cierra un cliente
2. Vuelve a abrir el cliente
3. Conéctate y autentícate nuevamente

**Resultado Esperado:**
- ✅ El usuario reaparece en la lista de contactos de todos
- ✅ No hay errores en el servidor

---

## 🐛 Solución de Problemas

### Problema: "Connection refused" al conectar

**Solución:**
- Verifica que el servidor esté corriendo
- Verifica que estés usando el puerto correcto (8888)
- En Windows: Verifica el firewall

### Problema: El cliente no avanza después del login

**Solución:**
- Revisa la consola del cliente y del servidor
- Verifica que el `GestorRespuesta` esté escuchando (debería decir "Gestor de respuestas iniciado")

### Problema: La lista de contactos no se actualiza

**Solución:**
- Verifica que el usuario esté autenticado (no solo conectado)
- Revisa los logs del servidor para ver si está difundiendo las actualizaciones
- Asegúrate de que el `Platform.runLater()` esté funcionando (es parte de JavaFX)

### Problema: "Address already in use"

**Solución:**
- El puerto 8888 está en uso
- Cierra otras instancias del servidor
- O cambia el puerto en `ServicioConexion.java` y `Main.java` del servidor

### Problema: Error al compilar el servidor

**Solución:**
```bash
cd Server
mvn clean install -U
```

### Problema: Error al compilar el cliente

**Solución:**
```bash
cd Cliente
mvn clean install -U
```

---

## 📊 Logs y Monitoreo

### Logs del Servidor

El servidor imprime logs detallados:
- 🚀 Inicio del servidor
- ✓ Conexiones/Desconexiones
- 📩 Mensajes recibidos
- 📤 Respuestas enviadas
- 📢 Difusiones a clientes

### Logs del Cliente

El cliente imprime:
- Intentos de conexión
- Peticiones enviadas (con JSON)
- Respuestas recibidas
- Estado del `GestorRespuesta`

---

## 🎯 Checklist de Pruebas Completas

Antes de considerar el sistema funcional, verifica:

- [ ] ✅ El servidor inicia correctamente
- [ ] ✅ El cliente puede conectarse al servidor
- [ ] ✅ El cliente puede autenticarse
- [ ] ✅ El cliente muestra la lista de usuarios en línea
- [ ] ✅ Múltiples clientes pueden conectarse simultáneamente
- [ ] ✅ La lista de contactos se actualiza automáticamente
- [ ] ✅ Las desconexiones se reflejan en todos los clientes
- [ ] ✅ Los usuarios pueden reconectarse sin problemas

---

## 📝 Notas Importantes

### Limitaciones Actuales (Esperadas)

- ⚠️ **No hay validación de credenciales**: Cualquier email/password es aceptado
- ⚠️ **No hay persistencia**: Todo se pierde al cerrar el servidor
- ⚠️ **No hay mensajes de chat**: Solo está implementada la lista de contactos
- ⚠️ **Los canales no funcionan**: La UI existe pero no está conectada al servidor

### Funcionalidades Pendientes

- Mensajes privados entre usuarios
- Canales/grupos de chat
- Historial de mensajes
- Validación real de usuarios
- Base de datos

---

## 🔄 Flujo de Comunicación (Para Referencia)

```
Cliente                          Servidor
  |                                 |
  |------ Conexión TCP -------->    |  (Netty acepta)
  |                                 |
  |<---- Conexión exitosa -------   |
  |                                 |
  |-- DTORequest(authenticate) -->  |
  |                                 |
  |                                 |  (Procesa y registra usuario)
  |<- DTOResponse(success) --------  |
  |                                 |
  |                                 |  (Difunde a todos)
  |<- DTOResponse(updateContacts)-  |
  |                                 |
```

---

## 📞 Siguiente Paso: Implementar Chat

Una vez que todas estas pruebas pasen, el siguiente paso es implementar:

1. Mensajes privados entre usuarios
2. Canales de chat grupal
3. Historial de mensajes
4. Notificaciones en tiempo real

---

¿Necesitas ayuda adicional? Revisa los logs en la consola del servidor y del cliente para obtener más información sobre cualquier error.

