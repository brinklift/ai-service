package com.blift.aiservice.service.insights;

import com.blift.aiservice.dto.feign.BookingCountsDto;
import com.blift.aiservice.dto.response.AiInsightsResponseDto;
import com.blift.aiservice.entity.RcicAiContext;
import com.blift.aiservice.feignClient.BookingFeignClient;
import com.blift.aiservice.feignClient.CaseMgtFeignClient;
import com.blift.aiservice.repository.RcicAiContextRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightsService {

    private final BookingFeignClient bookingFeignClient;
    private final CaseMgtFeignClient caseMgtFeignClient;
    private final RcicAiContextRepository contextRepository;
    private final ObjectMapper objectMapper;

    public AiInsightsResponseDto getInsights(Long rcicUserId) {
        LocalDate today = LocalDate.now();
        Optional<RcicAiContext> ctxOpt = contextRepository.findByRcicUserIdAndContextDate(rcicUserId, today);
        if (ctxOpt.isPresent()) {
            return buildFromSnapshot(ctxOpt.get(), rcicUserId);
        }
        return buildFromLiveData(rcicUserId);
    }

    @SuppressWarnings("unchecked")
    private AiInsightsResponseDto buildFromSnapshot(RcicAiContext context, Long rcicUserId) {
        AiInsightsResponseDto dto = new AiInsightsResponseDto();
        try {
            Map<String, Object> snapshot = objectMapper.readValue(context.getSnapshotJson(), new TypeReference<>() {});
            Map<String, Object> week = (Map<String, Object>) snapshot.getOrDefault("week", Map.of());
            Map<String, Object> today = (Map<String, Object>) snapshot.getOrDefault("today", Map.of());
            Map<String, Object> reportStatus = (Map<String, Object>) snapshot.getOrDefault("reportStatus", Map.of());
            Map<String, Object> workload = (Map<String, Object>) snapshot.getOrDefault("workloadForecast", Map.of());

            double totalRevenue = toDouble(week.get("totalRevenue"));
            double lastWeekRevenue = toDouble(week.get("totalRevenueLastWeek"));
            double changePct = lastWeekRevenue > 0 ? ((totalRevenue - lastWeekRevenue) / lastWeekRevenue) * 100 : 0;

            dto.setTotalRevenue(totalRevenue);
            dto.setTotalRevenueLastWeek(lastWeekRevenue);
            dto.setRevenueChangePct(changePct);
            dto.setConsultationCount(toLong(week.get("consultationCount")));
            dto.setOutstandingPaymentCount(toLong(week.get("outstandingPaymentCount")));
            dto.setPendingRefunds(((List<?>) today.getOrDefault("pendingRefunds", List.of())).size());
            dto.setReportsReadyToShare(toLong(reportStatus.get("readyToShare")));
            dto.setReportsSharedWithClient(toLong(reportStatus.get("sharedWithClient")));
            dto.setReportsNotPurchased(toLong(reportStatus.get("notPurchased")));
            dto.setWorkloadTodayMinutes(toLong(workload.get("todayMinutes")));
            dto.setWorkloadTomorrowMinutes(toLong(workload.get("tomorrowMinutes")));

            // Always fetch workload live — snapshot may be stale if fields were added after snapshot was created
            try {
                var wlResp = bookingFeignClient.getWorkloadForecastForRcic(rcicUserId);
                if (wlResp.getBody() != null) {
                    Map<String, Object> wl = wlResp.getBody();
                    dto.setWeeklyAvailableHours(toDouble(wl.get("weeklyAvailableHours")));
                    dto.setWeeklyBookedHours(toDouble(wl.get("weeklyBookedHours")));
                    dto.setMonthlyAvailableHours(toDouble(wl.get("monthlyAvailableHours")));
                    dto.setMonthlyBookedHours(toDouble(wl.get("monthlyBookedHours")));
                    dto.setTodayBookedHours(toDouble(wl.get("todayBookedHours")));
                    dto.setTomorrowBookedHours(toDouble(wl.get("tomorrowBookedHours")));
                    dto.setWorkloadTodayMinutes((long)(toDouble(wl.get("todayBookedHours")) * 60));
                    dto.setWorkloadTomorrowMinutes((long)(toDouble(wl.get("tomorrowBookedHours")) * 60));
                }
            } catch (Exception ex) {
                log.warn("Could not fetch workload forecast from booking service, using snapshot value: {}", ex.getMessage());
                dto.setWeeklyAvailableHours(toDouble(workload.get("weeklyAvailableHours")));
                dto.setWeeklyBookedHours(toDouble(workload.get("weeklyBookedHours")));
                dto.setMonthlyAvailableHours(toDouble(workload.get("monthlyAvailableHours")));
                dto.setMonthlyBookedHours(toDouble(workload.get("monthlyBookedHours")));
                dto.setTodayBookedHours(toDouble(workload.get("todayBookedHours")));
                dto.setTomorrowBookedHours(toDouble(workload.get("tomorrowBookedHours")));
            }

            dto.setUpcomingToday(toLong(today.get("upcomingTodayCount")));
            dto.setUpcomingTomorrow(toLong(today.get("upcomingTomorrowCount")));
            dto.setUnsignedAgreements(toLong(today.get("unsignedAgreementsCount")));
            dto.setPendingPayments(toLong(today.get("pendingPaymentsCount")));
            dto.setPendingMeetingInvitations(toLong(today.get("pendingMeetingInvitationsCount")));

            List<Map<String, Object>> revenueByDay = (List<Map<String, Object>>) week.getOrDefault("revenueByDay", List.of());
            dto.setRevenueByDay(revenueByDay);

            // Always fetch live purchase count from case-mgt to avoid stale snapshot data
            try {
                var purchaseResp = caseMgtFeignClient.getCompletedReportPurchasesCount(rcicUserId);
                if (purchaseResp.getBody() != null) {
                    long purchased = purchaseResp.getBody();
                    dto.setTotalPurchasedReports(purchased);
                    dto.setTotalPurchaseAmount(purchased * 5.0);
                }
            } catch (Exception ex) {
                log.warn("Could not fetch purchase count from case-mgt, using snapshot value: {}", ex.getMessage());
                dto.setTotalPurchasedReports(toLong(reportStatus.get("totalPurchasedReports")));
                dto.setTotalPurchaseAmount(toDouble(reportStatus.get("totalPurchaseAmount")));
            }

            // Calculate average revenue per consultation and per client
            calculateAverageMetrics(dto, rcicUserId);

            return dto;
        } catch (Exception e) {
            log.warn("Failed to parse snapshot for insights: {}", e.getMessage());
            return buildFromLiveData(rcicUserId);
        }
    }

    private AiInsightsResponseDto buildFromLiveData(Long rcicUserId) {
        AiInsightsResponseDto dto = new AiInsightsResponseDto();
        double totalRevenue = 0;
        double lastWeekRevenue = 0;
        List<Map<String, Object>> revenueByDay = new ArrayList<>();
        try {
            var resp = caseMgtFeignClient.getWeeklyTransactionsForRcic(rcicUserId);
            if (resp.getBody() != null) {
                totalRevenue = resp.getBody().stream()
                        .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0)
                        .sum();
            }
        } catch (Exception e) {
            log.warn("Could not fetch weekly transactions: {}", e.getMessage());
        }

        try {
            var resp = caseMgtFeignClient.getLastWeekTransactionsForRcic(rcicUserId);
            if (resp.getBody() != null) {
                lastWeekRevenue = resp.getBody().stream()
                        .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0)
                        .sum();
            }
        } catch (Exception e) {
            log.warn("Could not fetch last week transactions: {}", e.getMessage());
        }

        try {
            var resp = caseMgtFeignClient.getRevenuByDayForRcic(rcicUserId);
            if (resp.getBody() != null) {
                revenueByDay = resp.getBody();
            }
        } catch (Exception e) {
            log.warn("Could not fetch revenue by day: {}", e.getMessage());
        }

        double changePct = lastWeekRevenue > 0 ? ((totalRevenue - lastWeekRevenue) / lastWeekRevenue) * 100 : 0;
        dto.setTotalRevenue(totalRevenue);
        dto.setTotalRevenueLastWeek(lastWeekRevenue);
        dto.setRevenueChangePct(changePct);
        dto.setRevenueByDay(revenueByDay);

        try {
            var resp = bookingFeignClient.getBookingCountsForRcic(rcicUserId);
            if (resp.getBody() != null) {
                BookingCountsDto counts = resp.getBody();
                dto.setUpcomingToday(counts.getUpcomingToday());
                dto.setUpcomingTomorrow(counts.getUpcomingTomorrow());
                dto.setUnsignedAgreements(counts.getUnsignedAgreements());
                dto.setPendingPayments(counts.getPendingPayments());
                dto.setPendingMeetingInvitations(counts.getPendingMeetingInvitations());
                dto.setConsultationCount(counts.getUpcomingToday() + counts.getUpcomingTomorrow() + counts.getUpcomingNext7Days());
                dto.setOutstandingPaymentCount(counts.getPendingPayments());
                dto.setWorkloadTodayMinutes(counts.getUpcomingToday() * 45);
                dto.setWorkloadTomorrowMinutes(counts.getUpcomingTomorrow() * 45);
            }
        } catch (Exception e) {
            log.warn("Could not fetch booking counts: {}", e.getMessage());
        }

        try {
            var resp = bookingFeignClient.getReportStatusCountsForRcic(rcicUserId);
            if (resp.getBody() != null) {
                Map<String, Object> status = resp.getBody();
                dto.setReportsReadyToShare(toLong(status.get("readyToShare")));
                dto.setReportsSharedWithClient(toLong(status.get("sharedWithClient")));
                dto.setReportsNotPurchased(toLong(status.get("notPurchased")));
            }
        } catch (Exception e) {
            log.warn("Could not fetch report status counts from booking service: {}", e.getMessage());
        }

        // Workload forecast from real availability + confirmed bookings
        try {
            var wlResp = bookingFeignClient.getWorkloadForecastForRcic(rcicUserId);
            if (wlResp.getBody() != null) {
                Map<String, Object> wl = wlResp.getBody();
                dto.setWeeklyAvailableHours(toDouble(wl.get("weeklyAvailableHours")));
                dto.setWeeklyBookedHours(toDouble(wl.get("weeklyBookedHours")));
                dto.setMonthlyAvailableHours(toDouble(wl.get("monthlyAvailableHours")));
                dto.setMonthlyBookedHours(toDouble(wl.get("monthlyBookedHours")));
                dto.setTodayBookedHours(toDouble(wl.get("todayBookedHours")));
                dto.setTomorrowBookedHours(toDouble(wl.get("tomorrowBookedHours")));
                dto.setWorkloadTodayMinutes((long)(toDouble(wl.get("todayBookedHours")) * 60));
                dto.setWorkloadTomorrowMinutes((long)(toDouble(wl.get("tomorrowBookedHours")) * 60));
            }
        } catch (Exception e) {
            log.warn("Could not fetch workload forecast from booking service: {}", e.getMessage());
        }

        // Fetch purchase count directly from case-mgt to ensure accuracy
        try {
            var purchaseResp = caseMgtFeignClient.getCompletedReportPurchasesCount(rcicUserId);
            if (purchaseResp.getBody() != null) {
                long purchased = purchaseResp.getBody();
                dto.setTotalPurchasedReports(purchased);
                dto.setTotalPurchaseAmount(purchased * 5.0);
            }
        } catch (Exception e) {
            log.warn("Could not fetch completed report purchase count from case-mgt: {}", e.getMessage());
        }

        // Calculate average revenue per consultation and per client
        calculateAverageMetrics(dto, rcicUserId);

        return dto;
    }

    private void calculateAverageMetrics(AiInsightsResponseDto dto, Long rcicUserId) {
        // Use total revenue for the calculation
        double totalRevenueForCalc = dto.getTotalRevenue();
        log.info("calculateAverageMetrics - RCIC ID: {}, Total Revenue: {}", rcicUserId, totalRevenueForCalc);
        
        // For average revenue per consultation: fetch total completed consultations
        try {
            var consultationsResp = bookingFeignClient.getTotalCompletedConsultationsForRcic(rcicUserId);
            if (consultationsResp.getBody() != null) {
                long totalConsultations = consultationsResp.getBody();
                log.info("Total Completed Consultations for RCIC {}: {}", rcicUserId, totalConsultations);
                
                // Calculate average revenue per consultation
                if (totalConsultations > 0 && totalRevenueForCalc > 0) {
                    double avgPerConsultation = totalRevenueForCalc / totalConsultations;
                    dto.setAverageRevenuePerConsultation(avgPerConsultation);
                    log.info("Average Revenue per Consultation: {} = {} / {}", avgPerConsultation, totalRevenueForCalc, totalConsultations);
                } else {
                    log.info("Not calculating - totalConsultations: {}, totalRevenueForCalc: {}", totalConsultations, totalRevenueForCalc);
                    dto.setAverageRevenuePerConsultation(0.0);
                }
            } else {
                log.warn("Consultations response body is null");
                dto.setAverageRevenuePerConsultation(0.0);
            }
        } catch (Exception e) {
            log.warn("Could not fetch total completed consultations: {}", e.getMessage(), e);
            dto.setAverageRevenuePerConsultation(0.0);
        }

        // For average revenue per client: Try to fetch from booking service
        try {
            var clientsResp = bookingFeignClient.getTotalClientsServedForRcic(rcicUserId);
            if (clientsResp.getBody() != null) {
                long totalClientsServed = clientsResp.getBody();
                log.info("Total Unique Clients for RCIC {}: {}", rcicUserId, totalClientsServed);
                dto.setTotalClientsServed(totalClientsServed);

                // Calculate average revenue per client
                if (totalClientsServed > 0 && totalRevenueForCalc > 0) {
                    double avgPerClient = totalRevenueForCalc / totalClientsServed;
                    dto.setAverageRevenuePerClient(avgPerClient);
                    log.info("Average Revenue per Client: {} = {} / {}", avgPerClient, totalRevenueForCalc, totalClientsServed);
                } else {
                    log.info("Not calculating client avg - totalClientsServed: {}, totalRevenueForCalc: {}", totalClientsServed, totalRevenueForCalc);
                    dto.setAverageRevenuePerClient(0.0);
                }
            } else {
                log.warn("Clients response body is null");
                dto.setTotalClientsServed(0);
                dto.setAverageRevenuePerClient(0.0);
            }
        } catch (Exception e) {
            log.warn("Could not fetch total clients served: {}", e.getMessage(), e);
            dto.setTotalClientsServed(0);
            dto.setAverageRevenuePerClient(0.0);
        }
    }

    private double toDouble(Object val) {
        if (val == null) {
            return 0.0;
        }
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private long toLong(Object val) {
        if (val == null) {
            return 0L;
        }
        if (val instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return 0L;
        }
    }
}
