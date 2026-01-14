package com.yourcompany.assistant.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.yourcompany.assistant.config.TwilioConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioService {
    
    private final TwilioConfig twilioConfig;
    
    /**
     * Invia un messaggio WhatsApp tramite Twilio
     *
     * @param to Numero destinatario (formato: whatsapp:+39123456789)
     * @param messageBody Testo del messaggio
     * @return Message SID se inviato con successo
     */
    public String sendWhatsAppMessage(String to, String messageBody) {
        try {
            String formattedTo = formatPhoneNumber(to);

            // Mock mode: logga il messaggio senza inviarlo
            if (twilioConfig.isMockMode()) {
                String mockSid = "MOCK_" + System.currentTimeMillis();
                log.info("═══════════════════════════════════════════════════════════");
                log.info("📱 MOCK MODE - Messaggio NON inviato (risparmio quota Twilio)");
                log.info("📤 To: {}", formattedTo);
                log.info("📝 Body: {}", messageBody);
                log.info("🔑 Mock SID: {}", mockSid);
                log.info("═══════════════════════════════════════════════════════════");
                return mockSid;
            }

            // Produzione: invia realmente via Twilio
            Message message = Message.creator(
                new PhoneNumber(formattedTo),
                new PhoneNumber(twilioConfig.getWhatsappNumber()),
                messageBody
            ).create();

            log.info("Messaggio inviato con successo. SID: {}", message.getSid());
            return message.getSid();

        } catch (Exception e) {
            log.error("Errore nell'invio del messaggio WhatsApp a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Impossibile inviare il messaggio WhatsApp", e);
        }
    }
    
    /**
     * Formatta il numero di telefono per WhatsApp
     * Aggiunge il prefisso "whatsapp:" se non presente
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new IllegalArgumentException("Il numero di telefono non può essere vuoto");
        }
        
        if (!phoneNumber.startsWith("whatsapp:")) {
            return "whatsapp:" + phoneNumber;
        }
        
        return phoneNumber;
    }
    
    /**
     * Invia un messaggio formattato con emoji per migliore UX
     */
    public String sendFormattedMessage(String to, String emoji, String message) {
        String formattedMessage = emoji + " " + message;
        return sendWhatsAppMessage(to, formattedMessage);
    }
}
