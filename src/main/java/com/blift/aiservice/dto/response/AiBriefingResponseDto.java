package com.blift.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiBriefingResponseDto {
    private String briefingText;
    private List<String> bulletPoints;
    private List<Map<String, Object>> clientSpotlights;
    private LocalDate briefingDate;
    private Instant generatedAt;
    private boolean fromCache;
    private String disclaimer;
}
