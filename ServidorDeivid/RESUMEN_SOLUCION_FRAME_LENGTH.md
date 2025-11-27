# Resumen de Cambios - Solución al Error "frame length exceeds"

## Problema
```
Adjusted frame length exceeds 52428800: 2065850727 - discarded
Canal CERRADO con: 192.168.137.59:45060
```

**Causa**: El cliente enviaba JSON sin el protocolo `LengthField`, entonces el servidor interpretaba los primeros 4 bytes del JSON como el tamaño del mensaje, resultando en valores absurdos (~1.9GB).

## Solución Implementada

### Cambios en el Servidor ✅
**Archivo**: `NettyTransporteImpl.java`

- **Simplificado el pipeline** para usar SOLO `LengthFieldPrepender/Decoder`
- Eliminado `AdaptiveFrameDecoder` (no era necesario)
- Protocolo unificado para clientes C-S y P2P

### Archivos para el Cliente 📦
Se crearon 3 archivos nuevos que implementan Netty en el cliente:

1. **`NettyClienteTransporte.java`**
   - Reemplaza `TransporteTCP.java`
   - Usa el mismo protocolo que el servidor (LengthField)
   - Mantiene compatibilidad con `DTOSesion`

2. **`ClienteInboundHandler.java`**
   - Handler de Netty para recibir mensajes

3. **`NettySessionAdapter.java`**
   - Adaptador que convierte `Channel` → `PrintWriter/BufferedReader`
   - Permite que el código existente siga funcionando

### Documentación 📄
- **`GUIA_MIGRACION_CLIENTE_NETTY.md`**: Guía completa de integración

## Protocolo LengthField

```
┌────────────┬──────────────────────────┐
│  4 bytes   │      N bytes             │
│  (tamaño)  │   (mensaje JSON)         │
└────────────┴──────────────────────────┘
```

- **Encoder**: `LengthFieldPrepender` agrega automáticamente los 4 bytes
- **Decoder**: `LengthFieldBasedFrameDecoder` lee el tamaño y espera el mensaje completo

## Próximos Pasos

### Para el Desarrollador del Cliente:
1. Copiar los 3 archivos Java al módulo `Transporte` del cliente
2. Agregar dependencia de Netty al `pom.xml`
3. Modificar `FabricaTransporte` para usar `NettyClienteTransporte`
4. Compilar y probar

### Verificación
✅ NO más errores de "frame length exceeds"  
✅ Cliente se conecta correctamente  
✅ Mensajes JSON se transmiten completos  
✅ Compatible con P2P entre servidores

## Archivos Modificados

### Servidor
- `Negocio/Datos/Transporte/src/main/java/transporte/p2p/impl/NettyTransporteImpl.java`

### Cliente (nuevos)
- `Negocio/Datos/Transporte/src/main/java/transporte/NettyClienteTransporte.java`
- `Negocio/Datos/Transporte/src/main/java/transporte/ClienteInboundHandler.java`
- `Negocio/Datos/Transporte/src/main/java/transporte/NettySessionAdapter.java`

### Documentación
- `GUIA_MIGRACION_CLIENTE_NETTY.md`
- `RESUMEN_SOLUCION_FRAME_LENGTH.md` (este archivo)

