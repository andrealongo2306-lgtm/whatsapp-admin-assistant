package com.yourcompany.assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * Anagrafica delle commesse salvata su MongoDB.
 * Contiene nome e tariffa giornaliera predefinita.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "commesse_anagrafica")
public class CommessaAnagrafica {

    @Id
    private String id;

    private String name;

    private BigDecimal tariffa;

    @Builder.Default
    private Boolean active = true;
}
