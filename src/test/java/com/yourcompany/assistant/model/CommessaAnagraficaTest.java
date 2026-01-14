package com.yourcompany.assistant.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test per il modello CommessaAnagrafica")
class CommessaAnagraficaTest {

    @Test
    @DisplayName("Builder crea oggetto con valori di default")
    void builder_shouldCreateWithDefaultValues() {
        CommessaAnagrafica commessa = CommessaAnagrafica.builder()
                .id("test-id")
                .name("Progetto Test")
                .tariffa(new BigDecimal("250.00"))
                .build();

        assertEquals("test-id", commessa.getId());
        assertEquals("Progetto Test", commessa.getName());
        assertEquals(new BigDecimal("250.00"), commessa.getTariffa());
        assertTrue(commessa.getActive()); // default true
    }

    @Test
    @DisplayName("Active può essere impostato a false")
    void active_canBeSetToFalse() {
        CommessaAnagrafica commessa = CommessaAnagrafica.builder()
                .id("test-id")
                .name("Progetto Inattivo")
                .tariffa(new BigDecimal("200.00"))
                .active(false)
                .build();

        assertFalse(commessa.getActive());
    }

    @Test
    @DisplayName("Setters funzionano correttamente")
    void setters_shouldWorkCorrectly() {
        CommessaAnagrafica commessa = new CommessaAnagrafica();

        commessa.setId("new-id");
        commessa.setName("Nuovo Nome");
        commessa.setTariffa(new BigDecimal("300.00"));
        commessa.setActive(false);

        assertEquals("new-id", commessa.getId());
        assertEquals("Nuovo Nome", commessa.getName());
        assertEquals(new BigDecimal("300.00"), commessa.getTariffa());
        assertFalse(commessa.getActive());
    }

    @Test
    @DisplayName("Tariffa con decimali")
    void tariffa_shouldSupportDecimals() {
        CommessaAnagrafica commessa = CommessaAnagrafica.builder()
                .id("test-id")
                .name("Test")
                .tariffa(new BigDecimal("275.50"))
                .build();

        assertEquals(new BigDecimal("275.50"), commessa.getTariffa());
    }

    @Test
    @DisplayName("Equals e hashCode basati sui campi")
    void equalsAndHashCode_shouldBeBasedOnFields() {
        CommessaAnagrafica c1 = CommessaAnagrafica.builder()
                .id("same-id")
                .name("Progetto")
                .tariffa(new BigDecimal("100"))
                .active(true)
                .build();

        CommessaAnagrafica c2 = CommessaAnagrafica.builder()
                .id("same-id")
                .name("Progetto")
                .tariffa(new BigDecimal("100"))
                .active(true)
                .build();

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
