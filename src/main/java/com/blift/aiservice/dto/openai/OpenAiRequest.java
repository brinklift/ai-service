package com.blift.aiservice.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenAiRequest {
    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<OpenAiMessage> messages;

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("stream")
    private Boolean stream;
}
