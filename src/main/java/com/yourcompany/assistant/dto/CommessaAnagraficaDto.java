package com.yourcompany.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommessaAnagraficaDto {

    private String id;

    @NotBlank(message = "Nome commessa obbligatorio")
    private String name;

    @NotNull(message = "Tariffa obbligatoria")
    @Positive(message = "Tariffa deve essere positiva")
    private BigDecimal tariffa;

    private Boolean active = true;
}
