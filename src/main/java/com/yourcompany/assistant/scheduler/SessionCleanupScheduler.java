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
     * Cron: ogni giorno alle 9:32 (TEST)
     */
    @Scheduled(cron = "0 53 9 * * *")
    public void monthlyBillingReminder() {
        // TODO: controllo ultimo giorno del mese dopo test
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        if (!today.equals(lastDayOfMonth)) {
            return;
        }

        log.info("Invio promemoria fatturazione (TEST - ogni giorno alle 9:32)");
        log.info("Admin phone number: {}", adminPhoneNumber);

        try {
            // Formatta il numero con prefisso whatsapp: per matchare il formato del webhook Twilio
            String formattedPhoneNumber = adminPhoneNumber.startsWith("whatsapp:")
                    ? adminPhoneNumber
                    : "whatsapp:" + adminPhoneNumber;

            // Reset o crea conversazione per admin
            log.info("Step 1: Recupero conversazione...");
            Conversation conversation = conversationRepository.findById(formattedPhoneNumber)
                    .orElse(Conversation.builder()
                            .phoneNumber(formattedPhoneNumber)
                            .createdAt(LocalDateTime.now())
                            .build());

            log.info("Step 2: Reset conversazione...");
            conversation.reset();
            conversation.setCurrentState(ConversationState.WAITING_MONTH_YEAR);
            conversation.setLastUpdated(LocalDateTime.now());
            conversation.setCommesseAttiveIds(new ArrayList<>());

            log.info("Step 3: Salvataggio conversazione...");
            conversationRepository.save(conversation);

            // Invia messaggio
            log.info("Step 4: Invio messaggio WhatsApp...");
            String message = "Ciao! Sono il tuo assistente. E' ora di inviare l'autorizzazione alla fatturazione. Mese e anno? (es: Gennaio-2024)";
            String sid = twilioService.sendWhatsAppMessage(adminPhoneNumber, message);

            log.info("Step 5: Messaggio inviato con SID: {}", sid);
            log.info("Promemoria fatturazione inviato a {}", adminPhoneNumber);
        } catch (Exception e) {
            log.error("Errore invio promemoria fatturazione: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
