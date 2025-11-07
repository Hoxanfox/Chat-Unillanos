# 🌐 Instrucciones para Ejecutar en Múltiples PCs (Red Local)

## 📋 Requisitos Previos

**En cada PC:**
- Java 11 o superior
- Maven (solo para compilar)
- Docker y Docker Compose (solo para la base de datos)
- Estar en la **misma red local**

## 🏗️ Arquitectura de Red

```
┌─────────────────────────────────────────────────────────────┐
│                    RED LOCAL (192.168.1.x)                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────┐      ┌──────────────────┐           │
│  │   PC 1 (Server)  │      │   PC 2 (Peer A)  │           │
│  │  IP: 192.168.1.5 │      │  IP: 192.168.1.6 │           │
│  │  Puerto: 22100   │◄─────┤  Puerto: 22100   │           │
│  │  MySQL: 3306     │      │  MySQL: 3306     │           │
│  │  Bootstrap: -    │      │  Bootstrap:      │           │
│  │                  │      │  192.168.1.5     │           │
│  └──────────────────┘      └──────────────────┘           │
│           ▲                                                │
│           │                                                │
│           │         ┌──────────────────┐                  │
│           │         │   PC 3 (Peer B)  │                  │
│           │         │  IP: 192.168.1.7 │                  │
│           └─────────┤  Puerto: 22100   │                  │
│                     │  MySQL: 3306     │                  │
│                     │  Bootstrap:      │                  │
│                     │  192.168.1.5     │                  │
│                     └──────────────────┘                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Configuración Paso a Paso

### 📦 Paso 1: Preparar el Proyecto (Solo una vez)

En **cualquier PC**, compila el proyecto:

```bash
mvn clean package -DskipTests
```

Esto genera el archivo:
```
comunes/server-app/target/server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Copia este JAR a todas las PCs** donde quieras ejecutar un servidor.

---

### 🖥️ Paso 2: Configurar PC 1 (Servidor Principal)

#### 2.1 Levantar Base de Datos

```bash
docker-compose up -d
```

#### 2.2 Crear Usuario MySQL

```bash
docker exec mysql-chat-server mysql -uroot -proot123 -e "CREATE USER IF NOT EXISTS 'chatuser'@'%' IDENTIFIED WITH mysql_native_password BY 'chatpass123'; GRANT ALL PRIVILEGES ON *.* TO 'chatuser'@'%'; FLUSH PRIVILEGES;"
```

#### 2.3 Obtener IP de la PC

**Windows:**
```bash
ipconfig
```

**Linux/Mac:**
```bash
ifconfig
# o
ip addr show
```

Busca tu IP local (ejemplo: `192.168.1.5`)

#### 2.4 Configurar Firewall

**Windows:**
```bash
# Permitir puerto 22100 (TCP del servidor)
netsh advfirewall firewall add rule name="Chat Server TCP" dir=in action=allow protocol=TCP localport=22100

# Permitir puerto 22200 (P2P)
netsh advfirewall firewall add rule name="Chat Server P2P" dir=in action=allow protocol=TCP localport=22200
```

**Linux:**
```bash
sudo ufw allow 22100/tcp
sudo ufw allow 22200/tcp
```

#### 2.5 Editar application.properties

**NO necesitas cambiar nada**, pero puedes personalizar:

```properties
# Opcional: Cambiar el nombre del servidor
p2p.nombre.servidor=Servidor-Principal-PC1

# Dejar vacío (este es el servidor principal)
peer.bootstrap.nodes=
```

#### 2.6 Iniciar Servidor

```bash
java -jar server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Logs esperados:**
```
PeerConnectionManager inicializado. Local Peer ID: xxx-xxx-xxx
Servidor P2P iniciado en puerto 22200 (Cliente en puerto 22100)
Servidor de chat iniciado en puerto 22100
```

✅ **El servidor principal está listo y esperando conexiones**

---

### 🖥️ Paso 3: Configurar PC 2 (Peer A)

#### 3.1 Copiar Archivos

Copia a esta PC:
- `server-app-1.0-SNAPSHOT-jar-with-dependencies.jar`
- Carpeta `config/` completa
- `docker-compose.yml`

#### 3.2 Levantar Base de Datos

```bash
docker-compose up -d
```

#### 3.3 Crear Usuario MySQL

```bash
docker exec mysql-chat-server mysql -uroot -proot123 -e "CREATE USER IF NOT EXISTS 'chatuser'@'%' IDENTIFIED WITH mysql_native_password BY 'chatpass123'; GRANT ALL PRIVILEGES ON *.* TO 'chatuser'@'%'; FLUSH PRIVILEGES;"
```

#### 3.4 Configurar Firewall

```bash
# Windows
netsh advfirewall firewall add rule name="Chat Server TCP" dir=in action=allow protocol=TCP localport=22100
netsh advfirewall firewall add rule name="Chat Server P2P" dir=in action=allow protocol=TCP localport=22200

