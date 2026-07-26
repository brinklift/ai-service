    package com.blift.aiservice.service.briefing;

    import com.blift.aiservice.dto.openai.OpenAiMessage;
    import com.blift.aiservice.dto.openai.OpenAiRequest;
    import com.blift.aiservice.dto.openai.OpenAiResponse;
    import com.blift.aiservice.entity.RcicAiBriefing;
    import com.blift.aiservice.entity.RcicAiContext;
    import com.blift.aiservice.repository.RcicAiBriefingRepository;
    import com.blift.aiservice.repository.RcicAiContextRepository;
    import com.blift.aiservice.service.context.AiContextBuilderService;
    import com.fasterxml.jackson.core.type.TypeReference;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;
    import org.springframework.web.reactive.function.client.WebClient;
    import reactor.util.retry.Retry;

    import java.time.Duration;
    import java.time.LocalDate;
    import java.util.List;
    import java.util.Map;
    import java.util.Optional;

    @Slf4j
    @Service
    @RequiredArgsConstructor
    public class AiBriefingService {

        private final RcicAiContextRepository contextRepository;
        private final RcicAiBriefingRepository briefingRepository;
        private final AiContextBuilderService contextBuilderService;
        private final WebClient.Builder webClientBuilder;
        private final ObjectMapper objectMapper;

        @Value("${openai.api.key}")
        private String openAiApiKey;

        @Value("${openai.api.url}")
        private String openAiApiUrl;

        @Value("${openai.api.model:gpt-4-turbo}")
        private String openAiModel;

        private static final String SYSTEM_PROMPT = """
                You are an AI Practice Assistant for a Regulated Canadian Immigration Consultant (RCIC) on the Blift platform.

                Your job is to do two things:
                1. Write a concise, warm daily briefing (under 180 words) from the operational facts provided.
                2. For each client with eligibility data, assess their immigration pathway match level and estimate their CRS score if sufficient data is present.

                Rules:
                - Address the RCIC by first name.
                - Do not invent facts not present in the data.
                - Do not provide immigration legal advice.
                - CRS estimates must be labelled "Estimated" and include a brief rationale.
                - If insufficient data exists to estimate CRS, omit the score and say "Incomplete profile".
                - Respond in valid JSON only. No markdown, no code blocks. Pure JSON.

                Response schema:
                {
                  "briefingText": "Good morning, [FirstName]! Here's what's important today...",
                  "bulletPoints": ["bullet 1", "bullet 2"],
                  "clientSpotlights": [
                    {
                      "clientUserId": 456,
                      "matchLevel": "High",
                      "estimatedCrsScore": 468,
                      "rationale": "..."
                    }
                  ]
                }
                """;

        public RcicAiBriefing getOrGenerateBriefing(Long rcicUserId) {
            LocalDate today = LocalDate.now();
            Optional<RcicAiBriefing> cached = briefingRepository.findByRcicUserIdAndBriefingDate(rcicUserId, today);
            if (cached.isPresent()) {
                log.info("[AI Briefing] Returning cached briefing for RCIC {}", rcicUserId);
                return cached.get();
            }
            return generateBriefing(rcicUserId);
        }

        public RcicAiBriefing refreshBriefing(Long rcicUserId) {
            LocalDate today = LocalDate.now();
            Optional<RcicAiBriefing> cached = briefingRepository.findByRcicUserIdAndBriefingDate(rcicUserId, today);
            if (cached.isPresent()) {
                log.info("[AI Briefing] Refresh requested but cached briefing already exists for RCIC {} today — skipping OpenAI call", rcicUserId);
                return cached.get();
            }
            RcicAiContext context = contextBuilderService.buildContextForRcic(rcicUserId);
            return generateBriefingFromContext(rcicUserId, context);
        }

        private RcicAiBriefing generateBriefing(Long rcicUserId) {
            LocalDate today = LocalDate.now();
            RcicAiContext context = contextRepository
                    .findByRcicUserIdAndContextDate(rcicUserId, today)
                    .orElseGet(() -> contextBuilderService.buildContextForRcic(rcicUserId));
            return generateBriefingFromContext(rcicUserId, context);
        }

        @SuppressWarnings("unchecked")
        private RcicAiBriefing generateBriefingFromContext(Long rcicUserId, RcicAiContext context) {
            LocalDate today = LocalDate.now();
            log.info("[AI Briefing] Generating briefing for RCIC {} from context id={}", rcicUserId, context.getId());

            try {
                String userPrompt = buildUserPrompt(context.getSnapshotJson());

                OpenAiMessage systemMsg = new OpenAiMessage("system", SYSTEM_PROMPT);
                OpenAiMessage userMsg = new OpenAiMessage("user", userPrompt);

                OpenAiRequest request = OpenAiRequest.builder()
                        .model(openAiModel)
                        .messages(List.of(systemMsg, userMsg))
                        .temperature(0.4)
                        .stream(false)
                        .build();

                OpenAiResponse aiResponse = webClientBuilder.build()
                        .post()
                        .uri(openAiApiUrl)
                        .header("Authorization", "Bearer " + openAiApiKey)
                        .header("Content-Type", "application/json")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(OpenAiResponse.class)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                        .block(Duration.ofSeconds(60));

                if (aiResponse == null || aiResponse.getChoices() == null || aiResponse.getChoices().isEmpty()) {
                    throw new IllegalStateException("Empty response from OpenAI");
                }

                String rawContent = aiResponse.getChoices().get(0).getMessage().getContent();
                Map<String, Object> parsed = objectMapper.readValue(rawContent, new TypeReference<>() {});

                String briefingText = (String) parsed.getOrDefault("briefingText", "Good morning! Here is your daily briefing.");
                List<String> bulletPoints = (List<String>) parsed.getOrDefault("bulletPoints", List.of());
                List<Map<String, Object>> spotlights = (List<Map<String, Object>>) parsed.getOrDefault("clientSpotlights", List.of());

                RcicAiBriefing briefing = briefingRepository
                        .findByRcicUserIdAndBriefingDate(rcicUserId, today)
                        .orElseGet(() -> {
                            RcicAiBriefing created = new RcicAiBriefing();
                            created.setRcicUserId(rcicUserId);
                            created.setBriefingDate(today);
                            return created;
                        });

                briefing.setBriefingText(briefingText);
                briefing.setBulletPoints(objectMapper.writeValueAsString(bulletPoints));
                briefing.setClientSpotlights(objectMapper.writeValueAsString(spotlights));
                briefing.setContextSnapshot(context);
                briefing.setModelVersion(openAiModel);
                if (aiResponse.getUsage() != null) {
                    briefing.setPromptTokens(aiResponse.getUsage().getPromptTokens());
                    briefing.setCompletionTokens(aiResponse.getUsage().getCompletionTokens());
                }

                return briefingRepository.save(briefing);
            } catch (Exception e) {
                log.error("[AI Briefing] Failed to generate briefing for RCIC {}: {}", rcicUserId, e.getMessage(), e);
                RcicAiBriefing fallback = new RcicAiBriefing();
                fallback.setRcicUserId(rcicUserId);
                fallback.setBriefingDate(today);
                fallback.setBriefingText("AI briefing is temporarily unavailable. Your Action Center remains fully functional.");
                fallback.setBulletPoints("[]");
                fallback.setClientSpotlights("[]");
                fallback.setContextSnapshot(context);
                fallback.setModelVersion(openAiModel);
                return fallback;
            }
        }

        private String buildUserPrompt(String snapshotJson) {
            return "Here is today's operational data for the RCIC. Generate the daily briefing based on these facts:\n\n" + snapshotJson;
        }
    }
