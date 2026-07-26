package com.blift.aiservice.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiActionCentreSummaryDto {
    private long upcomingConsultations;
    private long pendingRescheduleRequests;
    private long pendingMeetingInvitations;
    private long unsignedAgreements;
    private long pendingPayments;
    private long consultationReports;
    private long pendingRefunds;
    private long unreadMessages;
    private long walletActivity;
    private long criticalItems;
}
