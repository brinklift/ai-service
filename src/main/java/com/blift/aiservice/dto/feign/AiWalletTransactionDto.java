package com.blift.aiservice.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiWalletTransactionDto {
    private Long id;
    private BigDecimal amount;
    private String transactionType;
    private String description;
    private String createdAt;
}
