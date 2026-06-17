package com.yourcompany.assistant.scheduler;

import com.yourcompany.assistant.constants.BotMessages;
import com.yourcompany.assistant.enums.ConversationState;
import com.yourcompany.assistant.model.Conversation;
import com.yourcompany.assistant.repository.ConversationRepository;
import com.yourcompany.assistant.service.TwilioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// ... (resto invariato fino al metodo monthlyBillingReminder)

            log.info("Step 3: Salvataggio conversazione...");
            conversationRepository.save(conversation);

            // Invia messaggio — testo allineato con BotMessages.WELCOME_MESSAGE
            log.info("Step 4: Invio messaggio WhatsApp...");
            String sid = twilioService.sendWhatsAppMessage(adminPhoneNumber, BotMessages.WELCOME_MESSAGE);

            log.info("Step 5: Messaggio inviato con SID: {}", sid);
            log.info("Promemoria fatturazione inviato a {}", adminPhoneNumber);
