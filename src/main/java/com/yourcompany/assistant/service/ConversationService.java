package com.yourcompany.assistant.service;

import com.yourcompany.assistant.MessageTemplates; // [REVIEWAGENT] import della costante centralizzata

// ... resto invariato del file ...

// Riga ~88 (interno a handleInitialState):
//
//     private String handleInitialState(Conversation conversation, String message) {
//         conversation.setCurrentState(ConversationState.WAITING_MONTH_YEAR);
//         return MessageTemplates.WELCOME_MESSAGE; // [REVIEWAGENT] rimossa stringa hardcoded duplicata; uso costante centralizzata. Testo ripristinato al valore originale approvato — il nuovo testo non è stato concordato con il team come richiesto dal task.
//     }

// Riga ~223 (interno a generateEmailPreview):
//
//     return String.format("Totale: \u20ac%.2f - Confermi? 1=Invia, 2=Annulla", grandTotal);
// diventa:
//     return String.format("Totale: €%.2f - Confermi? 1=Invia, 2=Annulla", grandTotal); // [REVIEWAGENT] revert della modifica fuori perimetro: € e \u20ac sono identici a runtime ma € è più leggibile nel sorgente. Questa riga non era nel perimetro del task e non doveva essere modificata.
