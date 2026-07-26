package com.blift.aiservice.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiWorkspaceDto {
    private Long id;
    private Long bookingId;
    private Long clientId;
    private String status;
    private Boolean sharedWithClient;
    private String reportContent;
    private Integer age;
    private String education;
    private String workExperience;
    private String languageTestResults;
    private String statusInCanada;
    private String clientName;
    private String bookingReference;
}
