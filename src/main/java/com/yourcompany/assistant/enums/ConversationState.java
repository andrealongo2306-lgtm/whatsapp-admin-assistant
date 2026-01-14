package com.yourcompany.assistant.enums;

public enum ConversationState {
    INITIAL,
    WAITING_MONTH_YEAR,      // Mese e anno insieme (es: Gennaio-2024)
    WAITING_GIORNATE,        // Giornate per ogni commessa attiva dal DB
    REVIEW_EMAIL,
    COMPLETED,
    CANCELLED
}
