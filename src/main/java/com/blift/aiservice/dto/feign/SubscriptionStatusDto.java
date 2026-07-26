package com.blift.aiservice.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatusDto {
    private String planType;
    private boolean active;
    private boolean inGracePeriod;
    private Instant trialExpiresAt;
    private Instant subscriptionActiveUntil;
    private boolean aiConsentGiven;
    private long daysRemaining;
}
