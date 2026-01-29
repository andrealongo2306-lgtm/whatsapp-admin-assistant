package com.yourcompany.assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "richieste_fatturazione")
public class RichiestaFatturazione {

    @Id
    private String id;

    private String nomeCliente;

    private BigDecimal giornate;

    private BigDecimal tariffa;

    private String mese;

    private String anno;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
