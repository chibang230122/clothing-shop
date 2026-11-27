package com.clothingshop.chat;

import com.clothingshop.chat.entity.ChatConversation;
import com.clothingshop.chat.entity.ChatParticipant;
import com.clothingshop.chat.repository.ChatConversationRepository;
import com.clothingshop.chat.repository.ChatMessageRepository;
import com.clothingshop.chat.repository.ChatParticipantRepository;
import com.clothingshop.chat.service.BannedWordFilterService;
import com.clothingshop.chat.service.ChatService;
import com.clothingshop.chat.service.RealtimeCatalogService;
import com.clothingshop.socket.v2.ChatClientSession;
import com.clothingshop.socket.v2.RealtimeSessionRegistry;
import com.clothingshop.socket.v2.V2CommandDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatConversationLifecycleTest {

    private ChatConversationRepository conversationRepository;
    private ChatParticipantRepository participantRepository;
    private RealtimeSessionRegistry sessionRegistry;
    private V2CommandDispatcher dispatcher;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ChatConversationRepository.class);
        participantRepository = mock(ChatParticipantRepository.class);
        ChatMessageRepository messageRepository = mock(ChatMessageRepository.class);
        BannedWordFilterService filterService = mock(BannedWordFilterService.class);
        RealtimeCatalogService catalogService = mock(RealtimeCatalogService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        sessionRegistry = mock(RealtimeSessionRegistry.class);
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        ChatService chatService = new ChatService(
            conversationRepository, participantRepository, messageRepository, filterService
        );
        ReflectionTestUtils.setField(chatService, "entityManager", em);

        dispatcher = new V2CommandDispatcher(
            mapper, passwordEncoder, chatService, filterService, catalogService, sessionRegistry
        );
    }

    private ChatClientSession mockSession(int userId, String username, String role) {
        ChatClientSession session = mock(ChatClientSession.class);
        when(session.isAuthenticated()).thenReturn(true);
        when(session.getUserId()).thenReturn(userId);
        when(session.getUsername()).thenReturn(username);
        when(session.getRole()).thenReturn(role);
        return session;
    }

    @Test
    void customerCannotCloseOrDeleteConversation() throws Exception {
        ChatClientSession customer = mockSession(10, "cust@local", "CUSTOMER");

        JsonNode closeRes = mapper.readTree(dispatcher.dispatch(
            "{\"v\":2,\"type\":\"CLOSE_CONVERSATION\",\"requestId\":\"r1\",\"payload\":{\"conversationId\":5}}",
            customer
        ));
        assertFalse(closeRes.get("ok").asBoolean());
        assertEquals("FORBIDDEN", closeRes.get("errorCode").asText());

        JsonNode delRes = mapper.readTree(dispatcher.dispatch(
            "{\"v\":2,\"type\":\"DELETE_CONVERSATION\",\"requestId\":\"r2\",\"payload\":{\"conversationId\":5}}",
            customer
        ));
        assertFalse(delRes.get("ok").asBoolean());
        assertEquals("FORBIDDEN", delRes.get("errorCode").asText());
    }

    @Test
    void staffCanCloseConversationButCannotDelete() throws Exception {
        ChatClientSession staff = mockSession(2, "staff@clothingshop.local", "STAFF");
        ChatConversation conv = new ChatConversation();
        conv.setId(5L);
        conv.setCustomerId(10);
        conv.setStatus("OPEN");
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JsonNode closeRes = mapper.readTree(dispatcher.dispatch(
            "{\"v\":2,\"type\":\"CLOSE_CONVERSATION\",\"requestId\":\"r3\",\"payload\":{\"conversationId\":5}}",
            staff
        ));
        assertTrue(closeRes.get("ok").asBoolean());
        assertEquals("CLOSED", closeRes.get("payload").get("status").asText());
        verify(sessionRegistry).broadcastEvent(eq("CONVERSATION_CLOSED"), any());

        JsonNode delRes = mapper.readTree(dispatcher.dispatch(
            "{\"v\":2,\"type\":\"DELETE_CONVERSATION\",\"requestId\":\"r4\",\"payload\":{\"conversationId\":5}}",
            staff
        ));
        assertFalse(delRes.get("ok").asBoolean());
        assertEquals("FORBIDDEN", delRes.get("errorCode").asText());
    }

    @Test
    void adminCanDeleteInternalOrCustomerConversation() throws Exception {
        ChatClientSession admin = mockSession(1, "admin@clothingshop.local", "ADMIN");
        ChatConversation internal = new ChatConversation();
        internal.setId(9L);
        internal.setCustomerId(1);
        internal.setStatus("INTERNAL");
        when(conversationRepository.findById(9L)).thenReturn(Optional.of(internal));

        JsonNode delRes = mapper.readTree(dispatcher.dispatch(
            "{\"v\":2,\"type\":\"DELETE_CONVERSATION\",\"requestId\":\"r5\",\"payload\":{\"conversationId\":9}}",
            admin
        ));
        assertTrue(delRes.get("ok").asBoolean());
        assertEquals("CONVERSATION_DELETED", delRes.get("type").asText());
        verify(conversationRepository).delete(internal);
    }

        @Test
    void internalRoomVisibleAndAccessibleOnlyToParticipants() throws Exception {
        ChatClientSession admin = mockSession(1, "admin@clothingshop.local", "ADMIN");
        ChatClientSession nonMemberStaff = mockSession(2, "staff@clothingshop.local", "STAFF");

        ChatConversation warehouseRoom = new ChatConversation();
        warehouseRoom.setId(20L);
        warehouseRoom.setCustomerId(1);
        warehouseRoom.setTitle("Nh\u00f3m Kho");
        warehouseRoom.setStatus("INTERNAL");

        ChatParticipant adminPart = new ChatParticipant();
        adminPart.setConversationId(20L);
        adminPart.setUserId(1);
        adminPart.setRoleInChat("ADMIN");

        when(conversationRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(warehouseRoom));
        when(conversationRepository.findById(20L)).thenReturn(Optional.of(warehouseRoom));
        when(participantRepository.findByConversationIdAndUserId(20L, 1)).thenReturn(Optional.of(adminPart));
        when(participantRepository.findByConversationIdAndUserId(20L, 2)).thenReturn(Optional.empty());

        JsonNode adminList = mapper.readTree(dispatcher.dispatch(
            "{\"v\":2,\"type\":\"LIST_CONVERSATIONS\",\"requestId\":\"r6\",\"payload\":{}}",
            admin
        ));
        assertNotNull(adminList.get("payload"), "Response JSON: " + adminList.toPrettyString());
        assertEquals(1, adminList.get("payload").get("conversations").size());

        JsonNode staffList = mapper.readTree(dispatcher.dispatch(
            "{\"v\":2,\"type\":\"LIST_CONVERSATIONS\",\"requestId\":\"r7\",\"payload\":{}}",
            nonMemberStaff
        ));
        assertNotNull(staffList.get("payload"), "Staff Response JSON: " + staffList.toPrettyString());
        assertEquals(0, staffList.get("payload").get("conversations").size());

        JsonNode staffRead = mapper.readTree(dispatcher.dispatch(
            "{\"v\":2,\"type\":\"GET_MESSAGES\",\"requestId\":\"r8\",\"payload\":{\"conversationId\":20}}",
            nonMemberStaff
        ));
        assertFalse(staffRead.get("ok").asBoolean());
        assertEquals("FORBIDDEN", staffRead.get("errorCode").asText());
    }
}