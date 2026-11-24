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
    private long consultationRequests;
    private long failedPayments;
    private long caseTasks;
    /** Sources case-mgt could not read; their counts mean "unknown", not zero. */
    private java.util.List<String> unavailable;
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
