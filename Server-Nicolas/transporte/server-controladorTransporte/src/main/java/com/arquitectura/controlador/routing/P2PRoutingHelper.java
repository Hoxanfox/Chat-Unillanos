package com.arquitectura.controlador.routing;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Helper para enrutamiento automático P2P.
 *
 * Detecta automáticamente cuando una petición involucra a dos usuarios en peers distintos
 * y maneja la retransmisión transparentemente.
 *
 * FASE 1: Transmisión (El Cartero Puro)
 * - retransmitirpeticion ahora recibe el destino en el primer nivel
 * - No necesita abrir el paquete para saber a dónde enviarlo
 */
@Component
public class P2PRoutingHelper {

    private final IChatFachada chatFachada;
    private final Gson gson;

    @Autowired
    public P2PRoutingHelper(IChatFachada chatFachada, Gson gson) {
        this.chatFachada = chatFachada;
        this.gson = gson;
    }

    /**
     * Detecta si el destinatario está en otro peer y enruta automáticamente.
     *
     * @param destinatarioId ID del usuario destinatario
     * @param accionOriginal Acción original que se está ejecutando
     * @param payloadOriginal Payload de la petición original
     * @param handler Handler del cliente actual
     * @return Optional con la respuesta del peer remoto, o vacío si el usuario es local
     */
    public Optional<DTOResponse> enrutarSiEsNecesario(
            UUID destinatarioId,
            String accionOriginal,
            Map<String, Object> payloadOriginal,
            IClientHandler handler) {

        System.out.println("🔍 [P2PRouting] Verificando ubicación del destinatario: " + destinatarioId);

        try {
            // 1. Buscar al usuario destinatario primero en BD local
            List<UserResponseDto> todosUsuarios = chatFachada.usuarios().obtenerTodosLosUsuarios();
            UserResponseDto destinatario = null;

            for (UserResponseDto usuario : todosUsuarios) {
                if (usuario.getUserId().equals(destinatarioId)) {
                    destinatario = usuario;
                    break;
                }
            }

            // 2. Si NO está en BD local, buscar en peers remotos usando buscarUsuario
            if (destinatario == null) {
                System.out.println("⚠ [P2PRouting] Usuario no encontrado en BD local, buscando en peers remotos...");

                try {
                    // Usar el servicio P2P para buscar el usuario en toda la red
                    com.arquitectura.DTO.p2p.UserLocationResponseDto userLocation =
                        chatFachada.p2p().buscarUsuario(destinatarioId);

                    // Verificar si el usuario está conectado
                    if (!userLocation.isConectado() || userLocation.getPeerId() == null) {
                        System.out.println("❌ [P2PRouting] Usuario encontrado pero no está conectado");
                        throw new Exception("Usuario destinatario no está conectado a ningún peer");
                    }

                    System.out.println("✅ [P2PRouting] Usuario encontrado en peer remoto: " + userLocation.getPeerId());

                    // Crear un UserResponseDto temporal con la información encontrada
                    // Usando el constructor con 6 parámetros: userId, username, email, photoAddress, fechaRegistro, estado
                    destinatario = new UserResponseDto(
                        userLocation.getUsuarioId(),
                        userLocation.getUsername(),
                        "", // email (no disponible en UserLocationResponseDto)
                        "", // photoAddress (no disponible)
                        java.time.LocalDateTime.now(), // fechaRegistro (usar timestamp actual)
                        "ONLINE" // estado
                    );

                    // Establecer el peerId usando el setter
                    destinatario.setPeerId(userLocation.getPeerId());

                } catch (Exception e) {
                    System.err.println("❌ [P2PRouting] Error buscando usuario en peers remotos: " + e.getMessage());
                    throw new Exception("Usuario destinatario no encontrado en la red P2P: " + destinatarioId);
                }
            }

            // 3. Verificar si el destinatario está en este peer (local)
            UUID peerLocalId = chatFachada.p2p().obtenerPeerActualId();
            UUID peerDestinatarioId = destinatario.getPeerId();

            System.out.println("📍 [P2PRouting] Peer local: " + peerLocalId);
            System.out.println("📍 [P2PRouting] Peer destinatario: " + peerDestinatarioId);

            // Si el destinatario está en el mismo peer, no hay que retransmitir
            if (peerLocalId.equals(peerDestinatarioId)) {
                System.out.println("✓ [P2PRouting] Destinatario es local, no se requiere enrutamiento");
                return Optional.empty();
            }

            // 4. El destinatario está en otro peer, necesitamos retransmitir
            System.out.println("🌐 [P2PRouting] Destinatario está en peer remoto: " + peerDestinatarioId);
            System.out.println("📨 [P2PRouting] Enrutando petición '" + accionOriginal + "' al peer remoto...");

            // 5. Construir la petición limpia (FASE 1: Cartero Puro)
            // El peerDestinoId ahora va en el primer nivel, NO dentro del payload
            Map<String, Object> retransmisionPayload = new HashMap<>();

            // Información del peer origen
            retransmisionPayload.put("peerOrigen", Map.of(
                "peerId", peerLocalId.toString(),
                "nombreServidor", "Local"
            ));

            // CAMBIO CLAVE FASE 1: El destino está fuera, claro y en el primer nivel
            retransmisionPayload.put("peerDestinoId", peerDestinatarioId.toString());

            // IMPORTANTE: Agregar marca para evitar re-enrutamiento
            // Copiar el payload original y agregar peerDestinoId y marca de ya enrutado
            Map<String, Object> payloadModificado = new HashMap<>(payloadOriginal);
            payloadModificado.put("peerDestinoId", peerDestinatarioId.toString());
            payloadModificado.put("peerRemitenteId", peerLocalId.toString());
            payloadModificado.put("_yaEnrutado", true); // Marca para evitar re-enrutamiento

            // La petición del cliente es un "paquete sellado"
            Map<String, Object> peticionCliente = new HashMap<>();
            peticionCliente.put("action", accionOriginal);
            peticionCliente.put("payload", payloadModificado);

            retransmisionPayload.put("peticionCliente", peticionCliente);

            // 6. Crear DTORequest para retransmitir
            DTORequest retransmisionRequest = new DTORequest(
                "retransmitirpeticion",
                retransmisionPayload
            );

            // 7. Enviar al peer remoto usando la fachada
            DTOResponse respuestaPeer = chatFachada.p2p().retransmitirPeticion(
                peerDestinatarioId,
                retransmisionRequest
            );

            System.out.println("✅ [P2PRouting] Petición enrutada exitosamente al peer remoto");

            return Optional.of(respuestaPeer);

        } catch (Exception e) {
            System.err.println("❌ [P2PRouting] Error al enrutar petición: " + e.getMessage());
            e.printStackTrace();

            // Crear respuesta de error
            DTOResponse errorResponse = new DTOResponse(
                accionOriginal,
                "error",
                "Error al enrutar petición al peer remoto: " + e.getMessage(),
                null
            );

            return Optional.of(errorResponse);
        }
    }

