package com.yourcompany.assistant.service;

import com.yourcompany.assistant.constants.BotMessages;
// ... (altri import invariati)

// La costante WELCOME_MESSAGE è stata rimossa da questa classe e spostata in BotMessages.
// Tutti i riferimenti interni usano ora BotMessages.WELCOME_MESSAGE.

    // ...

    private String handleInitialState(Conversation conversation, String message) {
        conversation.setCurrentState(ConversationState.WAITING_MONTH_YEAR);
        return BotMessages.WELCOME_MESSAGE;
    }

    // ...

    private String generateEmailPreview(Conversation conversation) {
        // ...
        return String.format("Totale: \u20ac%.2f - Confermi? 1=Invia, 2=Annulla", grandTotal);
        // NOTA: €  ripristinato come carattere letterale nel sorgente originale
    }
