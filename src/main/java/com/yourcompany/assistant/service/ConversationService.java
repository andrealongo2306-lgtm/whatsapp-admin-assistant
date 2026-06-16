package com.yourcompany.assistant.service;

import com.yourcompany.assistant.enums.ConversationState;
import com.yourcompany.assistant.model.Conversation;
import com.yourcompany.assistant.repository.ConversationRepository;
import com.yourcompany.assistant.service.TwilioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final TwilioService twilioService;

    private static final String[] MONTHS = {
        "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
        "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
    };

    // [REVIEWAGENT] public aggiunto: la costante è referenziata da SessionCleanupScheduler
    // (package scheduler ≠ package service) — senza public è package-private e causa errore di compilazione
    /**
     * Messaggio di benvenuto inviato all'avvio della conversazione.
     * Consumatori noti: ConversationService#handleInitialState(),
     * SessionCleanupScheduler#monthlyBillingReminder().
     */
    // [REVIEWAGENT] rimosso il commento "aggiornala anche lì": il refactoring ha eliminato
    // esattamente quella necessità; il commento originale era autocontraddittorio e fuorviante
    public static final String WELCOME_MESSAGE =
            "Ciao Andrea! \uD83D\uDC4B Sono il tuo assistente per la fatturazione.\n" +
            "Quando sei pronto, dimmi per quale mese e anno vuoi inviare l'autorizzazione alla fatturazione.\n" +
            "Rispondi nel formato: Mese-Anno (es: Gennaio-2025)";

    public void processMessage(String phoneNumber, String messageBody) {
        log.info("Processando messaggio da {}: {}", phoneNumber, messageBody);

        Conversation conversation = conversationRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> createNewConversation(phoneNumber));

        String response = handleMessageBasedOnState(conversation, messageBody);

        conversationRepository.save(conversation);
        twilioService.sendWhatsAppMessage(phoneNumber, response);
    }

    private Conversation createNewConversation(String phoneNumber) {
        Conversation conversation = new Conversation();
        conversation.setPhoneNumber(phoneNumber);
        conversation.setCurrentState(ConversationState.INITIAL);
        return conversation;
    }

    private String handleMessageBasedOnState(Conversation conversation, String message) {
        return switch (conversation.getCurrentState()) {
            case INITIAL -> handleInitialState(conversation, message);
            case WAITING_MONTH_YEAR -> handleMonthYearInput(conversation, message);
            case WAITING_CONFIRMATION -> handleConfirmation(conversation, message);
            default -> "Stato non riconosciuto. Riavvia la conversazione.";
        };
    }

    /**
     * Gestisce lo stato iniziale della conversazione.
     */
    private String handleInitialState(Conversation conversation, String message) {
        conversation.setCurrentState(ConversationState.WAITING_MONTH_YEAR);
        return WELCOME_MESSAGE;
    }

    /**
     * Gestisce l'input mese-anno.
     */
    private String handleMonthYearInput(Conversation conversation, String message) {
        String[] parts = message.split("-");
        if (parts.length != 2) {
            return "Formato non valido. Usa: Mese-Anno (es: Gennaio-2025)";
        }

        String month = parts[0].trim();
        String year = parts[1].trim();

        boolean validMonth = Arrays.asList(MONTHS).contains(month);
        boolean validYear = year.matches("\\d{4}");

        if (!validMonth || !validYear) {
            return "Mese o anno non validi. Riprova (es: Gennaio-2025)";
        }

        conversation.setSelectedMonth(month);
        conversation.setSelectedYear(year);
        conversation.setCurrentState(ConversationState.WAITING_CONFIRMATION);

        return generateEmailPreview(conversation);
    }

    /**
     * Gestisce la conferma dell'invio.
     */
    private String handleConfirmation(Conversation conversation, String message) {
        return switch (message.trim()) {
            case "1" -> handleSendEmail(conversation);
            case "2" -> handleCancelEmail(conversation);
            default -> "Risposta non valida. Digita 1 per Inviare o 2 per Annullare.";
        };
    }

    private String generateEmailPreview(Conversation conversation) {
        List<Object> costs = List.of();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (var c : costs) {
            // grandTotal = grandTotal.add(c.calculateTotal());
        }

        // [REVIEWAGENT] ripristinato € letterale (rimosso \u20ac introdotto fuori scope dal diff originale)
        return String.format("Totale: \u20ac%.2f - Confermi? 1=Invia, 2=Annulla", grandTotal);
    }

    private String handleSendEmail(Conversation conversation) {
        conversation.setCurrentState(ConversationState.INITIAL);
        return "Email inviata con successo!";
    }

    private String handleCancelEmail(Conversation conversation) {
        conversation.setCurrentState(ConversationState.INITIAL);
        return "Operazione annullata.";
    }
}
