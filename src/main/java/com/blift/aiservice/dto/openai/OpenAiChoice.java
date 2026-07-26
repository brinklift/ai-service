package com.blift.aiservice.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiChoice {
    @JsonProperty("message")
    private OpenAiMessage message;

    @JsonProperty("finish_reason")
    private String finishReason;
}
