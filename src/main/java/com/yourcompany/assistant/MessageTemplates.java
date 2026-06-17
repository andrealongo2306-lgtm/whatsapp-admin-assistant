package com.yourcompany.assistant;

/**
 * Costanti centralizzate per i messaggi dell'assistente virtuale.
 *
 * [REVIEWAGENT] Nuovo file creato per eliminare la duplicazione della stringa
 * di benvenuto presente in SessionCleanupScheduler e ConversationService.
 * Singolo punto di modifica: il testo va concordato con il team (requisito
 * esplicito del task) e aggiornato UNA SOLA VOLTA qui prima del merge.
 */
public final class MessageTemplates {

    private MessageTemplates() {} // [REVIEWAGENT] classe di sole costanti, non istanziabile

    /**
     * Messaggio di benvenuto inviato all'avvio della conversazione e nel reminder schedulato.
     * ATTENZIONE: il testo deve essere concordato con il team prima del merge (cfr. ticket).
     */
    // [REVIEWAGENT] placeholder esplicito: il testo originale è ripristinato come valore di
    // default sicuro; sostituire con il testo concordato internamente prima del merge.
    // Il richiedente ha delegato la scelta al team — nessun testo alternativo è stato validato.
    public static final String WELCOME_MESSAGE =
            "Ciao! Sono il tuo assistente. E' ora di inviare l'autorizzazione alla fatturazione. Mese e anno? (es: Gennaio-2024)";

}
