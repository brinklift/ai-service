package com.blift.aiservice.service.context;

import com.blift.aiservice.dto.feign.AiRcicProfileDto;
import com.blift.aiservice.dto.feign.AiRefundDto;
import com.blift.aiservice.dto.feign.AiWorkspaceDto;
import com.blift.aiservice.dto.feign.BookingCountsDto;
import com.blift.aiservice.entity.RcicAiContext;
import com.blift.aiservice.feignClient.BookingFeignClient;
import com.blift.aiservice.feignClient.CaseMgtFeignClient;
import com.blift.aiservice.feignClient.UserFeignClient;
import com.blift.aiservice.repository.RcicAiContextRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiContextBuilderService {

    private final UserFeignClient userFeignClient;
    private final BookingFeignClient bookingFeignClient;
    private final CaseMgtFeignClient caseMgtFeignClient;
    private final RcicAiContextRepository contextRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${ai.briefing.schedule-cron:0 0 4 * * *}")
    public void runNightlyContextBuild() {
        log.info("[AI Context Builder] Starting nightly context build");
        List<Long> activeRcicIds = getActiveSubscriberIds();
        log.info("[AI Context Builder] Found {} active Blift Pro subscribers", activeRcicIds.size());

        int batchSize = 50;
        for (int i = 0; i < activeRcicIds.size(); i += batchSize) {
            List<Long> batch = activeRcicIds.subList(i, Math.min(i + batchSize, activeRcicIds.size()));
            processBatch(batch);
            if (i + batchSize < activeRcicIds.size()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        log.info("[AI Context Builder] Nightly context build complete");
    }

    public RcicAiContext buildContextForRcic(Long rcicUserId) {
        LocalDate today = LocalDate.now();
        log.info("[AI Context Builder] Building context for RCIC user {}", rcicUserId);
        try {
            Map<String, Object> snapshot = assembleSnapshot(rcicUserId, today);
            String snapshotJson = objectMapper.writeValueAsString(snapshot);

            RcicAiContext context = contextRepository
                    .findByRcicUserIdAndContextDate(rcicUserId, today)
                    .orElseGet(() -> {
                        RcicAiContext created = new RcicAiContext();
                        created.setRcicUserId(rcicUserId);
                        created.setContextDate(today);
                        return created;
                    });
            context.setSnapshotJson(snapshotJson);
            context.setBuildStatus("SUCCESS");
            context.setBuildErrorMessage(null);
            return contextRepository.save(context);
        } catch (Exception e) {
            log.error("[AI Context Builder] Failed to build context for RCIC {}: {}", rcicUserId, e.getMessage(), e);
            RcicAiContext failed = contextRepository
                    .findByRcicUserIdAndContextDate(rcicUserId, today)
                    .orElseGet(() -> {
                        RcicAiContext created = new RcicAiContext();
                        created.setRcicUserId(rcicUserId);
                        created.setContextDate(today);
                        created.setSnapshotJson("{}");
                        return created;
                    });
            failed.setBuildStatus("FAILED");
            failed.setBuildErrorMessage(e.getMessage());
            return contextRepository.save(failed);
        }
    }

    private void processBatch(List<Long> rcicIds) {
        for (Long rcicUserId : rcicIds) {
            try {
                buildContextForRcic(rcicUserId);
            } catch (Exception e) {
                log.error("[AI Context Builder] Unhandled error for RCIC {}: {}", rcicUserId, e.getMessage());
            }
        }
    }

    private List<Long> getActiveSubscriberIds() {
        try {
            var response = userFeignClient.getActiveBliftProRcicIds();
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.error("[AI Context Builder] Failed to get active subscriber IDs: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> assembleSnapshot(Long rcicUserId, LocalDate today) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("builtAt", Instant.now().toString());

        try {
            var profileResp = userFeignClient.getRcicProfileByUserId(rcicUserId);
            if (profileResp.getBody() != null) {
                AiRcicProfileDto p = profileResp.getBody();
                String firstName = Optional.ofNullable(p.getFirstName()).orElse("");
                String lastName = Optional.ofNullable(p.getLastName()).orElse("");
                snapshot.put("rcic", Map.of(
                        "userId", rcicUserId,
                        "name", (firstName + " " + lastName).trim(),
                        "firstName", firstName,
                        "timezone", Optional.ofNullable(p.getTimezone()).orElse("America/Toronto")
                ));
            }
        } catch (Exception e) {
            log.warn("[AI Context] Could not fetch RCIC profile for {}: {}", rcicUserId, e.getMessage());
            snapshot.put("rcic", Map.of("userId", rcicUserId, "name", "RCIC", "timezone", "America/Toronto"));
        }

        BookingCountsDto counts = null;
        try {
            var resp = bookingFeignClient.getBookingCountsForRcic(rcicUserId);
            counts = resp.getBody();
        } catch (Exception e) {
            log.warn("[AI Context] Could not fetch booking counts for {}: {}", rcicUserId, e.getMessage());
        }

        Map<String, Object> todaySection = new LinkedHashMap<>();
        todaySection.put("date", today.toString());
        todaySection.put("consultationCount", counts != null ? counts.getUpcomingToday() : 0);
        todaySection.put("upcomingTodayCount", counts != null ? counts.getUpcomingToday() : 0);
        todaySection.put("upcomingTomorrowCount", counts != null ? counts.getUpcomingTomorrow() : 0);
        todaySection.put("unreadMessageCount", 0);

        List<Map<String, Object>> reportsReady = new ArrayList<>();
        try {
            var resp = bookingFeignClient.getReportsReadyToShareForRcic(rcicUserId);
            if (resp.getBody() != null) {
                for (AiWorkspaceDto w : resp.getBody()) {
                    reportsReady.add(Map.of(
                            "workspaceId", w.getId(),
                            "clientName", Optional.ofNullable(w.getClientName()).orElse(""),
                            "bookingReference", Optional.ofNullable(w.getBookingReference()).orElse("")
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("[AI Context] Could not fetch reports for {}: {}", rcicUserId, e.getMessage());
        }
        todaySection.put("reportsReadyToShare", reportsReady);

        List<Map<String, Object>> refunds = new ArrayList<>();
        try {
            var resp = caseMgtFeignClient.getPendingRefundsForRcic(rcicUserId);
            if (resp.getBody() != null) {
                for (AiRefundDto r : resp.getBody()) {
                    long daysPending = 0;
                    if (r.getCreatedAt() != null) {
                        try {
                            Instant created = Instant.parse(r.getCreatedAt());
                            daysPending = ChronoUnit.DAYS.between(created, Instant.now());
                        } catch (Exception ignored) {
                        }
                    }
                    refunds.add(Map.of(
                            "refundId", r.getId(),
                            "clientName", Optional.ofNullable(r.getClientName()).orElse(""),
                            "amount", r.getAmount() != null ? r.getAmount() : 0,
                            "currencyCode", Optional.ofNullable(r.getCurrencyCode()).orElse("CAD"),
                            "daysPending", daysPending
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("[AI Context] Could not fetch refunds for {}: {}", rcicUserId, e.getMessage());
        }
        todaySection.put("pendingRefunds", refunds);
        todaySection.put("unsignedAgreementsCount", counts != null ? counts.getUnsignedAgreements() : 0);
        todaySection.put("pendingPaymentsCount", counts != null ? counts.getPendingPayments() : 0);
        todaySection.put("pendingMeetingInvitationsCount", counts != null ? counts.getPendingMeetingInvitations() : 0);
        snapshot.put("today", todaySection);

        Map<String, Object> weekSection = new LinkedHashMap<>();
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
            log.warn("[AI Context] Could not fetch weekly transactions for {}: {}", rcicUserId, e.getMessage());
        }

        try {
            var resp = caseMgtFeignClient.getLastWeekTransactionsForRcic(rcicUserId);
            if (resp.getBody() != null) {
                lastWeekRevenue = resp.getBody().stream()
                        .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0)
                        .sum();
            }
        } catch (Exception e) {
            log.warn("[AI Context] Could not fetch last week transactions for {}: {}", rcicUserId, e.getMessage());
        }

        try {
            var resp = caseMgtFeignClient.getRevenuByDayForRcic(rcicUserId);
            if (resp.getBody() != null) {
                revenueByDay = resp.getBody();
            }
        } catch (Exception e) {
            log.warn("[AI Context] Could not fetch revenue by day for {}: {}", rcicUserId, e.getMessage());
        }

        weekSection.put("totalRevenue", totalRevenue);
        weekSection.put("totalRevenueLastWeek", lastWeekRevenue);
        weekSection.put("consultationCount", counts != null ? (counts.getUpcomingToday() + counts.getUpcomingTomorrow() + counts.getUpcomingNext7Days()) : 0);
        weekSection.put("revenueByDay", revenueByDay);
        weekSection.put("outstandingPaymentCount", counts != null ? counts.getPendingPayments() : 0);
        snapshot.put("week", weekSection);

        Map<String, Object> workloadMap = new LinkedHashMap<>();
        workloadMap.put("weeklyAvailableHours", 0.0);
        workloadMap.put("weeklyBookedHours", 0.0);
        workloadMap.put("todayBookedHours", 0.0);
        workloadMap.put("tomorrowBookedHours", 0.0);
        // Keep legacy minute fields for backward compatibility
        workloadMap.put("todayMinutes", counts != null ? counts.getUpcomingToday() * 45 : 0);
        workloadMap.put("tomorrowMinutes", counts != null ? counts.getUpcomingTomorrow() * 45 : 0);

        try {
            var wlResp = bookingFeignClient.getWorkloadForecastForRcic(rcicUserId);
            if (wlResp.getBody() != null) {
                Map<String, Object> wl = wlResp.getBody();
                workloadMap.put("weeklyAvailableHours", wl.getOrDefault("weeklyAvailableHours", 0.0));
                workloadMap.put("weeklyBookedHours",    wl.getOrDefault("weeklyBookedHours",    0.0));
                workloadMap.put("todayBookedHours",     wl.getOrDefault("todayBookedHours",     0.0));
                workloadMap.put("tomorrowBookedHours",  wl.getOrDefault("tomorrowBookedHours",  0.0));
                // Update minute fields from real data too
                workloadMap.put("todayMinutes",    (long)(((Number)wl.getOrDefault("todayBookedHours",    0.0)).doubleValue() * 60));
                workloadMap.put("tomorrowMinutes", (long)(((Number)wl.getOrDefault("tomorrowBookedHours", 0.0)).doubleValue() * 60));
            }
        } catch (Exception e) {
            log.warn("[AI Context] Could not fetch workload forecast for RCIC {}: {}", rcicUserId, e.getMessage());
        }

        snapshot.put("workloadForecast", workloadMap);

        Map<String, Object> reportStatusMap = new java.util.LinkedHashMap<>();
        reportStatusMap.put("readyToShare", 0L);
        reportStatusMap.put("sharedWithClient", 0L);
        reportStatusMap.put("notPurchased", 0L);
        reportStatusMap.put("totalPurchasedReports", 0L);
        reportStatusMap.put("totalPurchaseAmount", 0.0);

        try {
            var resp = bookingFeignClient.getReportStatusCountsForRcic(rcicUserId);
            if (resp.getBody() != null) {
                reportStatusMap.putAll(resp.getBody());
            }
        } catch (Exception e) {
            log.warn("Could not fetch report workspace counts from booking service: {}", e.getMessage());
        }

        // Override purchase count directly from case-mgt for accuracy
        try {
            var purchaseResp = caseMgtFeignClient.getCompletedReportPurchasesCount(rcicUserId);
            if (purchaseResp.getBody() != null) {
                long purchased = purchaseResp.getBody();
                reportStatusMap.put("totalPurchasedReports", purchased);
                reportStatusMap.put("totalPurchaseAmount", purchased * 5.0);
            }
        } catch (Exception e) {
            log.warn("Could not fetch completed report purchase count from case-mgt: {}", e.getMessage());
        }

        snapshot.put("reportStatus", reportStatusMap);

        return snapshot;
    }
}
