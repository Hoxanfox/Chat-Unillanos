package gestorP2P;

import comunicacion.IGestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import conexion.enums.TipoPool;
import dto.comunicacion.DTOResponse;
import dto.p2p.DTOPeer;
import dto.p2p.DTOPeerListResponse;
import dto.p2p.DTOJoinResponse;
import gestorP2P.config.IConfigReader;
import gestorP2P.inicio.IStarterP2P;
import gestorP2P.registroP2P.IPeerRegistrar;
import dominio.p2p.Peer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GestorP2PImplTest {

    @Mock
    IEnviadorPeticiones enviador;

    @Mock
    IGestorRespuesta gestorRespuesta;

    @Mock
    IPeerRegistrar peerRegistrar;

    @Mock
    IConfigReader config;

    @Mock
    IStarterP2P starter;

    @Test
    public void testUnirseRed_success() throws Exception {
        when(config.getString(eq("peer.host"), anyString())).thenReturn("127.0.0.1");
        when(config.getInt(eq("peer.puerto"), anyInt())).thenReturn(9001);
        when(peerRegistrar.registrarPeer(any(Peer.class), anyString())).thenReturn(true);

        AtomicReference<String> keyRef = new AtomicReference<>();
        AtomicReference<Consumer<DTOResponse>> handlerRef = new AtomicReference<>();

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Consumer<DTOResponse> h = invocation.getArgument(1);
            keyRef.set(key);
            handlerRef.set(h);
            return null;
        }).when(gestorRespuesta).registrarManejador(anyString(), any());

        GestorP2PImpl gestor = new GestorP2PImpl(enviador, gestorRespuesta, peerRegistrar, config, starter);

        CompletableFuture<UUID> futuro = gestor.unirseRed("10.0.0.2", 9002);

        // comprobar que se registró un manejador y se envió la petición
        verify(gestorRespuesta, timeout(1000)).registrarManejador(anyString(), any());
        verify(enviador, timeout(1000)).enviar(any(), eq(TipoPool.PEERS));

        String clave = keyRef.get();
        assertNotNull(clave, "clave del manejador debe estar registrada");
        String[] parts = clave.split(":", 2);
        assertEquals("PEER_JOIN", parts[0]);
        String requestId = parts[1];

        UUID expectedId = UUID.randomUUID();
        Map<String,Object> data = new HashMap<>();
        data.put("uuid", expectedId.toString());
        data.put("requestId", requestId);

        DTOResponse response = new DTOResponse("PEER_JOIN", "success", "ok", data);

        // simular la llegada de la respuesta
        handlerRef.get().accept(response);

        UUID result = futuro.get(2, TimeUnit.SECONDS);
        assertEquals(expectedId, result);

        // verificar que el peer fue registrado
        verify(peerRegistrar, times(1)).registrarPeer(any(Peer.class), eq("10.0.0.2:9002"));
        verify(gestorRespuesta, times(1)).removerManejador(clave);
    }

    @Test
    public void testSolicitarListaPeers_success() throws Exception {
        when(config.getString(eq("peer.host"), anyString())).thenReturn("127.0.0.1");
        when(config.getInt(eq("peer.puerto"), anyInt())).thenReturn(9001);

        AtomicReference<String> keyRef = new AtomicReference<>();
        AtomicReference<Consumer<DTOResponse>> handlerRef = new AtomicReference<>();

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Consumer<DTOResponse> h = invocation.getArgument(1);
            keyRef.set(key);
            handlerRef.set(h);
            return null;
        }).when(gestorRespuesta).registrarManejador(anyString(), any());

        // preparar retorno simulado del registrador al recibir DTOPeer list
        UUID peerId = UUID.randomUUID();
        Peer p = new Peer(peerId, "10.0.0.2", null, Peer.Estado.ONLINE, Instant.now());
        List<Peer> processed = Collections.singletonList(p);
        when(peerRegistrar.registrarListaDesdeDTO(anyList())).thenReturn(processed);

        GestorP2PImpl gestor = new GestorP2PImpl(enviador, gestorRespuesta, peerRegistrar, config, starter);

        CompletableFuture<List<Peer>> futuro = gestor.solicitarListaPeers("10.0.0.2", 9002);

        verify(gestorRespuesta, timeout(1000)).registrarManejador(anyString(), any());
        verify(enviador, timeout(1000)).enviar(any(), eq(TipoPool.PEERS));

        String clave = keyRef.get();
        assertNotNull(clave);
        String[] parts = clave.split(":", 2);
        assertEquals("PEER_LIST", parts[0]);
        String requestId = parts[1];

        // construir DTOPeerListResponse con un DTOPeer dentro
        DTOPeer dto = new DTOPeer();
        dto.setId(peerId.toString());
        dto.setIp("10.0.0.2");
        dto.setSocketInfo("10.0.0.2:9002");
        dto.setEstado("ONLINE");

        DTOPeerListResponse listResp = new DTOPeerListResponse();
        listResp.setRequestId(requestId);
        listResp.setPeers(Collections.singletonList(dto));
        listResp.setCount(1);

        DTOResponse response = new DTOResponse("PEER_LIST", "success", "ok", listResp);

        handlerRef.get().accept(response);

        List<Peer> result = futuro.get(2, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(peerId, result.get(0).getId());

        verify(peerRegistrar, times(1)).registrarListaDesdeDTO(anyList());
        verify(gestorRespuesta, times(1)).removerManejador(clave);
    }
}

