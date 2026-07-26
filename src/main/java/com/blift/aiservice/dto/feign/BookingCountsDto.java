package com.blift.aiservice.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCountsDto {
    private long upcomingToday;
    private long upcomingTomorrow;
    private long upcomingNext7Days;
    private long pendingRescheduleRequests;
    private long counterProposedRequests;
    private long pendingMeetingInvitations;
    private long unsignedAgreements;
    private long pendingPayments;
    private long reportsReadyToShare;
    private long failedPayments;
}
