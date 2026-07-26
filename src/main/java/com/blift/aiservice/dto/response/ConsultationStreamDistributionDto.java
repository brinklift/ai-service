package com.blift.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing stream type distribution of consultations.
 * Contains a map of stream type names to consultation counts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationStreamDistributionDto {
    private Map<String, Long> streamDistribution;
    private Long totalConsultations;
}
