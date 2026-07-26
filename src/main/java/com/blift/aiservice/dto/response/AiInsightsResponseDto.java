package com.blift.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsightsResponseDto {
    private double totalRevenue;
    private double totalRevenueLastWeek;
    private double revenueChangePct;
    private double outstandingPayments;
    private long outstandingPaymentCount;
    private long consultationCount;
    private long consultationCountLastWeek;
    private double conversionRate;
    private long workloadTodayMinutes;
    private long workloadTomorrowMinutes;
    private double weeklyAvailableHours;
    private double weeklyBookedHours;
    private double monthlyAvailableHours;
    private double monthlyBookedHours;
    private double todayBookedHours;
    private double tomorrowBookedHours;
    private long reportsReadyToShare;
    private long reportsSharedWithClient;
    private long reportsNotPurchased;
    private long totalPurchasedReports;
    private double totalPurchaseAmount;
    private List<Map<String, Object>> revenueByDay;
    private long upcomingToday;
    private long upcomingTomorrow;
    private long pendingRefunds;
    private long unsignedAgreements;
    private long pendingPayments;
    private long pendingMeetingInvitations;
    private double averageRevenuePerConsultation;
    private double averageRevenuePerClient;
    private long totalClientsServed;
}
