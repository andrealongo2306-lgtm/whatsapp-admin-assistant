package com.yourcompany.assistant.service;

import com.yourcompany.assistant.enums.ConversationState;
import com.yourcompany.assistant.model.Conversation;
import com.yourcompany.assistant.repository.ConversationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    private static final String PHONE_NUMBER = "+39123456789";

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private TwilioService twilioService;

    @InjectMocks
    private ConversationService conversationService;

    @Nested
    @DisplayName("Test processMessage - nuovo utente")
    class NewUserTests {

        @Test
        @DisplayName("Dovrebbe inviare il messaggio di benvenuto per un nuovo utente")
        void processMessage_shouldSendWelcomeMessage_forNewUser() {
            when(conversationRepository.findByPhoneNumber(PHONE_NUMBER))
                    .thenReturn(Optional.empty());

            conversationService.processMessage(PHONE_NUMBER, "ciao");

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            String message = messageCaptor.getValue();
            assertEquals(ConversationService.WELCOME_MESSAGE, message);
            // [REVIEWAGENT] rimosso assertTrue ridondante: se assertEquals passa, contains è garantito;
            // la proprietà "contiene Mese-Anno" è già coperta da WelcomeMessageTests
        }

        @Test
        @DisplayName("Dovrebbe impostare lo stato WAITING_MONTH_YEAR dopo il benvenuto")
        void processMessage_shouldSetWaitingMonthYearState_afterWelcome() {
            when(conversationRepository.findByPhoneNumber(PHONE_NUMBER))
                    .thenReturn(Optional.empty());

            conversationService.processMessage(PHONE_NUMBER, "ciao");

            ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
            verify(conversationRepository).save(conversationCaptor.capture());
            assertEquals(ConversationState.WAITING_MONTH_YEAR, conversationCaptor.getValue().getCurrentState());
        }
    }

    @Nested
    @DisplayName("Test WELCOME_MESSAGE costante")
    class WelcomeMessageTests {

        @Test
        @DisplayName("WELCOME_MESSAGE non è null o vuoto")
        void welcomeMessage_shouldNotBeNullOrEmpty() {
            assertNotNull(ConversationService.WELCOME_MESSAGE);
            assertFalse(ConversationService.WELCOME_MESSAGE.isBlank());
        }

        @Test
        @DisplayName("WELCOME_MESSAGE contiene istruzioni sul formato Mese-Anno")
        void welcomeMessage_shouldContainFormatInstructions() {
            assertTrue(ConversationService.WELCOME_MESSAGE.contains("Mese-Anno"));
        }
    }
}
