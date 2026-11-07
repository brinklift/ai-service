package com.blift.aiservice.feignClient;

import com.blift.aiservice.dto.feign.AiRcicProfileDto;
import com.blift.aiservice.dto.feign.SubscriptionStatusDto;
import com.blift.common.dto.JWTToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/api/rcic-profile/internal/{userId}")
    ResponseEntity<AiRcicProfileDto> getRcicProfileByUserId(@PathVariable Long userId);

    @GetMapping("/api/blift-pro/subscription/internal/active-rcic-ids")
    ResponseEntity<List<Long>> getActiveBliftProRcicIds();

    @GetMapping("/api/blift-pro/subscription/internal/{rcicUserId}/status")
    ResponseEntity<SubscriptionStatusDto> getSubscriptionStatusForRcic(@PathVariable Long rcicUserId);

    @GetMapping("/api/user/internal/id-by-email/{email}")
    ResponseEntity<Long> getUserIdByEmail(@PathVariable String email);

    @GetMapping("/api/user/internal/{userId}")
    ResponseEntity<AiRcicProfileDto> getUserById(@PathVariable Long userId);

    /** user-service only answers for the caller's own token, so the RCIC's Authorization header is forwarded. */
    @GetMapping("/api/direct-messages/unread-message-counter/{userId}")
    ResponseEntity<com.blift.common.dto.LongDto> getUnreadMessageCounter(@org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization, @PathVariable Long userId);

    @PostMapping("/api/user/get-user-id-by-token")
    ResponseEntity<Long> getCurrentUserId(@RequestBody JWTToken jwtToken);
}
