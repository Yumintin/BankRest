package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
    private UUID id;
    private String maskedCardNumber;
    private Long ownerId;
    private String expiryDate;
    private String status;
    private BigDecimal balance;
    private String last4;
}
