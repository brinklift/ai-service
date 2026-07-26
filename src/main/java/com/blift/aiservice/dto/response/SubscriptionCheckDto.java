package com.blift.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionCheckDto {
    private boolean active;
    private String planType;
    private boolean inGracePeriod;
    private long daysRemaining;
    private boolean aiConsentGiven;
    private Instant trialExpiresAt;
}
