package com.yourcompany.assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Commessa {
    private String name;
    private BigDecimal giornate;
    private BigDecimal tariffa;

    public BigDecimal calculateTotal() {
        if (giornate == null || tariffa == null) {
            return BigDecimal.ZERO;
        }
        return tariffa.multiply(giornate);
    }
}
