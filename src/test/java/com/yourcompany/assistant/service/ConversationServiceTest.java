package com.yourcompany.assistant.service;

import com.yourcompany.assistant.enums.ConversationState;
import com.yourcompany.assistant.model.Commessa;
import com.yourcompany.assistant.model.CommessaAnagrafica;
import com.yourcompany.assistant.model.Conversation;
import com.yourcompany.assistant.repository.CommessaAnagraficaRepository;
import com.yourcompany.assistant.repository.ConversationRepository;
import com.yourcompany.assistant.repository.RichiestaFatturazioneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test per ConversationService")
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private CommessaAnagraficaRepository commessaAnagraficaRepository;

    @Mock
    private RichiestaFatturazioneRepository richiestaFatturazioneRepository;

    @Mock
    private TwilioService twilioService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ConversationService conversationService;

    @Captor
    private ArgumentCaptor<Conversation> conversationCaptor;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private static final String PHONE_NUMBER = "whatsapp:+39123456789";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(conversationService, "clientEmail", "client@example.com");
    }

    @Nested
    @DisplayName("Test stato INITIAL")
    class InitialStateTests {

        @Test
        @DisplayName("Nuovo utente riceve messaggio di benvenuto")
        void processMessage_shouldSendWelcomeMessage_forNewUser() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.empty());
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "start");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            String message = messageCaptor.getValue();
            assertTrue(message.contains("Mese e anno"));
        }

        @Test
        @DisplayName("Lo stato passa a WAITING_MONTH_YEAR dopo messaggio iniziale")
        void processMessage_shouldTransitionToWaitingMonthYear() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.empty());
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "start");

            verify(conversationRepository, times(2)).save(conversationCaptor.capture());
            Conversation saved = conversationCaptor.getValue();
            assertEquals(ConversationState.WAITING_MONTH_YEAR, saved.getCurrentState());
        }
    }

    @Nested
    @DisplayName("Test stato WAITING_MONTH_YEAR")
    class WaitingMonthYearTests {

        private Conversation existingConversation;

        @BeforeEach
        void setUp() {
            existingConversation = Conversation.builder()
                    .phoneNumber(PHONE_NUMBER)
                    .currentState(ConversationState.WAITING_MONTH_YEAR)
                    .currentCommessaIndex(0)
                    .createdAt(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("Input mese-anno valido passa a WAITING_GIORNATE")
        void processMessage_shouldTransitionToWaitingGiornate_withValidInput() {
            List<CommessaAnagrafica> commesse = Arrays.asList(
                    CommessaAnagrafica.builder().id("1").name("Progetto A").tariffa(new BigDecimal("250")).active(true).build()
            );

            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(existingConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(commessaAnagraficaRepository.findByActiveTrue()).thenReturn(commesse);

            conversationService.processMessage(PHONE_NUMBER, "Gennaio-2024");

            verify(conversationRepository).save(conversationCaptor.capture());
            Conversation saved = conversationCaptor.getValue();
            assertEquals(ConversationState.WAITING_GIORNATE, saved.getCurrentState());
            assertEquals("Gennaio", saved.getMonth());
            assertEquals("2024", saved.getYear());
        }

        @Test
        @DisplayName("Formato non valido restituisce errore")
        void processMessage_shouldReturnError_withInvalidFormat() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(existingConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "Gennaio 2024");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Formato non valido"));
        }

        @Test
        @DisplayName("Mese non valido restituisce errore")
        void processMessage_shouldReturnError_withInvalidMonth() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(existingConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "Gennario-2024");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Mese non valido"));
        }

        @Test
        @DisplayName("Anno fuori range restituisce errore")
        void processMessage_shouldReturnError_withYearOutOfRange() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(existingConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "Gennaio-2055");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Anno non valido"));
        }

        @Test
        @DisplayName("Nessuna commessa attiva resetta conversazione")
        void processMessage_shouldReset_whenNoActiveCommesse() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(existingConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(commessaAnagraficaRepository.findByActiveTrue()).thenReturn(new ArrayList<>());

            conversationService.processMessage(PHONE_NUMBER, "Gennaio-2024");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Nessuna commessa attiva"));
        }

        @Test
        @DisplayName("Input mese case-insensitive funziona")
        void processMessage_shouldAcceptCaseInsensitiveMonth() {
            List<CommessaAnagrafica> commesse = Arrays.asList(
                    CommessaAnagrafica.builder().id("1").name("Progetto A").tariffa(new BigDecimal("250")).active(true).build()
            );

            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(existingConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(commessaAnagraficaRepository.findByActiveTrue()).thenReturn(commesse);

            conversationService.processMessage(PHONE_NUMBER, "GENNAIO-2024");

            verify(conversationRepository).save(conversationCaptor.capture());
            assertEquals("Gennaio", conversationCaptor.getValue().getMonth());
        }
    }

    @Nested
    @DisplayName("Test stato WAITING_GIORNATE")
    class WaitingGiornateTests {

        private Conversation conversationWithCommesse;
        private CommessaAnagrafica commessaA;
        private CommessaAnagrafica commessaB;

        @BeforeEach
        void setUp() {
            commessaA = CommessaAnagrafica.builder()
                    .id("id1")
                    .name("Progetto A")
                    .tariffa(new BigDecimal("250"))
                    .active(true)
                    .build();

            commessaB = CommessaAnagrafica.builder()
                    .id("id2")
                    .name("Progetto B")
                    .tariffa(new BigDecimal("300"))
                    .active(true)
                    .build();

            conversationWithCommesse = Conversation.builder()
                    .phoneNumber(PHONE_NUMBER)
                    .currentState(ConversationState.WAITING_GIORNATE)
                    .month("Gennaio")
                    .year("2024")
                    .commesseAttiveIds(Arrays.asList("id1", "id2"))
                    .currentCommessaIndex(0)
                    .createdAt(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("Input giornate valido passa alla commessa successiva")
        void processMessage_shouldMoveToNextCommessa_withValidGiornate() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationWithCommesse));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(commessaAnagraficaRepository.findById("id1")).thenReturn(Optional.of(commessaA));
            when(commessaAnagraficaRepository.findById("id2")).thenReturn(Optional.of(commessaB));

            conversationService.processMessage(PHONE_NUMBER, "5");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Progetto B"));
        }

        @Test
        @DisplayName("Zero giornate non aggiunge la commessa alla lista")
        void processMessage_shouldNotAddCommessa_withZeroGiornate() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationWithCommesse));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(commessaAnagraficaRepository.findById("id1")).thenReturn(Optional.of(commessaA));
            when(commessaAnagraficaRepository.findById("id2")).thenReturn(Optional.of(commessaB));

            conversationService.processMessage(PHONE_NUMBER, "0");

            verify(conversationRepository).save(conversationCaptor.capture());
            assertTrue(conversationCaptor.getValue().getCommesse().isEmpty());
        }

        @Test
        @DisplayName("Giornate negative restituisce errore")
        void processMessage_shouldReturnError_withNegativeGiornate() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationWithCommesse));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "-5");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Numero non valido"));
        }

        @Test
        @DisplayName("Giornate > 31 restituisce errore")
        void processMessage_shouldReturnError_withGiornateOver31() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationWithCommesse));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "32");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Numero non valido"));
        }

        @Test
        @DisplayName("Input non numerico restituisce errore")
        void processMessage_shouldReturnError_withNonNumericInput() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationWithCommesse));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "cinque");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("numero valido"));
        }

        @Test
        @DisplayName("Ultima commessa passa a REVIEW_EMAIL")
        void processMessage_shouldTransitionToReviewEmail_afterLastCommessa() {
            conversationWithCommesse.setCurrentCommessaIndex(1);
            conversationWithCommesse.addCommessa(Commessa.builder()
                    .name("Progetto A")
                    .giornate(new BigDecimal("5"))
                    .tariffa(new BigDecimal("250"))
                    .build());

            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationWithCommesse));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(commessaAnagraficaRepository.findById("id2")).thenReturn(Optional.of(commessaB));

            conversationService.processMessage(PHONE_NUMBER, "3");

            verify(conversationRepository).save(conversationCaptor.capture());
            assertEquals(ConversationState.REVIEW_EMAIL, conversationCaptor.getValue().getCurrentState());
        }

        @Test
        @DisplayName("Nessuna giornata inserita annulla la conversazione")
        void processMessage_shouldCancel_whenNoGiornateEntered() {
            conversationWithCommesse.setCommesseAttiveIds(Arrays.asList("id1"));
            conversationWithCommesse.setCurrentCommessaIndex(0);

            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationWithCommesse));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(commessaAnagraficaRepository.findById("id1")).thenReturn(Optional.of(commessaA));

            conversationService.processMessage(PHONE_NUMBER, "0");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Nessuna giornata"));
        }
    }

    @Nested
    @DisplayName("Test stato REVIEW_EMAIL")
    class ReviewEmailTests {

        private Conversation conversationInReview;

        @BeforeEach
        void setUp() {
            conversationInReview = Conversation.builder()
                    .phoneNumber(PHONE_NUMBER)
                    .currentState(ConversationState.REVIEW_EMAIL)
                    .month("Gennaio")
                    .year("2024")
                    .currentCommessaIndex(0)
                    .createdAt(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .build();

            conversationInReview.addCommessa(Commessa.builder()
                    .name("Progetto A")
                    .giornate(new BigDecimal("5"))
                    .tariffa(new BigDecimal("250"))
                    .build());
        }

        @Test
        @DisplayName("Conferma (1) invia l'email")
        void processMessage_shouldSendEmail_whenConfirmed() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationInReview));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(emailService.sendEmail(any())).thenReturn("email-id");

            conversationService.processMessage(PHONE_NUMBER, "1");

            verify(emailService).sendEmail(any());
            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("inviata"));
        }

        @Test
        @DisplayName("Conferma (1) passa a COMPLETED")
        void processMessage_shouldTransitionToCompleted_whenConfirmed() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationInReview));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(emailService.sendEmail(any())).thenReturn("email-id");

            conversationService.processMessage(PHONE_NUMBER, "1");

            verify(conversationRepository).save(conversationCaptor.capture());
            assertEquals(ConversationState.COMPLETED, conversationCaptor.getValue().getCurrentState());
        }

        @Test
        @DisplayName("Annulla (2) resetta la conversazione")
        void processMessage_shouldReset_whenCancelled() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationInReview));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "2");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Annullato"));
        }

        @Test
        @DisplayName("Annulla (2) passa a CANCELLED")
        void processMessage_shouldTransitionToCancelled_whenCancelled() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationInReview));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "2");

            verify(conversationRepository).save(conversationCaptor.capture());
            assertEquals(ConversationState.CANCELLED, conversationCaptor.getValue().getCurrentState());
        }

        @Test
        @DisplayName("Input non valido mostra le opzioni")
        void processMessage_shouldShowOptions_withInvalidInput() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationInReview));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "3");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("1 = Invia"));
        }

        @Test
        @DisplayName("Errore invio email mostra messaggio di errore")
        void processMessage_shouldShowError_whenEmailFails() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(conversationInReview));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
            when(emailService.sendEmail(any())).thenThrow(new RuntimeException("Errore Gmail"));

            conversationService.processMessage(PHONE_NUMBER, "1");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("Errore"));
        }
    }

    @Nested
    @DisplayName("Test stato COMPLETED")
    class CompletedStateTests {

        private Conversation completedConversation;

        @BeforeEach
        void setUp() {
            completedConversation = Conversation.builder()
                    .phoneNumber(PHONE_NUMBER)
                    .currentState(ConversationState.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("'start' avvia nuova conversazione")
        void processMessage_shouldStartNewConversation_withStart() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(completedConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "start");

            verify(conversationRepository).save(conversationCaptor.capture());
            assertEquals(ConversationState.WAITING_MONTH_YEAR, conversationCaptor.getValue().getCurrentState());
        }

        @Test
        @DisplayName("'inizia' avvia nuova conversazione")
        void processMessage_shouldStartNewConversation_withInizia() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(completedConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "inizia");

            verify(conversationRepository).save(conversationCaptor.capture());
            assertEquals(ConversationState.WAITING_MONTH_YEAR, conversationCaptor.getValue().getCurrentState());
        }

        @Test
        @DisplayName("Altri messaggi mostrano istruzioni")
        void processMessage_shouldShowInstructions_withOtherMessage() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(completedConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "ciao");

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            assertTrue(messageCaptor.getValue().contains("start"));
        }
    }

    @Nested
    @DisplayName("Test stato CANCELLED")
    class CancelledStateTests {

        private Conversation cancelledConversation;

        @BeforeEach
        void setUp() {
            cancelledConversation = Conversation.builder()
                    .phoneNumber(PHONE_NUMBER)
                    .currentState(ConversationState.CANCELLED)
                    .createdAt(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("'start' avvia nuova conversazione")
        void processMessage_shouldStartNewConversation_withStart() {
            when(conversationRepository.findById(PHONE_NUMBER)).thenReturn(Optional.of(cancelledConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

            conversationService.processMessage(PHONE_NUMBER, "start");

            verify(conversationRepository).save(conversationCaptor.capture());
            assertEquals(ConversationState.WAITING_MONTH_YEAR, conversationCaptor.getValue().getCurrentState());
        }
    }
}
