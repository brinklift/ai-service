package com.blift.aiservice.feignClient;

import com.blift.aiservice.dto.feign.AiActionCentreSummaryDto;
import com.blift.aiservice.dto.feign.AiRefundDto;
import com.blift.aiservice.dto.feign.AiWalletTransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "case-management-service")
public interface CaseMgtFeignClient {

    @GetMapping("/api/refund-request/rcic-internal/{rcicUserId}/pending")
    ResponseEntity<List<AiRefundDto>> getPendingRefundsForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/rcic-wallet/internal/{rcicUserId}/transactions/week")
    ResponseEntity<List<AiWalletTransactionDto>> getWeeklyTransactionsForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/rcic-wallet/internal/{rcicUserId}/transactions/last-week")
    ResponseEntity<List<AiWalletTransactionDto>> getLastWeekTransactionsForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/rcic-wallet/internal/{rcicUserId}/revenue-by-day")
    ResponseEntity<List<Map<String, Object>>> getRevenuByDayForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/rcic-wallet/internal/{rcicUserId}/revenue-analytics")
    ResponseEntity<List<Map<String, Object>>> getRevenueAnalyticsForRcic(
            @PathVariable Long rcicUserId,
            @RequestParam(defaultValue = "monthly") String period);

    @GetMapping("/api/action-centre/internal/{rcicUserId}/summary")
    ResponseEntity<AiActionCentreSummaryDto> getActionCentreSummaryForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/payment/rcic-internal/{rcicUserId}/completed-report-purchases-count")
    ResponseEntity<Long> getCompletedReportPurchasesCount(@PathVariable Long rcicUserId);

    /**
     * Checks whether an RCIC has active Blift Pro access (managed in case-mgt via Stripe).
     * This is a separate subscription system from the legacy user-service BliftProSubscription.
     */
    @GetMapping("/api/pro-subscription/internal/{rcicUserId}/has-access")
    ResponseEntity<java.util.Map<String, Boolean>> hasCaseMgtProAccess(@PathVariable Long rcicUserId);

    /**
     * Get aggregated revenue from all sources (Stripe + all consultation types) for this week.
     * Returns [{source, amount, currency, transactions}] for AI Business Insights calculation.
     */
    @GetMapping("/api/rcic-wallet/internal/{rcicUserId}/revenue/week/all-sources")
    ResponseEntity<List<Map<String, Object>>> getWeeklyRevenueAllSources(@PathVariable Long rcicUserId);

    /**
     * Get aggregated revenue from all sources for last week.
     */
    @GetMapping("/api/rcic-wallet/internal/{rcicUserId}/revenue/last-week/all-sources")
    ResponseEntity<List<Map<String, Object>>> getLastWeekRevenueAllSources(@PathVariable Long rcicUserId);

    /**
     * Get lifetime/all-time aggregated revenue from all sources.
     * Used for "Total Revenue" in Business Insights.
     */
    @GetMapping("/api/rcic-wallet/internal/{rcicUserId}/revenue/lifetime/all-sources")
    ResponseEntity<List<Map<String, Object>>> getLifetimeRevenueAllSources(@PathVariable Long rcicUserId);
}