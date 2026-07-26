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
public class AiBookingDto {
    private Long id;
    private String bookingId;
    private String status;
    private Long bookingUserId;
    private Long rcicUserId;
    private BigDecimal agreedFee;
    private String agreedCurrencyCode;
    private String createDateTime;
    private String updateDateTime;
}
