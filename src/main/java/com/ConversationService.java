package com;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class ConversationService {
    public void scheduleFirstMessage() {
        LocalDate lastWorkingDay = getLastWorkingDayOfMonth();
        LocalDateTime scheduledTime = lastWorkingDay.atTime(17, 50);
        // Logica per inviare il messaggio pianificato
    }

    private LocalDate getLastWorkingDayOfMonth() {
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());
        while (lastDayOfMonth.getDayOfWeek().getValue() > 5) { // 6 = Saturday, 7 = Sunday
            lastDayOfMonth = lastDayOfMonth.minusDays(1);
        }
        return lastDayOfMonth;
    }
}
