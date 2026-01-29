package com.yourcompany.assistant.service;

import com.yourcompany.assistant.enums.ConversationState;
import com.yourcompany.assistant.model.Commessa;
import com.yourcompany.assistant.model.CommessaAnagrafica;
import com.yourcompany.assistant.model.Conversation;
import com.yourcompany.assistant.model.EmailDraft;
import com.yourcompany.assistant.model.RichiestaFatturazione;
import com.yourcompany.assistant.repository.CommessaAnagraficaRepository;
import com.yourcompany.assistant.repository.ConversationRepository;
import com.yourcompany.assistant.repository.RichiestaFatturazioneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final CommessaAnagraficaRepository commessaAnagraficaRepository;
    private final RichiestaFatturazioneRepository richiestaFatturazioneRepository;
    private final TwilioService twilioService;
    private final EmailService emailService;

    @Value("${app.client.email}")
    private String clientEmail;

    private static final String[] MONTHS = {
        "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
        "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
    };

    public void processMessage(String phoneNumber, String messageBody) {
        log.info("Processando messaggio da {}: {}", phoneNumber, messageBody);

        Conversation conversation = getOrCreateConversation(phoneNumber);
        String response = handleMessageBasedOnState(conversation, messageBody.trim());

        conversation.setLastUpdated(LocalDateTime.now());
        conversationRepository.save(conversation);

        twilioService.sendWhatsAppMessage(phoneNumber, response);
    }

    private Conversation getOrCreateConversation(String phoneNumber) {
        Optional<Conversation> existing = conversationRepository.findById(phoneNumber);

        if (existing.isPresent()) {
            return existing.get();
        }

        Conversation newConversation = Conversation.builder()
                .phoneNumber(phoneNumber)
                .currentState(ConversationState.INITIAL)
                .currentCommessaIndex(0)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();

        return conversationRepository.save(newConversation);
    }

    private String handleMessageBasedOnState(Conversation conversation, String message) {
        return switch (conversation.getCurrentState()) {
            case INITIAL -> handleInitialState(conversation, message);
            case WAITING_MONTH_YEAR -> handleMonthYearInput(conversation, message);
            case WAITING_GIORNATE -> handleGiornateInput(conversation, message);
            case REVIEW_EMAIL -> handleEmailReview(conversation, message);
            case COMPLETED -> handleCompleted(conversation, message);
            case CANCELLED -> handleCancelled(conversation, message);
        };
    }

    /**
     * INITIAL: Avvia la conversazione
     */
    private String handleInitialState(Conversation conversation, String message) {
        conversation.setCurrentState(ConversationState.WAITING_MONTH_YEAR);
        return "Ciao! Sono il tuo assistente. E' ora di inviare l'autorizzazione alla fatturazione. Mese e anno? (es: Gennaio-2024)";
    }

    /**
     * WAITING_MONTH_YEAR: Gestisce input mese-anno insieme
     */
    private String handleMonthYearInput(Conversation conversation, String message) {
        // Formato atteso: "Mese-Anno" (es: "Gennaio-2026")
        String[] parts = message.split("-");

        if (parts.length != 2) {
            return "Formato non valido. Usa: Mese-Anno (es: Gennaio-2026)";
        }

        String month = capitalizeFirstLetter(parts[0].trim().toLowerCase());
        String yearStr = parts[1].trim();

        if (!isValidMonth(month)) {
            return "Mese non valido. Usa: Mese-Anno (es: Gennaio-2026)";
        }

        try {
            int year = Integer.parseInt(yearStr);
            if (year < 2020 || year > 2050) {
                return "Anno non valido (2020-2050). Usa: Mese-Anno";
            }

            conversation.setMonth(month);
            conversation.setYear(String.valueOf(year));

            // Carica commesse attive dal DB
            List<CommessaAnagrafica> commesseAttive = commessaAnagraficaRepository.findByActiveTrue();

            if (commesseAttive.isEmpty()) {
                conversation.reset();
                return "Nessuna commessa attiva nel sistema.";
            }

            // Salva gli ID delle commesse da processare
            List<String> ids = commesseAttive.stream()
                    .map(CommessaAnagrafica::getId)
                    .collect(Collectors.toList());
            conversation.setCommesseAttiveIds(ids);
            conversation.setCurrentCommessaIndex(0);
            conversation.setCurrentState(ConversationState.WAITING_GIORNATE);

            // Chiedi giornate per la prima commessa
            CommessaAnagrafica prima = commesseAttive.get(0);
            return String.format("Giornate per %s?", prima.getName());

        } catch (NumberFormatException e) {
            return "Anno non valido. Usa: Mese-Anno (es: Gennaio-2024)";
        }
    }

    /**
     * WAITING_GIORNATE: Gestisce le giornate per ogni commessa attiva
     */
    private String handleGiornateInput(Conversation conversation, String message) {
        try {
            // Normalizza: sostituisce virgola con punto per supportare entrambi i formati
            String normalizedInput = message.trim().replace(",", ".");
            BigDecimal giornate = new BigDecimal(normalizedInput);

            if (giornate.compareTo(BigDecimal.ZERO) < 0 || giornate.compareTo(new BigDecimal("31")) > 0) {
                return "Numero non valido (0-31):";
            }

            // Recupera la commessa corrente dall'anagrafica
            int currentIndex = conversation.getCurrentCommessaIndex();
            String commessaId = conversation.getCommesseAttiveIds().get(currentIndex);
            Optional<CommessaAnagrafica> optCommessa = commessaAnagraficaRepository.findById(commessaId);

            if (optCommessa.isEmpty()) {
                return "Errore: commessa non trovata.";
            }

            CommessaAnagrafica anagrafica = optCommessa.get();

            // Crea e salva la commessa solo se giornate > 0
            if (giornate.compareTo(BigDecimal.ZERO) > 0) {
                Commessa commessa = Commessa.builder()
                        .name(anagrafica.getName())
                        .tariffa(anagrafica.getTariffa())
                        .giornate(giornate)
                        .build();
                conversation.addCommessa(commessa);
            }

            // Passa alla prossima commessa
            conversation.setCurrentCommessaIndex(currentIndex + 1);

            // Controlla se ci sono altre commesse
            if (conversation.getCurrentCommessaIndex() < conversation.getCommesseAttiveIds().size()) {
                String nextId = conversation.getCommesseAttiveIds().get(conversation.getCurrentCommessaIndex());
                Optional<CommessaAnagrafica> nextOpt = commessaAnagraficaRepository.findById(nextId);

                if (nextOpt.isPresent()) {
                    return String.format("Giornate per %s?", nextOpt.get().getName());
                }
            }

            // Tutte le commesse processate
            if (conversation.getCommesse().isEmpty()) {
                conversation.reset();
                return "Nessuna giornata inserita. Annullato.";
            }

            conversation.setCurrentState(ConversationState.REVIEW_EMAIL);
            return generateEmailPreview(conversation);

        } catch (NumberFormatException e) {
            return "Inserisci un numero valido (es: 20 o 20,5):";
        }
    }

    /**
     * REVIEW_EMAIL: Conferma invio
     */
    private String handleEmailReview(Conversation conversation, String message) {
        String choice = message.trim();

        return switch (choice) {
            case "1" -> handleSendEmail(conversation);
            case "2" -> handleCancel(conversation);
            default -> "1 = Invia, 2 = Annulla";
        };
    }

    private String generateEmailPreview(Conversation conversation) {
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Commessa c : conversation.getCommesse()) {
            grandTotal = grandTotal.add(c.calculateTotal());
        }

        return String.format("Totale: €%.2f - Confermi? 1=Invia, 2=Annulla", grandTotal);
    }

    private String handleSendEmail(Conversation conversation) {
        try {
            EmailDraft draft = buildEmailDraft(conversation);
            emailService.sendEmail(draft);

            // Salva le richieste di fatturazione nel DB
            for (Commessa c : conversation.getCommesse()) {
                RichiestaFatturazione richiesta = RichiestaFatturazione.builder()
                        .nomeCliente(c.getName())
                        .giornate(c.getGiornate())
                        .tariffa(c.getTariffa())
                        .mese(conversation.getMonth())
                        .anno(conversation.getYear())
                        .build();
                richiestaFatturazioneRepository.save(richiesta);
            }

            conversation.setCurrentState(ConversationState.COMPLETED);
            return "Email inviata!";
        } catch (Exception e) {
            log.error("Errore nell'invio dell'email", e);
            return "Errore invio. Riprova con 1";
        }
    }

    private String handleCancel(Conversation conversation) {
        conversation.reset();
        conversation.setCurrentState(ConversationState.CANCELLED);
        return "Annullato";
    }

    private String handleCompleted(Conversation conversation, String message) {
        String lowerMessage = message.toLowerCase().trim();
        if (lowerMessage.equals("start") || lowerMessage.equals("inizia")) {
            conversation.reset();
            return handleInitialState(conversation, message);
        }
        return "Scrivi 'start' per nuova richiesta";
    }

    private String handleCancelled(Conversation conversation, String message) {
        String lowerMessage = message.toLowerCase().trim();
        if (lowerMessage.equals("start") || lowerMessage.equals("inizia")) {
            conversation.reset();
            return handleInitialState(conversation, message);
        }
        return "Scrivi 'start' per nuova richiesta";
    }

    private EmailDraft buildEmailDraft(Conversation conversation) {
        String subject = String.format("Autorizzazione fatturazione %s %s",
                conversation.getMonth(), conversation.getYear());

        StringBuilder body = new StringBuilder();
        body.append("<p>Ciao,</p>");
        body.append(String.format("<p>con la presente si chiede l'autorizzazione alla fatturazione " +
                "per il mese di <strong>%s %s</strong>.</p>", conversation.getMonth(), conversation.getYear()));
        body.append("<p>Di seguito un recap delle prestazioni svolte:</p>");

        body.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; margin: 20px 0;'>");
        body.append("<thead style='background-color: #f2f2f2;'>");
        body.append("<tr>");
        body.append("<th style='text-align: left;'>Commessa</th>");
        body.append("<th style='text-align: center;'>Giornate</th>");
        body.append("<th style='text-align: right;'>Tariffa</th>");
        body.append("<th style='text-align: right;'>Totale</th>");
        body.append("</tr>");
        body.append("</thead>");
        body.append("<tbody>");

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Commessa c : conversation.getCommesse()) {
            BigDecimal total = c.calculateTotal();
            grandTotal = grandTotal.add(total);

            body.append("<tr>");
            body.append(String.format("<td>%s</td>", c.getName()));
            body.append(String.format("<td style='text-align: center;'>%s</td>", c.getGiornate().stripTrailingZeros().toPlainString()));
            body.append(String.format("<td style='text-align: right;'>&euro;%.2f</td>", c.getTariffa()));
            body.append(String.format("<td style='text-align: right;'>&euro;%.2f</td>", total));
            body.append("</tr>");
        }

        body.append("<tr style='background-color: #f2f2f2; font-weight: bold;'>");
        body.append("<td colspan='3' style='text-align: right;'>TOTALE COMPLESSIVO</td>");
        body.append(String.format("<td style='text-align: right;'>&euro;%.2f</td>", grandTotal));
        body.append("</tr>");

        body.append("</tbody>");
        body.append("</table>");

        body.append("<p>Cordiali saluti</p>");

        return EmailDraft.builder()
                .subject(subject)
                .body(body.toString())
                .recipientEmail(clientEmail)
                .build();
    }

    private boolean isValidMonth(String month) {
        for (String m : MONTHS) {
            if (m.equalsIgnoreCase(month)) {
                return true;
            }
        }
        return false;
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
