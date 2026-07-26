package com.blift.aiservice.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRcicProfileDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String timezone;
}
