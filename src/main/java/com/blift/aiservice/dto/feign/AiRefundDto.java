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
public class AiRefundDto {
    private Long id;
    private String status;
    private BigDecimal amount;
    private String currencyCode;
    private String clientName;
    private String createdAt;
    private String bookingId;
}
