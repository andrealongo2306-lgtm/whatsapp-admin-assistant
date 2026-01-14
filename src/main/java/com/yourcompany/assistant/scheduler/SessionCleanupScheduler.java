package com.yourcompany.assistant.scheduler;

import com.yourcompany.assistant.enums.ConversationState;
import com.yourcompany.assistant.model.Conversation;
import com.yourcompany.assistant.repository.ConversationRepository;
import com.yourcompany.assistant.service.TwilioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Scheduler per la pulizia automatica delle sessioni scadute
 * 
 * Esegue una pulizia periodica delle conversazioni non aggiornate
 * oltre il timeout configurato
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupScheduler {

    private final ConversationRepository conversationRepository;
    private final TwilioService twilioService;

    @Value("${app.session-timeout-minutes:30}")
    private int sessionTimeoutMinutes;

    @Value("${app.admin.phone-number}")
    private String adminPhoneNumber;

    /**
     * Reset di tutte le conversazioni all'avvio dell'applicazione
     */
    @PostConstruct
    public void resetOnStartup() {
        log.info("Avvio applicazione - reset di tutte le conversazioni");
        conversationRepository.deleteAll();
        log.info("Conversazioni resettate");
    }

    /**
     * Esegue la pulizia ogni ora
     * Cron: ogni ora al minuto 0
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredSessions() {
        log.info("Avvio pulizia sessioni scadute (timeout: {} minuti)", sessionTimeoutMinutes);
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
            
            var expiredConversations = conversationRepository.findByLastUpdatedBefore(cutoffTime);
            
            if (expiredConversations.isEmpty()) {
                log.info("Nessuna sessione scaduta trovata");
                return;
            }
            
            conversationRepository.deleteAll(expiredConversations);
            
            log.info("Pulite {} sessioni scadute", expiredConversations.size());
            
        } catch (Exception e) {
            log.error("Errore durante la pulizia delle sessioni: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Statistiche giornaliere (opzionale)
     * Cron: ogni giorno alle 9:00
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void dailyStatistics() {
        try {
            long totalSessions = conversationRepository.count();
            log.info("Statistiche giornaliere - Sessioni attive: {}", totalSessions);
        } catch (Exception e) {
            log.error("Errore nel calcolo delle statistiche: {}", e.getMessage(), e);
        }
    }

    /**
     * Promemoria fatturazione ultimo giorno del mese
     * Cron: ogni giorno alle 9:00, ma esegue solo se è l'ultimo giorno del mese
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void monthlyBillingReminder() {
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        if (!today.equals(lastDayOfMonth)) {
            return;
        }

        log.info("Ultimo giorno del mese - invio promemoria fatturazione");

        try {
            // Reset o crea conversazione per admin
            Conversation conversation = conversationRepository.findById(adminPhoneNumber)
                    .orElse(Conversation.builder()
                            .phoneNumber(adminPhoneNumber)
                            .createdAt(LocalDateTime.now())
                            .build());

            conversation.reset();
            conversation.setCurrentState(ConversationState.WAITING_MONTH_YEAR);
            conversation.setLastUpdated(LocalDateTime.now());
            conversation.setCommesseAttiveIds(new ArrayList<>());
            conversationRepository.save(conversation);

            // Invia messaggio
            String message = "Ciao! Sono il tuo assistente. E' ora di inviare l'autorizzazione alla fatturazione. Mese e anno? (es: Gennaio-2024)";
            twilioService.sendWhatsAppMessage(adminPhoneNumber, message);

            log.info("Promemoria fatturazione inviato a {}", adminPhoneNumber);
        } catch (Exception e) {
            log.error("Errore invio promemoria fatturazione: {}", e.getMessage(), e);
        }
    }
}
