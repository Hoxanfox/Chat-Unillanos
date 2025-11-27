# Guía para Migrar el Cliente a Netty con LengthField Protocol

## Problema Resuelto
El servidor ahora usa **LengthFieldPrepender/Decoder** que agrega 4 bytes al inicio de cada mensaje indicando su tamaño. El cliente debe usar el mismo protocolo.

## Solución Implementada

### 1. Archivos Creados para el Cliente

He creado 3 archivos nuevos que debes agregar a tu proyecto Cliente:

#### `NettyClienteTransporte.java`
Reemplaza a `TransporteTCP.java`. Usa Netty en lugar de Socket plano.

#### `ClienteInboundHandler.java`
Handler que procesa mensajes entrantes del servidor.

#### `NettySessionAdapter.java`
Adaptador que convierte Channel de Netty → PrintWriter/BufferedReader para mantener compatibilidad.

### 2. Cómo Integrar

#### Opción A: Reemplazar TransporteTCP
Renombra tu `TransporteTCP.java` actual a `TransporteTCP.java.old` y usa `NettyClienteTransporte` en su lugar.

#### Opción B: Actualizar FabricaTransporte
```java
package transporte;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;

public class FabricaTransporte {

    public DTOSesion iniciarConexion(DTOConexion datosConexion) {
        // CAMBIO: Usar NettyClienteTransporte en lugar de TransporteTCP
        ITransporte transporte = new NettyClienteTransporte();
        return transporte.conectar(datosConexion);
    }
}
```

### 3. Agregar Dependencia Netty

En el `pom.xml` del módulo de Transporte del Cliente, agrega:

```xml
<dependencies>
    <!-- Netty para comunicación de red -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>4.1.104.Final</version>
    </dependency>
    
    <!-- Tu DTO existente -->
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>DTO</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 4. Estructura de Carpetas Esperada

```
Cliente/
  Negocio/
    Datos/
      Transporte/
        src/main/java/transporte/
          ITransporte.java (existente)
          NettyClienteTransporte.java (NUEVO)
          ClienteInboundHandler.java (NUEVO)
          NettySessionAdapter.java (NUEVO)
          FabricaTransporte.java (modificar)
```

### 5. Ventajas de Esta Solución

✅ **Protocolo Unificado**: Cliente y Servidor usan LengthField (4 bytes + mensaje)  
✅ **Compatible**: `DTOSesion` sigue usando `PrintWriter` y `BufferedReader`  
✅ **Robusto**: No más errores de "frame length exceeds"  
✅ **Escalable**: Netty maneja múltiples conexiones eficientemente  

### 6. Verificación

Cuando el cliente se conecte, verás:
```
[NettyCliente] ✓ Conexión establecida con 192.168.1.100:9000
[ClienteHandler] ✓ Canal activo con servidor
```

En el servidor verás:
```
[Netty-Inbound] >>> Canal ACTIVO con: 192.168.1.50:45123
```

Y NO verás el error:
```
Adjusted frame length exceeds 52428800: 2065850727
```

## Archivos Adjuntos

Los 3 archivos Java están listos para copiar en tu proyecto Cliente.

**IMPORTANTE**: Asegúrate de que la estructura de paquetes coincida con tu proyecto.

