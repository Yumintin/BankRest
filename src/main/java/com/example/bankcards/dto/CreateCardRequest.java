package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {
    @NotBlank
    @Pattern(regexp = "\\d{16}", message = "Номер карты должен состоять из 16 цифр")
    private String cardNumber;
    @NotNull
    private Long ownerId;
    private String ownerName;
    @NotBlank
    private String expiryDate;
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal initialBalance;
}