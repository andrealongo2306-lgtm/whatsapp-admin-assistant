package com.yourcompany.assistant.scheduler;

import com.yourcompany.assistant.MessageTemplates; // [REVIEWAGENT] import della costante centralizzata

// ... resto invariato del file ...

// Riga ~131 (interno a monthlyBillingReminder):
//
//     log.info("Step 4: Invio messaggio WhatsApp...");
//     String message = MessageTemplates.WELCOME_MESSAGE; // [REVIEWAGENT] rimossa stringa hardcoded duplicata; uso costante centralizzata. Testo ripristinato al valore originale approvato — il nuovo testo non è stato concordato con il team come richiesto dal task.
//     String sid = twilioService.sendWhatsAppMessage(adminPhoneNumber, message);