# Linux
sudo ufw allow 22100/tcp
sudo ufw allow 22200/tcp
```

#### 3.5 Editar application.properties

**IMPORTANTE:** Configura el bootstrap con la IP del Servidor Principal (PC 1):

```properties
# Personalizar nombre
p2p.nombre.servidor=Servidor-Peer-A-PC2

# CONFIGURAR CON LA IP DE PC 1
peer.bootstrap.nodes=192.168.1.5:22100
```

#### 3.6 Iniciar Servidor

```bash
java -jar server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Logs esperados:**
```
PeerConnectionManager inicializado. Local Peer ID: yyy-yyy-yyy
Iniciando auto-registro con bootstrap peers: 192.168.1.5:22100
✓ Peer bootstrap registrado exitosamente: 192.168.1.5:22100
✓ Conexión saliente establecida con peer xxx-xxx-xxx
```

✅ **Peer A conectado al servidor principal**

---

### 🖥️ Paso 4: Configurar PC 3 (Peer B)

Repite los mismos pasos que PC 2, pero:

```properties
# Personalizar nombre
p2p.nombre.servidor=Servidor-Peer-B-PC3

# CONFIGURAR CON LA IP DE PC 1
peer.bootstrap.nodes=192.168.1.5:22100
```

---

## ✅ Verificar que Todo Funciona

### Opción 1: Interfaz Gráfica (PC 1)

En la ventana del servidor principal:
- Clic en **"Monitor de Peers"**
- Deberías ver 2 peers conectados (PC 2 y PC 3)

### Opción 2: Logs

En los logs de PC 1, busca:
```
Nuevo cliente conectado: 192.168.1.6  (PC 2)
Nuevo cliente conectado: 192.168.1.7  (PC 3)
```

### Opción 3: Conectar Clientes

1. **Cliente en PC 1** → Conectar a `192.168.1.5:22100`
2. **Cliente en PC 2** → Conectar a `192.168.1.6:22100`
3. **Cliente en PC 3** → Conectar a `192.168.1.7:22100`

Envía mensajes entre usuarios de diferentes PCs y verifica que se enrutan correctamente.

---

## 🔧 Configuración Avanzada

### Múltiples Servidores Bootstrap

Si quieres que un peer se conecte a múltiples servidores:

```properties
peer.bootstrap.nodes=192.168.1.5:22100,192.168.1.6:22100
```

### Cambiar Puerto del Servidor

Si el puerto 22100 está ocupado:

```properties
server.port=22500
p2p.puerto=22500
peer.server.port=22501
```

**No olvides actualizar el firewall y el bootstrap en otros peers.**

---

## 🐛 Troubleshooting

### "Connection refused" al iniciar Peer

**Causa:** El servidor principal (PC 1) no está accesible.

**Soluciones:**
1. Verifica que PC 1 esté encendida y el servidor corriendo
2. Verifica la IP en `peer.bootstrap.nodes`
3. Verifica el firewall de PC 1
4. Haz ping: `ping 192.168.1.5`

### "Access denied for user 'chatuser'"

**Causa:** El usuario MySQL no fue creado correctamente.

**Solución:**
```bash
docker exec mysql-chat-server mysql -uroot -proot123 -e "GRANT ALL PRIVILEGES ON *.* TO 'chatuser'@'%'; FLUSH PRIVILEGES;"
```

### Los peers no aparecen en el monitor

**Causa:** El auto-registro tarda 5 segundos.

**Solución:** Espera unos segundos y refresca el monitor.

### "Address already in use"

**Causa:** El puerto ya está en uso.

**Solución:**
```bash
# Windows
netstat -ano | findstr "22100"

# Linux
lsof -i :22100
```

Mata el proceso o cambia el puerto en `application.properties`.

---

## 📊 Puertos Utilizados

| Puerto | Uso                          | Protocolo |
|--------|------------------------------|-----------|
| 22100  | Servidor TCP (clientes)      | TCP       |
| 22200  | Servidor P2P (entre servers) | TCP       |
| 3306   | MySQL (local)                | TCP       |

---

## 🎯 Resumen de Configuración

### PC 1 (Servidor Principal)
```properties
p2p.nombre.servidor=Servidor-Principal
peer.bootstrap.nodes=
```

### PC 2 (Peer A)
```properties
p2p.nombre.servidor=Servidor-Peer-A
peer.bootstrap.nodes=192.168.1.5:22100
```

### PC 3 (Peer B)
```properties
p2p.nombre.servidor=Servidor-Peer-B
peer.bootstrap.nodes=192.168.1.5:22100
```

---

## 🔐 Notas de Seguridad

⚠️ **Para producción:**
- Cambiar contraseñas de MySQL
- Usar HTTPS/TLS para comunicación
- Implementar autenticación entre peers
- Configurar firewall más restrictivo
- Usar VPN para conexiones entre PCs remotas
