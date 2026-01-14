package com.yourcompany.assistant.model;

import com.yourcompany.assistant.enums.ConversationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test per il modello Conversation")
class ConversationTest {

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        conversation = Conversation.builder()
                .phoneNumber("whatsapp:+39123456789")
                .currentState(ConversationState.INITIAL)
                .currentCommessaIndex(0)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("addCommessa aggiunge correttamente una commessa")
    void addCommessa_shouldAddCommessa() {
        Commessa commessa = Commessa.builder()
                .name("Progetto Test")
                .giornate(5)
                .tariffa(new BigDecimal("250.00"))
                .build();

        conversation.addCommessa(commessa);

        assertEquals(1, conversation.getCommesse().size());
        assertEquals("Progetto Test", conversation.getCommesse().get(0).getName());
    }

    @Test
    @DisplayName("addCommessa inizializza la lista se null")
    void addCommessa_shouldInitializeListIfNull() {
        conversation.setCommesse(null);

        Commessa commessa = Commessa.builder()
                .name("Progetto Test")
                .giornate(5)
                .tariffa(new BigDecimal("250.00"))
                .build();

        conversation.addCommessa(commessa);

        assertNotNull(conversation.getCommesse());
        assertEquals(1, conversation.getCommesse().size());
    }

    @Test
    @DisplayName("hasMoreCommesseToProcess restituisce true quando ci sono altre commesse")
    void hasMoreCommesseToProcess_shouldReturnTrue_whenMoreCommessesExist() {
        conversation.setCommesseAttiveIds(Arrays.asList("id1", "id2", "id3"));

        Commessa commessa = Commessa.builder().name("Test").build();
        conversation.addCommessa(commessa);

        assertTrue(conversation.hasMoreCommesseToProcess());
    }

    @Test
    @DisplayName("hasMoreCommesseToProcess restituisce false quando tutte le commesse sono processate")
    void hasMoreCommesseToProcess_shouldReturnFalse_whenAllProcessed() {
        conversation.setCommesseAttiveIds(Arrays.asList("id1", "id2"));

        conversation.addCommessa(Commessa.builder().name("Test1").build());
        conversation.addCommessa(Commessa.builder().name("Test2").build());

        assertFalse(conversation.hasMoreCommesseToProcess());
    }

    @Test
    @DisplayName("hasMoreCommesseToProcess restituisce false quando le liste sono null")
    void hasMoreCommesseToProcess_shouldReturnFalse_whenListsAreNull() {
        conversation.setCommesseAttiveIds(null);
        conversation.setCommesse(null);

        assertFalse(conversation.hasMoreCommesseToProcess());
    }

    @Test
    @DisplayName("reset ripristina tutti i campi")
    void reset_shouldResetAllFields() {
        conversation.setMonth("Gennaio");
        conversation.setYear("2024");
        conversation.setCurrentCommessaIndex(3);
        conversation.setCommesseAttiveIds(Arrays.asList("id1", "id2"));
        conversation.addCommessa(Commessa.builder().name("Test").build());
        conversation.setCurrentState(ConversationState.COMPLETED);

        conversation.reset();

        assertEquals(ConversationState.INITIAL, conversation.getCurrentState());
        assertNull(conversation.getMonth());
        assertNull(conversation.getYear());
        assertEquals(0, conversation.getCurrentCommessaIndex());
        assertTrue(conversation.getCommesseAttiveIds().isEmpty());
        assertTrue(conversation.getCommesse().isEmpty());
    }

    @Test
    @DisplayName("Builder con valori di default")
    void builder_shouldHaveDefaultValues() {
        Conversation newConv = Conversation.builder()
                .phoneNumber("test")
                .build();

        assertNotNull(newConv.getCommesseAttiveIds());
        assertNotNull(newConv.getCommesse());
        assertTrue(newConv.getCommesseAttiveIds().isEmpty());
        assertTrue(newConv.getCommesse().isEmpty());
    }

    @Test
    @DisplayName("Il numero di telefono è l'ID")
    void phoneNumber_shouldBeId() {
        assertEquals("whatsapp:+39123456789", conversation.getPhoneNumber());
    }
}
