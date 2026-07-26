package com.blift.aiservice.feignClient;

import com.blift.aiservice.dto.feign.AiWorkspaceDto;
import com.blift.aiservice.dto.feign.BookingCountsDto;
import com.blift.aiservice.dto.response.ClientGeographicalDistributionDto;
import com.blift.aiservice.dto.response.ConsultationStreamDistributionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "booking-service")
public interface BookingFeignClient {

    @PostMapping("/api/appointment-slot-booking/slots-by-rcic-user-id/{rcicUserId}")
    ResponseEntity<Map<String, Object>> getBookingsByRcicUserId(
            @PathVariable Long rcicUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size);

    @GetMapping("/api/action-centre/booking-counts-internal/{rcicUserId}")
    ResponseEntity<BookingCountsDto> getBookingCountsForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/consultation-workspace/rcic-internal/{rcicUserId}/reports-ready-to-share")
    ResponseEntity<List<AiWorkspaceDto>> getReportsReadyToShareForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/consultation-workspace/rcic-internal/{rcicUserId}/report-status-counts")
    ResponseEntity<Map<String, Object>> getReportStatusCountsForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/action-centre/workload-forecast-internal/{rcicUserId}")
    ResponseEntity<Map<String, Object>> getWorkloadForecastForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/appointment-slot-booking/rcic-internal/{rcicUserId}/unique-clients-count")
    ResponseEntity<Long> getTotalClientsServedForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/appointment-slot-booking/rcic-internal/{rcicUserId}/total-completed-consultations")
    ResponseEntity<Long> getTotalCompletedConsultationsForRcic(@PathVariable Long rcicUserId);

    /**
     * Returns the geographical distribution of clients by country.
     * Optionally filtered by date range.
     * User ID is extracted from JWT token in the request.
     */
    @GetMapping("/api/appointment-slot-booking/rcic-internal/client-geographical-distribution")
    ResponseEntity<ClientGeographicalDistributionDto> getClientGeographicalDistributionForRcic(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate);

    /**
     * Returns the stream type distribution of consultations.
     * Optionally filtered by date range.
     * User ID is extracted from JWT token in the request.
     */
    @GetMapping("/api/appointment-slot-booking/rcic-internal/consultation-stream-distribution")
    ResponseEntity<ConsultationStreamDistributionDto> getConsultationStreamDistributionForRcic(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate);
}
