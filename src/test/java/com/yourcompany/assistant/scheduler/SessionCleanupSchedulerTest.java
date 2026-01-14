package com.yourcompany.assistant.scheduler;

import com.yourcompany.assistant.enums.ConversationState;
import com.yourcompany.assistant.model.Conversation;
import com.yourcompany.assistant.repository.ConversationRepository;
import com.yourcompany.assistant.service.TwilioService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test per SessionCleanupScheduler")
class SessionCleanupSchedulerTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private TwilioService twilioService;

    @InjectMocks
    private SessionCleanupScheduler scheduler;

    @Captor
    private ArgumentCaptor<Conversation> conversationCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "sessionTimeoutMinutes", 30);
        ReflectionTestUtils.setField(scheduler, "adminPhoneNumber", "whatsapp:+39123456789");
    }

    @Nested
    @DisplayName("Test resetOnStartup")
    class ResetOnStartupTests {

        @Test
        @DisplayName("resetOnStartup elimina tutte le conversazioni")
        void resetOnStartup_shouldDeleteAllConversations() {
            scheduler.resetOnStartup();

            verify(conversationRepository).deleteAll();
        }
    }

    @Nested
    @DisplayName("Test cleanupExpiredSessions")
    class CleanupExpiredSessionsTests {

        @Test
        @DisplayName("Pulizia elimina sessioni scadute")
        void cleanupExpiredSessions_shouldDeleteExpiredSessions() {
            List<Conversation> expiredConversations = Arrays.asList(
                    createConversation("phone1", LocalDateTime.now().minusHours(1)),
                    createConversation("phone2", LocalDateTime.now().minusHours(2))
            );

            when(conversationRepository.findByLastUpdatedBefore(any(LocalDateTime.class)))
                    .thenReturn(expiredConversations);

            scheduler.cleanupExpiredSessions();

            verify(conversationRepository).deleteAll(expiredConversations);
        }

        @Test
        @DisplayName("Pulizia non fa nulla se non ci sono sessioni scadute")
        void cleanupExpiredSessions_shouldDoNothing_whenNoExpiredSessions() {
            when(conversationRepository.findByLastUpdatedBefore(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            scheduler.cleanupExpiredSessions();

            verify(conversationRepository, never()).deleteAll(anyList());
        }

        @Test
        @DisplayName("Pulizia gestisce eccezioni senza propagarle")
        void cleanupExpiredSessions_shouldHandleExceptions() {
            when(conversationRepository.findByLastUpdatedBefore(any(LocalDateTime.class)))
                    .thenThrow(new RuntimeException("DB Error"));

            assertDoesNotThrow(() -> scheduler.cleanupExpiredSessions());
        }
    }

    @Nested
    @DisplayName("Test dailyStatistics")
    class DailyStatisticsTests {

        @Test
        @DisplayName("Statistiche giornaliere contano le sessioni")
        void dailyStatistics_shouldCountSessions() {
            when(conversationRepository.count()).thenReturn(5L);

            assertDoesNotThrow(() -> scheduler.dailyStatistics());

            verify(conversationRepository).count();
        }

        @Test
        @DisplayName("Statistiche gestiscono errori senza propagarli")
        void dailyStatistics_shouldHandleErrors() {
            when(conversationRepository.count()).thenThrow(new RuntimeException("DB Error"));

            assertDoesNotThrow(() -> scheduler.dailyStatistics());
        }
    }

    @Nested
    @DisplayName("Test monthlyBillingReminder")
    class MonthlyBillingReminderTests {

        @Test
        @DisplayName("Promemoria inviato solo l'ultimo giorno del mese")
        void monthlyBillingReminder_shouldOnlySend_onLastDayOfMonth() {
            LocalDate today = LocalDate.now();

            if (today.getDayOfMonth() != today.lengthOfMonth()) {
                scheduler.monthlyBillingReminder();
                verify(twilioService, never()).sendWhatsAppMessage(anyString(), anyString());
            }
        }

        @Test
        @DisplayName("Promemoria crea/resetta conversazione admin")
        void monthlyBillingReminder_shouldCreateOrResetAdminConversation() {
            LocalDate today = LocalDate.now();

            // Skip test se non è l'ultimo giorno del mese
            if (today.getDayOfMonth() != today.lengthOfMonth()) {
                return;
            }

            when(conversationRepository.findById("whatsapp:+39123456789"))
                    .thenReturn(Optional.empty());
            when(conversationRepository.save(any(Conversation.class)))
                    .thenAnswer(i -> i.getArgument(0));

            scheduler.monthlyBillingReminder();

            verify(conversationRepository).save(conversationCaptor.capture());
            Conversation saved = conversationCaptor.getValue();
            assertEquals(ConversationState.WAITING_MONTH_YEAR, saved.getCurrentState());
        }

        @Test
        @DisplayName("Promemoria gestisce eccezioni senza propagarle")
        void monthlyBillingReminder_shouldHandleExceptions() {
            LocalDate today = LocalDate.now();

            if (today.getDayOfMonth() == today.lengthOfMonth()) {
                when(conversationRepository.findById(anyString()))
                        .thenThrow(new RuntimeException("DB Error"));
            }

            assertDoesNotThrow(() -> scheduler.monthlyBillingReminder());
        }
    }

    private Conversation createConversation(String phoneNumber, LocalDateTime lastUpdated) {
        return Conversation.builder()
                .phoneNumber(phoneNumber)
                .currentState(ConversationState.INITIAL)
                .lastUpdated(lastUpdated)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
