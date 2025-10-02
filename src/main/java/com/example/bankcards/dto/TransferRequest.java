package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotNull
    private UUID fromCardId;
    @NotNull
    private UUID toCardId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
}