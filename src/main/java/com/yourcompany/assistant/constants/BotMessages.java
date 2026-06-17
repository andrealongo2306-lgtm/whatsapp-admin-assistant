package com.yourcompany.assistant.constants;

/**
 * Costanti testuali utilizzate dal chatbot.
 * Centralizzate qui per essere accessibili da layer diversi (service, scheduler)
 * senza introdurre dipendenze architetturali improprie.
 */
public final class BotMessages {

    private BotMessages() {
        // utility class — non istanziabile
    }

    /**
     * Messaggio di benvenuto inviato all'utente all'avvio della conversazione.
     * Utilizzata in: ConversationService.handleInitialState(), SessionCleanupScheduler.monthlyBillingReminder().
     * Modificare qui ha effetto su entrambi i punti di utilizzo.
     * NOTA: il testo non contiene nomi propri hardcoded — personalizzare a livello di chiamante se necessario.
     */
    public static final String WELCOME_MESSAGE =
            "Ciao! \uD83D\uDC4B Sono il tuo assistente per la fatturazione.\n" +
            "Indica il mese e l'anno per cui vuoi richiedere l'autorizzazione alla fatturazione.\n" +
            "Esempio: Gennaio-2024";
}
