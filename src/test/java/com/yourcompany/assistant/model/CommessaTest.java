package com.yourcompany.assistant.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test per il modello Commessa")
class CommessaTest {

    @Test
    @DisplayName("calculateTotal restituisce il totale corretto")
    void calculateTotal_shouldReturnCorrectTotal() {
        Commessa commessa = Commessa.builder()
                .name("Progetto Test")
                .giornate(new BigDecimal("5"))
                .tariffa(new BigDecimal("250.00"))
                .build();

        BigDecimal expected = new BigDecimal("1250.00");
        assertEquals(0, expected.compareTo(commessa.calculateTotal()));
    }

    @Test
    @DisplayName("calculateTotal restituisce ZERO quando giornate è null")
    void calculateTotal_shouldReturnZero_whenGiornateIsNull() {
        Commessa commessa = Commessa.builder()
                .name("Progetto Test")
                .giornate(null)
                .tariffa(new BigDecimal("250.00"))
                .build();

        assertEquals(BigDecimal.ZERO, commessa.calculateTotal());
    }

    @Test
    @DisplayName("calculateTotal restituisce ZERO quando tariffa è null")
    void calculateTotal_shouldReturnZero_whenTariffaIsNull() {
        Commessa commessa = Commessa.builder()
                .name("Progetto Test")
                .giornate(new BigDecimal("5"))
                .tariffa(null)
                .build();

        assertEquals(BigDecimal.ZERO, commessa.calculateTotal());
    }

    @Test
    @DisplayName("calculateTotal restituisce ZERO quando entrambi sono null")
    void calculateTotal_shouldReturnZero_whenBothAreNull() {
        Commessa commessa = Commessa.builder()
                .name("Progetto Test")
                .build();

        assertEquals(BigDecimal.ZERO, commessa.calculateTotal());
    }

    @Test
    @DisplayName("calculateTotal con zero giornate")
    void calculateTotal_shouldReturnZero_whenGiornateIsZero() {
        Commessa commessa = Commessa.builder()
                .name("Progetto Test")
                .giornate(BigDecimal.ZERO)
                .tariffa(new BigDecimal("250.00"))
                .build();

        assertEquals(0, BigDecimal.ZERO.compareTo(commessa.calculateTotal()));
    }

    @Test
    @DisplayName("Builder crea oggetto correttamente")
    void builder_shouldCreateObjectCorrectly() {
        Commessa commessa = Commessa.builder()
                .name("Progetto ABC")
                .giornate(new BigDecimal("10"))
                .tariffa(new BigDecimal("300.00"))
                .build();

        assertEquals("Progetto ABC", commessa.getName());
        assertEquals(0, new BigDecimal("10").compareTo(commessa.getGiornate()));
        assertEquals(new BigDecimal("300.00"), commessa.getTariffa());
    }
}
