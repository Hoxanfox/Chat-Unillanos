# 🚀 Instrucciones de Ejecución - Sistema de Chat P2P

## 📋 Requisitos Previos

- Java 11 o superior
- Maven
- Docker y Docker Compose

## 🔧 Configuración Inicial

### 1. Levantar Bases de Datos

```bash
docker-compose up -d
```

Esto creará 3 contenedores MySQL:
- **mysql-server-22100** (Puerto 3306) → Base de datos: `server-22100`
- **mysql-server-22300** (Puerto 3307) → Base de datos: `server-22300`
- **mysql-server-22400** (Puerto 3308) → Base de datos: `server-22400`

### 2. Crear Usuarios en las Bases de Datos

```bash
docker exec mysql-server-22100 mysql -uroot -proot123 -e "CREATE USER IF NOT EXISTS 'chatuser'@'%' IDENTIFIED WITH mysql_native_password BY 'chatpass123'; GRANT ALL PRIVILEGES ON *.* TO 'chatuser'@'%'; FLUSH PRIVILEGES;"

docker exec mysql-server-22300 mysql -uroot -proot123 -e "CREATE USER IF NOT EXISTS 'chatuser'@'%' IDENTIFIED WITH mysql_native_password BY 'chatpass123'; GRANT ALL PRIVILEGES ON *.* TO 'chatuser'@'%'; FLUSH PRIVILEGES;"

docker exec mysql-server-22400 mysql -uroot -proot123 -e "CREATE USER IF NOT EXISTS 'chatuser'@'%' IDENTIFIED WITH mysql_native_password BY 'chatpass123'; GRANT ALL PRIVILEGES ON *.* TO 'chatuser'@'%'; FLUSH PRIVILEGES;"
```

### 3. Compilar el Proyecto

```bash
mvn clean package -DskipTests
```

## ▶️ Ejecutar los Servidores

### Servidor 1 - Servidor Principal (Puerto 22100)

**Terminal 1:**
```bash
start-server1.bat
```

### Servidor 2 - Peer A (Puerto 22300)

**Terminal 2:**
```bash
start-server2.bat
```

### Servidor 3 - Peer B (Puerto 22400)

**Terminal 3:**
```bash
start-server3.bat
```

## 📁 Estructura de Archivos de Configuración

```
config/
├── database.properties              # Archivo temporal (se sobrescribe al iniciar)
├── database-server1.properties      # Configuración DB Servidor 1
├── database-server2.properties      # Configuración DB Servidor 2
├── database-server3.properties      # Configuración DB Servidor 3
├── application-server1.properties   # Configuración App Servidor 1
├── application-server2.properties   # Configuración App Servidor 2
└── application-server3.properties   # Configuración App Servidor 3
```

## 🔗 Configuración de Red P2P

### Servidor 1 (Principal)
- Puerto TCP: **22100**
- Puerto P2P: **22200**
- Base de Datos: **server-22100** (MySQL 3306)
- Bootstrap Peers: Ninguno (es el servidor principal)

### Servidor 2 (Peer A)
- Puerto TCP: **22300**
- Puerto P2P: **22301**
- Base de Datos: **server-22300** (MySQL 3307)
- Bootstrap Peers: **127.0.0.1:22100** (se conecta al servidor principal)

### Servidor 3 (Peer B)
- Puerto TCP: **22400**
- Puerto P2P: **22401**
- Base de Datos: **server-22400** (MySQL 3308)
- Bootstrap Peers: **127.0.0.1:22100** (se conecta al servidor principal)

## 🧪 Probar el Sistema P2P

### 1. Registrar Peers en el Servidor Principal

Desde un cliente conectado al **Servidor 1 (22100)**:

```json
{
  "action": "añadirPeer",
  "data": {
    "ip": "127.0.0.1",
    "puerto": 22300
  }
}
```

```json
{
  "action": "añadirPeer",
  "data": {
    "ip": "127.0.0.1",
    "puerto": 22400
  }
}
```

### 2. Verificar Peers

Usar el panel "Monitor de Peers" en la interfaz gráfica del Servidor 1.

### 3. Probar Enrutamiento de Mensajes

1. Conectar **Cliente A** al Servidor 2 (22300)
2. Conectar **Cliente B** al Servidor 3 (22400)
3. Enviar mensaje desde Cliente A a Cliente B
4. El mensaje debe enrutarse a través del Servidor 1

## 🛑 Detener el Sistema

### Detener Servidores
Presiona `Ctrl+C` en cada terminal.

### Detener Bases de Datos

```bash
docker-compose down
```

### Eliminar Datos (Opcional)

```bash
docker-compose down -v
```

## 🔍 Verificar Estado

### Ver Contenedores Docker

```bash
docker ps
```

### Ver Logs de un Servidor

Los logs se guardan en:
- `logs/server-22100.log`
- `logs/server-22300.log`
- `logs/server-22400.log`

### Conectarse a MySQL

```bash
# Servidor 1
docker exec -it mysql-server-22100 mysql -uchatuser -pchatpass123 server-22100

# Servidor 2
docker exec -it mysql-server-22300 mysql -uchatuser -pchatpass123 server-22300

# Servidor 3
docker exec -it mysql-server-22400 mysql -uchatuser -pchatpass123 server-22400
```

## 🎯 Escenarios de Prueba

### Escenario 1: Comunicación Básica P2P
1. Iniciar los 3 servidores
2. Registrar usuarios en cada servidor
3. Registrar peers en el servidor principal
4. Enviar mensajes entre usuarios de diferentes servidores

### Escenario 2: Sincronización de Usuarios
1. Desde el Servidor 1, usar la acción `sincronizarUsuarios`
2. Verificar que se muestran usuarios de todos los servidores

### Escenario 3: Topología de Red
1. Abrir panel "Topología de Red" en el Servidor 1
2. Verificar que se muestran los 3 servidores
3. Verificar distribución de usuarios

### Escenario 4: Heartbeat y Reconexión
1. Detener el Servidor 2
2. Esperar 60 segundos
3. Verificar que aparece como OFFLINE en el monitor
4. Reiniciar el Servidor 2
5. Verificar reconexión automática

## 📊 Puertos Utilizados

| Servidor | Puerto TCP | Puerto P2P | Puerto MySQL |
|----------|-----------|-----------|--------------|
| Server 1 | 22100     | 22200     | 3306         |
| Server 2 | 22300     | 22301     | 3307         |
| Server 3 | 22400     | 22401     | 3308         |

## ⚠️ Solución de Problemas

### Error: "Access denied for user"
- Verificar que los usuarios MySQL fueron creados correctamente
- Ejecutar los comandos de creación de usuarios nuevamente

### Error: "Address already in use"
- Verificar que no hay otro proceso usando los puertos
- Usar `netstat -ano | findstr "22100"` para verificar

### Error: "Connection refused"
- Verificar que Docker está corriendo
- Verificar que los contenedores MySQL están activos con `docker ps`

### Los peers no se conectan
- Verificar que el Servidor 1 está corriendo primero
- Verificar los logs en `logs/server-*.log`
- Verificar que los peers están registrados en la base de datos
