package com.blift.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing geographical distribution of clients by country.
 * Contains a map of country names to consultation counts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientGeographicalDistributionDto {
    private Map<String, Long> countryDistribution;
    private Long totalClients;
}
