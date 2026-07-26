package com.blift.aiservice.controller;

import com.blift.aiservice.dto.feign.SubscriptionStatusDto;
import com.blift.aiservice.dto.response.AiBriefingResponseDto;
import com.blift.aiservice.dto.response.AiInsightsResponseDto;
import com.blift.aiservice.dto.response.SubscriptionCheckDto;
import com.blift.aiservice.entity.RcicAiBriefing;
import com.blift.aiservice.feignClient.CaseMgtFeignClient;
import com.blift.aiservice.feignClient.UserFeignClient;
import com.blift.aiservice.service.briefing.AiBriefingService;
import com.blift.aiservice.service.insights.AiInsightsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiPracticeAssistantController {

    private static final String DISCLAIMER = "AI insights are provided for informational and productivity purposes only. " +
            "They do not constitute immigration advice, legal guidance, or professional recommendations. " +
            "The RCIC is solely responsible for all decisions made on behalf of their clients.";

    private final AiBriefingService briefingService;
    private final AiInsightsService insightsService;
    private final CaseMgtFeignClient caseMgtFeignClient;
    private final UserFeignClient userFeignClient;
    private final ObjectMapper objectMapper;

    @GetMapping("/subscription/status")
    public ResponseEntity<SubscriptionCheckDto> getSubscriptionStatus(HttpServletRequest request) {
        Long rcicUserId = extractUserId(request);
        if (rcicUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            SubscriptionStatusDto status = userFeignClient.getSubscriptionStatusForRcic(rcicUserId).getBody();
            if (status == null) {
                SubscriptionCheckDto dto = new SubscriptionCheckDto();
                dto.setActive(false);
                dto.setPlanType("NONE");
                return ResponseEntity.ok(dto);
            }
            SubscriptionCheckDto dto = new SubscriptionCheckDto();
            dto.setActive(status.isActive());
            dto.setPlanType(status.getPlanType());
            dto.setInGracePeriod(status.isInGracePeriod());
            dto.setDaysRemaining(status.getDaysRemaining());
            dto.setAiConsentGiven(status.isAiConsentGiven());
            dto.setTrialExpiresAt(status.getTrialExpiresAt());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error checking subscription for RCIC {}: {}", rcicUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/briefing")
    public ResponseEntity<AiBriefingResponseDto> getBriefing(HttpServletRequest request) {
        Long rcicUserId = extractUserId(request);
        if (rcicUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isSubscribed(rcicUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            RcicAiBriefing briefing = briefingService.getOrGenerateBriefing(rcicUserId);
            return ResponseEntity.ok(toBriefingDto(briefing, false));
        } catch (Exception e) {
            log.error("Error getting briefing for RCIC {}: {}", rcicUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/briefing/refresh")
    public ResponseEntity<AiBriefingResponseDto> refreshBriefing(HttpServletRequest request) {
        Long rcicUserId = extractUserId(request);
        if (rcicUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isSubscribed(rcicUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            RcicAiBriefing briefing = briefingService.refreshBriefing(rcicUserId);
            return ResponseEntity.ok(toBriefingDto(briefing, false));
        } catch (Exception e) {
            log.error("Error refreshing briefing for RCIC {}: {}", rcicUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/insights")
    public ResponseEntity<AiInsightsResponseDto> getInsights(HttpServletRequest request) {
        Long rcicUserId = extractUserId(request);
        if (rcicUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isSubscribed(rcicUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(insightsService.getInsights(rcicUserId));
        } catch (Exception e) {
            log.error("Error getting insights for RCIC {}: {}", rcicUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/revenue-analytics")
    public ResponseEntity<List<Map<String, Object>>> getRevenueAnalytics(
            HttpServletRequest request,
            @RequestParam(defaultValue = "monthly") String period) {
        Long rcicUserId = extractUserId(request);
        if (rcicUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isSubscribed(rcicUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            var resp = caseMgtFeignClient.getRevenueAnalyticsForRcic(rcicUserId, period);
            return ResponseEntity.ok(resp.getBody() != null ? resp.getBody() : List.of());
        } catch (Exception e) {
            log.error("Error getting revenue analytics for RCIC {}: {}", rcicUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean isSubscribed(Long rcicUserId) {
        try {
            SubscriptionStatusDto status = userFeignClient.getSubscriptionStatusForRcic(rcicUserId).getBody();
            return status != null && status.isActive();
        } catch (Exception e) {
            log.warn("Subscription check failed for RCIC {}: {}", rcicUserId, e.getMessage());
            return false;
        }
    }

    private Long extractUserId(HttpServletRequest request) {
        try {
            // Extract email from validated JWT in Spring Security context (most reliable)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String email = jwt.getClaimAsString("email");
                if (email == null || email.isBlank()) {
                    // Fallback: use preferred_username as email
                    email = jwt.getClaimAsString("preferred_username");
                }
                if (email != null && !email.isBlank()) {
                    log.debug("Resolving user ID from JWT email: {}", email);
                    return userFeignClient.getUserIdByEmail(email).getBody();
                }
            }
            log.warn("No JwtAuthenticationToken in SecurityContext, cannot extract user ID");
            return null;
        } catch (Exception e) {
            log.warn("Could not extract user_id from token: {}", e.getMessage());
            return null;
        }
    }

    private AiBriefingResponseDto toBriefingDto(RcicAiBriefing briefing, boolean fromCache) {
        List<String> bullets = List.of();
        List<Map<String, Object>> spotlights = List.of();
        try {
            if (briefing.getBulletPoints() != null) {
                bullets = objectMapper.readValue(briefing.getBulletPoints(), new TypeReference<>() {});
            }
            if (briefing.getClientSpotlights() != null) {
                spotlights = objectMapper.readValue(briefing.getClientSpotlights(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("Could not parse briefing JSON fields: {}", e.getMessage());
        }
        AiBriefingResponseDto dto = new AiBriefingResponseDto();
        dto.setBriefingText(briefing.getBriefingText());
        dto.setBulletPoints(bullets);
        dto.setClientSpotlights(spotlights);
        dto.setBriefingDate(briefing.getBriefingDate());
        dto.setGeneratedAt(briefing.getGeneratedAt());
        dto.setFromCache(fromCache);
        dto.setDisclaimer(DISCLAIMER);
        return dto;
    }
}