    /**
     * Versión simplificada que solo retorna booleano si necesita enrutamiento.
     *
     * @param destinatarioId ID del usuario destinatario
     * @return true si el destinatario está en otro peer
     */
    public boolean necesitaEnrutamiento(UUID destinatarioId) {
        try {
            List<UserResponseDto> todosUsuarios = chatFachada.usuarios().obtenerTodosLosUsuarios();
            UserResponseDto destinatario = null;

            for (UserResponseDto usuario : todosUsuarios) {
                if (usuario.getUserId().equals(destinatarioId)) {
                    destinatario = usuario;
                    break;
                }
            }

            if (destinatario == null) {
                return false;
            }

            UUID peerLocalId = chatFachada.p2p().obtenerPeerActualId();
            UUID peerDestinatarioId = destinatario.getPeerId();

            return !peerLocalId.equals(peerDestinatarioId);
        } catch (Exception e) {
            System.err.println("❌ [P2PRouting] Error al verificar enrutamiento: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el peer ID del destinatario.
     *
     * @param destinatarioId ID del usuario destinatario
     * @return UUID del peer donde está el destinatario
     * @throws Exception si el usuario no existe
     */
    public UUID obtenerPeerIdDelDestinatario(UUID destinatarioId) throws Exception {
        List<UserResponseDto> todosUsuarios = chatFachada.usuarios().obtenerTodosLosUsuarios();

        for (UserResponseDto usuario : todosUsuarios) {
            if (usuario.getUserId().equals(destinatarioId)) {
                return usuario.getPeerId();
            }
        }

        throw new Exception("Usuario no encontrado: " + destinatarioId);
    }
}
