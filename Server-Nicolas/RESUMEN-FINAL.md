# ✅ Sistema de Chat P2P - Configuración Final

## 🎯 Estado Actual

El sistema está **completamente funcional** y listo para ejecutarse en múltiples PCs en red local.

## 📁 Archivos de Configuración

### Archivos Necesarios (Mínimos)

```
Server-Nicolas/
├── comunes/server-app/target/
│   └── server-app-1.0-SNAPSHOT-jar-with-dependencies.jar  ← Ejecutable
├── config/
│   └── database.properties                                 ← Config BD
├── comunes/server-app/src/main/resources/
│   └── application.properties                              ← Config servidor
└── docker-compose.yml                                      ← MySQL
```

## 🚀 Inicio Rápido

### En Esta PC (Servidor Principal)

```bash
# 1. Levantar MySQL
docker-compose up -d

# 2. Crear usuario
docker exec mysql-chat-server mysql -uroot -proot123 -e "CREATE USER IF NOT EXISTS 'chatuser'@'%' IDENTIFIED WITH mysql_native_password BY 'chatpass123'; GRANT ALL PRIVILEGES ON *.* TO 'chatuser'@'%'; FLUSH PRIVILEGES;"

# 3. Iniciar servidor
java -jar comunes\server-app\target\server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### En Otras PCs (Peers)

1. **Copiar archivos:**
   - `server-app-1.0-SNAPSHOT-jar-with-dependencies.jar`
   - Carpeta `config/`
   - `docker-compose.yml`

2. **Levantar MySQL y crear usuario** (igual que arriba)

3. **Editar `application.properties`:**
   ```properties
   # Cambiar solo esta línea con la IP del servidor principal
   peer.bootstrap.nodes=192.168.1.X:22100
   ```

4. **Iniciar servidor:**
   ```bash
   java -jar server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

## 🔧 Configuración Importante

### application.properties

**Para Servidor Principal:**
```properties
server.port=22100
peer.server.port=22200
peer.bootstrap.nodes=
```

**Para Peers (otras PCs):**
```properties
server.port=22100
peer.server.port=22200
peer.bootstrap.nodes=192.168.1.5:22100  # IP del servidor principal
```

### Firewall (Windows)

```bash
netsh advfirewall firewall add rule name="Chat Server TCP" dir=in action=allow protocol=TCP localport=22100
netsh advfirewall firewall add rule name="Chat Server P2P" dir=in action=allow protocol=TCP localport=22200
```

## 📊 Puertos Utilizados

| Puerto | Uso                     | Debe estar abierto |
|--------|-------------------------|--------------------|
| 22100  | Servidor TCP (clientes) | ✅ Sí              |
| 22200  | Servidor P2P (servers)  | ✅ Sí              |
| 3306   | MySQL (local)           | ❌ No (solo local) |

## ✅ Verificación

### Logs Esperados al Iniciar

```
✓ Servidor de Chat iniciado en el puerto 22100
✓ Servidor P2P iniciado en puerto 22200
✓ PeerConnectionManager inicializado
✓ Dirección IP del servidor detectada: X.X.X.X
```

### Si es un Peer (no servidor principal)

```
✓ Iniciando auto-registro con bootstrap peers: X.X.X.X:22100
✓ Peer bootstrap registrado exitosamente
✓ Conexión saliente establecida con peer
```

## 🎮 Interfaz Gráfica

Al iniciar el servidor, se abre una ventana con botones:

- **Monitor de Peers** - Ver peers conectados
- **Topología de Red** - Ver estructura de la red P2P
- **Usuarios P2P** - Ver usuarios distribuidos en la red

## 🔍 Comandos Útiles

### Ver contenedores Docker
```bash
docker ps
```

### Ver logs del servidor
```bash
# Los logs se guardan en:
logs/server.log
```

### Conectarse a MySQL
```bash
docker exec -it mysql-chat-server mysql -uchatuser -pchatpass123 chat_db
```

### Ver peers registrados
```sql
SELECT * FROM peers;
```

### Ver usuarios
```sql
SELECT * FROM users;
```

## 🐛 Solución de Problemas

### "Access denied for user 'chatuser'"

```bash
docker exec mysql-chat-server mysql -uroot -proot123 -e "GRANT ALL PRIVILEGES ON *.* TO 'chatuser'@'%'; FLUSH PRIVILEGES;"
```

### "Address already in use"

```bash
# Ver qué está usando el puerto
netstat -ano | findstr "22100"

# Matar el proceso
taskkill /PID <PID> /F
```

### "Connection refused" desde peer

1. Verifica que el servidor principal esté corriendo
2. Verifica la IP en `peer.bootstrap.nodes`
3. Verifica el firewall
4. Haz ping: `ping 192.168.1.X`

### Peers no aparecen en el monitor

- Espera 5 segundos (el auto-registro tiene un delay)
- Refresca el monitor
- Verifica los logs

## 📝 Notas Importantes

### Auto-Registro
- Los peers se registran **automáticamente** al iniciar
- No necesitas enviar comandos desde clientes
- El registro ocurre 5 segundos después de iniciar

### Seguridad
- La acción `añadirPeer` está **deshabilitada** para clientes
- Solo el sistema puede registrar peers automáticamente
- Para producción, cambiar contraseñas y usar TLS

### Base de Datos
- Cada PC tiene su **propia base de datos** independiente
- Los peers se sincronizan a través de la red P2P
- Los usuarios se registran en su servidor local

## 🎯 Próximos Pasos

1. ✅ **Compilar** - `mvn clean package -DskipTests`
2. ✅ **Copiar JAR** a otras PCs
3. ✅ **Configurar** `peer.bootstrap.nodes` en cada peer
4. ✅ **Iniciar** servidor principal primero
5. ✅ **Iniciar** peers después
6. ✅ **Verificar** en Monitor de Peers

## 📚 Documentación Adicional

- `INSTRUCCIONES-MULTIPLES-PCS.md` - Guía detallada para múltiples PCs
- `README_PROYECTO.md` - Documentación general del proyecto

## ✨ Características Implementadas

✅ Auto-registro de peers  
✅ Heartbeat automático  
✅ Sincronización de usuarios  
✅ Enrutamiento de mensajes P2P  
✅ Monitor de peers en tiempo real  
✅ Topología de red  
✅ Múltiples bases de datos independientes  
✅ Configuración simplificada  

---

**¡El sistema está listo para usar!** 🚀
